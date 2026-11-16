package app.geeflow.presentation.feature.device.dashboard.quicksettings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import app.geeflow.ui.theme.GeeFlowThemePreview
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TemperatureWorkspaceTest {
    private val state = QuickSettingsViewState(
        brewBoiler = QuickSettingsViewState.Boiler(true, 93.5f, "94", (80..100).map(Int::toString)),
        steamBoiler = QuickSettingsViewState.Boiler(true, 122f, "125", (110..140).map(Int::toString)),
    )
    private fun dial(tag: String) = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress) and
        hasAnyAncestor(hasTestTag(tag))

    @Test
    fun popupRoutesEachBoilerToItsOwnBoundedTarget() = runComposeUiTest {
        val events = mutableListOf<QuickSettingsEvent>()
        setContent { GeeFlowThemePreview(false) { QuickSettingsContent(state, events::add, isExpanded = true) } }
        onNode(dial("boiler-steam")).performSemanticsAction(SemanticsActions.SetProgress) { it(128.4f) }
        onNode(dial("boiler-steam")).performSemanticsAction(SemanticsActions.SetProgress) { it(999f) }
        onNode(dial("boiler-brew")).performSemanticsAction(SemanticsActions.SetProgress) { it(91f) }
        runOnIdle {
            assertEquals(listOf(
                QuickSettingsEvent.SteamTempChanged("128"),
                QuickSettingsEvent.SteamTempChanged("140"),
                QuickSettingsEvent.BrewTempChanged("91"),
            ), events)
        }
    }

    @Test
    fun applyingDisablesBothBoilerInputs() = runComposeUiTest {
        val events = mutableListOf<QuickSettingsEvent>()
        setContent { GeeFlowThemePreview(false) {
            QuickSettingsContent(state.copy(applying = true), events::add, isExpanded = true)
        } }
        onNode(dial("boiler-steam")).assertIsNotEnabled()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(130f) }
        onNode(dial("boiler-brew")).assertIsNotEnabled()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(90f) }
        runOnIdle { assertEquals(emptyList(), events) }
    }
}
