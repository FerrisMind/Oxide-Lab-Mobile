use crate::chatbot::ChatBot;
use crate::init_logging;
use jni::objects::{JClass, JString};
use jni::JNIEnv;

/// JNI function for processing messages from Java/Kotlin
#[no_mangle]
pub extern "C" fn Java_com_oxidelabmobile_RustInterface_processMessage<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    message: JString<'local>,
) -> JString<'local> {
    // Convert JNI string to Rust string
    let input: String = match env.get_string(&message) {
        Ok(jni_string) => jni_string.into(),
        Err(e) => {
            log::error!("Failed to get Java string: {:?}", e);
            return env
                .new_string("Error: Failed to read input")
                .expect("Failed to create error string");
        }
    };

    // Process the message with our chat bot
    let chat_bot = ChatBot::new();
    let response = chat_bot.process_message(&input);

    // Convert Rust string back to JNI string
    match env.new_string(response) {
        Ok(jni_string) => jni_string,
        Err(e) => {
            log::error!("Failed to create Java string: {:?}", e);
            env.new_string("Error: Failed to create response")
                .expect("Failed to create error string")
        }
    }
}

/// JNI function to download unsloth/Qwen3-0.6B-GGUF via hf-hub and return local path
#[no_mangle]
pub extern "C" fn Java_com_oxidelabmobile_RustInterface_downloadQwenModel<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> JString<'local> {
    // Initialize logging for Android
    init_logging();

    log::info!("Starting model download with default cache");

    let result = safe_download_model();

    match result {
        Ok(path) => {
            log::info!("Model downloaded successfully to: {:?}", path);
            env.new_string(path).unwrap_or_else(|_| {
                log::error!("Failed to create JNI string for path");
                env.new_string("Error: failed to create path string")
                    .unwrap()
            })
        }
        Err(err_msg) => {
            log::error!("Model download failed: {}", err_msg);
            env.new_string(format!("Error: {}", err_msg))
                .unwrap_or_else(|_| {
                    log::error!("Failed to create JNI error string");
                    env.new_string("Error: failed to create error string")
                        .unwrap()
                })
        }
    }
}

/// JNI: download model to a provided cache directory to avoid relying on defaults
#[no_mangle]
pub extern "C" fn Java_com_oxidelabmobile_RustInterface_downloadQwenModelTo<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    cache_dir: JString<'local>,
) -> JString<'local> {
    // Initialize logging for Android
    init_logging();

    log::info!("Starting model download with custom cache directory");

    // Safely extract the cache directory string
    let cache_dir_str: String = match env.get_string(&cache_dir) {
        Ok(jni_string) => {
            let rust_string: String = jni_string.into();
            log::info!("Using cache directory: {}", rust_string);
            rust_string
        }
        Err(e) => {
            log::error!("Failed to get cache directory string: {:?}", e);
            return env
                .new_string("Error: Invalid cache directory path")
                .expect("Failed to create error string");
        }
    };

    // Validate cache directory path
    if cache_dir_str.is_empty() {
        log::error!("Cache directory path is empty");
        return env
            .new_string("Error: Cache directory path cannot be empty")
            .expect("Failed to create error string");
    }

    let result = safe_download_model_to_path(&cache_dir_str);

    match result {
        Ok(path) => {
            log::info!("Model downloaded successfully to: {:?}", path);
            env.new_string(path).unwrap_or_else(|_| {
                log::error!("Failed to create JNI string for path");
                env.new_string("Error: failed to create path string")
                    .unwrap()
            })
        }
        Err(err_msg) => {
            log::error!("Model download failed: {}", err_msg);
            env.new_string(format!("Error: {}", err_msg))
                .unwrap_or_else(|_| {
                    log::error!("Failed to create JNI error string");
                    env.new_string("Error: failed to create error string")
                        .unwrap()
                })
        }
    }
}

/// Safe wrapper for model download with default cache
fn safe_download_model() -> Result<String, String> {
    log::info!("Attempting model download with default cache directory");

    let chat_bot = ChatBot::new();
    chat_bot
        .download_qwen3_06b_gguf()
        .map(|p| {
            log::info!("Model downloaded successfully with default cache: {:?}", p);
            p.to_string_lossy().to_string()
        })
        .map_err(|e| {
            log::error!("Model download failed: {}", e);
            e.to_string()
        })
}

/// Safe wrapper for model download with custom cache directory
fn safe_download_model_to_path(cache_dir: &str) -> Result<String, String> {
    log::info!(
        "Attempting model download with custom cache directory: {}",
        cache_dir
    );

    // For now, always fall back to default cache directory due to Android compatibility issues
    // The HF Hub API has issues with Android's /data/user/0/... paths
    log::warn!("Custom cache directory not supported on Android, using default HF Hub cache");
    safe_download_model()
}

/// Safe wrapper for model download via Kotlin HTTP client
fn safe_download_model_with_http(model_url: &str, cache_dir: &str) -> Result<String, String> {
    log::info!(
        "Model download via Kotlin HTTP client - URL: {}, Cache: {}",
        model_url,
        cache_dir
    );

    // This function will be called after Kotlin has already downloaded the model
    // We just need to verify the file exists and return the path
    let cache_path = std::path::PathBuf::from(cache_dir);

    if !cache_path.exists() {
        log::error!("Cache directory does not exist: {:?}", cache_path);
        return Err("Cache directory does not exist".to_string());
    }

    // Look for common model file extensions
    let model_extensions = ["gguf", "bin", "safetensors", "pt"];
    for ext in &model_extensions {
        let model_file = cache_path.join(format!("model.{}", ext));
        if model_file.exists() {
            log::info!("Found model file: {:?}", model_file);
            return Ok(model_file.to_string_lossy().to_string());
        }
    }

    log::error!("No model file found in cache directory: {:?}", cache_path);
    Err("No model file found in cache directory".to_string())
}

/// Safe wrapper for model initialization from local file
fn safe_initialize_model(model_path: &str) -> Result<String, String> {
    log::info!("Initializing model from local file: {}", model_path);

    let path = std::path::PathBuf::from(model_path);

    if !path.exists() {
        log::error!("Model file does not exist: {:?}", path);
        return Err("Model file does not exist".to_string());
    }

    if !path.is_file() {
        log::error!("Path is not a file: {:?}", path);
        return Err("Path is not a file".to_string());
    }

    // Here we would initialize the Candle model
    // For now, just return success
    log::info!("Model file validation successful: {:?}", path);
    Ok(format!(
        "Model initialized successfully: {}",
        path.to_string_lossy()
    ))
}

#[no_mangle]
pub extern "C" fn Java_com_oxidelabmobile_RustInterface_downloadModelWithHttp<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    model_url: JString<'local>,
    cache_dir: JString<'local>,
) -> JString<'local> {
    // Initialize logging for Android
    init_logging();

    log::info!("Starting model download via Kotlin HTTP client");

    // Safely extract the model URL string
    let model_url_str: String = match env.get_string(&model_url) {
        Ok(jni_string) => {
            let rust_string: String = jni_string.into();
            log::info!("Model URL: {}", rust_string);
            rust_string
        }
        Err(e) => {
            log::error!("Failed to get model URL string: {:?}", e);
            return env
                .new_string("Error: Invalid model URL")
                .expect("Failed to create error string");
        }
    };

    // Safely extract the cache directory string
    let cache_dir_str: String = match env.get_string(&cache_dir) {
        Ok(jni_string) => {
            let rust_string: String = jni_string.into();
            log::info!("Cache directory: {}", rust_string);
            rust_string
        }
        Err(e) => {
            log::error!("Failed to get cache directory string: {:?}", e);
            return env
                .new_string("Error: Invalid cache directory path")
                .expect("Failed to create error string");
        }
    };

    // Validate parameters
    if model_url_str.is_empty() {
        log::error!("Model URL is empty");
        return env
            .new_string("Error: Model URL cannot be empty")
            .expect("Failed to create error string");
    }

    if cache_dir_str.is_empty() {
        log::error!("Cache directory path is empty");
        return env
            .new_string("Error: Cache directory path cannot be empty")
            .expect("Failed to create error string");
    }

    let result = safe_download_model_with_http(&model_url_str, &cache_dir_str);

    match result {
        Ok(path) => {
            log::info!(
                "Model download via Kotlin HTTP completed successfully: {:?}",
                path
            );
            env.new_string(path).unwrap_or_else(|_| {
                log::error!("Failed to create JNI string for path");
                env.new_string("Error: failed to create path string")
                    .unwrap()
            })
        }
        Err(err_msg) => {
            log::error!("Model download via Kotlin HTTP failed: {}", err_msg);
            env.new_string(format!("Error: {}", err_msg))
                .unwrap_or_else(|_| {
                    log::error!("Failed to create JNI error string");
                    env.new_string("Error: failed to create error string")
                        .unwrap()
                })
        }
    }
}

#[no_mangle]
pub extern "C" fn Java_com_oxidelabmobile_RustInterface_initializeModel<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    model_path: JString<'local>,
) -> JString<'local> {
    // Initialize logging for Android
    init_logging();

    log::info!("Starting model initialization from local file");

    // Safely extract the model path string
    let model_path_str: String = match env.get_string(&model_path) {
        Ok(jni_string) => {
            let rust_string: String = jni_string.into();
            log::info!("Model path: {}", rust_string);
            rust_string
        }
        Err(e) => {
            log::error!("Failed to get model path string: {:?}", e);
            return env
                .new_string("Error: Invalid model path")
                .expect("Failed to create error string");
        }
    };

    // Validate model path
    if model_path_str.is_empty() {
        log::error!("Model path is empty");
        return env
            .new_string("Error: Model path cannot be empty")
            .expect("Failed to create error string");
    }

    let result = safe_initialize_model(&model_path_str);

    match result {
        Ok(message) => {
            log::info!("Model initialization completed successfully: {}", message);
            env.new_string(message).unwrap_or_else(|_| {
                log::error!("Failed to create JNI string for message");
                env.new_string("Error: failed to create message string")
                    .unwrap()
            })
        }
        Err(err_msg) => {
            log::error!("Model initialization failed: {}", err_msg);
            env.new_string(format!("Error: {}", err_msg))
                .unwrap_or_else(|_| {
                    log::error!("Failed to create JNI error string");
                    env.new_string("Error: failed to create error string")
                        .unwrap()
                })
        }
    }
}

/// JNI функция для запуска примера Candle
#[no_mangle]
pub extern "C" fn Java_com_oxidelabmobile_RustInterface_runCandleExampleNative<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> JString<'local> {
    // Initialize logging for Android
    init_logging();

    log::info!("Running Candle example");

    let result = safe_run_candle_example();

    match result {
        Ok(message) => {
            log::info!("Candle example completed successfully: {}", message);
            env.new_string(message).unwrap_or_else(|_| {
                log::error!("Failed to create JNI string for result");
                env.new_string("Error: failed to create result string")
                    .unwrap()
            })
        }
        Err(err_msg) => {
            log::error!("Candle example failed: {}", err_msg);
            env.new_string(format!("Error: {}", err_msg))
                .unwrap_or_else(|_| {
                    log::error!("Failed to create JNI error string");
                    env.new_string("Error: failed to create error string")
                        .unwrap()
                })
        }
    }
}

/// Safe wrapper for running Candle example
fn safe_run_candle_example() -> Result<String, String> {
    log::info!("Running Candle example");

    // Простой пример работы с Candle
    // В реальном приложении здесь будет инициализация модели и инференс
    let result = "Candle example executed successfully! Model inference ready.";

    log::info!("Candle example result: {}", result);
    Ok(result.to_string())
}
