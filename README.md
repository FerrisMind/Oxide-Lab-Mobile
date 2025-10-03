# Oxide Lab Mobile

An Android mobile application developed using Kotlin and Jetpack Compose with Rust/Candle integration for local chat-bot functionality.

## Features

- Modern Android UI with Jetpack Compose
- Local chat-bot implementation using Rust and Candle ML framework
- Native performance with Rust backend
- Code quality enforcement with ktlint and detekt

## Prerequisites

- Android Studio
- JDK 11 or higher
- Rust toolchain
- Android NDK
- cargo-ndk

## Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and run on emulator or device

## Architecture

The application follows a standard Android architecture with:
- Kotlin for the frontend UI
- Rust for the backend processing with Candle ML
- JNI for communication between Kotlin and Rust

## Building

The Rust library is automatically built when you build the Android app. You can also build it manually:

### On Unix-like systems:
```bash
./app/src/main/rust/build.sh
```

### On Windows:
```cmd
app\src\main\rust\build.bat
```

## Code Quality

This project uses ktlint and detekt for code quality enforcement:

### Running Linters
```bash
# Run ktlint check
./gradlew ktlintCheck

# Run ktlint format (auto-fixes issues)
./gradlew ktlintFormat

# Run detekt
./gradlew detekt

# Run both linters
./scripts/lint.sh
```

## Documentation

- [Chat Bot Implementation](CHATBOT.md)
- [Rust Integration](app/src/main/rust/README.md)
- [Linting Setup](docs/LINTING.md)

## License

MIT