import { useState, useEffect } from 'react';
import { DayData, PillType, PillStatus, ReminderSettings } from '@/types/pill-types';
import { HomeScreen } from './components/HomeScreen';
import { SettingsScreen } from './components/SettingsScreen';
import { HistoryScreen } from './components/HistoryScreen';
import { StatsScreen } from './components/StatsScreen';
import { addDays } from 'date-fns';

type Screen = 'home' | 'settings' | 'history' | 'stats';

function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');
  const [pillType, setPillType] = useState<PillType>('21+7');
  const [startDate, setStartDate] = useState(new Date(2015, 0, 22)); // January 22, 2015
  const [days, setDays] = useState<DayData[]>([]);

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

  // Initialize days based on pill type
  useEffect(() => {
    const initializeDays = () => {
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
    };

    initializeDays();
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
      {/* Mobile viewport container */}
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
          <StatsScreen days={days} startDate={startDate} onBack={handleBack} />
        )}
      </div>
    </div>
  );
}

export default App;