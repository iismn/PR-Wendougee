package app.geeflow.presentation.feature.device.dashboard.quickscale

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardPreviewContent
import app.geeflow.ui.components.*
import geeflow.shared.core.ui.generated.resources.common_connect
import geeflow.shared.core.ui.generated.resources.common_disconnect
import geeflow.shared.feature.device.dashboard.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun QuickScaleScreen(viewModel: QuickScaleViewModel, navigator: Navigator) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    NavigatorEffect(navigator, viewModel.navEvent)
    Box(Modifier.padding(24.dp)) { QuickScaleContent(state, viewModel::handleEvent) }
}

@Composable
internal fun QuickScaleContent(state: QuickScaleViewState, onEvent: (QuickScaleEvent) -> Unit) = CoffeeWorkspaceTheme {
    Column(Modifier.testTag("quick-scale-dialog").widthIn(max = 600.dp).fillMaxWidth()
        .clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface)
        .animateContentSize(tween(220, easing = CoffeeMotionEasing))
        .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        GeeFlowDialogTopBar(stringResource(Res.string.quick_scale_title),
            onCloseClick = { onEvent(QuickScaleEvent.CloseClicked) })
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.MonitorWeight, null, Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(Res.string.quick_scale_enabled), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f))
            Switch(checked = state.enabled, enabled = state.machineConnected && !state.busy,
                onCheckedChange = { onEvent(QuickScaleEvent.EnabledChanged(it)) },
                modifier = Modifier.testTag("quick-scale-enable"))
        }
        when {
            !state.machineConnected -> Text(stringResource(Res.string.quick_scale_machine_disconnected),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            !state.enabled -> Text(stringResource(Res.string.quick_scale_disabled),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                if (state.searching) Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.quick_scale_searching), style = MaterialTheme.typography.bodyMedium)
                }
                if (state.devices.isEmpty()) Box(Modifier.fillMaxWidth().heightIn(min = 100.dp), Alignment.Center) {
                    Text(stringResource(Res.string.quick_scale_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.devices, key = { it.name }) { scale ->
                        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(scale.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                if (scale.connected) Text(stringResource(Res.string.quick_scale_connected),
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton(onClick = { onEvent(QuickScaleEvent.ConnectionClicked(scale.name)) },
                                enabled = state.machineConnected && !state.busy,
                                modifier = Modifier.heightIn(min = 48.dp).testTag("quick-scale-device-${scale.name}")) {
                                if (state.busyName == scale.name) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Text(stringResource(if (scale.connected) CoreRes.string.common_disconnect else CoreRes.string.common_connect))
                            }
                        }
                    }
                }
            }
        }
        if (state.error) Text(stringResource(Res.string.quick_scale_error), color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("quick-scale-error"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onEvent(QuickScaleEvent.MoreSettingsClicked) },
                enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.quick_scale_more))
            }
            Button(onClick = { onEvent(QuickScaleEvent.SearchClicked) },
                enabled = state.machineConnected && state.enabled && !state.searching && !state.busy,
                modifier = Modifier.heightIn(min = 48.dp).testTag("quick-scale-search")) {
                Text(stringResource(Res.string.quick_scale_search))
            }
        }
    }
}

/** Review fixture: no discovery, connection or machine command is invoked. */
@Composable
internal fun QuickScaleDesignPreview() {
    var shown by remember { mutableStateOf(true) }
    var state by remember { mutableStateOf(QuickScaleViewState(machineConnected = true, enabled = true,
        devices = listOf(QuickScaleViewState.Scale("Wendougee Scale", true), QuickScaleViewState.Scale("Coffee Scale")))) }
    Box(Modifier.fillMaxSize()) {
        DeviceDashboardPreviewContent()
        if (shown) CoffeePopup(onDismissRequest = { shown = false },
            properties = DialogProperties(usePlatformDefaultWidth = false), modifier = Modifier.padding(24.dp)) {
            QuickScaleContent(state) { event ->
                when (event) {
                    is QuickScaleEvent.EnabledChanged -> state = state.copy(enabled = event.enabled)
                    is QuickScaleEvent.ConnectionClicked -> state = state.copy(devices = state.devices.map {
                        it.copy(connected = it.name == event.name && !it.connected)
                    })
                    QuickScaleEvent.CloseClicked -> shown = false
                    else -> Unit
                }
            }
        }
    }
}
