import { ChevronDown, Settings, BarChart3, History, Bell, Calendar as CalendarIcon, Pill, FileText } from 'lucide-react';
import { DayData, PillType, ReminderSettings } from '@/types/pill-types';
import { SwipeableBlisterPacks } from './SwipeableBlisterPacks';
import { PillTypeModal } from './PillTypeModal';
import { CustomPillConfigModal } from './CustomPillConfigModal';
import { format } from 'date-fns';
import { useRef, useState } from 'react';

interface HomeScreenProps {
  pillType: PillType;
  startDate: Date;
  days: DayData[];
  settings: ReminderSettings;
  onPillTypeChange: (type: PillType) => void;
  onCustomPillConfigChange: (active: number, placebo: number, lowDose: number) => void;
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
  onCustomPillConfigChange,
  onStartDateChange,
  onStatusChange,
  onNavigate,
}: HomeScreenProps) {
  const [showPillTypeModal, setShowPillTypeModal] = useState(false);
  const [showCustomConfigModal, setShowCustomConfigModal] = useState(false);

  const dateInputRef = useRef<HTMLInputElement>(null);

  const handleStartDateEdit = () => {
    dateInputRef.current?.showPicker();
  };

  // Get display label for current pill type
  const getPillTypeLabel = () => {
    const labels: Record<PillType, string> = {
      '21+7': '21 Active + 7 Placebo',
      '24+4': '24 Active + 4 Placebo',
      '26+2': '26 Active + 2 Placebo',
      '28-day': '28-Day Continuous',
      '84+7': '84+7 (91-day)',
      '84+7-low': '84+7 Low-dose',
      '365-day': '365-Day Continuous',
      '28-pop': '28-Day POP',
      'flexible': 'Flexible Extended',
      'custom': 'Custom Configuration',
    };
    return labels[pillType] || pillType;
  };

  return (
    <div className="h-full flex flex-col bg-gradient-to-br from-pink-50 via-orange-50 to-yellow-50">
      {/* Hidden native date picker */}
      <input
        ref={dateInputRef}
        type="date"
        className="hidden"
        value={format(startDate, 'yyyy-MM-dd')}
        onChange={(e) => {
          const newDate = new Date(e.target.value);
          if (!isNaN(newDate.getTime())) {
            onStartDateChange(newDate);
          }
        }}
      />

      {/* Header - Fixed at top - FULLY RESPONSIVE */}
      <div
        className="flex-shrink-0 border-b"
        style={{
          background: 'linear-gradient(135deg, #f609bc, #fab86d)',
          borderColor: 'rgba(255, 255, 255, 0.3)',
        }}
      >
        <div className="max-w-2xl mx-auto px-3 sm:px-4 py-3 sm:py-4 md:py-5">
          <div className="text-center mb-3 sm:mb-4 md:mb-5">
            <h1 className="text-xl sm:text-2xl md:text-3xl font-bold text-white mb-1" style={{ textShadow: '0 2px 10px rgba(0, 0, 0, 0.2)' }}>
              Dosevia
            </h1>
            <p className="text-xs sm:text-sm text-white/90">Professional Pill Reminder</p>
          </div>

          {/* Pill Type Selector - RESPONSIVE */}
          <button
            onClick={() => setShowPillTypeModal(true)}
            className="w-full bg-white/95 backdrop-blur-sm rounded-xl px-3 sm:px-4 py-2.5 sm:py-3 border-2 border-white/50 shadow-lg hover:bg-white transition-colors active:scale-98 mb-2 sm:mb-3"
          >
            <div className="flex items-center gap-2 sm:gap-3">
              <div
                className="flex-shrink-0 w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center"
                style={{
                  background: 'linear-gradient(135deg, #f609bc, #fab86d)',
                }}
              >
                <Pill className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
              </div>
              <div className="flex-1 text-left min-w-0">
                <p className="text-xs font-medium mb-0.5 sm:mb-1 truncate" style={{ color: '#d007a0' }}>
                  Pill Type (Tap to change)
                </p>
                <p className="text-sm sm:text-base font-semibold text-gray-900 truncate">
                  {getPillTypeLabel()}
                </p>
              </div>
              <ChevronDown className="w-4 h-4 text-gray-400 transform -rotate-90 flex-shrink-0" />
            </div>
          </button>

          {/* Start Date - RESPONSIVE */}
          <button
            onClick={handleStartDateEdit}
            className="w-full bg-white/95 backdrop-blur-sm rounded-xl px-3 sm:px-4 py-2.5 sm:py-3 border-2 border-white/50 shadow-lg hover:bg-white transition-colors active:scale-98"
          >
            <div className="flex items-center gap-2 sm:gap-3">
              <div
                className="flex-shrink-0 w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center"
                style={{
                  background: 'linear-gradient(135deg, #f609bc, #fab86d)',
                }}
              >
                <CalendarIcon className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
              </div>
              <div className="flex-1 text-left min-w-0">
                <p className="text-xs font-medium mb-0.5 sm:mb-1 truncate" style={{ color: '#d007a0' }}>
                  Started (Tap to edit)
                </p>
                <p className="text-sm sm:text-base font-semibold text-gray-900 truncate">
                  {format(startDate, 'MMMM d, yyyy')}
                </p>
              </div>
              <ChevronDown className="w-4 h-4 text-gray-400 transform -rotate-90 flex-shrink-0" />
            </div>
          </button>
        </div>
      </div>

      {/* Main Content - Scrollable - FULLY RESPONSIVE */}
      <div className="flex-1 overflow-y-auto overflow-x-hidden">
        <div className="max-w-2xl mx-auto py-3 sm:py-4 md:py-6 px-2 sm:px-3">
          <SwipeableBlisterPacks pillType={pillType} days={days} onStatusChange={onStatusChange} />

          {/* Reminder Status - RESPONSIVE */}
          <div className="mt-4 sm:mt-6 md:mt-8 px-2 sm:px-0">
            <div
              className="rounded-xl shadow-lg p-3 sm:p-4 md:p-5 border-2"
              style={{
                background: 'linear-gradient(135deg, rgba(246, 9, 188, 0.1), rgba(250, 184, 109, 0.1))',
                borderColor: 'rgba(246, 9, 188, 0.2)',
              }}
            >
              <div className="flex items-center gap-2 sm:gap-3">
                <div
                  className="flex-shrink-0 w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center"
                  style={{
                    background: 'linear-gradient(135deg, #f609bc, #fab86d)',
                  }}
                >
                  <Bell className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-xs sm:text-sm font-medium truncate" style={{ color: '#d007a0' }}>
                    Next Reminder
                  </p>
                  <p className="text-sm sm:text-base font-semibold text-gray-900 truncate">
                    {settings.appActive
                      ? `Today at ${settings.dailyReminderTime}`
                      : 'Reminders disabled'}
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Quick Actions - RESPONSIVE 4-COLUMN GRID */}
          <div className="mt-3 sm:mt-4 md:mt-6 px-2 sm:px-0 grid grid-cols-4 gap-2 sm:gap-3 pb-4 sm:pb-6">
            <button
              onClick={() => onNavigate('settings')}
              className="bg-white rounded-xl shadow-lg p-2.5 sm:p-3 md:p-4 flex flex-col items-center justify-center gap-1.5 sm:gap-2 hover:shadow-xl transition-shadow active:scale-95"
            >
              <Settings className="w-5 h-5 sm:w-6 sm:h-6" style={{ color: '#f609bc' }} />
              <span className="text-xs sm:text-sm font-medium text-gray-700">Settings</span>
            </button>

            <button
              onClick={() => onNavigate('history')}
              className="bg-white rounded-xl shadow-lg p-2.5 sm:p-3 md:p-4 flex flex-col items-center justify-center gap-1.5 sm:gap-2 hover:shadow-xl transition-shadow active:scale-95"
            >
              <History className="w-5 h-5 sm:w-6 sm:h-6" style={{ color: '#f609bc' }} />
              <span className="text-xs sm:text-sm font-medium text-gray-700">History</span>
            </button>

            <button
              onClick={() => onNavigate('stats')}
              className="bg-white rounded-xl shadow-lg p-2.5 sm:p-3 md:p-4 flex flex-col items-center justify-center gap-1.5 sm:gap-2 hover:shadow-xl transition-shadow active:scale-95"
            >
              <BarChart3 className="w-5 h-5 sm:w-6 sm:h-6" style={{ color: '#f609bc' }} />
              <span className="text-xs sm:text-sm font-medium text-gray-700">Stats</span>
            </button>

            <button
              onClick={() => onNavigate('notes')}
              className="bg-white rounded-xl shadow-lg p-2.5 sm:p-3 md:p-4 flex flex-col items-center justify-center gap-1.5 sm:gap-2 hover:shadow-xl transition-shadow active:scale-95"
            >
              <FileText className="w-5 h-5 sm:w-6 sm:h-6" style={{ color: '#f609bc' }} />
              <span className="text-xs sm:text-sm font-medium text-gray-700">Notes</span>
            </button>
          </div>
        </div>
      </div>

      {/* Pill Type Modal */}
      <PillTypeModal
        isOpen={showPillTypeModal}
        currentType={pillType}
        onClose={() => setShowPillTypeModal(false)}
        onSelect={onPillTypeChange}
        onCustomSelect={() => setShowCustomConfigModal(true)}
      />

      {/* Custom Pill Configuration Modal */}
      <CustomPillConfigModal
        isOpen={showCustomConfigModal}
        onClose={() => setShowCustomConfigModal(false)}
        onSave={(active, placebo, lowDose) => {
          onCustomPillConfigChange(active, placebo, lowDose);
          onPillTypeChange('custom');
        }}
      />
    </div>
  );
}
