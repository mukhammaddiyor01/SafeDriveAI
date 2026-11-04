package uz.safedrive.safedrive_ai.controller

import androidx.camera.view.PreviewView
import io.flutter.plugin.common.EventChannel
import uz.safedrive.safedrive_ai.model.MonitorState

/** Main-thread bridge only. Camera frames never pass through Flutter channels. */
object MonitorRuntime {
    var state = MonitorState()
        private set
    var sink: EventChannel.EventSink? = null
    var service: MonitorService? = null
    var preview: PreviewView? = null
    var activityVisible = false

    fun publish(next: MonitorState) {
        state = next
        sink?.success(next.toMap())
    }

    fun attachPreview() {
        service?.setPreview(if (activityVisible) preview?.surfaceProvider else null)
    }
}
