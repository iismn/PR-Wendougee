package app.geeflow

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import app.geeflow.app.App
import app.geeflow.platform.LocalNativeSystemUi
import app.geeflow.platform.NativeSystemUi
import app.geeflow.presentation.feature.device.dashboard.DeviceDesignReviewContent
import app.geeflow.ui.theme.CoffeeAmbientColors
import app.geeflow.ui.theme.CoffeeDayPhase
import app.geeflow.ui.theme.coffeeClockAppearanceAt
import app.geeflow.ui.theme.GeeFlowTheme
import app.geeflow.ui.theme.LocalNativeCoffeeMaterials
import app.geeflow.ui.theme.NativeCoffeeMaterials
import app.geeflow.ui.theme.coffeeColorScheme
import app.geeflow.ui.components.NativeDeviceScanner
import app.geeflow.ui.components.LocalNativeDeviceScanner
import app.geeflow.presentation.feature.intro.IntroDesignPreview
import platform.posix.exit

/** Kept in the exported app framework so Swift does not depend on Compose types. */
interface NativeGlassHost : NativeDeviceScanner {
    fun setFullScreen(enabled: Boolean)
    fun setBackdropBlur(progress: Double)
    fun setPhase(phase: Int)
    fun setAmbientColors(base: Int, glow: Int, cool: Int)
    fun updateMaterial(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double)
    fun setMaterialPressed(key: String, pressed: Boolean)
    fun setMaterialOpacity(key: String, opacity: Double)
    fun setMaterialMask(key: String, path: String)
    fun removeMaterial(key: String)
}

@Suppress("FunctionNaming")
@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController(
    nativeGlassHost: NativeGlassHost,
    reviewScene: String = "",
    reviewNight: Boolean = false,
    reviewClockMinute: Int = -1,
) =
    ComposeUIViewController(configure = { opaque = false }) {
        val materials = remember(nativeGlassHost) {
            object : NativeCoffeeMaterials {
                override fun setPhase(phase: Int) = nativeGlassHost.setPhase(phase)
                override fun setAmbientColors(colors: CoffeeAmbientColors) =
                    nativeGlassHost.setAmbientColors(colors.base.toArgb(), colors.glow.toArgb(), colors.cool.toArgb())
                override fun backdropBlur(progress: Float) = nativeGlassHost.setBackdropBlur(progress.toDouble())
                override fun update(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double) =
                    nativeGlassHost.updateMaterial(key, x, y, width, height, radius)
                override fun press(key: String, pressed: Boolean) = nativeGlassHost.setMaterialPressed(key, pressed)
                override fun opacity(key: String, value: Float) = nativeGlassHost.setMaterialOpacity(key, value.toDouble())
                override fun mask(key: String, path: String) = nativeGlassHost.setMaterialMask(key, path)
                override fun remove(key: String) = nativeGlassHost.removeMaterial(key)
            }
        }
        val systemUi = remember(nativeGlassHost) {
            object : NativeSystemUi {
                override fun setFullScreen(enabled: Boolean) = nativeGlassHost.setFullScreen(enabled)
            }
        }
        CompositionLocalProvider(LocalNativeCoffeeMaterials provides materials, LocalNativeDeviceScanner provides nativeGlassHost,
            LocalNativeSystemUi provides systemUi) {
            if (reviewScene.isEmpty()) App { exit(0) }
            else {
                val clock = reviewClockMinute.takeIf { it in 0 until 24 * 60 }
                    ?.let { coffeeClockAppearanceAt(it.toFloat()) }
                val phase = clock?.phase ?: if (reviewNight) CoffeeDayPhase.Night else CoffeeDayPhase.Day
                GeeFlowTheme(darkMode = phase.dark, colorScheme = coffeeColorScheme(phase),
                    phase = phase, ambient = clock?.ambient) {
                    if (reviewScene == "keyboard") IntroDesignPreview(formShown = true)
                    else DeviceDesignReviewContent(reviewScene)
                }
            }
        }
}
