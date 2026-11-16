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
import app.geeflow.ui.theme.rememberCoffeeDayPhase
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
        val phase = rememberCoffeeDayPhase()
        val darkMode = when (appearance.themeMode) {
            UserThemeMode.CLOCK -> phase.dark
            UserThemeMode.SYSTEM -> isSystemInDarkTheme()
            UserThemeMode.LIGHT -> false
            UserThemeMode.DARK -> true
        }

        GeeFlowTheme(
            darkMode = darkMode,
            colorScheme = if (appearance.themeMode == UserThemeMode.CLOCK) coffeeColorScheme(phase)
                else rememberAppColorScheme(appearance, darkMode),
            modifier = Modifier.fillMaxSize(),
        ) {
            ThemeModeEffect(if (appearance.themeMode == UserThemeMode.CLOCK) {
                if (darkMode) ThemeMode.Dark else ThemeMode.Light
            } else appearance.themeMode.toUiThemeMode())
            FullScreenEffect(appearance.fullScreenMode)
            ScreensaverContainer(
                enabled = appearance.screensaverEnabled && isKeepScreenOnSupported,
                timeoutMinutes = appearance.screensaverTimeoutMinutes,
            ) {
                RootNavigation(closeApp)
            }
            val connectedToPower = rememberIsConnectedToPower()
            ProfileExecutionEffect(
                appearance.keepScreenOn && (!appearance.keepScreenOnOnlyWhileCharging || connectedToPower),
            )
            AutoFlushHost()
        }
    }
}
