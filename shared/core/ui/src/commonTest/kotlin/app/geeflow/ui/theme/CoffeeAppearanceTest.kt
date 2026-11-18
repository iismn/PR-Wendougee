package app.geeflow.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import app.geeflow.domain.ClockThemeSchedule
import app.geeflow.domain.ClockThemeStage
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoffeeAppearanceTest {
    @Test
    fun editedStagesApplyAtTheirOwnTimesAndContinueAcrossMidnight() {
        val schedule = requireNotNull(ClockThemeSchedule.Default.withTime(ClockThemeStage.Morning, 9 * 60))
        val lateNight = requireNotNull(schedule.withTime(ClockThemeStage.Blackout, 30))
        fun at(minute: Int) = coffeeClockAppearanceAt(minute.toFloat(), lateNight)
        assertEquals(coffeeClockAppearanceAt(8f * 60).ambient, at(9 * 60).ambient)
        assertTrue(at(8 * 60).ambient.base.luminance() < at(9 * 60).ambient.base.luminance())
        assertTrue(at(23 * 60).ambient.glow.alpha > at(0).ambient.glow.alpha)
        assertTrue(at(0).ambient.glow.alpha > 0f)
        assertEquals(Color.Black, at(30).ambient.base)
        assertEquals(Color.Black, at(4 * 60 + 30).ambient.base)
        assertEquals(lateNight, ClockThemeSchedule.decode(lateNight.encode()))
        assertEquals(ClockThemeSchedule.Default, ClockThemeSchedule.decode("not a schedule"))
        assertEquals(null, schedule.withTime(ClockThemeStage.Morning, 15 * 60))
    }

    @Test
    fun clockBackgroundFadesFromBlackIntoGreenThenThroughSunsetToBlack() {
        fun at(hour: Int, minute: Int = 0) = coffeeClockAppearanceAt((hour * 60 + minute).toFloat())
        val dawn = at(4, 30).ambient
        val morning = at(8).ambient
        val sunset = at(15, 30).ambient
        val evening = at(18, 30).ambient

        assertEquals(CoffeeAmbientColors(Color.Black, Color.Transparent, Color.Transparent), dawn)
        assertTrue(at(6).ambient.base.luminance() in 0f..morning.base.luminance())
        assertTrue(at(6).ambient.base.luminance() > dawn.base.luminance())
        assertTrue(morning.glow.green > morning.glow.red)
        assertTrue(sunset.glow.red > sunset.glow.green)
        assertEquals(coffeeAmbientColors(CoffeeDayPhase.Evening), evening)
        assertFalse(at(15, 30).phase.dark)
        assertTrue(at(18, 30).phase.dark)

        val levels = listOf(sunset, at(17).ambient, evening, at(20).ambient, at(21).ambient, at(22).ambient)
        val brightness = levels.map { it.glow.compositeOver(it.base).luminance() }
        assertTrue(brightness.zipWithNext().all { (before, after) -> before > after }, brightness.toString())
        listOf(at(22), at(22, 30), at(23, 59), at(0), at(3)).forEach {
            assertEquals(dawn, it.ambient)
            assertTrue(it.phase.dark)
        }
    }

    @Test
    fun adjacentClockSamplesHaveNoAbruptBackgroundChange() {
        val colors = (0 until 24 * 60 * 2).map { coffeeClockAppearanceAt(it / 2f).ambient }
        for ((before, after) in (colors + colors.first()).zipWithNext()) {
            val first = listOf(before.base, before.glow, before.cool)
            val second = listOf(after.base, after.glow, after.cool)
            for ((a, b) in first.zip(second)) {
                // Compare the visible color over black, so an invisible RGB value cannot count as a jump.
                fun visibleArgb(color: Color) = color.copy(
                    red = color.red * color.alpha, green = color.green * color.alpha,
                    blue = color.blue * color.alpha, alpha = 1f,
                ).toArgb()
                val from = visibleArgb(a)
                val to = visibleArgb(b)
                for (shift in listOf(16, 8, 0)) {
                    assertTrue(abs(((from shr shift) and 255) - ((to shr shift) and 255)) <= 3)
                }
            }
        }
    }

    @Test
    fun textStaysReadableAsSurfacesDim() {
        CoffeeDayPhase.entries.forEach { phase ->
            val scheme = coffeeColorScheme(phase)
            listOf(scheme.onSurface, scheme.onSurfaceVariant).forEach { text ->
                val a = text.luminance()
                val b = scheme.surface.luminance()
                assertTrue((maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f) >= 4.5f, "$phase text contrast")
            }
        }
        assertTrue(coffeeColorScheme(CoffeeDayPhase.Night).background.luminance() < coffeeColorScheme(CoffeeDayPhase.Evening).background.luminance())
    }
}
