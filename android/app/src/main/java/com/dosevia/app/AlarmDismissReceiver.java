package com.dosevia.app;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmDismissReceiver extends BroadcastReceiver {
    
    private static final int NOTIFICATION_ID = 1001;
    private static final int EARLY_NOTIFICATION_ID = 1000;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        // Stop the alarm service (stops continuous sound/vibration)
        Intent stopService = new Intent(context, AlarmService.class);
        context.stopService(stopService);
        
        // Cancel the repeating alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 999,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);
        }
        
        // Dismiss all notifications
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
            notificationManager.cancel(EARLY_NOTIFICATION_ID);
        }
        
        // Reset repeat count
        AlarmReceiver.resetRepeatCount(context);
    }
}
