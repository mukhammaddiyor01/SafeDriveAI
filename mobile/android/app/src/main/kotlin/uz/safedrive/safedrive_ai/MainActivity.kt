package uz.safedrive.safedrive_ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import uz.safedrive.safedrive_ai.assistant.ChatGptAccount
import uz.safedrive.safedrive_ai.controller.AlarmController
import uz.safedrive.safedrive_ai.controller.MonitorRuntime
import uz.safedrive.safedrive_ai.controller.MonitorService
import uz.safedrive.safedrive_ai.model.MonitorState
import uz.safedrive.safedrive_ai.view.CameraPreviewFactory

class MainActivity : FlutterActivity() {
    private var pendingStart: MethodChannel.Result? = null
    private var startAfterResume: MethodChannel.Result? = null
    private var tester: AlarmController? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        flutterEngine.platformViewsController.registry.registerViewFactory("uz.safedrive/camera", CameraPreviewFactory())
        val messenger = flutterEngine.dartExecutor.binaryMessenger
        EventChannel(messenger, "uz.safedrive/states").setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, sink: EventChannel.EventSink) {
                MonitorRuntime.sink = sink
                sink.success(MonitorRuntime.state.toMap())
            }
            override fun onCancel(arguments: Any?) { MonitorRuntime.sink = null }
        })
        MethodChannel(messenger, "uz.safedrive/monitor").setMethodCallHandler { call, result ->
            val preferences = getSharedPreferences("monitor", MODE_PRIVATE)
            when (call.method) {
                "state" -> result.success(MonitorRuntime.state.toMap())
                "start" -> requestStart(result)
                "stop" -> { stopService(Intent(this, MonitorService::class.java)); result.success(null) }
                "testAlarm" -> {
                    if (MonitorRuntime.state.active) result.error("active", "Stop monitoring before testing the alarm.", null)
                    else if (!audible()) result.error("volume", "Turn up your phone’s alarm volume.", null)
                    else {
                        try {
                            tester?.close()
                            tester = AlarmController(this, prepareVoice = false) {}
                            tester?.test()
                            result.success(null)
                        } catch (_: Exception) { result.error("audio", "Could not play the alarm.", null) }
                    }
                }
                "chatGptAccount" -> result.success(runCatching { ChatGptAccount(this).email() }.getOrNull())
                "signInChatGpt", "signOutChatGpt" -> {
                    val signIn = call.method == "signInChatGpt"
                    Thread {
                        try {
                            val account = ChatGptAccount(this)
                            val value = if (signIn) account.signIn(this) else account.disconnect()
                            runOnUiThread { result.success(value) }
                        } catch (_: java.net.SocketTimeoutException) {
                            runOnUiThread { result.error("chatgpt_timeout", "ChatGPT did not respond in time. Try again.", null) }
                        } catch (error: Exception) {
                            val message = error.message?.takeIf {
                                it.startsWith("ChatGPT returned HTTP ") ||
                                    it in setOf("This ChatGPT account did not grant plan usage.",
                                        "No ChatGPT model is available for this account.",
                                        "ChatGPT sign-in was declined.", "ChatGPT sign-in timed out.")
                            } ?: "ChatGPT sign-in could not be completed. Check your account and internet connection."
                            runOnUiThread { result.error("chatgpt_account", message, null) }
                        }
                    }.start()
                }
                "preferences" -> {
                    if (call.arguments != null) {
                        preferences.edit().putBoolean("voice", call.argument<Boolean>("voice") ?: true)
                            .putBoolean("llm", call.argument<Boolean>("llm") ?: false).apply()
                    }
                    result.success(mapOf("voice" to preferences.getBoolean("voice", true),
                        "llm" to preferences.getBoolean("llm", false)))
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun audible() = getSystemService(AudioManager::class.java).getStreamVolume(AudioManager.STREAM_ALARM) > 0

    private fun requestStart(result: MethodChannel.Result) {
        if (pendingStart != null || startAfterResume != null) { result.error("busy", "Respond to the permission prompt.", null); return }
        if (!audible()) { result.error("volume", "Turn up your phone’s alarm volume.", null); return }
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissions += Manifest.permission.CAMERA
        if (getSharedPreferences("monitor", MODE_PRIVATE).getBoolean("llm", false) &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            permissions += Manifest.permission.RECORD_AUDIO
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            permissions += Manifest.permission.POST_NOTIFICATIONS
        if (permissions.isEmpty()) startMonitor(result)
        else { pendingStart = result; requestPermissions(permissions.toTypedArray(), 41) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 41) return
        val result = pendingStart ?: return
        pendingStart = null
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (MonitorRuntime.activityVisible) startMonitor(result) else startAfterResume = result
        }
        else result.error("permission", "Allow camera access in your phone settings.", null)
    }

    private fun startMonitor(result: MethodChannel.Result) {
        if (!MonitorRuntime.activityVisible) {
            result.error("background", "Open the app and restart monitoring.", null); return
        }
        try {
            tester?.close(); tester = null
            ContextCompat.startForegroundService(this, Intent(this, MonitorService::class.java))
            MonitorRuntime.publish(MonitorState("starting", "Starting camera", active = true))
            result.success(null)
        } catch (_: Exception) { result.error("service", "Could not start monitoring.", null) }
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        MonitorRuntime.activityVisible = true
        MonitorRuntime.attachPreview()
        startAfterResume?.let { result -> startAfterResume = null; startMonitor(result) }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        MonitorRuntime.service?.updateRotation()
    }

    override fun onPause() {
        MonitorRuntime.activityVisible = false
        MonitorRuntime.attachPreview()
        tester?.close(); tester = null
        super.onPause()
    }

    override fun onDestroy() {
        pendingStart?.error("closed", "The app closed. Open it and try again.", null)
        pendingStart = null
        startAfterResume?.error("closed", "The app closed. Open it and try again.", null)
        startAfterResume = null
        MonitorRuntime.sink = null
        super.onDestroy()
    }
}
