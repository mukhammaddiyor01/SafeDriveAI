package uz.safedrive.safedrive_ai.controller

import android.content.Context
import android.graphics.Rect
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
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import uz.safedrive.safedrive_ai.model.Lighting
import uz.safedrive.safedrive_ai.model.LowLightPolicy
import uz.safedrive.safedrive_ai.model.Observation
import uz.safedrive.safedrive_ai.model.SideEye
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
    private val denseEye = lazy { DenseEyeDetector(context) }
    private val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
        ).build()).build()
    private val preview = Preview.Builder().build()
    private var provider: ProcessCameraProvider? = null
    private var surface: Preview.SurfaceProvider? = null
    @Volatile private var closed = false
    @Volatile var eyeDiagnostic = "waiting"
        private set
    private var lastSubmitted = 0L
    private var lastEyeFailure = -10_000L
    private val lowLight = LowLightPolicy()
    private var previousFace: Rect? = null
    private var previousRotation: Int? = null
    private var lastFaceAt = 0L

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
            var prepared: PreparedCameraFrame? = null
            try {
                val rotation = proxy.imageInfo.rotationDegrees
                if (rotation != previousRotation) previousFace = null
                previousRotation = rotation
                val recentFace = previousFace?.takeIf { now - lastFaceAt < 2_000 }
                val frame = PreparedCameraFrame(proxy, recentFace, lowLight)
                prepared = frame
                val observation = if (frame.lighting == Lighting.INSUFFICIENT) {
                    if (denseEye.isInitialized()) denseEye.value.resetCalibration()
                    eyeDiagnostic = frame.diagnostic
                    Tasks.forResult(Observation(now, 0, lighting = Lighting.INSUFFICIENT))
                } else {
                    detector.process(frame.input()).onSuccessTask { faces ->
                        if (closed) Tasks.forResult(Observation(now, 0))
                        else Tasks.call(executor) { analyzeFaces(frame, faces, now) }
                    }
                }
                observation
                    .addOnSuccessListener { if (!closed) onObservation(it) }
                    .addOnFailureListener { if (!closed) onFailure("Face detection failed. Restart monitoring.") }
                    .addOnCompleteListener {
                        frame.close()
                        proxy.close()
                        busy.set(false)
                    }
            } catch (error: Exception) {
                android.util.Log.e("SafeDriveAI", "Frame submission failed", error)
                prepared?.close()
                proxy.close()
                busy.set(false)
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

    private fun analyzeFaces(frame: PreparedCameraFrame, faces: List<Face>, timeMs: Long): Observation {
        val face = faces.singleOrNull()
        val faceBounds = face?.boundingBox?.let { Rect(it) }
        if (faceBounds != null) {
            previousFace = faceBounds
            lastFaceAt = timeMs
        }
        frame.updateFace(faceBounds)
        val usable = frame.eyeEvidenceUsable
        val observation = Observation(
            timeMs = timeMs, faceCount = faces.size,
            leftEyeOpen = if (usable) face?.leftEyeOpenProbability else null,
            rightEyeOpen = if (usable) face?.rightEyeOpenProbability else null,
            yaw = face?.headEulerAngleY ?: 0f,
            pitch = face?.headEulerAngleX ?: 0f,
            roll = face?.headEulerAngleZ ?: 0f,
            trackingId = face?.trackingId,
            lighting = frame.lighting,
        )
        val diagnostic = "${frame.diagnostic} faces=${faces.size} yaw=${observation.yaw} pitch=${observation.pitch} roll=${observation.roll}"
        if (closed || face == null || !usable ||
            !SideEye.supports(observation.yaw, observation.pitch, observation.roll)) {
            if (!usable && denseEye.isInitialized()) denseEye.value.resetCalibration()
            eyeDiagnostic = diagnostic
            return observation
        }
        return try {
            val eye = denseEye.value
            eye.trackFace(observation.trackingId)
            eye.setEnhancement(frame.enhanced)
            val open = eye.observe(frame.bitmap(), face.boundingBox, observation.yaw)
            eyeDiagnostic = diagnostic + " " + eye.diagnostic
            observation.copy(sideEyeOpen = open, sideEyeOpenness = eye.openness)
        } catch (error: Exception) {
            eyeDiagnostic = diagnostic + " model-error:${error.javaClass.simpleName}"
            val failureTime = SystemClock.elapsedRealtime()
            if (failureTime - lastEyeFailure >= 10_000) {
                lastEyeFailure = failureTime
                android.util.Log.w("SafeDriveAI", "Side-eye analysis unavailable", error)
            }
            observation
        }
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
        executor.execute { if (denseEye.isInitialized()) denseEye.value.close() }
        executor.shutdown()
    }
}
