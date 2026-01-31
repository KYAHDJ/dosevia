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
import { addDays, isSameDay, isAfter, isBefore, startOfDay } from 'date-fns';
import { Preferences } from '@capacitor/preferences';
import { 
  initNotifications, 
  scheduleDailyAlarm, 
  cancelAllAlarms,
  scheduleMissedPillWarning,
} from '@/app/lib/notifications';

type Screen = 'home' | 'settings' | 'history' | 'stats';

const STORAGE_KEY = 'dosevia-app-state';
const STORAGE_VERSION = '2.0';

function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');
  const [pillType, setPillType] = useState<PillType>('21+7');
  const [startDate, setStartDate] = useState(new Date());
  const [days, setDays] = useState<DayData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isFirstRun, setIsFirstRun] = useState(false);

  const [settings, setSettings] = useState<ReminderSettings>({
    placeboReminder: true,
    dailyReminderTime: '9:00 PM',
    pillBuyingDaysBefore: 7,
    pillBuyingReminderTime: '9:00 AM',
    appActive: true,
    repeatInterval: 30,
    notificationSound: 'Default',
    soundFileUri: '',
    playSoundAlways: true,
    vibrateAlways: true,
    notificationTitle: 'Time to take your pill',
    notificationSubtitle: 'Don\'t forget your daily dose',
    notificationIcon: '💊',
  });

  /* INIT NOTIFICATIONS */
  useEffect(() => {
    initNotifications();
  }, []);

  /* LOAD SAVED STATE - RUNS ONCE ON APP START */
  useEffect(() => {
    const loadState = async () => {
      try {
        console.log('🔄 Loading saved state...');
        const { value } = await Preferences.get({ key: STORAGE_KEY });
        
        if (!value) {
          console.log('❌ No saved data - this is first run');
          setIsFirstRun(true);
          setIsLoading(false);
          return;
        }

        const saved = JSON.parse(value);
        console.log('📦 Found saved data');
        
        // Restore everything
        if (saved.pillType) {
          console.log('✅ Restoring pillType:', saved.pillType);
          setPillType(saved.pillType);
        }

        if (saved.startDate) {
          console.log('✅ Restoring startDate:', saved.startDate);
          setStartDate(new Date(saved.startDate));
        }

        if (saved.days && Array.isArray(saved.days)) {
          const restoredDays = saved.days.map((day: any) => ({
            ...day,
            date: new Date(day.date),
          }));
          console.log('✅ Restored', restoredDays.length, 'pills');
          console.log('📊 First 5 statuses:', restoredDays.slice(0, 5).map((d: any) => `Day ${d.day}: ${d.status}`).join(', '));
          
          // AUTO-MARK MISSED PILLS
          const today = startOfDay(new Date());
          const updatedDays = restoredDays.map((day: DayData) => {
            const pillDate = startOfDay(day.date);
            // If pill date is in the past and not taken, mark as missed
            if (isBefore(pillDate, today) && day.status === 'not_taken') {
              console.log(`⚠️ Auto-marking Day ${day.day} as missed (date: ${pillDate.toDateString()})`);
              return { ...day, status: 'missed' as PillStatus };
            }
            return day;
          });
          
          setDays(updatedDays);
        }

        if (saved.settings) {
          console.log('✅ Restoring settings');
          setSettings(saved.settings);
        }

        console.log('✅ Load complete');
      } catch (error) {
        console.error('❌ Error loading:', error);
        setIsFirstRun(true);
      } finally {
        setIsLoading(false);
      }
    };

    loadState();
  }, []); // Only run once on mount

  /* CREATE INITIAL PILLS - ONLY ON FIRST RUN */
  useEffect(() => {
    // Don't run if loading, already have pills, or not first run
    if (isLoading || days.length > 0 || !isFirstRun) {
      return;
    }

    console.log('🆕 Creating initial pills (first run only)');
    
    const newDays: DayData[] = [];
    let active = 21;
    let placebo = 7;

    if (pillType === '24+4') {
      active = 24;
      placebo = 4;
    } else if (pillType === '28-day') {
      active = 28;
      placebo = 0;
    }

    for (let i = 0; i < active + placebo; i++) {
      newDays.push({
        day: i + 1,
        status: 'not_taken',
        isPlacebo: i >= active,
        date: addDays(startDate, i),
        takenAt: undefined,
      });
    }

    console.log('✅ Created', newDays.length, 'pills');
    setDays(newDays);
  }, [isLoading, isFirstRun, days.length]);

  /* SAVE STATE WHENEVER IT CHANGES */
  useEffect(() => {
    if (isLoading || days.length === 0) {
      return;
    }

    const saveState = async () => {
      try {
        const stateToSave = {
          version: STORAGE_VERSION,
          pillType,
          startDate: startDate.toISOString(),
          days: days.map(day => ({
            ...day,
            date: day.date.toISOString(),
          })),
          settings,
          lastSaved: new Date().toISOString(),
        };

        await Preferences.set({
          key: STORAGE_KEY,
          value: JSON.stringify(stateToSave),
        });

        console.log('💾 Saved - First 3 pills:', days.slice(0, 3).map(d => `Day ${d.day}: ${d.status}`).join(', '));
      } catch (error) {
        console.error('❌ Save error:', error);
      }
    };

    saveState();
  }, [days, pillType, startDate, settings, isLoading]);

  /* HANDLE PILL STATUS CHANGE */
  const handleStatusChange = (day: number, status: PillStatus) => {
    console.log(`🔄 Changing Day ${day} to: ${status}`);
    
    setDays((currentDays) => {
      const newDays = currentDays.map((x) => {
        if (x.day === day) {
          return { 
            ...x, 
            status,
            takenAt: status === 'taken' ? new Date().toISOString() : undefined,
          };
        }
        return x;
      });

      console.log(`✅ Updated Day ${day}`);
      
      // Cancel alarms if taken
      const updatedPill = newDays.find(d => d.day === day);
      if (updatedPill && status === 'taken' && isSameDay(updatedPill.date, new Date())) {
        console.log('🔕 Canceling alarms');
        cancelAllAlarms();
      }

      return newDays;
    });
  };

  /* HANDLE START DATE CHANGE - Auto-mark missed pills */
  const handleStartDateChange = (newStartDate: Date) => {
    console.log('📅 Start date changed to:', newStartDate.toDateString());
    setStartDate(newStartDate);
    
    // Recalculate all pill dates and auto-mark missed ones
    setDays((currentDays) => {
      const today = startOfDay(new Date());
      const updatedDays = currentDays.map((day, index) => {
        const newDate = addDays(newStartDate, index);
        const pillDate = startOfDay(newDate);
        
        // Auto-mark as missed if in the past and not taken
        let newStatus = day.status;
        if (isBefore(pillDate, today) && day.status === 'not_taken') {
          console.log(`⚠️ Auto-marking Day ${day.day} as missed (new date: ${newDate.toDateString()})`);
          newStatus = 'missed';
        }
        
        return {
          ...day,
          date: newDate,
          status: newStatus,
        };
      });
      
      return updatedDays;
    });
  };

  /* SCHEDULE ALARM AND MISSED PILL WARNING */
  useEffect(() => {
    if (!settings.appActive || days.length === 0) {
      return;
    }

    const [hour, minute] = parseTime(settings.dailyReminderTime);
    const today = new Date();
    const todayPill = days.find(day => isSameDay(day.date, today));

    // Schedule main alarm with ICON and SOUND
    scheduleDailyAlarm(
      hour,
      minute,
      settings.notificationTitle,
      settings.notificationSubtitle,
      settings.notificationIcon,      // 🎨 Icon emoji
      settings.soundFileUri,           // 🔊 Sound file URI
      settings.vibrateAlways,
      todayPill?.status === 'taken'
    );

    // Schedule missed pill warning
    if (todayPill && todayPill.status === 'not_taken') {
      scheduleMissedPillWarning(
        hour, 
        minute,
        settings.notificationIcon,     // 🎨 Icon emoji
        settings.soundFileUri          // 🔊 Sound file URI
      );
      console.log('⚠️ Scheduled missed pill warning');
    }
  }, [settings.appActive, settings.dailyReminderTime, settings.notificationIcon, settings.soundFileUri, days]);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-orange-50 to-yellow-50 flex items-center justify-center">
        <div className="text-center">
          <div 
            className="inline-block w-16 h-16 rounded-full mb-4"
            style={{
              background: 'linear-gradient(135deg, #f609bc, #fab86d)',
              animation: 'pulse 2s ease-in-out infinite',
            }}
          />
          <p className="text-gray-600 font-medium">Loading Dosevia...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-orange-50 to-yellow-50">
      <div className="max-w-md mx-auto bg-white min-h-screen shadow-xl">
        {currentScreen === 'home' && (
          <HomeScreen
            pillType={pillType}
            startDate={startDate}
            days={days}
            settings={settings}
            onPillTypeChange={setPillType}
            onStartDateChange={handleStartDateChange}
            onStatusChange={handleStatusChange}
            onNavigate={setCurrentScreen}
          />
        )}

        {currentScreen === 'settings' && (
          <SettingsScreen
            settings={settings}
            onSettingsChange={setSettings}
            onBack={() => setCurrentScreen('home')}
          />
        )}

        {currentScreen === 'history' && (
          <HistoryScreen days={days} onBack={() => setCurrentScreen('home')} />
        )}

        {currentScreen === 'stats' && (
          <StatsScreen
            days={days}
            startDate={startDate}
            onBack={() => setCurrentScreen('home')}
          />
        )}
      </div>
    </div>
  );
}

function parseTime(time: string): [number, number] {
  if (time.includes('AM') || time.includes('PM')) {
    const [t, mod] = time.split(' ');
    let [h, m] = t.split(':').map(Number);
    if (mod === 'PM' && h < 12) h += 12;
    if (mod === 'AM' && h === 12) h = 0;
    return [h, m];
  }
  return time.split(':').map(Number) as [number, number];
}

export default App;
