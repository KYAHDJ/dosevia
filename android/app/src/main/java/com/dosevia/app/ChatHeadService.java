package com.dosevia.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.core.app.NotificationCompat;

public class ChatHeadService extends Service {
    
    private WindowManager windowManager;
    private View chatHeadView;
    private static final int NOTIFICATION_ID = 2000;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Create notification channel for service
        createNotificationChannel();
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createServiceNotification());
        
        // Only show chat head if permission granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                createChatHead();
            }
        } else {
            createChatHead();
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "chat_head_service",
                "Medication Reminder",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Displays floating reminder bubble");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createServiceNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, "chat_head_service")
            .setContentTitle("Medication Reminder Active")
            .setContentText("Tap the floating pill icon to take your medication")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }
    
    private void createChatHead() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Inflate the chat head layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        chatHeadView = inflater.inflate(R.layout.chat_head_layout, null);
        
        // Set up window manager parameters
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;
        
        // Add the view to window
        windowManager.addView(chatHeadView, params);
        
        // Set up click listener
        ImageView chatHeadImage = chatHeadView.findViewById(R.id.chat_head_image);
        chatHeadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the app
                Intent intent = new Intent(ChatHeadService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                
                // Stop the service (remove chat head)
                stopSelf();
            }
        });
        
        // Set up drag listener
        chatHeadImage.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(chatHeadView, params);
                        return true;
                }
                return false;
            }
        });
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatHeadView != null && windowManager != null) {
            windowManager.removeView(chatHeadView);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
