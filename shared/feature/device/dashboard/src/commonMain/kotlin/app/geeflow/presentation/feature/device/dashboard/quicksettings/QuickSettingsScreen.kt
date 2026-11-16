package app.geeflow.presentation.feature.device.dashboard.quicksettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import app.geeflow.ui.components.*
import app.geeflow.ui.theme.coffeeSurface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsViewState.Boiler
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.GeeFlowToggleListItem
import app.geeflow.ui.components.BoilerControlCard
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.common_brew_boiler
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.common_current_temperature
import geeflow.shared.core.ui.generated.resources.common_steam_boiler
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.settings_quick_more
import geeflow.shared.feature.device.dashboard.generated.resources.settings_quick_title
import kotlinx.coroutines.launch
import geeflow.shared.feature.device.dashboard.generated.resources.workspace_temperature
import geeflow.shared.feature.device.dashboard.generated.resources.workspace_target
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun QuickSettingsScreen(
    viewModel: QuickSettingsViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    NavigatorEffect(navigator, viewModel.navEvent)

    EventsDispatcher(viewModel.events) {
        when (it) {
            is QuickSettingsViewModelEvent.ShowSnackbar -> coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(it.message)
            }
        }
    }

    Box {
        QuickSettingsContent(
            viewState = viewState,
            onEvent = viewModel::handleEvent,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
internal fun QuickSettingsContent(
    viewState: QuickSettingsViewState,
    onEvent: (QuickSettingsEvent) -> Unit,
    isExpanded: Boolean = isWidthExpanded(),
    initialSteamSelected: Boolean = false,
) {
    if (isExpanded) { ExpandedTemperatureWorkspace(viewState, onEvent, initialSteamSelected); return }
    CoffeeWorkspaceTheme { Column(
    modifier = Modifier
        .widthIn(max = 940.dp)
        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    GeeFlowDialogTopBar(
        title = stringResource(Res.string.settings_quick_title),
        onCloseClick = { onEvent(QuickSettingsEvent.CloseClicked) },
    )
    VerticalSpacer(16.dp)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        @Composable fun card(boiler: Boiler, steam: Boolean, modifier: Modifier) {
            BoilerControlCard(
                title = stringResource(if (steam) CoreRes.string.common_steam_boiler else CoreRes.string.common_brew_boiler),
                actualTemp = boiler.actualTemp, selectedTemp = boiler.selectedTemp,
                values = boiler.tempList, checked = boiler.enabled, enabled = !viewState.applying,
                onCheckedChanged = { onEvent(if (steam) QuickSettingsEvent.SteamBoilerToggled(it) else QuickSettingsEvent.BrewBoilerToggled(it)) },
                onValueChanged = { onEvent(if (steam) QuickSettingsEvent.SteamTempChanged(it) else QuickSettingsEvent.BrewTempChanged(it)) },
                modifier = modifier,
            )
        }
        if (maxWidth >= 660.dp) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            card(viewState.brewBoiler, false, Modifier.weight(1f))
            card(viewState.steamBoiler, true, Modifier.weight(1f))
        } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            card(viewState.brewBoiler, false, Modifier.fillMaxWidth())
            card(viewState.steamBoiler, true, Modifier.fillMaxWidth())
        }
    }
    VerticalSpacer(24.dp)
    Buttons(viewState.applying, isExpanded, onEvent)
}

    }
}

@Composable
private fun Buttons(
    applying: Boolean,
    isExpanded: Boolean,
    onEvent: (QuickSettingsEvent) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    TextButton(
        onClick = { onEvent(QuickSettingsEvent.MoreSettingsClicked(isExpanded)) },
        content = { Text(stringResource(Res.string.settings_quick_more)) },
    )
    Button(
        enabled = !applying,
        onClick = { if (!applying) onEvent(QuickSettingsEvent.ConfirmClicked) },
        content = {
            AnimatedContent(applying) {
                if (it) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(CoreRes.string.common_confirm))
                }
            }
        },
    )
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() {
    QuickSettingsContent(
        viewState = QuickSettingsViewState(
            steamBoiler = Boiler(
                enabled = true,
                actualTemp = 122.0f,
                selectedTemp = "125",
            ),
            brewBoiler = Boiler(
                enabled = false,
                actualTemp = 93.5f,
                selectedTemp = "93",
            ),
        ),
        onEvent = {},
    )
}

/** Interactive fixture; events update local state and never call a machine. */
@Composable
internal fun QuickSettingsReviewContent(steam: Boolean = false) {
    var state by remember { mutableStateOf(QuickSettingsViewState(
        brewBoiler = Boiler(true, 93.5f, "94", (80..100).map { it.toString() }),
        steamBoiler = Boiler(true, 122f, "125", (110..140).map { it.toString() }),
    )) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        QuickSettingsContent(state, initialSteamSelected = steam, onEvent = { event ->
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

@Composable
private fun ExpandedTemperatureWorkspace(state: QuickSettingsViewState, onEvent: (QuickSettingsEvent) -> Unit, initialSteamSelected: Boolean) {
    var steamSelected by remember { mutableStateOf(initialSteamSelected) }
    val brewTitle = stringResource(CoreRes.string.common_brew_boiler)
    val steamTitle = stringResource(CoreRes.string.common_steam_boiler)
    val boiler = if (steamSelected) state.steamBoiler else state.brewBoiler
    CoffeeWorkspace(
        title = "Wendougee LITA", subtitle = stringResource(Res.string.workspace_temperature),
        onBack = { onEvent(QuickSettingsEvent.CloseClicked) },
        actions = { Button(onClick = { onEvent(QuickSettingsEvent.ConfirmClicked) }, enabled = !state.applying, modifier = Modifier.height(40.dp)) {
            Text(stringResource(CoreRes.string.common_confirm))
        } },
        metrics = {
            CoffeeMetric(brewTitle, state.brewBoiler.actualTemp.toString(), "°")
            CoffeeMetric(steamTitle, state.steamBoiler.actualTemp.toString(), "°")
            CoffeeMetric(stringResource(Res.string.workspace_target), boiler.selectedTemp, "°")
        },
        left = {
            Text(stringResource(Res.string.workspace_temperature), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp))
            listOf(false, true).forEach { steam ->
                Row(Modifier.testTag(if (steam) "boiler-select-steam" else "boiler-select-brew").fillMaxWidth().heightIn(min = 52.dp).clip(MaterialTheme.shapes.small)
                    .background(if (steam == steamSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = .075f) else Color.Transparent)
                    .clickable { steamSelected = steam }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (steam) Icons.Outlined.Air else Icons.Outlined.Thermostat, null, Modifier.size(20.dp))
                    Text(if (steam) steamTitle else brewTitle, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        right = {
            Text(stringResource(Res.string.workspace_target), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(32.dp))
            CoffeeMetric(brewTitle, state.brewBoiler.selectedTemp, "°", small = true)
            Spacer(Modifier.height(28.dp))
            CoffeeMetric(steamTitle, state.steamBoiler.selectedTemp, "°", small = true)
            Spacer(Modifier.weight(1f))
            Buttons(state.applying, true, onEvent)
        },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // A boiler switch starts a fresh control and dismisses any input for the previous boiler.
            key(steamSelected) {
                val steam = steamSelected
                BoilerControlCard(title = if (steamSelected) steamTitle else brewTitle,
                    actualTemp = boiler.actualTemp, selectedTemp = boiler.selectedTemp, values = boiler.tempList,
                    checked = boiler.enabled, enabled = !state.applying,
                    onCheckedChanged = { onEvent(if (steam) QuickSettingsEvent.SteamBoilerToggled(it) else QuickSettingsEvent.BrewBoilerToggled(it)) },
                    onValueChanged = { onEvent(if (steam) QuickSettingsEvent.SteamTempChanged(it) else QuickSettingsEvent.BrewTempChanged(it)) },
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
