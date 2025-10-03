# Rust Integration for Oxide Lab Mobile

This directory contains the Rust code that integrates with the Android application using the Candle ML framework.

## Prerequisites

1. Install Rust: https://www.rust-lang.org/
2. Install Android NDK
3. Install cargo-ndk: `cargo install cargo-ndk`

## Building the Rust Library

### On Unix-like systems (Linux/macOS):
```bash
./build.sh
```

### On Windows:
```cmd
build.bat
```

### Manual build:
```bash
cargo ndk --target aarch64-linux-android --android-platform 29 -- build --release
```

## Project Structure

- `Cargo.toml`: Project dependencies and configuration
- `src/lib.rs`: Main library file with JNI interface
- `src/chatbot/`: Chat bot implementation using Candle
- `build.sh`: Build script for Unix-like systems
- `build.bat`: Build script for Windows

## Integration with Android

The Rust library is compiled as a shared library and placed in the `jniLibs` directory. The Kotlin code in `RustInterface.kt` loads this library and provides methods to call the Rust functions.