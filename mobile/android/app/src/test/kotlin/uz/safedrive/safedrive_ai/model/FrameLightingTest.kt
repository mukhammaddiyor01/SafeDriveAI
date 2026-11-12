package uz.safedrive.safedrive_ai.model

import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class FrameLightingTest {
    @Test fun separatesDimDetailFromBlackAndNoise() {
        assertEquals(Lighting.LOW, LuminanceStats(35, 10, 75).lighting)
        assertEquals(Lighting.NORMAL, LuminanceStats(100, 20, 210).lighting)
        assertEquals(Lighting.INSUFFICIENT, LuminanceStats(3, 0, 8).lighting)
        assertEquals(Lighting.INSUFFICIENT, LuminanceStats(20, 18, 22).lighting)
        assertEquals(Lighting.INSUFFICIENT, LuminanceStats.fromHistogram(IntArray(256)).lighting)
    }
    @Test fun computesPercentiles() {
        val histogram = IntArray(256)
        for (i in 0..99) histogram[i] = 1
        val stats = LuminanceStats.fromHistogram(histogram)
        assertEquals(49, stats.median)
        assertEquals(9, stats.p10)
        assertEquals(89, stats.p90)
    }
    @Test fun yPlaneRespectsPaddingPixelStrideAndBufferPosition() {
        val bytes = ByteArray(4 * 24 + 3) { 255.toByte() }
        for (y in 0..3) for (x in 0..7) bytes[3 + y * 24 + x * 2] = (30 + x * 5).toByte()
        val padded = ByteBuffer.wrap(bytes).apply { position(3) }
        val tight = ByteBuffer.wrap(ByteArray(32) { (30 + (it % 8) * 5).toByte() })
        assertEquals(LuminanceStats.fromYPlane(tight, 8, 4, 8, 1),
            LuminanceStats.fromYPlane(padded, 8, 4, 24, 2))
        assertEquals(3, padded.position())
    }
    @Test fun hysteresisNeedsFiveClearlyBrightFrames() {
        val policy = LowLightPolicy()
        assertFalse(policy.shouldEnhance(LuminanceStats(120, 30, 220)))
        assertTrue(policy.shouldEnhance(LuminanceStats(30, 10, 60)))
        repeat(10) { assertTrue(policy.shouldEnhance(LuminanceStats(70, 20, 100))) }
        repeat(4) { assertTrue(policy.shouldEnhance(LuminanceStats(120, 30, 220))) }
        assertFalse(policy.shouldEnhance(LuminanceStats(120, 30, 220)))
        assertFalse(policy.shouldEnhance(LuminanceStats(1, 0, 3)))
    }
    @Test fun missingLowLightEyesAreNotMisreportedAsAnInvalidMountAngle() {
        val engine = DrowsinessEngine()
        for (time in 0L..3200L step 100) {
            val state = engine.update(Observation(time, 1, yaw = 45f, lighting = Lighting.LOW))
            assertEquals("limited", state.status)
            assertTrue(state.message.startsWith("Low light"))
        }
    }
    @Test fun blackFramesBreakTheConsecutiveBrightRecoverySequence() {
        val policy = LowLightPolicy()
        policy.shouldEnhance(LuminanceStats(30, 10, 60))
        repeat(4) { policy.shouldEnhance(LuminanceStats(120, 30, 220)) }
        assertFalse(policy.shouldEnhance(LuminanceStats(0, 0, 0)))
        repeat(4) { assertTrue(policy.shouldEnhance(LuminanceStats(120, 30, 220))) }
        assertFalse(policy.shouldEnhance(LuminanceStats(120, 30, 220)))
    }
    @Test fun darknessCannotClearAHeldAlarmOrStartTheAssistant() {
        val engine = DrowsinessEngine()
        val recovery = RecoveryTrigger()
        for (time in 0L..7700L step 100) {
            val closed = time <= 3000 || time >= 4700
            val state = engine.update(Observation(time, 1, yaw = 45f,
                sideEyeOpenness = if (closed) 0.1f else 1f))
            recovery.update(state, time)
        }
        for (time in 7800L..24000L step 100) {
            val state = engine.update(Observation(time, 1, yaw = 45f,
                sideEyeOpenness = 1f, lighting = Lighting.INSUFFICIENT))
            assertTrue(state.alarm)
            assertNull(state.eyesOpen)
            assertFalse(recovery.update(state, time))
        }
    }
    @Test fun boundedToneIsMonotonicAndPreservesBlack() {
        val lut = LowLightTone.lookup
        assertEquals(0, lut[0])
        assertEquals(255, lut[255])
        for (value in 1..255) {
            assertTrue(lut[value] >= lut[value - 1])
            assertTrue(lut[value] >= value)
            assertTrue(lut[value] <= minOf(value * 3, 255))
        }
        assertEquals(0xff000000.toInt(), LowLightTone.correct(0xff000000.toInt()))
        assertEquals(0x7f000000, LowLightTone.correct(0x7f000000))
    }
    @Test fun darknessIsUnknownNotClosedAndCannotStartAssistant() {
        val engine = DrowsinessEngine()
        for (time in 0L..3200L step 100) {
            val state = engine.update(Observation(time, 1, yaw = 45f,
                sideEyeOpenness = 0.1f, lighting = Lighting.INSUFFICIENT))
            assertNull(state.eyesOpen)
            assertEquals(0L, state.closedMs)
            assertFalse(state.recoveryEvidence)
            assertEquals("limited", state.status)
            assertTrue(state.message.startsWith("Too dark"))
            assertEquals(time >= 3000, state.alarm)
        }
    }
}
