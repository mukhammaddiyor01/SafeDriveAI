package uz.safedrive.safedrive_ai.model

/** Arm on drowsiness, then fire once after a second of fresh, open-eye evidence. */
class RecoveryTrigger {
    private var lastDrowsy: Long? = null
    private var openSince: Long? = null
    private var lastOpen: Long? = null

    fun update(state: MonitorState, now: Long): Boolean {
        if (state.active && state.status == "drowsy" && state.alarm) lastDrowsy = now
        val armedAt = lastDrowsy
        if (!state.active || armedAt == null || now - armedAt > 15_000) {
            lastDrowsy = null
            openSince = null
            lastOpen = null
            return false
        }
        if (!state.recoveryEvidence) {
            openSince = null
            lastOpen = null
            return false
        }
        val previous = lastOpen
        if (previous == null || now - previous > 700) openSince = now
        lastOpen = now
        if (now - (openSince ?: now) < 1_000) return false
        lastDrowsy = null
        openSince = null
        lastOpen = null
        return true
    }
}

val MonitorState.recoveryEvidence: Boolean
    get() = active && status == "monitoring" && !alarm && (eyesOpen ?: 0.0) > SideEye.CLOSED_SCORE_THRESHOLD
