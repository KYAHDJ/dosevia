import { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from './ui/dialog';
import { Button } from './ui/button';
import { Pill, Plus, Minus } from 'lucide-react';

interface CustomPillConfigModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (active: number, placebo: number, lowDose: number) => void;
}

export function CustomPillConfigModal({ isOpen, onClose, onSave }: CustomPillConfigModalProps) {
  const [activePills, setActivePills] = useState(21);
  const [placeboPills, setPlaceboPills] = useState(7);
  const [lowDosePills, setLowDosePills] = useState(0);

  const totalPills = activePills + placeboPills + lowDosePills;

  const handleSave = () => {
    if (activePills < 1) {
      alert('You must have at least 1 active pill');
      return;
    }
    if (totalPills < 1) {
      alert('Total pills must be at least 1');
      return;
    }
    onSave(activePills, placeboPills, lowDosePills);
    onClose();
  };

  const adjustValue = (
    current: number,
    delta: number,
    min: number,
    max: number
  ): number => {
    const newValue = current + delta;
    if (newValue < min) return min;
    if (newValue > max) return max;
    return newValue;
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
              <Pill className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
            </div>
            <DialogTitle className="text-base sm:text-xl font-semibold bg-gradient-to-r from-pink-600 to-orange-500 bg-clip-text text-transparent leading-tight">
              Custom Pill Configuration
            </DialogTitle>
          </div>
          <p className="text-xs sm:text-sm text-gray-600">Configure your pill pack exactly how you need it</p>
        </DialogHeader>

        <div className="space-y-3 sm:space-y-4 py-2 sm:py-4">
          {/* Active Pills */}
          <div className="bg-white rounded-xl p-3 sm:p-4 border-2 border-pink-200">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex-1 min-w-[140px]">
                <h3 className="text-sm sm:text-base font-semibold text-gray-900">Active Pills</h3>
                <p className="text-xs text-gray-600">Hormone-containing pills</p>
              </div>
              <div className="flex items-center gap-2 sm:gap-3 flex-shrink-0">
                <button
                  onClick={() => setActivePills(adjustValue(activePills, -1, 0, 365))}
                  className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center transition-colors active:scale-95"
                >
                  <Minus className="w-3 h-3 sm:w-4 sm:h-4 text-gray-700" />
                </button>
                <div 
                  className="w-12 h-9 sm:w-16 sm:h-10 rounded-lg flex items-center justify-center font-bold text-base sm:text-lg"
                  style={{
                    background: 'linear-gradient(135deg, #f609bc, #fab86d)',
                    color: 'white',
                  }}
                >
                  {activePills}
                </div>
                <button
                  onClick={() => setActivePills(adjustValue(activePills, 1, 0, 365))}
                  className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center transition-colors active:scale-95"
                >
                  <Plus className="w-3 h-3 sm:w-4 sm:h-4 text-gray-700" />
                </button>
              </div>
            </div>
          </div>

          {/* Placebo Pills */}
          <div className="bg-white rounded-xl p-3 sm:p-4 border-2 border-pink-200">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex-1 min-w-[140px]">
                <h3 className="text-sm sm:text-base font-semibold text-gray-900">Placebo Pills</h3>
                <p className="text-xs text-gray-600">No hormones (period week)</p>
              </div>
              <div className="flex items-center gap-2 sm:gap-3 flex-shrink-0">
                <button
                  onClick={() => setPlaceboPills(adjustValue(placeboPills, -1, 0, 28))}
                  className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center transition-colors active:scale-95"
                >
                  <Minus className="w-3 h-3 sm:w-4 sm:h-4 text-gray-700" />
                </button>
                <div className="w-12 h-9 sm:w-16 sm:h-10 rounded-lg bg-gray-400 flex items-center justify-center font-bold text-base sm:text-lg text-white">
                  {placeboPills}
                </div>
                <button
                  onClick={() => setPlaceboPills(adjustValue(placeboPills, 1, 0, 28))}
                  className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center transition-colors active:scale-95"
                >
                  <Plus className="w-3 h-3 sm:w-4 sm:h-4 text-gray-700" />
                </button>
              </div>
            </div>
          </div>

          {/* Low-Dose Pills */}
          <div className="bg-white rounded-xl p-3 sm:p-4 border-2 border-pink-200">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex-1 min-w-[140px]">
                <h3 className="text-sm sm:text-base font-semibold text-gray-900">Low-Dose Pills</h3>
                <p className="text-xs text-gray-600">Low hormone (instead of placebo)</p>
              </div>
              <div className="flex items-center gap-2 sm:gap-3 flex-shrink-0">
                <button
                  onClick={() => setLowDosePills(adjustValue(lowDosePills, -1, 0, 28))}
                  className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center transition-colors active:scale-95"
                >
                  <Minus className="w-3 h-3 sm:w-4 sm:h-4 text-gray-700" />
                </button>
                <div className="w-12 h-9 sm:w-16 sm:h-10 rounded-lg bg-amber-400 flex items-center justify-center font-bold text-base sm:text-lg text-white">
                  {lowDosePills}
                </div>
                <button
                  onClick={() => setLowDosePills(adjustValue(lowDosePills, 1, 0, 28))}
                  className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-gray-200 hover:bg-gray-300 flex items-center justify-center transition-colors active:scale-95"
                >
                  <Plus className="w-3 h-3 sm:w-4 sm:h-4 text-gray-700" />
                </button>
              </div>
            </div>
          </div>

          {/* Total Summary */}
          <div className="bg-gradient-to-r from-pink-50 to-orange-50 rounded-xl p-3 sm:p-4 border-2 border-pink-200">
            <div className="flex items-center justify-between">
              <span className="text-sm sm:text-base font-semibold text-gray-900">Total Pills:</span>
              <span 
                className="text-xl sm:text-2xl font-bold"
                style={{
                  background: 'linear-gradient(135deg, #f609bc, #fab86d)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                }}
              >
                {totalPills}
              </span>
            </div>
            <p className="text-xs text-gray-600 mt-1 sm:mt-2">
              {activePills} active + {placeboPills} placebo + {lowDosePills} low-dose
            </p>
          </div>

          {/* Info */}
          <div className="bg-blue-50 rounded-lg p-2.5 sm:p-3 border border-blue-200">
            <p className="text-xs text-blue-800">
              💡 <span className="font-semibold">Tip:</span> Set this to match your exact prescription. 
              Most packs are 21-28 pills total. Extended cycles can be up to 365 pills.
            </p>
          </div>
        </div>

        <DialogFooter className="gap-2 flex-col sm:flex-row pt-2">
          <Button
            variant="outline"
            onClick={onClose}
            className="w-full sm:w-auto border-gray-300 hover:bg-gray-50 text-sm sm:text-base h-10 sm:h-11"
          >
            Cancel
          </Button>
          <Button
            onClick={handleSave}
            className="w-full sm:w-auto text-white hover:opacity-90 text-sm sm:text-base h-10 sm:h-11"
            style={{
              background: 'linear-gradient(135deg, #f609bc, #fab86d)',
            }}
          >
            Save Configuration
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
