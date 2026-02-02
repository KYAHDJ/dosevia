# Dosevia - Professional Pill Reminder App

A modern, feature-rich pill reminder application built with React, TypeScript, and Capacitor for Android.

## Features

### Core Functionality
- **Flexible Pill Schedules**: Support for various regimens (21+7, 24+4, 84+7, 365-day, custom)
- **Smart Tracking**: Visual blister pack calendar with swipe navigation
- **Notes System**: Track your experience with date/time-stamped notes
- **Statistics Dashboard**: Monitor adherence and track patterns
- **History View**: Complete log of all pill intake

### Enhanced User Experience
- **Improved Swipe Animation**: Smooth, responsive navigation for packs with more than 28 pills
- **Visual Feedback**: Real-time drag indicators and pack previews
- **Responsive Design**: Optimized for all screen sizes
- **Professional UI**: Clean, modern interface with gradient accents

### Notifications & Reminders
- **Daily Reminders**: Customizable notification times
- **Missed Pill Warnings**: Automatic alerts for forgotten pills
- **Pill Buying Reminders**: Never run out with advance warnings
- **Custom Sounds**: Use your own notification sounds
- **Vibration Support**: Optional vibration alerts

## Tech Stack

- **Frontend**: React 18.3+ with TypeScript
- **UI Framework**: Tailwind CSS 4+ with custom components
- **Icons**: Lucide React
- **Mobile**: Capacitor 8+ for native Android features
- **State Management**: React Hooks with local storage persistence
- **Date Handling**: date-fns library

## Project Structure

```
dosevia/
├── src/
│   ├── app/
│   │   ├── components/
│   │   │   ├── HomeScreen.tsx          # Main dashboard
│   │   │   ├── SettingsScreen.tsx      # App configuration
│   │   │   ├── HistoryScreen.tsx       # Pill history
│   │   │   ├── StatsScreen.tsx         # Statistics
│   │   │   ├── NotesScreen.tsx         # Notes management
│   │   │   ├── SwipeableBlisterPacks.tsx  # Enhanced swipe UI
│   │   │   ├── CalendarBlisterPack.tsx    # Calendar view
│   │   │   └── ui/                     # Reusable UI components
│   │   ├── lib/
│   │   │   └── notifications.ts        # Notification management
│   │   └── App.tsx                     # Main app component
│   ├── types/
│   │   └── pill-types.ts              # TypeScript interfaces
│   ├── styles/
│   │   ├── index.css                  # Global styles
│   │   ├── theme.css                  # Theme variables
│   │   └── tailwind.css               # Tailwind directives
│   └── main.tsx                       # App entry point
├── android/                           # Android native project
├── capacitor.config.json              # Capacitor configuration
├── package.json                       # Dependencies
├── vite.config.ts                     # Vite configuration
└── README.md                          # This file
```

## Installation

### Prerequisites
- Node.js 18+ and npm/pnpm
- Android Studio (for Android development)
- Java JDK 17+

### Setup Steps

1. **Install Dependencies**
   ```bash
   npm install
   # or
   pnpm install
   ```

2. **Build the Web App**
   ```bash
   npm run build
   # or
   pnpm build
   ```

3. **Sync with Capacitor**
   ```bash
   npx cap sync android
   ```

4. **Open in Android Studio**
   ```bash
   npx cap open android
   ```

5. **Run on Device**
   - Connect your Android device or start an emulator
   - Click "Run" in Android Studio

## Development

### Running Development Server
```bash
npm run dev
# or
pnpm dev
```

### Building for Production
```bash
npm run build
npx cap sync android
```

### Testing on Device
```bash
npx cap run android
```

## Configuration

### Capacitor Configuration
Edit `capacitor.config.json`:
- `appId`: Your unique app identifier
- `appName`: Display name in app launcher
- `webDir`: Build output directory (default: `dist`)

### Android Configuration
Edit `android/app/build.gradle`:
- `applicationId`: Must match Capacitor appId
- `versionCode`: Increment for each release
- `versionName`: User-visible version string

## Key Features Explained

### Enhanced Swipe Animation
The swipe animation for packs >28 pills now includes:
- **Real-time drag feedback**: Visual indication of swipe progress
- **Pack previews**: See adjacent packs while dragging
- **Smooth transitions**: 300ms animated transitions
- **Bounded drag**: Maximum drag distance prevents over-scrolling
- **Touch indicators**: Animated arrows show swipe direction
- **Navigation arrows**: Tap arrows for quick navigation

### Notes System
Track your experience with comprehensive notes:
- **Date & Time**: Automatic timestamp for each note
- **Search**: Quickly find notes by content
- **Organization**: Grouped by Today, Yesterday, This Week, etc.
- **Edit & Delete**: Full CRUD operations
- **Persistent Storage**: Notes saved with app state

### Pill Type Support
- **21+7**: Standard 28-day cycle (21 active + 7 placebo)
- **24+4**: Shorter placebo interval
- **26+2**: Minimal placebo interval
- **28-day**: Continuous active pills
- **84+7**: Quarterly cycle (3 months)
- **365-day**: Year-long continuous
- **28-POP**: Progestin-only pills
- **Flexible**: User-controlled cycle
- **Custom**: Define your own configuration

## Data Storage

All data is stored locally using Capacitor Preferences API:
- Pill schedule and tracking data
- User settings and preferences
- Notes with timestamps
- App configuration

Data persists across app restarts and survives app updates.

## Notifications

### Android Permissions
The app requests the following permissions:
- `POST_NOTIFICATIONS`: Display notifications
- `SCHEDULE_EXACT_ALARM`: Schedule precise reminders
- `USE_EXACT_ALARM`: Required for Android 14+
- `VIBRATE`: Vibration alerts
- `RECEIVE_BOOT_COMPLETED`: Restore alarms after reboot

### Notification Types
1. **Daily Reminder**: Main pill-taking reminder
2. **Missed Pill Warning**: Alert after specified interval
3. **Pill Buying Reminder**: Advance notice before running out

## Troubleshooting

### Build Issues
- Clear build cache: `rm -rf android/build android/.gradle`
- Resync Capacitor: `npx cap sync android`
- Clean Android project in Android Studio

### Notification Issues
- Check system notification permissions
- Verify exact alarm permissions (Android 12+)
- Ensure battery optimization is disabled for the app

### State Not Persisting
- Check browser console for storage errors
- Verify Capacitor Preferences plugin is installed
- Test on actual device (not just browser)

## Contributing

When contributing, please:
1. Follow the existing code style
2. Add TypeScript types for new features
3. Test on actual Android devices
4. Update this README for new features

## License

This project is proprietary software. All rights reserved.

## Version History

### Version 2.0 (Current)
- Enhanced swipe animation for multi-pack navigation
- Added comprehensive notes system
- Improved UI feedback and visual indicators
- Consolidated documentation
- Removed unnecessary build scripts
- Professional code organization

### Version 1.0
- Initial release
- Basic pill tracking
- Notification system
- Statistics and history

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review the Android logs: `adb logcat`
3. Inspect browser console in development mode
4. Test on multiple devices if possible

---

**Built with ❤️ using React + Capacitor**
