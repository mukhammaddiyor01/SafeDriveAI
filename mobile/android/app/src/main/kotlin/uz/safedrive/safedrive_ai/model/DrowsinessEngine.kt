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
    private var openSince: Long? = null
    private var perclosRecovered = false
    private val repeatedClosure = RepeatedClosureAlarm()

    fun reset(clearEscalation: Boolean = true) {
        history.clear()
        previous = null
        closedSince = null
        poseSince = null
        unknownSince = null
        identity = null
        openSince = null
        perclosRecovered = false
        if (clearEscalation) repeatedClosure.reset()
    }

    private fun eyeOpenness(o: Observation): Float? {
        if (o.faceCount != 1 || !o.yaw.isFinite() || !o.pitch.isFinite() || !o.roll.isFinite()) return null
        if (SideEye.supports(o.yaw, o.pitch, o.roll)) {
            if (o.sideEyeOpenness != null) {
                return o.sideEyeOpenness.takeIf { it.isFinite() && it in 0f..1f }
            }
            return o.sideEyeOpen?.let { if (it) 1f else 0f }
        }
        if (abs(o.yaw) > 18 || abs(o.pitch) > 30 || abs(o.roll) > 25) return null
        val left = o.leftEyeOpen ?: return null
        val right = o.rightEyeOpen ?: return null
        if (left !in 0f..1f || right !in 0f..1f) return null
        return minOf(left, right)
    }

    private fun eyesClosed(o: Observation): Boolean =
        if (abs(o.yaw) > 18f) (eyeOpenness(o) ?: 1f) <= SideEye.CLOSED_SCORE_THRESHOLD
        else maxOf(o.leftEyeOpen ?: 1f, o.rightEyeOpen ?: 1f) < 0.35f

    fun update(o: Observation): MonitorState {
        val now = o.timeMs
        val last = previous
        if (last != null && now <= last.timeMs) {
            return MonitorState("limited", "Camera frames stopped updating.", active = true)
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
                openSince = null
            } else {
                eyeOpenness(prior)?.let { history.addLast(Interval(prior.timeMs, now, eyesClosed(prior))) }
            }
        }
        previous = o
        val openness = eyeOpenness(o)
        val eyeKnown = openness != null
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
        val poseUnusual = o.faceCount == 1 && (abs(o.pitch) > 25 || (!eyeKnown && abs(o.yaw) > 30))
        poseSince = if (poseUnusual) poseSince ?: now else null
        val poseDuration = poseSince?.let { now - it } ?: 0L
        closedSince = if (openness != null && eyesClosed(o)) closedSince ?: now else null
        val duration = closedSince?.let { now - it } ?: 0L
        val consistentlyOpen = openness != null && openness > 0.5f && !eyesClosed(o)
        openSince = if (consistentlyOpen) openSince ?: now else null
        val openDuration = openSince?.let { now - it } ?: 0L
        val elevatedPerclos = perclos != null && perclos >= 0.35
        if (!elevatedPerclos) perclosRecovered = false
        if (elevatedPerclos && openDuration >= 1_000) perclosRecovered = true
        val heldAlarm = repeatedClosure.update(now, duration, openDuration)
        val base = MonitorState(
            active = true,
            eyesOpen = openness?.toDouble(),
            sideEye = eyeKnown && abs(o.yaw) > 18f,
            closedMs = duration,
            yaw = if (o.faceCount == 1) o.yaw.toDouble() else null,
            pitch = if (o.faceCount == 1) o.pitch.toDouble() else null,
            perclos = perclos,
        )
        return when {
            heldAlarm -> {
                val seconds = (repeatedClosure.remainingMs + 999) / 1_000
                val message = if (seconds > 0)
                    "Repeated eye closure. Alarm: ${seconds}s. Pull over safely."
                else "Keep your eyes open. Pull over safely and rest."
                base.copy(status = "drowsy", message = message, alarm = true)
            }
            eyeKnown && (duration >= 1_500 || (elevatedPerclos && !perclosRecovered)) ->
                base.copy(status = "drowsy", message = "Drowsiness detected. Pull over safely.", alarm = true)
            poseDuration >= 2_500 ->
                base.copy(status = "warning", message = "Face angle is outside the monitoring range. Adjust the phone mount while parked.", alarm = true)
            !eyeKnown -> {
                val reason = when {
                    o.faceCount == 0 -> "Face not visible. Check the camera angle and lighting."
                    o.faceCount > 1 -> "Multiple faces in frame. Aim the camera at the driver."
                    abs(o.yaw) > 18 -> "Side angle limits eye tracking. Adjust the phone mount while parked."
                    else -> "Eyes not clearly visible. Adjust the camera angle."
                }
                base.copy(status = "limited", message = reason,
                    alarm = visibilityTimedOut)
            }
            duration >= 700 -> base.copy(status = "warning", message = "Eyes closed")
            elevatedPerclos -> base.copy(status = "monitoring",
                message = "Eyes open. Recent drowsiness detected; pull over safely and rest.")
            else -> base.copy(status = "monitoring", message = "Monitoring active")
        }
    }
}
