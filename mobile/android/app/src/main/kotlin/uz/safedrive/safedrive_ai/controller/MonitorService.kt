package uz.safedrive.safedrive_ai.controller

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.core.Preview
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import uz.safedrive.safedrive_ai.MainActivity
import uz.safedrive.safedrive_ai.R
import uz.safedrive.safedrive_ai.model.DrowsinessEngine
import uz.safedrive.safedrive_ai.model.MonitorState

class MonitorService : LifecycleService() {
    private val handler = Handler(Looper.getMainLooper())
    private val engine = DrowsinessEngine()
    private var camera: CameraSession? = null
    private var alarm: AlarmController? = null
    private var failure = false
    private var lastFrame = 0L
    private var alerts = 0
    private var wasAlarm = false
    private var lastStatus = ""
    private var lastMessage = ""
    private var observations = 0L
    private var assistant = "Voice reminders"
    private val watchdog = object : Runnable {
        override fun run() {
            if (camera == null) return
            if (SystemClock.elapsedRealtime() - lastFrame >= 3_000) {
                update(MonitorState("limited", "No camera frames. Check the camera.", active = true, alarm = true))
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        MonitorRuntime.service = this
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Driver monitoring", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        if (camera != null) return START_NOT_STICKY
        try {
            val preferences = getSharedPreferences("monitor", MODE_PRIVATE)
            val microphoneEnabled = (preferences.getBoolean("llm", false) ||
                preferences.getBoolean("geminiLive", false)) &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                (if (microphoneEnabled) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0)
            ServiceCompat.startForeground(this, 31, notification("Starting camera"), serviceType)
            MonitorRuntime.publish(MonitorState("starting", "Face the camera", active = true))
            alarm = AlarmController(this, allowVoiceReply = microphoneEnabled) { source ->
                assistant = source
                MonitorRuntime.publish(MonitorRuntime.state.copy(assistant = source))
            }
            camera = CameraSession(this, this, { observation ->
                if (SystemClock.elapsedRealtime() - observation.timeMs > 700) {
                    engine.reset(clearEscalation = false)
                    update(MonitorState("limited", "Camera analysis slowed down. Check your phone.", active = true, alarm = true))
                } else {
                    lastFrame = SystemClock.elapsedRealtime()
                    observations++
                    update(engine.update(observation))
                }
            }, ::fail)
            MonitorRuntime.attachPreview()
            lastFrame = SystemClock.elapsedRealtime()
            camera?.start()
            handler.postDelayed(watchdog, 1000)
        } catch (error: Exception) {
            android.util.Log.e("SafeDriveAI", "Monitor startup failed", error)
            fail("Could not start monitoring. Check camera permission and try again with the app open.")
        }
        return START_NOT_STICKY
    }

    fun setPreview(surface: Preview.SurfaceProvider?) { camera?.setPreview(surface) }
    fun updateRotation() { camera?.updateRotation() }

    private fun update(state: MonitorState) {
        if (failure) return
        if (state.alarm && !wasAlarm) alerts++
        wasAlarm = state.alarm
        val next = state.copy(alerts = alerts, assistant = assistant)
        MonitorRuntime.publish(next)
        alarm?.update(next)
        if (lastStatus != state.status || lastMessage != state.message) {
            lastStatus = state.status
            lastMessage = state.message
            getSystemService(NotificationManager::class.java).notify(31, notification(state.message))
        }
    }

    private fun fail(message: String) {
        if (failure) return
        failure = true
        MonitorRuntime.publish(MonitorState("error", message))
        stopSelf()
    }

    private fun notification(message: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 1, Intent(this, MonitorService::class.java).setAction(STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_drive)
            .setContentTitle("SafeDriveAI monitoring").setContentText(message).setContentIntent(open)
            .setOngoing(true).setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_drive, "Stop", stop).build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        camera?.close()
        camera = null
        alarm?.close()
        engine.reset()
        MonitorRuntime.service = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (!failure) MonitorRuntime.publish(MonitorState())
        super.onDestroy()
    }

    override fun dump(fd: java.io.FileDescriptor, writer: java.io.PrintWriter, args: Array<out String>) {
        writer.println("SafeDriveAI status=${MonitorRuntime.state.status} active=${MonitorRuntime.state.active}")
        writer.println("observations=$observations lastFrameAgeMs=${SystemClock.elapsedRealtime() - lastFrame}")
        writer.println("alerts=$alerts")
        writer.println("eyeScore=${MonitorRuntime.state.eyesOpen} closedMs=${MonitorRuntime.state.closedMs}")
        writer.println("eyeDiagnostic=${camera?.eyeDiagnostic}")
    }

    companion object {
        private const val CHANNEL = "safedrive.monitor"
        const val STOP = "uz.safedrive.STOP"
    }
}
