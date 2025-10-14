use std::sync::Arc;

use candle_core::Device;
use parking_lot::RwLock;

use crate::model_inference::{GenerationConfig, InferenceEngine, InferenceError, StreamCallback};
use crate::model_manager::{ModelManager, ModelManagerError, ModelType};

/// ChatBot управляет загрузкой моделей и выполнением инференса.
pub struct ChatBot {
    model_manager: Arc<ModelManager>,
    engine: RwLock<Option<InferenceEngine>>,
}

impl Default for ChatBot {
    fn default() -> Self {
        Self::new(Device::Cpu)
    }
}

impl ChatBot {
    /// Создаёт новый чат-бот с указанным устройством.
    pub fn new(device: Device) -> Self {
        let root_dir = std::env::var("RUST_MODEL_ROOT").unwrap_or_else(|_| "/data/local/tmp/models".into());
        let manager = ModelManager::new(root_dir, device);
        Self {
            model_manager: Arc::new(manager),
            engine: RwLock::new(None),
        }
    }

    /// Загружает модель Qwen3 указанного варианта (например, "0.6b").
    pub async fn load_qwen3(&self, variant: &str) -> Result<(), ModelManagerError> {
        self.model_manager.load_model(ModelType::Qwen3, variant)?;
        self.refresh_engine();
        Ok(())
    }

    /// Загружает модель Gemma3 указанного варианта.
    pub async fn load_gemma3(&self, variant: &str) -> Result<(), ModelManagerError> {
        self.model_manager.load_model(ModelType::Gemma3, variant)?;
        self.refresh_engine();
        Ok(())
    }

    /// Загружает модель из указанного пути к GGUF файлу.
    pub fn load_model_from_path(
        &self,
        model_path: &std::path::Path,
    ) -> Result<ModelType, ModelManagerError> {
        log::info!("ChatBot::load_model_from_path called with path: {:?}", model_path);
        let model_type = self.model_manager.load_model_from_path(model_path)?;
        self.refresh_engine();
        Ok(model_type)
    }

    /// Переключает модель, выгружая текущую и загружая новую.
    pub async fn switch_model(
        &self,
        model_type: ModelType,
        variant: &str,
    ) -> Result<(), ModelManagerError> {
        self.model_manager.unload_model();
        self.model_manager.load_model(model_type, variant)?;
        self.refresh_engine();
        Ok(())
    }

    /// Выгружает текущую модель из памяти.
    pub fn unload_model(&self) {
        log::info!("ChatBot::unload_model called");
        self.model_manager.unload_model();
        self.refresh_engine();
    }

    /// Устанавливает параметры генерации.
    pub fn set_generation_params(&self, config: GenerationConfig) {
        if let Some(engine) = self.engine.write().as_mut() {
            engine.set_generation_params(config);
        }
    }

    /// Вызывает инференс, возвращая полный ответ.
    pub fn generate_text(
        &self,
        prompt: &str,
        callback: Option<Arc<dyn StreamCallback>>,
    ) -> Result<String, InferenceError> {
        let engine_guard = self.engine.read();
        let engine = engine_guard
            .as_ref()
            .ok_or(InferenceError::ModelNotLoaded)?;
        engine.generate_blocking(prompt, callback)
    }

    fn refresh_engine(&self) {
        match self.model_manager.current_model() {
            Some(snapshot) => {
                let engine = InferenceEngine::new(snapshot);
                *self.engine.write() = Some(engine);
            }
            None => {
                *self.engine.write() = None;
            }
        }
    }

    /// Проверяет, загружена ли модель.
    pub fn is_model_loaded(&self) -> bool {
        self.model_manager.is_loaded()
    }

    /// Возвращает ссылку на engine для JNI.
    pub fn get_engine(&self) -> &RwLock<Option<InferenceEngine>> {
        &self.engine
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_default_initialization() {
        let bot = ChatBot::default();
        assert!(!bot.is_model_loaded());
    }
}
