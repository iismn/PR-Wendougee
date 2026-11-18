package app.geeflow.app.screensaver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import app.geeflow.ui.components.LocalShowScreensaver
import app.geeflow.ui.theme.GeeFlowThemePreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.TestTimeSource
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
class ScreensaverContainerTest {
    @Test
    fun dotClockCanBeOpenedManuallyAndDismissedWithoutClickingThePageBelow() = runComposeUiTest {
        var pageClicks = 0
        setContent { GeeFlowThemePreview(false) {
            ScreensaverContainer(enabled = false, timeoutMinutes = 1) {
                val show = LocalShowScreensaver.current!!
                Box(Modifier.fillMaxSize().clickable { pageClicks++ }) {
                    Button(onClick = show, modifier = Modifier.testTag("show")) { Text("Show clock") }
                }
            }
        } }
        onNodeWithTag("show").performClick()
        onNodeWithTag("dot-screensaver").assertIsDisplayed().performTouchInput { click() }
        onNodeWithTag("dot-screensaver").assertDoesNotExist()
        runOnIdle { assertEquals(0, pageClicks) }
    }

    @Test
    fun idleTimeoutShowsTheClockAndTouchStartsAFreshTimeout() = runComposeUiTest {
        val time = TestTimeSource()
        setContent { GeeFlowThemePreview(false) {
            ScreensaverContainer(enabled = true, timeoutMinutes = 1, timeSource = time) {
                Box(Modifier.fillMaxSize().testTag("page"))
            }
        } }
        runOnIdle { time += 2.minutes }
        mainClock.advanceTimeBy(1500)
        onNodeWithTag("dot-screensaver").assertIsDisplayed().performTouchInput { click() }
        onNodeWithTag("dot-screensaver").assertDoesNotExist()
        mainClock.advanceTimeBy(1500)
        onNodeWithTag("dot-screensaver").assertDoesNotExist()
    }
}
