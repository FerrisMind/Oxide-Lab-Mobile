#!/bin/bash

# Build script for Rust library

# Check if cargo is installed
if ! command -v cargo &> /dev/null
then
    echo "cargo could not be found. Please install Rust from https://www.rust-lang.org/"
    exit 1
fi

# Check if cargo-ndk is installed
if ! command -v cargo-ndk &> /dev/null
then
    echo "cargo-ndk could not be found. Installing..."
    cargo install cargo-ndk
fi

# Build the Rust library for Android
echo "Building Rust library for Android..."
cargo ndk --target aarch64-linux-android --android-platform 29 -- build --release

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Rust library built successfully!"
    
    # Create jniLibs directory if it doesn't exist
    mkdir -p ../jniLibs/arm64-v8a
    
    # Copy the built library to jniLibs
    cp target/aarch64-linux-android/release/liboxide_lab_mobile.so ../jniLibs/arm64-v8a/
    
    echo "Library copied to jniLibs directory!"
else
    echo "Build failed!"
    exit 1
fi