package uz.safedrive.safedrive_ai.model

import org.junit.Assert.*
import org.junit.Test

class SideEyeCalibrationTest {
    private fun reference(model: SideEyeCalibration, yaw: Float) {
        repeat(5) { model.openness(yaw, 0.15f, 0.24f, 20f) }
    }

    @Test fun moderateBlinkAndRelativeLidClosureAreKnownOnBothSides() {
        for (yaw in listOf(-45f, -42f, 42f, 45f)) {
            val model = SideEyeCalibration()
            reference(model, yaw)
            assertEquals(1f, model.openness(yaw, 0.15f, 0.24f, 20f)!!, 0.001f)
            assertTrue(model.openness(yaw, 0.42f, 0.125f, 20f)!! < 0.5f)
            assertTrue(model.openness(yaw, 0.43f, 0.11f, 20f)!! < 0.5f)
        }
    }

    @Test fun closedEvidenceCannotBecomeAnOpenReference() {
        val model = SideEyeCalibration()
        repeat(100) { model.openness(-42f, 0.42f, 0.11f, 20f) }
        assertNull(model.openness(-42f, 0.42f, 0.11f, 20f))
        reference(model, -42f)
        assertTrue(model.openness(-42f, 0.42f, 0.11f, 20f)!! < 0.5f)
        model.reset()
        assertNull(model.openness(-42f, 0.42f, 0.11f, 20f))
    }

    @Test fun sidesAnglesAndMissingEvidenceStaySeparate() {
        val model = SideEyeCalibration()
        reference(model, -42f)
        assertNull(model.openness(42f, 0.42f, 0.11f, 20f))
        assertNull(model.openness(-25f, 0.42f, 0.11f, 20f))
        assertNull(model.openness(-42f, 0.8f, 0.3f, 20f))
        assertNull(model.openness(-42f, 0.1f, 0.02f, 20f))
        assertNull(model.openness(-42f, Float.NaN, 0.11f, 20f))
        assertNull(model.openness(-42f, 0.42f, 0.11f, 8f))
    }

    @Test fun calibratedScoreFeedsSustainedClosureAlarmAt45Degrees() {
        for (yaw in listOf(-45f, 45f)) {
            val model = SideEyeCalibration()
            reference(model, yaw)
            val engine = DrowsinessEngine()
            var state = MonitorState()
            for (time in 0L..1500L step 100) {
                state = engine.update(Observation(time, 1, yaw = yaw,
                    sideEyeOpenness = model.openness(yaw, 0.42f, 0.125f, 20f)))
            }
            assertEquals("drowsy", state.status)
            assertTrue(state.alarm)
            assertTrue(state.eyesOpen!! < 0.5)
        }
    }
}
