# 📦 INSTALLATION INSTRUCTIONS

## REQUIRED: Install File Picker Plugin

The new audio picker feature requires the Capacitor File Picker plugin.

### Step 1: Install the package

```bash
npm install @capawesome/capacitor-file-picker
```

### Step 2: Add to package.json dependencies

Add this line to the `dependencies` section of `package.json`:

```json
"@capawesome/capacitor-file-picker": "^6.0.1"
```

### Step 3: Sync to Android

```bash
npx cap sync android
```

### Step 4: Add Permissions

Edit `android/app/src/main/AndroidManifest.xml` and add these permissions inside the `<manifest>` tag:

```xml
<!-- For reading audio files -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" android:maxSdkVersion="32"/>
```

### Step 5: Build and Run

```bash
npm run build
npx cap sync android
npx cap run android
```

---

## ✅ VERIFICATION

After installation, check that the plugin is installed:

```bash
npm list @capawesome/capacitor-file-picker
```

Should output:
```
@capawesome/capacitor-file-picker@6.0.1
```

---

## 🚀 READY TO USE

Once installed:
1. Open app → Settings
2. Tap "Notification Sound"
3. Tap "Browse"
4. File picker should open!

---

## ⚠️ TROUBLESHOOTING

### "Module not found: @capawesome/capacitor-file-picker"

**Solution:** Run `npm install @capawesome/capacitor-file-picker` then `npx cap sync android`

### File picker doesn't open

**Solution:** 
1. Check permissions in AndroidManifest.xml
2. On device: Settings → Apps → Dosevia → Permissions → Files: Allow
3. Rebuild app

### "Permission denied" when selecting file

**Solution:** Grant storage/media permissions to app in phone settings
