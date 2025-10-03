@echo off
REM Manual native debugging setup for Oxide Lab Mobile
REM This script helps you set up ndk-gdb debugging manually

echo Manual Native Debugging Setup
echo =============================
echo.

REM Set paths
set ANDROID_SDK_ROOT=C:\Users\PC\AppData\Local\Android\Sdk
set ANDROID_NDK_HOME=%ANDROID_SDK_ROOT%\ndk\27.0.12077973

echo Android SDK: %ANDROID_SDK_ROOT%
echo Android NDK: %ANDROID_NDK_HOME%
echo.

REM Check if NDK exists
if not exist "%ANDROID_NDK_HOME%" (
    echo ERROR: NDK not found at %ANDROID_NDK_HOME%
    echo Available NDK versions:
    dir "%ANDROID_SDK_ROOT%\ndk"
    pause
    exit /b 1
)

REM Check if device is connected
echo Checking connected devices...
adb devices
echo.

REM Check if APK exists
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ERROR: Debug APK not found!
    echo Please build it first using Android Studio or:
    echo .\gradlew assembleDebug
    echo.
    echo For now, let's try to build it...
    .\gradlew assembleDebug --no-daemon
    if %ERRORLEVEL% neq 0 (
        echo Build failed. Please build manually in Android Studio.
        pause
        exit /b 1
    )
)

echo Installing debug APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to install APK
    pause
    exit /b 1
)

echo.
echo Starting application...
adb shell am start -n com.oxidelabmobile/.MainActivity

echo.
echo Waiting for app to start...
timeout /t 3 /nobreak > nul

echo.
echo Starting ndk-gdb...
echo.
echo IMPORTANT DEBUGGING COMMANDS:
echo =============================
echo break Java_com_oxidelabmobile_RustInterface_downloadQwenModelTo
echo break Java_com_oxidelabmobile_RustInterface_downloadQwenModel
echo continue
echo step
echo next
echo print variable_name
echo backtrace
echo quit
echo.
echo Press any key to start debugging...
pause

REM Start ndk-gdb
"%ANDROID_NDK_HOME%\ndk-gdb" --start --verbose

pause
