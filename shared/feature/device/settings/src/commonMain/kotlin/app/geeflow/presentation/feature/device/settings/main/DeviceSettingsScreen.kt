package app.geeflow.presentation.feature.device.settings.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.settings.BrewingSettings
import app.geeflow.presentation.feature.device.settings.ConnectivitySettings
import app.geeflow.presentation.feature.device.settings.MaintenanceSettings
import app.geeflow.presentation.feature.device.settings.main.DeviceSettingsEvent.BackClicked
import app.geeflow.presentation.feature.device.settings.main.DeviceSettingsEvent.ItemClicked
import app.geeflow.presentation.feature.device.settings.main.DeviceSettingsViewState.Item
import app.geeflow.presentation.feature.user.settings.main.userSettingsListSection
import app.geeflow.ui.components.GeeFlowListScaffold
import app.geeflow.ui.components.GeeFlowSettingsRow
import geeflow.shared.core.ui.generated.resources.common_settings
import geeflow.shared.core.ui.generated.resources.Res as CoreRes
import app.geeflow.ui.components.isListDetailExpanded
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.feature.device.settings.generated.resources.Res
import geeflow.shared.feature.device.settings.generated.resources.device_settings_brewing
import geeflow.shared.feature.device.settings.generated.resources.device_settings_connectivity
import geeflow.shared.feature.device.settings.generated.resources.device_settings_maintenance
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeviceSettingsScreen(
    viewModel: DeviceSettingsViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val selectedIndex = when (navigator.getCurrentDestination()) {
        is BrewingSettings -> viewState.items.indexOfFirst { it is Item.Brewing }
        is MaintenanceSettings -> viewState.items.indexOfFirst { it is Item.Maintenance }
        is ConnectivitySettings -> viewState.items.indexOfFirst { it is Item.Connectivity }
        else -> null
    }.takeIf { it != null && it >= 0 }

    NavigatorEffect(navigator, viewModel.navEvent)
    Content(
        viewState = viewState,
        selectedIndex = selectedIndex,
        onEvent = viewModel::handleEvent,
        userItems = userSettingsListSection(navigator, viewModel.args),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    viewState: DeviceSettingsViewState,
    selectedIndex: Int? = null,
    onEvent: (DeviceSettingsEvent) -> Unit = {},
    userItems: LazyListScope.() -> Unit = {},
) {
    val expanded = isListDetailExpanded()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    GeeFlowListScaffold(
        title = stringResource(CoreRes.string.common_settings),
        navIconClick = { onEvent(BackClicked) },
        expanded = expanded,
        scrollBehavior = scrollBehavior,
        modifier = Modifier,
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding + PaddingValues(
                horizontal = if (expanded) 24.dp else GeeFlowTheme.spacing.contentHorizontal,
                vertical = if (expanded) 8.dp else GeeFlowTheme.spacing.contentVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(viewState.items, key = { index, _ -> "machine-settings-$index" }) { index, item ->
                GeeFlowSettingsRow(
                    title = item.name,
                    selected = expanded && index == selectedIndex,
                    expanded = expanded,
                    onClick = { onEvent(ItemClicked(item)) },
                )
            }
            userItems()
        }
    }
}

@Composable
private fun previewViewState() = DeviceSettingsViewState(
    items = listOf(
        Item.Brewing(stringResource(Res.string.device_settings_brewing)),
        Item.Maintenance(stringResource(Res.string.device_settings_maintenance)),
        Item.Connectivity(stringResource(Res.string.device_settings_connectivity)),
    ),
)

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
fun DeviceSettingsDesignPreview(selected: Int? = null) {
    Content(previewViewState(), selectedIndex = selected)
}
