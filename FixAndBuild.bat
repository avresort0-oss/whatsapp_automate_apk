@echo off
title Android Auto-Builder
cls

echo [1/3] Cleaning Cache...
rd /s /q "app\build"
rd /s /q ".gradle"

echo [2/3] Setting Up Memory Limit...
set GRADLE_OPTS=-Xmx1024m -XX:MaxMetaspaceSize=512m

echo [3/3] Building APK...
call gradlew assembleDebug --no-daemon --parallel --offline

echo.
echo ===========================================
echo  SUCCESS! APK is ready in app\build\outputs\apk\debug\
echo ===========================================
pause