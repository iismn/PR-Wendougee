package app.geeflow.presentation.feature.user.settings.brewing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.AutoConnectChanged
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.RestoreDefaultProfilesClicked
import app.geeflow.presentation.feature.user.settings.brewing.BrewingPreferencesEvent.TemperatureUnitClicked
import app.geeflow.presentation.feature.user.settings.brewing.components.BrewingPreferencesDialogs
import app.geeflow.ui.components.GeeFlowDetailScaffold
import app.geeflow.ui.components.settingsDetailVerticalPadding
import app.geeflow.ui.components.GeeFlowNavigationListItem
import app.geeflow.ui.components.GeeFlowToggleListItem
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.feature.user.settings.generated.resources.Res
import geeflow.shared.feature.user.settings.generated.resources.user_settings_brewing_auto_connect
import geeflow.shared.feature.user.settings.generated.resources.user_settings_brewing_preferences_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_brewing_preferences_title
import geeflow.shared.feature.user.settings.generated.resources.user_settings_brewing_restore_default_profiles
import geeflow.shared.feature.user.settings.generated.resources.user_settings_brewing_temperature_unit
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BrewingPreferencesScreen(
    viewModel: BrewingPreferencesViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    NavigatorEffect(navigator, viewModel.navEvent)

    Content(
        viewState = viewState,
        onEvent = viewModel::handleEvent,
    )

    BrewingPreferencesDialogs(
        dialog = viewState.dialog,
        currentTemperatureUnit = viewState.temperatureUnit,
        onEvent = viewModel::handleEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    viewState: BrewingPreferencesViewState,
    onEvent: (BrewingPreferencesEvent) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    GeeFlowDetailScaffold(
        title = stringResource(Res.string.user_settings_brewing_preferences_title),
        subtitle = stringResource(Res.string.user_settings_brewing_preferences_description),
        navIconClick = { onEvent(BackClicked) },
        scrollBehavior = scrollBehavior,
        modifier = Modifier,
        content = { paddingValues ->
            BrewingContent(
                viewState = viewState,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(settingsDetailVerticalPadding(firstItemPadding = 16.dp)),
            )
        },
    )
}

@Composable
private fun BrewingContent(
    viewState: BrewingPreferencesViewState,
    onEvent: (BrewingPreferencesEvent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        GeeFlowToggleListItem(
            title = stringResource(Res.string.user_settings_brewing_auto_connect),
            checked = viewState.autoConnect,
            onCheckedChanged = { onEvent(AutoConnectChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_brewing_temperature_unit),
            subtitle = stringResource(viewState.temperatureUnit.titleRes),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(TemperatureUnitClicked) },
        )
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_brewing_restore_default_profiles),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(RestoreDefaultProfilesClicked) },
        )
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
fun BrewingPreferencesDesignPreview(scene: String = "") {
    BrewingPreferencesDialogs(when (scene) {
        "temperature-unit" -> BrewingPreferencesViewState.Dialog.TemperatureUnit
        "restore-profiles" -> BrewingPreferencesViewState.Dialog.RestoreDefaultProfiles
        else -> null
    }, BrewingPreferencesViewState().temperatureUnit, {})
    Content(viewState = BrewingPreferencesViewState())
}
