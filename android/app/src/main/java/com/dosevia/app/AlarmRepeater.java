package com.dosevia.app;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

public class AlarmRepeater extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        
        // Acquire wake lock
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK | 
                PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Dosevia::RepeaterWakeLock"
            );
            wakeLock.acquire(10000);
        }
        
        // Start alarm service for sound and vibration
        Intent serviceIntent = new Intent(context, AlarmService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        
        // Update notification
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent openAppIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        openApp,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        Intent dismissIntent = new Intent(context, AlarmDismissReceiver.class);
        PendingIntent dismissPendingIntent = 
                PendingIntent.getBroadcast(
                        context,
                        0,
                        dismissIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        Uri alarmSound = android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, "dosevia_reminders")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("⏰ MEDICATION REMINDER")
                        .setContentText("Time to take your medication!")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("Time to take your medication!\n\nRepeating every 30 seconds until dismissed"))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setSound(alarmSound, AudioAttributes.USAGE_ALARM)
                        .setVibrate(new long[]{0, 1000, 500, 1000, 500, 1000})
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .setContentIntent(openAppIntent)
                        .addAction(R.drawable.ic_launcher_foreground, "DISMISS", dismissPendingIntent);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(1001, builder.build());
        }
        
        // Schedule next alarm in 30 seconds
        scheduleNextAlarm(context);
        
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
    
    private void scheduleNextAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(context, AlarmRepeater.class);
        intent.putExtra("is_repeating", true);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (alarmManager != null) {
            long triggerTime = System.currentTimeMillis() + 30000;
            
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
    }
}
