package com.dosevia.app;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {
    
    private static final String PREFS_NAME = "DoseviaPrefs";
    private static final String KEY_REPEAT_COUNT = "repeatCount";
    private static final int NOTIFICATION_ID = 1001;
    private static final int EARLY_NOTIFICATION_ID = 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        
        // Check if this is the 1-minute early notification
        boolean isEarly = intent.getBooleanExtra("is_early", false);
        
        if (isEarly) {
            showEarlyNotification(context);
            return;
        }
        
        // Acquire wake lock to ensure device wakes up
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | 
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Dosevia::AlarmWakeLock"
            );
            wakeLock.acquire(10000); // 10 seconds
            wakeLock.release();
        }
        
        // Start alarm service
        Intent serviceIntent = new Intent(context, AlarmService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        
        // Show main notification
        showAlarmNotification(context);
        
        // Get repeat count (reuse prefs variable from earlier)
        SharedPreferences repeatPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int repeatCount = repeatPrefs.getInt(KEY_REPEAT_COUNT, 0);
        
        // Schedule next repeating alarm (30 seconds, then decreasing to 10 seconds)
        scheduleNextAlarm(context, repeatCount);
        
        // Increment repeat count
        repeatPrefs.edit().putInt(KEY_REPEAT_COUNT, repeatCount + 1).apply();
    }
    
    private void showEarlyNotification(Context context) {
        // Create intent to open app
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAppIntent = PendingIntent.getActivity(
            context, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build early notification (1 minute before)
        Uri notificationSound = Settings.System.DEFAULT_NOTIFICATION_URI;
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "dosevia_reminders")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ Medication Reminder - 1 Minute")
            .setContentText("Your medication time is in 1 minute!")
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("⏰ 1 Minute Warning\n\nYour medication time is coming up. Get ready to take your pill."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(notificationSound, AudioAttributes.USAGE_NOTIFICATION)
            .setVibrate(new long[]{0, 500, 250, 500})
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .setTimeoutAfter(60 * 1000); // Auto-dismiss after 1 minute

        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(EARLY_NOTIFICATION_ID, builder.build());
        }
    }
    
    private void showAlarmNotification(Context context) {
        // Create intent to open app
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAppIntent = PendingIntent.getActivity(
            context, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create TAKE PILL action
        Intent dismissIntent = new Intent(context, AlarmDismissReceiver.class);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
            context, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build main alarm notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "dosevia_reminders")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💊 MEDICATION TIME!")
            .setContentText("Time to take your pill! Tap 'TAKE PILL' to stop alarm.")
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("💊 MEDICATION ALARM\n\nIt's time to take your medication!\n\n⚠️ ALARM WILL REPEAT until you tap TAKE PILL button."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)  // Cannot be swiped away
            .setAutoCancel(false)
            .setContentIntent(openAppIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "✅ TAKE PILL - STOP ALARM",
                dismissPendingIntent
            );

        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
    
    private void scheduleNextAlarm(Context context, int repeatCount) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        
        // Start at 30 seconds, decrease by 2 seconds each repeat until minimum 10 seconds
        int delaySeconds = Math.max(10, 30 - (repeatCount * 2));
        long delayMillis = delaySeconds * 1000L;
        
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Cancel any existing repeating alarm first
        alarmManager.cancel(pendingIntent);
        
        // Schedule next alarm
        long triggerTime = System.currentTimeMillis() + delayMillis;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            );
        }
    }
    
    // Public method to schedule 1-minute early notification
    public static void scheduleEarlyNotification(Context context, long alarmTimeMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        
        // Schedule notification 1 minute before alarm (e.g., 8:59 for 9:00)
        long earlyTime = alarmTimeMillis - (60 * 1000); // 1 minute in milliseconds
        
        // Only schedule if early time is in the future
        if (earlyTime > System.currentTimeMillis()) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("is_early", true);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 998, // Different request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Use setExactAndAllowWhileIdle for precise timing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    earlyTime,
                    pendingIntent
                );
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    earlyTime,
                    pendingIntent
                );
            }
        }
    }
    
    // Reset repeat count when pill is taken
    public static void resetRepeatCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_REPEAT_COUNT, 0).apply();
    }
}
