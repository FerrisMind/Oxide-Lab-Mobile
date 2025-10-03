@echo off
REM Native debugging script for Oxide Lab Mobile
REM This script sets up ndk-gdb debugging for the Rust JNI code

echo Setting up native debugging for Oxide Lab Mobile...
echo.

REM Check if Android SDK is set
if "%ANDROID_SDK_ROOT%"=="" (
    echo ERROR: ANDROID_SDK_ROOT environment variable is not set
    echo Please set it to your Android SDK path, e.g.:
    echo set ANDROID_SDK_ROOT=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
    pause
    exit /b 1
)

REM Set Android SDK and NDK paths
set ANDROID_SDK_ROOT=C:\Users\PC\AppData\Local\Android\Sdk
set ANDROID_NDK_HOME=%ANDROID_SDK_ROOT%\ndk\27.0.12077973

REM Check if NDK exists
if not exist "%ANDROID_NDK_HOME%" (
    echo ERROR: Android NDK not found at: %ANDROID_NDK_HOME%
    echo Available NDK versions:
    dir "%ANDROID_SDK_ROOT%\ndk"
    pause
    exit /b 1
)

echo Using Android SDK: %ANDROID_SDK_ROOT%
echo Using Android NDK: %ANDROID_NDK_HOME%
echo.

REM Check if device is connected
echo Checking for connected devices...
adb devices
echo.

REM Install debug APK
echo Installing debug APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to install APK. Make sure to build it first with:
    echo .\gradlew assembleDebug
    pause
    exit /b 1
)

echo.
echo Starting application...
adb shell am start -n com.oxidelabmobile/.MainActivity

echo.
echo Waiting for application to start...
timeout /t 3 /nobreak > nul

echo.
echo Starting ndk-gdb debugging session...
echo You can now set breakpoints and debug the native code.
echo.
echo Common gdb commands:
echo   break function_name    - Set breakpoint
echo   continue              - Continue execution
echo   step                  - Step into function
echo   next                  - Step over function
echo   print variable        - Print variable value
echo   backtrace             - Show call stack
echo   quit                  - Exit debugger
echo.

REM Start ndk-gdb
"%ANDROID_NDK_HOME%\ndk-gdb" --start --verbose

pause
