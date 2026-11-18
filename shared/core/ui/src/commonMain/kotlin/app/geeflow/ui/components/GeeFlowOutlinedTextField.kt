package app.geeflow.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun GeeFlowOutlinedTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    autoFocus: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(autoFocus) {
        if (autoFocus && enabled) {
            withFrameNanos { }
            focusRequester.requestFocus()
        }
    }
    LaunchedEffect(focused) {
        if (focused) {
            // Let the dialog's text input session attach before asking UIKit to show IME.
            withFrameNanos { }
            keyboard?.show()
        }
    }
    OutlinedTextField(
    lineLimits = lineLimits,
    state = state,
    modifier = modifier.focusRequester(focusRequester).onFocusChanged { focused = it.isFocused },
    label = label,
    enabled = enabled,
    contentPadding = contentPadding,
    shape = shape,
    keyboardOptions = platformKeyboardOptions(keyboardOptions),
    onKeyboardAction = onKeyboardAction,
)
}

internal expect fun platformKeyboardOptions(options: KeyboardOptions): KeyboardOptions
