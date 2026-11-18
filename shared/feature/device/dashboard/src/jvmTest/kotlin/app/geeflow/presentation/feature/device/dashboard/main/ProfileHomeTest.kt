package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import app.geeflow.presentation.feature.device.dashboard.components.BrewButtonState
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.ui.theme.GeeFlowThemePreview
import app.geeflow.ui.theme.LocalNativeCoffeeMaterials
import app.geeflow.ui.theme.NativeCoffeeMaterials
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ProfileHomeTest {
    @Test
    fun metricsUseEqualVerticalGapsWithSlightHorizontalInsets() = runComposeUiTest {
        setContent {
            GeeFlowThemePreview(false) {
                ProfileHome(DeviceDashboardViewState(), ProfileListViewState(), SnackbarHostState(), {}, {}, {})
            }
        }
        val metrics = onNodeWithTag("home-metrics").getUnclippedBoundsInRoot()
        val headerDock = onNodeWithTag("home-header-actions").getUnclippedBoundsInRoot()
        val brewSummary = onNodeWithTag("home-brew-summary").getUnclippedBoundsInRoot()
        val leftGauge = onNodeWithTag("home-flow-gauge").getUnclippedBoundsInRoot()

        val topGap = metrics.top - headerDock.bottom
        val bottomGap = brewSummary.top - metrics.bottom
        assertTrue(topGap > 0.dp)
        assertTrue(abs((topGap - bottomGap).value) <= 1f, "Top gap $topGap, bottom gap $bottomGap")
        assertTrue(abs((metrics.left - leftGauge.left - 12.dp).value) <= 1f)
        assertTrue(abs((brewSummary.right - metrics.right - 12.dp).value) <= 1f)
    }

    @Test
    fun listSelectionAndEditingUseExistingEventsWithoutStartingTheMachine() = runComposeUiTest {
        val profileEvents = mutableListOf<ProfileListEvent>()
        val deviceEvents = mutableListOf<DeviceDashboardEvent>()
        val profiles = ProfileListViewState(profiles = listOf(
            ProfileListViewState.Profile("a", "1", "Orbit A", "36g", true, selected = true),
            ProfileListViewState.Profile("b", "2", "Orbit B", "42g", true),
        ))
        setContent { GeeFlowThemePreview(false) {
            ProfileHome(DeviceDashboardViewState(), profiles, SnackbarHostState(), deviceEvents::add, profileEvents::add, {})
        } }
        onNodeWithTag("profile-row-b").performClick()
        onNodeWithTag("home-manual-dial").performTouchInput { click() }
        runOnIdle {
            assertEquals(listOf<ProfileListEvent>(ProfileListEvent.ProfileSelected("b")), profileEvents)
            assertTrue(deviceEvents.isEmpty())
        }
    }

    @Test
    fun emptyLibraryDoesNotInventASelectedProfile() = runComposeUiTest {
        val events = mutableListOf<ProfileListEvent>()
        setContent { GeeFlowThemePreview(false) {
            ProfileHome(DeviceDashboardViewState(), ProfileListViewState(), SnackbarHostState(), {}, events::add, {})
        } }
        onNodeWithTag("home-manual-dial").assertIsNotEnabled()
        runOnIdle { assertTrue(events.isEmpty()) }
    }

    @Test
    fun swipeLeftRevealsDeleteAndOnlyTheButtonOffersConfirmation() = runComposeUiTest {
        val events = mutableListOf<ProfileListEvent>()
        val profiles = ProfileListViewState(profiles = listOf(
            ProfileListViewState.Profile("a", "1", "Test recipe", "", false),
        ))
        setContent { GeeFlowThemePreview(false) {
            ProfileHome(DeviceDashboardViewState(), profiles, SnackbarHostState(), {}, events::add, {})
        } }
        onNodeWithTag("profile-row-a").performTouchInput { longClick() }
        onNodeWithTag("profile-delete-confirm").assertDoesNotExist()
        onNodeWithTag("profile-delete-a").assertDoesNotExist()
        onNodeWithTag("profile-edit-a").assertDoesNotExist()
        events.clear()
        onNodeWithTag("profile-row-a").performTouchInput { swipeLeft() }
        onNodeWithTag("profile-delete-a").assertIsDisplayed()
        onNodeWithTag("profile-delete-confirm").assertDoesNotExist()
        onNodeWithTag("profile-delete-a").performClick()
        onNodeWithTag("profile-delete-confirm").assertIsDisplayed()
        runOnIdle { assertTrue(events.isEmpty()) }
        onNodeWithTag("profile-delete-confirm").performClick()
        runOnIdle { assertEquals(listOf<ProfileListEvent>(ProfileListEvent.RemoveProfileClicked("a")), events) }
    }

    @Test
    fun boundMachineProfileCannotBeDeletedFromTheSwipeAction() = runComposeUiTest {
        val events = mutableListOf<ProfileListEvent>()
        val profiles = ProfileListViewState(profiles = listOf(
            ProfileListViewState.Profile("a", "1", "Bound", "", false, bound = true),
        ))
        setContent { GeeFlowThemePreview(false) {
            ProfileHome(DeviceDashboardViewState(), profiles, SnackbarHostState(), {}, events::add, {})
        } }
        onNodeWithTag("profile-row-a").performTouchInput { swipeLeft() }
        onNodeWithTag("profile-delete-a").performClick()
        onNodeWithTag("profile-delete-confirm").assertIsNotEnabled()
        runOnIdle { assertTrue(events.isEmpty()) }
    }

    @Test
    fun swipeRightRevealsEditAndNeverEditsBeforeTheButtonIsPressed() = runComposeUiTest {
        val events = mutableListOf<ProfileListEvent>()
        val profiles = ProfileListViewState(profiles = listOf(
            ProfileListViewState.Profile("a", "1", "Recipe", "", false),
        ))
        setContent { GeeFlowThemePreview(false) {
            ProfileHome(DeviceDashboardViewState(), profiles, SnackbarHostState(), {}, events::add, {})
        } }
        onNodeWithTag("profile-edit-a").assertDoesNotExist()
        onNodeWithTag("profile-row-a").performTouchInput { swipeRight() }
        onNodeWithTag("profile-edit-a").assertIsDisplayed()
        runOnIdle { assertTrue(events.isEmpty()) }
        onNodeWithTag("profile-edit-a").performClick()
        runOnIdle { assertEquals(listOf<ProfileListEvent>(ProfileListEvent.EditProfileClicked("a")), events) }
    }

    @Test
    fun dockHasSevenClearlyNamedActionsAndNoTemperatureShortcut() = runComposeUiTest {
        val events = mutableListOf<DeviceDashboardEvent>()
        setContent { GeeFlowThemePreview(false) {
            ProfileHome(DeviceDashboardViewState(), ProfileListViewState(), SnackbarHostState(), events::add, {}, {})
        } }
        onNodeWithTag("profile-home-dock").onChildren().filter(hasClickAction()).assertCountEquals(7)
        onAllNodesWithTag("home-dock-free-control").assertCountEquals(1)
        onNodeWithTag("home-dock-free-control").performClick()
        runOnIdle { assertEquals(listOf<DeviceDashboardEvent>(DeviceDashboardEvent.FlowControlClicked), events) }
    }

    @Test
    fun circularMachineActionsKeepBusyAndStopBehavior() = runComposeUiTest {
        val events = mutableListOf<DeviceDashboardEvent>()
        var state by mutableStateOf(DeviceDashboardViewState(brewButtonState = BrewButtonState.Busy))
        setContent { GeeFlowThemePreview(false) {
            ProfileHome(state, ProfileListViewState(), SnackbarHostState(), events::add, {}, {})
        } }
        onNodeWithTag("home-dock-flush").assertIsNotEnabled()
        onNodeWithTag("home-dock-brew").assertIsNotEnabled()
        runOnIdle { state = state.copy(brewButtonState = BrewButtonState.Brewing) }
        onNodeWithTag("home-dock-flush").assertIsNotEnabled()
        onNodeWithTag("home-dock-brew").assertIsEnabled().performClick()
        runOnIdle {
            assertEquals(listOf<DeviceDashboardEvent>(DeviceDashboardEvent.StopBrewClicked), events)
            state = state.copy(brewButtonState = BrewButtonState.Idle)
        }
        onNodeWithTag("home-dock-brew").performClick()
        runOnIdle { assertEquals(listOf(DeviceDashboardEvent.StopBrewClicked, DeviceDashboardEvent.BrewClicked), events) }
    }

    @Test
    fun longPressNamesEveryDockActionWithoutSendingAnyCommand() = runComposeUiTest {
        val deviceEvents = mutableListOf<DeviceDashboardEvent>()
        val profileEvents = mutableListOf<ProfileListEvent>()
        var chartsOpened = 0
        var generation by mutableIntStateOf(0)
        setContent { GeeFlowThemePreview(false) {
            key(generation) {
            ProfileHome(DeviceDashboardViewState(), ProfileListViewState(), SnackbarHostState(), deviceEvents::add, profileEvents::add, { chartsOpened++ })
            }
        } }
        for (action in listOf("profiles","charts","history","free-control","flush","scale","brew")) {
            val node = onNodeWithTag("home-dock-$action")
            val label = node.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
            node.performTouchInput { longClick() }
            onNodeWithTag("home-dock-tooltip").assertIsDisplayed().assertTextEquals(label)
            runOnIdle { generation++ }
        }
        runOnIdle { assertTrue(deviceEvents.isEmpty()); assertTrue(profileEvents.isEmpty()); assertEquals(0,chartsOpened) }
    }

    @Test
    fun historyOverlaysTheExistingOrbitAndCloseRevealsItWithoutRecreatingGlass() = runComposeUiTest {
        var profiles by mutableStateOf(ProfileListViewState())
        val deviceEvents = mutableListOf<DeviceDashboardEvent>()
        val circles = mutableSetOf<String>()
        val activeCircles = mutableSetOf<String>()
        val host = object : NativeCoffeeMaterials {
            override fun setPhase(phase: Int) = Unit
            override fun press(key: String, pressed: Boolean) = Unit
            override fun update(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double) {
                if (width > 140 && kotlin.math.abs(width - height) < 1 && radius >= width * .45) {
                    circles += key
                    activeCircles += key
                }
            }
            override fun remove(key: String) { activeCircles -= key }
        }
        setContent { CompositionLocalProvider(LocalNativeCoffeeMaterials provides host) { GeeFlowThemePreview(false) {
            ProfileHome(DeviceDashboardViewState(), profiles, SnackbarHostState(), deviceEvents::add, {
                if (it == ProfileListEvent.HistoryClicked) profiles = profiles.copy(showHistory = !profiles.showHistory)
            }, {})
        } } }
        runOnIdle { assertEquals(1, activeCircles.size) }
        val originalCircle = activeCircles.single()
        repeat(3) {
            onNodeWithTag("home-dock-history").performClick()
            onNodeWithTag("home-history-panel").assertIsDisplayed()
            onNodeWithTag("home-history-close").assertIsDisplayed()
            runOnIdle { assertTrue(originalCircle in activeCircles) }
            onNodeWithTag("home-history-close").performClick()
            onNodeWithTag("home-history-panel").assertDoesNotExist()
        }
        runOnIdle {
            assertEquals(setOf(originalCircle), circles)
            assertEquals(setOf(originalCircle), activeCircles)
            assertTrue(deviceEvents.isEmpty())
        }
    }
}
