package uz.safedrive.safedrive_ai.model

/** Escalate two separate prolonged closures in one minute to a 15-second alarm. */
internal class RepeatedClosureAlarm {
    private val episodes = ArrayDeque<Long>()
    private var counted = false
    private var holdUntil: Long? = null
    var remainingMs: Long = 0
        private set

    fun reset() {
        episodes.clear()
        counted = false
        holdUntil = null
        remainingMs = 0
    }

    fun update(now: Long, closedForMs: Long, openForMs: Long): Boolean {
        val deadline = holdUntil
        if (deadline != null) {
            remainingMs = (deadline - now).coerceAtLeast(0)
            if (now < deadline || openForMs < 1_000) return true
            reset()
            return false
        }
        while (episodes.isNotEmpty() && now - episodes.first() > 60_000) episodes.removeFirst()
        // Unknown frames do not turn one continuous closure into two episodes.
        if (openForMs >= 300) counted = false
        if (closedForMs >= 3_000 && !counted) {
            counted = true
            episodes.addLast(now)
            if (episodes.size >= 2) {
                holdUntil = now + 15_000
                remainingMs = 15_000
                episodes.clear()
                return true
            }
        }
        remainingMs = 0
        return false
    }
}
