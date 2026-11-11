package uz.safedrive.safedrive_ai.model

import org.junit.Assert.*
import org.junit.Test

class SideEyeTest {
    @Test fun rejectsContradictoryAndMissingEyeEvidence() {
        assertEquals(true, SideEye.classify(0.1f, 0.22f, 24f))
        assertEquals(false, SideEye.classify(0.85f, 0.08f, 24f))
        assertNull(SideEye.classify(0.85f, 0.3f, 24f))
        assertNull(SideEye.classify(0.1f, 0.05f, 24f))
        assertNull(SideEye.classify(Float.NaN, 0.22f, 24f))
        assertNull(SideEye.classify(0.1f, 0.22f, 8f))
    }

    @Test fun supportsBothSidesAndRejectsExtremeOrInvalidPoses() {
        for (yaw in listOf(-60f, -44f, -30f, 30f, 44f, 60f)) {
            assertTrue(SideEye.supports(yaw, 0f, 0f))
        }
        assertFalse(SideEye.supports(61f, 0f, 0f))
        assertFalse(SideEye.supports(-61f, 0f, 0f))
        assertFalse(SideEye.supports(44f, 30f, 0f))
        assertFalse(SideEye.supports(Float.NaN, 0f, 0f))
    }
}
