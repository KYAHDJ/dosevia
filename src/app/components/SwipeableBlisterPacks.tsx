import { useState, useRef, useEffect, useCallback } from 'react';
import { DayData, PillType, PillStatus } from '@/types/pill-types';
import { CalendarBlisterPack } from './CalendarBlisterPack';
import { startOfDay } from 'date-fns';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface SwipeableBlisterPacksProps {
  pillType: PillType;
  days: DayData[];
  onStatusChange: (day: number, status: PillStatus) => void;
}

export function SwipeableBlisterPacks({ pillType, days, onStatusChange }: SwipeableBlisterPacksProps) {
  const [currentPackIndex, setCurrentPackIndex] = useState(0);
  const [isTransitioning, setIsTransitioning] = useState(false);
  const [slideDirection, setSlideDirection] = useState<'left' | 'right' | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  
  // Determine how many pills per pack (28 pills = 1 blister pack)
  const pillsPerPack = 28;
  const totalPacks = Math.ceil(days.length / pillsPerPack);
  
  // Color variations for each pack to help distinguish them
  const packColors = [
    { bg: 'linear-gradient(135deg, #fef3f9 0%, #fef9ed 50%, #fefce8 100%)', accent: '#f609bc' }, // Pink-Orange-Yellow
    { bg: 'linear-gradient(135deg, #fef3f9 0%, #fce7f3 50%, #fef3f9 100%)', accent: '#db2777' }, // Pink-Rose
    { bg: 'linear-gradient(135deg, #fef9ed 0%, #fed7aa 50%, #fef9ed 100%)', accent: '#f97316' }, // Orange
    { bg: 'linear-gradient(135deg, #fefce8 0%, #fef3c7 50%, #fefce8 100%)', accent: '#eab308' }, // Yellow
    { bg: 'linear-gradient(135deg, #f0fdf4 0%, #dcfce7 50%, #f0fdf4 100%)', accent: '#22c55e' }, // Green
    { bg: 'linear-gradient(135deg, #eff6ff 0%, #dbeafe 50%, #eff6ff 100%)', accent: '#3b82f6' }, // Blue
    { bg: 'linear-gradient(135deg, #faf5ff 0%, #f3e8ff 50%, #faf5ff 100%)', accent: '#a855f7' }, // Purple
    { bg: 'linear-gradient(135deg, #fdf4ff 0%, #fae8ff 50%, #fdf4ff 100%)', accent: '#d946ef' }, // Fuchsia
  ];
  
  // Get color for current pack (cycles through colors if more than 8 packs)
  const getCurrentPackColor = (index: number) => packColors[index % packColors.length];
  
  // Split days into packs of 28
  const packs: DayData[][] = [];
  for (let i = 0; i < totalPacks; i++) {
    const start = i * pillsPerPack;
    const end = Math.min(start + pillsPerPack, days.length);
    packs.push(days.slice(start, end));
  }
  
  // Auto-navigate to pack containing today's pill on mount
  useEffect(() => {
    const today = startOfDay(new Date());
    const todayIndex = days.findIndex(d => {
      const pillDate = startOfDay(d.date);
      return pillDate.getTime() === today.getTime();
    });
    
    if (todayIndex >= 0) {
      const packIndex = Math.floor(todayIndex / pillsPerPack);
      setCurrentPackIndex(packIndex);
    }
  }, [days.length]); // Only run when days array changes length
  
  const goToPreviousPack = useCallback(() => {
    if (currentPackIndex > 0 && !isTransitioning) {
      setIsTransitioning(true);
      setSlideDirection('right');
      setCurrentPackIndex(currentPackIndex - 1);
      setTimeout(() => {
        setIsTransitioning(false);
        setSlideDirection(null);
      }, 400);
    }
  }, [currentPackIndex, isTransitioning]);
  
  const goToNextPack = useCallback(() => {
    if (currentPackIndex < totalPacks - 1 && !isTransitioning) {
      setIsTransitioning(true);
      setSlideDirection('left');
      setCurrentPackIndex(currentPackIndex + 1);
      setTimeout(() => {
        setIsTransitioning(false);
        setSlideDirection(null);
      }, 400);
    }
  }, [currentPackIndex, totalPacks, isTransitioning]);
  
  // For single pack (28 or less pills), just show regular calendar
  if (totalPacks <= 1) {
    return (
      <CalendarBlisterPack
        pillType={pillType}
        days={days}
        onStatusChange={onStatusChange}
      />
    );
  }
  
  // For multiple packs, show button navigation with smooth slide transitions
  return (
    <div className="relative w-full">
      {/* Pack indicator dots with colors - Now scrollable for many packs */}
      <div className="overflow-x-auto scrollbar-hide mb-3 sm:mb-4">
        <div className="flex items-center justify-center gap-1.5 sm:gap-2 min-w-max px-4">
          {packs.map((_, index) => {
            const packColor = getCurrentPackColor(index);
            return (
              <button
                key={index}
                onClick={() => {
                  if (!isTransitioning && index !== currentPackIndex) {
                    setIsTransitioning(true);
                    setSlideDirection(index > currentPackIndex ? 'left' : 'right');
                    setCurrentPackIndex(index);
                    setTimeout(() => {
                      setIsTransitioning(false);
                      setSlideDirection(null);
                    }, 400);
                  }
                }}
                className={`transition-all duration-300 ${
                  index === currentPackIndex
                    ? 'w-7 h-3 sm:w-9 sm:h-3.5 rounded-full scale-110'
                    : 'w-2 h-2 sm:w-2.5 sm:h-2.5 rounded-full opacity-40 hover:opacity-70 hover:scale-110'
                }`}
                style={{
                  background: index === currentPackIndex 
                    ? packColor.accent
                    : '#d1d5db',
                }}
                aria-label={`Go to pack ${index + 1}`}
              />
            );
          })}
        </div>
      </div>
      
      {/* Pack title with navigation arrows */}
      <div className="text-center mb-3 sm:mb-4 px-2 flex items-center justify-center gap-2 sm:gap-4">
        <button
          onClick={goToPreviousPack}
          disabled={currentPackIndex === 0 || isTransitioning}
          className={`p-1.5 sm:p-2 rounded-full transition-all ${
            currentPackIndex === 0 
              ? 'opacity-0 pointer-events-none' 
              : 'opacity-70 hover:opacity-100 active:scale-90'
          }`}
          style={{
            background: currentPackIndex === 0 ? 'transparent' : getCurrentPackColor(currentPackIndex).accent,
          }}
        >
          <ChevronLeft className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
        </button>
        
        <div className="flex-1">
          <h3 
            className="text-base sm:text-lg font-bold"
            style={{
              color: getCurrentPackColor(currentPackIndex).accent,
            }}
          >
            Pack {currentPackIndex + 1} of {totalPacks}
          </h3>
          <p className="text-xs sm:text-sm text-gray-600">
            Pills {currentPackIndex * pillsPerPack + 1}–{Math.min((currentPackIndex + 1) * pillsPerPack, days.length)}
          </p>
        </div>
        
        <button
          onClick={goToNextPack}
          disabled={currentPackIndex === totalPacks - 1 || isTransitioning}
          className={`p-1.5 sm:p-2 rounded-full transition-all ${
            currentPackIndex === totalPacks - 1 
              ? 'opacity-0 pointer-events-none' 
              : 'opacity-70 hover:opacity-100 active:scale-90'
          }`}
          style={{
            background: currentPackIndex === totalPacks - 1 ? 'transparent' : getCurrentPackColor(currentPackIndex).accent,
          }}
        >
          <ChevronRight className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
        </button>
      </div>
      
      {/* Smooth slide container - button navigation only */}
      <div 
        ref={containerRef}
        className="relative w-full overflow-hidden"
      >
        {/* Current pack with smooth slide transition */}
        <div 
          className="w-full"
          style={{
            transform: isTransitioning 
              ? slideDirection === 'left' 
                ? 'translateX(-100%)' 
                : 'translateX(100%)'
              : 'translateX(0)',
            opacity: isTransitioning ? 0 : 1,
            transition: isTransitioning 
              ? 'transform 0.4s cubic-bezier(0.4, 0.0, 0.2, 1), opacity 0.4s cubic-bezier(0.4, 0.0, 0.2, 1)' 
              : 'none',
          }}
        >
          <CalendarBlisterPack
            pillType={pillType}
            days={packs[currentPackIndex]}
            onStatusChange={onStatusChange}
            packColor={getCurrentPackColor(currentPackIndex).bg}
          />
        </div>
      </div>
      
      {/* Navigation instruction */}
      <div className="text-center mt-4 sm:mt-5 px-2">
        <div className="inline-flex items-center gap-2 sm:gap-3 text-xs sm:text-sm text-gray-500 bg-white/70 backdrop-blur-sm rounded-full px-3 sm:px-4 py-1.5 sm:py-2 border border-gray-200 shadow-sm">
          {currentPackIndex > 0 && (
            <span className="animate-pulse" style={{ color: getCurrentPackColor(currentPackIndex).accent }}>←</span>
          )}
          <span className="font-medium">Use arrows to navigate packs</span>
          {currentPackIndex < totalPacks - 1 && (
            <span className="animate-pulse" style={{ color: getCurrentPackColor(currentPackIndex).accent }}>→</span>
          )}
        </div>
      </div>
      
      {/* Edge indicators with pack colors */}
      {currentPackIndex > 0 && (
        <div 
          className="absolute left-0 top-1/2 -translate-y-1/2 w-1.5 h-20 sm:h-24 rounded-r-full opacity-60 animate-pulse"
          style={{
            background: getCurrentPackColor(currentPackIndex).accent,
            boxShadow: `0 0 10px ${getCurrentPackColor(currentPackIndex).accent}40`,
          }}
        />
      )}
      {currentPackIndex < totalPacks - 1 && (
        <div 
          className="absolute right-0 top-1/2 -translate-y-1/2 w-1.5 h-20 sm:h-24 rounded-l-full opacity-60 animate-pulse"
          style={{
            background: getCurrentPackColor(currentPackIndex).accent,
            boxShadow: `0 0 10px ${getCurrentPackColor(currentPackIndex).accent}40`,
          }}
        />
      )}
    </div>
  );
}
