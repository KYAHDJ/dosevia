# Dosevia - Terminal Commands Reference

This guide provides all the terminal commands you need to build and run Dosevia. No batch files or scripts required!

---

## Prerequisites Setup

### 1. Install Node.js
```bash
# Check if Node.js is installed
node --version
npm --version

# Should show version 18 or higher
```

### 2. Install Dependencies
```bash
# Navigate to project directory
cd dosevia-improved

# Install all dependencies
npm install

# Or if using pnpm:
pnpm install
```

---

## Development Commands

### Start Development Server
```bash
# Run dev server (for browser testing)
npm run dev

# Access at: http://localhost:5173
```

### Build for Production
```bash
# Create production build
npm run build

# Output will be in: dist/
```

---

## Capacitor Commands

### Sync with Android
```bash
# Copy web assets to Android project
npx cap sync android

# Run this after each build
```

### Open Android Studio
```bash
# Open the Android project in Android Studio
npx cap open android

# Then click "Run" button in Android Studio
```

### Run on Device
```bash
# Build and run on connected device
npx cap run android

# Make sure device is connected via USB
```

---

## Android Build Commands

### Debug Build
```bash
# Navigate to android directory
cd android

# Create debug APK
./gradlew assembleDebug

# APK will be at: android/app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
# Create release APK (requires signing config)
cd android
./gradlew assembleRelease

# APK will be at: android/app/build/outputs/apk/release/app-release.apk
```

### Bundle for Play Store
```bash
# Create Android App Bundle
cd android
./gradlew bundleRelease

# AAB will be at: android/app/build/outputs/bundle/release/app-release.aab
```

---

## Testing Commands

### Check Device Connection
```bash
# List connected devices
adb devices

# Should show your device
```

### View Logs
```bash
# View Android logs
adb logcat

# Filter for app logs
adb logcat | grep Dosevia

# Clear logs
adb logcat -c
```

### Install APK
```bash
# Install APK on connected device
adb install path/to/app-debug.apk

# Force reinstall
adb install -r path/to/app-debug.apk
```

---

## Cleaning Commands

### Clean Node Modules
```bash
# Remove node_modules
rm -rf node_modules

# Remove package-lock
rm package-lock.json

# Reinstall
npm install
```

### Clean Android Build
```bash
# Navigate to android directory
cd android

# Clean build cache
./gradlew clean

# Or delete build directories
rm -rf build app/build
```

### Full Clean
```bash
# Remove all build artifacts
rm -rf node_modules dist android/build android/app/build

# Reinstall and rebuild
npm install
npm run build
npx cap sync android
```

---

## Git Commands

### Initialize Repository
```bash
# Initialize git (if not already)
git init

# Add all files
git add .

# Commit
git commit -m "Initial commit with improvements"
```

### Check Status
```bash
# View changed files
git status

# View changes
git diff
```

---

## Common Workflows

### 1. First Time Setup
```bash
npm install
npm run build
npx cap sync android
npx cap open android
# Click "Run" in Android Studio
```

### 2. After Code Changes
```bash
npm run build
npx cap sync android
# Rebuild in Android Studio
```

### 3. Quick Dev Cycle
```bash
# For web testing only
npm run dev
# View at http://localhost:5173
```

### 4. Create Release APK
```bash
npm run build
npx cap sync android
cd android
./gradlew assembleRelease
cd ..
```

---

## Troubleshooting Commands

### Check Capacitor Status
```bash
npx cap doctor

# Shows configuration and issues
```

### Update Capacitor
```bash
# Update all Capacitor packages
npm install @capacitor/core@latest @capacitor/cli@latest @capacitor/android@latest

# Sync after update
npx cap sync android
```

### Verify Build Configuration
```bash
# Check package.json scripts
cat package.json | grep scripts -A 5

# Check Capacitor config
cat capacitor.config.json

# Check Android gradle config
cat android/app/build.gradle
```

### Permission Check
```bash
# List all permissions
adb shell dumpsys package com.dosevia.app | grep permission
```

---

## Performance & Optimization

### Bundle Size Analysis
```bash
# Install bundle analyzer
npm install --save-dev rollup-plugin-visualizer

# Build and analyze
npm run build

# View report
open dist/stats.html
```

### Dependency Audit
```bash
# Check for security issues
npm audit

# Fix automatically
npm audit fix
```

---

## Environment Setup

### Android SDK Path
```bash
# Set ANDROID_HOME (if not set)
export ANDROID_HOME=$HOME/Android/Sdk

# Add to PATH
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/tools
```

### Java Version
```bash
# Check Java version
java -version

# Should be Java 17 or higher
```

---

## Quick Reference Table

| Task | Command |
|------|---------|
| Install | `npm install` |
| Dev Server | `npm run dev` |
| Build | `npm run build` |
| Sync Android | `npx cap sync android` |
| Open Android Studio | `npx cap open android` |
| Run on Device | `npx cap run android` |
| Debug APK | `cd android && ./gradlew assembleDebug` |
| Release APK | `cd android && ./gradlew assembleRelease` |
| View Logs | `adb logcat` |
| Clean | `./gradlew clean` |

---

## Tips

1. **Always sync after building**: `npm run build && npx cap sync android`
2. **Use Android Studio for device testing**: More reliable than CLI
3. **Check logs for issues**: `adb logcat` is your friend
4. **Clean when stuck**: `./gradlew clean` fixes many issues
5. **Update regularly**: Keep Capacitor and dependencies current

---

## Help & Support

### Capacitor Documentation
- Official Docs: https://capacitorjs.com
- Android Guide: https://capacitorjs.com/docs/android

### Common Issues
- **Build failed**: Run `./gradlew clean` then rebuild
- **Device not found**: Check `adb devices`
- **Permission denied**: Check Android Studio SDK settings
- **Sync failed**: Delete `android/.gradle` and try again

---

**Last Updated**: February 2, 2026
**Version**: 2.0

*All commands tested on macOS/Linux. Windows users may need to adjust path separators.*
