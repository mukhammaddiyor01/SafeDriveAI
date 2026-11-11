package uz.safedrive.safedrive_ai.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import kotlin.math.hypot
import uz.safedrive.safedrive_ai.model.SideEye
import uz.safedrive.safedrive_ai.model.SideEyeCalibration

/** Synchronous on-device model. CameraSession owns its worker thread and lifetime. */
internal class DenseEyeDetector(context: Context) : AutoCloseable {
    private val detector = FaceLandmarker.createFromOptions(context,
        FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_landmarker.task").build())
            .setNumFaces(2)
            .setOutputFaceBlendshapes(true)
            .build())

    private val calibration = SideEyeCalibration()
    private var trackingId: Int? = null

    fun trackFace(id: Int?) {
        if (id != null && trackingId != null && id != trackingId) calibration.reset()
        if (id != null) trackingId = id
    }

    var diagnostic: String = "waiting"
        private set

    var openness: Float? = null
        private set

    fun observe(bitmap: Bitmap, bounds: Rect, yaw: Float): Boolean? {
        openness = null
        val region = Rect(bounds)
        val padding = (maxOf(region.width(), region.height()) * 0.2f).toInt()
        region.inset(-padding, -padding)
        if (!region.intersect(0, 0, bitmap.width, bitmap.height) || region.width() < 32 || region.height() < 32) {
            diagnostic = "face-region-invalid"
            return null
        }
        val regionBitmap = Bitmap.createBitmap(bitmap, region.left, region.top, region.width(), region.height())
        // MPImage owns its bitmap. Preserve the caller's frame when the region is the full image.
        val crop = if (regionBitmap === bitmap) bitmap.copy(Bitmap.Config.ARGB_8888, false) else regionBitmap
        val image = BitmapImageBuilder(crop).build()
        try {
            diagnostic = "face-not-found"
            val result = detector.detect(image)
            val face = result.faceLandmarks().singleOrNull() ?: return null
            diagnostic = "blink-scores-unavailable"
            val scores = result.faceBlendshapes().orElse(emptyList()).singleOrNull() ?: return null
            val left = yaw < 0
            val indexes = if (left) listOf(263, 387, 385, 362, 380, 373)
                else listOf(33, 160, 158, 133, 153, 144)
            val points = indexes.map { index ->
                val point = face[index]
                Pair(region.left + point.x() * crop.width, region.top + point.y() * crop.height)
            }
            diagnostic = "eye-outside-face"
            val nose = face[1]
            if (!bounds.contains((region.left + nose.x() * crop.width).toInt(), (region.top + nose.y() * crop.height).toInt())) return null
            if (points.any { !bounds.contains(it.first.toInt(), it.second.toInt()) }) return null
            fun distance(a: Int, b: Int): Float =
                hypot(points[a].first - points[b].first, points[a].second - points[b].second)
            val width = distance(0, 3)
            diagnostic = "eye-width=$width"
            if (width < 10f) return null
            val ratio = (distance(1, 5) + distance(2, 4)) / (2f * width)
            val category = if (left) "eyeBlinkLeft" else "eyeBlinkRight"
            val blink = scores.singleOrNull { it.categoryName() == category }?.score() ?: return null
            openness = calibration.openness(yaw, blink, ratio, width)
            val open = openness?.let { it > SideEye.CLOSED_SCORE_THRESHOLD }
            diagnostic = "yaw=$yaw blink=$blink gap=$ratio width=$width score=$openness open=$open"
            return open
        } finally {
            image.close()
            if (!crop.isRecycled) crop.recycle()
        }
    }

    override fun close() = detector.close()
}
