import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from './ui/dialog';
import { Button } from './ui/button';
import { Volume2, Settings, ArrowRight, Check } from 'lucide-react';
import { Capacitor, registerPlugin } from '@capacitor/core';

// Define the plugin interface
interface NotificationSettingsPlugin {
  openNotificationSettings(): Promise<{ success: boolean; opened: boolean }>;
}

// Register the plugin (connects to native code registered in MainActivity)
const NotificationSettings = registerPlugin<NotificationSettingsPlugin>('NotificationSettings');

interface NotificationSoundGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function NotificationSoundGuideModal({ isOpen, onClose }: NotificationSoundGuideModalProps) {
  
  const handleOpenSettings = async () => {
    console.log('🔘 Attempting to open settings...');
    
    try {
      const platform = Capacitor.getPlatform();
      console.log('📱 Platform:', platform);
      
      if (platform === 'android' || platform === 'ios') {
        try {
          // Try the native plugin - DON'T close modal
          const result = await NotificationSettings.openNotificationSettings();
          console.log('✅ Settings opened:', result);
          // Modal stays open so user can follow the guide
          
        } catch (pluginError) {
          console.log('⚠️ Plugin failed', pluginError);
          // Still don't close - user might want to try again
        }
      } else {
        console.log('ℹ️ Not on mobile platform');
      }
    } catch (error) {
      console.log('ℹ️ Error opening settings:', error);
      // Don't close modal - let user try again or close manually
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="w-[90vw] max-w-[500px] bg-gradient-to-br from-pink-50/95 via-white to-orange-50/95 backdrop-blur-sm border-2 border-pink-200/50 max-h-[85vh] overflow-y-auto overflow-x-hidden">
        <DialogHeader>
          <div className="flex items-center gap-2 sm:gap-3 mb-2">
            <div 
              className="w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center flex-shrink-0"
              style={{
                background: 'linear-gradient(135deg, #f609bc, #fab86d)',
              }}
            >
              <Volume2 className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
            </div>
            <DialogTitle className="text-base sm:text-xl font-semibold bg-gradient-to-r from-pink-600 to-orange-500 bg-clip-text text-transparent leading-tight">
              Alarm Settings
            </DialogTitle>
          </div>
          <p className="text-xs sm:text-sm text-gray-600">Configure sound and vibration for your medication alarms</p>
        </DialogHeader>

        <div className="py-2 sm:py-4 space-y-3 sm:space-y-4">
          {/* Important Notice */}
          <div className="bg-yellow-50 rounded-xl p-3 sm:p-4 border-2 border-yellow-300">
            <p className="text-xs sm:text-sm font-semibold text-yellow-900 mb-1 sm:mb-2">
              ⚠️ Sound & Vibration Settings
            </p>
            <p className="text-xs text-yellow-800 leading-relaxed">
              Alarm sound AND vibration are both controlled together in your phone's settings. 
              You cannot change them separately within this app.
            </p>
          </div>

          {/* Step-by-step guide */}
          <div className="bg-gradient-to-r from-pink-50 to-orange-50 rounded-xl p-3 sm:p-5 border-2 border-pink-300">
            <h3 className="text-sm sm:text-base font-bold text-gray-900 mb-3 sm:mb-4 flex items-center gap-2">
              <Settings className="w-4 h-4 sm:w-5 sm:h-5" />
              Follow These Steps:
            </h3>
            
            <div className="space-y-3 sm:space-y-4">
              <div className="flex gap-2 sm:gap-3 items-start">
                <div 
                  className="w-6 h-6 sm:w-8 sm:h-8 rounded-full flex items-center justify-center text-xs sm:text-sm font-bold text-white flex-shrink-0"
                  style={{ background: 'linear-gradient(135deg, #f609bc, #fab86d)' }}
                >
                  1
                </div>
                <div className="flex-1">
                  <p className="text-sm sm:text-base font-semibold text-gray-900 leading-tight">
                    Tap "Open Settings"
                  </p>
                  <p className="text-xs sm:text-sm text-gray-600 mt-0.5 sm:mt-1">
                    Opens Dosevia's settings page
                  </p>
                </div>
              </div>
              
              <div className="flex gap-2 sm:gap-3 items-start">
                <div 
                  className="w-6 h-6 sm:w-8 sm:h-8 rounded-full flex items-center justify-center text-xs sm:text-sm font-bold text-white flex-shrink-0"
                  style={{ background: 'linear-gradient(135deg, #f609bc, #fab86d)' }}
                >
                  2
                </div>
                <div className="flex-1">
                  <p className="text-sm sm:text-base font-semibold text-gray-900 leading-tight">
                    Tap "Notifications"
                  </p>
                  <p className="text-xs sm:text-sm text-gray-600 mt-0.5 sm:mt-1">
                    Find and tap the Notifications option
                  </p>
                </div>
              </div>
              
              <div className="flex gap-2 sm:gap-3 items-start">
                <div 
                  className="w-6 h-6 sm:w-8 sm:h-8 rounded-full flex items-center justify-center text-xs sm:text-sm font-bold text-white flex-shrink-0"
                  style={{ background: 'linear-gradient(135deg, #f609bc, #fab86d)' }}
                >
                  3
                </div>
                <div className="flex-1">
                  <p className="text-sm sm:text-base font-semibold text-gray-900 leading-tight">
                    Tap "Alarms" channel
                  </p>
                  <p className="text-xs sm:text-sm text-gray-600 mt-0.5 sm:mt-1">
                    Look for the "Alarms" notification channel
                  </p>
                </div>
              </div>
              
              <div className="flex gap-2 sm:gap-3 items-start">
                <div 
                  className="w-6 h-6 sm:w-8 sm:h-8 rounded-full flex items-center justify-center text-xs sm:text-sm font-bold text-white flex-shrink-0"
                  style={{ background: 'linear-gradient(135deg, #f609bc, #fab86d)' }}
                >
                  4
                </div>
                <div className="flex-1">
                  <p className="text-sm sm:text-base font-semibold text-gray-900 leading-tight">
                    Change Sound & Vibration
                  </p>
                  <p className="text-xs sm:text-sm text-gray-600 mt-0.5 sm:mt-1">
                    Tap "Sound" to choose alarm sound<br/>
                    Toggle "Vibrate" to enable/disable vibration
                  </p>
                </div>
              </div>

              <div className="flex gap-2 sm:gap-3 items-start">
                <div 
                  className="w-6 h-6 sm:w-8 sm:h-8 rounded-full flex items-center justify-center text-xs sm:text-sm font-bold text-white flex-shrink-0"
                  style={{ background: 'linear-gradient(135deg, #f609bc, #fab86d)' }}
                >
                  5
                </div>
                <div className="flex-1">
                  <p className="text-sm sm:text-base font-semibold text-gray-900 leading-tight">
                    Done! Come back
                  </p>
                  <p className="text-xs sm:text-sm text-gray-600 mt-0.5 sm:mt-1">
                    Return to Dosevia after making changes
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Helpful tips */}
          <div className="bg-blue-50 rounded-lg p-3 sm:p-4 border-2 border-blue-200">
            <p className="text-xs sm:text-sm font-semibold text-blue-900 mb-1 sm:mb-2">
              💡 Helpful Tips:
            </p>
            <ul className="text-xs text-blue-800 space-y-1 sm:space-y-1.5 ml-3 sm:ml-4">
              <li className="list-disc">Choose a LOUD alarm sound so you don't miss your medication</li>
              <li className="list-disc">Test the sound before confirming your choice</li>
              <li className="list-disc">Enable vibration if you want physical alerts</li>
              <li className="list-disc">These settings apply to ALL medication reminders</li>
            </ul>
          </div>
        </div>

        <DialogFooter className="gap-2 flex-col sm:flex-row pt-2">
          <Button
            onClick={handleOpenSettings}
            className="w-full sm:w-auto text-white hover:opacity-90 order-1 text-sm sm:text-base h-9 sm:h-10"
            style={{
              background: 'linear-gradient(135deg, #f609bc, #fab86d)',
            }}
          >
            <Settings className="w-4 h-4 mr-2" />
            Open Settings
            <ArrowRight className="w-4 h-4 ml-2" />
          </Button>
          <Button
            variant="outline"
            onClick={onClose}
            className="w-full sm:w-auto border-gray-300 hover:bg-gray-50 order-2 text-sm sm:text-base h-9 sm:h-10"
          >
            Close Guide
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
