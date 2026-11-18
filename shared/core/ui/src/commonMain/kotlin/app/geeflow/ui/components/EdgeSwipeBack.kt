package app.geeflow.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Only claims horizontal drags that start at the left edge; lists and sliders keep their input. */
@Composable
fun EdgeSwipeBack(
    enabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeStarted: () -> Unit = {},
    onSwipeProgress: (Float) -> Unit = {},
    onSwipeCancelled: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val currentBack by rememberUpdatedState(onBack)
    val currentStart by rememberUpdatedState(onSwipeStarted)
    val currentProgress by rememberUpdatedState(onSwipeProgress)
    val currentCancel by rememberUpdatedState(onSwipeCancelled)
    Box(modifier.pointerInput(enabled) {
        if (!enabled) return@pointerInput
        val edge = 28.dp.toPx()
        val threshold = minOf(size.width * .16f, 120.dp.toPx()).coerceAtLeast(64.dp.toPx())
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.position.x > edge) return@awaitEachGesture
            var claimed = false
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val delta = change.position - down.position
                    if (!claimed) {
                        if (abs(delta.y) > viewConfiguration.touchSlop && abs(delta.y) >= abs(delta.x)) break
                        if (delta.x < -viewConfiguration.touchSlop) break
                        if (delta.x > viewConfiguration.touchSlop && delta.x > abs(delta.y)) {
                            claimed = true
                            currentStart()
                        }
                    }
                    if (claimed) {
                        currentProgress((delta.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                        change.consume()
                    }
                    if (!change.pressed) {
                        if (claimed && delta.x >= threshold) {
                            claimed = false
                            currentBack()
                        }
                        break
                    }
                }
            } finally {
                if (claimed) currentCancel()
            }
        }
    }) { content() }
}
