package app.geeflow.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import app.geeflow.ui.animations.BackwardTransition
import app.geeflow.ui.animations.ForwardTransition
import app.geeflow.ui.animations.predictiveBackTransition
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NavigationSwipeBackTest {
    @Test
    fun pageFollowsTheDragAndReversesWhenTheGestureIsCancelled() = runComposeUiTest {
        mainClock.autoAdvance = false
        val pages = mutableStateListOf("home", "settings")
        setContent {
            NavigationEdgeSwipeBack(
                enabled = pages.size > 1,
                onBack = { pages.removeLast() },
                modifier = Modifier.fillMaxSize().testTag("host"),
            ) {
                NavDisplay(
                    backStack = pages,
                    onBack = { pages.removeLast() },
                    transitionSpec = { ForwardTransition },
                    popTransitionSpec = { BackwardTransition },
                    predictivePopTransitionSpec = { edge ->
                        predictiveBackTransition(edge == NavigationEvent.EDGE_RIGHT)
                    },
                    entryProvider = entryProvider {
                        entry<String> { page -> Box(Modifier.fillMaxSize().testTag(page)) }
                    },
                )
            }
        }
        waitForIdle()
        val original = onNodeWithTag("settings").getUnclippedBoundsInRoot()
        val host = onNodeWithTag("host")
        fun horizontalTravel() =
            (onNodeWithTag("settings").getUnclippedBoundsInRoot().left - original.left).value * density.density

        host.performTouchInput { down(Offset(5f, 150f)); moveTo(Offset(55f, 150f)) }
        mainClock.advanceTimeBy(96)
        waitForIdle()
        assertTrue(abs(horizontalTravel() - 50f) < 4f, "50px drag travelled ${horizontalTravel()}px")
        assertEquals(2, pages.size)

        host.performTouchInput { moveTo(Offset(30f, 150f)) }
        mainClock.advanceTimeBy(96)
        waitForIdle()
        assertTrue(abs(horizontalTravel() - 25f) < 4f, "Reverse drag travelled ${horizontalTravel()}px")
        host.performTouchInput { up() }
        mainClock.advanceTimeBy(600)
        waitForIdle()
        assertEquals(original, onNodeWithTag("settings").getUnclippedBoundsInRoot())
        assertEquals(2, pages.size)

        host.performTouchInput { down(Offset(5f, 150f)); moveTo(Offset(42f, 150f)) }
        mainClock.advanceTimeBy(64)
        waitForIdle()
        assertTrue(horizontalTravel() > 16f, "The next gesture must also move the page")
        host.performTouchInput { moveTo(Offset(180f, 150f)) }
        mainClock.advanceTimeBy(96)
        waitForIdle()
        val releasePosition = horizontalTravel()
        assertTrue(releasePosition in 155f..180f, "Second drag travelled ${releasePosition}px")
        host.performTouchInput { up() }
        mainClock.advanceTimeBy(48)
        waitForIdle()
        assertTrue(horizontalTravel() >= releasePosition - 2f, "The page should keep moving out after release. It travelled ${horizontalTravel()}px")
        mainClock.advanceTimeBy(600)
        waitForIdle()
        assertEquals(listOf("home"), pages.toList())
        onNodeWithTag("home").assertExists()
        onNodeWithTag("settings").assertDoesNotExist()
    }
}
