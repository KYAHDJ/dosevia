# 🎯 QUICK REFERENCE - SOUND + VIBRATION FIX

## 🔧 WHAT WAS FIXED

| Issue | Solution |
|-------|----------|
| No sound on notifications | Changed `sound: 'alarm'` → `sound: 'default'` |
| Sound only on first notification | Set `ongoing: false` on all notifications |
| Vibration only once | Each notification fires independently |
| Notifications auto-dismiss | Set `autoCancel: false` |

## 🚀 BUILD COMMANDS

```powershell
npm run build
npx cap sync android
npx cap run android
```

## 🧪 TEST IN 30 SECONDS

1. Open app → Settings
2. Set reminder time to 2 minutes from now
3. Save and wait
4. You'll hear: SOUND + VIBRATE on EVERY notification
5. Press "Taken" to stop

## ✅ VERIFY BEFORE TESTING

- [ ] Phone volume UP (notification volume, not media)
- [ ] Do Not Disturb is OFF
- [ ] App has notification permission
- [ ] Notification channel enabled in Settings → Apps → Dosevia

## 📱 NOTIFICATION PATTERN

```
Main Alarm    → 🔊 + 📳
  ↓ 30s
Reminder #1   → 🔊 + 📳
  ↓ 30s  
Reminder #2   → 🔊 + 📳
  ↓ 25s
Reminder #3   → 🔊 + 📳
  ... decreases to 10s intervals ...
```

**Stops ONLY when you press "Taken"!**

## 🎵 WHAT YOU'LL HEAR

- **Sound**: Phone's default alarm/notification sound
- **Vibration**: Single pulse on each notification
- **Pattern**: Discrete notifications (sound → stop → wait → repeat)
- **NOT**: Continuous alarm (that's not what you wanted)

## 🐛 IF NO SOUND

1. Volume UP (notification volume)
2. Check Settings → Apps → Dosevia → Notifications → ON
3. Check Do Not Disturb is OFF
4. Clear app data and reinstall

## 📝 KEY FILES CHANGED

- `src/app/lib/notifications.ts` - All sound/vibration logic

## 🎯 SUCCESS CRITERIA

✅ Sound plays on EVERY notification
✅ Vibration happens on EVERY notification  
✅ Pattern: 30s → 25s → 20s → 15s → 10s
✅ Stops ONLY when pill marked "Taken"
✅ Each notification is discrete (not continuous)

**IT WILL BE VERY ANNOYING - THAT'S THE POINT!** 😈💊
