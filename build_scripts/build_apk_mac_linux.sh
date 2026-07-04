#!/bin/bash
# =========================================================================
# WhatsApp Automator - Mac/Linux Build Script (Easy Mode)
# =========================================================================
# Terminal এ এই ফাইলের লোকেশনে যান, তারপর চালান:
#   chmod +x build_apk_mac_linux.sh
#   ./build_apk_mac_linux.sh
# =========================================================================

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo ""
echo "============================================================"
echo "  WhatsApp Automator - APK Builder (Mac/Linux)"
echo "============================================================"
echo ""

# Go to project root (parent of build_scripts folder)
cd "$(dirname "$0")/.."

echo -e "${YELLOW}[1/4]${NC} Checking Java..."
echo "----------------------------------------"
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ ERROR: Java/JDK not found!${NC}"
    echo ""
    echo "Please install JDK 17 first:"
    echo "  Mac:   brew install openjdk@17"
    echo "  Linux: sudo apt install openjdk-17-jdk"
    echo ""
    exit 1
fi
java -version

echo ""
echo -e "${YELLOW}[2/4]${NC} Checking Android SDK..."
echo "----------------------------------------"

if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
    elif [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/usr/local/android-sdk" ]; then
        export ANDROID_HOME="/usr/local/android-sdk"
    fi
fi

if [ -z "$ANDROID_HOME" ]; then
    echo -e "${RED}❌ ERROR: Android SDK not found!${NC}"
    echo ""
    echo "Please install Android SDK Command-line Tools."
    echo "See VSCODE_BUILD_GUIDE.md for instructions."
    echo ""
    exit 1
fi

echo "Android SDK found: $ANDROID_HOME"
echo ""

echo -e "${YELLOW}[3/4]${NC} Building APK (5-15 minutes on first run)..."
echo "----------------------------------------"
echo "Gradle will download required dependencies. Please be patient."
echo ""

chmod +x gradlew
./gradlew assembleDebug

echo ""
echo -e "${GREEN}[4/4] Build Successful! 🎉${NC}"
echo "----------------------------------------"
echo ""
echo "Your APK is ready at:"
echo ""
echo "  app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "Copy this file to your Android phone and install it."
echo ""

# Try to open the folder
if [ "$(uname)" == "Darwin" ]; then
    open "app/build/outputs/apk/debug/" 2>/dev/null || true
elif [ "$(uname)" == "Linux" ]; then
    xdg-open "app/build/outputs/apk/debug/" 2>/dev/null || true
fi
