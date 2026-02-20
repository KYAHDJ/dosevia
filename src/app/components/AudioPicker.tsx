import { useState, useRef, useEffect } from 'react';
import { motion } from 'motion/react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from './ui/dialog';
import { Button } from './ui/button';
import { Folder, Play, Pause, Scissors, Volume2, Check } from 'lucide-react';
import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';
import { FilePicker } from '@capawesome/capacitor-file-picker';
import { Capacitor } from '@capacitor/core';

interface AudioPickerProps {
  isOpen: boolean;
  currentAudioUri: string;
  onClose: () => void;
  onSelect: (soundFileName: string) => void; // Returns just the filename without extension
}

export function AudioPicker({ isOpen, currentAudioUri, onClose, onSelect }: AudioPickerProps) {
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const [fileName, setFileName] = useState<string>('');
  const [audioData, setAudioData] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [startTrim, setStartTrim] = useState(0);
  const [endTrim, setEndTrim] = useState(0);
  const [isSaving, setIsSaving] = useState(false);
  
  const audioRef = useRef<HTMLAudioElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  // Reset state when modal opens
  useEffect(() => {
    if (isOpen) {
      setSelectedFile(null);
      setFileName('');
      setAudioData(null);
      setIsPlaying(false);
      setCurrentTime(0);
      setDuration(0);
      setStartTrim(0);
      setEndTrim(0);
      setIsSaving(false);
    }
  }, [isOpen]);

  // Pick audio file
  const handlePickFile = async () => {
    try {
      // Allow user to browse ALL files in their phone
      const result = await FilePicker.pickFiles({
        multiple: false,
        readData: true,
      });

      if (result.files && result.files.length > 0) {
        const file = result.files[0];
        console.log('📁 Picked file:', file.name, 'MIME:', file.mimeType);
        
        // ⚠️ STRICT VALIDATION: Only allow audio files
        const isAudioFile = 
          file.mimeType?.startsWith('audio/') || 
          /\.(mp3|wav|m4a|ogg|aac|flac|wma|opus)$/i.test(file.name);
        
        if (!isAudioFile) {
          alert('❌ Invalid file type!\n\nPlease select an audio file.\n\nSupported formats:\nMP3, WAV, M4A, OGG, AAC, FLAC, WMA, OPUS');
          console.error('❌ Not an audio file:', file.name, file.mimeType);
          return;
        }
        
        console.log('✅ Valid audio file selected');
        setFileName(file.name);
        setSelectedFile(file.path || '');
        
        // Convert to base64 data URL
        if (file.data) {
          const mimeType = file.mimeType || 'audio/mpeg';
          const dataUrl = `data:${mimeType};base64,${file.data}`;
          setAudioData(dataUrl);
        }
      }
    } catch (error) {
      console.error('❌ Error picking file:', error);
      alert('Failed to pick audio file. Please try again.');
    }
  };

  // Audio loaded - set duration and end trim
  const handleAudioLoaded = () => {
    if (audioRef.current) {
      const dur = audioRef.current.duration;
      setDuration(dur);
      setEndTrim(Math.min(dur, 10)); // Default to 10 seconds or full duration
      console.log('🎵 Audio loaded, duration:', dur);
    }
  };

  // Update current time while playing
  const handleTimeUpdate = () => {
    if (audioRef.current) {
      setCurrentTime(audioRef.current.currentTime);
      
      // Stop at end trim point
      if (audioRef.current.currentTime >= endTrim) {
        audioRef.current.pause();
        audioRef.current.currentTime = startTrim;
        setIsPlaying(false);
      }
    }
  };

  // Play/pause toggle
  const togglePlayPause = () => {
    if (!audioRef.current) return;

    if (isPlaying) {
      audioRef.current.pause();
    } else {
      audioRef.current.currentTime = startTrim;
      audioRef.current.play();
    }
    setIsPlaying(!isPlaying);
  };

  /**
   * WORKING SOLUTION: Save audio file using SoundManager plugin
   * This saves the file to app storage where Android can access it
   */
  const handleSave = async () => {
    if (!selectedFile || !audioData) {
      alert('Please select an audio file first');
      return;
    }

    setIsSaving(true);

    try {
      // Extract base64 data (remove data URL prefix)
      const base64Data = audioData.split(',')[1];
      
      // Generate a safe filename
      const originalName = fileName.replace(/\.[^/.]+$/, ''); // Remove extension
      const safeFileName = originalName
        .toLowerCase()
        .replace(/[^a-z0-9]/g, '_')
        .replace(/_+/g, '_')
        .substring(0, 30);
      
      const finalFileName = `sound_${Date.now()}_${safeFileName}`;
      
      console.log(`💾 Saving audio: ${finalFileName}`);

      if (Capacitor.getPlatform() === 'android') {
        // Import Capacitor and register the plugin
        const { Capacitor: CapacitorCore } = await import('@capacitor/core');
        const SoundManager = CapacitorCore.registerPlugin('SoundManager');
        
        // Save the sound file
        const result = await SoundManager.saveSound({
          base64Data: base64Data,
          fileName: finalFileName,
        });

        console.log(`✅ Sound saved:`, result);
        
        // Test play the sound
        console.log(`🔊 Testing sound...`);
        await SoundManager.playSound({
          fileName: finalFileName,
          vibrate: false,
        });
        
        // Stop after 2 seconds
        setTimeout(async () => {
          await SoundManager.stopSound();
          console.log(`✅ Sound test complete`);
        }, 2000);
        
        // Return the filename to be saved in settings
        onSelect(finalFileName);
        onClose();
      } else {
        alert('⚠️ Custom sounds only supported on Android');
      }
      
    } catch (error) {
      console.error('❌ Error saving audio:', error);
      alert(`Failed to save audio:\n${error}`);
    } finally {
      setIsSaving(false);
    }
  };

  // Draw waveform visualization
  useEffect(() => {
    if (!canvasRef.current || !audioData) return;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Simple waveform visualization
    const width = canvas.width;
    const height = canvas.height;
    
    ctx.clearRect(0, 0, width, height);
    
    // Draw background
    ctx.fillStyle = '#f3f4f6';
    ctx.fillRect(0, 0, width, height);
    
    // Draw waveform (simplified - just decorative bars)
    ctx.fillStyle = '#d1d5db';
    const barCount = 50;
    const barWidth = width / barCount;
    
    for (let i = 0; i < barCount; i++) {
      const barHeight = Math.random() * height * 0.7 + height * 0.15;
      const x = i * barWidth;
      const y = (height - barHeight) / 2;
      
      ctx.fillRect(x + 1, y, barWidth - 2, barHeight);
    }
    
    // Draw trim region overlay
    const startX = (startTrim / duration) * width;
    const endX = (endTrim / duration) * width;
    
    // Highlight selected region
    ctx.fillStyle = 'rgba(246, 9, 188, 0.2)';
    ctx.fillRect(startX, 0, endX - startX, height);
    
    // Draw trim markers
    ctx.strokeStyle = '#f609bc';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(startX, 0);
    ctx.lineTo(startX, height);
    ctx.stroke();
    
    ctx.beginPath();
    ctx.moveTo(endX, 0);
    ctx.lineTo(endX, height);
    ctx.stroke();
    
    // Draw current time indicator if playing
    if (isPlaying && currentTime > 0) {
      const currentX = (currentTime / duration) * width;
      ctx.strokeStyle = '#10b981';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(currentX, 0);
      ctx.lineTo(currentX, height);
      ctx.stroke();
    }
  }, [audioData, duration, startTrim, endTrim, currentTime, isPlaying]);

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="w-[95vw] sm:w-full sm:max-w-2xl bg-gradient-to-br from-pink-50/95 via-white to-orange-50/95 backdrop-blur-sm border-2 border-pink-200/50 max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center gap-2 sm:gap-3 mb-2">
            <div 
              className="w-8 h-8 sm:w-10 sm:h-10 rounded-full flex items-center justify-center flex-shrink-0"
              style={{
                background: 'linear-gradient(135deg, #f609bc, #fab86d)',
              }}
            >
              <Volume2 className="w-4 h-4 sm:w-5 sm:h-5 text-white" />
            </div>
            <DialogTitle className="text-base sm:text-xl font-semibold bg-gradient-to-r from-pink-600 to-orange-500 bg-clip-text text-transparent">
              Choose Notification Sound
            </DialogTitle>
          </div>
          <p className="text-xs sm:text-sm text-gray-600">Select an audio file and trim it to your desired length</p>
        </DialogHeader>

        <div className="space-y-4 sm:space-y-6 py-3 sm:py-4">{/* Rest of content */}
          {/* File Picker */}
          <div className="space-y-2">
            <label className="text-xs sm:text-sm font-medium text-gray-700">Audio File</label>
            <div className="flex flex-col sm:flex-row gap-2">
              <div 
                className="flex-1 px-3 sm:px-4 py-2 sm:py-3 rounded-xl border-2 border-pink-200 bg-white flex items-center gap-2"
              >
                <Volume2 className="w-4 h-4 text-gray-400 flex-shrink-0" />
                <span className="text-xs sm:text-sm text-gray-600 truncate">
                  {fileName || 'No file selected'}
                </span>
              </div>
              <Button
                onClick={handlePickFile}
                style={{
                  background: 'linear-gradient(135deg, #f609bc, #fab86d)',
                }}
                className="w-full sm:w-auto text-white hover:opacity-90"
              >
                <Folder className="w-4 h-4 mr-2" />
                Browse
              </Button>
            </div>
            <p className="text-xs text-gray-500">Supported: MP3, WAV, M4A, OGG, AAC, FLAC, WMA, OPUS</p>
          </div>

          {/* Audio Player & Trimmer */}
          {audioData && (
            <>
              {/* Hidden audio element */}
              <audio
                ref={audioRef}
                src={audioData}
                onLoadedMetadata={handleAudioLoaded}
                onTimeUpdate={handleTimeUpdate}
                onEnded={() => setIsPlaying(false)}
              />

              {/* Waveform Canvas */}
              <div className="space-y-2">
                <label className="text-sm font-medium text-gray-700 flex items-center gap-2">
                  <Scissors className="w-4 h-4" />
                  Trim Audio Clip
                </label>
                <div className="relative bg-white rounded-xl p-4 border-2 border-pink-200">
                  <canvas
                    ref={canvasRef}
                    width={600}
                    height={120}
                    className="w-full h-[120px] rounded-lg"
                  />
                  
                  {/* Time labels */}
                  <div className="flex justify-between mt-2 text-xs text-gray-500">
                    <span>0:00</span>
                    <span>{duration.toFixed(1)}s</span>
                  </div>
                </div>
              </div>

              {/* Trim Controls */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium text-gray-700">Start Time (seconds)</label>
                  <input
                    type="range"
                    min="0"
                    max={duration}
                    step="0.1"
                    value={startTrim}
                    onChange={(e) => {
                      const val = parseFloat(e.target.value);
                      setStartTrim(Math.min(val, endTrim - 0.5));
                    }}
                    className="w-full h-2 rounded-lg appearance-none cursor-pointer"
                    style={{
                      background: 'linear-gradient(to right, #f609bc, #fab86d)',
                    }}
                  />
                  <div className="text-center text-sm font-semibold text-gray-700">
                    {startTrim.toFixed(1)}s
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium text-gray-700">End Time (seconds)</label>
                  <input
                    type="range"
                    min="0"
                    max={duration}
                    step="0.1"
                    value={endTrim}
                    onChange={(e) => {
                      const val = parseFloat(e.target.value);
                      setEndTrim(Math.max(val, startTrim + 0.5));
                    }}
                    className="w-full h-2 rounded-lg appearance-none cursor-pointer"
                    style={{
                      background: 'linear-gradient(to right, #f609bc, #fab86d)',
                    }}
                  />
                  <div className="text-center text-sm font-semibold text-gray-700">
                    {endTrim.toFixed(1)}s
                  </div>
                </div>
              </div>

              {/* Duration Display */}
              <div 
                className="p-3 rounded-xl border-2 border-pink-200 bg-pink-50/50"
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-700">Selected Duration:</span>
                  <span className="text-lg font-bold text-pink-600">
                    {(endTrim - startTrim).toFixed(1)} seconds
                  </span>
                </div>
              </div>

              {/* Play/Pause Button */}
              <div className="flex justify-center">
                <Button
                  onClick={togglePlayPause}
                  size="lg"
                  style={{
                    background: isPlaying 
                      ? 'linear-gradient(135deg, #10b981, #059669)'
                      : 'linear-gradient(135deg, #f609bc, #fab86d)',
                  }}
                  className="text-white hover:opacity-90 px-8"
                >
                  {isPlaying ? (
                    <>
                      <Pause className="w-5 h-5 mr-2" />
                      Pause Preview
                    </>
                  ) : (
                    <>
                      <Play className="w-5 h-5 mr-2" />
                      Play Preview
                    </>
                  )}
                </Button>
              </div>
            </>
          )}
        </div>

        <DialogFooter className="gap-2 flex-col sm:flex-row">
          <Button
            variant="outline"
            onClick={onClose}
            className="w-full sm:w-auto border-gray-300 hover:bg-gray-50 order-2 sm:order-1"
          >
            Cancel
          </Button>
          <Button
            onClick={handleSave}
            disabled={!audioData || isSaving}
            style={{
              background: (audioData && !isSaving)
                ? 'linear-gradient(135deg, #f609bc, #fab86d)'
                : '#d1d5db',
            }}
            className="w-full sm:w-auto text-white hover:opacity-90 order-1 sm:order-2"
          >
            <Check className="w-4 h-4 mr-2" />
            {isSaving ? 'Saving...' : 'Save Sound'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
