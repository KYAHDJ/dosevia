# Full-Screen Alarm System + 30-Minute Warning Update

## 🎯 What's New

### 1. **Full-Screen Alarm UI** 🚨
When it's time to take your medication:
- **Full-screen alarm activity** appears (even over lock screen)
- **Continuous alarm sound** plays (looping until dismissed)
- **Continuous vibration** pattern
- **User MUST tap "TAKE PILL" button** to dismiss
- **Opens the app** after dismissal
- **Cannot be dismissed** with back button

### 2. **30-Minute Warning Notification** ⏰
30 minutes before medication time:
- **Gentle notification** appears
- **Single vibration** pattern
- **Auto-dismisses** after user reads it
- **Prepares user** for upcoming alarm

### 3. **Repeating Full-Screen Alarms** 🔁
If user doesn't take the pill:
- **Alarm repeats every 30 seconds** initially
- **Decreases to 10 seconds** gradually
- **Each repeat** shows full-screen alarm again
- **Continuous until** user taps TAKE PILL

## 🔄 System Flow

```
30 minutes before
    ↓
📱 Warning Notification
"You have 30 minutes..."
    ↓
[User can dismiss or ignore]
    ↓
At scheduled time
    ↓
🚨 FULL-SCREEN ALARM
    ↓
📢 Continuous alarm sound
💥 Continuous vibration
    ↓
[User ignores it]
    ↓
Wait 30 seconds
    ↓
🚨 FULL-SCREEN ALARM AGAIN
    ↓
[User ignores again]
    ↓
Wait 28 seconds
    ↓
🚨 FULL-SCREEN ALARM AGAIN
    ↓
... continues decreasing to 10s minimum
    ↓
[User taps TAKE PILL]
    ↓
✅ Alarm stops
✅ App opens
✅ User marks pill taken
```

## 📱 Full-Screen Alarm UI

### Design
- **Background:** Bright red (#FF1744)
- **Icon:** App launcher icon (centered)
- **Title:** "💊 MEDICATION TIME!"
- **Message:** "It's time to take your pill"
- **Instruction:** "Tap the button below to confirm and stop the alarm"
- **Button:** Large white "TAKE PILL" button

### Features
- **Shows over lock screen** - No need to unlock
- **Turns screen on** - Wakes up device
- **Cannot dismiss with back** - Forces user to tap button
- **Full screen** - Takes entire screen
- **Persistent** - Stays until dismissed

## 🔔 Notification Types

### 1. Warning Notification (30 min before)
```
Title: ⏰ Medication Reminder - 30 Minutes
Body: You have 30 minutes until it's time to take your medication!
Sound: Default notification sound
Vibration: Short pattern (500ms vibrate, 250ms pause, 500ms vibrate)
Priority: High
Auto-dismiss: Yes
```

### 2. Alarm Notification (at scheduled time)
```
Title: 🚨 MEDICATION TIME!
Body: Time to take your medication NOW!
Sound: Alarm sound (continuous via service)
Vibration: Continuous (1s vibrate, 0.5s pause, repeat)
Priority: Maximum
Full-screen intent: Yes (opens AlarmActivity)
Ongoing: Yes (cannot swipe away)
Action: "TAKE PILL" button
```

## 🛠️ Technical Implementation

### New/Updated Files

1. **AlarmReceiver.java**
   - Handles both warning and alarm triggers
   - Schedules 30-minute warning
   - Launches full-screen AlarmActivity
   - Starts AlarmService for continuous sound
   - Manages repeating alarms

2. **AlarmService.java**
   - Plays continuous alarm sound (looping)
   - Continuous vibration pattern
   - Runs as foreground service
   - Sets alarm volume to maximum
   - Stops when dismissed

3. **AlarmActivity.java**
   - Full-screen alarm UI
   - Shows over lock screen
   - Turns screen on
   - Prevents back button dismiss
   - Stops alarm service on dismiss
   - Opens main app

4. **activity_alarm.xml**
   - Beautiful red alarm screen design
   - Large pill icon
   - Clear messaging
   - Prominent TAKE PILL button

5. **AlarmDismissReceiver.java**
   - Stops alarm service
   - Cancels all pending alarms
   - Dismisses all notifications
   - Resets repeat count

6. **AlarmPlugin.java**
   - Automatically schedules 30-minute warning
   - Schedules main alarm
   - Called from JavaScript/TypeScript

7. **MainActivity.java**
   - Creates notification channels
   - Creates alarm service channel

8. **AndroidManifest.xml**
   - Registers AlarmActivity
   - Registers AlarmService
   - Adds necessary permissions

### New Permissions Added
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/>
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT"/>
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
```

## 🎨 Alarm UI Customization

To customize the alarm screen, edit `activity_alarm.xml`:

```xml
<!-- Change background color -->
android:background="#FF1744"  <!-- Red -->

<!-- Change title text -->
android:text="💊 MEDICATION TIME!"

<!-- Change button text -->
android:text="TAKE PILL"

<!-- Change button color -->
android:textColor="#FF1744"
android:background="@android:color/white"
```

## 🔧 Configuration

### Adjust Warning Time
In `AlarmReceiver.java`, line ~195:
```java
long warningTime = alarmTimeMillis - (30 * 60 * 1000); // 30 minutes
// Change 30 to any number of minutes
```

### Adjust Repeat Timing
In `AlarmReceiver.java`, line ~151:
```java
int delaySeconds = Math.max(10, 30 - (repeatCount * 2));
// Start: 30 seconds, decrease by 2 each time, min: 10 seconds
```

### Adjust Vibration Pattern
In `AlarmService.java`, line ~67:
```java
long[] pattern = {
    0,      // Start immediately
    1000,   // Vibrate 1 second
    500     // Pause 0.5 seconds
};
```

## 📊 Testing Checklist

### Basic Tests
- [ ] Schedule alarm for 31 minutes from now
- [ ] Verify warning notification appears at 30 min mark
- [ ] Verify full-screen alarm appears at scheduled time
- [ ] Verify alarm sound is loud and continuous
- [ ] Verify vibration is continuous
- [ ] Verify screen turns on
- [ ] Verify alarm shows over lock screen

### Interaction Tests
- [ ] Try dismissing with back button (should fail)
- [ ] Tap TAKE PILL button
- [ ] Verify alarm stops immediately
- [ ] Verify app opens
- [ ] Verify all notifications dismissed

### Repeat Tests
- [ ] Schedule alarm
- [ ] Wait for alarm
- [ ] Don't tap TAKE PILL
- [ ] Verify alarm repeats after 30 seconds
- [ ] Verify it repeats again after 28 seconds
- [ ] Verify continues decreasing to 10 seconds

### Edge Cases
- [ ] Test with phone locked
- [ ] Test with Do Not Disturb on
- [ ] Test with silent mode on
- [ ] Test with low battery
- [ ] Test with multiple alarms scheduled
- [ ] Test alarm while using another app

## 🚨 Troubleshooting

### Alarm doesn't show full-screen
**Solution:** Grant "Display over other apps" permission
- Settings → Apps → Dosevia → Display over other apps → Allow

### Alarm doesn't turn on screen
**Solution:** Ensure battery optimization is disabled
- The permission check already handles this

### Sound doesn't play
**Check:**
1. Alarm volume is not muted
2. Phone is not in silent mode
3. Alarm permission granted
4. Battery optimization disabled

### Alarm doesn't repeat
**Check:**
1. Exact alarm permission granted
2. Battery optimization disabled
3. Check logcat for errors

## 📱 User Instructions

### First Time Setup
1. Launch app
2. Grant all required permissions:
   - Notifications ✓
   - Exact Alarms ✓
   - Battery Optimization OFF ✓
   - Display over other apps ✓

### Setting an Alarm
1. Open app
2. Set medication time
3. App automatically schedules:
   - 30-minute warning
   - Full-screen alarm
   - Repeating alarms

### When Alarm Rings
1. **Full-screen alarm** appears
2. **Tap "TAKE PILL"** to stop
3. **App opens** automatically
4. **Mark pill as taken** in the app

### If You Miss the Alarm
- **Alarm repeats** every 30 seconds
- **Gets more frequent** (down to 10 seconds)
- **Must tap TAKE PILL** to stop

## 🎯 Key Features Summary

✅ **30-minute warning** notification  
✅ **Full-screen alarm** at scheduled time  
✅ **Continuous alarm sound** (looping)  
✅ **Continuous vibration** pattern  
✅ **Cannot dismiss** with back button  
✅ **Shows over lock screen**  
✅ **Turns screen on** automatically  
✅ **Repeating alarms** (30s → 10s)  
✅ **Forces user** to tap button  
✅ **Opens app** after dismissal  

## 📦 Files Modified/Created

### Modified
- `AlarmReceiver.java` - Added warning + full-screen logic
- `AlarmDismissReceiver.java` - Added service stop
- `AlarmPlugin.java` - Added auto-warning scheduling
- `MainActivity.java` - Added service channel
- `AndroidManifest.xml` - Added activity + permissions

### Created/Updated
- `AlarmService.java` - Continuous alarm service
- `AlarmActivity.java` - Full-screen alarm UI
- `activity_alarm.xml` - Alarm screen layout

## 🚀 Deployment

1. **Replace all files** in your project
2. **Clean build**:
   ```bash
   cd android
   ./gradlew clean
   ```
3. **Rebuild**:
   ```bash
   npm run build
   npx cap sync android
   npx cap run android
   ```
4. **Test thoroughly** before production

---

**Version:** 3.0  
**Last Updated:** January 27, 2026  
**Features:** Full-screen alarm + 30-min warning + repeating alarms  
