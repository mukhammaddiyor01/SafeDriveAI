package uz.safedrive.safedrive_ai.model

import kotlin.math.abs

internal object SideEye {
    fun supports(yaw: Float, pitch: Float, roll: Float): Boolean =
        yaw.isFinite() && pitch.isFinite() && roll.isFinite() &&
            abs(yaw) > 18f && abs(yaw) <= 60f && abs(pitch) <= 25f && abs(roll) <= 25f

    /** Both eyelid shape and the model's blink score must agree. */
    fun classify(blink: Float, gapRatio: Float, width: Float): Boolean? {
        if (!blink.isFinite() || !gapRatio.isFinite() || !width.isFinite() ||
            blink !in 0f..1f || width < 10f || gapRatio < 0f) return null
        return when {
            blink >= 0.65f && gapRatio <= 0.16f -> false
            blink <= 0.25f && gapRatio in 0.12f..0.65f -> true
            else -> null
        }
    }
}
