import { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from './ui/dialog';
import { Button } from './ui/button';
import { Pill, Check } from 'lucide-react';
import { format } from 'date-fns';

interface DailyPillReminderModalProps {
  isOpen: boolean;
  onTakePill: () => void;
  pillDay: number;
  pillDate: Date;
  isPlacebo: boolean;
}

export function DailyPillReminderModal({
  isOpen,
  onTakePill,
  pillDay,
  pillDate,
  isPlacebo,
}: DailyPillReminderModalProps) {
  const [isAnimating, setIsAnimating] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setIsAnimating(false);
      setShowSuccess(false);
    }
  }, [isOpen]);

  const handleTakePill = () => {
    setIsAnimating(true);
    
    // Show animation
    setTimeout(() => {
      setShowSuccess(true);
    }, 600);

    // Complete action
    setTimeout(() => {
      onTakePill();
    }, 1200);
  };

  return (
    <Dialog open={isOpen} onOpenChange={() => {}}>
      <DialogContent 
        className="sm:max-w-md"
        onPointerDownOutside={(e) => e.preventDefault()}
        onEscapeKeyDown={(e) => e.preventDefault()}
      >
        {!showSuccess ? (
          <>
            <DialogHeader>
              <div className="flex items-center justify-center mb-4">
                <div 
                  className={`w-20 h-20 rounded-full flex items-center justify-center transition-all duration-500 ${
                    isAnimating ? 'scale-110 rotate-12' : ''
                  }`}
                  style={{
                    background: isPlacebo 
                      ? 'linear-gradient(135deg, #9ca3af, #d1d5db)'
                      : 'linear-gradient(135deg, #f609bc, #fab86d)',
                  }}
                >
                  <Pill 
                    className={`w-10 h-10 text-white transition-all duration-500 ${
                      isAnimating ? 'scale-75 opacity-0' : ''
                    }`} 
                  />
                </div>
              </div>
              <DialogTitle className="text-center text-xl">
                Time to Take Your Pill! 💊
              </DialogTitle>
            </DialogHeader>
            
            <div className="py-6 text-center">
              <p className="text-lg font-semibold text-gray-800 mb-2">
                Day {pillDay} - {isPlacebo ? 'Placebo' : 'Active'}
              </p>
              <p className="text-sm text-gray-600 mb-4">
                {format(pillDate, 'EEEE, MMMM d, yyyy')}
              </p>
              <p className="text-gray-700">
                Please take your {isPlacebo ? 'placebo' : 'active'} pill now.
              </p>
            </div>

            <div className="flex flex-col gap-2">
              <Button
                onClick={handleTakePill}
                disabled={isAnimating}
                className="w-full bg-gradient-to-r from-pink-500 to-orange-400 hover:from-pink-600 hover:to-orange-500 text-white font-semibold py-6 text-lg"
              >
                {isAnimating ? (
                  <span className="flex items-center justify-center gap-2">
                    <span className="animate-spin">⏳</span> Taking Pill...
                  </span>
                ) : (
                  '✨ Take Pill Now'
                )}
              </Button>
            </div>
          </>
        ) : (
          <div className="py-12 text-center animate-in fade-in zoom-in duration-500">
            <div 
              className="w-24 h-24 mx-auto rounded-full flex items-center justify-center mb-6"
              style={{
                background: 'linear-gradient(135deg, #10b981, #059669)',
              }}
            >
              <Check className="w-12 h-12 text-white" strokeWidth={3} />
            </div>
            <h3 className="text-2xl font-bold text-gray-800 mb-2">
              Perfect! ✅
            </h3>
            <p className="text-gray-600">
              Pill marked as taken for today
            </p>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
