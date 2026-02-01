@echo off
echo ========================================
echo Dosevia - Clean Build Script
echo ========================================
echo.

echo Step 1: Cleaning Android build...
cd android
call gradlew clean
cd ..
echo.

echo Step 2: Building web assets...
call npm run build
echo.

echo Step 3: Syncing with Capacitor...
call npx cap sync android
echo.

echo Step 4: Running on device...
call npx cap run android
echo.

echo ========================================
echo Build Complete!
echo ========================================
pause
