package app.geeflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class RulerSliderTest {
    @Test
    fun rulerDragAccumulatesSmallMovementsAndUpdatesContinuouslyWithoutScrolling() = runComposeUiTest {
        var value by mutableIntStateOf(50)
        val changes = mutableListOf<Int>()
        lateinit var scroll: androidx.compose.foundation.ScrollState
        setContent { MaterialTheme {
            scroll = rememberScrollState()
            Column(Modifier.size(360.dp, 200.dp).verticalScroll(scroll)) {
                RulerSlider(value, 0..100, "Seconds", { value = it; changes += it }, Modifier.fillMaxWidth().testTag("ruler"))
                Spacer(Modifier.height(800.dp))
            }
        } }
        onNodeWithTag("ruler").performTouchInput { down(center); moveBy(Offset(-32f, 0f), 100) }
        waitForIdle()
        runOnIdle { assertTrue(value > 50, "The number must change before lifting the finger") }
        onNodeWithTag("ruler").performTouchInput {
            repeat(40) { moveBy(Offset(-1f, 0f), 16) }
            up()
        }
        runOnIdle {
            assertTrue(value >= 57, "Fractional movements should accumulate, got $value")
            assertTrue(changes.size >= 3)
            assertTrue(changes.zipWithNext().all { (a,b) -> a <= b })
            assertEquals(0, scroll.value)
        }
        val before = value
        onNodeWithTag("ruler").performTouchInput { swipe(Offset(40f, center.y), Offset(160f, center.y)) }
        runOnIdle { assertTrue(value < before, "Moving the ruler right must select a lower value") }
    }

    @Test
    fun touchDownDoesNotJumpAndBothEndsStayInsideTheMachineRange() = runComposeUiTest {
        var value by mutableIntStateOf(5)
        setContent { MaterialTheme {
            RulerSlider(value, 1..10, "Cycles", { value = it }, Modifier.width(300.dp).testTag("ruler"))
        } }
        onNodeWithTag("ruler").performTouchInput { click(Offset(10f, center.y)) }
        runOnIdle { assertEquals(5,value) }
        onNodeWithTag("ruler").performTouchInput { swipe(Offset(280f, center.y), Offset(10f, center.y)) }
        runOnIdle { assertEquals(10,value) }
        onNodeWithTag("ruler").performTouchInput { swipe(Offset(10f, center.y), Offset(280f, center.y)) }
        runOnIdle { assertEquals(1,value) }
        onNodeWithTag("ruler").performSemanticsAction(SemanticsActions.SetProgress) { it(100f) }
        runOnIdle { assertEquals(10,value) }
        onNodeWithTag("ruler").performSemanticsAction(SemanticsActions.SetProgress) { it(Float.NaN) }
        runOnIdle { assertEquals(10,value) }
    }

    @Test
    fun machineLimitsCanArriveAfterCompositionAndDisabledInputStaysInactive() = runComposeUiTest {
        var value by mutableIntStateOf(5)
        var range by mutableStateOf(IntRange.EMPTY)
        var enabled by mutableStateOf(true)
        setContent { MaterialTheme {
            RulerSlider(value, range, "Seconds", { value = it }, Modifier.width(300.dp).testTag("ruler"), enabled)
        } }
        onNodeWithTag("ruler").assertIsNotEnabled()
        runOnIdle { range = 1..60 }
        onNodeWithTag("ruler").assertIsEnabled().performTouchInput { swipe(Offset(240f, center.y), Offset(60f, center.y)) }
        runOnIdle { assertTrue(value > 5); enabled = false }
        val before = value
        onNodeWithTag("ruler").assertIsNotEnabled().performTouchInput { swipe(Offset(240f, center.y), Offset(60f, center.y)) }
        onNodeWithTag("ruler").performSemanticsAction(SemanticsActions.SetProgress) { it(60f) }
        runOnIdle { assertEquals(before,value) }
    }

    @Test
    fun ticksContinuePastTheLimitAndTouchIlluminatesThenFadesWithoutChangingValue() = runComposeUiTest {
        mainClock.autoAdvance = false
        var value by mutableIntStateOf(1)
        setContent { MaterialTheme(colorScheme = darkColorScheme(onSurface = Color.White, primary = Color.Cyan)) {
            RulerSlider(value, 1..10, "Cycles", { value = it }, Modifier.width(300.dp).background(Color.Black).testTag("ruler"))
        } }
        waitForIdle()
        val node = onNodeWithTag("ruler")
        fun brightness(relativeX: Float): Float {
            val pixels = node.captureToImage().toPixelMap()
            val color = pixels[(pixels.width * relativeX).toInt(), (pixels.height * .625f).toInt()]
            return maxOf(color.red, color.green, color.blue)
        }
        val outside = brightness(130f / 300f) // tick 0, beyond the lower bound
        val allowed = brightness(170f / 300f) // tick 2, within the allowed range
        val gap = brightness(160f / 300f)
        assertTrue(outside > .03f, "The ruler must continue beyond its limit")
        assertTrue(allowed > outside, "Selectable ticks must be brighter than the continuation")
        node.performTouchInput { down(center) }
        mainClock.advanceTimeBy(160)
        waitForIdle()
        assertTrue(brightness(170f / 300f) > allowed + .08f, "Contact must visibly illuminate the ticks")
        assertEquals(gap, brightness(160f / 300f), .01f, "Only the tick strokes may glow; their surrounding background stays unchanged")
        node.performTouchInput { up() }
        mainClock.advanceTimeBy(600)
        waitForIdle()
        assertEquals(allowed, brightness(170f / 300f), .02f)
        runOnIdle { assertEquals(1, value, "Touch alone must not move the selected value") }
    }
}
