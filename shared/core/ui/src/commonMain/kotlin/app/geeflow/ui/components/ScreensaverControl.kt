package app.geeflow.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/** The app owns the full-screen clock; settings can request it without navigating away. */
val LocalShowScreensaver = staticCompositionLocalOf<(() -> Unit)?> { null }
