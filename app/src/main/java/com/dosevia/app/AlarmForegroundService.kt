package com.dosevia.app

// ─────────────────────────────────────────────────────────────────────────────
//  AlarmForegroundService.kt  — v15
//
//  Sound logic (v15):
//   • notificationSound pref == "default"  → system alarm URI
//   • notificationSound pref == "silent"   → no sound
//   • notificationSound pref == <file path> → play that file from alarm_sounds/
//     The file is accessed via FileProvider so MediaPlayer can open it safely.
// ─────────────────────────────────────────────────────────────────────────────

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.Calendar

class AlarmForegroundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator?       = null

    override fun onCreate() {
        super.onCreate()
        recreateAlarmChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title    = intent?.getStringExtra(EXTRA_TITLE)    ?: "Time to take your pill"
        val subtitle = intent?.getStringExtra(EXTRA_SUBTITLE) ?: "Don't forget your daily dose"

        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_TITLE,    title)
            putExtra(EXTRA_SUBTITLE, subtitle)
        }
        val alarmPi = PendingIntent.getActivity(
            this, 4000, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takePi = PendingIntent.getBroadcast(
            this, 4001,
            Intent(this, TakePillReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(alarmPi)
            .setFullScreenIntent(alarmPi, true)
            .addAction(android.R.drawable.ic_menu_send, "✓ Take", takePi)
            .build()

        startForeground(ALARM_NOTIF_ID, notification)
        startAlarmSound()
        startVibration()

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) { stopSelf(); super.onTaskRemoved(rootIntent) }

    // ─────────────────────────────────────────────────────────────────────────
    //  Sound playback — supports default system alarm, silent, or custom file
    // ─────────────────────────────────────────────────────────────────────────

    private fun startAlarmSound() {
        try {
            val soundPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_NOTIF_SOUND, "default") ?: "default"

            if (soundPref == "silent") return

            val uri: Uri = when {
                soundPref == "default" -> {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        ?: return
                }
                else -> {
                    // Custom file path stored in alarm_sounds/
                    val file = File(soundPref)
                    if (!file.exists()) {
                        // File was deleted externally — fall back to default
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: return
                    } else {
                        // Wrap in a content:// URI via FileProvider
                        FileProvider.getUriForFile(
                            applicationContext,
                            "${applicationContext.packageName}.fileprovider",
                            file
                        )
                    }
                }
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                // Grant URI read permission to ourselves (needed for content:// URIs)
                if (soundPref != "default") {
                    applicationContext.grantUriPermission(
                        packageName, uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                setDataSource(applicationContext, uri)
                isLooping = true
                setVolume(1f, 1f)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // On any failure, try falling back to system alarm
            try {
                val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(applicationContext, fallback)
                    isLooping = true
                    setVolume(1f, 1f)
                    prepare()
                    start()
                }
            } catch (_: Exception) {}
        }
    }

    private fun startVibration() {
        if (!getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_VIBRATION, true)) return
        try {
            val pattern = longArrayOf(0, 600, 400)
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            else @Suppress("DEPRECATION") vibrator?.vibrate(pattern, 0)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onDestroy() { stopAlarmResources(); super.onDestroy() }

    private fun stopAlarmResources() {
        try { mediaPlayer?.apply { if (isPlaying) stop(); release() }; mediaPlayer = null } catch (_: Exception) {}
        try { vibrator?.cancel(); vibrator = null } catch (_: Exception) {}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─────────────────────────────────────────────────────────────────────────
    //  Notification channel — always recreated so sound changes take effect.
    //  Android caches the channel sound the first time it is created; deleting
    //  and re-creating forces the new sound to be applied.
    // ─────────────────────────────────────────────────────────────────────────

    private fun recreateAlarmChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.deleteNotificationChannel(ALARM_CHANNEL_ID)

        val soundUri: Uri = run {
            val pref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_NOTIF_SOUND, "default") ?: "default"

            when {
                pref == "silent" -> android.net.Uri.EMPTY
                pref == "default" ->
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        ?: android.net.Uri.EMPTY
                else -> {
                    val file = File(pref)
                    if (file.exists()) {
                        FileProvider.getUriForFile(
                            applicationContext,
                            "${applicationContext.packageName}.fileprovider",
                            file
                        )
                    } else {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: android.net.Uri.EMPTY
                    }
                }
            }
        }

        val audioAttr = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        val channel = NotificationChannel(
            ALARM_CHANNEL_ID, ALARM_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description          = "Dosevia daily pill alarm"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            enableVibration(true)
            enableLights(true)
            if (soundUri != android.net.Uri.EMPTY) {
                setSound(soundUri, audioAttr)
            } else {
                setSound(null, null)   // silent channel
            }
        }
        nm.createNotificationChannel(channel)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TakePillReceiver — "✓ Take" notification action button
// ─────────────────────────────────────────────────────────────────────────────

class TakePillReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todayMs = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong("alarm_taken_date", todayMs)
            .apply()

        context.stopService(Intent(context, AlarmForegroundService::class.java))

        context.startActivity(Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("from_alarm", true)
        })
    }
}
