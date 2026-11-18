package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import app.geeflow.ui.theme.GeeFlowThemePreview
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class HomeManualDialTest {
    @Test
    fun theDecimalNumberStaysAtTheDialCenterWhenTheUnitAndDigitCountChange() = runComposeUiTest {
        var state by mutableStateOf(HomeManualState(ready = true, connected = true, flowSupported = true))
        setContent { GeeFlowThemePreview(false) {
            HomeManualDial(state, 0, Color.Gray, {}, Modifier.size(640.dp, 560.dp))
        } }
        fun assertCentered(value: String, unit: String) {
            val dial = onNodeWithTag("home-manual-dial").getUnclippedBoundsInRoot()
            val number = onNodeWithTag("home-manual-number")
                .assertContentDescriptionEquals(value).getUnclippedBoundsInRoot()
            val suffix = onNodeWithTag("home-manual-unit").assertTextEquals(unit).getUnclippedBoundsInRoot()
            val title = onNodeWithTag("home-manual-title").getUnclippedBoundsInRoot()
            assertEquals((dial.left.value + dial.right.value) / 2f,
                (number.left.value + number.right.value) / 2f, 1f, "The digits themselves must be horizontally centered")
            assertEquals((dial.top.value + dial.bottom.value) / 2f,
                (number.top.value + number.bottom.value) / 2f, 1f, "The digits themselves must be vertically centered")
            assertTrue(suffix.left >= number.right, "The unit must sit beside the centered number")
            assertTrue(title.bottom <= number.top, "The mode label must sit above the number")
        }
        assertCentered("0.0", "bar")
        runOnIdle { state = state.copy(mode = ManualPriority.Flow, flow = 12f) }
        assertCentered("12.0", "ml/s")
    }

    @Test
    fun clockwiseRaisesTheTargetAndCounterclockwiseLowersItWithoutStartingOrNavigating() = runComposeUiTest {
        var state by mutableStateOf(HomeManualState(ready = true, connected = true, flowSupported = true, pressure = 6f))
        val events = mutableListOf<DeviceDashboardEvent>()
        setContent { GeeFlowThemePreview(false) {
            HomeManualDial(state,0,Color.Gray,{ event ->
                events += event
                if (event is DeviceDashboardEvent.DialTargetChanged) state = state.copy(pressure = event.value)
            }, Modifier.size(640.dp,560.dp))
        } }
        val dial = onNodeWithTag("home-manual-dial")
        dial.performTouchInput { down(Offset(center.x,15f)); moveTo(Offset(width-15f,center.y),200); up() }
        val raised = state.pressure
        assertTrue(raised > 6f)
        dial.performTouchInput { down(Offset(width-15f,center.y)); moveTo(Offset(center.x,15f),200); up() }
        assertTrue(state.pressure < raised)
        assertTrue(events.all { it is DeviceDashboardEvent.DialTargetChanged })
    }

    @Test
    fun priorityLensMovesAndLocksToTwoModesAndCanBeDraggedBetweenThem() = runComposeUiTest {
        var state by mutableStateOf(HomeManualState(ready = true, connected = true, flowSupported = true))
        val events = mutableListOf<DeviceDashboardEvent>()
        setContent { GeeFlowThemePreview(false) {
            HomeManualDial(state,0,Color.Gray,{ event ->
                events += event
                if (event is DeviceDashboardEvent.DialModeChanged) state = state.copy(mode = event.mode)
            }, Modifier.size(640.dp,560.dp))
        } }
        onNodeWithTag("home-mode-pressure").assertIsSelected()
        onNodeWithTag("home-mode-pressure-icon", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("home-mode-flow-icon", useUnmergedTree = true).assertIsDisplayed()
        val top = onNodeWithTag("home-mode-lens").getUnclippedBoundsInRoot().top
        onNodeWithTag("home-mode-flow").performClick()
        onNodeWithTag("home-mode-flow").assertIsSelected()
        onNodeWithTag("home-mode-pressure-icon", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("home-mode-flow-icon", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(onNodeWithTag("home-mode-lens").getUnclippedBoundsInRoot().top > top)
        onNodeWithTag("home-mode-lens").performTouchInput {
            down(center); moveTo(Offset(center.x, -height.toFloat()),300); up()
        }
        onNodeWithTag("home-mode-pressure").assertIsSelected()
        assertTrue(events.all { it is DeviceDashboardEvent.DialModeChanged })
    }

    @Test
    fun anOffCenterGrabFollowsTheFingerBeforeReleaseWithoutJumpingOrCommittingTheMode() = runComposeUiTest {
        mainClock.autoAdvance = false
        var state by mutableStateOf(HomeManualState(mode = ManualPriority.Flow,
            ready = true, connected = true, flowSupported = true))
        val events = mutableListOf<DeviceDashboardEvent>()
        setContent { GeeFlowThemePreview(false) {
            HomeManualDial(state, 0, Color.Gray, { event ->
                events += event
                if (event is DeviceDashboardEvent.DialModeChanged) state = state.copy(mode = event.mode)
            }, Modifier.size(640.dp, 560.dp).testTag("test-manual-root"))
        } }
        waitForIdle()
        val root = onNodeWithTag("test-manual-root")
        val lens = onNodeWithTag("home-mode-lens")
        val rootBounds = root.fetchSemanticsNode().boundsInRoot
        val initial = lens.fetchSemanticsNode().boundsInRoot
        // Grab below the pane's centre: movement must be relative to this contact point.
        val contact = Offset(initial.center.x, initial.top + initial.height * .70f) - rootBounds.topLeft
        root.performTouchInput {
            down(contact)
            moveTo(contact + Offset(0f, -30f), 200)
        }
        mainClock.advanceTimeBy(48)
        waitForIdle()
        val moved = lens.fetchSemanticsNode().boundsInRoot
        assertTrue(moved.top < initial.top - 1f, "The pane must follow a small upward drag while the finger remains down")
        assertTrue(initial.top - moved.top < initial.height * .40f,
            "Grabbing away from the centre must not jump the pane to another position")
        onNodeWithTag("home-mode-flow").assertIsSelected()
        assertTrue(events.isEmpty(), "Dragging must remain a preview until the finger is released")

        root.performTouchInput { moveTo(contact + Offset(0f, -initial.height * 1.7f), 400) }
        mainClock.advanceTimeBy(48)
        waitForIdle()
        assertTrue(lens.fetchSemanticsNode().boundsInRoot.top < initial.top - initial.height * .5f,
            "The pane must reach the upper slot before release")
        onNodeWithTag("home-mode-flow").assertIsSelected()
        assertTrue(events.isEmpty(), "Even a full drag must not commit a mode before release")

        root.performTouchInput { up() }
        mainClock.advanceTimeBy(500)
        waitForIdle()
        onNodeWithTag("home-mode-pressure").assertIsSelected()
        assertEquals(listOf<DeviceDashboardEvent>(DeviceDashboardEvent.DialModeChanged(ManualPriority.Pressure)), events)
    }

    @Test
    fun holdingTheDialIlluminatesOnlyItsTicksThenFadesWithoutChangingTheTarget() = runComposeUiTest {
        mainClock.autoAdvance = false
        val events = mutableListOf<DeviceDashboardEvent>()
        setContent { MaterialTheme(colorScheme = darkColorScheme(onSurface = Color.White, primary = Color.Cyan)) {
            HomeManualDial(HomeManualState(ready = true, connected = true, flowSupported = true),
                0, Color.Gray, { events += it }, Modifier.size(640.dp, 560.dp))
        } }
        waitForIdle()
        val dial = onNodeWithTag("home-manual-dial")
        fun brightness(yDp: Float): Float {
            val pixels = dial.captureToImage().toPixelMap()
            val scale = pixels.width / dial.getUnclippedBoundsInRoot().width.value
            val x = pixels.width / 2
            val y = (yDp * scale).toInt()
            return (-1..1).flatMap { dx -> (-1..1).map { dy ->
                val color = pixels[x + dx, y + dy]
                (color.red + color.green + color.blue) / 3f
            } }.max()
        }
        val tickBefore = brightness(16f)
        val gapBefore = brightness(40f)
        dial.performTouchInput { down(Offset(center.x, 15f)) }
        mainClock.advanceTimeBy(180)
        waitForIdle()
        assertTrue(brightness(16f) > tickBefore + .03f, "A held dial must visibly illuminate its tick strokes")
        assertEquals(gapBefore, brightness(40f), .01f, "The background inside the ring must not turn into a glowing patch")
        dial.performTouchInput { up() }
        mainClock.advanceTimeBy(650)
        waitForIdle()
        assertEquals(tickBefore, brightness(16f), .02f, "The contact glow must fade after release")
        assertTrue(events.isEmpty(), "Touch without rotation must not change or start extraction")
    }
}
