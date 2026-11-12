package uz.safedrive.safedrive_ai.model

import java.nio.ByteBuffer
import kotlin.math.pow
import kotlin.math.roundToInt

enum class Lighting { NORMAL, LOW, INSUFFICIENT }

/** Assess original pixels: brightened noise cannot become eye evidence. */
internal data class LuminanceStats(val median: Int, val p10: Int, val p90: Int) {
    val lighting: Lighting
        get() = when {
            p90 < 12 || (p90 - p10 < 6 && median < 32) -> Lighting.INSUFFICIENT
            median < 64 || p90 < 90 -> Lighting.LOW
            else -> Lighting.NORMAL
        }

    companion object {
        fun fromHistogram(histogram: IntArray): LuminanceStats {
            require(histogram.size == 256 && histogram.all { it >= 0 })
            val count = histogram.sum()
            if (count < 16) return LuminanceStats(0, 0, 0)
            fun percentile(percent: Int): Int {
                val target = (count * percent + 99) / 100
                var cumulative = 0
                for (value in histogram.indices) {
                    cumulative += histogram[value]
                    if (cumulative >= target) return value
                }
                return 255
            }
            return LuminanceStats(percentile(50), percentile(10), percentile(90))
        }

        fun fromYPlane(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int, pixelStride: Int): LuminanceStats {
            val histogram = IntArray(256)
            val base = buffer.position()
            for (y in 0 until height step maxOf(1, height / 48)) {
                for (x in 0 until width step maxOf(1, width / 64)) {
                    val index = base + y * rowStride + x * pixelStride
                    if (index >= buffer.limit()) continue
                    val value = buffer.get(index).toInt() and 255
                    val luma = ((value - 16) * 255 / 219).coerceIn(0, 255)
                    histogram[luma]++
                }
            }
            return fromHistogram(histogram)
        }
    }
}

/** Prevent noisy twilight frames from switching image appearance every frame. */
internal class LowLightPolicy {
    private var enhancing = false
    private var brightFrames = 0

    fun shouldEnhance(stats: LuminanceStats): Boolean {
        if (stats.lighting == Lighting.INSUFFICIENT) {
            brightFrames = 0
            return false
        }
        if (stats.lighting == Lighting.LOW) {
            enhancing = true
            brightFrames = 0
        } else if (enhancing) {
            brightFrames = if (stats.median >= 85 && stats.p90 >= 110) brightFrames + 1 else 0
            if (brightFrames >= 5) enhancing = false
        }
        return enhancing
    }
}

/** Fixed, bounded curve. Black input stays black; no synthesized image detail. */
internal object LowLightTone {
    val lookup: IntArray = IntArray(256) { value ->
        minOf((255.0 * (value / 255.0).pow(0.65)).roundToInt(), value * 3)
    }
    fun correct(pixel: Int): Int = (pixel and -0x1000000) or
        (lookup[(pixel ushr 16) and 255] shl 16) or
        (lookup[(pixel ushr 8) and 255] shl 8) or lookup[pixel and 255]
}
