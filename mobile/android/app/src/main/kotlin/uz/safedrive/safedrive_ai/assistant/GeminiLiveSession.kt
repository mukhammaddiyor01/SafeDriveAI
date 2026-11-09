package uz.safedrive.safedrive_ai.assistant

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Base64
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** The monitor owns one voice conversation from recovery until it ends. */
internal class GeminiLiveSession(
    private val brokerUrl: String?,
    private val brokerSecret: String?,
    private val onState: (String, Boolean) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val stopped = AtomicBoolean()
    private val audioLock = Any()
    private val connectionTimeout = Runnable { finish("Gemini Live unavailable", failed = true) }
    private var waitingForPlayback = true
    private var writtenFrames = 0L
    private val transcript = StringBuilder()
    private var socket: WebSocket? = null
    private var tokenCall: okhttp3.Call? = null
    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null

    fun start() {
        onState("Gemini Live connecting", false)
        handler.postDelayed(connectionTimeout, 20_000)
        val call = client.newCall(tokenRequest(brokerUrl, brokerSecret))
        tokenCall = call
        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) = unavailable()
            override fun onResponse(call: okhttp3.Call, response: Response) {
                try {
                    val credentials = response.use { readCredentials(it) }
                    handler.post { if (!stopped.get()) connect(credentials) }
                } catch (_: Exception) { unavailable() }
            }
        })
    }

    private fun connect(credentials: JSONObject) {
        try {
            val endpoint = ("https://generativelanguage.googleapis.com/ws/" +
                "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContentConstrained")
                .toHttpUrl().newBuilder()
                .addQueryParameter("access_token", credentials.getString("token")).build()
            val model = credentials.getString("model")
            socket = client.newWebSocket(Request.Builder().url(endpoint).build(), object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val instruction = """
                        You are SafeDriveAI, an English-speaking conversation partner.
                        Open with a brief reminder to pull over safely and rest, then start a casual conversation.
                        Listen to what the driver says and respond to it. If they ask a question, answer it directly
                        before moving on. Follow their topic changes and remember what they said earlier in this session.
                        Use varied, easy questions about everyday topics such as music, hobbies, or favorite places.
                        Keep replies short and natural. Ask at most one question at a time and wait for the reply.
                        You can answer without a follow-up when that feels natural. Keep the conversation open
                        until the driver says "I feel okay" as a standalone reply.
                        Never encourage them to keep driving or imply that conversation makes driving safe.
                    """.trimIndent()
                    val setup = JSONObject().put("model", model)
                        .put("generationConfig", JSONObject().put("responseModalities", JSONArray().put("AUDIO")))
                        .put("inputAudioTranscription", JSONObject())
                        .put("realtimeInputConfig", JSONObject().put("automaticActivityDetection",
                            JSONObject().put("silenceDurationMs", 650)))
                        .put("systemInstruction", JSONObject().put("parts",
                            JSONArray().put(JSONObject().put("text", instruction))))
                    webSocket.send(JSONObject().put("setup", setup).toString())
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) = onMessage(webSocket, bytes.utf8())

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (stopped.get()) return
                    try {
                        val event = JSONObject(text)
                        if (event.has("setupComplete")) {
                            handler.post { if (!stopped.get()) startAudio(webSocket) }
                        }
                        val content = event.optJSONObject("serverContent") ?: return
                        val inputTranscript = content.optJSONObject("inputTranscription")
                        if (inputTranscript != null && transcript.length < 512) {
                            transcript.append(inputTranscript.optString("text").take(512 - transcript.length))
                        }
                        val turnComplete = content.optBoolean("turnComplete")
                        if (turnComplete || inputTranscript?.optBoolean("finished") == true) {
                            val words = transcript.toString().lowercase()
                                .replace(Regex("[^a-z]+"), " ").trim()
                            transcript.clear()
                            if (words == "i feel okay" || words == "i feel ok") {
                                handler.post { finish("Gemini Live ended") }
                                return
                            }
                        }
                        synchronized(audioLock) {
                            if (stopped.get()) return
                            if (content.optBoolean("interrupted")) {
                                player?.apply { pause(); flush(); play() }
                                writtenFrames = 0
                            }
                            val parts = content.optJSONObject("modelTurn")?.optJSONArray("parts")
                            for (index in 0 until (parts?.length() ?: 0)) {
                                val audio = parts?.optJSONObject(index)?.optJSONObject("inlineData") ?: continue
                                if (!audio.optString("mimeType").startsWith("audio/pcm")) continue
                                waitingForPlayback = true
                                val pcm = Base64.decode(audio.getString("data"), Base64.DEFAULT)
                                val written = player?.write(pcm, 0, pcm.size) ?: 0
                                check(written >= 0)
                                writtenFrames += written / 2
                            }
                            if (turnComplete) waitingForPlayback = false
                        }
                    } catch (_: Exception) { unavailable() }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = unavailable()
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    handler.post { finish("Gemini Live ended") }
                }
            })
        } catch (_: Exception) { unavailable() }
    }

    @SuppressLint("MissingPermission")
    private fun startAudio(webSocket: WebSocket) {
        if (recorder != null) return
        try {
            val inputSize = AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(3200)
            val outputSize = AudioTrack.getMinBufferSize(24_000, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4800)
            val input = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16_000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, inputSize)
            recorder = input
            synchronized(audioLock) {
                player = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(24_000)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                    .setBufferSizeInBytes(outputSize).build().apply { play() }
            }
            input.startRecording()
            check(input.recordingState == AudioRecord.RECORDSTATE_RECORDING)
            handler.removeCallbacks(connectionTimeout)
            onState("Gemini Live active", false)
            sendInput(webSocket, JSONObject().put("text",
                "The driver has opened their eyes after an alert. Give the opening reminder and start a casual conversation."))
            Thread {
                try {
                    val buffer = ByteArray(1600)
                    var microphoneStreaming = false
                    while (!stopped.get()) {
                        val count = input.read(buffer, 0, buffer.size)
                        if (count <= 0 || stopped.get()) break
                        val canListen = synchronized(audioLock) {
                            !waitingForPlayback &&
                                (player?.playbackHeadPosition?.toLong() ?: 0L) >= writtenFrames
                        }
                        if (!canListen) {
                            if (microphoneStreaming) {
                                if (!sendInput(webSocket, JSONObject().put("audioStreamEnd", true))) break
                                microphoneStreaming = false
                            }
                            continue
                        }
                        val audio = JSONObject().put("mimeType", "audio/pcm;rate=16000")
                            .put("data", Base64.encodeToString(buffer, 0, count, Base64.NO_WRAP))
                        if (!sendInput(webSocket, JSONObject().put("audio", audio))) break
                        microphoneStreaming = true
                    }
                } catch (_: Exception) {
                    // The microphone may be interrupted by Android or revoked mid-session.
                } finally {
                    input.release()
                    if (!stopped.get()) unavailable()
                }
            }.start()
        } catch (_: Exception) {
            recorder?.release()
            recorder = null
            unavailable()
        }
    }

    private fun sendInput(webSocket: WebSocket, input: JSONObject): Boolean =
        webSocket.send(JSONObject().put("realtimeInput", input).toString())

    private fun unavailable() { handler.post { finish("Gemini Live unavailable", failed = true) } }

    fun finish(label: String = "Gemini Live ended", failed: Boolean = false) {
        if (!stopped.compareAndSet(false, true)) return
        handler.removeCallbacks(connectionTimeout)
        tokenCall?.cancel()
        socket?.cancel()
        runCatching { recorder?.stop() }
        recorder = null
        synchronized(audioLock) {
            player?.release()
            player = null
        }
        onState(label, failed)
    }

    companion object {
        private val client = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()

        private fun tokenRequest(url: String?, secret: String?): Request =
            Request.Builder().url(url ?: "http://127.0.0.1:8765/live/token")
                .post(ByteArray(0).toRequestBody())
                .apply { if (url != null && secret != null) header("Authorization", "Bearer $secret") }
                .build()

        private fun readCredentials(response: Response): JSONObject {
            check(response.isSuccessful)
            val payload = JSONObject(checkNotNull(response.body).string())
            check(payload.getString("token").isNotBlank() && payload.getString("model").isNotBlank())
            return payload
        }

        /** Check the same token endpoint the alarm will use. */
        fun checkBroker(url: String, secret: String): String? = try {
            client.newCall(tokenRequest(url, secret)).execute().use { response ->
                when (response.code) {
                    401, 403 -> "The Gemini Live server rejected the access token."
                    404 -> "The Gemini Live token endpoint was not found. Check the server URL."
                    503 -> "The Gemini Live server could not issue a Gemini token."
                    else -> {
                        readCredentials(response)
                        null
                    }
                }
            }
        } catch (_: javax.net.ssl.SSLException) {
            "The Gemini Live server has an invalid or untrusted HTTPS certificate."
        } catch (_: java.io.IOException) {
            "The phone could not reach the Gemini Live server. Check its address and internet connection."
        } catch (_: Exception) {
            "The server did not return a valid Gemini Live token."
        }
    }
}
