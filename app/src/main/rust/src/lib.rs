use android_activity::AndroidApp;
use log::*;

pub mod chatbot;
pub mod jni_bridge;
use chatbot::ChatBot;

#[no_mangle]
fn android_main(app: AndroidApp) {
    android_logger::init_once(
        android_logger::Config::default().with_max_level(log::LevelFilter::Info),
    );

    info!("Starting Oxide Lab Mobile with Candle integration");

    // Initialize our chat bot
    let chat_bot = ChatBot::new();

    loop {
        app.poll_events(Some(std::time::Duration::from_millis(500)), |event| {
            match event {
                android_activity::PollEvent::Wake => {
                    info!("Early wake up");
                }
                android_activity::PollEvent::Timeout => {
                    info!("Timeout - running Candle operations");
                    // This is where we would run our Candle operations
                    run_candle_operations(&chat_bot);
                }
                android_activity::PollEvent::Main(main_event) => {
                    info!("Main event: {:?}", main_event);
                    match main_event {
                        android_activity::MainEvent::Destroy => {
                            info!("Application destroyed");
                            return;
                        }
                        _ => {}
                    }
                }
                _ => {}
            }

            // Fix the input_events method call
            // Remove the debug print that was causing issues
            // Properly handle the InputIterator
            match app.input_events_iter() {
                Ok(mut iter) => {
                    loop {
                        let read_input = iter.next(|_event| {
                            // Process input events if needed
                            // Return Unhandled if the event was not handled
                            android_activity::InputStatus::Unhandled
                        });

                        if !read_input {
                            break;
                        }
                    }
                }
                Err(err) => {
                    info!("Failed to get input events iterator: {:?}", err);
                }
            }
        });
    }
}

fn run_candle_operations(chat_bot: &ChatBot) {
    info!("Running Candle operations...");
    // This is where we would implement our Candle-based chat bot logic
    let response = chat_bot.process_message("Hello from Candle!");
    info!("Chat bot response: {}", response);
    info!("Candle operations completed");
}
