import { ArrowLeft, ChevronRight, Bell, ShoppingCart, Volume2, Vibrate, Download, Info, Star, Clock } from 'lucide-react';
import { ReminderSettings } from '@/types/pill-types';
import { motion } from 'motion/react';
import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from './ui/dialog';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { IconPicker } from './IconPicker';
import { NotificationSoundGuideModal } from './NotificationSoundGuideModal';

interface SettingsScreenProps {
  settings: ReminderSettings;
  onSettingsChange: (settings: ReminderSettings) => void;
  onBack: () => void;
}

type ModalType = 
  | 'notification-title'
  | 'notification-subtitle'
  | 'notification-icon'
  | 'daily-time'
  | 'buying-time'
  | 'buying-days'
  | null;

export function SettingsScreen({ settings, onSettingsChange, onBack }: SettingsScreenProps) {
  const [activeModal, setActiveModal] = useState<ModalType>(null);
  const [tempValue, setTempValue] = useState<string>('');
  const [showIconPicker, setShowIconPicker] = useState(false);
  const [showSoundGuide, setShowSoundGuide] = useState(false);
  const updateSetting = <K extends keyof ReminderSettings>(key: K, value: ReminderSettings[K]) => {
    onSettingsChange({ ...settings, [key]: value });
  };

  const openModal = (type: ModalType, currentValue: string | number) => {
    setTempValue(String(currentValue));
    setActiveModal(type);
  };

  const closeModal = () => {
    setActiveModal(null);
    setTempValue('');
  };

  const handleSave = () => {
    if (!activeModal) return;

    switch (activeModal) {
      case 'notification-title':
        updateSetting('notificationTitle', tempValue);
        break;
      case 'notification-subtitle':
        updateSetting('notificationSubtitle', tempValue);
        break;
      case 'daily-time':
        updateSetting('dailyReminderTime', tempValue);
        break;
      case 'buying-time':
        updateSetting('pillBuyingReminderTime', tempValue);
        break;
      case 'buying-days':
        updateSetting('pillBuyingDaysBefore', parseInt(tempValue) || 7);
        break;
    }
    closeModal();
  };

  const getModalConfig = () => {
    switch (activeModal) {
      case 'notification-title':
        return {
          title: 'Notification Title',
          description: 'Customize your notification title',
          inputType: 'text' as const,
          label: 'Title',
          placeholder: 'Time to take your pill',
        };
      case 'notification-subtitle':
        return {
          title: 'Notification Subtitle',
          description: 'Add a subtitle to your notification',
          inputType: 'text' as const,
          label: 'Subtitle',
          placeholder: "Don't forget your daily dose",
        };
      case 'daily-time':
        return {
          title: 'Daily Reminder Time',
          description: 'Set your daily pill reminder time',
          inputType: 'time' as const,
          label: 'Time',
          placeholder: '09:00',
        };
      case 'buying-time':
        return {
          title: 'Pill Buying Reminder Time',
          description: 'Set when to remind you to buy pills',
          inputType: 'time' as const,
          label: 'Time',
          placeholder: '09:00',
        };
      case 'buying-days':
        return {
          title: 'Pill Buying Days Before',
          description: 'How many days before running out?',
          inputType: 'number' as const,
          label: 'Days Before',
          placeholder: '7',
        };
      default:
        return null;
    }
  };

  const modalConfig = getModalConfig();

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-orange-50 to-yellow-50">
      {/* Header */}
      <div
        className="border-b sticky top-0 z-10"
        style={{
          background: 'linear-gradient(135deg, #f609bc, #fab86d)',
          borderColor: 'rgba(255, 255, 255, 0.3)',
        }}
      >
        <div className="max-w-2xl mx-auto px-3 sm:px-4 py-3 sm:py-4 flex items-center">
          <button
            onClick={onBack}
            className="mr-2 sm:mr-3 p-2 -ml-2 hover:bg-white/20 rounded-full transition-colors"
          >
            <ArrowLeft className="w-5 h-5 text-white" />
          </button>
          <h1 className="text-lg sm:text-xl font-semibold text-white">Settings</h1>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-2xl mx-auto px-3 sm:px-4 py-4 sm:py-6 space-y-4 sm:space-y-6 pb-20">{/* Rest of content */}

        {/* PILL & REMINDERS */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3 px-1">
            Pill & Reminders
          </h2>
          <div className="bg-white rounded-xl shadow-sm overflow-hidden divide-y divide-gray-100">

            <SettingRow
              icon={<Bell className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="Placebo Reminder"
              subtitle="Get reminded during placebo days"
            >
              <Toggle
                enabled={settings.placeboReminder}
                onChange={(val) => updateSetting('placeboReminder', val)}
              />
            </SettingRow>

            <SettingRow
              icon={<Clock className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="Daily Reminder Time"
              subtitle={settings.dailyReminderTime}
              onClick={() => openModal('daily-time', settings.dailyReminderTime)}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            <SettingRow
              icon={<ShoppingCart className="w-5 h-5" style={{ color: '#fab86d' }} />}
              title="Pill Buying Reminder"
              subtitle={`${settings.pillBuyingDaysBefore} days before at ${settings.pillBuyingReminderTime}`}
              onClick={() => openModal('buying-days', settings.pillBuyingDaysBefore)}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            <SettingRow
              icon={<Bell className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="App Active"
              subtitle="Enable all reminders"
            >
              <Toggle
                enabled={settings.appActive}
                onChange={(val) => updateSetting('appActive', val)}
              />
            </SettingRow>

          </div>
        </section>

        {/* NOTIFICATIONS */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3 px-1">
            Notifications
          </h2>
          <div className="bg-white rounded-xl shadow-sm overflow-hidden divide-y divide-gray-100">

            <SettingRow
              icon={<Volume2 className="w-5 h-5" style={{ color: '#fab86d' }} />}
              title="Notification Sound"
              subtitle="Configure sound in Android settings"
              onClick={() => setShowSoundGuide(true)}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            <SettingRow
              icon={<Volume2 className="w-5 h-5" style={{ color: '#fab86d' }} />}
              title="Play Sound Always"
              subtitle="Override silent mode"
            >
              <Toggle
                enabled={settings.playSoundAlways}
                onChange={(val) => updateSetting('playSoundAlways', val)}
              />
            </SettingRow>

            <SettingRow
              icon={<Vibrate className="w-5 h-5" style={{ color: '#f9f849' }} />}
              title="Vibrate Always"
              subtitle="Vibrate with notification"
            >
              <Toggle
                enabled={settings.vibrateAlways}
                onChange={(val) => updateSetting('vibrateAlways', val)}
              />
            </SettingRow>

            <SettingRow
              icon={<Bell className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="Notification Title"
              subtitle={settings.notificationTitle}
              onClick={() => openModal('notification-title', settings.notificationTitle)}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            <SettingRow
              icon={<Bell className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="Notification Subtitle"
              subtitle={settings.notificationSubtitle}
              onClick={() => openModal('notification-subtitle', settings.notificationSubtitle)}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            <SettingRow
              icon={<Bell className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="Notification Icon"
              subtitle={settings.notificationIcon}
              onClick={() => setShowIconPicker(true)}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

          </div>
        </section>

        {/* OTHER */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3 px-1">
            Other
          </h2>
          <div className="bg-white rounded-xl shadow-sm overflow-hidden divide-y divide-gray-100">

            <SettingRow
              icon={<Download className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="Backup & Restore"
              subtitle="Save and restore your data"
              onClick={() => alert('Backup & Restore functionality')}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            <SettingRow
              icon={<Info className="w-5 h-5" style={{ color: '#fab86d' }} />}
              title="About & Help"
              subtitle="App info and support"
              onClick={() => alert('About & Help')}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            <SettingRow
              icon={<Star className="w-5 h-5" style={{ color: '#f9f849' }} />}
              title="Rate App"
              subtitle="Share your feedback"
              onClick={() => alert('Rate App')}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

          </div>
        </section>

      </div>

      {/* Icon Picker */}
      <IconPicker
        isOpen={showIconPicker}
        currentIcon={settings.notificationIcon}
        onClose={() => setShowIconPicker(false)}
        onSelect={(iconName) => {
          updateSetting('notificationIcon', iconName);
          console.log('🎨 Icon changed to:', iconName);
        }}
      />

      {/* Notification Sound Guide Modal */}
      <NotificationSoundGuideModal
        isOpen={showSoundGuide}
        onClose={() => setShowSoundGuide(false)}
      />

      {/* Professional Modal */}
      <Dialog open={activeModal !== null} onOpenChange={(open) => !open && closeModal()}>
        <DialogContent className="w-[95vw] sm:w-full sm:max-w-md bg-gradient-to-br from-pink-50/95 via-white to-orange-50/95 backdrop-blur-sm border-2 border-pink-200/50 max-h-[90vh] overflow-y-auto">
          {modalConfig && (
            <>
              <DialogHeader>
                <div className="flex items-center gap-2 sm:gap-3 mb-2">
                  <div 
                    className="w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center flex-shrink-0"
                    style={{
                      background: 'linear-gradient(135deg, #f609bc, #fab86d)',
                    }}
                  >
                    {activeModal?.includes('time') ? (
                      <Clock className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
                    ) : (
                      <Bell className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
                    )}
                  </div>
                  <DialogTitle className="text-base sm:text-xl font-semibold bg-gradient-to-r from-pink-600 to-orange-500 bg-clip-text text-transparent">
                    {modalConfig.title}
                  </DialogTitle>
                </div>
                <p className="text-xs sm:text-sm text-gray-600">{modalConfig.description}</p>
              </DialogHeader>

              <div className="space-y-3 sm:space-y-4 py-3 sm:py-4">
                <div className="space-y-2">
                  <label className="text-xs sm:text-sm font-medium text-gray-700">
                    {modalConfig.label}
                  </label>
                  <Input
                    type={modalConfig.inputType}
                    value={tempValue}
                    onChange={(e) => setTempValue(e.target.value)}
                    placeholder={modalConfig.placeholder}
                    className="flex-1 border-pink-200 focus:border-pink-400 focus:ring-pink-400/30 text-base"
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleSave();
                      }
                    }}
                  />
                </div>
              </div>

              <DialogFooter className="gap-2 flex-col sm:flex-row">
                <Button
                  variant="outline"
                  onClick={closeModal}
                  className="w-full sm:w-auto border-gray-300 hover:bg-gray-50 order-2 sm:order-1"
                >
                  Cancel
                </Button>
                <Button
                  onClick={handleSave}
                  style={{
                    background: 'linear-gradient(135deg, #f609bc, #fab86d)',
                  }}
                  className="w-full sm:w-auto text-white hover:opacity-90 order-1 sm:order-2"
                >
                  Save
                </Button>
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}

/* ===== Helpers unchanged ===== */

interface SettingRowProps {
  icon: React.ReactNode;
  title: string;
  subtitle: string;
  onClick?: () => void;
  children?: React.ReactNode;
}

function SettingRow({ icon, title, subtitle, onClick, children }: SettingRowProps) {
  return (
    <motion.div
      className={`flex items-center gap-3 px-4 py-4 ${onClick ? 'cursor-pointer active:bg-gray-50' : ''}`}
      onClick={onClick}
      whileTap={onClick ? { scale: 0.98 } : {}}
    >
      <div className="flex-shrink-0">{icon}</div>
      <div className="flex-1 min-w-0">
        <p className="font-medium text-gray-900">{title}</p>
        <p className="text-sm text-gray-500 truncate">{subtitle}</p>
      </div>
      <div className="flex-shrink-0">{children}</div>
    </motion.div>
  );
}

interface ToggleProps {
  enabled: boolean;
  onChange: (enabled: boolean) => void;
}

function Toggle({ enabled, onChange }: ToggleProps) {
  return (
    <button
      onClick={(e) => {
        e.stopPropagation();
        onChange(!enabled);
      }}
      style={{
        background: enabled ? 'linear-gradient(135deg, #f609bc, #fab86d)' : '#d1d5db',
      }}
      className="relative inline-flex h-7 w-12 items-center rounded-full transition-colors"
    >
      <motion.span
        className="inline-block h-5 w-5 transform rounded-full bg-white shadow-lg"
        animate={{ x: enabled ? 26 : 2 }}
        transition={{ type: 'spring', stiffness: 500, damping: 30 }}
      />
    </button>
  );
}
