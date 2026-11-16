package app.geeflow.presentation.feature.device.dashboard.quicksettings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import app.geeflow.ui.theme.GeeFlowThemePreview
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TemperatureWorkspaceTest {
    @Test
    fun switchingBoilersRoutesBoundedTargetsToTheSelectedBoiler() = runComposeUiTest {
        val events = mutableListOf<QuickSettingsEvent>()
        val state = QuickSettingsViewState(
            brewBoiler = QuickSettingsViewState.Boiler(true, 93.5f, "94", (80..100).map(Int::toString)),
            steamBoiler = QuickSettingsViewState.Boiler(true, 122f, "125", (110..140).map(Int::toString)),
        )
        setContent { GeeFlowThemePreview(false) { QuickSettingsContent(state, events::add, isExpanded = true) } }
        onNodeWithTag("boiler-select-steam").performClick()
        val dial = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)
        onNode(dial).performSemanticsAction(SemanticsActions.SetProgress) { it(128.4f) }
        onNode(dial).performSemanticsAction(SemanticsActions.SetProgress) { it(999f) }
        onNodeWithTag("boiler-select-brew").performClick()
        onNode(dial).performSemanticsAction(SemanticsActions.SetProgress) { it(91f) }
        runOnIdle {
            assertEquals(listOf(
                QuickSettingsEvent.SteamTempChanged("128"),
                QuickSettingsEvent.SteamTempChanged("140"),
                QuickSettingsEvent.BrewTempChanged("91"),
            ), events)
        }
    }
}
