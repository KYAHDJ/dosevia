import { useState, useRef, useEffect } from 'react';
import { DayData, PillType, PillStatus } from '@/types/pill-types';
import { CalendarBlisterPack } from './CalendarBlisterPack';
import { startOfDay } from 'date-fns';

interface SwipeableBlisterPacksProps {
  pillType: PillType;
  days: DayData[];
  onStatusChange: (day: number, status: PillStatus) => void;
}

export function SwipeableBlisterPacks({ pillType, days, onStatusChange }: SwipeableBlisterPacksProps) {
  const [currentPackIndex, setCurrentPackIndex] = useState(0);
  const [touchStart, setTouchStart] = useState<number | null>(null);
  const [touchEnd, setTouchEnd] = useState<number | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  
  // Determine how many pills per pack (28 pills = 1 blister pack)
  const pillsPerPack = 28;
  const totalPacks = Math.ceil(days.length / pillsPerPack);
  
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
  
  const goToPreviousPack = () => {
    if (currentPackIndex > 0) {
      setCurrentPackIndex(currentPackIndex - 1);
    }
  };
  
  const goToNextPack = () => {
    if (currentPackIndex < totalPacks - 1) {
      setCurrentPackIndex(currentPackIndex + 1);
    }
  };
  
  // Swipe detection with minimum distance
  const minSwipeDistance = 50;
  
  const onTouchStart = (e: React.TouchEvent) => {
    setTouchEnd(null);
    setTouchStart(e.targetTouches[0].clientX);
    setIsDragging(true);
  };
  
  const onTouchMove = (e: React.TouchEvent) => {
    setTouchEnd(e.targetTouches[0].clientX);
  };
  
  const onTouchEnd = () => {
    setIsDragging(false);
    if (!touchStart || !touchEnd) return;
    
    const distance = touchStart - touchEnd;
    const isLeftSwipe = distance > minSwipeDistance;
    const isRightSwipe = distance < -minSwipeDistance;
    
    if (isLeftSwipe && currentPackIndex < totalPacks - 1) {
      goToNextPack();
    }
    if (isRightSwipe && currentPackIndex > 0) {
      goToPreviousPack();
    }
  };
  
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
  
  // For multiple packs, show SWIPE-ONLY interface with visual feedback
  return (
    <div 
      ref={containerRef}
      className="relative w-full select-none"
      onTouchStart={onTouchStart}
      onTouchMove={onTouchMove}
      onTouchEnd={onTouchEnd}
    >
      {/* Pack indicator dots */}
      <div className="flex items-center justify-center gap-1.5 sm:gap-2 mb-3 sm:mb-4">
        {packs.map((_, index) => (
          <button
            key={index}
            onClick={() => setCurrentPackIndex(index)}
            className={`transition-all ${
              index === currentPackIndex
                ? 'w-6 h-2.5 sm:w-8 sm:h-3 rounded-full'
                : 'w-2 h-2 sm:w-2.5 sm:h-2.5 rounded-full opacity-40'
            }`}
            style={{
              background: index === currentPackIndex 
                ? 'linear-gradient(135deg, #f609bc, #fab86d)'
                : '#d1d5db',
            }}
            aria-label={`Go to pack ${index + 1}`}
          />
        ))}
      </div>
      
      {/* Pack title */}
      <div className="text-center mb-3 sm:mb-4 px-2">
        <h3 
          className="text-base sm:text-lg font-bold"
          style={{
            background: 'linear-gradient(135deg, #f609bc, #fab86d)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}
        >
          Pack {currentPackIndex + 1} of {totalPacks}
        </h3>
        <p className="text-xs sm:text-sm text-gray-600">
          Pills {currentPackIndex * pillsPerPack + 1}–{Math.min((currentPackIndex + 1) * pillsPerPack, days.length)}
        </p>
      </div>
      
      {/* Current pack - FULL WIDTH, SWIPE ONLY */}
      <div 
        className={`w-full transition-transform duration-300 ${isDragging ? 'cursor-grabbing' : 'cursor-grab'}`}
        style={{
          transform: isDragging && touchStart && touchEnd 
            ? `translateX(${touchEnd - touchStart}px)` 
            : 'translateX(0)',
        }}
      >
        <CalendarBlisterPack
          pillType={pillType}
          days={packs[currentPackIndex]}
          onStatusChange={onStatusChange}
        />
      </div>
      
      {/* Swipe instruction with visual indicators */}
      <div className="text-center mt-4 sm:mt-5 px-2">
        <div className="inline-flex items-center gap-2 sm:gap-3 text-xs sm:text-sm text-gray-500 bg-white/50 rounded-full px-3 sm:px-4 py-1.5 sm:py-2 border border-gray-200">
          {currentPackIndex > 0 && (
            <span className="animate-pulse">←</span>
          )}
          <span className="font-medium">Swipe to view other packs</span>
          {currentPackIndex < totalPacks - 1 && (
            <span className="animate-pulse">→</span>
          )}
        </div>
      </div>
      
      {/* Edge indicators - Show when can swipe */}
      {currentPackIndex > 0 && (
        <div 
          className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-16 sm:h-20 rounded-r-full opacity-50"
          style={{
            background: 'linear-gradient(135deg, #f609bc, #fab86d)',
          }}
        />
      )}
      {currentPackIndex < totalPacks - 1 && (
        <div 
          className="absolute right-0 top-1/2 -translate-y-1/2 w-1 h-16 sm:h-20 rounded-l-full opacity-50"
          style={{
            background: 'linear-gradient(135deg, #f609bc, #fab86d)',
          }}
        />
      )}
    </div>
  );
}
