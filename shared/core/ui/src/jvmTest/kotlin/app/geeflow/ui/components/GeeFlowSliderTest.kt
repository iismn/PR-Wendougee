package app.geeflow.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GeeFlowSliderTest {
    @Test
    fun draggingUsesTheVisibleRulerRangeAndTapOpensExactInput() = runComposeUiTest {
        var target = 0f
        var opened = false
        setContent {
            MaterialTheme {
                GeeFlowSlider(0f, { target = it }, 0f..12f, "bar",
                    modifier = Modifier.width(300.dp).height(64.dp).testTag("pressure"),
                    onClick = { opened = true },
                )
            }
        }
        onNodeWithTag("pressure").performTouchInput {
            swipe(Offset(width * 0.2f, center.y), Offset(width.toFloat(), center.y))
        }
        runOnIdle { assertEquals(12f, target); assertTrue(!opened) }
        onNodeWithTag("pressure").performClick()
        runOnIdle { assertTrue(opened) }
        onNodeWithTag("pressure").performSemanticsAction(SemanticsActions.SetProgress) { it(-5f) }
        runOnIdle { assertEquals(0f, target) }
    }

    @Test
    fun disabledControlDoesNotSendChanges() = runComposeUiTest {
        var calls = 0
        setContent {
            MaterialTheme {
                GeeFlowSlider(6f, { calls++ }, 0f..12f, "bar",
                    modifier = Modifier.width(300.dp).height(64.dp).testTag("pressure"), enabled = false,
                )
            }
        }
        onNodeWithTag("pressure").assertIsNotEnabled()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(9f) }
        runOnIdle { assertEquals(0, calls) }
    }
}
