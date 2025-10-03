@echo off

REM Build script for Rust library on Windows

REM Check if cargo is installed
cargo --version >nul 2>&1
if %errorlevel% neq 0 (
    echo cargo could not be found. Please install Rust from https://www.rust-lang.org/
    exit /b 1
)

REM Check if cargo-ndk is installed
cargo-ndk --version >nul 2>&1
if %errorlevel% neq 0 (
    echo cargo-ndk could not be found. Installing...
    cargo install cargo-ndk
)

REM Build the Rust library for Android
echo Building Rust library for Android...
cargo ndk --target aarch64-linux-android --android-platform 29 -- build --release

REM Check if build was successful
if %errorlevel% equ 0 (
    echo Rust library built successfully!
    
    REM Create jniLibs directory if it doesn't exist
    if not exist "..\jniLibs\arm64-v8a" mkdir "..\jniLibs\arm64-v8a"
    
    REM Copy the built library to jniLibs
    copy "target\aarch64-linux-android\release\liboxide_lab_mobile.so" "..\jniLibs\arm64-v8a\"
    
    echo Library copied to jniLibs directory!
) else (
    echo Build failed!
    exit /b 1
)