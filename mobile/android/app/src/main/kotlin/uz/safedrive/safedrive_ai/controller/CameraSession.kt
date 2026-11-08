package uz.safedrive.safedrive_ai.controller

import android.content.Context
import android.os.SystemClock
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import android.util.Size
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import uz.safedrive.safedrive_ai.model.Observation
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CameraSession(
    private val context: Context,
    private val owner: LifecycleOwner,
    private val onObservation: (Observation) -> Unit,
    private val onFailure: (String) -> Unit,
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)
    private val detector = FaceDetection.getClient(FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .enableTracking().setMinFaceSize(0.15f).build())
    private val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
        ).build()).build()
    private val preview = Preview.Builder().build()
    private var provider: ProcessCameraProvider? = null
    private var surface: Preview.SurfaceProvider? = null
    private var closed = false
    private var lastSubmitted = 0L

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun start() {
        analysis.setAnalyzer(executor) { proxy ->
            val now = SystemClock.elapsedRealtime()
            val image = proxy.image
            if (closed || image == null || now - lastSubmitted < 120 || !busy.compareAndSet(false, true)) {
                proxy.close()
                return@setAnalyzer
            }
            lastSubmitted = now
            val input = InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees)
            try {
                detector.process(input)
                    .addOnSuccessListener { faces ->
                        if (!closed) {
                            val face = faces.singleOrNull()
                            onObservation(Observation(
                                timeMs = now, faceCount = faces.size,
                                leftEyeOpen = face?.leftEyeOpenProbability,
                                rightEyeOpen = face?.rightEyeOpenProbability,
                                yaw = face?.headEulerAngleY ?: 0f,
                                pitch = face?.headEulerAngleX ?: 0f,
                                roll = face?.headEulerAngleZ ?: 0f,
                                trackingId = face?.trackingId,
                            ))
                        }
                    }
                    .addOnFailureListener { if (!closed) onFailure("Face detection failed. Restart monitoring.") }
                    .addOnCompleteListener { busy.set(false); proxy.close() }
            } catch (error: Exception) {
                android.util.Log.e("SafeDriveAI", "Frame submission failed", error)
                busy.set(false)
                proxy.close()
                if (!closed) ContextCompat.getMainExecutor(context).execute {
                    onFailure("Could not analyze the camera frame.")
                }
            }
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (closed) return@addListener
            try {
                provider = future.get()
                bind()
            } catch (error: Exception) {
                android.util.Log.e("SafeDriveAI", "Camera provider failed", error)
                onFailure("Could not open the front camera. Close other camera apps.")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setPreview(value: Preview.SurfaceProvider?) {
        if (surface === value) return
        surface = value
        if (provider != null && !closed) bind()
    }

    fun updateRotation() {
        val rotation = context.getSystemService(android.view.WindowManager::class.java)
            ?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        analysis.targetRotation = rotation
        preview.targetRotation = rotation
    }

    private fun bind() {
        val cameraProvider = provider ?: return
        try {
            cameraProvider.unbind(analysis, preview)
            // The service's lifecycle owns analysis. Hiding Flutter does not stop it.
            updateRotation()
            if (surface != null) {
                preview.setSurfaceProvider(surface)
                cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, analysis, preview)
            } else {
                preview.setSurfaceProvider(null)
                cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
            }
        } catch (error: Exception) {
            android.util.Log.e("SafeDriveAI", "Camera binding failed", error)
            onFailure("Camera unavailable. Check permissions and restart monitoring.")
        }
    }

    fun close() {
        closed = true
        analysis.clearAnalyzer()
        provider?.unbind(analysis, preview)
        detector.close()
        executor.shutdown()
    }
}
