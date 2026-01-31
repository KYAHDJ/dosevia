package com.dosevia.app;

import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

@CapacitorPlugin(name = "CustomSound")
public class CustomSoundPlugin extends Plugin {

    /**
     * Save an audio file to the app's raw resources directory
     * so it can be used as a notification sound
     */
    @PluginMethod
    public void saveNotificationSound(PluginCall call) {
        String base64Data = call.getString("base64Data");
        String fileName = call.getString("fileName");

        if (base64Data == null || fileName == null) {
            call.reject("Missing base64Data or fileName");
            return;
        }

        try {
            Context context = getContext();
            
            // Decode base64 to bytes
            byte[] audioBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
            
            // Create audio directory in app's internal storage
            File audioDir = new File(context.getFilesDir(), "audio");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }
            
            // Save file with .mp3 extension
            File audioFile = new File(audioDir, fileName + ".mp3");
            FileOutputStream fos = new FileOutputStream(audioFile);
            fos.write(audioBytes);
            fos.close();
            
            // Get content URI for the file
            Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                audioFile
            );
            
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("fileName", fileName);
            result.put("filePath", audioFile.getAbsolutePath());
            result.put("contentUri", contentUri.toString());
            
            call.resolve(result);
            
        } catch (Exception e) {
            call.reject("Failed to save audio file: " + e.getMessage());
        }
    }

    /**
     * Get the content URI for a saved notification sound
     */
    @PluginMethod
    public void getSoundUri(PluginCall call) {
        String fileName = call.getString("fileName");

        if (fileName == null) {
            call.reject("Missing fileName");
            return;
        }

        try {
            Context context = getContext();
            File audioFile = new File(context.getFilesDir(), "audio/" + fileName + ".mp3");
            
            if (!audioFile.exists()) {
                call.reject("Audio file not found: " + fileName);
                return;
            }
            
            Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                audioFile
            );
            
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("fileName", fileName);
            result.put("contentUri", contentUri.toString());
            
            call.resolve(result);
            
        } catch (Exception e) {
            call.reject("Failed to get sound URI: " + e.getMessage());
        }
    }
}
