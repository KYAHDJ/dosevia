package com.dosevia.app;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.media.AudioAttributes;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;


import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private boolean permissionsChecked = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Register custom plugins
        registerPlugin(CustomSoundPlugin.class);
        registerPlugin(SoundManagerPlugin.class);
        registerPlugin(NotificationSettingsPlugin.class);
        
        // Check permissions FIRST before doing anything
        checkCriticalPermissions();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Recheck permissions when user returns to the app
        if (permissionsChecked) {
            checkCriticalPermissions();
        }
    }
    
    private void checkCriticalPermissions() {
        boolean allGranted = true;
        StringBuilder missingPermissions = new StringBuilder("⚠️ REQUIRED PERMISSIONS MISSING:\n\n");
        
        // 1. Check Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                missingPermissions.append("❌ Notifications - REQUIRED\n");
            }
        }
        
        // 2. Check Exact Alarm Permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                allGranted = false;
                missingPermissions.append("❌ Alarms & Reminders - REQUIRED\n");
            }
        }
        
        // 3. Check Battery Optimization
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String packageName = getPackageName();
        boolean isBatteryOptimized = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(packageName);
        }
        
        if (isBatteryOptimized) {
            allGranted = false;
            missingPermissions.append("❌ Battery Optimization OFF - REQUIRED\n");
        }
        
        if (!allGranted) {
            // Block the app until permissions are granted
            missingPermissions.append("\n⛔ THE APP CANNOT FUNCTION WITHOUT THESE PERMISSIONS.\n\nClick OK to grant them now.");
            
            new AlertDialog.Builder(this)
                .setTitle("🚨 CRITICAL: Permissions Required")
                .setMessage(missingPermissions.toString())
                .setCancelable(false)
                .setPositiveButton("OK - Grant Permissions", (dialog, which) -> {
                    requestMissingPermissions();
                })
                .setNegativeButton("Exit App", (dialog, which) -> {
                    Toast.makeText(this, "❌ App cannot function without permissions. Exiting.", Toast.LENGTH_LONG).show();
                    finish();
                    System.exit(0);
                })
                .show();
        } else {
            // All permissions granted - proceed with initialization
            permissionsChecked = true;
            initializeApp();
        }
    }
    
    private void requestMissingPermissions() {
        // Request notification permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
                return; // Wait for result
            }
        }
        
        // Then check exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    Toast.makeText(this, "✓ Please allow 'Alarms & reminders' permission", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Please enable Alarms & Reminders in Settings", Toast.LENGTH_LONG).show();
                }
                return; // Wait for user action
            }
        }
        
        // Finally check battery optimization
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String packageName = getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                    Toast.makeText(this, "✓ Please turn OFF battery optimization for this app", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    // Fallback to general battery settings
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(intent);
                    Toast.makeText(this, "Find Dosevia and turn OFF battery optimization", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
        
        // If we get here, recheck all permissions
        new android.os.Handler().postDelayed(() -> {
            checkCriticalPermissions();
        }, 1000);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Continue with next permission check
            new android.os.Handler().postDelayed(() -> {
                requestMissingPermissions();
            }, 500);
        }
    }
    
    private void initializeApp() {
        // Create notification channel with sound and vibration
        createNotificationChannel();
        
        // Show success message
        Toast.makeText(this, "✅ All permissions granted! App ready.", Toast.LENGTH_SHORT).show();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                // DELETE ALL OLD CHANNELS - Clean slate
                notificationManager.deleteNotificationChannel("alarm_service_channel");
                notificationManager.deleteNotificationChannel("chat_head_service");
                notificationManager.deleteNotificationChannel("medication_alarms");
                notificationManager.deleteNotificationChannel("medication_reminders");
                notificationManager.deleteNotificationChannel("default");
                
                // Create ONLY ONE channel for all alarms
                String channelId = "dosevia_reminders";
                CharSequence name = "Alarms";
                String description = "Medication alarm notifications";
                int importance = NotificationManager.IMPORTANCE_HIGH;
                
                NotificationChannel channel = new NotificationChannel(channelId, name, importance);
                channel.setDescription(description);
                // Remove default vibration - let user control in Android settings
                channel.enableVibration(false);
                channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
                channel.setBypassDnd(true);
                
                // Set alarm sound
                Uri sound = Settings.System.DEFAULT_ALARM_ALERT_URI;
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                channel.setSound(sound, audioAttributes);
                
                notificationManager.createNotificationChannel(channel);
                
                Log.d("MainActivity", "✅ Created single alarm channel: " + channelId);
            }
        }
    }
}
