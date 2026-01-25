import { ChevronDown, Settings, BarChart3, History, Bell, Calendar as CalendarIcon } from 'lucide-react';
import { DayData, PillType, ReminderSettings } from '@/types/pill-types';
import { CalendarBlisterPack } from './CalendarBlisterPack';
import { format } from 'date-fns';

interface HomeScreenProps {
  pillType: PillType;
  startDate: Date;
  days: DayData[];
  settings: ReminderSettings;
  onPillTypeChange: (type: PillType) => void;
  onStartDateChange: (date: Date) => void;
  onStatusChange: (day: number, status: any) => void;
  onNavigate: (screen: 'settings' | 'history' | 'stats') => void;
}

export function HomeScreen({
  pillType,
  startDate,
  days,
  settings,
  onPillTypeChange,
  onStartDateChange,
  onStatusChange,
  onNavigate,
}: HomeScreenProps) {
  const pillTypes: PillType[] = ['21+7', '24+4', '28-day'];

  const handleStartDateEdit = () => {
    const dateStr = prompt('Enter start date (YYYY-MM-DD format):', format(startDate, 'yyyy-MM-dd'));
    if (dateStr) {
      const newDate = new Date(dateStr);
      if (!isNaN(newDate.getTime())) {
        onStartDateChange(newDate);
      }
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-orange-50 to-yellow-50">
      {/* Header */}
      <div 
        className="border-b"
        style={{
          background: 'linear-gradient(135deg, #f609bc, #fab86d)',
          borderColor: 'rgba(255, 255, 255, 0.3)',
        }}
      >
        <div className="max-w-2xl mx-auto px-4 py-6">
          <div className="text-center mb-6">
            <h1 className="text-3xl font-bold text-white mb-1" style={{ textShadow: '0 2px 10px rgba(0, 0, 0, 0.2)' }}>
              Dosevia
            </h1>
            <p className="text-sm text-white/90">Professional Pill Reminder</p>
          </div>

          {/* Pill Type Selector */}
          <div className="mb-4">
            <label className="block text-xs font-semibold text-white/90 uppercase tracking-wide mb-2">
              Pill Type
            </label>
            <div className="relative">
              <select
                value={pillType}
                onChange={(e) => onPillTypeChange(e.target.value as PillType)}
                className="w-full appearance-none bg-white/95 backdrop-blur-sm border-2 border-white/50 rounded-xl px-4 py-3 pr-10 font-medium text-gray-900 focus:outline-none focus:ring-2 focus:ring-white focus:border-white shadow-lg"
              >
                {pillTypes.map((type) => (
                  <option key={type} value={type}>
                    {type === '21+7' && '21 Active + 7 Placebo'}
                    {type === '24+4' && '24 Active + 4 Placebo'}
                    {type === '28-day' && '28-Day Continuous'}
                  </option>
                ))}
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" />
            </div>
          </div>

          {/* Start Date - Now Editable */}
          <button
            onClick={handleStartDateEdit}
            className="w-full bg-white/95 backdrop-blur-sm rounded-xl px-4 py-3 border-2 border-white/50 shadow-lg hover:bg-white transition-colors active:scale-98"
          >
            <div className="flex items-center gap-3">
              <div 
                className="flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center"
                style={{
                  background: 'linear-gradient(135deg, #f609bc, #fab86d)',
                }}
              >
                <CalendarIcon className="w-5 h-5 text-white" />
              </div>
              <div className="flex-1 text-left">
                <p className="text-xs font-medium mb-1" style={{ color: '#d007a0' }}>
                  Started (Tap to edit)
                </p>
                <p className="font-semibold text-gray-900">{format(startDate, 'MMMM d, yyyy')}</p>
              </div>
              <ChevronDown className="w-4 h-4 text-gray-400 transform -rotate-90" />
            </div>
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-2xl mx-auto py-8">
        {/* Calendar Blister Pack */}
        <CalendarBlisterPack pillType={pillType} days={days} onStatusChange={onStatusChange} />

        {/* Reminder Status */}
        <div className="mt-8 px-4">
          <div 
            className="rounded-xl shadow-lg p-5 border-2"
            style={{
              background: 'linear-gradient(135deg, rgba(246, 9, 188, 0.1), rgba(250, 184, 109, 0.1))',
              borderColor: 'rgba(246, 9, 188, 0.2)',
            }}
          >
            <div className="flex items-center gap-3">
              <div 
                className="flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center"
                style={{
                  background: 'linear-gradient(135deg, #f609bc, #fab86d)',
                }}
              >
                <Bell className="w-5 h-5 text-white" />
              </div>
              <div className="flex-1">
                <p className="text-sm font-medium" style={{ color: '#d007a0' }}>
                  Next Reminder
                </p>
                <p className="font-semibold text-gray-900">
                  {settings.appActive
                    ? `Today at ${settings.dailyReminderTime}`
                    : 'Reminders disabled'}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="mt-6 px-4 grid grid-cols-3 gap-3">
          <button
            onClick={() => onNavigate('settings')}
            className="bg-white rounded-xl shadow-lg p-4 flex flex-col items-center gap-2 border-2 border-pink-100 hover:border-pink-300 active:scale-95 transition-all"
          >
            <div 
              className="w-10 h-10 rounded-full flex items-center justify-center"
              style={{
                background: 'linear-gradient(135deg, #f609bc, #d007a0)',
              }}
            >
              <Settings className="w-5 h-5 text-white" />
            </div>
            <span className="text-sm font-medium text-gray-700">Settings</span>
          </button>

          <button
            onClick={() => onNavigate('history')}
            className="bg-white rounded-xl shadow-lg p-4 flex flex-col items-center gap-2 border-2 border-orange-100 hover:border-orange-300 active:scale-95 transition-all"
          >
            <div 
              className="w-10 h-10 rounded-full flex items-center justify-center"
              style={{
                background: 'linear-gradient(135deg, #fab86d, #f59e0b)',
              }}
            >
              <History className="w-5 h-5 text-white" />
            </div>
            <span className="text-sm font-medium text-gray-700">History</span>
          </button>

          <button
            onClick={() => onNavigate('stats')}
            className="bg-white rounded-xl shadow-lg p-4 flex flex-col items-center gap-2 border-2 border-yellow-100 hover:border-yellow-300 active:scale-95 transition-all"
          >
            <div 
              className="w-10 h-10 rounded-full flex items-center justify-center"
              style={{
                background: 'linear-gradient(135deg, #f9f849, #eab308)',
              }}
            >
              <BarChart3 className="w-5 h-5 text-white" />
            </div>
            <span className="text-sm font-medium text-gray-700">Stats</span>
          </button>
        </div>
      </div>
    </div>
  );
}