@echo off
REM Dosevia App - Complete Installation and Build Script for Windows
REM This script will install all dependencies and build the app

color 0B
echo =====================================
echo Dosevia App - Installation ^& Build
echo =====================================
echo.

REM Check if Node.js is installed
echo Checking Node.js installation...
where node >nul 2>nul
if %errorlevel% neq 0 (
    color 0C
    echo X Node.js is not installed!
    echo Please install Node.js from https://nodejs.org/
    pause
    exit /b 1
)

for /f "delims=" %%i in ('node --version') do set NODE_VERSION=%%i
echo [OK] Node.js version: %NODE_VERSION%

REM Check if npm is installed
echo Checking npm installation...
where npm >nul 2>nul
if %errorlevel% neq 0 (
    color 0C
    echo X npm is not installed!
    pause
    exit /b 1
)

for /f "delims=" %%i in ('npm --version') do set NPM_VERSION=%%i
echo [OK] npm version: %NPM_VERSION%

echo.
echo =====================================
echo Step 1: Cleaning old dependencies
echo =====================================

REM Remove old node_modules and lock files
if exist "node_modules" (
    echo Removing old node_modules...
    rmdir /s /q node_modules 2>nul
    echo [OK] Removed old node_modules
)

if exist "package-lock.json" (
    echo Removing old package-lock.json...
    del /f /q package-lock.json 2>nul
    echo [OK] Removed old package-lock.json
)

echo.
echo =====================================
echo Step 2: Installing dependencies
echo =====================================
echo This may take several minutes...
echo.

REM Install dependencies with legacy peer deps flag
call npm install --legacy-peer-deps

if %errorlevel% neq 0 (
    color 0C
    echo.
    echo X Failed to install dependencies!
    pause
    exit /b 1
)

echo.
echo [OK] All dependencies installed successfully!

echo.
echo =====================================
echo Step 3: Building the application
echo =====================================
echo.

REM Build the application
call npm run build

if %errorlevel% neq 0 (
    color 0C
    echo.
    echo X Build failed!
    pause
    exit /b 1
)

echo.
echo [OK] Build completed successfully!

echo.
echo =====================================
echo Step 4: Syncing with Capacitor
echo =====================================
echo.

REM Sync with Capacitor Android
call npx cap sync android

if %errorlevel% neq 0 (
    color 0C
    echo.
    echo X Capacitor sync failed!
    pause
    exit /b 1
)

echo.
echo [OK] Capacitor sync completed successfully!

color 0A
echo.
echo =====================================
echo SUCCESS! Installation Complete
echo =====================================
echo.
echo Next Steps:
echo   1. To run on Android device/emulator:
echo      npx cap run android
echo.
echo   2. To open in Android Studio:
echo      npx cap open android
echo.
echo   3. To run development server:
echo      npm run dev
echo.
echo   4. To rebuild after changes:
echo      npm run build ^&^& npx cap sync android
echo.
pause
