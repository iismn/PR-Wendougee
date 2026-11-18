package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.geeflow.data.user.model.HomeDockAction
import app.geeflow.presentation.feature.device.dashboard.components.BrewButtonState
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.ui.components.CoffeeMotionEasing
import app.geeflow.ui.components.CoffeeTooltip
import app.geeflow.ui.icons.*
import app.geeflow.ui.theme.coffeeGlass
import geeflow.shared.core.ui.generated.resources.*
import geeflow.shared.feature.device.dashboard.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt
import geeflow.shared.core.ui.generated.resources.Res as CoreRes
import geeflow.shared.feature.device.dashboard.generated.resources.Res

private data class DockItem(val action: HomeDockAction, val icon: ImageVector, val label: String,
    val enabled: Boolean = true, val selected: Boolean = false, val hero: Boolean = false,
    val danger: Boolean = false, val loading: Boolean = false, val onClick: () -> Unit)

/** One gesture owner separates taps from a long-press reorder; releasing a drag can never brew. */
@Composable
internal fun HomeDock(state: DeviceDashboardViewState, profiles: ProfileListViewState,
    onEvent: (DeviceDashboardEvent) -> Unit, onProfileEvent: (ProfileListEvent) -> Unit, onShowCharts: () -> Unit) {
    val idle = state.brewButtonState == BrewButtonState.Idle
    val brewing = state.brewButtonState == BrewButtonState.Brewing
    val brewLabel = stringResource(when (state.brewButtonState) {
        BrewButtonState.Brewing -> CoreRes.string.common_stop
        BrewButtonState.Syncing -> Res.string.device_dashboard_syncing
        BrewButtonState.Busy -> CoreRes.string.common_machine_busy
        BrewButtonState.Idle -> Res.string.home_profile_start
    })
    val items = buildList {
        add(DockItem(HomeDockAction.Profiles, Icons.Outlined.Apps, stringResource(Res.string.home_profiles),
            selected = !profiles.showHistory) { if (profiles.showHistory) onProfileEvent(ProfileListEvent.HistoryClicked) })
        add(DockItem(HomeDockAction.Charts, Icons.Outlined.ShowChart, stringResource(Res.string.dashboard_tab_details), onClick = onShowCharts))
        add(DockItem(HomeDockAction.History, Icons.Outlined.History, stringResource(Res.string.profile_list_show_history),
            selected = profiles.showHistory) { onProfileEvent(ProfileListEvent.HistoryClicked) })
        add(DockItem(HomeDockAction.FreeControl, GeeFlowIcon.FlowControl, stringResource(Res.string.free_control_screen_title)) {
            onEvent(DeviceDashboardEvent.FlowControlClicked) })
        add(DockItem(HomeDockAction.Flush, GeeFlowIcon.Flush, stringResource(CoreRes.string.common_flush), enabled = idle) {
            onEvent(DeviceDashboardEvent.ManualBrewClicked) })
        if (state.hotWaterSupported) add(DockItem(HomeDockAction.HotWater, Icons.Outlined.WaterDrop,
            stringResource(CoreRes.string.common_hot_water), enabled = idle) { onEvent(DeviceDashboardEvent.HotWaterClicked) })
        add(DockItem(HomeDockAction.Scale, Icons.Outlined.Settings,
            stringResource(CoreRes.string.common_settings)) { onEvent(DeviceDashboardEvent.SettingsClicked) })
        add(DockItem(HomeDockAction.Brew, if (brewing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, brewLabel,
            enabled = idle || brewing, hero = true, danger = brewing, loading = state.brewButtonState == BrewButtonState.Syncing) {
            onEvent(if (brewing) DeviceDashboardEvent.StopBrewClicked else DeviceDashboardEvent.BrewClicked) })
    }
    ReorderableDock(items, state.dockOrder, state.user.id, onOrderChanged = { onEvent(DeviceDashboardEvent.DockOrderChanged(it)) })
}

@Composable
private fun ReorderableDock(items: List<DockItem>, savedOrder: List<HomeDockAction>, userId: String,
    onOrderChanged: (List<HomeDockAction>) -> Unit) {
    val density = LocalDensity.current
    val step = with(density) { 45.dp.toPx() }
    val inset = with(density) { 4.dp.toPx() }
    val haptic = LocalHapticFeedback.current
    val available = items.map { it.action }.toSet()
    var localOrder by remember(userId) { mutableStateOf(HomeDockAction.normalize(savedOrder)) }
    var dragged by remember(userId) { mutableStateOf<HomeDockAction?>(null) }
    var pressed by remember(userId) { mutableStateOf<HomeDockAction?>(null) }
    var dragLeft by remember { mutableFloatStateOf(0f) }
    var tooltip by remember { mutableStateOf<HomeDockAction?>(null) }
    val currentItems by rememberUpdatedState(items)
    val currentOnOrderChanged by rememberUpdatedState(onOrderChanged)
    val currentOrder by rememberUpdatedState(localOrder)
    val slotPositions = remember(userId) { mutableMapOf<HomeDockAction, State<Float>>() }
    LaunchedEffect(savedOrder, userId) { if (dragged == null) localOrder = HomeDockAction.normalize(savedOrder) }
    LaunchedEffect(tooltip) { if (tooltip != null) { delay(2200); tooltip = null } }
    val visible = localOrder.filter { it in available }
    Box(Modifier.width((items.size * 45 - 3 + 8).dp).height(50.dp).testTag("profile-home-dock")
        .pointerInput(userId, available, step) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val orderAtDown = currentOrder
                val visibleAtDown = orderAtDown.filter { it in available }
                // While slots are crossing, a coordinate must not invoke the destination slot's
                // action rather than the icon still visible there (especially Flush or Brew).
                val settling = visibleAtDown.withIndex().any { (slot, id) ->
                    slotPositions[id]?.let { abs(it.value - slot * step) > .5f } ?: true
                }
                if (settling) {
                    down.consume()
                    do {
                        val changes = awaitPointerEvent().changes
                        changes.forEach { it.consume() }
                    } while (changes.any { it.id == down.id && it.pressed })
                    return@awaitEachGesture
                }
                val index = ((down.position.x - inset) / step).toInt().coerceIn(visibleAtDown.indices)
                val action = visibleAtDown[index]
                pressed = action
                tooltip = null
                down.consume()
                var released = false
                var longPress = false
                try {
                    var finishedWaiting = false
                    var cancelledTap = false
                    var stillDown = true
                    var tapped = false
                    withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                            if (change == null) { finishedWaiting = true; cancelledTap = true; break }
                            stillDown = change.pressed
                            if (change.isConsumed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                finishedWaiting = true; cancelledTap = true; change.consume(); break
                            }
                            if (!change.pressed) {
                                change.consume(); finishedWaiting = true; tapped = true; break
                            }
                        }
                    }
                    if (finishedWaiting) {
                        if (cancelledTap) {
                            pressed = null
                            // A short swipe is neither a tap nor a reorder, particularly over pump controls.
                            while (stillDown) {
                                val changes = awaitPointerEvent().changes
                                stillDown = changes.any { it.id == down.id && it.pressed }
                                changes.forEach { it.consume() }
                            }
                        } else if (tapped) {
                            currentItems.firstOrNull { it.action == action }?.let { if (it.enabled) it.onClick() }
                        }
                    } else {
                        longPress = true
                        dragged = action
                        dragLeft = index * step
                        tooltip = action
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val startX = down.position.x
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed) break
                            change.consume()
                            if (!change.pressed) {
                                released = true
                                if (moved && localOrder != orderAtDown) currentOnOrderChanged(localOrder)
                                break
                            }
                            val delta = change.position.x - startX
                            if (!moved && abs(delta) > viewConfiguration.touchSlop) { moved = true; tooltip = null }
                            if (moved) {
                                dragLeft = (index * step + delta).coerceIn(0f, (visibleAtDown.size - 1) * step)
                                val from = localOrder.filter { it in available }
                                val target = (dragLeft / step).roundToInt().coerceIn(from.indices)
                                val at = from.indexOf(action)
                                if (target != at) {
                                    val next = from.toMutableList().apply { removeAt(at); add(target, action) }
                                    localOrder = HomeDockAction.mergeVisible(localOrder, next)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    }
                } finally {
                    if (longPress && !released) { localOrder = orderAtDown; tooltip = null }
                    dragged = null
                    pressed = null
                }
            }
        }) {
        // Keep the UIKit glass geometry still while the icons move independently above it.
        Box(Modifier.matchParentSize().coffeeGlass(CircleShape))
        visible.forEachIndexed { index, action ->
            key(action) {
                val item = items.first { it.action == action }
                val held = dragged == action
                val position = animateFloatAsState(if (held) dragLeft else index * step,
                    tween(if (held) 0 else 240, easing = CoffeeMotionEasing), label = "dock-slot-${action.key}")
                val x by position
                SideEffect { slotPositions[action] = position }
                DisposableEffect(action) { onDispose { slotPositions.remove(action) } }
                val scale by animateFloatAsState(if (held) 1.14f else if (pressed == action) .94f else 1f,
                    tween(140, easing = CoffeeMotionEasing), label = "dock-lift-${action.key}")
                val lift by animateFloatAsState(if (held) -5f else 0f, tween(160), label = "dock-y-${action.key}")
                DockIcon(item, Modifier.offset { IntOffset((inset + x).roundToInt(), (inset + lift * density.density).roundToInt()) }
                    .zIndex(if (held) 1f else 0f)
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale
                        shape = CircleShape
                        shadowElevation = if (held) 8.dp.toPx() else 0f
                    }.testTag("home-dock-${action.key}"),
                    tooltip = tooltip == action,
                    onDismissTooltip = { tooltip = null },
                    onShowTooltip = { tooltip = action; haptic.performHapticFeedback(HapticFeedbackType.LongPress) })
            }
        }
    }
}

@Composable
private fun DockIcon(item: DockItem, modifier: Modifier, tooltip: Boolean,
    onDismissTooltip: () -> Unit, onShowTooltip: () -> Unit) {
    val background = when { item.danger -> MaterialTheme.colorScheme.error
        item.hero -> MaterialTheme.colorScheme.primary
        item.selected -> MaterialTheme.colorScheme.onSurface
        else -> Color.Transparent }
    val foreground = when { item.danger -> MaterialTheme.colorScheme.onError
        !item.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
        item.hero -> MaterialTheme.colorScheme.onPrimary
        item.selected -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.onSurface }
    Box(modifier.size(42.dp).background(background, CircleShape).semantics {
        role = Role.Button
        contentDescription = item.label
        if (!item.enabled) disabled()
        onClick { if (item.enabled) { onDismissTooltip(); item.onClick(); true } else false }
        onLongClick(item.label) { onShowTooltip(); true }
    }, contentAlignment = Alignment.Center) {
        if (item.loading) CircularProgressIndicator(Modifier.size(20.dp), color = foreground, strokeWidth = 2.dp)
        else Icon(item.icon, null, Modifier.size(20.dp), tint = foreground)
        CoffeeTooltip(tooltip, item.label, onDismissTooltip, Modifier.testTag("home-dock-tooltip"))
    }
}
