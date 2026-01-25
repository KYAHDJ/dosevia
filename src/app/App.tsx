import { useState, useEffect } from 'react';
import {
  DayData,
  PillType,
  PillStatus,
  ReminderSettings,
} from '@/types/pill-types';
import { HomeScreen } from './components/HomeScreen';
import { SettingsScreen } from './components/SettingsScreen';
import { HistoryScreen } from './components/HistoryScreen';
import { StatsScreen } from './components/StatsScreen';
import { addDays } from 'date-fns';
import { Preferences } from '@capacitor/preferences';
import { LocalNotifications } from '@capacitor/local-notifications';

type Screen = 'home' | 'settings' | 'history' | 'stats';

const STORAGE_KEY = 'dosevia-app-state';
const DAILY_REMINDER_ID = 1;

function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');
  const [pillType, setPillType] = useState<PillType>('21+7');
  const [startDate, setStartDate] = useState(new Date(2015, 0, 22));
  const [days, setDays] = useState<DayData[]>([]);

  useEffect(() => {
  LocalNotifications.requestPermissions().then((res) => {
    console.log('Notification permission:', res);
  });
}, []);


  const [settings, setSettings] = useState<ReminderSettings>({
    placeboReminder: true,
    dailyReminderTime: '9:00 PM',
    pillBuyingDaysBefore: 7,
    pillBuyingReminderTime: '9:00 AM',
    appActive: true,
    repeatInterval: 5,
    notificationSound: 'Default',
    playSoundAlways: false,
    vibrateAlways: true,
    notificationTitle: 'Time to take your pill!',
    notificationSubtitle: "Don't forget your daily dose",
    notificationIcon: 'pill',
  });

  /* ──────────────────────────────────────────────
     REQUEST NOTIFICATION PERMISSION (ONCE)
  ────────────────────────────────────────────── */
  useEffect(() => {
    LocalNotifications.requestPermissions();
  }, []);

  /* ──────────────────────────────────────────────
     LOAD SAVED STATE (ONCE)
  ────────────────────────────────────────────── */
  useEffect(() => {
    (async () => {
      const { value } = await Preferences.get({ key: STORAGE_KEY });
      if (!value) return;

      try {
        const saved = JSON.parse(value);

        if (saved.pillType) setPillType(saved.pillType);
        if (saved.startDate) setStartDate(new Date(saved.startDate));
        if (saved.days) setDays(saved.days);
        if (saved.settings) setSettings(saved.settings);
      } catch (e) {
        console.warn('Failed to load saved state', e);
      }
    })();
  }, []);

  /* ──────────────────────────────────────────────
     SAVE STATE (AUTOMATIC)
  ────────────────────────────────────────────── */
  useEffect(() => {
    Preferences.set({
      key: STORAGE_KEY,
      value: JSON.stringify({
        pillType,
        startDate,
        days,
        settings,
      }),
    });
  }, [pillType, startDate, days, settings]);

  /* ──────────────────────────────────────────────
     DAILY REMINDER ALARM (SYSTEM NOTIFICATION)
  ────────────────────────────────────────────── */
  useEffect(() => {
  const updateDailyReminder = async () => {
    await LocalNotifications.cancel({
      notifications: [{ id: DAILY_REMINDER_ID }],
    });

    if (!settings.appActive) return;

    const [hour, minute] = parseTime(settings.dailyReminderTime);

    const now = new Date();
    const firstTrigger = new Date();
    firstTrigger.setHours(hour, minute, 0, 0);

    // If time already passed today, schedule for tomorrow
    if (firstTrigger <= now) {
      firstTrigger.setDate(firstTrigger.getDate() + 1);
    }

    await LocalNotifications.schedule({
      notifications: [
        {
          id: DAILY_REMINDER_ID,
          title: settings.notificationTitle,
          body: settings.notificationSubtitle,
          schedule: {
            at: firstTrigger,
            repeats: true,
          },
        },
      ],
    });
  };

  updateDailyReminder();
}, [
  settings.appActive,
  settings.dailyReminderTime,
  settings.notificationTitle,
  settings.notificationSubtitle,
]);


  /* ──────────────────────────────────────────────
     Initialize days based on pill type & start date
  ────────────────────────────────────────────── */
  useEffect(() => {
    const newDays: DayData[] = [];
    let activePills = 21;
    let placeboPills = 7;

    if (pillType === '24+4') {
      activePills = 24;
      placeboPills = 4;
    } else if (pillType === '28-day') {
      activePills = 28;
      placeboPills = 0;
    }

    const totalDays = activePills + placeboPills;

    for (let i = 0; i < totalDays; i++) {
      newDays.push({
        day: i + 1,
        status: 'not_taken',
        isPlacebo: i >= activePills,
        date: addDays(startDate, i),
      });
    }

    setDays(newDays);
  }, [pillType, startDate]);

  const handleStatusChange = (day: number, status: PillStatus) => {
    setDays((prevDays) =>
      prevDays.map((d) => (d.day === day ? { ...d, status } : d))
    );
  };

  const handlePillTypeChange = (newType: PillType) => {
    setPillType(newType);
  };

  const handleStartDateChange = (newDate: Date) => {
    setStartDate(newDate);
  };

  const handleNavigate = (screen: Screen) => {
    setCurrentScreen(screen);
  };

  const handleBack = () => {
    setCurrentScreen('home');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-orange-50 to-yellow-50 font-sans antialiased">
      <div className="max-w-md mx-auto bg-white min-h-screen shadow-2xl">
        {currentScreen === 'home' && (
          <HomeScreen
            pillType={pillType}
            startDate={startDate}
            days={days}
            settings={settings}
            onPillTypeChange={handlePillTypeChange}
            onStartDateChange={handleStartDateChange}
            onStatusChange={handleStatusChange}
            onNavigate={handleNavigate}
          />
        )}

        {currentScreen === 'settings' && (
          <SettingsScreen
            settings={settings}
            onSettingsChange={setSettings}
            onBack={handleBack}
          />
        )}

        {currentScreen === 'history' && (
          <HistoryScreen days={days} onBack={handleBack} />
        )}

        {currentScreen === 'stats' && (
          <StatsScreen
            days={days}
            startDate={startDate}
            onBack={handleBack}
          />
        )}
      </div>
    </div>
  );
}

/* ──────────────────────────────────────────────
   Helpers
────────────────────────────────────────────── */
function parseTime(time: string): [number, number] {
  // Supports "9:00 PM" and "21:00"
  if (time.includes('AM') || time.includes('PM')) {
    const [t, modifier] = time.split(' ');
    let [hours, minutes] = t.split(':').map(Number);
    if (modifier === 'PM' && hours < 12) hours += 12;
    if (modifier === 'AM' && hours === 12) hours = 0;
    return [hours, minutes];
  }

  const [hours, minutes] = time.split(':').map(Number);
  return [hours, minutes];
}

export default App;
