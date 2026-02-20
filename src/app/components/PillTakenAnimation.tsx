import { motion, AnimatePresence } from 'motion/react';
import { Check } from 'lucide-react';
import { useState, useEffect } from 'react';
import { Portal } from './Portal';

interface PillTakenAnimationProps {
  isPlacebo: boolean;
  onComplete: () => void;
}

export function PillTakenAnimation({ isPlacebo, onComplete }: PillTakenAnimationProps) {
  const [stage, setStage] = useState<'puncture' | 'emerge' | 'spin' | 'check' | 'confirm'>('puncture');

  useEffect(() => {
    // Stage progression
    const timers = [
      setTimeout(() => setStage('emerge'), 300),
      setTimeout(() => setStage('spin'), 800),
      setTimeout(() => setStage('check'), 1600),
      setTimeout(() => setStage('confirm'), 2200),
    ];

    return () => timers.forEach(clearTimeout);
  }, []);

  return (
    <Portal>
      <AnimatePresence>
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
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="absolute inset-0 flex items-center justify-center"
          style={{
            background: 'radial-gradient(circle, rgba(0, 0, 0, 0.7), rgba(0, 0, 0, 0.9))',
            backdropFilter: 'blur(8px)',
          }}
        >
        {/* Pill animation container */}
        <div className="relative" style={{ perspective: '1000px' }}>
          {/* Stage 1: Puncture effect */}
          {stage === 'puncture' && (
            <motion.div
              className="w-24 h-24 rounded-full relative"
              initial={{ scale: 1 }}
              animate={{ scale: [1, 0.95, 1.05] }}
              transition={{ duration: 0.3 }}
            >
              {/* Plastic bubble cracking */}
              <motion.div
                className="absolute inset-0 rounded-full border-4"
                style={{
                  borderColor: '#fff',
                }}
                initial={{ opacity: 1 }}
                animate={{ opacity: [1, 0.5, 0] }}
                transition={{ duration: 0.3 }}
              />
              {/* Flash effect */}
              <motion.div
                className="absolute inset-0 rounded-full"
                style={{
                  background: 'radial-gradient(circle, rgba(255, 255, 255, 0.8), transparent)',
                }}
                initial={{ scale: 0.5, opacity: 0 }}
                animate={{ scale: 2, opacity: [0, 1, 0] }}
                transition={{ duration: 0.3 }}
              />
            </motion.div>
          )}

          {/* Stage 2: Pill emerging */}
          {(stage === 'emerge' || stage === 'spin' || stage === 'check' || stage === 'confirm') && (
            <motion.div
              className="w-24 h-24 rounded-full relative flex items-center justify-center"
              style={{
                background: isPlacebo
                  ? 'linear-gradient(135deg, #e5e7eb 0%, #d1d5db 50%, #9ca3af 100%)'
                  : 'linear-gradient(135deg, #ffffff 0%, #f3f4f6 50%, #e5e7eb 100%)',
                boxShadow: `
                  0 20px 60px rgba(0, 0, 0, 0.4),
                  inset 0 2px 0 rgba(255, 255, 255, 0.9),
                  inset 0 -2px 4px rgba(0, 0, 0, 0.1)
                `,
                transformStyle: 'preserve-3d',
              }}
              initial={{ scale: 0.6, z: -100, rotateX: 0, rotateY: 0 }}
              animate={
                stage === 'emerge'
                  ? { scale: 1.2, z: 50, rotateX: 0, rotateY: 0 }
                  : stage === 'spin'
                  ? { 
                      scale: 1.2, 
                      z: 50, 
                      rotateX: [0, 360],
                      rotateY: [0, 360],
                    }
                  : { scale: 1.2, z: 50, rotateX: 0, rotateY: 0 }
              }
              transition={
                stage === 'emerge'
                  ? { duration: 0.8, ease: [0.34, 1.56, 0.64, 1] }
                  : stage === 'spin'
                  ? { duration: 0.8, ease: 'easeInOut' }
                  : { duration: 0.3 }
              }
            >
              {/* Pill highlight */}
              <div
                className="absolute top-2 left-2 w-10 h-10 rounded-full pointer-events-none"
                style={{
                  background: 'radial-gradient(circle at center, rgba(255, 255, 255, 0.9), transparent 70%)',
                }}
              />

              {/* Checkmark appears after spin */}
              {(stage === 'check' || stage === 'confirm') && (
                <motion.div
                  initial={{ scale: 0, rotate: -180 }}
                  animate={{ scale: 1, rotate: 0 }}
                  transition={{ 
                    type: 'spring', 
                    stiffness: 200, 
                    damping: 10,
                    duration: 0.6 
                  }}
                  className="absolute inset-0 flex items-center justify-center"
                  style={{
                    background: 'radial-gradient(circle, rgba(16, 185, 129, 0.2), transparent)',
                  }}
                >
                  <motion.div
                    animate={{
                      scale: [1, 1.1, 1],
                    }}
                    transition={{
                      duration: 0.6,
                      repeat: Infinity,
                      ease: 'easeInOut',
                    }}
                  >
                    <Check 
                      className="w-16 h-16" 
                      style={{ 
                        color: '#10b981',
                        strokeWidth: 4,
                        filter: 'drop-shadow(0 4px 12px rgba(16, 185, 129, 0.4))',
                      }} 
                    />
                  </motion.div>
                </motion.div>
              )}
            </motion.div>
          )}

          {/* Particle effects */}
          {stage === 'puncture' && (
            <>
              {[...Array(8)].map((_, i) => (
                <motion.div
                  key={i}
                  className="absolute top-1/2 left-1/2 w-2 h-2 rounded-full bg-white"
                  initial={{ 
                    x: 0, 
                    y: 0, 
                    scale: 1, 
                    opacity: 1 
                  }}
                  animate={{
                    x: Math.cos((i / 8) * Math.PI * 2) * 60,
                    y: Math.sin((i / 8) * Math.PI * 2) * 60,
                    scale: 0,
                    opacity: 0,
                  }}
                  transition={{ duration: 0.5, ease: 'easeOut' }}
                />
              ))}
            </>
          )}

          {/* Glow effect during emergence */}
          {(stage === 'emerge' || stage === 'spin') && (
            <motion.div
              className="absolute inset-0 rounded-full"
              style={{
                background: 'radial-gradient(circle, rgba(246, 9, 188, 0.3), transparent 70%)',
                filter: 'blur(20px)',
              }}
              animate={{
                scale: [1, 1.3, 1],
                opacity: [0.5, 0.8, 0.5],
              }}
              transition={{
                duration: 1,
                repeat: Infinity,
                ease: 'easeInOut',
              }}
            />
          )}
        </div>

        {/* Confirmation button */}
        {stage === 'confirm' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="absolute bottom-32"
          >
            <motion.button
              onClick={onComplete}
              className="px-10 py-4 rounded-2xl font-bold text-white text-lg shadow-2xl"
              style={{
                background: 'linear-gradient(135deg, #f609bc, #d007a0)',
                boxShadow: '0 10px 40px rgba(246, 9, 188, 0.5)',
              }}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
            >
              ✓ Confirm Taken
            </motion.button>
          </motion.div>
        )}
        </motion.div>
        </div>
      </AnimatePresence>
    </Portal>
  );
}
