package app.geeflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BoilerControlCardTest {
    @Test
    fun arcTouchAndAccessibilityRespectTheBoilerRange() = runComposeUiTest {
        var requested = 94f
        setContent { MaterialTheme {
            ArcTemperatureDial(94f, 80f..100f, "Brew", { requested = it },
                Modifier.size(360.dp, 144.dp).testTag("arc"))
        } }
        onNodeWithTag("arc").performTouchInput { click(Offset(25f, 100f)) }
        runOnIdle { assertTrue(requested in 80f..82f) }
        onNodeWithTag("arc").performTouchInput { click(Offset(width.toFloat() - 25f, 100f)) }
        runOnIdle { assertTrue(requested in 98f..100f) }
        onNodeWithTag("arc").performTouchInput { click(Offset(center.x, 20f)) }
        runOnIdle { assertEquals(90f, requested, 0.1f) }
        onNodeWithTag("arc").performSemanticsAction(SemanticsActions.SetProgress) { it(400f) }
        runOnIdle { assertEquals(100f, requested) }
        onNodeWithTag("arc").performSemanticsAction(SemanticsActions.SetProgress) { it(Float.NaN) }
        runOnIdle { assertEquals(100f, requested) }
    }

    @Test
    fun continuousArcDragUpdatesTheValueWithoutScrollingItsParent() = runComposeUiTest {
        val requests = mutableListOf<Float>()
        var selected by mutableFloatStateOf(94f)
        lateinit var scroll: androidx.compose.foundation.ScrollState
        setContent { MaterialTheme {
            scroll = rememberScrollState()
            Column(Modifier.size(360.dp, 300.dp).verticalScroll(scroll)) {
                ArcTemperatureDial(selected, 80f..100f, "Brew", {
                    selected = it
                    requests += it
                }, Modifier.size(360.dp, 144.dp).testTag("arc"))
                Spacer(Modifier.height(1000.dp))
            }
        } }
        onNodeWithTag("arc").performTouchInput {
            down(Offset(25f, 100f))
            moveTo(Offset(center.x, 25f), 100)
        }
        waitForIdle()
        onNodeWithTag("arc").performTouchInput {
            moveTo(Offset(width.toFloat() - 25f, 100f), 100)
            moveTo(Offset(width.toFloat() + 100f, 100f), 100)
            up()
        }
        runOnIdle {
            assertTrue(requests.size >= 2, "The value must update during the drag, not only on release")
            assertTrue(requests.first() < requests.last())
            assertTrue(requests.zipWithNext().all { (a, b) -> a <= b })
            assertTrue(selected in 98f..100f, "Selected $selected after $requests")
            assertTrue(requests.all { it in 80f..100f })
            assertEquals(0, scroll.value)
        }
    }

    @Test
    fun disabledBoilerDoesNotAcceptTemperatureChanges() = runComposeUiTest {
        var requests = 0
        setContent { MaterialTheme {
            ArcTemperatureDial(125f, 110f..140f, "Steam", { requests++ },
                Modifier.size(360.dp, 144.dp).testTag("arc"), enabled = false)
        } }
        onNodeWithTag("arc").assertIsNotEnabled().performTouchInput { click() }
        onNodeWithTag("arc").performSemanticsAction(SemanticsActions.SetProgress) { it(140f) }
        runOnIdle { assertEquals(0, requests) }
    }

    @Test
    fun targetCanBePreparedWithTheBoilerOffWithoutTurningItOn() = runComposeUiTest {
        var requested = "94"
        var powerChanges = 0
        setContent { MaterialTheme {
            BoilerControlCard("Brew", 24f, "94", (80..100).map(Int::toString),
                checked = false, onCheckedChanged = { powerChanges++ }, onValueChanged = { requested = it },
                modifier = Modifier.size(430.dp, 420.dp))
        } }
        onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertIsEnabled().performSemanticsAction(SemanticsActions.SetProgress) { it(96f) }
        runOnIdle {
            assertEquals("96", requested)
            assertEquals(0, powerChanges)
        }
    }

    @Test
    fun dialUsesMachineValuesThatArriveAfterThePopupOpens() = runComposeUiTest {
        var values by mutableStateOf(emptyList<String>())
        var requested = "93"
        setContent { MaterialTheme {
            BoilerControlCard("Brew", 24f, "93", values,
                checked = false, onCheckedChanged = {}, onValueChanged = { requested = it },
                modifier = Modifier.size(430.dp, 420.dp))
        } }
        waitForIdle()
        runOnIdle { values = (80..100).map(Int::toString) }
        onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertIsEnabled().performSemanticsAction(SemanticsActions.SetProgress) { it(96f) }
        runOnIdle { assertEquals("96", requested) }
    }

    @Test
    fun cardSnapsToMachineSuppliedValuesIncludingFahrenheit() = runComposeUiTest {
        val requests = mutableListOf<String>()
        setContent { MaterialTheme {
            BoilerControlCard("Brew", 200f, "200", listOf("194", "197", "200", "203"),
                checked = true, onCheckedChanged = {}, onValueChanged = { requests += it },
                modifier = Modifier.size(430.dp, 420.dp))
        } }
        val dial = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        dial.performSemanticsAction(SemanticsActions.SetProgress) { it(198.5f) }
        dial.performSemanticsAction(SemanticsActions.SetProgress) { it(400f) }
        runOnIdle {
            assertTrue(requests.all { it in listOf("194", "197", "200", "203") })
            assertEquals("203", requests.last())
        }
    }
    @Test
    fun temperatureTicksGlowOnContactAndReturnToTheirOriginalAppearance() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { MaterialTheme(colorScheme = darkColorScheme(onSurface = Color.White, primary = Color.Cyan)) {
            ArcTemperatureDial(94f, 80f..100f, "Brew", {},
                Modifier.size(360.dp, 144.dp).background(Color.Black).testTag("arc"))
        } }
        waitForIdle()
        val dial = onNodeWithTag("arc")
        fun light(): Double {
            val pixels = dial.captureToImage().toPixelMap()
            var sum = 0.0
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                val pixel = pixels[x,y]
                sum += pixel.green + pixel.blue
            }
            return sum
        }
        val resting = light()
        dial.performTouchInput { down(Offset(center.x, 25f)) }
        mainClock.advanceTimeBy(160)
        waitForIdle()
        assertTrue(light() > resting * 1.1, "Pressing the temperature ruler must light up its ticks")
        dial.performTouchInput { up() }
        mainClock.advanceTimeBy(600)
        waitForIdle()
        assertEquals(resting, light(), 1.0)
    }

}
