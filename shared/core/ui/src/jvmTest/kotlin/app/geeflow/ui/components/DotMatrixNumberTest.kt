package app.geeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DotMatrixNumberTest {
    @Test
    fun decimalAndMinusOccupyTheirOwnColumnsWithoutChangingIntegerSizing() = runComposeUiTest {
        setContent {
            Column {
                DotMatrixNumber(12, Color.White, Modifier.height(70.dp).testTag("integer"))
                DotMatrixNumber("0.0", Color.White, Modifier.height(70.dp).testTag("decimal"))
                DotMatrixNumber("12.0", Color.White, Modifier.height(70.dp).testTag("two-digit-decimal"))
                DotMatrixNumber("-0.5", Color.White, Modifier.height(70.dp).testTag("negative"))
            }
        }
        onNodeWithTag("integer").assertContentDescriptionEquals("12")
        onNodeWithTag("decimal").assertContentDescriptionEquals("0.0")
        onNodeWithTag("negative").assertContentDescriptionEquals("-0.5")
        assertEquals(110.dp, onNodeWithTag("integer").getUnclippedBoundsInRoot().width)
        assertEquals(130.dp, onNodeWithTag("decimal").getUnclippedBoundsInRoot().width)
        assertEquals(190.dp, onNodeWithTag("two-digit-decimal").getUnclippedBoundsInRoot().width)
        assertEquals(170.dp, onNodeWithTag("negative").getUnclippedBoundsInRoot().width)
    }

    @Test
    fun changedDotsLightUpInSequenceAndSettleOnTheLatestNumber() = runComposeUiTest {
        mainClock.autoAdvance = false
        val value = mutableStateOf("1")
        setContent {
            Box(Modifier.background(Color.Black).testTag("display")) {
                DotMatrixNumber(value.value, Color.White, Modifier.height(70.dp))
            }
        }
        waitForIdle()
        val display = onNodeWithTag("display")
        fun levels(): Pair<Float, Float> {
            val pixels = display.captureToImage().toPixelMap()
            return pixels[pixels.width * 3 / 10, pixels.height / 14].red to
                pixels[pixels.width / 10, pixels.height * 11 / 14].red
        }

        val initial = levels()
        assertTrue(initial.first < .1f && initial.second < .1f)
        runOnIdle { value.value = "8" }
        mainClock.advanceTimeBy(96)
        waitForIdle()
        val mid = levels()
        assertTrue(mid.first in .1f..0.85f && mid.first > mid.second + .04f, "Mid-sweep levels: $mid")

        runOnIdle { value.value = "1" }
        mainClock.advanceTimeBy(80)
        waitForIdle()
        val interrupted = levels()
        assertTrue(interrupted.first > initial.first && interrupted.first <= mid.first + .08f, "Before retarget: $mid, after: $interrupted")
        mainClock.advanceTimeBy(250)
        waitForIdle()
        assertEquals(initial, levels())
    }
}
