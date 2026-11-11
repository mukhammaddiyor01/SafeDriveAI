package uz.safedrive.safedrive_ai.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryTriggerTest {
    @Test
    fun repeatedClosureHoldFinishesBeforeAssistantRecoveryFires() {
        val engine = DrowsinessEngine()
        val trigger = RecoveryTrigger()
        val fired = mutableListOf<Long>()
        for (time in 0L..25_000L step 100) {
            val closed = time <= 3_000 || time in 4_700L..7_700L
            val state = engine.update(Observation(time, 1, yaw = 45f,
                sideEyeOpenness = if (closed) 0.1f else 1f))
            if (trigger.update(state, time)) fired.add(time)
            if (time in 7_700L..22_600L) assertTrue(state.alarm)
        }
        assertTrue(fired.count { it >= 7_700 } == 1)
        assertTrue(fired.last() >= 23_700)
    }

    @Test
    fun firesOnceOnlyAfterFreshOpenEyeEvidence() {
        val trigger = RecoveryTrigger()
        val open = MonitorState(status = "monitoring", active = true, eyesOpen = 0.9)
        assertFalse(trigger.update(open, 0))
        assertFalse(trigger.update(open.copy(status = "drowsy", alarm = true), 100))
        assertFalse(trigger.update(open, 200))
        assertFalse(trigger.update(open.copy(eyesOpen = null), 700))
        assertFalse(trigger.update(open, 800))
        assertFalse(trigger.update(open, 1300))
        assertTrue(trigger.update(open, 1800))
        assertFalse(trigger.update(open, 2300))
    }
}
