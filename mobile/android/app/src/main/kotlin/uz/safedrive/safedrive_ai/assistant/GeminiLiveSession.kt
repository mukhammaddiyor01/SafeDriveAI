package uz.safedrive.safedrive_ai.assistant

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
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

/** One bounded voice conversation. The monitor owns its lifetime. */
internal class GeminiLiveSession(
    private val brokerUrl: String?,
    private val brokerSecret: String?,
    private val onState: (String, Boolean) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val stopped = AtomicBoolean()
    private val audioLock = Any()
    private val deadline = Runnable { finish("Gemini Live ended") }
    private var socket: WebSocket? = null
    private var tokenCall: okhttp3.Call? = null
    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null

    fun start() {
        onState("Gemini Live connecting", false)
        handler.postDelayed(deadline, 60_000)
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
                    val instruction = "You are SafeDriveAI. In English, ask the driver to pull over safely " +
                        "and rest. Keep each reply short. Never tell them to keep driving or rely on the app."
                    val setup = JSONObject().put("model", model)
                        .put("generationConfig", JSONObject().put("responseModalities", JSONArray().put("AUDIO")))
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
                        if (content.optBoolean("interrupted")) synchronized(audioLock) {
                            player?.apply { pause(); flush(); play() }
                        }
                        val parts = content.optJSONObject("modelTurn")?.optJSONArray("parts") ?: return
                        for (index in 0 until parts.length()) {
                            val audio = parts.optJSONObject(index)?.optJSONObject("inlineData") ?: continue
                            if (!audio.optString("mimeType").startsWith("audio/pcm")) continue
                            val pcm = Base64.decode(audio.getString("data"), Base64.DEFAULT)
                            synchronized(audioLock) {
                                if (!stopped.get()) player?.write(pcm, 0, pcm.size)
                            }
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
            val input = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 16_000,
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
            onState("Gemini Live active", false)
            sendInput(webSocket, JSONObject().put("text",
                "The driver has opened their eyes after an alert. Ask where they can safely stop and rest."))
            Thread {
                var echo: AcousticEchoCanceler? = null
                try {
                    echo = runCatching {
                        AcousticEchoCanceler.create(input.audioSessionId)?.apply { enabled = true }
                    }.getOrNull()
                    val buffer = ByteArray(1600)
                    while (!stopped.get()) {
                        val count = input.read(buffer, 0, buffer.size)
                        if (count <= 0) break
                        val audio = JSONObject().put("mimeType", "audio/pcm;rate=16000")
                            .put("data", Base64.encodeToString(buffer, 0, count, Base64.NO_WRAP))
                        if (!sendInput(webSocket, JSONObject().put("audio", audio))) break
                    }
                } catch (_: Exception) {
                    // The microphone may be interrupted by Android or revoked mid-session.
                } finally {
                    echo?.release()
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
        handler.removeCallbacks(deadline)
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
