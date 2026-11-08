package uz.safedrive.safedrive_ai.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryTriggerTest {
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
