package uz.safedrive.safedrive_ai.model

import org.junit.Assert.*
import org.junit.Test

class DrowsinessEngineTest {
    private fun sample(time: Long, open: Float = 0.9f, yaw: Float = 0f, count: Int = 1, id: Int = 1) =
        Observation(time, count, open, open, yaw, trackingId = id)

    @Test fun sideEyeDisplayScoreDoesNotSuppressClosedEyeAlarmAt45Degrees() {
        for (yaw in listOf(-45f, 45f)) {
            val engine = DrowsinessEngine()
            var state = MonitorState()
            for (t in 0L..1500L step 100) {
                state = engine.update(Observation(t, 1, yaw = yaw,
                    sideEyeOpen = false, sideEyeOpenness = 0.48f))
            }
            assertEquals(0.48, state.eyesOpen!!, 0.001)
            assertEquals("drowsy", state.status)
            assertTrue(state.alarm)
        }
    }

    @Test fun sideEyeScoreProvidesTheDecisionWithoutLegacyBoolean() {
        val state = DrowsinessEngine().update(Observation(0, 1, yaw = 45f,
            sideEyeOpenness = 0.95f))
        assertEquals(0.95, state.eyesOpen!!, 0.001)
        assertEquals("monitoring", state.status)
    }

    @Test fun displayedSideScoreControlsClosureOnBothSides() {
        for (yaw in listOf(-45f, 45f)) {
            for (score in listOf(0.2f, 0.5f, 0.8f)) {
                val engine = DrowsinessEngine()
                var state = MonitorState()
                for (t in 0L..1500L step 100) {
                    state = engine.update(Observation(t, 1, yaw = yaw,
                        sideEyeOpen = score <= 0.5f, sideEyeOpenness = score))
                }
                assertEquals(score <= 0.5f, state.alarm)
                assertEquals(score.toDouble(), state.eyesOpen!!, 0.001)
            }
        }
    }

    @Test fun stableReopeningSilencesOldPerclosWithoutErasingItsHistory() {
        val engine = DrowsinessEngine()
        var state = MonitorState()
        for (t in 0L..12_000L step 100) {
            state = engine.update(sample(t, if (t % 2000 < 1000) 0.1f else 0.9f))
        }
        assertTrue(state.alarm)
        for (t in 12_100L..13_000L step 100) {
            assertTrue(engine.update(sample(t)).alarm)
        }
        state = engine.update(sample(13_100))
        assertFalse(state.alarm)
        assertEquals("monitoring", state.status)
        assertTrue(state.perclos!! >= 0.35)
        assertFalse(engine.update(sample(13_200, 0.1f)).alarm)
        assertFalse(engine.update(sample(13_300)).alarm)
        for (t in 13_400L..14_900L step 100) state = engine.update(sample(t, 0.1f))
        assertTrue(state.alarm)
        assertEquals("drowsy", state.status)
    }

    @Test fun repeatedSideClosuresHoldAlarmEvenAfterEyesReopen() {
        for (yaw in listOf(-45f, 45f)) {
            val engine = DrowsinessEngine()
            var time = 0L
            for (step in 0..30) {
                engine.update(Observation(time, 1, yaw = yaw, sideEyeOpenness = 0.15f))
                time += 100
            }
            for (step in 0..15) {
                engine.update(Observation(time, 1, yaw = yaw, sideEyeOpenness = 1f))
                time += 100
            }
            var state = MonitorState()
            for (step in 0..30) {
                state = engine.update(Observation(time, 1, yaw = yaw, sideEyeOpenness = 0.15f))
                time += 100
            }
            assertTrue(state.message.contains("15s"))
            for (step in 0..148) {
                state = engine.update(Observation(time, 1, yaw = yaw, sideEyeOpenness = 1f))
                assertTrue(state.alarm)
                time += 100
            }
            state = engine.update(Observation(time, 1, yaw = yaw, sideEyeOpenness = 1f))
            assertFalse(state.alarm)
            assertEquals("monitoring", state.status)
        }
    }

    @Test fun analysisResetPreservesHeldAlarmWhileSessionResetClearsIt() {
        val engine = DrowsinessEngine()
        for (time in 0L..3000L step 100) {
            engine.update(Observation(time, 1, yaw = 45f, sideEyeOpenness = 0.1f))
        }
        for (time in 3100L..4600L step 100) {
            engine.update(Observation(time, 1, yaw = 45f, sideEyeOpenness = 1f))
        }
        for (time in 4700L..7700L step 100) {
            engine.update(Observation(time, 1, yaw = 45f, sideEyeOpenness = 0.1f))
        }
        engine.reset(clearEscalation = false)
        assertTrue(engine.update(Observation(8000, 1, yaw = 45f, sideEyeOpenness = 1f)).alarm)
        engine.reset()
        assertFalse(engine.update(Observation(8100, 1, yaw = 45f, sideEyeOpenness = 1f)).alarm)
    }

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

    @Test fun visibleEyeAt38DegreesKeepsMonitoringOnBothSides() {
        for (yaw in listOf(-60f, -44f, -38f, 38f, 44f, 60f)) {
            val engine = DrowsinessEngine()
            for (t in 0L..3500L step 100) {
                val state = engine.update(Observation(t, 1, yaw = yaw, sideEyeOpen = true))
                assertEquals("monitoring", state.status)
                assertTrue(state.sideEye)
                assertFalse(state.alarm)
            }
        }
    }

    @Test fun sustainedSideEyeClosureAlarmsAndUncertaintyResetsTimer() {
        val engine = DrowsinessEngine()
        for (t in 0L..1400L step 100) {
            engine.update(Observation(t, 1, yaw = 38f, sideEyeOpen = false))
        }
        assertNull(engine.update(Observation(1500, 1, yaw = 38f)).eyesOpen)
        for (t in 1600L..3000L step 100) {
            assertFalse(engine.update(Observation(t, 1, yaw = 38f, sideEyeOpen = false)).alarm)
        }
        assertEquals("drowsy", engine.update(Observation(3100, 1, yaw = 38f, sideEyeOpen = false)).status)
    }

    @Test fun frontalWinkDoesNotCountAsBothEyesClosed() {
        val engine = DrowsinessEngine()
        for (t in 0L..2000L step 100) {
            assertFalse(engine.update(Observation(t, 1, 0.05f, 0.9f)).alarm)
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
    @Test fun moderateBlinkWithNarrowLidsTriggersSideClosureAlarm() {
        for (yaw in listOf(-42f, 43f)) {
            val engine = DrowsinessEngine()
            for (time in 0L..1000L step 100) {
                val state = engine.update(Observation(time, 1, yaw = yaw,
                    sideEyeOpen = SideEye.classify(0.1f, 0.25f, 24f)))
                assertFalse(state.alarm)
            }
            var state = MonitorState()
            for (time in 1100L..2600L step 100) {
                state = engine.update(Observation(time, 1, yaw = yaw,
                    sideEyeOpen = SideEye.classify(0.52f, 0.093f, 24f)))
            }
            assertEquals("drowsy", state.status)
            assertTrue(state.alarm)
            assertEquals(1500L, state.closedMs)
        }
    }

}
