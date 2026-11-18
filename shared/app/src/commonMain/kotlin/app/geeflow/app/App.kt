package app.geeflow.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.geeflow.app.autoflush.AutoFlushHost
import app.geeflow.app.navigation.RootNavigation
import app.geeflow.app.screensaver.ScreensaverContainer
import app.geeflow.app.theme.rememberAppColorScheme
import app.geeflow.app.theme.rememberAppearanceSettings
import app.geeflow.app.theme.toUiThemeMode
import app.geeflow.platform.FullScreenEffect
import app.geeflow.platform.ThemeModeEffect
import app.geeflow.platform.isKeepScreenOnSupported
import app.geeflow.platform.rememberIsConnectedToPower
import app.geeflow.ui.theme.GeeFlowTheme
import app.geeflow.ui.theme.CoffeeDayPhase
import app.geeflow.ui.theme.rememberCoffeeClockAppearance
import app.geeflow.ui.theme.coffeeColorScheme
import app.geeflow.ui.theme.ThemeMode
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import org.koin.compose.KoinApplication
import org.koin.plugin.module.dsl.koinConfiguration
import app.geeflow.data.user.model.ThemeMode as UserThemeMode

@Composable
fun App(closeApp: () -> Unit) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { addPlatformFileSupport() }
            .build()
    }

    KoinApplication(koinConfiguration<GeeFlowApp>()) {
        val appearance by rememberAppearanceSettings()
        val clockAppearance = if (appearance.themeMode == UserThemeMode.CLOCK) {
            rememberCoffeeClockAppearance(appearance.clockThemeSchedule)
        } else null
        val darkMode = when (appearance.themeMode) {
            UserThemeMode.CLOCK -> clockAppearance?.phase?.dark == true
            UserThemeMode.SYSTEM -> isSystemInDarkTheme()
            UserThemeMode.LIGHT -> false
            UserThemeMode.DARK -> true
        }

        val phase = clockAppearance?.phase ?: if (darkMode) CoffeeDayPhase.Night else CoffeeDayPhase.Day
        GeeFlowTheme(
            darkMode = darkMode,
            textSizePercent = appearance.textSizePercent,
            colorScheme = if (clockAppearance != null) coffeeColorScheme(phase)
                else rememberAppColorScheme(appearance, darkMode),
            modifier = Modifier.fillMaxSize(),
            phase = phase,
            ambient = clockAppearance?.ambient,
        ) {
            ThemeModeEffect(if (appearance.themeMode == UserThemeMode.CLOCK) {
                if (darkMode) ThemeMode.Dark else ThemeMode.Light
            } else appearance.themeMode.toUiThemeMode())
            FullScreenEffect(appearance.fullScreenMode)
            ScreensaverContainer(
                enabled = appearance.screensaverEnabled && isKeepScreenOnSupported,
                timeoutMinutes = appearance.screensaverTimeoutMinutes,
                clockSizePercent = appearance.screensaverSizePercent,
            ) {
                RootNavigation(closeApp)
            }
            val connectedToPower = rememberIsConnectedToPower()
            ProfileExecutionEffect(
                appearance.screensaverEnabled ||
                    (appearance.keepScreenOn && (!appearance.keepScreenOnOnlyWhileCharging || connectedToPower)),
            )
            AutoFlushHost()
        }
    }
}
