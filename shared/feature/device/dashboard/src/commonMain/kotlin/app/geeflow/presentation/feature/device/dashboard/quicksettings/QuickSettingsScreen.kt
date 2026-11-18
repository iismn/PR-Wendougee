package app.geeflow.presentation.feature.device.dashboard.quicksettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardPreviewContent
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsViewState.Boiler
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.*
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.coffeeSurface
import geeflow.shared.core.ui.generated.resources.common_brew_boiler
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.common_steam_boiler
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.settings_quick_more
import geeflow.shared.feature.device.dashboard.generated.resources.settings_quick_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun QuickSettingsScreen(viewModel: QuickSettingsViewModel, navigator: Navigator) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    NavigatorEffect(navigator, viewModel.navEvent)
    EventsDispatcher(viewModel.events) {
        when (it) {
            is QuickSettingsViewModelEvent.ShowSnackbar -> scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(it.message)
            }
        }
    }
    Box(Modifier.padding(24.dp)) {
        QuickSettingsContent(state, viewModel::handleEvent)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

/** The navigation dialog owns the overlay; the two boilers share this one surface. */
@Composable
internal fun QuickSettingsContent(
    viewState: QuickSettingsViewState,
    onEvent: (QuickSettingsEvent) -> Unit,
    isExpanded: Boolean = isWidthExpanded(),
) = CoffeeWorkspaceTheme {
    Column(
        Modifier.testTag("quick-temperature-dialog").widthIn(max = 720.dp).fillMaxWidth()
            .clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        GeeFlowDialogTopBar(stringResource(Res.string.settings_quick_title),
            onCloseClick = { onEvent(QuickSettingsEvent.CloseClicked) })
        VerticalSpacer(16.dp)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            @Composable fun card(boiler: Boiler, steam: Boolean, modifier: Modifier) {
                BoilerControlCard(
                    title = stringResource(if (steam) CoreRes.string.common_steam_boiler else CoreRes.string.common_brew_boiler),
                    actualTemp = boiler.actualTemp, selectedTemp = boiler.selectedTemp,
                    values = boiler.tempList, checked = boiler.enabled, enabled = !viewState.applying,
                    onCheckedChanged = { onEvent(if (steam) QuickSettingsEvent.SteamBoilerToggled(it) else QuickSettingsEvent.BrewBoilerToggled(it)) },
                    onValueChanged = { onEvent(if (steam) QuickSettingsEvent.SteamTempChanged(it) else QuickSettingsEvent.BrewTempChanged(it)) },
                    modifier = modifier.testTag(if (steam) "boiler-steam" else "boiler-brew"),
                    embedded = true,
                )
            }
            if (maxWidth >= 560.dp) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                card(viewState.brewBoiler, false, Modifier.weight(1f))
                card(viewState.steamBoiler, true, Modifier.weight(1f))
            } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                card(viewState.brewBoiler, false, Modifier.fillMaxWidth())
                card(viewState.steamBoiler, true, Modifier.fillMaxWidth())
            }
        }
        VerticalSpacer(20.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onEvent(QuickSettingsEvent.MoreSettingsClicked(isExpanded)) },
                modifier = Modifier.height(CoffeeUi.touch)) { Text(stringResource(Res.string.settings_quick_more)) }
            Button(onClick = { onEvent(QuickSettingsEvent.ConfirmClicked) }, enabled = !viewState.applying,
                modifier = Modifier.height(CoffeeUi.touch)) {
                AnimatedContent(viewState.applying) { applying ->
                    if (applying) CircularProgressIndicator(Modifier.size(CoffeeUi.icon),
                        color = MaterialTheme.colorScheme.onPrimary)
                    else Text(stringResource(CoreRes.string.common_confirm))
                }
            }
        }
    }
}

/** Local fixture over the actual home; no machine commands are sent. */
@Composable
internal fun QuickSettingsReviewContent(applying: Boolean = false) {
    var state by remember { mutableStateOf(QuickSettingsViewState(
        brewBoiler = Boiler(true, 93.5f, "94", (80..100).map(Int::toString)),
        steamBoiler = Boiler(true, 122f, "125", (110..140).map(Int::toString)),
        applying = applying,
    )) }
    Box(Modifier.fillMaxSize()) {
        DeviceDashboardPreviewContent()
        CoffeePopup(onDismissRequest = {}, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.padding(24.dp)) {
            QuickSettingsContent(state, onEvent = { event ->
                state = when (event) {
                    is QuickSettingsEvent.BrewTempChanged -> state.copy(brewBoiler = state.brewBoiler.copy(selectedTemp = event.temp))
                    is QuickSettingsEvent.SteamTempChanged -> state.copy(steamBoiler = state.steamBoiler.copy(selectedTemp = event.temp))
                    is QuickSettingsEvent.BrewBoilerToggled -> state.copy(brewBoiler = state.brewBoiler.copy(enabled = event.enabled))
                    is QuickSettingsEvent.SteamBoilerToggled -> state.copy(steamBoiler = state.steamBoiler.copy(enabled = event.enabled))
                    else -> state
                }
            })
        }
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() = QuickSettingsReviewContent()
