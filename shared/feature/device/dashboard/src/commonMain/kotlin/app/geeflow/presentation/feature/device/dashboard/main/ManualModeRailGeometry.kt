package app.geeflow.presentation.feature.device.dashboard.main

import kotlin.math.PI
import kotlin.math.atan2

/** One circular rail, with a half-length thumb expanded by five percent of the rail width. */
internal data class ManualModeRailGeometry(val innerRadius: Float, val outerRadius: Float) {
    val trackStart = -40f
    val trackSweep = 80f
    val halfSweep = trackSweep / 2f
    val pressureAngle = trackStart + halfSweep / 2f
    val flowAngle = trackStart + trackSweep - halfSweep / 2f
    val midpointRadius = innerRadius + (outerRadius - innerRadius) / 2f
    val selectedOffset = (outerRadius - innerRadius) * .05f
    val selectedInner = innerRadius - selectedOffset
    val selectedOuter = outerRadius + selectedOffset
    val endpointAngleOffset = selectedOffset / midpointRadius * 180f / PI.toFloat()
    val selectionSweep = halfSweep + 2f * endpointAngleOffset

    init {
        require(innerRadius.isFinite() && outerRadius.isFinite() && innerRadius > 0f && outerRadius > innerRadius)
        require(selectedInner > 0f && selectedOuter.isFinite())
    }

    fun clampAngle(angle: Float): Float = angle.coerceIn(pressureAngle, flowAngle)

    fun selectionStart(angle: Float): Float = clampAngle(angle) - halfSweep / 2f - endpointAngleOffset

    fun pointerAngleAtPoint(x: Float, y: Float): Float = atan2(y, x) * 180f / PI.toFloat()

    fun angleAtPoint(x: Float, y: Float): Float = clampAngle(pointerAngleAtPoint(x, y))

    /** Retain the point where the thumb was grabbed, including touches near either end. */
    fun dragAngle(startAngle: Float, startPointerAngle: Float, currentPointerAngle: Float): Float {
        val delta = ((currentPointerAngle - startPointerAngle + 540f) % 360f) - 180f
        return clampAngle(startAngle + delta)
    }
}
