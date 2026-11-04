package uz.safedrive.safedrive_ai.view

import android.content.Context
import android.view.View
import androidx.camera.view.PreviewView
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import uz.safedrive.safedrive_ai.controller.MonitorRuntime

class CameraPreviewFactory : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val preview = PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        MonitorRuntime.preview = preview
        MonitorRuntime.attachPreview()
        return object : PlatformView {
            override fun getView(): View = preview
            override fun dispose() {
                if (MonitorRuntime.preview === preview) {
                    MonitorRuntime.preview = null
                    MonitorRuntime.attachPreview()
                }
            }
        }
    }
}
