package app.geeflow.presentation.feature.user.settings.appearance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import app.geeflow.ui.components.LocalShowScreensaver
import geeflow.shared.feature.user.settings.generated.resources.user_settings_screensaver_now
import geeflow.shared.feature.user.settings.generated.resources.user_settings_screensaver_size
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.data.user.model.ThemeMode
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.platform.isFullScreenSupported
import app.geeflow.platform.isKeepScreenOnSupported
import app.geeflow.platform.rememberLanguageSettingsLauncher
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.AppThemeClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.CustomColorClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.DarkModeClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.FullScreenChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.KeepScreenOnChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.KeepScreenOnOnlyWhileChargingChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.PaletteStyleClicked
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.ScreensaverChanged
import app.geeflow.presentation.feature.user.settings.appearance.AppearanceSettingsEvent.ScreensaverTimeoutClicked
import app.geeflow.presentation.feature.user.settings.appearance.components.AppearanceDialogs
import app.geeflow.ui.components.GeeFlowDetailScaffold
import app.geeflow.ui.components.settingsDetailVerticalPadding
import app.geeflow.ui.components.GeeFlowListItem
import app.geeflow.ui.components.GeeFlowNavigationListItem
import app.geeflow.ui.components.GeeFlowToggleListItem
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.feature.user.settings.generated.resources.Res
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_app_theme
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_custom_color
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_dark_mode
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_description
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_full_screen
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_keep_screen_on
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_keep_screen_on_only_while_charging
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_language
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_palette_style
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_screensaver
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_screensaver_timeout
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_screensaver_unit_minutes
import geeflow.shared.feature.user.settings.generated.resources.user_settings_clock_schedule
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance_title
import geeflow.shared.feature.user.settings.generated.resources.user_settings_text_size
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AppearanceSettingsScreen(
    viewModel: AppearanceSettingsViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val languageLauncher = rememberLanguageSettingsLauncher()

    NavigatorEffect(navigator, viewModel.navEvent)

    Content(
        viewState = viewState,
        isLanguageSupported = languageLauncher != null,
        onLanguageClicked = { languageLauncher?.invoke() },
        onEvent = viewModel::handleEvent,
    )

    AppearanceDialogs(
        dialog = viewState.dialog,
        currentThemeMode = viewState.themeMode,
        currentAppTheme = viewState.appTheme,
        currentPaletteStyle = viewState.paletteStyle,
        currentCustomSeedColor = viewState.customSeedColor,
        clockThemeSchedule = viewState.clockThemeSchedule,
        onEvent = viewModel::handleEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    viewState: AppearanceSettingsViewState,
    isLanguageSupported: Boolean = false,
    onLanguageClicked: () -> Unit = {},
    onEvent: (AppearanceSettingsEvent) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    GeeFlowDetailScaffold(
        title = stringResource(Res.string.user_settings_appearance_title),
        subtitle = stringResource(Res.string.user_settings_appearance_description),
        navIconClick = { onEvent(BackClicked) },
        scrollBehavior = scrollBehavior,
        modifier = Modifier,
        content = { paddingValues ->
            AppearanceContent(
                viewState = viewState,
                isLanguageSupported = isLanguageSupported,
                onLanguageClicked = onLanguageClicked,
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
private fun AppearanceContent(
    viewState: AppearanceSettingsViewState,
    isLanguageSupported: Boolean = false,
    onLanguageClicked: () -> Unit = {},
    onEvent: (AppearanceSettingsEvent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(horizontal = GeeFlowTheme.spacing.contentHorizontal, vertical = 16.dp)) {
            var textSize by remember(viewState.textSizePercent) { mutableFloatStateOf(viewState.textSizePercent.toFloat()) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.user_settings_text_size), style = MaterialTheme.typography.titleMedium)
                Text("${textSize.roundToInt()}%", style = MaterialTheme.typography.titleMedium)
            }
            Slider(value = textSize, onValueChange = { textSize = it },
                onValueChangeFinished = { onEvent(AppearanceSettingsEvent.TextSizeChanged(textSize.roundToInt())) },
                valueRange = 100f..160f, steps = 5, modifier = Modifier.testTag("text-size-slider"))
        }
        GeeFlowNavigationListItem(
            title = stringResource(Res.string.user_settings_appearance_dark_mode),
            subtitle = stringResource(viewState.themeMode.titleRes),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(DarkModeClicked) },
        )
        AnimatedVisibility(viewState.themeMode == ThemeMode.CLOCK) {
            GeeFlowNavigationListItem(
                title = stringResource(Res.string.user_settings_clock_schedule),
                modifier = Modifier.fillMaxWidth().testTag("clock-theme-schedule")
                    .clickable { onEvent(AppearanceSettingsEvent.ClockScheduleClicked) },
            )
        }
        if (viewState.themeMode != ThemeMode.CLOCK) {
            GeeFlowNavigationListItem(
                title = stringResource(Res.string.user_settings_appearance_app_theme),
                subtitle = stringResource(viewState.appTheme.titleRes),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(AppThemeClicked) },
            )
        }
        if (viewState.themeMode != ThemeMode.CLOCK && viewState.appTheme != AppTheme.SYSTEM && viewState.appTheme != AppTheme.ESPRESSO) {
            GeeFlowNavigationListItem(
                title = stringResource(Res.string.user_settings_appearance_palette_style),
                subtitle = stringResource(viewState.paletteStyle.titleRes),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .clickable { onEvent(PaletteStyleClicked) },
            )
        }
        if (viewState.themeMode != ThemeMode.CLOCK && viewState.appTheme == AppTheme.CUSTOM) {
            GeeFlowNavigationListItem(
                title = stringResource(Res.string.user_settings_appearance_custom_color),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .clickable { onEvent(CustomColorClicked) },
            )
        }
        if (isLanguageSupported) {
            GeeFlowNavigationListItem(
                title = stringResource(Res.string.user_settings_appearance_language),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLanguageClicked),
            )
        }
        if (isFullScreenSupported) {
            GeeFlowToggleListItem(
                title = stringResource(Res.string.user_settings_appearance_full_screen),
                checked = viewState.fullScreenMode,
                onCheckedChanged = { onEvent(FullScreenChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (isKeepScreenOnSupported) {
            GeeFlowToggleListItem(
                title = stringResource(Res.string.user_settings_appearance_keep_screen_on),
                checked = viewState.keepScreenOn,
                onCheckedChanged = { onEvent(KeepScreenOnChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedVisibility(
                visible = viewState.keepScreenOn,
            ) {
                GeeFlowToggleListItem(
                    title = stringResource(Res.string.user_settings_appearance_keep_screen_on_only_while_charging),
                    checked = viewState.keepScreenOnOnlyWhileCharging,
                    onCheckedChanged = { onEvent(KeepScreenOnOnlyWhileChargingChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                )
            }
            GeeFlowToggleListItem(
                title = stringResource(Res.string.user_settings_appearance_screensaver),
                checked = viewState.screensaverEnabled,
                onCheckedChanged = { onEvent(ScreensaverChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = GeeFlowTheme.spacing.contentHorizontal, vertical = 16.dp)) {
                var clockSize by remember(viewState.screensaverSizePercent) { mutableFloatStateOf(viewState.screensaverSizePercent.toFloat()) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(Res.string.user_settings_screensaver_size), style = MaterialTheme.typography.titleMedium)
                    Text("${clockSize.roundToInt()}%", style = MaterialTheme.typography.titleMedium)
                }
                Slider(value = clockSize, onValueChange = { clockSize = it }, valueRange = 60f..200f, steps = 13,
                    onValueChangeFinished = { onEvent(AppearanceSettingsEvent.ScreensaverSizeChanged(clockSize.roundToInt())) },
                    modifier = Modifier.testTag("screensaver-size-slider"))
            }
            LocalShowScreensaver.current?.let { show ->
                TextButton(onClick = show, modifier = Modifier.testTag("show-screensaver-now")) {
                    Text(stringResource(Res.string.user_settings_screensaver_now))
                }
            }
            AnimatedVisibility(
                visible = viewState.screensaverEnabled,
            ) {
                val unit = stringResource(Res.string.user_settings_appearance_screensaver_unit_minutes)
                GeeFlowListItem(
                    title = stringResource(Res.string.user_settings_appearance_screensaver_timeout),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .clickable { onEvent(ScreensaverTimeoutClicked) },
                    trailingContent = {
                        Surface(
                            onClick = { onEvent(ScreensaverTimeoutClicked) },
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = "${viewState.screensaverTimeoutMinutes} $unit",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
fun AppearanceSettingsDesignPreview(scene: String = "") {
    val state = AppearanceSettingsViewState()
    AppearanceDialogs(when (scene) {
        "theme-mode" -> AppearanceSettingsViewState.Dialog.DarkMode
        "clock-schedule" -> AppearanceSettingsViewState.Dialog.ClockSchedule
        "theme-picker" -> AppearanceSettingsViewState.Dialog.AppTheme
        "palette-picker" -> AppearanceSettingsViewState.Dialog.PaletteStyle
        "custom-color" -> AppearanceSettingsViewState.Dialog.CustomColor
        "screensaver-timeout" -> AppearanceSettingsViewState.Dialog.ScreensaverTimeout
        else -> null
    }, state.themeMode, state.appTheme, state.paletteStyle, state.customSeedColor, {})
    Content(
        viewState = AppearanceSettingsViewState(),
        isLanguageSupported = true,
    )
}
