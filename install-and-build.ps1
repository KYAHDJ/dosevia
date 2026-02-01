#!/usr/bin/env pwsh
# Dosevia App - Complete Installation and Build Script for Windows
# This script will install all dependencies and build the app

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Dosevia App - Installation & Build" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Check if Node.js is installed
Write-Host "Checking Node.js installation..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version
    Write-Host "✓ Node.js version: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Node.js is not installed!" -ForegroundColor Red
    Write-Host "Please install Node.js from https://nodejs.org/" -ForegroundColor Red
    exit 1
}

# Check if npm is installed
Write-Host "Checking npm installation..." -ForegroundColor Yellow
try {
    $npmVersion = npm --version
    Write-Host "✓ npm version: $npmVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ npm is not installed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Step 1: Cleaning old dependencies" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

# Remove old node_modules and lock files
if (Test-Path "node_modules") {
    Write-Host "Removing old node_modules..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force "node_modules" -ErrorAction SilentlyContinue
    Write-Host "✓ Removed old node_modules" -ForegroundColor Green
}

if (Test-Path "package-lock.json") {
    Write-Host "Removing old package-lock.json..." -ForegroundColor Yellow
    Remove-Item -Force "package-lock.json" -ErrorAction SilentlyContinue
    Write-Host "✓ Removed old package-lock.json" -ForegroundColor Green
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Step 2: Installing dependencies" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "This may take several minutes..." -ForegroundColor Yellow
Write-Host ""

# Install dependencies with legacy peer deps flag to avoid conflicts
npm install --legacy-peer-deps

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "✗ Failed to install dependencies!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✓ All dependencies installed successfully!" -ForegroundColor Green

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Step 3: Building the application" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Build the application
npm run build

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "✗ Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✓ Build completed successfully!" -ForegroundColor Green

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Step 4: Syncing with Capacitor" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Sync with Capacitor Android
npx cap sync android

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "✗ Capacitor sync failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✓ Capacitor sync completed successfully!" -ForegroundColor Green

Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "SUCCESS! Installation Complete" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. To run on Android device/emulator:" -ForegroundColor White
Write-Host "     npx cap run android" -ForegroundColor Cyan
Write-Host ""
Write-Host "  2. To open in Android Studio:" -ForegroundColor White
Write-Host "     npx cap open android" -ForegroundColor Cyan
Write-Host ""
Write-Host "  3. To run development server:" -ForegroundColor White
Write-Host "     npm run dev" -ForegroundColor Cyan
Write-Host ""
Write-Host "  4. To rebuild after changes:" -ForegroundColor White
Write-Host "     npm run build && npx cap sync android" -ForegroundColor Cyan
Write-Host ""
