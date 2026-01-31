package com.dosevia.app;

import android.provider.Settings;
import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.File;

public class AlarmService extends Service {
    Vibrator vibrator;
    AudioManager audioManager;
    private static final int NOTIFICATION_ID = 1002;
    private static final String TAG = "AlarmService";
    private int originalVolume;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🔔 AlarmService started");
        
        // Get notification title and subtitle
        String title = intent.getStringExtra("title");
        String subtitle = intent.getStringExtra("subtitle");
        
        if (title == null) title = "⏰ Time to take your medication";
        if (subtitle == null) subtitle = "Don't forget your daily dose";
        
        // Create the main notification that the user will see and interact with
        Intent stopIntent = new Intent(this, AlarmReceiver.class);
        stopIntent.setAction("STOP_ALARM");
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
            this, 
            0, 
            stopIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build the main medication reminder notification
        Notification mainNotification = new NotificationCompat.Builder(this, "dosevia_reminders")
                .setContentTitle(title)
                .setContentText(subtitle)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .setDeleteIntent(stopPendingIntent)
                .addAction(R.mipmap.ic_launcher, "Dismiss", stopPendingIntent)
                .build();
        
        // Show the main notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1003, mainNotification);
            Log.d(TAG, "✅ Main notification posted");
        }
        
        // Create a minimal foreground service notification (required for foreground service)
        Notification foregroundNotification = new NotificationCompat.Builder(this, "alarm_service_channel")
                .setContentTitle("⏰ Alarm Active")
                .setContentText("Tap the medication reminder to dismiss")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, foregroundNotification);
        
        // Save current alarm volume and set to MAX
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
            Log.d(TAG, "🔊 Volume set to max: " + maxVolume);
        }
        
        // The notification channel's sound will play automatically
        // We just need to handle vibration here
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {
                0,      // Start immediately
                1000,   // Vibrate 1 second
                500     // Pause 0.5 seconds
            };
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)); // 0 = repeat
            } else {
                vibrator.vibrate(pattern, 0); // 0 = repeat
            }
            Log.d(TAG, "📳 Vibration started");
        }
        
        Log.d(TAG, "✅ AlarmService fully initialized - notification sound playing via channel");
        
        return START_STICKY; // Keep running
    }

    @Override
    public void onDestroy() {
        // Stop vibration
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        
        // Restore original volume
        if (audioManager != null && originalVolume > 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0);
        }
        
        // Cancel the main notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(1003);
        }
        
        Log.d(TAG, "✅ AlarmService destroyed");
        
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
