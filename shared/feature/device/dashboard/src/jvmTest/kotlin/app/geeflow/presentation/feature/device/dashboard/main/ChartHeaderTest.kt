package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.*
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.ui.theme.GeeFlowThemePreview
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ChartHeaderTest {
    @Test fun chartsHaveOneHomeButtonInsideTheHeaderToTheRightOfBluetooth() = runComposeUiTest {
        var homes = 0
        val events = mutableListOf<DeviceDashboardEvent>()
        setContent { GeeFlowThemePreview(false) {
            ChartDashboard(DeviceDashboardViewState(), ProfileListViewState(), SnackbarHostState(),
                onEvent = events::add, onProfileListEvent = {}, onHome = { homes++ })
        } }
        onAllNodesWithTag("home-header-home").assertCountEquals(1)
        val home = onNodeWithTag("home-header-home").fetchSemanticsNode().boundsInRoot
        val bluetooth = onNodeWithTag("home-header-connection").fetchSemanticsNode().boundsInRoot
        val group = onNodeWithTag("home-header-actions").fetchSemanticsNode().boundsInRoot
        assertTrue(home.left >= bluetooth.right - 1f)
        assertTrue(group.contains(home.center))
        onNodeWithTag("home-header-home").performClick()
        runOnIdle { assertEquals(1, homes); assertTrue(events.isEmpty()) }
    }
}
