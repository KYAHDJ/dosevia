import { motion, AnimatePresence } from 'motion/react';
import { X } from 'lucide-react';
import { DayData, PillStatus } from '@/types/pill-types';
import { format, startOfDay, isAfter } from 'date-fns';
import { Portal } from './Portal';

interface DayModalProps {
  isOpen: boolean;
  dayData: DayData | null;
  onClose: () => void;
  onStatusChange: (day: number, status: PillStatus) => void;
}

export function DayModal({ isOpen, dayData, onClose, onStatusChange }: DayModalProps) {
  if (!dayData) return null;

  const pillDate = startOfDay(dayData.date);
  const today = startOfDay(new Date());
  const isFuture = isAfter(pillDate, today);

  const handleStatusClick = (status: PillStatus) => {
    if (isFuture) {
      console.log('🚫 Cannot change status - pill is in the future');
      return;
    }
    onStatusChange(dayData.day, status);
    onClose();
  };

  return (
    <Portal>
      <AnimatePresence>
        {isOpen && (
          <div 
            className="fixed inset-0 z-[99999]" 
            style={{ 
              position: 'fixed !important' as any,
              top: '0 !important',
              left: '0 !important', 
              right: '0 !important',
              bottom: '0 !important',
              width: '100vw',
              height: '100vh',
              margin: 0,
              padding: 0
            }}
          >
          {/* Backdrop - Full screen overlay */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="absolute inset-0 bg-black/50"
            style={{ 
              backdropFilter: 'blur(4px)',
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              width: '100%',
              height: '100%'
            }}
          />

          <div 
            className="absolute inset-0 flex items-center justify-center"
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              zIndex: 1,
              pointerEvents: 'none'
            }}
          >
            {/* Modal */}
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 20 }}
              transition={{ type: 'spring', damping: 25, stiffness: 300 }}
              className="w-[85%] max-w-md"
              style={{
                pointerEvents: 'auto'
              }}
            >
            <div
              className="bg-white rounded-2xl shadow-2xl overflow-hidden"
              style={{
                boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
              }}
            >
              {/* Header */}
              <div 
                className="px-6 py-5 border-b"
                style={{
                  background: 'linear-gradient(135deg, #fef3f9, #fef9ed)',
                  borderColor: 'rgba(246, 9, 188, 0.2)',
                }}
              >
                <div className="flex items-center justify-between">
                  <div>
                    <p 
                      className="text-xs font-medium uppercase tracking-wide mb-1"
                      style={{ color: '#d007a0' }}
                    >
                      Day {dayData.day}
                    </p>
                    <h3 className="text-lg font-semibold text-gray-800">
                      {format(dayData.date, 'EEEE, MMMM d')}
                    </h3>
                  </div>
                  <button
                    onClick={onClose}
                    className="text-gray-500 hover:text-gray-700 transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
                <div 
                  className="mt-2 inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
                  style={{
                    background: dayData.isPlacebo 
                      ? 'rgba(250, 184, 109, 0.2)'
                      : 'rgba(246, 9, 188, 0.2)',
                    color: dayData.isPlacebo ? '#c2410c' : '#d007a0',
                  }}
                >
                  {dayData.isPlacebo ? (
                    <span>Placebo Pill</span>
                  ) : (
                    <span>Active Pill</span>
                  )}
                </div>
              </div>

              {/* Action buttons */}
              <div className="p-6 space-y-3">
                {isFuture ? (
                  <div 
                    className="p-4 rounded-xl mb-4"
                    style={{
                      background: 'rgba(239, 68, 68, 0.1)',
                      border: '2px solid rgba(239, 68, 68, 0.3)',
                    }}
                  >
                    <p className="text-sm font-semibold text-red-600 text-center">
                      🚫 Cannot edit future pills
                    </p>
                    <p className="text-xs text-red-600 text-center mt-1">
                      You can only change today's pill or past pills
                    </p>
                  </div>
                ) : (
                  <p className="text-sm font-medium text-gray-600 mb-4">Mark this day as:</p>
                )}

                <button
                  onClick={() => handleStatusClick('taken')}
                  disabled={isFuture}
                  className="w-full py-4 px-6 rounded-xl font-semibold text-white transition-all active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed"
                  style={{
                    background: isFuture ? '#9ca3af' : 'linear-gradient(135deg, #10b981, #059669)',
                    boxShadow: isFuture ? 'none' : '0 4px 12px rgba(16, 185, 129, 0.3)',
                  }}
                >
                  ✓ Taken
                </button>

                <button
                  onClick={() => handleStatusClick('not_taken')}
                  disabled={isFuture}
                  className="w-full py-4 px-6 rounded-xl font-semibold transition-all active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed"
                  style={{
                    background: isFuture ? '#9ca3af' : 'linear-gradient(135deg, #e5e7eb, #d1d5db)',
                    color: '#374151',
                    boxShadow: isFuture ? 'none' : '0 2px 8px rgba(0, 0, 0, 0.1)',
                  }}
                >
                  Not Taken
                </button>

                <button
                  onClick={() => handleStatusClick('missed')}
                  disabled={isFuture}
                  className="w-full py-4 px-6 rounded-xl font-semibold text-white transition-all active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed"
                  style={{
                    background: isFuture ? '#9ca3af' : 'linear-gradient(135deg, #fab86d, #f59e0b)',
                    boxShadow: isFuture ? 'none' : '0 4px 12px rgba(250, 184, 109, 0.3)',
                  }}
                >
                  ⚠ Missed
                </button>
              </div>
            </div>
          </motion.div>
          </div>
        </div>
        )}
      </AnimatePresence>
    </Portal>
  );
}