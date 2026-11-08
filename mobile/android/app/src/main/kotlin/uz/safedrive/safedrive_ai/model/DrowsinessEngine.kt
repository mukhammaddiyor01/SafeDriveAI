package uz.safedrive.safedrive_ai.model

import kotlin.math.abs

private const val VISIBILITY_TIMEOUT_MS = 3_000L

/** Pure temporal logic. Time comes from a monotonic clock, never the wall clock. */
class DrowsinessEngine {
    private data class Interval(val start: Long, val end: Long, val closed: Boolean)
    private val history = ArrayDeque<Interval>()
    private var previous: Observation? = null
    private var closedSince: Long? = null
    private var poseSince: Long? = null
    private var unknownSince: Long? = null
    private var identity: Int? = null

    fun reset() {
        history.clear()
        previous = null
        closedSince = null
        poseSince = null
        unknownSince = null
        identity = null
    }

    private fun validEyes(o: Observation): Boolean =
        o.faceCount == 1 && abs(o.yaw) <= 18 && abs(o.pitch) <= 30 && abs(o.roll) <= 25 &&
            o.leftEyeOpen?.let { it.isFinite() && it in 0f..1f } == true &&
            o.rightEyeOpen?.let { it.isFinite() && it in 0f..1f } == true

    private fun closed(o: Observation): Boolean =
        (o.leftEyeOpen ?: 1f) < 0.35f && (o.rightEyeOpen ?: 1f) < 0.35f

    fun update(o: Observation): MonitorState {
        val now = o.timeMs
        val last = previous
        if (last != null && now <= last.timeMs) {
            return MonitorState("limited", "Kamera vaqti yangilanmadi", active = true)
        }
        if (o.faceCount == 1 && o.trackingId != null && identity != null && o.trackingId != identity) {
            reset()
        }
        if (o.faceCount == 1 && o.trackingId != null) identity = o.trackingId
        val prior = previous
        if (prior != null) {
            val gap = now - prior.timeMs
            if (gap > 700) {
                // No evidence exists inside a long inference/camera gap.
                history.clear()
                closedSince = null
                poseSince = null
                unknownSince = null
            } else if (validEyes(prior)) {
                history.addLast(Interval(prior.timeMs, now, closed(prior)))
            }
        }
        previous = o
        val eyeKnown = validEyes(o)
        val visibilityLostAt = unknownSince ?: now
        val visibilityTimedOut = now - visibilityLostAt >= VISIBILITY_TIMEOUT_MS
        if (visibilityTimedOut) history.clear()
        unknownSince = if (eyeKnown) null else visibilityLostAt
        while (history.isNotEmpty() && history.first().end <= now - 30_000) history.removeFirst()
        val cutoff = now - 30_000
        var knownMs = 0L
        var closedMs = 0L
        for (interval in history) {
            val duration = interval.end - maxOf(interval.start, cutoff)
            knownMs += duration
            if (interval.closed) closedMs += duration
        }
        val perclos = if (knownMs >= 10_000) closedMs.toDouble() / knownMs else null
        val poseUnusual = o.faceCount == 1 && (abs(o.pitch) > 25 || abs(o.yaw) > 30)
        poseSince = if (poseUnusual) poseSince ?: now else null
        val poseDuration = poseSince?.let { now - it } ?: 0L
        closedSince = if (eyeKnown && closed(o)) closedSince ?: now else null
        val duration = closedSince?.let { now - it } ?: 0L
        val base = MonitorState(
            active = true,
            eyesOpen = if (eyeKnown) minOf(o.leftEyeOpen!!, o.rightEyeOpen!!).toDouble() else null,
            closedMs = duration,
            yaw = if (o.faceCount == 1) o.yaw.toDouble() else null,
            pitch = if (o.faceCount == 1) o.pitch.toDouble() else null,
            perclos = perclos,
        )
        return when {
            eyeKnown && (duration >= 1_500 || (perclos != null && perclos >= 0.35)) ->
                base.copy(status = "drowsy", message = "Uyquchanlik alomati. Xavfsiz joyda to‘xtang!", alarm = true)
            poseDuration >= 2_500 ->
                base.copy(status = "warning", message = "Bosh holati o‘zgardi. Ko‘zlarni baholab bo‘lmayapti.", alarm = true)
            !eyeKnown -> {
                val reason = when {
                    o.faceCount == 0 -> "Yuz ko‘rinmayapti. Kamera va yorug‘likni tekshiring."
                    o.faceCount > 1 -> "Kadrda bir nechta yuz bor. Kamerani haydovchiga qarating."
                    else -> "Ko‘zlar aniq ko‘rinmayapti. Kamera burchagini moslang."
                }
                base.copy(status = "limited", message = reason,
                    alarm = visibilityTimedOut)
            }
            duration >= 700 -> base.copy(status = "warning", message = "Ko‘zlar yumilgan")
            else -> base.copy(status = "monitoring", message = "Kuzatuv faol")
        }
    }
}
