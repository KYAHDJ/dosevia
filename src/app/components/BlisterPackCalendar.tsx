import { useState } from 'react';
import { DayData, PillType, PillStatus } from '@/types/pill-types';
import { PillBubble } from './PillBubble';
import { DayModal } from './DayModal';

interface BlisterPackCalendarProps {
  pillType: PillType;
  days: DayData[];
  onStatusChange: (day: number, status: PillStatus) => void;
}

export function BlisterPackCalendar({ pillType, days, onStatusChange }: BlisterPackCalendarProps) {
  const [selectedDay, setSelectedDay] = useState<DayData | null>(null);
  const [animatingDay, setAnimatingDay] = useState<number | null>(null);

  const currentDayIndex = days.findIndex(d => d.status === 'not_taken');
  const currentDay = currentDayIndex >= 0 ? currentDayIndex + 1 : 1;

  const handleDayClick = (dayData: DayData) => {
    setSelectedDay(dayData);
  };

  const handleStatusChange = (day: number, status: PillStatus) => {
    if (status === 'taken') {
      setAnimatingDay(day);
      setTimeout(() => {
        onStatusChange(day, status);
        setAnimatingDay(null);
      }, 600);
    } else {
      onStatusChange(day, status);
    }
  };

  // Determine grid layout based on pill type
  const getGridLayout = () => {
    switch (pillType) {
      case '21+7':
        return 'grid-cols-7'; // 4 rows of 7
      case '24+4':
        return 'grid-cols-7'; // 4 rows of 7
      case '28-day':
        return 'grid-cols-7'; // 4 rows of 7
      default:
        return 'grid-cols-7';
    }
  };

  return (
    <>
      <div className="w-full max-w-md mx-auto px-4">
        {/* Blister pack container */}
        <div
          className="relative rounded-3xl p-6 shadow-2xl"
          style={{
            background: 'linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)',
            boxShadow: '0 10px 40px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.8)',
          }}
        >
          {/* Metallic foil texture overlay */}
          <div
            className="absolute inset-0 rounded-3xl opacity-30 pointer-events-none"
            style={{
              background: `
                repeating-linear-gradient(
                  45deg,
                  transparent,
                  transparent 2px,
                  rgba(255, 255, 255, 0.1) 2px,
                  rgba(255, 255, 255, 0.1) 4px
                ),
                repeating-linear-gradient(
                  -45deg,
                  transparent,
                  transparent 2px,
                  rgba(0, 0, 0, 0.05) 2px,
                  rgba(0, 0, 0, 0.05) 4px
                )
              `,
            }}
          />

          {/* Brand label area */}
          <div className="mb-4 text-center relative z-10">
            <div className="inline-block px-4 py-1.5 rounded-lg bg-white/40 backdrop-blur-sm">
              <p className="text-xs font-semibold text-gray-600 tracking-wide">
                {pillType === '21+7' && '21 Active + 7 Placebo'}
                {pillType === '24+4' && '24 Active + 4 Placebo'}
                {pillType === '28-day' && '28-Day Continuous'}
              </p>
            </div>
          </div>

          {/* Pills grid */}
          <div className={`grid ${getGridLayout()} gap-3 relative z-10`}>
            {days.map((dayData) => (
              <PillBubble
                key={dayData.day}
                dayData={dayData}
                onClick={() => handleDayClick(dayData)}
                isCurrentDay={dayData.day === currentDay}
                animatingTaken={animatingDay === dayData.day}
              />
            ))}
          </div>

          {/* Subtle embossing effect */}
          <div
            className="absolute inset-0 rounded-3xl pointer-events-none"
            style={{
              boxShadow: 'inset 0 2px 4px rgba(255, 255, 255, 0.5), inset 0 -2px 4px rgba(0, 0, 0, 0.1)',
            }}
          />
        </div>
      </div>

      {/* Day status modal */}
      <DayModal
        isOpen={selectedDay !== null}
        dayData={selectedDay}
        onClose={() => setSelectedDay(null)}
        onStatusChange={handleStatusChange}
      />
    </>
  );
}
