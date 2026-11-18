package app.geeflow.presentation.feature.device.dashboard.main

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ManualModeRailGeometryTest {
    private val geometry = ManualModeRailGeometry(300f, 374f)

    @Test
    fun thumbIsHalfTheRailWithTheSameFivePercentMarginOnEveryEdge() {
        assertEquals(geometry.trackSweep / 2f, geometry.halfSweep)
        assertEquals(3.7f, geometry.selectedOffset, .0001f)
        assertEquals(geometry.selectedOffset, geometry.innerRadius - geometry.selectedInner, .0001f)
        assertEquals(geometry.selectedOffset, geometry.selectedOuter - geometry.outerRadius, .0001f)
        val endMarginLength = geometry.endpointAngleOffset * PI.toFloat() / 180f * geometry.midpointRadius
        assertEquals(geometry.selectedOffset, endMarginLength, .0001f)
        assertEquals(geometry.halfSweep, geometry.selectionSweep - 2f * geometry.endpointAngleOffset, .0001f)
    }

    @Test
    fun bothStopsAndIntermediatePositionsShareTheRailCircle() {
        assertEquals(-20f, geometry.pressureAngle)
        assertEquals(20f, geometry.flowAngle)
        assertEquals(geometry.midpointRadius, (geometry.selectedInner + geometry.selectedOuter) / 2f, .0001f)
        for (angle in listOf(-20f, -8f, 0f, 13f, 20f)) {
            assertEquals(angle, geometry.selectionStart(angle) + geometry.selectionSweep / 2f, .0001f)
            val radians = angle * PI.toFloat() / 180f
            assertEquals(angle, geometry.angleAtPoint(cos(radians) * geometry.midpointRadius,
                sin(radians) * geometry.midpointRadius), .0001f)
        }
    }

    @Test
    fun grabbingTheThumbNearItsEdgeDoesNotJumpAndDraggingStaysWithinBothStops() {
        assertEquals(-20f, geometry.dragAngle(-20f, -37f, -37f))
        assertEquals(-13f, geometry.dragAngle(-20f, -37f, -30f))
        assertEquals(0f, geometry.dragAngle(-20f, -37f, -17f))
        assertEquals(20f, geometry.dragAngle(-20f, -37f, 80f))
        assertEquals(13f, geometry.dragAngle(20f, 37f, 30f))
        assertEquals(-20f, geometry.dragAngle(20f, 37f, -80f))
        assertEquals(-18f, geometry.dragAngle(-20f, 179f, -179f), .0001f)
        assertEquals(18f, geometry.dragAngle(20f, -179f, 179f), .0001f)
    }

    @Test
    fun invalidRadiiCannotCreateANonFiniteOrInvertedThumb() {
        assertFailsWith<IllegalArgumentException> { ManualModeRailGeometry(0f, 74f) }
        assertFailsWith<IllegalArgumentException> { ManualModeRailGeometry(300f, 300f) }
        assertFailsWith<IllegalArgumentException> { ManualModeRailGeometry(Float.NaN, 374f) }
        assertFailsWith<IllegalArgumentException> { ManualModeRailGeometry(300f, Float.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { ManualModeRailGeometry(1f, 100f) }
    }
}
