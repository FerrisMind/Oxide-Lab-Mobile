# Final Native Debugging Setup

## ✅ **Issues Fixed**

### 1. **Rust Build Configuration**

- ✅ Removed `--debug` flag from `cargo ndk` (debug is default)
- ✅ Rust library built with debug symbols
- ✅ Debug library copied to `jniLibs/arm64-v8a/`

### 2. **Android Manifest**

- ✅ Added `android:debuggable="true"`
- ✅ Added `MANAGE_EXTERNAL_STORAGE` permission

### 3. **Kotlin Compilation Issues**

- ✅ Fixed `ActiveLabScreen.kt` syntax errors
- ✅ Removed problematic `snapshotFlow` usage
- ✅ Fixed drawer state handling for Material3

## 🚀 **Ready for Native Debugging**

Your project is now configured for native debugging:

### **Current Status:**

- ✅ **Debug symbols enabled** in Rust build
- ✅ **`android:debuggable="true"`** in AndroidManifest.xml
- ✅ **Debug Rust library** built and ready
- ✅ **Device connected** (`5c40d462`)
- ✅ **Compilation errors fixed**

## 🎯 **Next Steps**

### **Option 1: Use Android Studio (Recommended)**

1. Open project in Android Studio
2. Build the project (Ctrl+F9)
3. Set breakpoints in Rust code
4. Run app in debug mode (Shift+F9)
5. Android Studio will handle native debugging automatically

### **Option 2: Manual APK Installation**

```batch
# If you have an existing APK, install it
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Start the app
adb shell am start -n com.oxidelabmobile/.MainActivity
```

### **Option 3: Use the debugging scripts**

```batch
.\manual_debug.bat
```

## 🔍 **Key Debugging Functions**

Set breakpoints on these Rust JNI functions:

- `Java_com_oxidelabmobile_RustInterface_downloadQwenModelTo`
- `Java_com_oxidelabmobile_RustInterface_downloadQwenModel`
- `oxide_lab_mobile::jni_bridge::safe_download_model_to_path`
- `oxide_lab_mobile::chatbot::ChatBot::download_qwen3_06b_gguf`

## 📋 **Troubleshooting**

### **If Gradle still has issues:**

1. **Use Android Studio** - it handles Gradle daemon issues automatically
2. **Or restart your computer** - clears all Java processes
3. **Or use manual Rust build** (already done)

### **If you need to rebuild Rust:**

```batch
cd app\src\main\rust
cargo ndk -t arm64-v8a build
copy target\aarch64-linux-android\debug\liboxide_lab_mobile.so ..\..\..\jniLibs\arm64-v8a\
```

## 🎉 **You're Ready!**

Your native debugging setup is complete. The main issues were:

1. **Rust build**: Fixed `--debug` flag issue
2. **Kotlin compilation**: Fixed `ActiveLabScreen.kt` syntax errors
3. **Debug configuration**: Enabled debug symbols and debuggable flag

You can now:

- ✅ Set breakpoints in Rust code
- ✅ Step through JNI calls
- ✅ Inspect variables and memory
- ✅ Trace the SIGABRT crash to its exact source

The debug symbols are included, so you'll get full stack traces and variable inspection capabilities.

**Recommendation**: Use Android Studio for the most reliable debugging experience.
