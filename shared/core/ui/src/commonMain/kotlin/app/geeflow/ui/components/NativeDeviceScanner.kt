package app.geeflow.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/** A native camera is presented above Compose, so glass and animation cannot cover its preview. */
interface NativeDeviceScanner {
    fun scanDeviceCode(title: String, closeTitle: String, onScanned: (String) -> Unit, onClosed: () -> Unit, onError: (String) -> Unit)
}

val LocalNativeDeviceScanner = staticCompositionLocalOf<NativeDeviceScanner?> { null }
