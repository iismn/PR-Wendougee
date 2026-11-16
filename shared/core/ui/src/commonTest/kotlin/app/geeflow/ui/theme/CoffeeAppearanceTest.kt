package app.geeflow.ui.theme

import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoffeeAppearanceTest {
    @Test
    fun changesAtLocalTimeBoundaries() {
        val expected = mapOf(
            0 to CoffeeDayPhase.Night, 5 to CoffeeDayPhase.Night,
            6 to CoffeeDayPhase.Morning, 11 to CoffeeDayPhase.Morning,
            12 to CoffeeDayPhase.Day, 17 to CoffeeDayPhase.Day,
            18 to CoffeeDayPhase.Evening, 22 to CoffeeDayPhase.Evening, 23 to CoffeeDayPhase.Night,
        )
        expected.forEach { (hour, phase) -> assertEquals(phase, CoffeeDayPhase.atHour(hour)) }
        assertFalse(CoffeeDayPhase.Day.dark)
        assertTrue(CoffeeDayPhase.Evening.dark)
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
