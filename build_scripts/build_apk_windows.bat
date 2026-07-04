# =========================================================================
# WhatsApp Automator - Windows Build Script (Easy Mode)
# =========================================================================
# Double-click করলেই APK build হবে।
#
# প্রথমবার চালানোর আগে নিশ্চিত করুন যে:
#   1. JDK 17 install করা আছে
#   2. Android SDK Command-line Tools install করা আছে
#   3. ANDROID_HOME environment variable set করা আছে
#
# বিস্তারিত: VSCODE_BUILD_GUIDE.md ফাইলটি পড়ুন
# =========================================================================

@echo off
chcp 65001 >nul
title WhatsApp Automator - APK Builder
color 0A

echo.
echo ============================================================
echo   WhatsApp Automator - APK Builder (Windows)
echo ============================================================
echo.

cd /d "%~dp0\.."

echo [1/4] Checking Java...
echo ----------------------------------------
java -version 2>nul
if errorlevel 1 (
    echo.
    echo ❌ ERROR: Java/JDK not found!
    echo.
    echo Please install JDK 17 first:
    echo   https://adoptium.net/temurin/releases/?version=17
    echo.
    echo Then re-run this script.
    echo.
    pause
    exit /b 1
)

echo.
echo [2/4] Checking Android SDK...
echo ----------------------------------------
if "%ANDROID_HOME%"=="" (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
    ) else if exist "C:\Android\Sdk" (
        set "ANDROID_HOME=C:\Android\Sdk"
    )
)

if "%ANDROID_HOME%"=="" (
    echo.
    echo ❌ ERROR: Android SDK not found!
    echo.
    echo Please install Android SDK Command-line Tools.
    echo See VSCODE_BUILD_GUIDE.md for instructions.
    echo.
    pause
    exit /b 1
)

echo Android SDK found: %ANDROID_HOME%
echo.

echo [3/4] Building APK (this may take 5-15 minutes on first run)...
echo ----------------------------------------
echo Gradle will download required dependencies. Please be patient.
echo.

call gradlew.bat assembleDebug

if errorlevel 1 (
    echo.
echo ============================================================
echo   ❌ BUILD FAILED
echo ============================================================
echo.
echo Common fixes:
echo   1. Check internet connection
echo   2. Make sure JDK 17 (not 8, not 18+) is installed
echo   3. Make sure ANDROID_HOME is set correctly
echo   4. Try: gradlew.bat clean
echo      Then: gradlew.bat assembleDebug
echo.
    pause
    exit /b 1
)

echo.
echo [4/4] Build Successful! 🎉
echo ----------------------------------------
echo.
echo Your APK is ready at:
echo.
echo   app\build\outputs\apk\debug\app-debug.apk
echo.
echo Copy this file to your Android phone and install it.
echo.
echo Note: You may need to enable "Install from unknown sources"
echo in your phone's Settings.
echo.

explorer "app\build\outputs\apk\debug\"

pause
