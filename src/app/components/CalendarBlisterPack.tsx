import { useState } from 'react';
import { DayData, PillType, PillStatus } from '@/types/pill-types';
import { CalendarPillBubble } from './CalendarPillBubble';
import { DayModal } from './DayModal';
import { PillTakenAnimation } from './PillTakenAnimation';
import { ChevronDown } from 'lucide-react';
import { format, startOfWeek, addDays, startOfDay } from 'date-fns';

interface CalendarBlisterPackProps {
  pillType: PillType;
  days: DayData[];
  onStatusChange: (day: number, status: PillStatus) => void;
}

export function CalendarBlisterPack({ pillType, days, onStatusChange }: CalendarBlisterPackProps) {
  const [selectedDay, setSelectedDay] = useState<DayData | null>(null);
  const [puncturingDay, setPuncturingDay] = useState<number | null>(null);
  const [animatingDay, setAnimatingDay] = useState<DayData | null>(null);

  // Find TODAY's pill (by comparing dates)
  const today = startOfDay(new Date());
  const todayPillIndex = days.findIndex(d => {
    const pillDate = startOfDay(d.date);
    return pillDate.getTime() === today.getTime();
  });
  const currentDay = todayPillIndex >= 0 ? todayPillIndex + 1 : -1;

  const handleDayClick = (dayData: DayData) => {
    const pillDate = startOfDay(dayData.date);
    const todayDate = startOfDay(new Date());
    
    // Prevent editing future pills
    if (pillDate > todayDate) {
      console.log(`🚫 Cannot edit Day ${dayData.day} - it's in the future (${dayData.date.toDateString()})`);
      // Could show a toast/alert here
      return;
    }
    
    setSelectedDay(dayData);
  };

  const handleStatusChange = (day: number, status: PillStatus) => {
    if (status === 'taken') {
      // Start the multi-stage animation - ALWAYS FULLSCREEN
      const dayData = days.find(d => d.day === day);
      if (dayData) {
        // Stage 1: Puncture the blister
        setPuncturingDay(day);
        
        // Stage 2: Show fullscreen animation (ALWAYS, regardless of pill count)
        setTimeout(() => {
          setPuncturingDay(null);
          setAnimatingDay(dayData);
        }, 300);
      }
    } else {
      onStatusChange(day, status);
    }
  };

  const handleAnimationComplete = () => {
    if (animatingDay) {
      onStatusChange(animatingDay.day, 'taken');
      setAnimatingDay(null);
    }
  };

  // Organize days into 4 columns (weeks)
  const columns: DayData[][] = [[], [], [], []];
  days.forEach((day, index) => {
    const columnIndex = Math.floor(index / 7);
    if (columnIndex < 4) {
      columns[columnIndex].push(day);
    }
  });

  // Get month range for display
  const getMonthRange = () => {
    if (days.length === 0) return '';
    const firstMonth = format(days[0].date, 'MMMM');
    const lastMonth = format(days[days.length - 1].date, 'MMMM');
    return firstMonth === lastMonth ? firstMonth : `${firstMonth} - ${lastMonth}`;
  };

  return (
    <>
      <div className="w-full mx-auto px-2 sm:px-3">
        {/* Blister pack container - RESPONSIVE */}
        <div
          className="relative rounded-2xl sm:rounded-3xl p-3 sm:p-4 md:p-5 shadow-xl sm:shadow-2xl overflow-hidden"
          style={{
            background: 'linear-gradient(135deg, #fef3f9 0%, #fef9ed 50%, #fefce8 100%)',
            boxShadow: '0 10px 40px rgba(246, 9, 188, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.5)',
          }}
        >
          {/* Foil texture overlay */}
          <div
            className="absolute inset-0 opacity-20 pointer-events-none"
            style={{
              backgroundImage: `
                repeating-linear-gradient(
                  0deg,
                  transparent,
                  transparent 1px,
                  rgba(246, 9, 188, 0.1) 1px,
                  rgba(246, 9, 188, 0.1) 2px
                ),
                repeating-linear-gradient(
                  90deg,
                  transparent,
                  transparent 1px,
                  rgba(250, 184, 109, 0.1) 1px,
                  rgba(250, 184, 109, 0.1) 2px
                )
              `,
              backgroundSize: '3px 3px',
            }}
          />

          {/* Columns container - 4 weeks - IMPROVED SPACING */}
          <div className="grid grid-cols-4 gap-2 sm:gap-3 md:gap-4 relative z-10">
            {columns.map((column, colIndex) => (
              <div key={colIndex} className="flex flex-col items-center space-y-1.5 sm:space-y-2">
                {column.map((dayData, rowIndex) => (
                  <div key={dayData.day} className="flex flex-col items-center">
                    <CalendarPillBubble
                      dayData={dayData}
                      onClick={() => handleDayClick(dayData)}
                      isCurrentDay={dayData.day === currentDay}
                      shouldPuncture={puncturingDay === dayData.day}
                    />
                    {/* Arrow connector between pills (except last in column) */}
                    {rowIndex < column.length - 1 && (
                      <ChevronDown 
                        className="w-3 h-3 sm:w-4 sm:h-4 my-0.5 opacity-60"
                        style={{
                          color: dayData.isLowDose ? '#fbbf24' : dayData.isPlacebo ? '#fab86d' : '#f609bc',
                        }}
                      />
                    )}
                  </div>
                ))}
              </div>
            ))}
          </div>

          {/* Month label at bottom - RESPONSIVE */}
          <div className="mt-3 sm:mt-4 md:mt-5 text-center">
            <p 
              className="text-xs sm:text-sm font-semibold"
              style={{
                color: '#d007a0',
              }}
            >
              {getMonthRange()}
            </p>
          </div>

          {/* Embossed border effect */}
          <div
            className="absolute inset-0 rounded-2xl sm:rounded-3xl pointer-events-none"
            style={{
              boxShadow: 'inset 0 2px 4px rgba(255, 255, 255, 0.6), inset 0 -2px 4px rgba(246, 9, 188, 0.1)',
            }}
          />
        </div>
      </div>

      {/* Day status modal */}
      <DayModal
        isOpen={selectedDay !== null && !animatingDay}
        dayData={selectedDay}
        onClose={() => setSelectedDay(null)}
        onStatusChange={handleStatusChange}
      />

      {/* Fullscreen pill taken animation */}
      {animatingDay && (
        <PillTakenAnimation
          isPlacebo={animatingDay.isPlacebo}
          onComplete={handleAnimationComplete}
        />
      )}
    </>
  );
}