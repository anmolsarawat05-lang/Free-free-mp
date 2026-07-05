package com.example.game

import android.media.AudioManager
import android.media.ToneGenerator
import kotlin.concurrent.thread

object GameSound {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playClick() {
        thread {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
            } catch (e: Exception) {
                // fallback
            }
        }
    }

    fun playShoot(recoilMultiplier: Float) {
        thread {
            try {
                // Different weapons can have different tones
                val duration = if (recoilMultiplier > 5f) 180 else 80
                val toneType = if (recoilMultiplier > 5f) ToneGenerator.TONE_CDMA_PIP else ToneGenerator.TONE_PROP_ACK
                toneGenerator?.startTone(toneType, duration)
            } catch (e: Exception) {
                // ignore fallback
            }
        }
    }

    fun playHit() {
        thread {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 100)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun playHeal() {
        thread {
            try {
                // Rising scale
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 100)
                Thread.sleep(100)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_5, 100)
                Thread.sleep(100)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 150)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun playZoneAlert() {
        thread {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun playBooyah() {
        thread {
            try {
                // Triumphant arpeggio
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_3, 150)
                Thread.sleep(150)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_6, 150)
                Thread.sleep(150)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 200)
                Thread.sleep(200)
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 400)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun playEliminated() {
        thread {
            try {
                // Sad falling tones
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 200)
                Thread.sleep(200)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_5, 200)
                Thread.sleep(200)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 300)
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
