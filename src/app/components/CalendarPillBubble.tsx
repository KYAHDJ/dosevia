import { motion } from 'motion/react';
import { DayData } from '@/types/pill-types';
import { format } from 'date-fns';

interface CalendarPillBubbleProps {
  dayData: DayData;
  onClick: () => void;
  isCurrentDay: boolean;
  shouldPuncture?: boolean;
}

export function CalendarPillBubble({ dayData, onClick, isCurrentDay, shouldPuncture }: CalendarPillBubbleProps) {
  const { status, isPlacebo, date } = dayData;

  const dayName = format(date, 'EEE'); // Thu, Fri, etc.
  const dateNum = format(date, 'd'); // 19, 20, etc.

  // Determine if pill is physically present
  const isPillPresent = status !== 'taken';

  return (
    <div className="relative flex flex-col items-center">
      {/* Pill bubble container */}
      <motion.button
        onClick={onClick}
        className="relative w-16 h-16 focus:outline-none"
        whileTap={isPillPresent ? { scale: 0.95 } : {}}
        style={{
          perspective: '1000px',
        }}
      >
        {/* Current day ring */}
        {isCurrentDay && isPillPresent && (
          <motion.div
            className="absolute inset-[-4px] rounded-full border-3"
            style={{
              borderWidth: '3px',
              borderColor: isPlacebo ? '#fab86d' : '#f609bc',
            }}
            animate={{
              scale: [1, 1.05, 1],
            }}
            transition={{
              duration: 2,
              repeat: Infinity,
              ease: 'easeInOut',
            }}
          />
        )}

        {/* Foil backing */}
        <div
          className="absolute inset-0 rounded-full"
          style={{
            background: status === 'taken'
              ? 'radial-gradient(circle at 40% 40%, #3a3a3a, #1a1a1a)' // Dark broken foil
              : 'radial-gradient(circle at 30% 30%, #d4d4d4, #8a8a8a)',
            boxShadow: status === 'taken'
              ? 'inset 0 3px 8px rgba(0, 0, 0, 0.6)'
              : 'inset 0 1px 3px rgba(0, 0, 0, 0.2)',
          }}
        />

        {/* Puncture animation effect */}
        {shouldPuncture && isPillPresent && (
          <motion.div
            className="absolute inset-0 rounded-full"
            initial={{ scale: 1, opacity: 1 }}
            animate={{ 
              scale: [1, 0.95, 1.05, 0.98],
              opacity: [1, 0.8, 0.6, 0],
            }}
            transition={{ duration: 0.3 }}
          >
            <div
              className="absolute inset-0 rounded-full border-4"
              style={{
                borderColor: '#fff',
              }}
            />
          </motion.div>
        )}

        {status === 'taken' ? (
          // Empty - pill was popped out, show broken foil texture
          <div className="absolute inset-0 flex items-center justify-center">
            <div
              className="w-11 h-11 rounded-full relative"
              style={{
                background: 'radial-gradient(circle at 30% 30%, #2a2a2a, #0a0a0a)',
                boxShadow: 'inset 0 4px 12px rgba(0, 0, 0, 0.9), inset 0 -2px 6px rgba(255, 255, 255, 0.05)',
              }}
            >
              {/* Torn edges effect */}
              <div
                className="absolute inset-0 rounded-full"
                style={{
                  background: `
                    radial-gradient(circle at 20% 80%, transparent 45%, #1a1a1a 45%, #1a1a1a 47%, transparent 47%),
                    radial-gradient(circle at 80% 30%, transparent 48%, #222 48%, #222 50%, transparent 50%),
                    radial-gradient(circle at 60% 70%, transparent 46%, #1a1a1a 46%, #1a1a1a 48%, transparent 48%)
                  `,
                }}
              />
            </div>
          </div>
        ) : (
          // Pill present with plastic bubble
          <>
            {/* Plastic bubble dome */}
            <div
              className="absolute inset-0 rounded-full pointer-events-none"
              style={{
                background: 'radial-gradient(circle at 35% 25%, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.3) 40%, rgba(255, 255, 255, 0.1) 70%, transparent)',
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.2), inset 0 1px 3px rgba(255, 255, 255, 0.8)',
              }}
            />

            {/* Pill inside bubble - hide during puncture animation */}
            {!shouldPuncture && (
              <motion.div
                className="absolute inset-0 flex items-center justify-center"
                animate={
                  status === 'missed'
                    ? {
                        x: [0, -3, 3, -3, 3, 0],
                      }
                    : {}
                }
                transition={
                  status === 'missed'
                    ? {
                        duration: 0.5,
                        repeat: 2,
                      }
                    : {}
                }
                style={{
                  transformStyle: 'preserve-3d',
                }}
              >
                <div
                  className="relative w-11 h-11 rounded-full flex items-center justify-center"
                  style={{
                    background: isPlacebo
                      ? 'linear-gradient(135deg, #e5e7eb 0%, #d1d5db 50%, #9ca3af 100%)'
                      : 'linear-gradient(135deg, #ffffff 0%, #f3f4f6 50%, #e5e7eb 100%)',
                    boxShadow: `
                      0 3px 8px ${isPlacebo ? 'rgba(156, 163, 175, 0.4)' : 'rgba(0, 0, 0, 0.15)'},
                      inset 0 1px 0 rgba(255, 255, 255, 0.9),
                      inset 0 -1px 2px rgba(0, 0, 0, 0.1)
                    `,
                    border: status === 'missed' ? '2px solid #fab86d' : 'none',
                    transform: 'translateZ(8px)',
                  }}
                >
                  {/* Pill shine/highlight */}
                  <div
                    className="absolute top-1 left-1 w-5 h-5 rounded-full pointer-events-none"
                    style={{
                      background: 'radial-gradient(circle at center, rgba(255, 255, 255, 0.8), transparent 60%)',
                    }}
                  />

                  {/* Day and date text on pill */}
                  <div className="flex flex-col items-center justify-center relative z-10">
                    <span className="text-[9px] font-bold text-gray-900 leading-none">
                      {dayName}
                    </span>
                    <span className="text-sm font-bold text-gray-900 leading-none mt-0.5">
                      {dateNum}
                    </span>
                  </div>
                </div>
              </motion.div>
            )}
          </>
        )}

        {/* Taken state - show day/date on dark background */}
        {status === 'taken' && (
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div className="flex flex-col items-center justify-center">
              <span className="text-[9px] font-bold text-gray-500 leading-none">
                {dayName}
              </span>
              <span className="text-sm font-bold text-gray-600 leading-none mt-0.5">
                {dateNum}
              </span>
            </div>
          </div>
        )}
      </motion.button>
    </div>
  );
}