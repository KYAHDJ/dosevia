# 📊 NOTIFICATION FLOW DIAGRAM

## ⏰ TIMELINE VISUALIZATION

```
SET PILL TIME: 9:00 PM
═══════════════════════════════════════════════════════════════════

8:30 PM  │  ⏰ Early Warning (-30 min)
         │  🔊 SOUND + 📳 VIBRATE
         │  "Your medication time is in 30 minutes"
         │
         ↓ (29 minutes)
         │
8:59 PM  │  ⏰ Early Warning (-1 min)
         │  🔊 SOUND + 📳 VIBRATE
         │  "Your medication time is in 1 minute!"
         │
         ↓ (1 minute)
         │
9:00 PM  │  🔔 MAIN ALARM (PILL TIME!)
         │  🔊 SOUND + 📳 VIBRATE
         │  "Time to take your medication!"
         │
         ↓ (30 seconds) ← First escalation interval
         │
9:00:30  │  🚨 URGENT Reminder #1
         │  🔊 SOUND + 📳 VIBRATE
         │  "⏰ Reminder #1: Time to take your medication!"
         │
         ↓ (30 seconds) ← Second 30s interval
         │
9:01:00  │  🚨 URGENT Reminder #2
         │  🔊 SOUND + 📳 VIBRATE
         │  "⏰ Reminder #2: Time to take your medication!"
         │
         ↓ (25 seconds) ← Decreasing to 25s
         │
9:01:25  │  🚨 URGENT Reminder #3
         │  🔊 SOUND + 📳 VIBRATE
         │  "⏰ Reminder #3: Time to take your medication!"
         │
         ↓ (25 seconds) ← Second 25s interval
         │
9:01:50  │  🚨 URGENT Reminder #4
         │  🔊 SOUND + 📳 VIBRATE
         │  "⏰ Reminder #4: Time to take your medication!"
         │
         ↓ (20 seconds) ← Decreasing to 20s
         │
9:02:10  │  🚨 URGENT Reminder #5
         │  🔊 SOUND + 📳 VIBRATE
         │  "⏰ Reminder #5: Time to take your medication!"
         │
         ↓ (20 seconds) ← Second 20s interval
         │
9:02:30  │  🚨 URGENT Reminder #6
         │  🔊 SOUND + 📳 VIBRATE
         │  "⏰ Reminder #6: Time to take your medication!"
         │
         ↓ (15 seconds) ← Decreasing to 15s
         │
9:02:45  │  🚨 URGENT Reminder #7
         │  🔊 SOUND + 📳 VIBRATE
         │  "⏰ Reminder #7: Time to take your medication!"
         │
         ↓ (15 seconds) ← Second 15s interval
         │
9:03:00  │  🚨 URGENT Reminder #8
         │  🔊 SOUND + 📳 VIBRATE
         │  "⏰ Reminder #8: Time to take your medication!"
         │
         ↓ (10 seconds) ← NOW AT FINAL INTERVAL: 10s
         │
9:03:10  │  🚨 URGENT Reminder #9
         │  🔊 SOUND + 📳 VIBRATE
         │  "⏰ Reminder #9: Time to take your medication!"
         │
         ↓ (10 seconds) ← Stays at 10s
         │
9:03:20  │  🚨 URGENT Reminder #10
         │  🔊 SOUND + 📳 VIBRATE
         │  "⏰ Reminder #10: Time to take your medication!"
         │
         ↓ (10 seconds)
         │
9:03:30  │  🚨 URGENT Reminder #11
         │  🔊 SOUND + 📳 VIBRATE
         │  
         ↓ (10 seconds)
         │
9:03:40  │  🚨 URGENT Reminder #12
         │  🔊 SOUND + 📳 VIBRATE
         │
         │  ... continues every 10 seconds ...
         │
9:05:10  │  🚨 URGENT Reminder #20 (FINAL)
         │  🔊 SOUND + 📳 VIBRATE
         │  "⏰ Reminder #20: Time to take your medication!"
         │
         ↓ END (no more scheduled notifications)

═══════════════════════════════════════════════════════════════════
TOTAL DURATION: ~5 minutes of constant reminders
TOTAL NOTIFICATIONS: 23 (2 early + 1 main + 20 escalating)
ESCALATION PATTERN: 30s → 30s → 25s → 25s → 20s → 20s → 15s → 15s → 10s (×12)
```

## 🛑 HOW TO STOP THE NOTIFICATIONS

```
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║  NOTIFICATIONS ARE FIRING EVERY 10 SECONDS!               ║
║                                                           ║
║  TO STOP THEM:                                            ║
║                                                           ║
║  1. Open Dosevia app                                      ║
║  2. Find today's pill on calendar                         ║
║  3. Tap the pill                                          ║
║  4. Press "Taken" button                                  ║
║                                                           ║
║  ✅ ALL NOTIFICATIONS IMMEDIATELY CANCELED                ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
```

## 📱 WHAT EACH NOTIFICATION DOES

### Individual Notification Behavior:
```
┌─────────────────────────────────────────┐
│                                         │
│  1. NOTIFICATION FIRES                  │
│     ↓                                   │
│  2. 🔊 SOUND PLAYS                      │
│     (Phone's default alarm sound)       │
│     ↓                                   │
│  3. 📳 VIBRATION PULSE                  │
│     (Single vibration burst)            │
│     ↓                                   │
│  4. 📲 NOTIFICATION APPEARS              │
│     (On screen + lock screen)           │
│     ↓                                   │
│  5. ⏸️ SOUND STOPS                      │
│     (Sound ends, notification stays)    │
│     ↓                                   │
│  6. ⏰ WAIT PERIOD                       │
│     (30s, 25s, 20s, 15s, or 10s)       │
│     ↓                                   │
│  7. 🔁 NEXT NOTIFICATION FIRES           │
│     (Back to step 1)                    │
│                                         │
└─────────────────────────────────────────┘
```

## 🎵 SOUND + VIBRATION PATTERN

```
Each Notification:

🔊 SOUND:  ■■■■■■■■■■■■░░░░░░░░░░░░░░░░░░
           ↑                        ↑
         Plays                    Stops
         (2-3 seconds)

📳 VIBRATE: ████░░░░░░░░░░░░░░░░░░░░░░░░░
           ↑    ↑
        Buzz  Stop
         (0.5s)

📲 NOTIFICATION: ███████████████████████████
                 ↑                        ↑
               Appears               Stays visible

⏰ WAIT:  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░
          (30s → 25s → 20s → 15s → 10s)

Then repeat! 🔁
```

## 📊 ESCALATION INTERVALS

```
Notification Count  │  Interval  │  Purpose
════════════════════╪════════════╪═══════════════════════
#1 - #2            │   30 sec   │  Give user time to respond
#3 - #4            │   25 sec   │  Start increasing urgency
#5 - #6            │   20 sec   │  More frequent reminders
#7 - #8            │   15 sec   │  Getting more urgent
#9 - #20           │   10 sec   │  MAXIMUM ANNOYANCE! 😈
```

## 🎯 COMPARISON: CONTINUOUS vs DISCRETE

### ❌ CONTINUOUS ALARM (What you DON'T have):
```
9:00 PM ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━►
        SOUND NEVER STOPS UNTIL DISMISSED
        🔊🔊🔊🔊🔊🔊🔊🔊🔊🔊🔊🔊🔊🔊🔊🔊🔊🔊🔊🔊
```

### ✅ DISCRETE NOTIFICATIONS (What you HAVE):
```
9:00:00  🔊─────────────────────────────────────────────►
9:00:30  🔊─────────────────────────────────────────────►
9:01:00  🔊─────────────────────────────────────────────►
9:01:25  🔊─────────────────────────────────────────────►
9:01:50  🔊─────────────────────────────────────────────►
9:02:10  🔊─────────────────────────────────────────────►
         ... continues every 10 seconds ...

Each line: Sound plays → Stops → Waits → Next fires
```

## 🎭 USER EXPERIENCE

```
┌───────────────────────────────────────────────────┐
│                                                   │
│  👤 USER: "I'll just ignore the notification..."  │
│                                                   │
│  ⏰ 30 seconds later...                            │
│  🔊 DING! 📳 BUZZZ!                                │
│                                                   │
│  👤 USER: "Okay, okay, I'll do it later..."       │
│                                                   │
│  ⏰ 30 seconds later...                            │
│  🔊 DING! 📳 BUZZZ!                                │
│                                                   │
│  👤 USER: "FINE! I'll take it now!"               │
│  ✅ Presses "Taken"                                │
│                                                   │
│  🔕 ALL NOTIFICATIONS STOPPED ✨                   │
│                                                   │
└───────────────────────────────────────────────────┘
```

## 📈 ANNOYANCE FACTOR CHART

```
Annoyance
  100% │                                    ████████
       │                              ██████
       │                         █████
       │                    ████
   50% │              ████
       │        ████
       │   ███
     0% │██
        └────────────────────────────────────────────────► Time
        9:00  9:01  9:02  9:03  9:04  9:05

Legend:
█ = Notification frequency
Higher = More frequent = More annoying
Goal: Make user take pill ASAP! 💊
```

## ✅ EXPECTED OUTCOME

After 5 minutes of notifications:
```
╔══════════════════════════════════════════════════════╗
║                                                      ║
║  👤 USER IS VERY ANNOYED                             ║
║  💊 USER TAKES PILL                                   ║
║  📱 USER PRESSES "TAKEN"                              ║
║  🔕 NOTIFICATIONS STOP                                ║
║  ✅ MISSION ACCOMPLISHED!                             ║
║                                                      ║
╚══════════════════════════════════════════════════════╝
```

**This is EXACTLY what you asked for!** 🎉
