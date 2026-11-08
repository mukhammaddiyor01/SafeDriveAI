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

class AlarmController(context: Context, onSource: (String) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val voice = VoiceCoach(context, onSource)
    private val preferences = context.getSharedPreferences("monitor", Context.MODE_PRIVATE)
    private var ringing = false
    private var urgent = false
    private var lastPrompt = -30_000L
    private var closed = false
    private var generation = 0
    private val pulse = object : Runnable {
        override fun run() {
            if (!ringing || closed) return
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
            vibrator?.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
            handler.postDelayed(this, if (urgent) 900 else 4000)
        }
    }

    fun update(state: MonitorState) {
        val wasUrgent = urgent
        urgent = state.status == "drowsy"
        if (wasUrgent && !urgent) voice.stop()
        if (state.alarm && !ringing) {
            generation++
            ringing = true
            handler.post(pulse)
        } else if (!state.alarm && ringing) {
            ringing = false
            handler.removeCallbacks(pulse)
            tone.stopTone()
            vibrator?.cancel()
            generation++
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
        val token = ++generation
        handler.post(pulse)
        voice.prompt(false)
        handler.postDelayed({
            if (generation == token) update(MonitorState())
        }, 2500)
    }

    fun close() {
        closed = true
        ringing = false
        handler.removeCallbacksAndMessages(null)
        tone.stopTone()
        tone.release()
        vibrator?.cancel()
        voice.close()
    }
}
