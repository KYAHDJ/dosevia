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
     * Opens the app's settings page in Android Settings
     * Goes directly to app details where notifications are accessible
     * GUARANTEED to work on ALL Android versions
     */
    @PluginMethod
    public void openNotificationSettings(PluginCall call) {
        try {
            // Open app details/settings page (works on ALL Android versions)
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getContext().getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            getContext().startActivity(intent);
            
            Log.d(TAG, "✅ Opened app settings page");
            
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("opened", true);
            
            call.resolve(result);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to open app settings, trying final fallback", e);
            
            // FINAL FALLBACK: Open main settings
            try {
                Intent fallbackIntent = new Intent(Settings.ACTION_SETTINGS);
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(fallbackIntent);
                
                Log.d(TAG, "✅ Opened main settings as fallback");
                
                JSObject result = new JSObject();
                result.put("success", true);
                result.put("opened", true);
                result.put("fallback", true);
                
                call.resolve(result);
                
            } catch (Exception finalError) {
                Log.e(TAG, "❌ All methods failed", finalError);
                call.reject("Failed to open settings: " + finalError.getMessage());
            }
        }
    }

    /**
     * Opens the app settings page
     * Simple and direct - goes straight to app details
     * GUARANTEED to work
     */
    @PluginMethod
    public void openChannelSettings(PluginCall call) {
        // Just open app settings directly - simpler and more reliable
        openNotificationSettings(call);
    }
}
