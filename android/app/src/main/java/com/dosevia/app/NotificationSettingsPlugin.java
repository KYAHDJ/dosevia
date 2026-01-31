package com.dosevia.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Plugin to open Android's notification settings for this app
 * This allows users to change notification sound, vibration, etc. using native Android UI
 */
@CapacitorPlugin(name = "NotificationSettings")
public class NotificationSettingsPlugin extends Plugin {

    private static final String TAG = "NotificationSettings";

    /**
     * Opens the app's notification settings page in Android Settings
     * Works on ALL Android versions (API 21+)
     * GUARANTEED to open SOME settings page
     */
    @PluginMethod
    public void openNotificationSettings(PluginCall call) {
        try {
            Intent intent;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+ - Open app notification settings
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
                Log.d(TAG, "📱 Opening notification settings (Android 8+)");
            } else {
                // Android 5.0 to 7.1 - Open app details (includes notifications)
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getContext().getPackageName(), null));
                Log.d(TAG, "📱 Opening app details (Android 5-7)");
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            
            Log.d(TAG, "✅ Successfully opened settings");
            
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("opened", true);
            
            call.resolve(result);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Primary method failed, trying fallback", e);
            
            // FALLBACK: Open general app settings if notification settings fail
            try {
                Intent fallbackIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                fallbackIntent.setData(Uri.fromParts("package", getContext().getPackageName(), null));
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(fallbackIntent);
                
                Log.d(TAG, "✅ Opened fallback app settings");
                
                JSObject result = new JSObject();
                result.put("success", true);
                result.put("opened", true);
                result.put("fallback", true);
                
                call.resolve(result);
                
            } catch (Exception fallbackError) {
                Log.e(TAG, "❌ Even fallback failed, trying final fallback", fallbackError);
                
                // FINAL FALLBACK: Open main settings page
                try {
                    Intent finalIntent = new Intent(Settings.ACTION_SETTINGS);
                    finalIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(finalIntent);
                    
                    Log.d(TAG, "✅ Opened main settings as final fallback");
                    
                    JSObject result = new JSObject();
                    result.put("success", true);
                    result.put("opened", true);
                    result.put("finalFallback", true);
                    
                    call.resolve(result);
                    
                } catch (Exception finalError) {
                    Log.e(TAG, "❌ All methods failed", finalError);
                    call.reject("Failed to open any settings: " + finalError.getMessage());
                }
            }
        }
    }

    /**
     * Opens the specific notification channel settings (Android 8.0+)
     * Falls back to app notification settings on older devices or if channel settings fail
     * GUARANTEED to open SOME settings page
     */
    @PluginMethod
    public void openChannelSettings(PluginCall call) {
        String channelId = call.getString("channelId");
        
        if (channelId == null) {
            // If no channel ID provided, just open general notification settings
            openNotificationSettings(call);
            return;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ - Try to open specific channel settings
            try {
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, channelId);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                getContext().startActivity(intent);
                
                Log.d(TAG, "✅ Opened channel settings for: " + channelId);
                
                JSObject result = new JSObject();
                result.put("success", true);
                result.put("opened", true);
                result.put("channelId", channelId);
                
                call.resolve(result);
                return;
                
            } catch (Exception e) {
                Log.w(TAG, "⚠️ Channel settings failed, falling back to app notification settings", e);
                // Fall through to fallback
            }
        }
        
        // FALLBACK: Open general app notification settings
        // This works for older Android OR if channel settings fail
        Log.d(TAG, "📱 Using fallback: app notification settings");
        openNotificationSettings(call);
    }
}
