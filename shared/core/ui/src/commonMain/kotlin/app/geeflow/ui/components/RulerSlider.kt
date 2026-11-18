package app.geeflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.ceil
import kotlin.math.floor

/** A physical ruler moves under a fixed centre line: dragging left selects a higher value. */
@Composable
fun RulerSlider(
    value: Int,
    range: IntRange,
    label: String,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val validRange = !range.isEmpty() && range.last > range.first
    val bounds = if (validRange) range else 0..1
    val selected = value.coerceIn(bounds)
    val interactive = enabled && validRange
    val spacing = with(LocalDensity.current) { (if (bounds.last - bounds.first <= 20) 20.dp else 8.dp).toPx() }
    var position by remember { mutableFloatStateOf(selected.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }
    val illuminated = interactive && (touched || dragging)
    val glow by animateFloatAsState(if (illuminated) 1f else 0f,
        tween(if (illuminated) 100 else 420), label = "ruler-contact-glow")
    val currentBounds by rememberUpdatedState(bounds)
    val currentSelected by rememberUpdatedState(selected)
    val currentInteractive by rememberUpdatedState(interactive)
    val currentSpacing by rememberUpdatedState(spacing)
    val currentOnChange by rememberUpdatedState(onChange)
    LaunchedEffect(selected, bounds, interactive) {
        if (!dragging) position = selected.toFloat()
    }
    val dragState = rememberDraggableState { delta ->
        if (currentInteractive) {
            val next = (position - delta / currentSpacing).coerceIn(currentBounds.first.toFloat(), currentBounds.last.toFloat())
            position = next
            val rounded = next.roundToInt()
            if (rounded != currentSelected) currentOnChange(rounded)
        }
    }
    val ink = MaterialTheme.colorScheme.onSurface
    val glowColor = MaterialTheme.colorScheme.primary
    Canvas(modifier.height(64.dp).clipToBounds()
        .progressSemantics(selected.toFloat(), bounds.first.toFloat()..bounds.last.toFloat(), bounds.last - bounds.first - 1)
        .semantics {
            contentDescription = label
            if (!interactive) disabled()
            setProgress { requested ->
                if (!interactive || !requested.isFinite()) false
                else {
                    val next = requested.roundToInt().coerceIn(bounds)
                    position = next.toFloat()
                    onChange(next)
                    true
                }
            }
        }
        .focusable(interactive)
        .pointerInput(interactive) {
            if (!interactive) return@pointerInput
            try {
                awaitPointerEventScope {
                    while (true) {
                        val changes = awaitPointerEvent(PointerEventPass.Initial).changes
                        val contact = changes.firstOrNull { it.pressed }
                        touched = contact != null
                    }
                }
            } finally { touched = false }
        }
        .draggable(
            state = dragState, orientation = Orientation.Horizontal, enabled = interactive,
            onDragStarted = { position = currentSelected.toFloat(); dragging = true },
            onDragStopped = { position = position.roundToInt().toFloat(); dragging = false },
        ),
    ) {
        val visibleValue = if (dragging) position else selected.toFloat()
        // Continue drawing beyond the selectable limits, like one uninterrupted ruler.
        val first = floor(visibleValue - size.width / (2f * spacing)).toInt()
        val last = ceil(visibleValue + size.width / (2f * spacing)).toInt()
        val baseline = size.height * .73f
        for (tick in first..last) {
            val x = center.x + (tick - visibleValue) * spacing
            val inRange = tick in bounds
            val limit = tick == bounds.first || tick == bounds.last
            val major = tick % 5 == 0 || limit
            val height = (if (major) 24.dp else if (tick % 2 == 0) 16.dp else 10.dp).toPx()
            val edgeFade = (minOf(x, size.width - x) / 20.dp.toPx()).coerceIn(0f, 1f)
            val alpha = (if (!interactive) .12f else if (!inRange) { if (major) .15f else .09f }
                else if (limit) .90f else if (major) .58f else .38f) * edgeFade
            val from = Offset(x, baseline - height)
            val to = Offset(x, baseline)
            if (glow > .001f) {
                val strength = glow * edgeFade * if (inRange) 1f else .25f
                drawLine(glowColor.copy(alpha = .09f * strength), from, to, 8.dp.toPx(), StrokeCap.Round)
                drawLine(glowColor.copy(alpha = .22f * strength), from, to, 3.dp.toPx(), StrokeCap.Round)
            }
            drawLine(ink.copy(alpha = (alpha + if (inRange) .24f * glow * edgeFade else 0f).coerceAtMost(1f)),
                from, to, (if (limit) 1.5.dp else 1.dp).toPx(), StrokeCap.Round)
        }
        if (glow > .001f) drawLine(glowColor.copy(alpha = .20f * glow),
            Offset(center.x, baseline - 36.dp.toPx()), Offset(center.x, baseline + 4.dp.toPx()), 8.dp.toPx(), StrokeCap.Round)
        drawLine(ink.copy(alpha = if (interactive) .95f else .3f),
            Offset(center.x, baseline - 36.dp.toPx()), Offset(center.x, baseline + 4.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
    }
}
