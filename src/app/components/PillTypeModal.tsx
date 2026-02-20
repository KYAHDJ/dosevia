import { Dialog, DialogContent, DialogHeader, DialogTitle } from './ui/dialog';
import { Pill, Check, Info } from 'lucide-react';
import { PillType } from '@/types/pill-types';

interface PillTypeModalProps {
  isOpen: boolean;
  currentType: PillType;
  onClose: () => void;
  onSelect: (type: PillType) => void;
  onCustomSelect: () => void; // Trigger custom config modal
}

const pillTypeOptions: { 
  value: PillType; 
  label: string; 
  description: string;
  category: string;
  brands?: string;
}[] = [
  // STANDARD 28-DAY CYCLES
  {
    value: '21+7',
    label: '21 Active + 7 Placebo',
    description: 'Most common traditional pack. Take hormone pills for 21 days, then placebo for 7 days. Period occurs during placebo week.',
    category: 'Standard Cycle',
    brands: 'Ortho Tri-Cyclen, Apri, Yasmin',
  },
  {
    value: '24+4',
    label: '24 Active + 4 Placebo',
    description: 'Shorter placebo interval reduces withdrawal symptoms. Take active pills for 24 days, placebo for 4 days.',
    category: 'Standard Cycle',
    brands: 'Yaz, Beyaz, Slynd',
  },
  {
    value: '26+2',
    label: '26 Active + 2 Placebo',
    description: 'Very short placebo break. Take active pills for 26 days, placebo for only 2 days. Minimal withdrawal bleeding.',
    category: 'Standard Cycle',
  },
  {
    value: '28-day',
    label: '28-Day Continuous',
    description: 'All 28 pills are active. No placebo week, no periods. Continuous hormone protection.',
    category: 'Standard Cycle',
    brands: 'Minipill variants',
  },
  
  // EXTENDED CYCLE REGIMENS
  {
    value: '84+7',
    label: '84 Active + 7 Placebo (91-Day)',
    description: '3-month extended cycle. Take active pills for 84 days (12 weeks), then placebo for 7 days. Period only 4 times per year.',
    category: 'Extended Cycle',
    brands: 'Seasonale, Jolessa, Quasense',
  },
  {
    value: '84+7-low',
    label: '84 Active + 7 Low-Dose (91-Day)',
    description: '3-month cycle with low-dose estrogen instead of placebo. Better hormone stability, fewer withdrawal symptoms.',
    category: 'Extended Cycle',
    brands: 'Seasonique, Camrese, LoSeasonique',
  },
  {
    value: '365-day',
    label: '365-Day Continuous',
    description: 'Year-round continuous active pills. No placebo, no periods. Take one active pill every day of the year.',
    category: 'Extended Cycle',
    brands: 'Lybrel, Amethyst',
  },
  
  // PROGESTIN-ONLY
  {
    value: '28-pop',
    label: '28-Day Progestin-Only (Minipill)',
    description: 'All 28 pills contain only progestin. Must take at same time daily (within 3-hour window). Safe for breastfeeding.',
    category: 'Progestin-Only',
    brands: 'Nor-QD, Camila, Errin',
  },
  
  // FLEXIBLE/CUSTOM
  {
    value: 'flexible',
    label: 'Flexible Extended Cycle',
    description: 'You control cycle length. Take active pills for 24-120 days, then take 4-day break when ready. Reduces breakthrough bleeding.',
    category: 'Flexible Regimen',
  },
  {
    value: 'custom',
    label: 'Custom Configuration',
    description: 'For special regimens prescribed by your healthcare provider. Contact us for custom setup.',
    category: 'Custom',
  },
];

export function PillTypeModal({ isOpen, currentType, onClose, onSelect, onCustomSelect }: PillTypeModalProps) {
  const handleSelect = (type: PillType) => {
    if (type === 'custom') {
      // Close this modal and open custom config modal
      onClose();
      onCustomSelect();
    } else {
      onSelect(type);
      onClose();
    }
  };

  // Group options by category
  const categories = Array.from(new Set(pillTypeOptions.map(opt => opt.category)));

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="w-[95vw] sm:w-full sm:max-w-2xl bg-gradient-to-br from-pink-50/95 via-white to-orange-50/95 backdrop-blur-sm border-2 border-pink-200/50 max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center gap-3 mb-2">
            <div 
              className="w-10 h-10 rounded-full flex items-center justify-center"
              style={{
                background: 'linear-gradient(135deg, #f609bc, #fab86d)',
              }}
            >
              <Pill className="w-5 h-5 text-white" />
            </div>
            <DialogTitle className="text-xl font-semibold bg-gradient-to-r from-pink-600 to-orange-500 bg-clip-text text-transparent">
              Select Your Pill Pack Type
            </DialogTitle>
          </div>
          <p className="text-sm text-gray-600">Choose the regimen that matches your prescription</p>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {categories.map((category) => {
            const categoryOptions = pillTypeOptions.filter(opt => opt.category === category);
            
            return (
              <div key={category}>
                <h3 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
                  <Info className="w-4 h-4" />
                  {category}
                </h3>
                <div className="space-y-2">
                  {categoryOptions.map((option) => {
                    const isSelected = currentType === option.value;
                    
                    return (
                      <button
                        key={option.value}
                        onClick={() => handleSelect(option.value)}
                        className={`w-full text-left p-4 rounded-xl border-2 transition-all ${
                          isSelected
                            ? 'border-pink-400 bg-gradient-to-r from-pink-50 to-orange-50 shadow-md'
                            : 'border-pink-200 bg-white hover:border-pink-300 hover:shadow-sm'
                        }`}
                      >
                        <div className="flex items-start gap-3">
                          <div 
                            className={`flex-shrink-0 w-6 h-6 rounded-full border-2 flex items-center justify-center mt-0.5 transition-all ${
                              isSelected
                                ? 'border-pink-500 bg-gradient-to-r from-pink-500 to-orange-500'
                                : 'border-gray-300 bg-white'
                            }`}
                          >
                            {isSelected && <Check className="w-4 h-4 text-white" />}
                          </div>
                          
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-1">
                              <h3 className={`font-semibold ${
                                isSelected
                                  ? 'bg-gradient-to-r from-pink-600 to-orange-500 bg-clip-text text-transparent'
                                  : 'text-gray-900'
                              }`}>
                                {option.label}
                              </h3>
                            </div>
                            <p className="text-xs text-gray-600 leading-relaxed mb-2">
                              {option.description}
                            </p>
                            {option.brands && (
                              <p className="text-xs text-pink-600 font-medium">
                                Examples: {option.brands}
                              </p>
                            )}
                          </div>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>

        <div className="pt-2 space-y-2">
          <div className="bg-blue-50 rounded-lg p-3 border border-blue-200">
            <p className="text-xs text-blue-800">
              💡 <span className="font-semibold">Medical Accuracy:</span> All pill types are based on actual birth control regimens 
              prescribed worldwide. Choose the one that matches your prescription.
            </p>
          </div>
          <div className="bg-amber-50 rounded-lg p-3 border border-amber-200">
            <p className="text-xs text-amber-800">
              ⚠️ <span className="font-semibold">Important:</span> Always follow your healthcare provider's instructions. 
              If unsure which type you have, check your pill pack or consult your doctor.
            </p>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
