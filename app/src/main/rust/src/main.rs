// Use the local chatbot module instead of trying to import from the library
mod chatbot;
use chatbot::ChatBot;

fn main() {
    println!("Testing Oxide Lab Mobile Rust library");
    
    let chat_bot = ChatBot::new();
    let response = chat_bot.process_message("Hello from Rust test!");
    println!("Chat bot response: {}", response);
    
    println!("Test completed successfully!");
}