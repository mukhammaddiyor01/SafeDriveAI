package uz.safedrive.safedrive_ai.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import kotlin.math.hypot
import uz.safedrive.safedrive_ai.model.SideEye

/** Synchronous on-device model. CameraSession owns its worker thread and lifetime. */
internal class DenseEyeDetector(context: Context) : AutoCloseable {
    private val detector = FaceLandmarker.createFromOptions(context,
        FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_landmarker.task").build())
            .setNumFaces(2)
            .setOutputFaceBlendshapes(true)
            .build())

    var diagnostic: String = "waiting"
        private set

    fun observe(bitmap: Bitmap, bounds: Rect, yaw: Float): Boolean? {
        val image = BitmapImageBuilder(bitmap).build()
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
                Pair(point.x() * bitmap.width, point.y() * bitmap.height)
            }
            diagnostic = "eye-outside-face"
            val nose = face[1]
            if (!bounds.contains((nose.x() * bitmap.width).toInt(), (nose.y() * bitmap.height).toInt())) return null
            if (points.any { !bounds.contains(it.first.toInt(), it.second.toInt()) }) return null
            fun distance(a: Int, b: Int): Float =
                hypot(points[a].first - points[b].first, points[a].second - points[b].second)
            val width = distance(0, 3)
            diagnostic = "eye-width=$width"
            if (width < 10f) return null
            val ratio = (distance(1, 5) + distance(2, 4)) / (2f * width)
            val category = if (left) "eyeBlinkLeft" else "eyeBlinkRight"
            val blink = scores.singleOrNull { it.categoryName() == category }?.score() ?: return null
            val open = SideEye.classify(blink, ratio, width)
            diagnostic = "yaw=$yaw blink=$blink gap=$ratio width=$width open=$open"
            return open
        } finally {
            image.close()
        }
    }

    override fun close() = detector.close()
}
