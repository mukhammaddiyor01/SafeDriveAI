package uz.safedrive.safedrive_ai.controller

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

private const val FALLBACK_QUESTION = "Where is the next safe place to pull over?"

class VoiceCoach(context: Context, private val onSource: (String) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var closed = false
    private var ready = false
    private var generation = 0
    private lateinit var speech: TextToSpeech
    init {
        speech = TextToSpeech(context.applicationContext) { status ->
            handler.post {
                if (!closed && status == TextToSpeech.SUCCESS) {
                    ready = speech.setLanguage(Locale.US) >= TextToSpeech.LANG_AVAILABLE
                    speech.setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    if (!ready) onSource("English voice unavailable")
                }
            }
        }
    }

    fun prompt(useLlm: Boolean) {
        if (closed || !ready) return
        val token = ++generation
        speech.speak("Pull over safely and rest.", TextToSpeech.QUEUE_FLUSH, null, "rest")
        if (!useLlm) {
            onSource("Local voice")
            speech.speak(FALLBACK_QUESTION, TextToSpeech.QUEUE_ADD, null, "question")
            return
        }
        executor.execute {
            var source = "Local voice"
            val text = try {
                val connection = URL("http://127.0.0.1:8765/question").openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 1500
                    connection.readTimeout = 9000
                    connection.doOutput = true
                    connection.outputStream.use { it.write(byteArrayOf()) }
                    val result = connection.inputStream.bufferedReader().use { it.readText() }
                    val data = JSONObject(result)
                    val question = data.getString("text").trim()
                    if (question.isBlank() || question.length > 300) {
                        FALLBACK_QUESTION
                    } else {
                        if (data.optString("source") == "openai") source = "ChatGPT"
                        question
                    }
                } finally { connection.disconnect() }
            } catch (_: Exception) { FALLBACK_QUESTION }
            handler.post {
                if (!closed && ready && token == generation) {
                    onSource(source)
                    speech.speak(text, TextToSpeech.QUEUE_ADD, null, "question")
                }
            }
        }
    }

    fun stop() { generation++; if (ready) speech.stop() }
    fun close() {
        closed = true
        speech.stop()
        speech.shutdown()
        executor.shutdownNow()
        handler.removeCallbacksAndMessages(null)
    }
}
