use std::ptr;
use std::sync::{Arc, Mutex};

use jni::objects::{GlobalRef, JClass, JObject, JString, JValue};
use jni::sys::{jint, jstring};
use jni::JNIEnv;
use once_cell::sync::Lazy;

use crate::chatbot::ChatBot;
use crate::model_inference::{GenerationConfig, InferenceError, StreamCallback};
use crate::model_manager::ModelType;

/// Глобальное состояние чат-бота для JNI слоёв.
struct BotState {
    chatbot: ChatBot,
}

impl BotState {
    fn singleton() -> &'static Mutex<Option<Self>> {
        static INSTANCE: Lazy<Mutex<Option<BotState>>> = Lazy::new(|| Mutex::new(None));
        &INSTANCE
    }
}

fn init_bot() {
    let mut guard = BotState::singleton().lock().expect("bot mutex poisoned");
    if guard.is_none() {
        let chatbot = ChatBot::default();
        *guard = Some(BotState { chatbot });
    }
}

fn with_bot<F, R>(f: F) -> R
where
    F: FnOnce(&ChatBot) -> R,
{
    init_bot();
    let guard = BotState::singleton().lock().expect("bot mutex poisoned");
    let state = guard.as_ref().expect("bot not initialized");
    f(&state.chatbot)
}

/// Колбэк для потока генерации, вызывающий методы Java-объекта.
struct JniStreamCallback {
    java_vm: jni::JavaVM,
    callback: GlobalRef,
}

impl JniStreamCallback {
    fn with_attached_env<F>(&self, f: F)
    where
        F: FnOnce(&mut JNIEnv),
    {
        if let Ok(mut env) = self.java_vm.attach_current_thread() {
            f(&mut env);
        }
    }
}

impl StreamCallback for JniStreamCallback {
    fn on_token(&self, token: &str) {
        self.with_attached_env(|env| {
            let _ = env.call_method(
                self.callback.as_obj(),
                "onToken",
                "(Ljava/lang/String;)V",
                &[JValue::Object(&JObject::from(
                    env.new_string(token).unwrap(),
                ))],
            );
        });
    }

    fn on_complete(&self) {
        self.with_attached_env(|env| {
            let _ = env.call_method(self.callback.as_obj(), "onComplete", "()V", &[]);
        });
    }

    fn on_error(&self, error: &str) {
        self.with_attached_env(|env| {
            let _ = env.call_method(
                self.callback.as_obj(),
                "onError",
                "(Ljava/lang/String;)V",
                &[JValue::Object(&JObject::from(
                    env.new_string(error).unwrap(),
                ))],
            );
        });
    }
}

fn jni_exception(env: &mut JNIEnv, message: &str) {
    let _ = env.throw_new("java/lang/RuntimeException", message);
}

fn map_generation_config(env: &mut JNIEnv, config_obj: JObject) -> Option<GenerationConfig> {
    let max_tokens = env
        .call_method(&config_obj, "getMaxTokens", "()I", &[])
        .ok()?;
    let temperature = env
        .call_method(&config_obj, "getTemperature", "()F", &[])
        .ok()?;
    let top_p = env.call_method(&config_obj, "getTopP", "()F", &[]).ok()?;
    let repeat_penalty = env
        .call_method(&config_obj, "getRepeatPenalty", "()F", &[])
        .ok()?;

    Some(GenerationConfig {
        max_tokens: max_tokens.i().unwrap_or(512) as usize,
        temperature: temperature.f().unwrap_or(0.7),
        top_p: top_p.f().unwrap_or(0.9),
        repeat_penalty: repeat_penalty.f().unwrap_or(1.1),
        seed: 299792458,
    })
}

fn model_type_from_jint(model_type: jint) -> Option<ModelType> {
    match model_type {
        0 => Some(ModelType::Qwen3),
        1 => Some(ModelType::Gemma3),
        _ => None,
    }
}

fn handle_inference_error(env: &mut JNIEnv, error: InferenceError) -> jstring {
    let msg = format!("Ошибка инференса: {}", error);
    jni_exception(env, &msg);
    env.new_string(msg)
        .expect("failed to allocate error string")
        .into_raw()
}

/// Загружает модель (Qwen3/Gemma3).
#[no_mangle]
pub extern "system" fn Java_com_oxidelabmobile_RustInterface_loadModel(
    mut env: JNIEnv,
    _class: JClass,
    model_type: jint,
    variant: JString,
) {
    let model_type = match model_type_from_jint(model_type) {
        Some(t) => t,
        None => {
            jni_exception(&mut env, "Неизвестный тип модели");
            return;
        }
    };

    let variant_str = match env.get_string(&variant) {
        Ok(s) => s.to_str().unwrap_or("").to_owned(),
        Err(_) => {
            jni_exception(&mut env, "Не удалось прочитать variant");
            return;
        }
    };

    let result = with_bot(|bot| match model_type {
        ModelType::Qwen3 => futures::executor::block_on(bot.load_qwen3(&variant_str)),
        ModelType::Gemma3 => futures::executor::block_on(bot.load_gemma3(&variant_str)),
    });

    if let Err(err) = result {
        jni_exception(&mut env, &format!("Ошибка загрузки модели: {}", err));
    }
}

/// Переключает активную модель.
#[no_mangle]
pub extern "system" fn Java_com_oxidelabmobile_RustInterface_switchModel(
    mut env: JNIEnv,
    _class: JClass,
    model_type: jint,
    variant: JString,
) {
    let model_type = match model_type_from_jint(model_type) {
        Some(t) => t,
        None => {
            jni_exception(&mut env, "Неизвестный тип модели");
            return;
        }
    };

    let variant_str = match env.get_string(&variant) {
        Ok(s) => s.to_str().unwrap_or("").to_owned(),
        Err(_) => {
            jni_exception(&mut env, "Не удалось прочитать variant");
            return;
        }
    };

    let result =
        with_bot(|bot| futures::executor::block_on(bot.switch_model(model_type, &variant_str)));
    if let Err(err) = result {
        jni_exception(&mut env, &format!("Ошибка переключения модели: {}", err));
    }
}

/// Генерирует текст с опциональным потоковым колбэком.
#[no_mangle]
pub extern "system" fn Java_com_oxidelabmobile_RustInterface_generateText(
    mut env: JNIEnv,
    _class: JClass,
    prompt: JString,
    config: JObject,
    callback: JObject,
) -> jstring {
    let prompt_str = match env.get_string(&prompt) {
        Ok(s) => s.to_str().unwrap_or("").to_owned(),
        Err(_) => {
            jni_exception(&mut env, "Не удалось прочитать prompt");
            return std::ptr::null_mut();
        }
    };

    if let Some(config) = map_generation_config(&mut env, config) {
        with_bot(|bot| bot.set_generation_params(config));
    }

    let callback_arc = if !callback.is_null() {
        match env.new_global_ref(callback) {
            Ok(global) => {
                if let Ok(vm) = env.get_java_vm() {
                    Some(Arc::new(JniStreamCallback {
                        java_vm: vm,
                        callback: global,
                    }) as Arc<dyn StreamCallback>)
                } else {
                    jni_exception(&mut env, "Не удалось получить JavaVM");
                    None
                }
            }
            Err(_) => {
                jni_exception(&mut env, "Не удалось создать глобальную ссылку на callback");
                None
            }
        }
    } else {
        None
    };

    let result = with_bot(|bot| bot.generate_text(&prompt_str, callback_arc));
    match result {
        Ok(text) => env
            .new_string(text)
            .map(|s| s.into_raw())
            .unwrap_or(ptr::null_mut()),
        Err(err) => handle_inference_error(&mut env, err),
    }
}

/// Загружает модель из указанного пути к GGUF файлу.
/// Токенизатор извлекается из метаданных GGUF файла.
#[no_mangle]
pub extern "system" fn Java_com_oxidelabmobile_RustInterface_loadModelFromPath(
    mut env: JNIEnv,
    _class: JClass,
    model_path: JString,
) {
    let model_path_str = match env.get_string(&model_path) {
        Ok(s) => s.to_str().unwrap_or("").to_owned(),
        Err(_) => {
            jni_exception(&mut env, "Не удалось прочитать model_path");
            return;
        }
    };

    let model_path = std::path::Path::new(&model_path_str);

    let result = with_bot(|bot| bot.load_model_from_path(model_path));
    match result {
        Ok(model_type) => {
            log::info!(
                "Модель {:?} загружена из пути: {}",
                model_type,
                model_path_str
            );
        }
        Err(err) => {
            jni_exception(&mut env, &format!("Ошибка загрузки модели: {}", err));
        }
    }
}

/// JNI функция для выгрузки модели из памяти.
#[no_mangle]
pub extern "system" fn Java_com_oxidelabmobile_RustInterface_unloadModel(
    _env: JNIEnv,
    _class: JClass,
) {
    with_bot(|bot| bot.unload_model());
    log::info!("Модель выгружена из памяти");
}

/// Останавливает текущую генерацию текста.
#[no_mangle]
pub extern "system" fn Java_com_oxidelabmobile_RustInterface_stopGeneration(
    _env: JNIEnv,
    _class: JClass,
) {
    log::info!("JNI stopGeneration called");
    with_bot(|bot| {
        if let Some(engine) = bot.get_engine().read().as_ref() {
            log::info!("Setting stop flag on inference engine");
            engine.stop_generation();
            log::info!(
                "Stop flag set, current state: {}",
                engine.is_stop_requested()
            );
            log::info!("Generation stop requested successfully");
        } else {
            log::warn!("No active engine to stop generation");
        }
    });
}

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_vm: jni::JavaVM, _: *mut std::ffi::c_void) -> jint {
    // Initialize logging for Android
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Info)
            .with_tag("oxide_lab_mobile"),
    );

    init_bot();
    log::info!("JNI_OnLoad: Rust logging initialized for Oxide Lab Mobile");
    jni::sys::JNI_VERSION_1_6
}
