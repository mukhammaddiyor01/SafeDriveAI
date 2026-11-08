package uz.safedrive.safedrive_ai.controller

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import uz.safedrive.safedrive_ai.model.MonitorState

class AlarmController(context: Context, prepareVoice: Boolean = true, onSource: (String) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val voiceCoach = lazy { VoiceCoach(context, onSource) }
    private val voice by voiceCoach
    private val preferences = context.getSharedPreferences("monitor", Context.MODE_PRIVATE)
    private var ringing = false
    private var urgent = false
    private var lastPrompt = -30_000L
    private var closed = false
    private val pulse = object : Runnable {
        override fun run() {
            if (!ringing || closed) return
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
            vibrator?.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
            handler.postDelayed(this, if (urgent) 900 else 4000)
        }
    }

    init {
        if (prepareVoice) voiceCoach.value
    }

    fun update(state: MonitorState) {
        val wasUrgent = urgent
        urgent = state.status == "drowsy"
        if (wasUrgent && !urgent) voice.stop()
        if (state.alarm && !ringing) {
            ringing = true
            handler.post(pulse)
        } else if (!state.alarm && ringing) {
            ringing = false
            handler.removeCallbacks(pulse)
            tone.stopTone()
            vibrator?.cancel()
        }
        val now = SystemClock.elapsedRealtime()
        if (urgent && state.alarm && now - lastPrompt >= 20_000 && preferences.getBoolean("voice", true)) {
            lastPrompt = now
            voice.prompt(preferences.getBoolean("llm", false))
        }
    }

    fun test() {
        ringing = true
        urgent = true
        handler.post(pulse)
        handler.postDelayed({ close() }, 2500)
    }

    fun close() {
        if (closed) return
        closed = true
        ringing = false
        handler.removeCallbacksAndMessages(null)
        tone.stopTone()
        tone.release()
        vibrator?.cancel()
        if (voiceCoach.isInitialized()) voice.close()
    }
}
