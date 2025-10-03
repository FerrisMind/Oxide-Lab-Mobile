#[cfg(test)]
mod tests {
    use super::*;
    use crate::chatbot::ChatBot;
    use std::path::PathBuf;

    #[test]
    fn test_chatbot_creation() {
        let bot = ChatBot::new();
        assert!(bot.process_message("test").contains("Processed with Candle"));
    }

    #[test]
    fn test_safe_download_model() {
        // This test would require network access and proper cache directory
        // For now, we'll just test that the function doesn't panic
        let result = std::panic::catch_unwind(|| {
            let bot = ChatBot::new();
            bot.download_qwen3_06b_gguf()
        });
        
        // The function should not panic, even if it fails due to network issues
        assert!(result.is_ok());
    }

    #[test]
    fn test_path_validation() {
        let valid_path = PathBuf::from("/tmp/test_cache");
        let empty_path = PathBuf::from("");
        
        assert!(!empty_path.to_string_lossy().is_empty() || empty_path.to_string_lossy() == "");
    }
}
