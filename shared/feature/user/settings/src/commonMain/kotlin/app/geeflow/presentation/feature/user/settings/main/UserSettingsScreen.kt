package app.geeflow.presentation.feature.user.settings.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.user.settings.AboutSettings
import app.geeflow.presentation.feature.user.settings.AppearanceSettings
import app.geeflow.presentation.feature.user.settings.BrewingPreferencesSettings
import app.geeflow.presentation.feature.user.settings.ProfileSettings
import app.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.ChangeUserClicked
import app.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.ItemClicked
import app.geeflow.presentation.feature.user.settings.main.UserSettingsViewState.Item
import app.geeflow.ui.components.GeeFlowListScaffold
import app.geeflow.ui.components.GeeFlowSettingsRow
import app.geeflow.ui.components.isListDetailExpanded
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.feature.user.settings.generated.resources.Res
import geeflow.shared.feature.user.settings.generated.resources.user_settings_about
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance
import geeflow.shared.feature.user.settings.generated.resources.user_settings_brewing_preferences
import geeflow.shared.feature.user.settings.generated.resources.user_settings_change
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile
import geeflow.shared.feature.user.settings.generated.resources.user_settings_subtitle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun UserSettingsScreen(
    viewModel: UserSettingsViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val selectedIndex = selectedUserSettingsIndex(viewState, navigator.getCurrentDestination())

    NavigatorEffect(navigator, viewModel.navEvent)

    Content(
        viewState = viewState,
        selectedIndex = selectedIndex,
        onEvent = { viewModel.handleEvent(it) },
    )
}

/** Adds the same profile, preferences, display and app entries to a shared settings sidebar. */
@Composable
fun userSettingsListSection(navigator: Navigator, settingsRoot: NavKey): LazyListScope.() -> Unit {
    val viewModel = koinViewModel<UserSettingsViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val selectedIndex = selectedUserSettingsIndex(viewState, navigator.getCurrentDestination())
    val expanded = isListDetailExpanded()
    NavigatorEffect(navigator, viewModel.navEvent)

    return {
        userSettingsItems(viewState, selectedIndex, expanded, { viewModel.handleEvent(it, settingsRoot) })
    }
}

private fun selectedUserSettingsIndex(viewState: UserSettingsViewState, destination: NavKey?): Int? =
    when (destination) {
        ProfileSettings -> viewState.items.indexOfFirst { it is Item.Profile }
        BrewingPreferencesSettings -> viewState.items.indexOfFirst { it is Item.BrewingPreferences }
        AppearanceSettings -> viewState.items.indexOfFirst { it is Item.AppearanceDisplay }
        AboutSettings -> viewState.items.indexOfFirst { it is Item.About }
        else -> null
    }.takeIf { it != null && it >= 0 }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    viewState: UserSettingsViewState,
    selectedIndex: Int? = null,
    onEvent: (UserSettingsEvent) -> Unit = {},
) {
    val expanded = isListDetailExpanded()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    GeeFlowListScaffold(
        title = viewState.userName,
        subtitle = stringResource(Res.string.user_settings_subtitle),
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
            userSettingsItems(viewState, selectedIndex, expanded, onEvent)
        }
    }
}

private fun LazyListScope.userSettingsItems(
    viewState: UserSettingsViewState,
    selectedIndex: Int?,
    expanded: Boolean,
    onEvent: (UserSettingsEvent) -> Unit,
) {
    itemsIndexed(viewState.items, key = { index, _ -> "user-settings-$index" }) { index, item ->
        GeeFlowSettingsRow(
            title = item.name,
            selected = expanded && index == selectedIndex,
            expanded = expanded,
            onClick = { onEvent(ItemClicked(item)) },
            trailingContent = if (item is Item.Profile) {
                {
                    TextButton(
                        onClick = { onEvent(ChangeUserClicked) },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    ) {
                        Text(text = stringResource(Res.string.user_settings_change))
                    }
                }
            } else null,
        )
    }
}

@Composable
private fun previewViewState() = UserSettingsViewState(
    userName = "Barista",
    items = listOf(
        Item.AppearanceDisplay(stringResource(Res.string.user_settings_appearance)),
        Item.BrewingPreferences(stringResource(Res.string.user_settings_brewing_preferences)),
        Item.About(stringResource(Res.string.user_settings_about)),
        Item.Profile(stringResource(Res.string.user_settings_profile)),
    ),
)

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
fun UserSettingsDesignPreview(selected: Int? = null) {
    Content(previewViewState(), selectedIndex = selected)
}
