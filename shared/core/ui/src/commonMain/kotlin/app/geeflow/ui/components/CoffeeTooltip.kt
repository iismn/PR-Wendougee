package app.geeflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun CoffeeTooltip(
    visible: Boolean,
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopCenter,
    offset: IntOffset = IntOffset(0, with(LocalDensity.current) { (-48).dp.roundToPx() }),
) {
    val state = remember { MutableTransitionState(false) }
    var retainedText by remember { mutableStateOf(text) }
    SideEffect { if (visible) retainedText = text }
    LaunchedEffect(visible) { state.targetState = visible }
    if (state.currentState || state.targetState) Popup(
        alignment = alignment, offset = offset, onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        CompositionLocalProvider(LocalDensity provides (LocalAppTextDensity.current ?: LocalDensity.current)) {
            AnimatedVisibility(state, enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = .96f), exit = fadeOut(tween(100))) {
                Surface(modifier.semantics(mergeDescendants = true) {}, shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 4.dp) {
                    Text(if (visible) text else retainedText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
        }
    }
}
