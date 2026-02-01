
package com.dosevia.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.Button;

public class AlarmActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        
        // DEBUG: Show that activity was created
        android.widget.Toast.makeText(this, "🚨 ALARM ACTIVITY OPENED!", android.widget.Toast.LENGTH_SHORT).show();
        
        // Modern way to show over lock screen and turn on screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }
        
        // Make sure window is on top
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_alarm);

        Button takeButton = findViewById(R.id.takeButton);
        takeButton.setOnClickListener(v -> {
            // Stop the alarm service
            Intent stopService = new Intent(this, AlarmService.class);
            stopService(stopService);
            
            // Cancel all notifications
            NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(1001); // Main alarm notification
                notificationManager.cancel(1000); // Warning notification
            }
            
            // Cancel all repeating alarms
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                Intent alarmIntent = new Intent(this, AlarmReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 999,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.cancel(pendingIntent);
            }
            
            // Reset repeat count
            AlarmReceiver.resetRepeatCount(this);
            
            // Open main app
            Intent openApp = new Intent(this, MainActivity.class);
            openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(openApp);
            finish();
        });
    }
    
    @Override
    public void onBackPressed() {
        // Prevent dismissing alarm with back button - user must tap TAKE
    }
}
