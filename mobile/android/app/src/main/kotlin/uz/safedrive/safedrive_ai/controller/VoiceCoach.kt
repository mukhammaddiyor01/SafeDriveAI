package uz.safedrive.safedrive_ai.controller

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class VoiceCoach(
    context: Context,
    private val allowVoiceReply: Boolean,
    private val onSource: (String) -> Unit,
) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var closed = false
    private var ready = false
    private var generation = 0
    private var conversationActive = false
    private var recognizer: SpeechRecognizer? = null
    private val endListening = Runnable {
        releaseRecognizer()
        conversationActive = false
    }
    private val speech = TextToSpeech(appContext) { status ->
        handler.post { configureSpeech(status) }
    }

    private fun configureSpeech(status: Int) {
        if (closed) return
        ready = status == TextToSpeech.SUCCESS &&
            speech.setLanguage(Locale.US) >= TextToSpeech.LANG_AVAILABLE
        if (!ready) {
            onSource("English voice unavailable")
            return
        }
        speech.setSpeechRate(1.18f)
        speech.setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        speech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String) = Unit
            override fun onDone(id: String) {
                handler.post {
                    if (!conversationActive) return@post
                    if (id == "question-$generation") listenForReply()
                    if (id == "reply-$generation") conversationActive = false
                }
            }
            @Deprecated("Required by Android")
            override fun onError(id: String) {
                handler.post {
                    if (id == "question-$generation" || id == "reply-$generation") cancelConversation()
                }
            }
        })
    }

    fun warn() {
        if (closed || !ready) return
        cancelConversation()
        onSource("Local voice")
        speech.speak("Pull over safely.", TextToSpeech.QUEUE_FLUSH, null, "rest")
    }

    fun startConversation() {
        if (closed || !ready) return
        conversationActive = true
        generation++
        requestTurn(null)
    }

    private fun requestTurn(reply: String?) {
        val token = generation
        executor.execute {
            val text = try {
                val connection = URL("http://127.0.0.1:8765/turn").openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 1500
                    connection.readTimeout = 9000
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    val body = JSONObject()
                    if (reply != null) body.put("reply", reply)
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    val result = connection.inputStream.bufferedReader().use { it.readText() }
                    JSONObject(result).getString("text").takeIf { it.isNotBlank() && it.length <= 300 }
                } finally { connection.disconnect() }
            } catch (_: Exception) { null }
            handler.post {
                if (closed || !conversationActive || token != generation) return@post
                onSource(if (text == null) "ChatGPT unavailable" else "ChatGPT")
                val utterance = when {
                    text == null -> "Pull over safely and rest."
                    reply != null -> text
                    !allowVoiceReply -> {
                        onSource("ChatGPT · microphone unavailable")
                        text
                    }
                    else -> text
                }
                val id = if (text != null && reply == null && allowVoiceReply) "question-$token" else "reply-$token"
                if (speech.speak(utterance, TextToSpeech.QUEUE_ADD, null, id) == TextToSpeech.ERROR) {
                    cancelConversation()
                }
            }
        }
    }

    private fun listenForReply() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            conversationActive = false
            onSource("ChatGPT · speech recognition unavailable")
            return
        }
        val token = generation
        try {
            val listener = SpeechRecognizer.createSpeechRecognizer(appContext)
            recognizer = listener
            listener.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    if (!conversationActive || token != generation || recognizer !== listener) return
                    releaseRecognizer()
                    val reply = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim()?.take(300)
                    if (reply.isNullOrEmpty()) conversationActive = false else requestTurn(reply)
                }
                override fun onError(error: Int) {
                    if (recognizer !== listener) return
                    releaseRecognizer()
                    conversationActive = false
                }
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            listener.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true))
            handler.postDelayed(endListening, 8_000)
        } catch (_: RuntimeException) {
            releaseRecognizer()
            conversationActive = false
            onSource("ChatGPT · speech recognition unavailable")
        }
    }

    private fun releaseRecognizer() {
        handler.removeCallbacks(endListening)
        val listener = recognizer
        recognizer = null
        listener?.destroy()
    }

    fun cancelConversation() {
        if (conversationActive) stop()
    }

    fun stop() {
        generation++
        conversationActive = false
        releaseRecognizer()
        if (ready) speech.stop()
    }

    fun close() {
        closed = true
        stop()
        speech.shutdown()
        executor.shutdownNow()
        handler.removeCallbacksAndMessages(null)
    }
}
