package app.geeflow.ui.components

import androidx.compose.foundation.text.KeyboardOptions

internal actual fun platformKeyboardOptions(options: KeyboardOptions): KeyboardOptions = options.copy(showKeyboardOnFocus = true)
