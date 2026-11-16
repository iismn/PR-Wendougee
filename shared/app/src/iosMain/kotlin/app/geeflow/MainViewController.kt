package app.geeflow

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import app.geeflow.app.App
import app.geeflow.presentation.feature.device.dashboard.DeviceDesignReviewContent
import app.geeflow.ui.theme.CoffeeDayPhase
import app.geeflow.ui.theme.GeeFlowTheme
import app.geeflow.ui.theme.LocalNativeCoffeeMaterials
import app.geeflow.ui.theme.NativeCoffeeMaterials
import app.geeflow.ui.theme.coffeeColorScheme
import platform.posix.exit

/** Kept in the exported app framework so Swift does not depend on Compose types. */
interface NativeGlassHost {
    fun setPhase(phase: Int)
    fun updateMaterial(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double)
    fun setMaterialPressed(key: String, pressed: Boolean)
    fun removeMaterial(key: String)
}

@Suppress("FunctionNaming")
fun MainViewController(nativeGlassHost: NativeGlassHost, reviewScene: String = "", reviewNight: Boolean = false) =
    ComposeUIViewController(configure = { opaque = false }) {
        val materials = remember(nativeGlassHost) {
            object : NativeCoffeeMaterials {
                override fun setPhase(phase: Int) = nativeGlassHost.setPhase(phase)
                override fun update(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double) =
                    nativeGlassHost.updateMaterial(key, x, y, width, height, radius)
                override fun press(key: String, pressed: Boolean) = nativeGlassHost.setMaterialPressed(key, pressed)
                override fun remove(key: String) = nativeGlassHost.removeMaterial(key)
            }
        }
        CompositionLocalProvider(LocalNativeCoffeeMaterials provides materials) {
            if (reviewScene.isEmpty()) App { exit(0) }
            else {
                val phase = if (reviewNight) CoffeeDayPhase.Night else CoffeeDayPhase.Day
                GeeFlowTheme(darkMode = phase.dark, colorScheme = coffeeColorScheme(phase), phase = phase) {
                    DeviceDesignReviewContent(reviewScene)
                }
            }
        }
}
