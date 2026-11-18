package app.geeflow.platform

import androidx.compose.runtime.staticCompositionLocalOf

/** The iOS root owns status-bar visibility; Compose only forwards the saved preference. */
interface NativeSystemUi {
    fun setFullScreen(enabled: Boolean)
}

val LocalNativeSystemUi = staticCompositionLocalOf<NativeSystemUi?> { null }
