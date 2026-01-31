import { motion } from 'motion/react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from './ui/dialog';
import { 
  Pill, 
  Bell, 
  Heart, 
  Star, 
  Calendar,
  AlertCircle,
  CheckCircle,
  Clock,
  Zap,
  Activity,
  Coffee,
  Sun,
  Moon,
  Sparkles,
  CircleDot,
  type LucideIcon
} from 'lucide-react';

interface IconPickerProps {
  isOpen: boolean;
  currentIcon: string;
  onClose: () => void;
  onSelect: (iconEmoji: string) => void;
}

interface IconOption {
  emoji: string;
  icon: LucideIcon;
  label: string;
}

const ICON_OPTIONS: IconOption[] = [
  { emoji: '💊', icon: Pill, label: 'Pill' },
  { emoji: '🔔', icon: Bell, label: 'Bell' },
  { emoji: '❤️', icon: Heart, label: 'Heart' },
  { emoji: '⭐', icon: Star, label: 'Star' },
  { emoji: '📅', icon: Calendar, label: 'Calendar' },
  { emoji: '⚠️', icon: AlertCircle, label: 'Alert' },
  { emoji: '✅', icon: CheckCircle, label: 'Check' },
  { emoji: '⏰', icon: Clock, label: 'Clock' },
  { emoji: '⚡', icon: Zap, label: 'Zap' },
  { emoji: '📊', icon: Activity, label: 'Activity' },
  { emoji: '☕', icon: Coffee, label: 'Coffee' },
  { emoji: '☀️', icon: Sun, label: 'Sun' },
  { emoji: '🌙', icon: Moon, label: 'Moon' },
  { emoji: '✨', icon: Sparkles, label: 'Sparkles' },
  { emoji: '⭕', icon: CircleDot, label: 'Circle' },
];

export function IconPicker({ isOpen, currentIcon, onClose, onSelect }: IconPickerProps) {
  const handleSelect = (iconEmoji: string) => {
    console.log('🎨 Icon selected:', iconEmoji);
    onSelect(iconEmoji);
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="w-[95vw] sm:w-full sm:max-w-lg bg-gradient-to-br from-pink-50/95 via-white to-orange-50/95 backdrop-blur-sm border-2 border-pink-200/50 max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center gap-2 sm:gap-3 mb-2">
            <div 
              className="w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center flex-shrink-0"
              style={{
                background: 'linear-gradient(135deg, #f609bc, #fab86d)',
              }}
            >
              <Bell className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
            </div>
            <DialogTitle className="text-base sm:text-xl font-semibold bg-gradient-to-r from-pink-600 to-orange-500 bg-clip-text text-transparent">
              Choose Notification Icon
            </DialogTitle>
          </div>
          <p className="text-xs sm:text-sm text-gray-600">Select an icon for your notifications</p>
        </DialogHeader>

        <div className="py-3 sm:py-4">
          <div className="grid grid-cols-3 sm:grid-cols-5 gap-2 sm:gap-3">
            {ICON_OPTIONS.map(({ emoji, icon: Icon, label }) => {
              const isSelected = currentIcon === emoji;
              return (
                <motion.button
                  key={emoji}
                  onClick={() => handleSelect(emoji)}
                  className="flex flex-col items-center justify-center p-2 sm:p-3 rounded-xl transition-all"
                  style={{
                    background: isSelected 
                      ? 'linear-gradient(135deg, #f609bc, #fab86d)'
                      : '#f3f4f6',
                    border: isSelected ? '2px solid #f609bc' : '2px solid transparent',
                  }}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                >
                  <Icon 
                    className="w-5 h-5 sm:w-6 sm:h-6 mb-1" 
                    style={{ color: isSelected ? '#ffffff' : '#6b7280' }}
                  />
                  <span 
                    className="text-[10px] sm:text-xs font-medium truncate w-full text-center"
                    style={{ color: isSelected ? '#ffffff' : '#6b7280' }}
                  >
                    {label}
                  </span>
                </motion.button>
              );
            })}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
