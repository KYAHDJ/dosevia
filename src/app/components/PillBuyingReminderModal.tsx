import { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from './ui/dialog';
import { Button } from './ui/button';
import { ShoppingCart, Clock, X } from 'lucide-react';

interface PillBuyingReminderModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirmPurchased: () => void;
  onSnooze: () => void;
}

export function PillBuyingReminderModal({
  isOpen,
  onClose,
  onConfirmPurchased,
  onSnooze,
}: PillBuyingReminderModalProps) {
  const [isClosing, setIsClosing] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setIsClosing(false);
    }
  }, [isOpen]);

  const handleClose = (callback: () => void) => {
    setIsClosing(true);
    setTimeout(() => {
      callback();
      setIsClosing(false);
    }, 200);
  };

  return (
    <Dialog open={isOpen} onOpenChange={() => {}}>
      <DialogContent 
        className="sm:max-w-md"
        onPointerDownOutside={(e) => e.preventDefault()}
        onEscapeKeyDown={(e) => e.preventDefault()}
      >
        <DialogHeader>
          <div className="flex items-center justify-center mb-4">
            <div 
              className="w-16 h-16 rounded-full flex items-center justify-center"
              style={{
                background: 'linear-gradient(135deg, #f609bc, #fab86d)',
              }}
            >
              <ShoppingCart className="w-8 h-8 text-white" />
            </div>
          </div>
          <DialogTitle className="text-center text-xl">
            Time to Buy More Pills!
          </DialogTitle>
        </DialogHeader>
        
        <div className="py-6 text-center">
          <p className="text-gray-700 mb-4">
            Have you purchased your next pack of pills?
          </p>
          <p className="text-sm text-gray-500">
            It's important to ensure you don't run out of your medication.
          </p>
        </div>

        <DialogFooter className="flex flex-col gap-2 sm:flex-col">
          <Button
            onClick={() => handleClose(onConfirmPurchased)}
            className="w-full bg-gradient-to-r from-green-500 to-green-600 hover:from-green-600 hover:to-green-700 text-white"
            disabled={isClosing}
          >
            ✅ Yes, I Bought Them
          </Button>
          <Button
            onClick={() => handleClose(onSnooze)}
            variant="outline"
            className="w-full"
            disabled={isClosing}
          >
            <Clock className="w-4 h-4 mr-2" />
            Not Yet - Remind Me in 1 Hour
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
