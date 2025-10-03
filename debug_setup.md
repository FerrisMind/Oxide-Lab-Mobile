# Native Debugging Setup for Oxide Lab Mobile

## Prerequisites

1. **Android SDK and NDK installed**
2. **Device connected with USB debugging enabled**
3. **Debug APK built with symbols**

## Environment Variables

Set these environment variables in your system:

```batch
set ANDROID_SDK_ROOT=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set ANDROID_NDK_HOME=%ANDROID_SDK_ROOT%\ndk\25.2.9519653
```

## Steps to Debug

### 1. Build Debug APK

```batch
.\gradlew assembleDebug
```

### 2. Run Debugging Script

```batch
.\debug_native.bat
```

### 3. Manual ndk-gdb Commands

If the script doesn't work, run these commands manually:

```batch
# Install debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Start the app
adb shell am start -n com.oxidelabmobile/.MainActivity

# Start debugging (from project root)
%ANDROID_NDK_HOME%\ndk-gdb --start --verbose
```

## Debugging Rust JNI Functions

### Set Breakpoints

```gdb
# Break on Rust functions
break Java_com_oxidelabmobile_RustInterface_downloadQwenModelTo
break Java_com_oxidelabmobile_RustInterface_downloadQwenModel

# Break on internal Rust functions
break oxide_lab_mobile::jni_bridge::safe_download_model_to_path
break oxide_lab_mobile::chatbot::ChatBot::download_qwen3_06b_gguf
```

### Useful Commands

```gdb
# Continue execution
continue

# Step into function
step

# Step over function
next

# Print variables
print cache_dir_str
print result

# Show call stack
backtrace

# List source code
list

# Show registers
info registers

# Disassemble function
disassemble function_name
```

## Troubleshooting

### Common Issues

1. **"No symbol table loaded"**

   - Make sure you built with debug symbols
   - Check that `android:debuggable="true"` in AndroidManifest.xml

2. **"Cannot find gdb"**

   - Ensure NDK is properly installed
   - Check ANDROID_NDK_HOME environment variable

3. **"No process found"**

   - Make sure app is running
   - Check device connection with `adb devices`

4. **"Symbol not found"**
   - Rust symbols might be mangled
   - Try breaking on JNI function names instead

### Symbol Demangling

Rust symbols are mangled. To see demangled names:

```gdb
set print asm-demangle on
set print demangle on
```

### Finding Rust Functions

List all functions:

```gdb
info functions
```

Search for specific function:

```gdb
info functions download
```

## Advanced Debugging

### Memory Inspection

```gdb
# Examine memory at address
x/10x 0xaddress

# Examine string
x/s 0xaddress
```

### Conditional Breakpoints

```gdb
break function_name if condition
```

### Watchpoints

```gdb
watch variable_name
```

## Debugging Tips

1. **Start with JNI functions** - These are easier to find and break on
2. **Use logcat** - Check Android logs while debugging
3. **Set breakpoints early** - Before calling native functions
4. **Use step/next carefully** - JNI calls can be complex
5. **Check return values** - Verify JNI string conversions

## Example Debugging Session

```gdb
(gdb) break Java_com_oxidelabmobile_RustInterface_downloadQwenModelTo
Breakpoint 1 at 0x...
(gdb) continue
Continuing.

Breakpoint 1, Java_com_oxidelabmobile_RustInterface_downloadQwenModelTo
    (env=0x..., _class=0x..., cache_dir=0x...) at jni_bridge.rs:45
45      init_logging();
(gdb) step
(gdb) print cache_dir_str
$1 = "/data/data/com.oxidelabmobile/cache"
(gdb) continue
```
