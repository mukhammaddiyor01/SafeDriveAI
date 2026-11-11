package uz.safedrive.safedrive_ai.model

import kotlin.math.abs

internal object SideEye {
    fun supports(yaw: Float, pitch: Float, roll: Float): Boolean =
        yaw.isFinite() && pitch.isFinite() && roll.isFinite() &&
            abs(yaw) > 18f && abs(yaw) <= 60f && abs(pitch) <= 25f && abs(roll) <= 25f

    const val CLOSED_SCORE_THRESHOLD = 0.5f

    /** Normalized blink/gap evidence. Uncertain geometry remains unavailable. */
    fun openness(blink: Float, gapRatio: Float, width: Float): Float? {
        classify(blink, gapRatio, width) ?: return null
        val gap = gapRatio.coerceAtLeast(0.000001f)
        val narrowLidEvidence = minOf(blink / 0.45f, 0.11f / gap)
        val strongBlinkEvidence = minOf(blink / 0.65f, 0.16f / gap)
        val closedEvidence = maxOf(narrowLidEvidence, strongBlinkEvidence)
        return 1f / (1f + closedEvidence)
    }

    /** A narrow eyelid gap supports closure even when the side-view blink score is moderate. */
    fun classify(blink: Float, gapRatio: Float, width: Float): Boolean? {
        if (!blink.isFinite() || !gapRatio.isFinite() || !width.isFinite() ||
            blink !in 0f..1f || width < 10f || gapRatio < 0f) return null
        return when {
            blink >= 0.45f && gapRatio <= 0.11f -> false
            blink >= 0.65f && gapRatio <= 0.16f -> false
            blink <= 0.25f && gapRatio in 0.12f..0.65f -> true
            else -> null
        }
    }
}
