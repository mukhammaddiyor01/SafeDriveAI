package uz.safedrive.safedrive_ai.controller

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import uz.safedrive.safedrive_ai.model.Lighting
import uz.safedrive.safedrive_ai.model.LowLightPolicy
import uz.safedrive.safedrive_ai.model.LowLightTone
import uz.safedrive.safedrive_ai.model.LuminanceStats

/** Owns analysis bitmaps until both asynchronous detectors have finished. */
internal class PreparedCameraFrame(
    private val proxy: ImageProxy,
    previousFace: Rect?,
    policy: LowLightPolicy,
) : AutoCloseable {
    private var raw: Bitmap? = null
    private var corrected: Bitmap? = null
    private val scene: LuminanceStats = proxy.planes[0].let { plane ->
        LuminanceStats.fromYPlane(plane.buffer, proxy.width, proxy.height, plane.rowStride, plane.pixelStride)
    }
    var stats: LuminanceStats = previousFace?.let { measureFace(it) } ?: scene
        private set
    val enhanced = policy.shouldEnhance(stats)

    val lighting: Lighting
        get() = if (stats.lighting == Lighting.INSUFFICIENT) Lighting.INSUFFICIENT
            else if (enhanced) Lighting.LOW else stats.lighting
    val eyeEvidenceUsable: Boolean
        get() = stats.lighting != Lighting.INSUFFICIENT &&
            (stats.lighting != Lighting.LOW || enhanced)
    val diagnostic: String
        get() = "lighting=${lighting.name.lowercase()} enhanced=$enhanced luma=${stats.median} range=${stats.p10}..${stats.p90}"

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun input(): InputImage = if (enhanced) InputImage.fromBitmap(bitmap(), 0)
        else InputImage.fromMediaImage(checkNotNull(proxy.image), proxy.imageInfo.rotationDegrees)

    fun updateFace(bounds: Rect?) {
        stats = bounds?.let { measureFace(it) } ?: scene
    }

    private fun upright(): Bitmap {
        raw?.let { return it }
        val source = proxy.toBitmap()
        val rotation = proxy.imageInfo.rotationDegrees
        val result = if (rotation == 0) source else Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            Matrix().apply { postRotate(rotation.toFloat()) }, false)
        if (result !== source) source.recycle()
        raw = result
        return result
    }

    private fun measureFace(bounds: Rect): LuminanceStats {
        val image = upright()
        val region = Rect(bounds)
        if (!region.intersect(0, 0, image.width, image.height) || region.width() < 16 || region.height() < 16) return scene
        val histogram = IntArray(256)
        for (y in region.top until region.bottom step maxOf(1, region.height() / 48)) {
            for (x in region.left until region.right step maxOf(1, region.width() / 64)) {
                val pixel = image.getPixel(x, y)
                val r = (pixel ushr 16) and 255
                val g = (pixel ushr 8) and 255
                val b = pixel and 255
                histogram[(77 * r + 150 * g + 29 * b) ushr 8]++
            }
        }
        return LuminanceStats.fromHistogram(histogram)
    }

    fun bitmap(): Bitmap {
        val source = upright()
        if (!enhanced) return source
        corrected?.let { return it }
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        for (i in pixels.indices) pixels[i] = LowLightTone.correct(pixels[i])
        return Bitmap.createBitmap(pixels, source.width, source.height, Bitmap.Config.ARGB_8888).also {
            corrected = it
        }
    }

    override fun close() {
        corrected?.recycle()
        raw?.recycle()
        corrected = null
        raw = null
    }
}
