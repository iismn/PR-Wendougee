package app.geeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal class DialogCapture {
    val dialogs = mutableStateMapOf<Any, @Composable () -> Unit>()
}

internal val LocalDialogCapture = staticCompositionLocalOf<DialogCapture?> { null }

/** Review-only host: renders the production dialog body in the offscreen Compose scene.
 * Normal app dialogs still use the platform window, focus and dismiss behavior. */
@Composable
fun DialogCaptureHost(content: @Composable () -> Unit) {
    val capture = remember { DialogCapture() }
    CompositionLocalProvider(LocalDialogCapture provides capture) {
        Box(Modifier.fillMaxSize()) {
            content()
            capture.dialogs.values.lastOrNull()?.let { dialog ->
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f)), contentAlignment = Alignment.Center) {
                    Box(Modifier.padding(24.dp).widthIn(max = 480.dp)) { dialog() }
                }
            }
        }
    }
}

@Composable
internal fun CaptureDialog(capture: DialogCapture, content: @Composable () -> Unit) {
    val id = remember { Any() }
    val context = currentCompositionLocalContext
    val current by rememberUpdatedState(content)
    DisposableEffect(id) {
        capture.dialogs[id] = { CompositionLocalProvider(context) { current() } }
        onDispose { capture.dialogs.remove(id) }
    }
}
