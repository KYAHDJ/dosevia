package com.dosevia.app;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "Alarm")
public class AlarmPlugin extends Plugin {

    // 🔔 INSTANT notification
    @PluginMethod
    public void notifyNow(PluginCall call) {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        getContext(),
                        "dosevia_reminders"
                )
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Dosevia Reminder")
                        .setContentText("Instant test notification")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager)
                        getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }

        call.resolve(new JSObject());
    }

    // ⏰ Background-safe scheduled alarm
    @PluginMethod
    public void schedule(PluginCall call) {

        Long time = call.getLong("time");
        if (time == null) {
            call.reject("time required");
            return;
        }

        AlarmManager alarmManager =
                (AlarmManager)
                        getContext().getSystemService(Context.ALARM_SERVICE);

        // Schedule the main alarm
        Intent intent =
                new Intent(getContext(), AlarmReceiver.class);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        getContext(),
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    time,
                    pendingIntent
            );
        }
        
        // Schedule the 1-minute early notification
        AlarmReceiver.scheduleEarlyNotification(getContext(), time);

        call.resolve(new JSObject());
    }
}
