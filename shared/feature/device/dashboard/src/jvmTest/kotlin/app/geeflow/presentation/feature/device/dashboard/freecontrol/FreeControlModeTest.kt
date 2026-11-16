package app.geeflow.presentation.feature.device.dashboard.freecontrol

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import app.geeflow.ui.theme.GeeFlowThemePreview
import geeflow.shared.core.ui.generated.resources.common_pressure
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_mode_flow
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@OptIn(ExperimentalTestApi::class)
class FreeControlModeTest {
    private val pressure = runBlocking { getString(CoreRes.string.common_pressure) }
    private val flow = runBlocking { getString(Res.string.free_control_mode_flow) }
    private val initial = FreeControlViewState(pressureTarget = 6f, flowTarget = 3.5f,
        pressureRange = 0f..12f, flowRange = 0f..8f)

    @Test
    fun iconSwitchChangesTheSliderRangeAndUnitWithoutStartingABrew() = runComposeUiTest {
        var state by mutableStateOf(initial)
        val events = mutableListOf<FreeControlEvent>()
        setContent { GeeFlowThemePreview(false) {
            FreeControlContent(state, SnackbarHostState(), onEvent = { event ->
                events += event
                if (event is FreeControlEvent.ModeChanged) state = state.copy(mode = event.mode)
            })
        } }
        val slider = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)
        onNodeWithContentDescription(pressure).assertIsSelected()
        onNodeWithContentDescription(flow).performClick()
        onNodeWithContentDescription(flow).assertIsSelected()
        onNode(slider).assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo(3.5f, 0f..8f)))
        onNode(slider).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "3.5 ml/s"))
        onNode(slider).performSemanticsAction(SemanticsActions.SetProgress) { it(4.2f) }
        onNodeWithContentDescription(pressure).performClick()
        onNode(slider).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "6.0 bar"))
        runOnIdle { assertEquals(listOf(FreeControlEvent.ModeChanged(ControlMode.Flow),
            FreeControlEvent.TargetChanged(4.2f), FreeControlEvent.ModeChanged(ControlMode.Pressure)), events) }
    }

    @Test
    fun modelsWithoutFlowSupportDoNotOfferTheFlowAction() = runComposeUiTest {
        setContent { GeeFlowThemePreview(false) {
            FreeControlContent(initial.copy(flowControlSupported = false), SnackbarHostState(), {})
        } }
        onNodeWithContentDescription(pressure).assertExists()
        onNodeWithContentDescription(flow).assertDoesNotExist()
    }
}
