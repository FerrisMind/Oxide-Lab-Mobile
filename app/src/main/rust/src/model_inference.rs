//! Модуль инференса и потоковой генерации текста.
//! Предоставляет движок, поддерживающий настройку параметров генерации
//! и потоковый вывод токенов в Android через JNI.

use std::sync::Arc;

use candle_core::Device;
use thiserror::Error;

use crate::model_manager::{ActiveModel, LoadedModelSnapshot};
use candle_transformers::models::quantized_gemma3::ModelWeights as QuantizedGemma3;
use candle_transformers::models::quantized_qwen3::ModelWeights as QuantizedQwen3;

/// Ошибки движка инференса.
#[derive(Debug, Error)]
pub enum InferenceError {
    #[error("Модель не загружена")]
    ModelNotLoaded,
    #[error("Ошибка инференса: {0}")]
    Backend(String),
}

/// Настройки генерации текста.
#[derive(Debug, Clone)]
pub struct GenerationConfig {
    pub max_tokens: usize,
    pub temperature: f32,
    pub top_p: f32,
    pub repeat_penalty: f32,
    pub seed: u64,
}

impl Default for GenerationConfig {
    fn default() -> Self {
        Self {
            max_tokens: 512,
            temperature: 0.7,
            top_p: 0.9,
            repeat_penalty: 1.1,
            seed: 299792458,
        }
    }
}

/// Обработчик потоковой генерации.
pub trait StreamCallback: Send + Sync {
    /// Вызывается при генерации нового токена.
    fn on_token(&self, token: &str);
    /// Вызывается при завершении генерации.
    fn on_complete(&self);
    /// Вызывается при ошибке генерации.
    fn on_error(&self, error: &str);
}

/// Движок инференса для загруженной модели.
pub struct InferenceEngine {
    model_snapshot: LoadedModelSnapshot,
    config: GenerationConfig,
}

impl InferenceEngine {
    /// Создаёт новый движок для заданной модели.
    pub fn new(model_snapshot: LoadedModelSnapshot) -> Self {
        Self {
            model_snapshot,
            config: GenerationConfig::default(),
        }
    }

    /// Обновляет параметры генерации.
    pub fn set_generation_params(&mut self, config: GenerationConfig) {
        self.config = config;
    }

    /// Выполняет генерацию и возвращает полный результат строкой.
    pub fn generate_blocking(
        &self,
        prompt: &str,
        callback: Option<Arc<dyn StreamCallback>>,
    ) -> Result<String, InferenceError> {
        let model = self.model_snapshot.model();
        let tokenizer = self.model_snapshot.tokenizer();

        match model {
            ActiveModel::Qwen(model) => {
                self.generate_with_qwen3(model.clone(), tokenizer, prompt, callback)
            }
            ActiveModel::Gemma(model) => {
                self.generate_with_gemma3(model.clone(), tokenizer, prompt, callback)
            }
        }
    }

    fn generate_with_qwen3(
        &self,
        model: Arc<std::sync::Mutex<QuantizedQwen3>>,
        tokenizer: &tokenizers::Tokenizer,
        prompt: &str,
        callback: Option<Arc<dyn StreamCallback>>,
    ) -> Result<String, InferenceError> {
        use candle_transformers::generation::LogitsProcessor;

        // Применяем chat template если он доступен
        let formatted_prompt = if let Some(chat_template) = self.model_snapshot.chat_template() {
            apply_chat_template(chat_template, prompt)?
        } else {
            prompt.to_string()
        };

        let tokens = tokenizer
            .encode(formatted_prompt.as_str(), true)
            .map_err(|e| InferenceError::Backend(e.to_string()))?
            .get_ids()
            .to_vec();

        log::info!("Qwen3 - Prompt tokens: {}", tokens.len());
        log::info!("Qwen3 - Formatted prompt: {}", formatted_prompt);

        let mut logits_processor = LogitsProcessor::new(
            self.config.seed,
            Some(self.config.temperature as f64),
            Some(self.config.top_p as f64),
        );

        let mut all_tokens = Vec::new();
        let mut generated_text = String::new();

        // Process the prompt tokens with proper sampling to build context
        for (pos, &token) in tokens.iter().enumerate() {
            let input = candle_core::Tensor::new(&[token], self.model_snapshot.device())
                .map_err(|e| InferenceError::Backend(e.to_string()))?
                .unsqueeze(0)
                .map_err(|e| InferenceError::Backend(e.to_string()))?;

            let _logits = model
                .lock()
                .unwrap()
                .forward(&input, pos)
                .map_err(|e| InferenceError::Backend(e.to_string()))?;
            let _logits = _logits
                .squeeze(0)
                .map_err(|e| InferenceError::Backend(e.to_string()))?;

            all_tokens.push(token);

            // Decode incrementally to build the generated text
            if let Ok(decoded) = tokenizer.decode(&all_tokens, true) {
                generated_text = decoded.clone();
            }
        }

        // Start generation from the current position
        let mut current_pos = tokens.len();
        let max_context_length = 2048; // Limit context to avoid memory issues
        let eos_token = 151645u32; // Qwen3 EOS token

        for _ in 0..self.config.max_tokens {
            if all_tokens.is_empty() || all_tokens.len() >= max_context_length {
                break;
            }

            let input = candle_core::Tensor::new(
                &[all_tokens[all_tokens.len() - 1]],
                self.model_snapshot.device(),
            )
            .map_err(|e| InferenceError::Backend(e.to_string()))?
            .unsqueeze(0)
            .map_err(|e| InferenceError::Backend(e.to_string()))?;

            let logits_raw = model
                .lock()
                .unwrap()
                .forward(&input, current_pos)
                .map_err(|e| InferenceError::Backend(e.to_string()))?;
            let logits_raw = logits_raw
                .squeeze(0)
                .map_err(|e| InferenceError::Backend(e.to_string()))?;

            let logits = if self.config.repeat_penalty != 1.0 {
                candle_transformers::utils::apply_repeat_penalty(
                    &logits_raw,
                    self.config.repeat_penalty,
                    &all_tokens,
                )
                .map_err(|e| InferenceError::Backend(e.to_string()))?
            } else {
                logits_raw
            };

            let next_token = logits_processor
                .sample(&logits)
                .map_err(|e| InferenceError::Backend(e.to_string()))?;

            // Stop if we generated EOS token
            if next_token == eos_token {
                break;
            }

            all_tokens.push(next_token);
            current_pos += 1;

            if let Ok(decoded) = tokenizer.decode(&all_tokens, true) {
                if let Some(text) = decoded.strip_prefix(&generated_text) {
                    if !text.is_empty() {
                        if let Some(cb) = &callback {
                            cb.on_token(text);
                        }
                        generated_text.push_str(text);
                    }
                }
            }

            if next_token == *tokenizer.get_vocab(true).get("<|im_end|>").unwrap_or(&0) {
                break;
            }
        }

        if let Some(cb) = callback {
            cb.on_complete();
        }

        log::info!("Qwen3 - Generated {} tokens, result length: {}", all_tokens.len() - tokens.len(), generated_text.len());
        log::info!("Qwen3 - Final result: '{}'", generated_text);

        Ok(generated_text)
    }

    fn generate_with_gemma3(
        &self,
        model: Arc<std::sync::Mutex<QuantizedGemma3>>,
        tokenizer: &tokenizers::Tokenizer,
        prompt: &str,
        callback: Option<Arc<dyn StreamCallback>>,
    ) -> Result<String, InferenceError> {
        // Аналогичная логика для Gemma3, адаптированная под её API
        use candle_transformers::generation::LogitsProcessor;

        // Применяем chat template если он доступен
        let formatted_prompt = if let Some(chat_template) = self.model_snapshot.chat_template() {
            apply_chat_template(chat_template, prompt)?
        } else {
            prompt.to_string()
        };

        let tokens = tokenizer
            .encode(formatted_prompt.as_str(), true)
            .map_err(|e| InferenceError::Backend(e.to_string()))?
            .get_ids()
            .to_vec();

        let mut logits_processor = LogitsProcessor::new(
            self.config.seed,
            Some(self.config.temperature as f64),
            Some(self.config.top_p as f64),
        );

        let mut all_tokens = Vec::new();
        let mut generated_text = String::new();

        // Process the prompt tokens with proper sampling to build context
        for (pos, &token) in tokens.iter().enumerate() {
            let input = candle_core::Tensor::new(&[token], self.model_snapshot.device())
                .map_err(|e| InferenceError::Backend(e.to_string()))?
                .unsqueeze(0)
                .map_err(|e| InferenceError::Backend(e.to_string()))?;

            let _logits = model
                .lock()
                .unwrap()
                .forward(&input, pos)
                .map_err(|e| InferenceError::Backend(e.to_string()))?;
            let _logits = _logits
                .squeeze(0)
                .map_err(|e| InferenceError::Backend(e.to_string()))?;

            all_tokens.push(token);

            // Decode incrementally to build the generated text
            if let Ok(decoded) = tokenizer.decode(&all_tokens, true) {
                generated_text = decoded.clone();
            }
        }

        // Start generation from the current position
        let mut current_pos = tokens.len();
        let max_context_length = 2048; // Limit context to avoid memory issues
        let eos_token = 151645u32; // Qwen3 EOS token

        for _ in 0..self.config.max_tokens {
            if all_tokens.is_empty() || all_tokens.len() >= max_context_length {
                break;
            }

            let input = candle_core::Tensor::new(
                &[all_tokens[all_tokens.len() - 1]],
                self.model_snapshot.device(),
            )
            .map_err(|e| InferenceError::Backend(e.to_string()))?
            .unsqueeze(0)
            .map_err(|e| InferenceError::Backend(e.to_string()))?;

            let logits_raw = model
                .lock()
                .unwrap()
                .forward(&input, current_pos)
                .map_err(|e| InferenceError::Backend(e.to_string()))?;
            let logits_raw = logits_raw
                .squeeze(0)
                .map_err(|e| InferenceError::Backend(e.to_string()))?;

            let logits = if self.config.repeat_penalty != 1.0 {
                candle_transformers::utils::apply_repeat_penalty(
                    &logits_raw,
                    self.config.repeat_penalty,
                    &all_tokens,
                )
                .map_err(|e| InferenceError::Backend(e.to_string()))?
            } else {
                logits_raw
            };

            let next_token = logits_processor
                .sample(&logits)
                .map_err(|e| InferenceError::Backend(e.to_string()))?;

            // Stop if we generated EOS token
            if next_token == eos_token {
                break;
            }

            all_tokens.push(next_token);
            current_pos += 1;

            if let Ok(decoded) = tokenizer.decode(&all_tokens, true) {
                if let Some(text) = decoded.strip_prefix(&generated_text) {
                    if !text.is_empty() {
                        if let Some(cb) = &callback {
                            cb.on_token(text);
                        }
                        generated_text.push_str(text);
                    }
                }
            }

            if next_token == *tokenizer.get_vocab(true).get("<|im_end|>").unwrap_or(&0) {
                break;
            }
        }

        if let Some(cb) = callback {
            cb.on_complete();
        }

        Ok(generated_text)
    }

    /// Возвращает устройство вычислений.
    pub fn device(&self) -> &Device {
        self.model_snapshot.device()
    }
}

/// Применяет chat template для форматирования сообщения пользователя.
/// Пока реализуем простую версию, в будущем можно добавить поддержку Jinja2.
fn apply_chat_template(chat_template: &str, user_message: &str) -> Result<String, InferenceError> {
    log::info!("Applying chat template: {}", chat_template);

    // Для простоты используем базовое форматирование
    // В будущем можно добавить полноценный парсер Jinja2 шаблонов

    if chat_template.contains("{{messages}}") || chat_template.contains("{%") {
        // Это Jinja2 шаблон - пока возвращаем сообщение без изменений
        // TODO: реализовать полноценный парсер Jinja2
        log::warn!("Jinja2 chat template detected but not fully supported yet, using default format");

        // Попробуем базовое форматирование для Qwen моделей
        let system_message = "You are a helpful assistant.";
        let formatted = format!(
            "<|im_start|>system\n{}\n<|im_end|>\n<|im_start|>user\n{}\n<|im_end|>\n<|im_start|>assistant\n",
            system_message, user_message
        );
        Ok(formatted)
    } else if chat_template.contains("<|user|>") && chat_template.contains("<|assistant|>") {
        // Формат с специальными токенами
        let formatted = chat_template
            .replace("{{user_message}}", user_message)
            .replace("{{query}}", user_message)
            .replace("{{question}}", user_message);
        Ok(formatted)
    } else {
        // Простое форматирование для Qwen/HF моделей
        let system_message = "You are a helpful assistant.";
        let formatted = format!(
            "<|im_start|>system\n{}\n<|im_end|>\n<|im_start|>user\n{}\n<|im_end|>\n<|im_start|>assistant\n",
            system_message, user_message
        );
        Ok(formatted)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_default_generation_config() {
        let config = GenerationConfig::default();
        assert_eq!(config.max_tokens, 512);
        assert!((config.temperature - 0.7).abs() < f32::EPSILON);
        assert_eq!(config.seed, 299792458);
    }
}
