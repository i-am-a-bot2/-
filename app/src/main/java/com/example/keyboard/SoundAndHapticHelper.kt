package com.example.keyboard

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.KeyboardStateBus

/**
 * Provides tactile vibration and classic Windows sound feedback compatible from
 * Android 4.4 (API 19 KitKat) through Android 16 (API 36).
 */
object SoundAndHapticHelper {

    private var toneGenerator: ToneGenerator? = null

    private fun getOrCreateToneGenerator(): ToneGenerator? {
        if (toneGenerator == null) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 45)
            } catch (_: Throwable) {
                toneGenerator = null
            }
        }
        return toneGenerator
    }

    fun performKeyTapFeedback(context: Context, isModifierOrSpecial: Boolean = false) {
        val config = KeyboardStateBus.state.value
        if (config.vibrationEnabled) {
            vibrateCompat(context, if (isModifierOrSpecial) 22L else 12L)
        }
        if (config.soundEnabled) {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.playSoundEffect(
                    if (isModifierOrSpecial) AudioManager.FX_KEYPRESS_SPACEBAR
                    else AudioManager.FX_KEYPRESS_STANDARD,
                    0.6f
                )
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * Plays the distinctive Windows Classic pop-up chime & double pulse when long-pressing 0
     * brings up the Classic Windows Keyboard anywhere on the screen.
     */
    fun performZeroKeyPopUpFeedback(context: Context) {
        val config = KeyboardStateBus.state.value
        if (config.vibrationEnabled) {
            vibrateCompat(context, 55L)
        }
        if (config.soundEnabled) {
            try {
                getOrCreateToneGenerator()?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            } catch (_: Throwable) {
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateCompat(context: Context, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    // Android 4.4 (API 19) through API 25 direct vibration call
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Throwable) {
        }
    }
}
