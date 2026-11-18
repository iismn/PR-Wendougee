package app.geeflow.presentation.feature.device.dashboard.quickscale

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import app.geeflow.ui.theme.GeeFlowThemePreview
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class QuickScaleContentTest {
    @Test fun scaleActionsDispatchOnlyForTheChosenScaleAndBusyStateBlocksFurtherChanges() = runComposeUiTest {
        val events = mutableListOf<QuickScaleEvent>()
        var state by mutableStateOf(QuickScaleViewState(machineConnected = true, enabled = true,
            devices = listOf(QuickScaleViewState.Scale("A"), QuickScaleViewState.Scale("B", true))))
        setContent { GeeFlowThemePreview(false) { QuickScaleContent(state, events::add) } }
        onNodeWithTag("quick-scale-device-A").performClick()
        onNodeWithTag("quick-scale-device-B").performClick()
        onNodeWithTag("quick-scale-search").performClick()
        runOnIdle {
            assertEquals(listOf(QuickScaleEvent.ConnectionClicked("A"), QuickScaleEvent.ConnectionClicked("B"), QuickScaleEvent.SearchClicked), events)
            state = state.copy(busy = true, busyName = "A")
        }
        onNodeWithTag("quick-scale-enable").assertIsNotEnabled()
        onNodeWithTag("quick-scale-device-A").assertIsNotEnabled()
        onNodeWithTag("quick-scale-device-B").assertIsNotEnabled()
        onNodeWithTag("quick-scale-search").assertIsNotEnabled()
    }
    @Test fun disconnectedMachineCannotToggleOrSearch() = runComposeUiTest {
        setContent { GeeFlowThemePreview(false) { QuickScaleContent(QuickScaleViewState(), {}) } }
        onNodeWithTag("quick-scale-enable").assertIsNotEnabled()
        onNodeWithTag("quick-scale-search").assertIsNotEnabled()
    }
}
