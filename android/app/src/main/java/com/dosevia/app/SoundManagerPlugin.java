package com.dosevia.app;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;

@CapacitorPlugin(name = "SoundManager")
public class SoundManagerPlugin extends Plugin {

    private static final String TAG = "SoundManager";
    private MediaPlayer mediaPlayer;

    /**
     * Save an audio file to app storage
     */
    @PluginMethod
    public void saveSound(PluginCall call) {
        String base64Data = call.getString("base64Data");
        String fileName = call.getString("fileName");

        if (base64Data == null || fileName == null) {
            call.reject("Missing required parameters");
            return;
        }

        try {
            Context context = getContext();
            
            // Decode base64
            byte[] audioBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
            
            // Create audio directory
            File audioDir = new File(context.getFilesDir(), "notification_sounds");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }
            
            // Save file
            File audioFile = new File(audioDir, fileName + ".mp3");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(audioFile);
            fos.write(audioBytes);
            fos.close();
            
            Log.d(TAG, "✅ Sound saved: " + audioFile.getAbsolutePath());
            
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("fileName", fileName);
            result.put("path", audioFile.getAbsolutePath());
            
            call.resolve(result);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to save sound", e);
            call.reject("Failed to save sound: " + e.getMessage());
        }
    }

    /**
     * Play a saved sound file
     */
    @PluginMethod
    public void playSound(PluginCall call) {
        String fileName = call.getString("fileName");
        Boolean shouldVibrate = call.getBoolean("vibrate", true);

        if (fileName == null) {
            call.reject("Missing fileName");
            return;
        }

        try {
            Context context = getContext();
            
            // Stop any currently playing sound
            stopSound(null);
            
            // Get the audio file
            File audioFile = new File(context.getFilesDir(), "notification_sounds/" + fileName + ".mp3");
            
            if (!audioFile.exists()) {
                Log.w(TAG, "⚠️ Sound file not found, using default alarm");
                playDefaultAlarm();
                call.resolve();
                return;
            }
            
            // Play the sound
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.setAudioAttributes(
                new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            );
            mediaPlayer.setLooping(true); // Loop until dismissed
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            // Vibrate if requested
            if (shouldVibrate) {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    long[] pattern = {0, 1000, 500, 1000}; // Vibrate pattern
                    vibrator.vibrate(pattern, 0); // Repeat at index 0
                }
            }
            
            Log.d(TAG, "🔊 Playing sound: " + fileName);
            
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("playing", true);
            
            call.resolve(result);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to play sound", e);
            playDefaultAlarm();
            call.reject("Failed to play sound: " + e.getMessage());
        }
    }

    /**
     * Stop currently playing sound
     */
    @PluginMethod
    public void stopSound(PluginCall call) {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                Log.d(TAG, "🔇 Sound stopped");
            }
            
            // Stop vibration
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.cancel();
            }
            
            if (call != null) {
                JSObject result = new JSObject();
                result.put("success", true);
                result.put("playing", false);
                call.resolve(result);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error stopping sound", e);
            if (call != null) {
                call.reject("Failed to stop sound: " + e.getMessage());
            }
        }
    }

    /**
     * Play default system alarm sound
     */
    private void playDefaultAlarm() {
        try {
            stopSound(null);
            
            mediaPlayer = MediaPlayer.create(
                getContext(),
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            );
            
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
                Log.d(TAG, "🔊 Playing default alarm");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to play default alarm", e);
        }
    }

    @Override
    protected void handleOnDestroy() {
        stopSound(null);
        super.handleOnDestroy();
    }
}
