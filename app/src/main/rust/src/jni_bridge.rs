use crate::chatbot::ChatBot;
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
