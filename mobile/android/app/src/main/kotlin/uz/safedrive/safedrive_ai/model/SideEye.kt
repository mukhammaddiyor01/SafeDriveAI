package uz.safedrive.safedrive_ai.model

import kotlin.math.abs
import kotlin.math.hypot

internal object SideEye {
    data class Point(val x: Float, val y: Float)

    fun supports(yaw: Float, pitch: Float, roll: Float): Boolean =
        abs(yaw) > 18f && abs(yaw) <= 45f && abs(pitch) <= 25f && abs(roll) <= 25f

    /** Returns null for small or ambiguous contours. The result is a state, not a probability. */
    fun isOpen(points: List<Point>): Boolean? {
        if (points.size != 16 || points.any { !it.x.isFinite() || !it.y.isFinite() }) return null

        val first = points[0]
        val opposite = points[8]
        val dx = opposite.x - first.x
        val dy = opposite.y - first.y
        val width = hypot(dx, dy)
        if (width < 12f) return null

        // Measure the central eyelid gap perpendicular to the line between the corners.
        val gapRatio = (3..5).map { index ->
            val upper = points[index]
            val lower = points[16 - index]
            ((lower.y - upper.y) * dx - (lower.x - upper.x) * dy) / (width * width)
        }
        if (gapRatio.any { it > 0.02f } && gapRatio.any { it < -0.02f }) return null
        val ratio = abs(gapRatio.average())
        return when {
            ratio <= 0.11 -> false
            ratio in 0.18..0.65 -> true
            else -> null
        }
    }
}
