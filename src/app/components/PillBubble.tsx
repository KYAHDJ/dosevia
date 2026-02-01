import { motion } from 'motion/react';
import { DayData } from '@/types/pill-types';

interface PillBubbleProps {
  dayData: DayData;
  onClick: () => void;
  isCurrentDay: boolean;
  animatingTaken?: boolean;
}

export function PillBubble({ dayData, onClick, isCurrentDay, animatingTaken }: PillBubbleProps) {
  const { day, status, isPlacebo } = dayData;

  // Determine pill color based on type
  const pillColor = isPlacebo ? '#e8e8e8' : '#4a9eff';
  const pillShadowColor = isPlacebo ? 'rgba(0, 0, 0, 0.1)' : 'rgba(74, 158, 255, 0.4)';

  return (
    <motion.button
      onClick={onClick}
      className="relative w-12 h-12 focus:outline-none focus:ring-2 focus:ring-blue-400 rounded-full"
      whileTap={{ scale: 0.95 }}
      style={{
        perspective: '1000px',
      }}
    >
      {/* Foil backing */}
      <div
        className="absolute inset-0 rounded-full"
        style={{
          background: status === 'taken'
            ? 'radial-gradient(circle at 30% 30%, #8b8b8b, #5a5a5a)'
            : 'radial-gradient(circle at 30% 30%, #c0c0c0, #7a7a7a)',
          boxShadow: 'inset 0 2px 4px rgba(0, 0, 0, 0.3)',
        }}
      />

      {/* Current day highlight ring */}
      {isCurrentDay && (
        <motion.div
          className="absolute inset-[-3px] rounded-full"
          style={{
            background: 'linear-gradient(135deg, rgba(74, 158, 255, 0.4), rgba(74, 158, 255, 0.2))',
          }}
          animate={{
            opacity: [0.5, 0.8, 0.5],
          }}
          transition={{
            duration: 2,
            repeat: Infinity,
            ease: 'easeInOut',
          }}
        />
      )}

      {/* Plastic bubble overlay */}
      {status !== 'taken' && (
        <div
          className="absolute inset-0 rounded-full pointer-events-none"
          style={{
            background: 'radial-gradient(circle at 40% 30%, rgba(255, 255, 255, 0.6), rgba(255, 255, 255, 0.1))',
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)',
          }}
        />
      )}

      {/* Pill or empty state */}
      {status === 'taken' ? (
        // Empty hole - foil punctured
        <div className="absolute inset-0 flex items-center justify-center">
          <div
            className="w-7 h-7 rounded-full"
            style={{
              background: '#2a2a2a',
              boxShadow: 'inset 0 2px 6px rgba(0, 0, 0, 0.8)',
            }}
          />
        </div>
      ) : (
        // Pill present
        <motion.div
          className="absolute inset-0 flex items-center justify-center"
          animate={
            status === 'missed'
              ? {
                  x: [0, -2, 2, -2, 2, 0],
                }
              : animatingTaken
              ? {
                  scale: [1, 1.2, 0],
                  y: [0, -20, -40],
                  opacity: [1, 1, 0],
                }
              : {}
          }
          transition={
            status === 'missed'
              ? {
                  duration: 0.5,
                  repeat: 2,
                }
              : animatingTaken
              ? {
                  duration: 0.6,
                  ease: [0.34, 1.56, 0.64, 1],
                }
              : {}
          }
        >
          <div
            className="w-8 h-8 rounded-full relative"
            style={{
              background: `linear-gradient(135deg, ${pillColor}, ${adjustBrightness(pillColor, -20)})`,
              boxShadow: `0 3px 8px ${pillShadowColor}`,
              border: status === 'missed' ? '2px solid #f59e0b' : 'none',
            }}
          >
            {/* Pill highlight */}
            <div
              className="absolute top-1 left-1 w-3 h-3 rounded-full"
              style={{
                background: 'radial-gradient(circle at center, rgba(255, 255, 255, 0.6), transparent)',
              }}
            />
          </div>
        </motion.div>
      )}

      {/* Day number */}
      <div
        className="absolute inset-0 flex items-center justify-center pointer-events-none"
        style={{
          fontSize: '10px',
          fontWeight: 600,
          color: status === 'taken' ? '#888' : isPlacebo ? '#666' : '#fff',
          textShadow: status === 'taken' ? 'none' : '0 1px 2px rgba(0, 0, 0, 0.3)',
        }}
      >
        {day}
      </div>
    </motion.button>
  );
}

// Helper function to adjust color brightness
function adjustBrightness(hex: string, percent: number): string {
  const num = parseInt(hex.replace('#', ''), 16);
  const r = Math.max(0, Math.min(255, ((num >> 16) & 0xff) + percent));
  const g = Math.max(0, Math.min(255, ((num >> 8) & 0xff) + percent));
  const b = Math.max(0, Math.min(255, (num & 0xff) + percent));
  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`;
}
