package uz.safedrive.safedrive_ai.model

import org.junit.Assert.*
import org.junit.Test

class DrowsinessEngineTest {
    private fun sample(time: Long, open: Float = 0.9f, yaw: Float = 0f, count: Int = 1, id: Int = 1) =
        Observation(time, count, open, open, yaw, trackingId = id)

    @Test fun normalBlinkDoesNotAlarm() {
        val engine = DrowsinessEngine()
        engine.update(sample(0))
        engine.update(sample(100, 0.1f))
        engine.update(sample(300, 0.1f))
        assertFalse(engine.update(sample(400)).alarm)
    }

    @Test fun sustainedClosureAlarmsAfterOneAndHalfSeconds() {
        val engine = DrowsinessEngine()
        var state = MonitorState()
        for (t in 0L..1500L step 100) state = engine.update(sample(t, 0.1f))
        assertEquals("drowsy", state.status)
        assertTrue(state.alarm)
    }

    @Test fun profileIsUnknownEvenWhenEyeProbabilitiesExist() {
        val engine = DrowsinessEngine()
        for (t in 0L..1600L step 100) {
            val state = engine.update(sample(t, 0.05f, yaw = 45f))
            assertEquals("limited", state.status)
            assertNull(state.eyesOpen)
        }
    }

    @Test fun inferenceGapDoesNotCountAsClosedEyes() {
        val engine = DrowsinessEngine()
        engine.update(sample(0, 0.1f))
        engine.update(sample(500, 0.1f))
        val state = engine.update(sample(5000, 0.1f))
        assertEquals(0L, state.closedMs)
        assertFalse(state.alarm)
    }

    @Test fun lostFaceNeverReportsMonitoring() {
        val engine = DrowsinessEngine()
        engine.update(sample(0))
        var state = MonitorState()
        for (t in 100L..3200L step 100) state = engine.update(sample(t, count = 0))
        assertEquals("limited", state.status)
        assertTrue(state.alarm)
    }

    @Test fun switchingDriversResetsClosureHistory() {
        val engine = DrowsinessEngine()
        for (t in 0L..1400L step 100) engine.update(sample(t, 0.1f))
        val state = engine.update(sample(1500, 0.1f, id = 2))
        assertFalse(state.alarm)
        assertEquals(0L, state.closedMs)
    }

    @Test fun prolongedClosuresTriggerRollingPerclos() {
        val engine = DrowsinessEngine()
        var state = MonitorState()
        for (t in 0L..12_000L step 100) {
            state = engine.update(sample(t, if (t % 2000 < 1000) 0.1f else 0.9f))
        }
        assertEquals("drowsy", state.status)
        assertTrue(state.perclos!! >= 0.35)
    }

    @Test fun rollingPerclosResetsWhenEyesReturnAfterVisibilityTimeout() {
        val engine = DrowsinessEngine()
        for (t in 0L..12_000L step 100) {
            engine.update(sample(t, if (t % 2000 < 1000) 0.1f else 0.9f))
        }
        assertTrue(engine.update(sample(12_100)).alarm)

        for (t in 12_200L..15_100L step 100) {
            val state = engine.update(sample(t, count = 0))
            assertEquals("limited", state.status)
            assertFalse(state.alarm)
        }

        val recovered = engine.update(sample(15_200))
        assertEquals("monitoring", recovered.status)
        assertFalse(recovered.alarm)
        assertNull(recovered.perclos)
    }

    @Test fun multipleFacesNeverProvideEyeEvidence() {
        val engine = DrowsinessEngine()
        val state = engine.update(sample(0, count = 2))
        assertEquals("limited", state.status)
        assertNull(state.eyesOpen)
    }
}
