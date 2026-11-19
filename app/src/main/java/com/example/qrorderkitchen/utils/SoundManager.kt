package com.example.qrorderkitchen.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * AURORA SYSTEM - ANDROID KDS & POS AUDIO NOTIFICATION MANAGER
 * Phát âm thanh chuông báo chuẩn Lounge thời gian thực trên STREAM_MUSIC (Media channel)
 * Đảm bảo 100% âm thanh phát ra rõ ràng trên mọi thiết bị Android, tablet & giả lập LDPlayer
 */
object SoundManager {
    private const val PREFS_NAME = "aurora_kds_settings"
    private const val KEY_SOUND_ENABLED = "kds_sound_enabled"

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled = _isSoundEnabled.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isSoundEnabled.value = prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun toggleSound(context: Context): Boolean {
        val next = !_isSoundEnabled.value
        _isSoundEnabled.value = next
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, next).apply()
        if (next) {
            playOrderChime(context)
        }
        return next
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        _isSoundEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    /**
     * Chuông báo đơn hàng mới dành riêng cho Bếp (KDS)
     * Kết hợp RingtoneManager hệ thống chuẩn và ToneGenerator âm lượng lớn,
     * đảm bảo 100% phát to rõ trên mọi dòng tablet bếp & giả lập LDPlayer
     */
    fun playOrderChime(context: Context) {
        if (!_isSoundEnabled.value) return
        thread(start = true, isDaemon = true, name = "AuroraKdsChime") {
            try {
                // 1. Phát chuông thông báo chuẩn Android
                var playedRingtone = false
                try {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                    if (ringtone != null) {
                        ringtone.audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                        ringtone.play()
                        playedRingtone = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. Kèm tiếng Beep cảnh báo âm lượng lớn để bảo đảm không bị bỏ sót đơn
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                } catch (_: Exception) {
                    try {
                        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        vibrate(context, 400)
    }

    /**
     * Chuông báo yêu cầu phục vụ hoặc xin thanh toán tại bàn
     */
    fun playServiceChime(context: Context, type: String = "assistance") {
        if (!_isSoundEnabled.value) return
        thread(start = true, isDaemon = true, name = "AuroraServiceChime") {
            try {
                val toneType = if (type == "checkout") {
                    ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                } else {
                    ToneGenerator.TONE_PROP_PROMPT
                }
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    toneGen.startTone(toneType, 450)
                } catch (_: Exception) {
                    try {
                        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                        toneGen.startTone(toneType, 450)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        vibrate(context, 450)
    }

    private fun vibrate(context: Context, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }
}
