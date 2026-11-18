package app.geeflow.presentation.feature.device.dashboard.quickmaintenance

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import app.geeflow.ui.theme.GeeFlowThemePreview
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class QuickMaintenanceInputTest {
    @Test
    fun allThreeDraftValuesUpdateWhileDraggingWithoutStartingCleaning() = runComposeUiTest {
        val events = mutableListOf<QuickMaintenanceEvent>()
        var state by mutableStateOf(QuickMaintenanceViewState())
        setContent { GeeFlowThemePreview(false) {
            QuickMaintenanceContent(state, { event ->
                events += event
                state = when (event) {
                    is QuickMaintenanceEvent.FlushChanged -> state.copy(flushProgress = state.flushProgress.copy(target = event.seconds))
                    is QuickMaintenanceEvent.RestChanged -> state.copy(restProgress = state.restProgress.copy(target = event.seconds))
                    is QuickMaintenanceEvent.CyclesChanged -> state.copy(cycleProgress = state.cycleProgress.copy(target = event.count))
                    else -> state
                }
            }, isExpanded = true)
        } }
        // Ranges arrive from the machine after the popup was composed.
        runOnIdle { state = state.copy(flushRange = 1..60, restRange = 0..60, cycleRange = 1..20,
            flushProgress = QuickMaintenanceViewState.Progress(0,5), restProgress = QuickMaintenanceViewState.Progress(0,5),
            cycleProgress = QuickMaintenanceViewState.Progress(0,3)) }
        val tags = listOf("maintenance-flush", "maintenance-rest", "maintenance-cycles")
        for (tag in tags) {
            onNodeWithTag(tag).assertIsDisplayed()
            tags.filter { it != tag }.forEach { onNodeWithTag(it).assertDoesNotExist() }
            onNodeWithTag(tag).performTouchInput { swipe(Offset(width - 20f, center.y), Offset(25f, center.y)) }
            onNodeWithTag("maintenance-next").performClick()
        }
        runOnIdle {
            assertTrue(state.flushProgress.target > 5)
            assertTrue(state.restProgress.target > 5)
            assertTrue(state.cycleProgress.target > 3)
            assertTrue(events.none { it == QuickMaintenanceEvent.ToggleCleaningClicked })
        }
        onNodeWithTag("maintenance-flush-value").assertContentDescriptionEquals(state.flushProgress.target.toString())
        onNodeWithTag("maintenance-previous").performClick()
        onNodeWithTag("maintenance-cycles-value").assertContentDescriptionEquals(state.cycleProgress.target.toString())
        onNodeWithTag("maintenance-previous").performClick()
        onNodeWithTag("maintenance-rest-value").assertContentDescriptionEquals(state.restProgress.target.toString())
    }

    @Test
    fun runningCleaningDisablesEditing() = runComposeUiTest {
        setContent { GeeFlowThemePreview(false) {
            QuickMaintenanceContent(QuickMaintenanceViewState(isCleaning = true,
                flushRange = 1..60, restRange = 0..60, cycleRange = 1..20), {}, isExpanded = true)
        } }
        listOf("maintenance-flush", "maintenance-rest", "maintenance-cycles").forEach {
            onNodeWithTag(it).assertIsNotEnabled()
            onNodeWithTag("maintenance-next").performClick()
        }
    }

    @Test
    fun informationIconOpensTheExplanationWithoutStartingCleaning() = runComposeUiTest {
        val events = mutableListOf<QuickMaintenanceEvent>()
        setContent { GeeFlowThemePreview(false) { QuickMaintenanceContent(QuickMaintenanceViewState(), events::add, isExpanded = true) } }
        onNodeWithTag("maintenance-info-dialog").assertDoesNotExist()
        onNodeWithTag("maintenance-info").performClick()
        onNodeWithTag("maintenance-info-dialog").assertIsDisplayed()
        runOnIdle { assertTrue(events.isEmpty()) }
    }
}
