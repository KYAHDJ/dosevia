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
import { addDays, isSameDay } from 'date-fns';
import { Preferences } from '@capacitor/preferences';
import { 
  initNotifications, 
  scheduleDailyAlarm, 
  cancelAllAlarms,
  checkAndMarkMissedPills 
} from '@/app/lib/notifications';

type Screen = 'home' | 'settings' | 'history' | 'stats';

const STORAGE_KEY = 'dosevia-app-state';
const STORAGE_VERSION = '2.0'; // Increment when changing storage structure

function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');
  const [pillType, setPillType] = useState<PillType>('21+7');
  const [startDate, setStartDate] = useState(new Date());
  const [days, setDays] = useState<DayData[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [settings, setSettings] = useState<ReminderSettings>({
    placeboReminder: true,
    dailyReminderTime: '9:00 PM',
    pillBuyingDaysBefore: 7,
    pillBuyingReminderTime: '9:00 AM',
    appActive: true,
    repeatInterval: 30, // Start at 30 seconds
    notificationSound: 'Default',
    soundFileUri: '', // New: Custom sound file URI
    playSoundAlways: true,
    vibrateAlways: true,
    notificationTitle: 'Time to take your pill',
    notificationSubtitle: 'Don't forget your daily dose',
    notificationIcon: 'pill',
  });

  /* INIT NOTIFICATIONS */
  useEffect(() => {
    initNotifications();
  }, []);

  /* LOAD SAVED STATE FROM LOCAL STORAGE */
  useEffect(() => {
    (async () => {
      try {
        console.log('🔄 Loading saved state...');
        const { value } = await Preferences.get({ key: STORAGE_KEY });
        
        if (!value) {
          console.log('❌ No saved data found, using defaults');
          setIsLoading(false);
          return;
        }

        const saved = JSON.parse(value);
        console.log('📦 Loaded data:', saved);
        
        // Version check - if version mismatch, migrate or reset
        if (saved.version !== STORAGE_VERSION) {
          console.log('⚠️ Storage version mismatch, migrating data...');
          // Could add migration logic here
        }

        // Restore pill type
        if (saved.pillType) {
          console.log('✅ Restoring pillType:', saved.pillType);
          setPillType(saved.pillType);
        }

        // Restore start date
        if (saved.startDate) {
          console.log('✅ Restoring startDate:', saved.startDate);
          setStartDate(new Date(saved.startDate));
        }

        // Restore days with proper date parsing
        if (saved.days && Array.isArray(saved.days)) {
          const restoredDays = saved.days.map((day: any) => ({
            ...day,
            date: new Date(day.date), // Parse date string back to Date object
          }));
          console.log('✅ Restoring days:', restoredDays.length, 'pills');
          console.log('📊 Day statuses:', restoredDays.map((d: any) => `Day ${d.day}: ${d.status}`));
          setDays(restoredDays);
          
          // Check for missed pills on app load
          checkAndMarkMissedPills(restoredDays, setDays);
        }

        // Restore settings
        if (saved.settings) {
          console.log('✅ Restoring settings');
          setSettings({
            ...settings,
            ...saved.settings,
          });
        }

        console.log('✅ State loaded successfully from local storage');
      } catch (error) {
        console.error('❌ Error loading saved state:', error);
      } finally {
        setIsLoading(false);
      }
    })();
  }, []);

  /* SAVE STATE TO LOCAL STORAGE */
  useEffect(() => {
    if (isLoading) {
      console.log('⏸️ Skipping save - still loading');
      return; // Don't save during initial load
    }

    const saveState = async () => {
      try {
        const stateToSave = {
          version: STORAGE_VERSION,
          pillType,
          startDate: startDate.toISOString(), // Convert to ISO string for storage
          days: days.map(day => ({
            ...day,
            date: day.date.toISOString(), // Convert dates to ISO strings
          })),
          settings,
          lastSaved: new Date().toISOString(),
        };

        await Preferences.set({
          key: STORAGE_KEY,
          value: JSON.stringify(stateToSave),
        });

        console.log('💾 State saved to local storage');
        console.log('📊 Saved day statuses:', days.map(d => `Day ${d.day}: ${d.status}`).slice(0, 5).join(', '));
      } catch (error) {
        console.error('❌ Error saving state:', error);
      }
    };

    saveState();
  }, [pillType, startDate, days, settings, isLoading]);

  /* SCHEDULE DAILY ALARM - Enhanced with pill status tracking */
  useEffect(() => {
    if (!settings.appActive) {
      // Cancel all alarms if app is disabled
      cancelAllAlarms();
      return;
    }

    const [hour, minute] = parseTime(settings.dailyReminderTime);

    // Find today's pill
    const today = new Date();
    const todayPill = days.find(day => isSameDay(day.date, today));

    // Schedule alarm with pill status awareness
    scheduleDailyAlarm(
      hour,
      minute,
      settings.notificationTitle,
      settings.notificationSubtitle,
      settings.vibrateAlways,
      settings.soundFileUri || 'default',
      todayPill?.status === 'taken' // Pass if already taken
    );
  }, [
    settings.appActive,
    settings.dailyReminderTime,
    settings.notificationTitle,
    settings.notificationSubtitle,
    settings.vibrateAlways,
    settings.soundFileUri,
    days, // Re-schedule when day status changes
  ]);

  /* INIT DAYS - Only runs if NO saved data exists */
  useEffect(() => {
    // Don't run during initial load
    if (isLoading) return;
    
    // Don't reset if we have saved days
    if (days.length > 0) return;

    // Only create new days if truly empty
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

    setDays(newDays);
  }, [isLoading]); // Only depend on loading state!

  /* HANDLE PILL STATUS CHANGE - Enhanced with notification cancellation */
  const handleStatusChange = (day: number, status: PillStatus) => {
    console.log(`🔄 Changing Day ${day} status to: ${status}`);
    
    setDays((currentDays) =>
      currentDays.map((x) => {
        if (x.day === day) {
          const updatedDay = { 
            ...x, 
            status,
            takenAt: status === 'taken' ? new Date().toISOString() : undefined,
          };

          console.log(`✅ Day ${day} updated:`, updatedDay);

          // If marked as taken and it's today's pill, cancel escalating alarms
          const today = new Date();
          if (status === 'taken' && isSameDay(x.date, today)) {
            console.log('✅ Pill marked as taken - canceling alarms');
            cancelAllAlarms();
          }

          return updatedDay;
        }
        return x;
      })
    );
  };

  // TODO: Cloud Save Preparation
  // Function to sync with cloud storage
  const syncToCloud = async () => {
    // Implement cloud sync here (Firebase, Supabase, etc.)
    // This is a placeholder for future implementation
    console.log('🌐 Cloud sync would happen here');
  };

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
            onStartDateChange={setStartDate}
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

/* HELPERS */
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
