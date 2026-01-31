import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from './ui/dialog';
import { Button } from './ui/button';
import { Volume2, Settings, ArrowRight, Check } from 'lucide-react';
import { Capacitor } from '@capacitor/core';

interface NotificationSoundGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function NotificationSoundGuideModal({ isOpen, onClose }: NotificationSoundGuideModalProps) {
  
  const handleOpenSettings = async () => {
    try {
      if (Capacitor.getPlatform() === 'android') {
        // Import and register the plugin
        const { Capacitor: CapacitorCore } = await import('@capacitor/core');
        const NotificationSettings = CapacitorCore.registerPlugin('NotificationSettings');
        
        try {
          // Try to open the specific notification channel settings first
          await NotificationSettings.openChannelSettings({
            channelId: 'dosevia_reminders'
          });
          console.log('✅ Opened notification channel settings');
        } catch (channelError) {
          console.log('⚠️ Channel settings failed, trying app notification settings');
          
          // Fallback: Open general app notification settings
          try {
            await NotificationSettings.openNotificationSettings();
            console.log('✅ Opened app notification settings');
          } catch (appError) {
            console.error('❌ Both attempts failed:', appError);
            // This should never happen, but if it does, the plugin handles it
            throw appError;
          }
        }
        
        // Close the modal after opening settings
        setTimeout(() => {
          onClose();
        }, 500);
        
      } else {
        alert('⚠️ This feature is only available on Android');
      }
    } catch (error) {
      console.error('❌ Error opening notification settings:', error);
      // Don't show error to user - the plugin will open SOME settings page
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="w-[95vw] sm:w-full sm:max-w-lg bg-gradient-to-br from-pink-50/95 via-white to-orange-50/95 backdrop-blur-sm border-2 border-pink-200/50">
        <DialogHeader>
          <div className="flex items-center gap-3 mb-2">
            <div 
              className="w-12 h-12 rounded-full flex items-center justify-center"
              style={{
                background: 'linear-gradient(135deg, #f609bc, #fab86d)',
              }}
            >
              <Volume2 className="w-6 h-6 text-white" />
            </div>
            <DialogTitle className="text-xl font-semibold bg-gradient-to-r from-pink-600 to-orange-500 bg-clip-text text-transparent">
              Change Notification Sound
            </DialogTitle>
          </div>
        </DialogHeader>

        <div className="py-4 space-y-6">
          {/* Explanation */}
          <div className="bg-white rounded-xl p-4 border-2 border-pink-200 space-y-3">
            <p className="text-sm text-gray-700 leading-relaxed">
              To change your medication reminder sound, we'll open Android's notification settings where you can:
            </p>
            
            <div className="space-y-2 ml-2">
              <div className="flex items-start gap-2">
                <Check className="w-4 h-4 text-green-500 mt-0.5 flex-shrink-0" />
                <span className="text-sm text-gray-600">Choose from any sound on your phone</span>
              </div>
              <div className="flex items-start gap-2">
                <Check className="w-4 h-4 text-green-500 mt-0.5 flex-shrink-0" />
                <span className="text-sm text-gray-600">Adjust notification volume</span>
              </div>
              <div className="flex items-start gap-2">
                <Check className="w-4 h-4 text-green-500 mt-0.5 flex-shrink-0" />
                <span className="text-sm text-gray-600">Enable/disable vibration</span>
              </div>
              <div className="flex items-start gap-2">
                <Check className="w-4 h-4 text-green-500 mt-0.5 flex-shrink-0" />
                <span className="text-sm text-gray-600">Customize all notification settings</span>
              </div>
            </div>
          </div>

          {/* Step-by-step guide */}
          <div className="bg-gradient-to-r from-pink-50 to-orange-50 rounded-xl p-4 border-2 border-pink-200">
            <h3 className="text-sm font-semibold text-gray-800 mb-3 flex items-center gap-2">
              <Settings className="w-4 h-4" />
              What You'll Do:
            </h3>
            
            <div className="space-y-3">
              <div className="flex gap-3">
                <div 
                  className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white flex-shrink-0"
                  style={{ background: 'linear-gradient(135deg, #f609bc, #fab86d)' }}
                >
                  1
                </div>
                <p className="text-sm text-gray-700">
                  Click the button below to open settings
                </p>
              </div>
              
              <div className="flex gap-3">
                <div 
                  className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white flex-shrink-0"
                  style={{ background: 'linear-gradient(135deg, #f609bc, #fab86d)' }}
                >
                  2
                </div>
                <p className="text-sm text-gray-700">
                  Tap on <span className="font-semibold">"Sound"</span> or <span className="font-semibold">"Notification sound"</span>
                </p>
              </div>
              
              <div className="flex gap-3">
                <div 
                  className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white flex-shrink-0"
                  style={{ background: 'linear-gradient(135deg, #f609bc, #fab86d)' }}
                >
                  3
                </div>
                <p className="text-sm text-gray-700">
                  Choose your preferred sound from the list
                </p>
              </div>
              
              <div className="flex gap-3">
                <div 
                  className="w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold text-white flex-shrink-0"
                  style={{ background: 'linear-gradient(135deg, #f609bc, #fab86d)' }}
                >
                  4
                </div>
                <p className="text-sm text-gray-700">
                  Press the back button to return to the app
                </p>
              </div>
            </div>
          </div>

          {/* Important note */}
          <div className="bg-blue-50 rounded-lg p-3 border border-blue-200">
            <p className="text-xs text-blue-800">
              💡 <span className="font-semibold">Pro Tip:</span> This is the standard Android way to change notification sounds. 
              Any sound you select will be used for all medication reminders!
            </p>
          </div>
        </div>

        <DialogFooter className="gap-2 flex-col sm:flex-row">
          <Button
            variant="outline"
            onClick={onClose}
            className="w-full sm:w-auto border-gray-300 hover:bg-gray-50 order-2 sm:order-1"
          >
            Cancel
          </Button>
          <Button
            onClick={handleOpenSettings}
            className="w-full sm:w-auto text-white hover:opacity-90 order-1 sm:order-2"
            style={{
              background: 'linear-gradient(135deg, #f609bc, #fab86d)',
            }}
          >
            <Settings className="w-4 h-4 mr-2" />
            Open Settings
            <ArrowRight className="w-4 h-4 ml-2" />
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
