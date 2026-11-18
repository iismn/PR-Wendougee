package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import app.geeflow.data.user.model.HomeDockAction
import app.geeflow.presentation.feature.device.dashboard.components.BrewButtonState
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.ui.theme.GeeFlowThemePreview
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class HomeDockTest {
    @Test
    fun draggingPushesNeighborsBeforeReleaseAndCommitsOnlyTheNewOrder() = runComposeUiTest {
        var state by mutableStateOf(DeviceDashboardViewState())
        val events = mutableListOf<DeviceDashboardEvent>()
        var chartClicks = 0
        setContent { GeeFlowThemePreview(false) { Box(Modifier.size(500.dp, 100.dp).testTag("dock-test-root")) {
            HomeDock(state, ProfileListViewState(), { event ->
                events += event
                if (event is DeviceDashboardEvent.DockOrderChanged) state = state.copy(dockOrder = event.order)
            }, {}, { chartClicks++ })
        } } }
        val root = onNodeWithTag("dock-test-root")
        fun center(tag: String) = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center - root.fetchSemanticsNode().boundsInRoot.topLeft
        val start = center("home-dock-charts")
        val destination = center("home-dock-flush")
        val historyBefore = center("home-dock-history").x
        root.performTouchInput { down(start); advanceEventTime(700); moveTo(destination, 400) }
        waitForIdle()
        assertTrue(center("home-dock-history").x < historyBefore, "Neighbors must slide aside before the finger is released")
        assertTrue(events.isEmpty(), "Order is committed only on release, with no machine command during drag")
        root.performTouchInput { up() }
        waitForIdle()
        val saved = events.filterIsInstance<DeviceDashboardEvent.DockOrderChanged>().single().order
        assertEquals(listOf(HomeDockAction.Profiles, HomeDockAction.History, HomeDockAction.FreeControl,
            HomeDockAction.Flush, HomeDockAction.Charts, HomeDockAction.Scale, HomeDockAction.Brew),
            saved.filterNot { it == HomeDockAction.HotWater })
        assertEquals(HomeDockAction.HotWater, saved[5], "An unavailable hot-water action must retain its saved slot")
        assertTrue(events.all { it is DeviceDashboardEvent.DockOrderChanged })
        assertEquals(0, chartClicks)
        onNodeWithTag("home-dock-charts").performClick()
        assertEquals(1, chartClicks, "A normal tap must still execute its action after reordering")
    }

    @Test
    fun disabledFlushAndEnabledBrewCanBeReorderedWithoutRunningEitherAction() = runComposeUiTest {
        var state by mutableStateOf(DeviceDashboardViewState(brewButtonState = BrewButtonState.Busy))
        val events = mutableListOf<DeviceDashboardEvent>()
        setContent { GeeFlowThemePreview(false) { Box(Modifier.size(500.dp, 100.dp).testTag("dock-test-root")) {
            HomeDock(state, ProfileListViewState(), { event ->
                events += event
                if (event is DeviceDashboardEvent.DockOrderChanged) state = state.copy(dockOrder = event.order)
            }, {}, {})
        } } }
        val root = onNodeWithTag("dock-test-root")
        fun center(tag: String) = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center - root.fetchSemanticsNode().boundsInRoot.topLeft
        onNodeWithTag("home-dock-flush").assertIsNotEnabled()
        var start = center("home-dock-flush")
        var destination = center("home-dock-charts")
        root.performTouchInput { down(start); advanceEventTime(700); moveTo(destination, 400); up() }
        waitForIdle()
        assertEquals(HomeDockAction.Flush, state.dockOrder.filterNot { it == HomeDockAction.HotWater }[1])
        runOnIdle { state = state.copy(brewButtonState = BrewButtonState.Idle) }
        start = center("home-dock-brew")
        destination = center("home-dock-profiles")
        root.performTouchInput { down(start); advanceEventTime(700); moveTo(destination, 400); up() }
        waitForIdle()
        assertEquals(HomeDockAction.Brew, state.dockOrder.first())
        assertEquals(2, events.size)
        assertTrue(events.all { it is DeviceDashboardEvent.DockOrderChanged }, "Holding and releasing Flush or Brew must never activate the pump")
    }

    @Test
    fun quickSwipesAndStationaryLongHoldsNeverRunPumpActions() = runComposeUiTest {
        val events = mutableListOf<DeviceDashboardEvent>()
        setContent { GeeFlowThemePreview(false) { Box(Modifier.size(500.dp, 100.dp).testTag("dock-test-root")) {
            HomeDock(DeviceDashboardViewState(), ProfileListViewState(), events::add, {}, {})
        } } }
        val root = onNodeWithTag("dock-test-root")
        for (action in listOf("flush", "brew")) {
            val start = onNodeWithTag("home-dock-$action").fetchSemanticsNode().boundsInRoot.center - root.fetchSemanticsNode().boundsInRoot.topLeft
            root.performTouchInput { down(start); moveTo(start + Offset(-90f, 0f), 180); up() }
            assertTrue(events.isEmpty(), "A quick swipe over $action must not execute its tap")
            onNodeWithTag("home-dock-$action").performTouchInput { longClick() }
            onNodeWithTag("home-dock-tooltip").assertIsDisplayed()
            assertTrue(events.isEmpty(), "A stationary long hold on $action must only show its name")
        }
    }

    @Test
    fun tappingACrossingSlotCannotInvokeTheBrewActionMovingIntoIt() = runComposeUiTest {
        mainClock.autoAdvance = false
        var state by mutableStateOf(DeviceDashboardViewState())
        val events = mutableListOf<DeviceDashboardEvent>()
        var chartClicks = 0
        setContent { GeeFlowThemePreview(false) { Box(Modifier.size(500.dp, 100.dp).testTag("dock-test-root")) {
            HomeDock(state, ProfileListViewState(), events::add, {}, { chartClicks++ })
        } } }
        waitForIdle()
        val root = onNodeWithTag("dock-test-root")
        val oldProfiles = onNodeWithTag("home-dock-profiles").fetchSemanticsNode().boundsInRoot.center - root.fetchSemanticsNode().boundsInRoot.topLeft
        runOnIdle { state = state.copy(dockOrder = HomeDockAction.defaultOrder.reversed()) }
        mainClock.advanceTimeBy(32)
        waitForIdle()
        // Brew's final slot is now at the left, while the previous icon is still animating away.
        root.performTouchInput { click(oldProfiles) }
        assertTrue(events.isEmpty(), "A tap during slot motion must never invoke the incoming Brew button")
        assertEquals(0, chartClicks)
        mainClock.advanceTimeBy(600)
        waitForIdle()
        onNodeWithTag("home-dock-charts").performTouchInput { click() }
        assertEquals(1, chartClicks, "Normal taps must work once every icon has settled")
        assertTrue(events.isEmpty())
    }

    @Test
    fun cancellingADragRestoresSlotsAndNeverPersistsOrExecutesAnAction() = runComposeUiTest {
        var state by mutableStateOf(DeviceDashboardViewState())
        val events = mutableListOf<DeviceDashboardEvent>()
        setContent { GeeFlowThemePreview(false) { Box(Modifier.size(500.dp, 100.dp).testTag("dock-test-root")) {
            HomeDock(state, ProfileListViewState(), events::add, {}, {})
        } } }
        val root = onNodeWithTag("dock-test-root")
        fun center(tag: String) = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center - root.fetchSemanticsNode().boundsInRoot.topLeft
        val start = center("home-dock-free-control")
        val destination = center("home-dock-profiles")
        root.performTouchInput { down(start); advanceEventTime(700); moveTo(destination, 400) }
        waitForIdle()
        assertTrue(center("home-dock-free-control").x < start.x, "The gesture must be active before cancellation")
        // Skiko's touch-injection cancel() is a no-op. Changing machine capabilities replaces
        // the pointer-input key and exercises real cancellation of the held gesture instead.
        runOnIdle { state = state.copy(hotWaterSupported = true) }
        waitForIdle()
        root.performTouchInput { up() }
        waitForIdle()
        assertEquals(start.x, center("home-dock-free-control").x, 1f)
        assertTrue(events.isEmpty())
    }
}
