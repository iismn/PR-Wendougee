package app.geeflow.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.input.PlatformImeOptions

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun platformKeyboardOptions(options: KeyboardOptions): KeyboardOptions = options.copy(
    showKeyboardOnFocus = true,
    platformImeOptions = options.platformImeOptions ?: PlatformImeOptions { usingNativeTextInput(true) },
)
