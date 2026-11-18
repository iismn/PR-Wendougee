package app.geeflow.ui.components

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun platformModalProperties(properties: DialogProperties, managedTransition: Boolean): DialogProperties = DialogProperties(
    dismissOnBackPress = properties.dismissOnBackPress,
    dismissOnClickOutside = properties.dismissOnClickOutside,
    usePlatformDefaultWidth = properties.usePlatformDefaultWidth,
    usePlatformInsets = properties.usePlatformInsets,
    useSoftwareKeyboardInset = properties.useSoftwareKeyboardInset,
    scrimColor = Color.Black.copy(alpha = 0.20f),
    animateTransition = !managedTransition,
)
