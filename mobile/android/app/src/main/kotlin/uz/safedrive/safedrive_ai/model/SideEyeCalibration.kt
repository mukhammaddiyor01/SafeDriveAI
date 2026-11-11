package uz.safedrive.safedrive_ai.model

import kotlin.math.abs

/** Session-local open-eye references, separated by side and camera angle. */
internal class SideEyeCalibration {
    private data class Reference(var gap: Float, var samples: Int = 1)
    private val references = mutableMapOf<Int, Reference>()

    fun reset() = references.clear()

    fun openness(yaw: Float, blink: Float, gap: Float, width: Float): Float? {
        if (!yaw.isFinite() || !blink.isFinite() || !gap.isFinite() || !width.isFinite() ||
            blink !in 0f..1f || gap !in 0f..0.65f || width < 10f) return null
        val bucket = (abs(yaw).toInt() / 15) * if (yaw < 0) -1 else 1
        if (blink <= 0.25f && gap >= 0.12f) {
            val previous = references[bucket]
            if (previous == null) references[bucket] = Reference(gap)
            else if (gap >= previous.gap * 0.8f && gap <= previous.gap * 1.25f) {
                previous.gap = previous.gap * 0.9f + gap * 0.1f
                previous.samples++
            }
        }
        val reference = references[bucket]?.takeIf { it.samples >= 5 }
            ?: return SideEye.openness(blink, gap, width)
        // Reference-open = 100%; a 40% loss of aperture reaches the 50% score boundary.
        val score = ((gap / reference.gap - 0.2f) / 0.8f).coerceIn(0f, 1f)
        return when {
            score <= SideEye.CLOSED_SCORE_THRESHOLD && blink >= 0.3f -> score
            score > SideEye.CLOSED_SCORE_THRESHOLD && blink <= 0.35f -> score
            else -> null
        }
    }
}
