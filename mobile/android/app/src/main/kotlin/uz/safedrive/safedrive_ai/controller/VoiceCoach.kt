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

class VoiceCoach(context: Context, private val onSource: (String) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var closed = false
    private var ready = false
    private var uzbek = false
    private var generation = 0
    private lateinit var speech: TextToSpeech
    init {
        speech = TextToSpeech(context.applicationContext) { status ->
        handler.post {
            if (!closed && status == TextToSpeech.SUCCESS) {
                uzbek = speech.isLanguageAvailable(Locale.forLanguageTag("uz")) >= TextToSpeech.LANG_AVAILABLE
                speech.language = if (uzbek) Locale.forLanguageTag("uz") else Locale.US
                speech.setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                ready = true
            }
        }
        }
    }

    fun prompt(useLlm: Boolean) {
        if (closed || !ready) return
        val token = ++generation
        val warning = if (uzbek) "Xavfsiz joyda to‘xtab, dam oling." else "Please stop safely and rest."
        speech.speak(warning, TextToSpeech.QUEUE_FLUSH, null, "rest")
        if (!useLlm || !uzbek) {
            onSource(if (uzbek) "Mahalliy ovoz" else "English voice")
            speech.speak(if (uzbek) "Keyingi dam olish joyi qayerda?" else "Where is your next safe rest stop?",
                TextToSpeech.QUEUE_ADD, null, "question")
            return
        }
        executor.execute {
            var source = "Mahalliy ovoz"
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
                    if (data.optString("source") == "ollama") source = "Ollama LLM"
                    data.getString("text").take(300)
                } finally { connection.disconnect() }
            } catch (_: Exception) { "Keyingi dam olish joyi qayerda?" }
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
