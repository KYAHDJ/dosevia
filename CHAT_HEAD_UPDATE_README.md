# Dosevia - Chat Head Notification Update

## 🎉 What's New

This update transforms the medication reminder experience with a **floating chat-head bubble** (like Facebook Messenger) that appears on your screen when it's time to take your medication!

### ✨ Key Changes

1. **Floating Chat Head Bubble** 
   - A draggable pill icon that floats on your screen
   - Appears when it's medication time
   - Click it to open the app
   - Automatically disappears when you dismiss the notification or open the app

2. **Removed Alarm System**
   - No more continuous alarm sound
   - No more repeating alarms every 30 seconds
   - Cleaner, less intrusive experience

3. **Enhanced Notifications**
   - Clear notification with sound and vibration
   - Strong vibration pattern to alert you
   - Quick "TAKE PILL" action button
   - Auto-dismiss when you interact with it

4. **Improved 30-Minute Warning**
   - More precise timing using `setExactAndAllowWhileIdle`
   - Better reliability even in Doze mode
   - Auto-dismisses after 30 minutes

## 🔧 Technical Changes

### New Files
- `ChatHeadService.java` - Service that manages the floating bubble
- `chat_head_layout.xml` - Layout for the floating bubble
- `chat_head_background.xml` - Styled background for the bubble

### Modified Files
- `AlarmReceiver.java` - Removed alarm service logic, added chat head
- `AlarmDismissReceiver.java` - Simplified to stop chat head
- `MainActivity.java` - Added overlay permission check
- `AndroidManifest.xml` - Added chat head service and overlay permission

### Removed Files
- `AlarmService.java` is no longer used (but kept in project for compatibility)

## 📱 Required Permissions

The app now requires one additional permission:
- **Display over other apps** - For the floating chat head bubble

This permission will be requested automatically when you first run the updated app.

## 🚀 How It Works

### Medication Time Flow
1. **30 Minutes Before**: You receive a warning notification
2. **At Medication Time**:
   - A notification appears with sound and vibration
   - A floating chat head bubble pops up on your screen
   - Click the bubble or notification to open the app
3. **After Taking Pill**: 
   - Click "TAKE PILL" button to dismiss
   - Or open the app naturally - the bubble disappears
   - Chat head service stops automatically

### Chat Head Features
- **Draggable**: Move it anywhere on the screen
- **Clickable**: Opens the app when tapped
- **Auto-dismiss**: Disappears when you interact with the app or dismiss the notification
- **Permission-based**: Only shows if you've granted overlay permission

## 🔐 Battery & Performance

- Uses foreground service for reliability
- Minimal battery impact (chat head only appears briefly)
- No continuous alarm means less battery drain
- Strict timing using Android's exact alarm APIs

## 🛠️ Building the App

```bash
# Install dependencies
npm install

# Build web assets
npm run build

# Sync with Android
npx cap sync android

# Open in Android Studio
npx cap open android

# Build APK in Android Studio
```

## 📋 Testing Checklist

- [ ] Grant all required permissions (notifications, alarms, overlay, battery)
- [ ] Set a medication reminder for 2 minutes from now
- [ ] Wait for 30-minute warning (if testing with proper time)
- [ ] Verify notification appears with sound and vibration
- [ ] Verify floating chat head appears
- [ ] Test dragging the chat head
- [ ] Test clicking the chat head opens app
- [ ] Test "TAKE PILL" button dismisses everything
- [ ] Verify chat head disappears after dismissing

## 🐛 Troubleshooting

**Chat head not appearing?**
- Check if "Display over other apps" permission is granted
- Go to Settings > Apps > Dosevia > Display over other apps

**Notification not sounding?**
- Check notification channel settings
- Go to Settings > Apps > Dosevia > Notifications
- Ensure "Medication Reminders" is set to "Alert" or "High"

**Timing issues?**
- Ensure battery optimization is OFF for Dosevia
- Check that "Alarms & reminders" permission is granted

## 📝 Notes for Developers

### Chat Head Implementation
The chat head uses Android's `TYPE_APPLICATION_OVERLAY` window type (API 26+) with a foreground service to ensure it stays active. The service automatically cleans up when the user interacts with the app.

### Notification Timing
Both the 30-minute warning and main notification use `setExactAndAllowWhileIdle` to bypass Doze mode restrictions and ensure precise delivery.

### Service Lifecycle
The `ChatHeadService` is started when the alarm triggers and automatically stops when:
- User clicks the chat head
- User dismisses the notification via "TAKE PILL" button
- App is opened by any means

## 🎨 Customization

You can customize the chat head appearance by modifying:
- `chat_head_background.xml` - Change colors and size
- `chat_head_layout.xml` - Modify layout and icon
- Adjust icon size (currently 60dp)

## ⚖️ License

Same as the main Dosevia project.

---

**Version**: 2.0 - Chat Head Update
**Last Updated**: January 2026
