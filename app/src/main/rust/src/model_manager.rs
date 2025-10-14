//! Модуль управления загрузкой моделей GGUF (Qwen3/Gemma3).
//! Обеспечивает ленивую загрузку, выгрузку и кеширование метаданных.

use std::path::{Path, PathBuf};
use std::sync::Arc;

use candle_core::quantized::gguf_file;
use candle_core::Device;
use candle_transformers::models::quantized_gemma3::ModelWeights as QuantizedGemma3;
use candle_transformers::models::quantized_qwen3::ModelWeights as QuantizedQwen3;
use parking_lot::RwLock;
use thiserror::Error;

/// Поддерживаемые типы моделей.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ModelType {
    /// Семейство Qwen3.
    Qwen3,
    /// Семейство Gemma3.
    Gemma3,
}

impl ModelType {
    /// Возвращает человекочитаемое имя модели.
    pub fn as_str(&self) -> &'static str {
        match self {
            ModelType::Qwen3 => "Qwen3",
            ModelType::Gemma3 => "Gemma3",
        }
    }
}

/// Ошибки менеджера моделей.
#[derive(Debug, Error)]
pub enum ModelManagerError {
    #[error("Путь модели не задан")]
    MissingModelPath,
    #[error("Файл модели '{0}' не найден")]
    ModelFileMissing(String),
    #[error("Файл токенизатора '{0}' не найден")]
    TokenizerMissing(String),
    #[error("Ошибка Candle: {0}")]
    Candle(String),
    #[error("Ошибка инициализации модели: {0}")]
    Initialization(String),
}

impl From<candle_core::Error> for ModelManagerError {
    fn from(err: candle_core::Error) -> Self {
        Self::Candle(err.to_string())
    }
}

/// Обертка активной модели (Qwen3/Gemma3).
#[derive(Debug, Clone)]
pub enum ActiveModel {
    Qwen(Arc<std::sync::Mutex<QuantizedQwen3>>),
    Gemma(Arc<std::sync::Mutex<QuantizedGemma3>>),
}

/// Информация о загруженной модели.
#[derive(Debug)]
pub struct LoadedModel {
    model_type: ModelType,
    model: ActiveModel,
    tokenizer: tokenizers::Tokenizer,
    device: Device,
    chat_template: Option<String>,
}

impl LoadedModel {
    /// Возвращает токенизатор.
    pub fn tokenizer(&self) -> &tokenizers::Tokenizer {
        &self.tokenizer
    }

    /// Возвращает устройство вычислений.
    pub fn device(&self) -> &Device {
        &self.device
    }

    /// Возвращает ссылку на активную модель.
    pub fn model(&self) -> &ActiveModel {
        &self.model
    }

    /// Возвращает chat template, если он доступен.
    pub fn chat_template(&self) -> Option<&str> {
        self.chat_template.as_deref()
    }

    /// Возвращает тип загруженной модели.
    pub fn model_type(&self) -> ModelType {
        self.model_type
    }
}

/// Менеджер моделей с ленивой загрузкой.
pub struct ModelManager {
    root_dir: PathBuf,
    device: Device,
    inner: RwLock<Option<LoadedModel>>,
}

impl ModelManager {
    /// Создаёт новый менеджер.
    pub fn new(root_dir: impl AsRef<Path>, device: Device) -> Self {
        Self {
            root_dir: root_dir.as_ref().to_path_buf(),
            device,
            inner: RwLock::new(None),
        }
    }

    /// Загружает модель указанного типа.
    pub fn load_model(
        &self,
        model_type: ModelType,
        variant: &str,
    ) -> Result<(), ModelManagerError> {
        let model_dir = self.model_dir(model_type, variant);
        let model_path = self.validate_model_path(&model_dir)?;
        let tokenizer_path = self.validate_tokenizer_path(&model_dir)?;

        let tokenizer = tokenizers::Tokenizer::from_file(&tokenizer_path)
            .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;

        // Извлекаем chat template из метаданных GGUF файла
        let chat_template = {
            let mut reader = std::fs::File::open(&model_path)
                .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
            let content = gguf_file::Content::read(&mut reader)
                .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
            let metadata = &content.metadata;

            // Ищем chat template в различных возможных ключах
            metadata
                .get("tokenizer.chat_template")
                .or_else(|| metadata.get("chat_template"))
                .or_else(|| metadata.get("tokenizer.ggml.chat_template"))
                .and_then(|value| {
                    if let gguf_file::Value::String(template) = value {
                        Some(template.clone())
                    } else {
                        None
                    }
                })
        };

        let loaded_model = match model_type {
            ModelType::Qwen3 => {
                // Прочитаем файл заново для модели
                let mut reader = std::fs::File::open(&model_path)
                    .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
                let content = gguf_file::Content::read(&mut reader)
                    .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
                let qwen = QuantizedQwen3::from_gguf(content, &mut reader, &self.device)
                    .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
                ActiveModel::Qwen(Arc::new(std::sync::Mutex::new(qwen)))
            }
            ModelType::Gemma3 => {
                // Прочитаем файл заново для модели
                let mut reader = std::fs::File::open(&model_path)
                    .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
                let content = gguf_file::Content::read(&mut reader)
                    .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
                let gemma = QuantizedGemma3::from_gguf(content, &mut reader, &self.device)
                    .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
                ActiveModel::Gemma(Arc::new(std::sync::Mutex::new(gemma)))
            }
        };

        let loaded = LoadedModel {
            model_type,
            model: loaded_model,
            tokenizer,
            device: self.device.clone(),
            chat_template,
        };

        *self.inner.write() = Some(loaded);
        Ok(())
    }

    /// Загружает модель из указанного пути к GGUF файлу.
    /// Тип модели и токенизатор определяются автоматически из метаданных GGUF.
    pub fn load_model_from_path(&self, model_path: &Path) -> Result<ModelType, ModelManagerError> {
        log::info!(
            "ModelManager::load_model_from_path called with path: {:?}",
            model_path
        );

        // Определяем тип модели по имени файла
        let model_type = if model_path.to_string_lossy().to_lowercase().contains("qwen") {
            log::info!("Detected model type: Qwen3");
            ModelType::Qwen3
        } else if model_path
            .to_string_lossy()
            .to_lowercase()
            .contains("gemma")
        {
            log::info!("Detected model type: Gemma3");
            ModelType::Gemma3
        } else {
            log::info!("Cannot determine model type from filename");
            return Err(ModelManagerError::Initialization(
                "Cannot determine model type from filename".to_string(),
            ));
        };

        // Сначала читаем GGUF файл для извлечения метаданных и токенизатора
        let mut reader = std::fs::File::open(model_path)
            .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
        let content = gguf_file::Content::read(&mut reader)
            .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;

        // Извлекаем токенизатор из метаданных GGUF файла
        let tokenizer =
            Self::extract_tokenizer_from_gguf_metadata(&content.metadata).ok_or_else(|| {
                ModelManagerError::TokenizerMissing(
                    "No tokenizer found in GGUF metadata".to_string(),
                )
            })?;

        // Извлекаем chat template из метаданных
        let chat_template = content
            .metadata
            .get("tokenizer.chat_template")
            .or_else(|| content.metadata.get("chat_template"))
            .or_else(|| content.metadata.get("tokenizer.ggml.chat_template"))
            .and_then(|value| {
                if let gguf_file::Value::String(template) = value {
                    Some(template.clone())
                } else {
                    None
                }
            });

        // Теперь читаем файл заново для загрузки модели
        let mut reader = std::fs::File::open(model_path)
            .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;

        let loaded_model = match model_type {
            ModelType::Qwen3 => {
                let qwen = QuantizedQwen3::from_gguf(content, &mut reader, &self.device)
                    .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
                ActiveModel::Qwen(Arc::new(std::sync::Mutex::new(qwen)))
            }
            ModelType::Gemma3 => {
                // Прочитаем структуру заново, так как чтение consume reader.
                let mut reader = std::fs::File::open(model_path)
                    .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
                let content = gguf_file::Content::read(&mut reader)
                    .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
                let gemma = QuantizedGemma3::from_gguf(content, &mut reader, &self.device)
                    .map_err(|e| ModelManagerError::Initialization(e.to_string()))?;
                ActiveModel::Gemma(Arc::new(std::sync::Mutex::new(gemma)))
            }
        };

        let loaded = LoadedModel {
            model_type,
            model: loaded_model,
            tokenizer,
            device: self.device.clone(),
            chat_template,
        };

        *self.inner.write() = Some(loaded);
        Ok(model_type)
    }

    /// Извлекает токенизатор из метаданных GGUF файла.
    fn extract_tokenizer_from_gguf_metadata(
        metadata: &std::collections::HashMap<String, gguf_file::Value>,
    ) -> Option<tokenizers::Tokenizer> {
        // Отладка: выводим все ключи метаданных
        log::info!("GGUF metadata keys (total: {}):", metadata.len());
        for key in metadata.keys() {
            log::info!("  Key: {}", key);
            // Выведем значения для всех ключей (не только токенизаторных)
            match metadata.get(key) {
                Some(gguf_file::Value::String(s)) => log::info!("    String value: {}", s),
                Some(gguf_file::Value::Array(arr)) if arr.len() <= 10 => {
                    log::info!(
                        "    Array with {} elements: {:?}",
                        arr.len(),
                        arr.iter()
                            .take(10)
                            .map(|v| match v {
                                gguf_file::Value::String(s) =>
                                    format!("'{}'", s.chars().take(30).collect::<String>()),
                                gguf_file::Value::U32(n) => format!("U32:{}", n),
                                gguf_file::Value::F32(n) => format!("F32:{:.3}", n),
                                gguf_file::Value::U64(n) => format!("U64:{}", n),
                                _ => format!("{:?}", v),
                            })
                            .collect::<Vec<_>>()
                    );
                }
                Some(gguf_file::Value::Array(arr)) => {
                    log::info!("    Array with {} elements (showing first 3)", arr.len());
                    for (i, v) in arr.iter().take(3).enumerate() {
                        match v {
                            gguf_file::Value::String(s) => log::info!(
                                "      [{}]: '{}'",
                                i,
                                s.chars().take(50).collect::<String>()
                            ),
                            gguf_file::Value::U32(n) => log::info!("      [{}]: U32:{}", i, n),
                            gguf_file::Value::F32(n) => log::info!("      [{}]: F32:{:.3}", i, n),
                            _ => log::info!("      [{}]: {:?}", i, v),
                        }
                    }
                }
                Some(v) => log::info!("    Value: {:?}", v),
                None => {}
            }
        }

        // Пробуем разные возможные ключи для токенов (включая SentencePiece формат)
        let mut tokens = metadata
            .get("tokenizer.ggml.tokens")
            .or_else(|| metadata.get("tokenizer.huggingface.tokenizer.tokens"))
            .or_else(|| metadata.get("spm.vocab"))
            .or_else(|| metadata.get("vocab"))
            .or_else(|| metadata.get("tokenizer.tokens"))
            .or_else(|| metadata.get("tokenizer.spm.vocab"));

        // Если не нашли стандартные ключи, ищем любые ключи с "token"
        if tokens.is_none() {
            for key in metadata.keys() {
                if key.to_lowercase().contains("token")
                    && (key.to_lowercase().ends_with("vocab")
                        || key.to_lowercase().ends_with("tokens")
                        || key.to_lowercase().contains("vocab")
                        || key.to_lowercase().contains("tokens"))
                {
                    log::info!("Found potential token key: {}", key);
                    tokens = metadata.get(key);
                    if tokens.is_some() {
                        break;
                    }
                }
            }
        }

        let tokens = match tokens {
            Some(gguf_file::Value::Array(arr)) => {
                log::info!("Found token array with {} elements", arr.len());
                let mut tokens = Vec::new();
                for value in arr {
                    if let gguf_file::Value::String(s) = value {
                        // Обрабатываем специальные токены как в candle-pyo3
                        let processed = if s == "<0x0A>" {
                            "\n".to_string()
                        } else {
                            s.replace("▁", " ")
                        };
                        tokens.push(processed);
                    }
                }
                log::info!("Processed {} tokens", tokens.len());
                tokens
            }
            _ => {
                log::info!("No token array found, trying serialized tokenizer");
                // Пробуем найти сериализованный токенизатор (JSON) - разные возможные ключи
                let possible_keys = vec![
                    "tokenizer.ggml.tokenizer",
                    "tokenizer.huggingface.tokenizer",
                    "tokenizer",
                    "tokenizer_config",
                    "tokenizer.json",
                ];

                for key in possible_keys {
                    if let Some(gguf_file::Value::String(json_str)) = metadata.get(key) {
                        log::info!("Found serialized tokenizer JSON in key: {}", key);
                        match tokenizers::Tokenizer::from_bytes(json_str.as_bytes()) {
                            Ok(tokenizer) => {
                                log::info!("Successfully parsed tokenizer from key: {}", key);
                                return Some(tokenizer);
                            }
                            Err(e) => {
                                log::error!(
                                    "Failed to parse serialized tokenizer from {}: {}",
                                    key,
                                    e
                                );
                            }
                        }
                    }
                }

                log::info!("No serialized tokenizer found in any key");
                return None;
            }
        };

        // Извлекаем scores (могут отсутствовать)
        let scores = match metadata.get("tokenizer.ggml.scores") {
            Some(gguf_file::Value::Array(arr)) => {
                let mut scores = Vec::new();
                for value in arr {
                    if let gguf_file::Value::F32(s) = value {
                        scores.push(*s as f64);
                    }
                }
                scores
            }
            _ => {
                log::info!("No tokenizer.ggml.scores found, using default scores");
                // Создаем scores по умолчанию (все равны 0.0)
                vec![0.0; tokens.len()]
            }
        };

        // Выбираем тип токенизатора: BPE если есть merges, иначе Unigram
        let tokenizer = if let Some(gguf_file::Value::Array(merges_arr)) =
            metadata.get("tokenizer.ggml.merges")
        {
            log::info!("Creating BPE tokenizer with {} merges", merges_arr.len());

            // Извлекаем merges для BPE
            let mut merges = Vec::new();
            for value in merges_arr {
                if let gguf_file::Value::String(s) = value {
                    // Merge правило имеет формат "a b"
                    let parts: Vec<&str> = s.split(' ').collect();
                    if parts.len() == 2 {
                        merges.push((parts[0].to_string(), parts[1].to_string()));
                    }
                }
            }

            // Создаем vocab как HashMap<String, u32> для BPE
            let mut vocab = std::collections::HashMap::new();
            for (i, token) in tokens.into_iter().enumerate() {
                vocab.insert(token, i as u32);
            }

            // Создаем JSON конфигурацию для токенизатора
            let tokenizer_config = serde_json::json!({
                "version": "1.0",
                "model": {
                    "type": "BPE",
                    "vocab": vocab,
                    "merges": merges
                },
                "added_tokens": []
            });

            let tokenizer_bytes = tokenizer_config.to_string().into_bytes();
            let tokenizer = tokenizers::Tokenizer::from_bytes(tokenizer_bytes).ok()?;

            // Добавляем специальные токены если они есть
            if let Some(gguf_file::Value::U32(eos_id)) = metadata.get("tokenizer.ggml.eos_token_id")
            {
                // EOS token уже должен быть в словаре
                log::info!("EOS token ID: {}", eos_id);
            }
            if let Some(gguf_file::Value::U32(bos_id)) = metadata.get("tokenizer.ggml.bos_token_id")
            {
                log::info!("BOS token ID: {}", bos_id);
            }

            tokenizer
        } else {
            log::info!("Creating Unigram tokenizer (no merges found)");

            // Создаем простой токенизатор с Unigram моделью
            use std::collections::HashMap;
            let mut vocab_with_scores = HashMap::new();
            for (i, token) in tokens.into_iter().enumerate() {
                let score = scores.get(i).copied().unwrap_or(0.0);
                vocab_with_scores.insert(token, (i as u32, score));
            }

            // Конвертируем HashMap в Vec<(String, f64)> для Unigram::from
            let vocab_vec: Vec<(String, f64)> = vocab_with_scores
                .into_iter()
                .map(|(token, (_id, score))| (token, score))
                .collect();

            let unigram =
                tokenizers::models::unigram::Unigram::from(vocab_vec, None, false).ok()?;
            tokenizers::Tokenizer::new(unigram)
        };

        Some(tokenizer)
    }

    /// Выгружает текущую модель.
    pub fn unload_model(&self) {
        *self.inner.write() = None;
    }

    /// Проверяет, загружена ли модель.
    pub fn is_loaded(&self) -> bool {
        self.inner.read().is_some()
    }

    /// Возвращает активную модель, если она загружена.
    pub fn current_model(&self) -> Option<LoadedModelSnapshot> {
        self.inner.read().as_ref().map(LoadedModelSnapshot::new)
    }

    fn model_dir(&self, model_type: ModelType, variant: &str) -> PathBuf {
        let folder = match model_type {
            ModelType::Qwen3 => "qwen3",
            ModelType::Gemma3 => "gemma3",
        };
        self.root_dir.join(folder).join(variant)
    }

    fn validate_model_path(&self, model_dir: &Path) -> Result<PathBuf, ModelManagerError> {
        let gguf_path = model_dir.join("model.gguf");
        if !gguf_path.exists() {
            return Err(ModelManagerError::ModelFileMissing(
                gguf_path.to_string_lossy().into_owned(),
            ));
        }
        Ok(gguf_path)
    }

    fn validate_tokenizer_path(&self, model_dir: &Path) -> Result<PathBuf, ModelManagerError> {
        let tokenizer = model_dir.join("tokenizer.json");
        if !tokenizer.exists() {
            return Err(ModelManagerError::TokenizerMissing(
                tokenizer.to_string_lossy().into_owned(),
            ));
        }
        Ok(tokenizer)
    }
}

/// Снимок загруженной модели для потокобезопасного доступа.
#[derive(Clone)]
pub struct LoadedModelSnapshot {
    model_type: ModelType,
    tokenizer: tokenizers::Tokenizer,
    device: Device,
    model: Arc<ActiveModel>,
    chat_template: Option<String>,
}

impl LoadedModelSnapshot {
    fn new(loaded: &LoadedModel) -> Self {
        Self {
            model_type: loaded.model_type,
            tokenizer: loaded.tokenizer.clone(),
            device: loaded.device().clone(),
            model: Arc::new(loaded.model.clone()),
            chat_template: loaded.chat_template.clone(),
        }
    }

    /// Возвращает тип модели.
    pub fn model_type(&self) -> ModelType {
        self.model_type
    }

    /// Возвращает токенизатор.
    pub fn tokenizer(&self) -> &tokenizers::Tokenizer {
        &self.tokenizer
    }

    /// Возвращает устройство вычислений.
    pub fn device(&self) -> &Device {
        &self.device
    }

    /// Возвращает ссылку на модель.
    pub fn model(&self) -> &ActiveModel {
        &self.model
    }

    /// Возвращает chat template, если он доступен.
    pub fn chat_template(&self) -> Option<&str> {
        self.chat_template.as_deref()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;
    use tempfile::tempdir;

    fn setup_fake_model_dir(dir: &Path) {
        fs::create_dir_all(dir).unwrap();
        fs::write(dir.join("model.gguf"), b"fake gguf").unwrap();
        fs::write(dir.join("tokenizer.json"), b"{} ").unwrap();
    }

    #[test]
    fn test_missing_model_file() {
        let tmp = tempdir().unwrap();
        let manager = ModelManager::new(tmp.path(), Device::Cpu);
        let result = manager.load_model(ModelType::Qwen3, "0.6b");
        assert!(matches!(
            result,
            Err(ModelManagerError::ModelFileMissing(_))
        ));
    }

    #[test]
    fn test_missing_tokenizer_file() {
        let tmp = tempdir().unwrap();
        let model_dir = tmp.path().join("qwen3").join("0.6b");
        fs::create_dir_all(&model_dir).unwrap();
        fs::write(model_dir.join("model.gguf"), b"fake").unwrap();

        let manager = ModelManager::new(tmp.path(), Device::Cpu);
        let result = manager.load_model(ModelType::Qwen3, "0.6b");
        assert!(matches!(
            result,
            Err(ModelManagerError::TokenizerMissing(_))
        ));
    }
}
