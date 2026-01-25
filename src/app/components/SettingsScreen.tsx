import { ArrowLeft, ChevronRight, Bell, ShoppingCart, Volume2, Vibrate, Download, Info, Star } from 'lucide-react';
import { ReminderSettings } from '@/types/pill-types';
import { motion } from 'motion/react';

interface SettingsScreenProps {
  settings: ReminderSettings;
  onSettingsChange: (settings: ReminderSettings) => void;
  onBack: () => void;
}

export function SettingsScreen({ settings, onSettingsChange, onBack }: SettingsScreenProps) {
  const updateSetting = <K extends keyof ReminderSettings>(key: K, value: ReminderSettings[K]) => {
    onSettingsChange({ ...settings, [key]: value });
  };

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
        <div className="max-w-2xl mx-auto px-4 py-4 flex items-center">
          <button
            onClick={onBack}
            className="mr-3 p-2 -ml-2 hover:bg-white/20 rounded-full transition-colors"
          >
            <ArrowLeft className="w-5 h-5 text-white" />
          </button>
          <h1 className="text-xl font-semibold text-white">Settings</h1>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-2xl mx-auto px-4 py-6 space-y-6 pb-20">
        {/* PILL & REMINDERS Section */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3 px-1">
            Pill & Reminders
          </h2>
          <div className="bg-white rounded-xl shadow-sm overflow-hidden divide-y divide-gray-100">
            {/* Placebo Reminder */}
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

            {/* Daily Reminder Time */}
            <SettingRow
              icon={<Bell className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="Daily Reminder Time"
              subtitle={settings.dailyReminderTime}
              onClick={() => {
                const newTime = prompt('Enter time (HH:MM format)', settings.dailyReminderTime);
                if (newTime) updateSetting('dailyReminderTime', newTime);
              }}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            {/* Pill Buying Reminder */}
            <SettingRow
              icon={<ShoppingCart className="w-5 h-5" style={{ color: '#fab86d' }} />}
              title="Pill Buying Reminder"
              subtitle={`${settings.pillBuyingDaysBefore} days before at ${settings.pillBuyingReminderTime}`}
              onClick={() => {
                const days = prompt('Days before pack ends', String(settings.pillBuyingDaysBefore));
                if (days) updateSetting('pillBuyingDaysBefore', parseInt(days));
              }}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            {/* App Active */}
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

        {/* NOTIFICATIONS Section */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3 px-1">
            Notifications
          </h2>
          <div className="bg-white rounded-xl shadow-sm overflow-hidden divide-y divide-gray-100">
            {/* Repeat Interval */}
            <SettingRow
              icon={<Bell className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="Repeat Interval"
              subtitle={`Every ${settings.repeatInterval} minutes`}
              onClick={() => {
                const interval = prompt('Repeat interval (minutes)', String(settings.repeatInterval));
                if (interval) updateSetting('repeatInterval', parseInt(interval));
              }}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            {/* Notification Sound */}
            <SettingRow
              icon={<Volume2 className="w-5 h-5" style={{ color: '#fab86d' }} />}
              title="Notification Sound"
              subtitle={settings.notificationSound}
              onClick={() => {
                const sound = prompt('Sound name', settings.notificationSound);
                if (sound) updateSetting('notificationSound', sound);
              }}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            {/* Play Sound Always */}
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

            {/* Vibrate Always */}
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

            {/* Notification Title */}
            <SettingRow
              icon={<Bell className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="Notification Title"
              subtitle={settings.notificationTitle}
              onClick={() => {
                const title = prompt('Notification title', settings.notificationTitle);
                if (title) updateSetting('notificationTitle', title);
              }}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            {/* Notification Subtitle */}
            <SettingRow
              icon={<Bell className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="Notification Subtitle"
              subtitle={settings.notificationSubtitle}
              onClick={() => {
                const subtitle = prompt('Notification subtitle', settings.notificationSubtitle);
                if (subtitle) updateSetting('notificationSubtitle', subtitle);
              }}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            {/* Notification Icon */}
            <SettingRow
              icon={<Bell className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="Notification Icon"
              subtitle={settings.notificationIcon}
              onClick={() => {
                const icon = prompt('Icon name', settings.notificationIcon);
                if (icon) updateSetting('notificationIcon', icon);
              }}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>
          </div>
        </section>

        {/* OTHER Section */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3 px-1">
            Other
          </h2>
          <div className="bg-white rounded-xl shadow-sm overflow-hidden divide-y divide-gray-100">
            {/* Backup & Restore */}
            <SettingRow
              icon={<Download className="w-5 h-5" style={{ color: '#f609bc' }} />}
              title="Backup & Restore"
              subtitle="Save and restore your data"
              onClick={() => alert('Backup & Restore functionality')}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            {/* About & Help */}
            <SettingRow
              icon={<Info className="w-5 h-5" style={{ color: '#fab86d' }} />}
              title="About & Help"
              subtitle="App info and support"
              onClick={() => alert('About & Help')}
            >
              <ChevronRight className="w-5 h-5 text-gray-400" />
            </SettingRow>

            {/* Rate App */}
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
    </div>
  );
}

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