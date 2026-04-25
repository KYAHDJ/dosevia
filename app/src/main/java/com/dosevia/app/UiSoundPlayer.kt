package com.dosevia.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import java.util.concurrent.atomic.AtomicBoolean

object UiSoundPlayer {
    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var soundPool: SoundPool? = null

    @Volatile
    private var tapSoundId: Int = 0

    @Volatile
    private var backSoundId: Int = 0

    @Volatile
    private var takenPressSoundId: Int = 0

    @Volatile
    private var takenSpinSoundId: Int = 0

    @Volatile
    private var takenCheckSoundId: Int = 0

    @Volatile
    private var purchaseSuccessSoundId: Int = 0

    private val initialized = AtomicBoolean(false)

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        if (initialized.get()) return
        synchronized(this) {
            if (initialized.get()) return
            try {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                soundPool = SoundPool.Builder()
                    .setMaxStreams(6)
                    .setAudioAttributes(attributes)
                    .build()
                    .also { pool ->
                        tapSoundId = pool.load(ctx, R.raw.ui_tap, 1)
                        backSoundId = pool.load(ctx, R.raw.ui_back, 1)
                        takenPressSoundId = pool.load(ctx, R.raw.taken_press, 1)
                        takenSpinSoundId = pool.load(ctx, R.raw.taken_spin, 1)
                        takenCheckSoundId = pool.load(ctx, R.raw.taken_check, 1)
                        purchaseSuccessSoundId = pool.load(ctx, R.raw.purchase_success, 1)
                    }
                initialized.set(true)
            } catch (_: Exception) {
                soundPool = null
            }
        }
    }

    private fun playLoaded(soundId: Int, leftVolume: Float, rightVolume: Float, rate: Float) {
        val pool = soundPool
        if (pool == null || soundId == 0) return
        try {
            pool.play(soundId, leftVolume, rightVolume, 1, 0, rate)
        } catch (_: Exception) {
        }
    }

    private fun ensureInit() {
        val ctx = appContext ?: return
        if (!initialized.get()) init(ctx)
    }

    fun playBack() {
        ensureInit()
        playLoaded(backSoundId, 0.72f, 0.72f, 0.98f)
    }

    fun playNavigate() {
        ensureInit()
        playLoaded(tapSoundId, 0.62f, 0.62f, 1.02f)
    }

    fun playPrimaryAction() {
        ensureInit()
        playLoaded(tapSoundId, 0.70f, 0.70f, 1.0f)
    }

    fun playToggle(enabled: Boolean) {
        ensureInit()
        val rate = if (enabled) 1.04f else 0.96f
        playLoaded(tapSoundId, 0.66f, 0.66f, rate)
    }

    fun playTaken() {
        ensureInit()
        playLoaded(tapSoundId, 0.74f, 0.74f, 1.06f)
    }

    fun playTakenPress() {
        ensureInit()
        playLoaded(takenPressSoundId, 0.74f, 0.74f, 1.0f)
    }

    fun playTakenSpin() {
        ensureInit()
        playLoaded(takenSpinSoundId, 0.68f, 0.68f, 1.06f)
    }

    fun playTakenCheck() {
        ensureInit()
        playLoaded(takenCheckSoundId, 0.78f, 0.78f, 1.0f)
    }

    fun playPurchaseSuccess() {
        ensureInit()
        playLoaded(purchaseSuccessSoundId, 0.82f, 0.82f, 1.0f)
    }

    fun playNotTaken() {
        ensureInit()
        playLoaded(backSoundId, 0.68f, 0.68f, 1.04f)
    }

    fun playMissed() {
        ensureInit()
        playLoaded(backSoundId, 0.76f, 0.76f, 0.92f)
    }

    fun release() {
        synchronized(this) {
            try {
                soundPool?.release()
            } catch (_: Exception) {
            }
            soundPool = null
            tapSoundId = 0
            backSoundId = 0
            takenPressSoundId = 0
            takenSpinSoundId = 0
            takenCheckSoundId = 0
            purchaseSuccessSoundId = 0
            initialized.set(false)
        }
    }
}
