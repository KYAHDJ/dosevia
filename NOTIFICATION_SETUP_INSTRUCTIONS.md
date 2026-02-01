# 🔔 NOTIFICATION SETUP INSTRUCTIONS

## ✅ WHAT I FIXED

Your notifications now have:
- ✅ 30 minutes before warning
- ✅ 1 minute before warning
- ✅ Main alarm at pill time
- ✅ Escalating repeats: 30s → 30s → 25s → 25s → 20s → 20s → 15s → 15s → then every 10s
- ✅ Vibration on EVERY notification
- ✅ Sound on EVERY notification
- ✅ Lock screen display
- ✅ Stops immediately when pill taken

## 🎵 ADD DEFAULT ALARM SOUND

You need to add a sound file to your Android project:

### Step 1: Get a Sound File
You need an alarm sound file (MP3 format). Either:
- **Option A:** Download a free alarm sound from [Zapsplat](https://www.zapsplat.com/sound-effect-categories/alarms/) or [Freesound](https://freesound.org/)
- **Option B:** Use your phone's default alarm sound
- **Option C:** I'll tell you how to use Android's built-in alarm sound (see below)

### Step 2: Add Sound to Android Project

#### For Custom MP3 File:
1. Name the file: `notification_sound.mp3`
2. Place it in: `android/app/src/main/res/raw/`
3. Create the `raw` folder if it doesn't exist:

```powershell
# Run from your project root
mkdir android\app\src\main\res\raw
# Copy your sound file there
copy "path\to\your\sound.mp3" android\app\src\main\res\raw\notification_sound.mp3
```

#### For Android Built-in Alarm Sound:
If you just want to use Android's default alarm sound without adding a file, change this line in `notifications.ts`:

**Change from:**
```typescript
sound: 'notification_sound',
```

**To:**
```typescript
sound: 'alarm', // Uses Android's built-in alarm sound
```

## 📁 File Structure After Adding Sound

```
android/
├── app/
│   └── src/
│       └── main/
│           └── res/
│               └── raw/              ← Create this folder
│                   └── notification_sound.mp3  ← Add your sound here
```

## 🔧 Rebuild App After Adding Sound

After adding the sound file:

```powershell
npm run build
npx cap sync android
npx cap run android
```

## 🧪 TESTING THE NOTIFICATIONS

### Test 1: Immediate Test (Don't Wait for Scheduled Time)
Add this test code to `HomeScreen.tsx`:

```typescript
// Add this button to test notifications immediately
<button 
  onClick={async () => {
    const { LocalNotifications } = await import('@capacitor/local-notifications');
    await LocalNotifications.schedule({
      notifications: [{
        id: 999,
        title: 'TEST NOTIFICATION',
        body: 'This is a test with sound and vibration',
        schedule: { at: new Date(Date.now() + 3000) }, // 3 seconds from now
        sound: 'notification_sound',
        vibrate: true,
        channelId: 'dosevia-critical-alarm',
      }]
    });
    alert('Test notification will fire in 3 seconds!');
  }}
>
  🔔 Test Notification
</button>
```

### Test 2: Schedule Real Alarm
1. Set pill reminder time to 2 minutes from now
2. Wait and watch for notifications:
   - Nothing happens (waiting...)
   - Then: 30-min warning (if time allows)
   - Then: 1-min warning
   - Then: MAIN ALARM with sound + vibration
   - Then: 30s later → repeat
   - Then: 30s later → repeat
   - Then: 25s later → repeat
   - (continues escalating down to 10s)

### Test 3: Mark as Taken
1. Let notifications start firing
2. Click "Taken" on the pill
3. Check console: `🔕 All alarms canceled`
4. Notifications should STOP immediately

## 📊 WHAT YOU'LL SEE IN CONSOLE

```
⏰ Scheduling alarms for 2/1/2026, 9:00:00 PM
📢 Scheduled 30-min early warning
📢 Scheduled 1-min early warning
🔔 Scheduled main alarm
📢 Scheduled 20 escalating alarms
✅ Total notifications scheduled: 23
```

When pill is taken:
```
🔕 All alarms canceled
```

## 🎵 SOUND OPTIONS

### Option 1: Custom Sound (Recommended)
- Add `notification_sound.mp3` to `res/raw/`
- Full control over sound
- Can be loud and distinctive

### Option 2: Android Built-in Alarm
- Change `sound: 'notification_sound'` to `sound: 'alarm'`
- Uses phone's system alarm sound
- Always loud and distinctive
- No file needed

### Option 3: Android Built-in Notification Sound
- Change `sound: 'notification_sound'` to `sound: 'default'`
- Uses phone's default notification sound
- Might be quieter

## ⚠️ TROUBLESHOOTING

### No Sound Playing?
1. Check phone is not on silent mode
2. Check app notification settings: Settings → Apps → Dosevia → Notifications
3. Make sure "Medication Alarms" channel is enabled
4. Check volume: Increase notification volume on phone
5. Try using `sound: 'alarm'` instead of custom sound

### No Vibration?
1. Check phone is not in battery saver mode
2. Check app notification settings
3. Check vibration is enabled in channel settings

### Notifications Not Showing?
1. Check notification permission was granted
2. Check console for error messages
3. Check Android Logcat for detailed errors

### Sound File Not Found Error?
1. Make sure filename is exactly: `notification_sound.mp3`
2. Make sure it's in: `android/app/src/main/res/raw/`
3. Run `npx cap sync android` after adding file
4. Clean and rebuild: `cd android && ./gradlew clean`

## 🎯 QUICK START (EASIEST WAY)

**Just use Android's built-in alarm sound:**

In `src/app/lib/notifications.ts`, change ALL instances of:
```typescript
sound: 'notification_sound',
```

To:
```typescript
sound: 'alarm',
```

Then rebuild. That's it! No sound file needed.

## 📝 SUMMARY

- ✅ Notifications.ts completely rewritten
- ✅ Escalating pattern implemented (30s down to 10s)
- ✅ Vibration on every notification
- ✅ Sound on every notification (once you add the file or use 'alarm')
- ✅ Stops when pill taken
- ✅ 30-min and 1-min warnings added

All you need to do is add the sound file (or use 'alarm') and rebuild!
