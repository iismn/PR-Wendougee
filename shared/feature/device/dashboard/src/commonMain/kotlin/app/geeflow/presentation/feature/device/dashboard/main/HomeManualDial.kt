package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import app.geeflow.ui.components.CoffeeMotionEasing
import app.geeflow.ui.components.DotMatrixNumber
import app.geeflow.ui.theme.NativeMaterialShape
import app.geeflow.ui.theme.coffeeGlass
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.shadow.Shadow
import geeflow.shared.core.ui.generated.resources.common_stop
import geeflow.shared.feature.device.dashboard.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.*
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun HomeManualDial(state: HomeManualState, seconds: Int, accent: Color,
    onEvent: (DeviceDashboardEvent) -> Unit, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val modeLabel = stringResource(if (state.mode == ManualPriority.Pressure) Res.string.home_pressure_priority else Res.string.home_flow_priority)
    val title = stringResource(if (state.mode == ManualPriority.Pressure) Res.string.home_manual_pressure else Res.string.home_manual_flow)
    val unit = if (state.mode == ManualPriority.Pressure) "bar" else "ml/s"
    val value = (round(state.target * 10) / 10f).toString()
    val enabled by rememberUpdatedState(state.canTurn)
    val current by rememberUpdatedState(state)
    val event by rememberUpdatedState(onEvent)
    var touched by remember { mutableStateOf(false) }
    val glow by animateFloatAsState(if (touched && state.canTurn) 1f else 0f,
        tween(if (touched) 100 else 440), label = "manual-dial-contact")
    val glowColor = MaterialTheme.colorScheme.primary
    BoxWithConstraints(modifier) {
        val diameter = minOf(maxWidth * .72f, maxHeight * .86f)
        val radius = diameter / 2
        val left = ((maxWidth - diameter - 72.dp) / 2).coerceAtLeast(0.dp)
        val top = (maxHeight - diameter) / 2
        val fraction = ((state.target - state.range.minimum) / (state.range.maximum - state.range.minimum)).coerceIn(0f, 1f)
        Box(Modifier.offset(left, top).size(diameter).coffeeGlass(CircleShape)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = .38f), accent.copy(alpha = .10f))), CircleShape)) {
            Canvas(Modifier.fillMaxSize().testTag("home-manual-dial")
                .progressSemantics(state.target, state.range.minimum..state.range.maximum)
                .semantics {
                    contentDescription = modeLabel
                    stateDescription = "$value $unit"
                    if (!state.canTurn) disabled()
                    setProgress { requested ->
                        if (!enabled || !requested.isFinite()) false
                        else { event(DeviceDashboardEvent.DialTargetChanged(requested.coerceIn(current.range.minimum, current.range.maximum))); true }
                    }
                }.focusable(state.canTurn)
                .pointerInput(state.canTurn) {
                    if (!state.canTurn) return@pointerInput
                    try {
                        awaitPointerEventScope {
                            while (true) touched = awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed }
                        }
                    } finally { touched = false }
                }
                .pointerInput(state.mode) {
                    var position = 0f
                    var accepted = false
                    detectDragGestures(onDragStart = { point ->
                        val delta = point - Offset(size.width / 2f, size.height / 2f)
                        accepted = enabled && delta.getDistance() > size.width * .24f
                        position = current.target
                    }, onDrag = { change, movement ->
                        if (accepted && enabled) {
                            change.consume()
                            val delta = change.position - Offset(size.width / 2f, size.height / 2f)
                            val previous = delta - movement
                            val next = atan2(delta.y, delta.x)
                            var rotation = next - atan2(previous.y, previous.x)
                            while (rotation > PI) rotation -= (2 * PI).toFloat()
                            while (rotation < -PI) rotation += (2 * PI).toFloat()
                            position = (position + rotation / (1.5f * PI.toFloat()) * (current.range.maximum - current.range.minimum))
                                .coerceIn(current.range.minimum, current.range.maximum)
                            event(DeviceDashboardEvent.DialTargetChanged(position))
                        }
                    })
                }) {
                val r = size.minDimension / 2 - 12.dp.toPx()
                for (i in 0..108) {
                    val a = (135f + i * 2.5f) * PI / 180
                    val direction = Offset(cos(a).toFloat(), sin(a).toFloat())
                    val from = center + direction * r
                    val to = center + direction * (r - if (i % 9 == 0) 8.dp.toPx() else 3.dp.toPx())
                    if (glow > .001f) {
                        drawLine(glowColor.copy(alpha = .10f * glow), from, to, 7.dp.toPx(), StrokeCap.Round)
                        drawLine(glowColor.copy(alpha = .24f * glow), from, to, 3.dp.toPx(), StrokeCap.Round)
                    }
                    drawLine(ink.copy(alpha = (if (i % 9 == 0) .56f else .25f) + .30f * glow),
                        from, to, .8.dp.toPx(), StrokeCap.Round)
                }
                val a = (135 + 270 * fraction) * PI / 180
                val direction = Offset(cos(a).toFloat(), sin(a).toFloat())
                drawLine(ink.copy(alpha = .76f), center + direction * (r * .40f), center + direction * (r - 13.dp.toPx()),
                    1.2.dp.toPx(), StrokeCap.Round)
                drawCircle(ink, 3.dp.toPx(), center + direction * (r - 2.dp.toPx()))
            }
            ManualDialReadout(value, unit, title, if (state.active) "$seconds s" else null,
                ink, Modifier.matchParentSize())
            ManualTransportButton(state, onEvent,
                Modifier.align(Alignment.BottomCenter).padding(bottom = diameter * .14f))
        }
        ModeRail(state, radius, left + radius, top + radius, accent, onEvent)
    }
}

/** Only the transport glyph is visible; the 48dp target never paints a button container. */
@Composable
internal fun ManualTransportButton(state: HomeManualState, onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier) {
    val stopping = state.active || state.operation != ManualOperation.None
    val enabled = if (stopping) state.connected && state.operation != ManualOperation.Stopping else state.canStart
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .91f else 1f,
        tween(if (pressed) 90 else 180, easing = CoffeeMotionEasing), label = "manual-transport-press")
    val stopPresence by animateFloatAsState(if (stopping) 1f else 0f,
        tween(220, easing = CoffeeMotionEasing), label = "manual-transport-state")
    val ink = MaterialTheme.colorScheme.onSurface
    val label = stringResource(if (stopping) CoreRes.string.common_stop else Res.string.home_manual_start)
    Box(modifier.size(48.dp).testTag("home-manual-start-stop")
        .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
        .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button) {
            onEvent(if (stopping) DeviceDashboardEvent.DialStopClicked else DeviceDashboardEvent.DialStartClicked)
        }.semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.PlayArrow, null,
            modifier = Modifier.size(32.dp).testTag("home-manual-play-glyph").graphicsLayer {
                alpha = 1f - stopPresence
                scaleX = 1f - .12f * stopPresence; scaleY = scaleX
            }, tint = ink.copy(alpha = if (enabled) .95f else .38f))
        Canvas(Modifier.matchParentSize().testTag("home-manual-stop-glow").graphicsLayer {
            val radius = 5.5.dp.toPx()
            renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
        }) {
            if (stopPresence > .001f) {
                val edge = 24.dp.toPx() * (.9f + .1f * stopPresence)
                drawRoundRect(Color(0xFFFF4B55).copy(alpha = .66f * stopPresence),
                    topLeft = center - Offset(edge / 2, edge / 2), size = Size(edge, edge),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            }
        }
        Canvas(Modifier.matchParentSize().testTag("home-manual-stop-glyph")) {
            val strength = stopPresence
            if (strength > .001f) {
                val red = Color(0xFFFF4B55)
                val edge = 21.dp.toPx() * (.9f + .1f * strength)
                // Keep the core crisp above its separately blurred light, with no button container.
                drawRoundRect(red.copy(alpha = strength), topLeft = center - Offset(edge / 2, edge / 2),
                    size = Size(edge, edge), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()))
            }
        }
    }
}

/** The numeral is centred independently; neither its unit nor either label shifts its origin. */
@Composable
private fun ManualDialReadout(value: String, unit: String, title: String, timer: String?, ink: Color,
    modifier: Modifier = Modifier) {
    Layout(modifier = modifier, content = {
        DotMatrixNumber(value, ink, Modifier.height(54.dp).testTag("home-manual-number"),
            glowColor = if (ink.luminance() < .5f) Color(0xFF8EDFF5) else ink)
        Text(unit, style = MaterialTheme.typography.bodyMedium, color = ink.copy(alpha = .72f),
            modifier = Modifier.testTag("home-manual-unit"))
        Text(title, style = MaterialTheme.typography.labelLarge, color = ink.copy(alpha = .72f),
            modifier = Modifier.testTag("home-manual-title"))
        if (timer != null) Text(timer, style = MaterialTheme.typography.bodySmall)
    }) { measurables, constraints ->
        val children = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val number = children[0]
        val x = (constraints.maxWidth - number.width) / 2
        val y = (constraints.maxHeight - number.height) / 2
        layout(constraints.maxWidth, constraints.maxHeight) {
            number.placeRelative(x, y)
            children[1].placeRelative(x + number.width + 9.dp.roundToPx(), y + number.height - children[1].height)
            children[2].placeRelative((constraints.maxWidth - children[2].width) / 2, y - children[2].height - 18.dp.roundToPx())
            if (timer != null) children[3].placeRelative((constraints.maxWidth - children[3].width) / 2, y + number.height + 18.dp.roundToPx())
        }
    }
}

@Composable
private fun ModeRail(state: HomeManualState, radius: Dp, cx: Dp, cy: Dp, accent: Color,
    onEvent: (DeviceDashboardEvent) -> Unit) {
    val density = LocalDensity.current
    val r = with(density) { radius.toPx() }
    fun Float.dpPx() = with(density) { dp.toPx() }
    val inner = r + 12f.dpPx()
    val outer = r + 86f.dpPx()
    val geometry = remember(inner, outer) { ManualModeRailGeometry(inner, outer) }
    var draggedAngle by remember { mutableStateOf<Float?>(null) }
    val selectedAngle by animateFloatAsState(draggedAngle ?: if (state.mode == ManualPriority.Pressure) geometry.pressureAngle else geometry.flowAngle,
        tween(if (draggedAngle != null) 0 else 340, easing = CoffeeMotionEasing), label = "manual-priority-lens")
    val currentEvent by rememberUpdatedState(onEvent)
    val canSelect by rememberUpdatedState(state.canSelectMode)
    val supportsFlow by rememberUpdatedState(state.flowSupported)
    val currentAngle by rememberUpdatedState(selectedAngle)
    val railBackdrop = rememberLayerBackdrop()
    val rail = remember(r) { DialSector(r - 8f.dpPx(), r + 142f.dpPx(), -55f, 110f, 0f) }
    val railBounds = rail.bounds
    val dark = MaterialTheme.colorScheme.surface.luminance() < .1f
    Box(Modifier.offset(cx + with(density) { railBounds.left.toDp() }, cy + with(density) { railBounds.top.toDp() })
        .size(with(density) { railBounds.width.toDp() }, with(density) { railBounds.height.toDp() })
        .clip(rail)
        .pointerInput(geometry) {
            var startPointerAngle = 0f
            var startSelectionAngle = 0f
            detectDragGestures(orientationLock = null, onDragStart = { down, _, _ ->
                if (canSelect) {
                    val p = down.position + railBounds.topLeft
                    startPointerAngle = geometry.pointerAngleAtPoint(p.x, p.y)
                    startSelectionAngle = currentAngle
                    draggedAngle = startSelectionAngle
                }
            },
                onDragCancel = { draggedAngle = null },
                onDragEnd = { _ ->
                    draggedAngle?.let { angle ->
                        if (canSelect) currentEvent(DeviceDashboardEvent.DialModeChanged(if (angle > 0 && supportsFlow) ManualPriority.Flow else ManualPriority.Pressure))
                    }
                    draggedAngle = null
                }, onDrag = { change, _ ->
                    if (canSelect) {
                        change.consume()
                        val point = change.position + railBounds.topLeft
                        draggedAngle = if (supportsFlow) geometry.dragAngle(startSelectionAngle, startPointerAngle,
                            geometry.pointerAngleAtPoint(point.x, point.y)) else geometry.pressureAngle
                    }
                })
        }) {
        val origin = -railBounds.topLeft
        val track = remember(geometry) { DialSector(inner, outer, geometry.trackStart, geometry.trackSweep, 14f.dpPx()) }
        val trackPath = remember(track) { track.createOutline(Size.Zero, LayoutDirection.Ltr, density).path }
        val lens = remember(geometry, selectedAngle) {
            DialSector(geometry.selectedInner, geometry.selectedOuter, geometry.selectionStart(selectedAngle),
                geometry.selectionSweep, 14f.dpPx() + geometry.selectedOffset)
        }
        val b = lens.bounds
        val lensInRail = remember(lens, origin) { Path().apply {
            addPath(lens.createOutline(Size.Zero, LayoutDirection.Ltr, density).path, origin + b.topLeft)
        } }
        // Capture the full rail, but only display its crisp edges outside the frosted half-arc.
        Canvas(Modifier.matchParentSize().drawWithContent {
            clipPath(lensInRail, clipOp = ClipOp.Difference) { this@drawWithContent.drawContent() }
        }.layerBackdrop(railBackdrop)) {
            translate(origin.x + track.bounds.left, origin.y + track.bounds.top) {
                drawPath(trackPath, accent.copy(alpha = if (dark) .42f else .32f))
            }
        }
        Box(Modifier.offset(with(density) { (origin.x + b.left).toDp() }, with(density) { (origin.y + b.top).toDp() })
            .size(with(density) { b.width.toDp() }, with(density) { b.height.toDp() })
            .drawBackdrop(backdrop = railBackdrop, shape = { lens }, effects = {
                blur(9.dp.toPx())
            }, highlight = null, shadow = {
                Shadow(radius = 7.dp, color = Color.Black.copy(alpha = if (dark) .08f else .05f))
            }, onDrawSurface = {
                // The same rail contour, reduced to one half and offset outward by five percent.
                drawRect(accent.copy(alpha = if (dark) .16f else .11f))
                drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) .12f else .22f),
                    Color.White.copy(alpha = if (dark) .06f else .12f))))
            })
            .testTag("home-mode-lens"))
        listOf(ManualPriority.Pressure, ManualPriority.Flow).forEach { mode ->
            val label = stringResource(if (mode == ManualPriority.Pressure) Res.string.home_pressure_priority else Res.string.home_flow_priority)
            val selected = state.mode == mode
            val sector = remember(r, mode) { DialSector(inner - 8f.dpPx(), r + 130f.dpPx(),
                if (mode == ManualPriority.Pressure) -44f else 0f, 44f, 10f.dpPx()) }
            val bounds = sector.bounds
            Box(Modifier.offset(with(density) { (origin.x + bounds.left).toDp() }, with(density) { (origin.y + bounds.top).toDp() })
                .size(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() })
                .clip(sector)
                .clickable(enabled = state.canSelectMode && (mode != ManualPriority.Flow || state.flowSupported),
                    interactionSource = remember { MutableInteractionSource() }, indication = null) { onEvent(DeviceDashboardEvent.DialModeChanged(mode)) }
                .testTag(if (mode == ManualPriority.Pressure) "home-mode-pressure" else "home-mode-flow")
                .semantics { this.selected = selected; contentDescription = label })
            val a = (if (mode == ManualPriority.Pressure) geometry.pressureAngle else geometry.flowAngle) * PI.toFloat() / 180
            val iconR = geometry.midpointRadius
            val iconSize by animateFloatAsState(if (selected) 26f else 22f, tween(300, easing = CoffeeMotionEasing), label = "priority-icon-size")
            Icon(if (mode == ManualPriority.Pressure) Icons.Rounded.Speed else Icons.Rounded.WaterDrop, null,
                modifier = Modifier.offset(with(density) { (origin.x + cos(a) * iconR).toDp() } - (iconSize / 2).dp,
                    with(density) { (origin.y + sin(a) * iconR).toDp() } - (iconSize / 2).dp)
                    .size(iconSize.dp).testTag(if (mode == ManualPriority.Pressure) "home-mode-pressure-icon" else "home-mode-flow-icon"),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) .98f else .72f))
        }
    }
}

/** A rounded annular sector; both Compose clipping and UIKit blur use this exact outline. */
internal class DialSector(private val inner: Float, private val outer: Float, private val start: Float,
    private val sweep: Float, private val corner: Float) : NativeMaterialShape {
    private fun point(radius: Float, angle: Float): Offset {
        val a = angle * PI / 180
        return Offset(cos(a).toFloat() * radius, sin(a).toFloat() * radius)
    }
    private val samples = (0..24).flatMap { i -> listOf(point(inner, start + sweep * i / 24), point(outer, start + sweep * i / 24)) }
    val bounds = Rect(samples.minOf { it.x }, samples.minOf { it.y }, samples.maxOf { it.x }, samples.maxOf { it.y })
    private fun outline(scale: Float = 1f): Pair<Path, String> {
        val path = Path()
        val commands = mutableListOf<String>()
        fun local(p: Offset) = p - bounds.topLeft
        fun move(p: Offset) { val v = local(p); path.moveTo(v.x,v.y); commands += "M,${v.x / scale},${v.y / scale}" }
        fun line(p: Offset) { val v = local(p); path.lineTo(v.x,v.y); commands += "L,${v.x / scale},${v.y / scale}" }
        fun quad(control: Offset, end: Offset) {
            val c = local(control); val e = local(end)
            path.quadraticBezierTo(c.x,c.y,e.x,e.y)
            commands += "Q,${c.x / scale},${c.y / scale},${e.x / scale},${e.y / scale}"
        }
        val end = start + sweep
        val a = corner / outer * 180f / PI.toFloat()
        val b = corner / inner * 180f / PI.toFloat()
        move(point(outer, start + a))
        for (i in 1..24) line(point(outer, start + a + (sweep - 2*a) * i / 24))
        quad(point(outer,end), point(outer-corner,end))
        line(point(inner+corner,end))
        quad(point(inner,end), point(inner,end-b))
        for (i in 1..24) line(point(inner,end-b-(sweep-2*b)*i/24))
        quad(point(inner,start), point(inner+corner,start))
        line(point(outer-corner,start))
        quad(point(outer,start), point(outer,start+a))
        path.close(); commands += "Z"
        return path to commands.joinToString(";")
    }
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Generic(outline().first)
    override fun nativeMask(size: Size, density: Density): String = outline(density.density).second
}
