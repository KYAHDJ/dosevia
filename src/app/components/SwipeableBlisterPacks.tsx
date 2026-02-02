import { useState, useRef, useEffect } from 'react';
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
  const [touchStart, setTouchStart] = useState<number | null>(null);
  const [touchEnd, setTouchEnd] = useState<number | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [dragOffset, setDragOffset] = useState(0);
  const [isTransitioning, setIsTransitioning] = useState(false);
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
    if (currentPackIndex > 0 && !isTransitioning) {
      setIsTransitioning(true);
      setCurrentPackIndex(currentPackIndex - 1);
      setTimeout(() => setIsTransitioning(false), 300);
    }
  };
  
  const goToNextPack = () => {
    if (currentPackIndex < totalPacks - 1 && !isTransitioning) {
      setIsTransitioning(true);
      setCurrentPackIndex(currentPackIndex + 1);
      setTimeout(() => setIsTransitioning(false), 300);
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
    if (!touchStart) return;
    const currentTouch = e.targetTouches[0].clientX;
    setTouchEnd(currentTouch);
    
    // Calculate drag offset with bounds
    const offset = currentTouch - touchStart;
    const maxOffset = 100; // Max pixels to drag
    const boundedOffset = Math.max(-maxOffset, Math.min(maxOffset, offset));
    setDragOffset(boundedOffset);
  };
  
  const onTouchEnd = () => {
    setIsDragging(false);
    setDragOffset(0);
    
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
  
  // For multiple packs, show enhanced SWIPE interface with smooth visual feedback
  return (
    <div className="relative w-full">
      {/* Pack indicator dots - Now scrollable for many packs */}
      <div className="overflow-x-auto scrollbar-hide mb-3 sm:mb-4">
        <div className="flex items-center justify-center gap-1.5 sm:gap-2 min-w-max px-4">
          {packs.map((_, index) => (
            <button
              key={index}
              onClick={() => {
                if (!isTransitioning) {
                  setIsTransitioning(true);
                  setCurrentPackIndex(index);
                  setTimeout(() => setIsTransitioning(false), 300);
                }
              }}
              className={`transition-all duration-300 ${
                index === currentPackIndex
                  ? 'w-7 h-3 sm:w-9 sm:h-3.5 rounded-full scale-110'
                  : 'w-2 h-2 sm:w-2.5 sm:h-2.5 rounded-full opacity-40 hover:opacity-70 hover:scale-110'
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
      </div>
      
      {/* Pack title with navigation arrows */}
      <div className="text-center mb-3 sm:mb-4 px-2 flex items-center justify-center gap-2 sm:gap-4">
        <button
          onClick={goToPreviousPack}
          disabled={currentPackIndex === 0 || isTransitioning}
          className={`p-1.5 sm:p-2 rounded-full transition-all ${
            currentPackIndex === 0 
              ? 'opacity-0 pointer-events-none' 
              : 'opacity-60 hover:opacity-100 active:scale-90'
          }`}
          style={{
            background: currentPackIndex === 0 ? 'transparent' : 'linear-gradient(135deg, #f609bc, #fab86d)',
          }}
        >
          <ChevronLeft className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
        </button>
        
        <div className="flex-1">
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
        
        <button
          onClick={goToNextPack}
          disabled={currentPackIndex === totalPacks - 1 || isTransitioning}
          className={`p-1.5 sm:p-2 rounded-full transition-all ${
            currentPackIndex === totalPacks - 1 
              ? 'opacity-0 pointer-events-none' 
              : 'opacity-60 hover:opacity-100 active:scale-90'
          }`}
          style={{
            background: currentPackIndex === totalPacks - 1 ? 'transparent' : 'linear-gradient(135deg, #f609bc, #fab86d)',
          }}
        >
          <ChevronRight className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
        </button>
      </div>
      
      {/* Swipeable container with smooth feedback */}
      <div 
        ref={containerRef}
        className="relative w-full select-none overflow-hidden"
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
        onTouchEnd={onTouchEnd}
      >
        {/* Current pack with drag transform */}
        <div 
          className={`w-full ${
            isDragging ? 'cursor-grabbing' : 'cursor-grab'
          } ${
            isTransitioning ? 'transition-all duration-300 ease-out' : ''
          }`}
          style={{
            transform: isDragging 
              ? `translateX(${dragOffset}px) scale(${1 - Math.abs(dragOffset) / 1000})` 
              : 'translateX(0) scale(1)',
            opacity: isDragging ? 1 - Math.abs(dragOffset) / 300 : 1,
          }}
        >
          <CalendarBlisterPack
            pillType={pillType}
            days={packs[currentPackIndex]}
            onStatusChange={onStatusChange}
          />
        </div>
        
        {/* Preview of next/previous pack while dragging */}
        {isDragging && dragOffset < -20 && currentPackIndex < totalPacks - 1 && (
          <div 
            className="absolute inset-0 pointer-events-none"
            style={{
              transform: `translateX(${100 + (dragOffset / 2)}%)`,
              opacity: Math.abs(dragOffset) / 100,
            }}
          >
            <div className="w-full h-full bg-gradient-to-l from-pink-50 to-transparent rounded-lg" />
          </div>
        )}
        {isDragging && dragOffset > 20 && currentPackIndex > 0 && (
          <div 
            className="absolute inset-0 pointer-events-none"
            style={{
              transform: `translateX(${-100 + (dragOffset / 2)}%)`,
              opacity: Math.abs(dragOffset) / 100,
            }}
          >
            <div className="w-full h-full bg-gradient-to-r from-pink-50 to-transparent rounded-lg" />
          </div>
        )}
      </div>
      
      {/* Enhanced swipe instruction */}
      <div className="text-center mt-4 sm:mt-5 px-2">
        <div className="inline-flex items-center gap-2 sm:gap-3 text-xs sm:text-sm text-gray-500 bg-white/70 backdrop-blur-sm rounded-full px-3 sm:px-4 py-1.5 sm:py-2 border border-gray-200 shadow-sm">
          {currentPackIndex > 0 && (
            <span className="animate-pulse text-pink-500">←</span>
          )}
          <span className="font-medium">Swipe or tap arrows</span>
          {currentPackIndex < totalPacks - 1 && (
            <span className="animate-pulse text-pink-500">→</span>
          )}
        </div>
      </div>
      
      {/* Enhanced edge indicators with gradient */}
      {currentPackIndex > 0 && (
        <div 
          className="absolute left-0 top-1/2 -translate-y-1/2 w-1.5 h-20 sm:h-24 rounded-r-full opacity-60 animate-pulse"
          style={{
            background: 'linear-gradient(135deg, #f609bc, #fab86d)',
            boxShadow: '0 0 10px rgba(246, 9, 188, 0.3)',
          }}
        />
      )}
      {currentPackIndex < totalPacks - 1 && (
        <div 
          className="absolute right-0 top-1/2 -translate-y-1/2 w-1.5 h-20 sm:h-24 rounded-l-full opacity-60 animate-pulse"
          style={{
            background: 'linear-gradient(135deg, #f609bc, #fab86d)',
            boxShadow: '0 0 10px rgba(246, 9, 188, 0.3)',
          }}
        />
      )}
    </div>
  );
}
