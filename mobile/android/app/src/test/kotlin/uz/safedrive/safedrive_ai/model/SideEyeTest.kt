package uz.safedrive.safedrive_ai.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class SideEyeTest {
    private fun contour(width: Float, height: Float, rotation: Double = 0.0) =
        List(16) { index ->
            val theta = index * Math.PI / 8
            val x = width / 2 * cos(theta)
            val y = height / 2 * sin(theta)
            SideEye.Point(
                (x * cos(rotation) - y * sin(rotation)).toFloat(),
                (x * sin(rotation) + y * cos(rotation)).toFloat(),
            )
        }

    @Test fun distinguishesOpenClosedAndAmbiguousContours() {
        assertEquals(true, SideEye.isOpen(contour(24f, 6f, 0.3)))
        assertEquals(false, SideEye.isOpen(contour(24f, 2f)))
        assertNull(SideEye.isOpen(contour(24f, 3.6f)))
        assertNull(SideEye.isOpen(contour(8f, 2f)))
        assertNull(SideEye.isOpen(emptyList()))
    }
}
