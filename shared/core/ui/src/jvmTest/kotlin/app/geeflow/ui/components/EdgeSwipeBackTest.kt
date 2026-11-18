package app.geeflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class EdgeSwipeBackTest {
    @Test
    fun onlyACompletedHorizontalDragFromTheLeftEdgeGoesBack() = runComposeUiTest {
        var backs = 0
        setContent {
            EdgeSwipeBack(true, { backs++ }, Modifier.size(600.dp, 400.dp).testTag("page")) {
                Box(Modifier.fillMaxSize())
            }
        }
        onNodeWithTag("page").performTouchInput { swipe(Offset(200f, 150f), Offset(450f, 150f)) }
        onNodeWithTag("page").performTouchInput { swipe(Offset(5f, 50f), Offset(10f, 300f)) }
        onNodeWithTag("page").performTouchInput { swipe(Offset(5f, 150f), Offset(25f, 150f)) }
        runOnIdle { assertEquals(0, backs) }
        onNodeWithTag("page").performTouchInput { swipe(Offset(5f, 150f), Offset(260f, 155f)) }
        runOnIdle { assertEquals(1, backs) }
    }

    @Test
    fun gestureDoesNotShiftTheWholeHostBeforeNavigationRuns() = runComposeUiTest {
        var backs = 0
        setContent {
            EdgeSwipeBack(true, { backs++ }, Modifier.size(600.dp, 400.dp).testTag("page")) {
                Box(Modifier.fillMaxSize().testTag("content"))
            }
        }
        val original = onNodeWithTag("content").fetchSemanticsNode().boundsInRoot
        onNodeWithTag("page").performTouchInput { down(Offset(5f,150f)); moveTo(Offset(70f,150f),150) }
        mainClock.advanceTimeBy(150)
        assertEquals(original, onNodeWithTag("content").fetchSemanticsNode().boundsInRoot)
        onNodeWithTag("page").performTouchInput { up() }
        runOnIdle { assertEquals(0,backs) }
    }
}
