# 🎯 Dosevia Widget Fix - Implementation Guide

## 📋 Overview

This guide will help you remove all existing widgets and implement a single, clean **Pill Count Widget** that shows "Pills Taken Today: X / Y" with perfect real-time synchronization using SharedPreferences.

---

## ⚠️ CRITICAL PRINCIPLE

**The widget NEVER calculates or stores data. It ONLY reads from SharedPreferences.**

```
User Action → App Updates SharedPreferences → Widget Refreshes → Numbers Match 100%
```

---

## 🗑️ STEP 1: Remove Old Widgets

### 1.1 Delete Old Widget Files

Delete these Kotlin files:
```bash
android/app/src/main/java/com/dosevia/app/DoseviaSmallWidget.kt
android/app/src/main/java/com/dosevia/app/DoseviaMediumWidget.kt
android/app/src/main/java/com/dosevia/app/DoseviaLargeWidget.kt
```

### 1.2 Delete Old Layout Files

Delete these XML files:
```bash
android/app/src/main/res/layout/widget_small.xml
android/app/src/main/res/layout/widget_medium.xml
android/app/src/main/res/layout/widget_large.xml
```

### 1.3 Delete Old Widget Info Files

Delete these XML files:
```bash
android/app/src/main/res/xml/widget_info_small.xml
android/app/src/main/res/xml/widget_info_medium.xml
android/app/src/main/res/xml/widget_info_large.xml
```

### 1.4 Delete Old Drawables (Optional)

You can keep or delete these - they were specific to old widgets:
```bash
android/app/src/main/res/drawable/widget_small_background.xml
android/app/src/main/res/drawable/widget_medium_background.xml
android/app/src/main/res/drawable/widget_large_background.xml
```

---

## ✅ STEP 2: Add New Widget

### 2.1 Copy New Widget Provider

**File:** `DoseviaPillCountWidget.kt`

Copy the file I created to:
```bash
android/app/src/main/java/com/dosevia/app/DoseviaPillCountWidget.kt
```

### 2.2 Copy New Widget Layout

**File:** `widget_pill_count.xml`

Copy to:
```bash
android/app/src/main/res/layout/widget_pill_count.xml
```

### 2.3 Copy Widget Background

**File:** `widget_background.xml`

Copy to:
```bash
android/app/src/main/res/drawable/widget_background.xml
```

### 2.4 Copy Widget Info

**File:** `widget_info_pill_count.xml`

Copy to:
```bash
android/app/src/main/res/xml/widget_info_pill_count.xml
```

---

## 🔄 STEP 3: Replace Plugin Files

### 3.1 Replace WidgetSyncPlugin

**Backup the old one first:**
```bash
mv android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin.kt \
   android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin.kt.OLD
```

**Then copy the new one:**
```bash
# Rename WidgetSyncPlugin_NEW.kt to WidgetSyncPlugin.kt
```

### 3.2 Replace TypeScript Interface

**Backup the old one:**
```bash
mv src/app/lib/widgetSync.ts src/app/lib/widgetSync.ts.OLD
```

**Copy the new one:**
```bash
# Rename widgetSync_NEW.ts to widgetSync.ts
```

---

## 📱 STEP 4: Update AndroidManifest.xml

### 4.1 Remove Old Widget Declarations

In `android/app/src/main/AndroidManifest.xml`, **DELETE** these sections:

```xml
<!-- DELETE THIS -->
<receiver
    android:name=".DoseviaSmallWidget"
    android:exported="true">
    ...
</receiver>

<!-- DELETE THIS -->
<receiver
    android:name=".DoseviaMediumWidget"
    android:exported="true">
    ...
</receiver>

<!-- DELETE THIS -->
<receiver
    android:name=".DoseviaLargeWidget"
    android:exported="true">
    ...
</receiver>
```

### 4.2 Add New Widget Declaration

**ADD** this in the same location (after alarm receivers):

```xml
<!-- ============================================== -->
<!-- WIDGET: Pill Count Widget (Pills Taken Today) -->
<!-- ============================================== -->
<receiver
    android:name=".DoseviaPillCountWidget"
    android:exported="true"
    android:label="Pill Count">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        <action android:name="com.dosevia.app.UPDATE_WIDGET" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/widget_info_pill_count" />
</receiver>
```

---

## 📝 STEP 5: Update strings.xml

Add the widget description to `android/app/src/main/res/values/strings.xml`:

```xml
<string name="widget_description">Shows pills taken today</string>
```

---

## 🎨 STEP 6: Add Widget Preview (Optional)

Create a preview image for the widget picker:

1. Take a screenshot of the widget or design one
2. Save it as `widget_preview.png`
3. Place it in: `android/app/src/main/res/drawable/widget_preview.png`

---

## 💻 STEP 7: Update Your App Code

### 7.1 Import the New Functions

In your pill management component (e.g., `HomeScreen.tsx`):

```typescript
import { 
  updateWidgetPillCount,
  resetDailyPillCount 
} from '@/app/lib/widgetSync';
```

### 7.2 Call Widget Update When Pills Change

Whenever you mark a pill as taken/untaken:

```typescript
// Example: When user marks pill as taken
const handlePillTaken = async (pillId: string) => {
  // Update your app state
  const updatedPills = markPillAsTaken(pillId);
  
  // Calculate counts
  const pillsTakenToday = updatedPills.filter(p => 
    p.status === 'taken' && isToday(p.date)
  ).length;
  
  const totalPillsToday = updatedPills.filter(p => 
    isToday(p.date)
  ).length;
  
  // ✅ CRITICAL: Update widget immediately
  await updateWidgetPillCount(pillsTakenToday, totalPillsToday);
};
```

### 7.3 Call Reset at Midnight

In your app's initialization or timer logic:

```typescript
// Reset widget at midnight
const resetAtMidnight = async () => {
  const now = new Date();
  const midnight = new Date(now);
  midnight.setHours(24, 0, 0, 0);
  
  const msUntilMidnight = midnight.getTime() - now.getTime();
  
  setTimeout(async () => {
    await resetDailyPillCount();
    // Schedule next reset
    resetAtMidnight();
  }, msUntilMidnight);
};

// Call on app launch
useEffect(() => {
  resetAtMidnight();
}, []);
```

---

## 🔧 STEP 8: Build & Test

### 8.1 Clean Build

```bash
cd android
./gradlew clean
```

### 8.2 Sync & Build

```bash
npx cap sync android
npx cap build android
```

### 8.3 Run on Device

```bash
npx cap run android
```

---

## ✅ STEP 9: Test the Widget

### 9.1 Add Widget to Home Screen

1. Long-press on home screen
2. Select "Widgets"
3. Find "Dosevia" → "Pill Count"
4. Drag to home screen

### 9.2 Test Sync

1. Open the app
2. Mark a pill as taken
3. Press home button immediately
4. Widget should update within 1 second

### 9.3 Test Daily Reset

1. Change device time to 11:59 PM
2. Wait for midnight
3. Widget should reset to 0 / X

---

## 🐛 Troubleshooting

### Widget Not Updating?

**Check SharedPreferences:**
```kotlin
// Add this to WidgetSyncPlugin.kt temporarily
Log.d("DEBUG", "Saved to SharedPreferences: $pillsTaken / $totalPills")
```

**Check widget receives update:**
```kotlin
// Add this to DoseviaPillCountWidget.kt
Log.d("DEBUG", "Widget updated: $pillsTaken / $totalPills")
```

### Widget Shows Wrong Numbers?

Make sure you're calculating counts correctly:
```typescript
// Must be TODAY's pills only
const pillsToday = allPills.filter(p => {
  const pillDate = new Date(p.date);
  const today = new Date();
  return pillDate.toDateString() === today.toDateString();
});
```

### Widget Not Appearing in Picker?

1. Check AndroidManifest.xml has the receiver
2. Check widget_info_pill_count.xml exists
3. Rebuild: `npx cap sync android`

---

## 📊 Expected Behavior

### ✅ Instant Updates
- Mark pill → Widget updates < 1 second
- No delay, no manual refresh needed

### ✅ Perfect Sync
- App shows: 5 / 28 pills
- Widget shows: 5 / 28 pills
- Always match 100%

### ✅ Reliable Storage
- Close app → Reopen → Numbers persist
- Reboot device → Numbers persist

### ✅ Daily Reset
- At midnight → Resets to 0 / X automatically

---

## 🎯 Final Checklist

- [ ] Old widget files deleted
- [ ] New widget files added
- [ ] AndroidManifest.xml updated
- [ ] strings.xml updated
- [ ] App code calls `updateWidgetPillCount()`
- [ ] Daily reset implemented
- [ ] App builds without errors
- [ ] Widget appears in picker
- [ ] Widget updates instantly
- [ ] Numbers match perfectly
- [ ] Widget opens app when tapped

---

## 📚 Key Files Summary

### Kotlin Files
- `DoseviaPillCountWidget.kt` - Widget provider (reads SharedPreferences)
- `WidgetSyncPlugin.kt` - Capacitor plugin (writes SharedPreferences)

### Layout Files
- `widget_pill_count.xml` - Widget UI layout
- `widget_background.xml` - Widget background style

### Config Files
- `widget_info_pill_count.xml` - Widget configuration
- `AndroidManifest.xml` - Widget registration

### TypeScript Files
- `widgetSync.ts` - TypeScript interface

---

## 🚀 Next Steps

After implementing this first widget successfully:

1. **Test thoroughly** - Make sure it works perfectly
2. **Add more features** - Could add streak count, progress bar, etc.
3. **Add more widgets** - Could add calendar widget, stats widget, etc.

But do ONE at a time, ensuring each one works perfectly with SharedPreferences!

---

## 💡 Pro Tips

1. **Always update SharedPreferences FIRST, then trigger widget update**
2. **Use atomic writes with .apply()**
3. **Keep widget lightweight - only read & display**
4. **Test on real device, not just emulator**
5. **Use Logcat to debug sync issues**

---

Good luck! 🎉
