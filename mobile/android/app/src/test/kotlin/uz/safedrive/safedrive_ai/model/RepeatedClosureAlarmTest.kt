package uz.safedrive.safedrive_ai.model

import org.junit.Assert.*
import org.junit.Test

class RepeatedClosureAlarmTest {
    private fun arm(policy: RepeatedClosureAlarm) {
        assertFalse(policy.update(3_000, 3_000, 0))
        assertFalse(policy.update(3_500, 0, 400))
        assertTrue(policy.update(7_000, 3_000, 0))
    }

    @Test fun holdsForFifteenSecondsThenRequiresOpenEyes() {
        val policy = RepeatedClosureAlarm()
        arm(policy)
        assertTrue(policy.update(21_999, 0, 10_000))
        assertEquals(1L, policy.remainingMs)
        assertTrue(policy.update(22_000, 12_000, 0))
        assertTrue(policy.update(23_000, 0, 900))
        assertFalse(policy.update(23_100, 0, 1_000))
    }

    @Test fun unknownOrOneLongClosureCannotCountTwice() {
        val policy = RepeatedClosureAlarm()
        assertFalse(policy.update(3_000, 3_000, 0))
        assertFalse(policy.update(9_000, 9_000, 0))
        assertFalse(policy.update(10_000, 0, 0))
        assertFalse(policy.update(14_000, 3_000, 0))
        assertFalse(policy.update(15_000, 0, 400))
        assertTrue(policy.update(18_000, 3_000, 0))
    }

    @Test fun expiredEpisodesAndResetDoNotCarryIntoNextSession() {
        val policy = RepeatedClosureAlarm()
        policy.update(3_000, 3_000, 0)
        policy.update(4_000, 0, 500)
        assertFalse(policy.update(64_000, 3_000, 0))
        policy.reset()
        arm(policy)
        policy.reset()
        assertFalse(policy.update(8_000, 0, 2_000))
    }
}
