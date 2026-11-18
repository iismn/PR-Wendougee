package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.presentation.feature.device.dashboard.components.*
import app.geeflow.presentation.feature.device.dashboard.model.BrewChartModel
import app.geeflow.presentation.feature.device.dashboard.model.chartModel
import app.geeflow.presentation.feature.device.dashboard.profiles.*
import app.geeflow.ui.modifier.geeFlowInsetsPadding
import app.geeflow.ui.theme.coffeeGlass
import app.geeflow.ui.theme.coffeeSurface
import app.geeflow.ui.theme.NativeMaterialFade
import app.geeflow.ui.animations.NavigationEnterMillis
import app.geeflow.ui.animations.NavigationExitMillis
import app.geeflow.ui.components.CoffeeMotionEasing
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.CoffeeTooltip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.delay
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Flush
import app.geeflow.ui.icons.Logo
import app.geeflow.ui.icons.FlowControl
import geeflow.shared.core.ui.generated.resources.common_cancel
import geeflow.shared.core.ui.generated.resources.common_close
import geeflow.shared.core.ui.generated.resources.common_delete
import geeflow.shared.core.ui.generated.resources.common_flush
import geeflow.shared.core.ui.generated.resources.common_hot_water
import geeflow.shared.core.ui.generated.resources.common_start
import geeflow.shared.core.ui.generated.resources.common_stop
import geeflow.shared.core.ui.generated.resources.common_machine_busy
import geeflow.shared.feature.device.dashboard.generated.resources.*
import geeflow.shared.core.ui.generated.resources.common_brew_boiler
import geeflow.shared.core.ui.generated.resources.common_steam_boiler
import geeflow.shared.core.ui.generated.resources.common_pressure
import org.jetbrains.compose.resources.stringResource
import kotlin.math.*
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

/** The profile overview uses the existing dashboard and profile events, including device guards. */
@Composable
internal fun ProfileHome(
    state: DeviceDashboardViewState,
    profiles: ProfileListViewState,
    snackbar: SnackbarHostState,
    onEvent: (DeviceDashboardEvent) -> Unit,
    onProfileEvent: (ProfileListEvent) -> Unit,
    onShowCharts: () -> Unit,
) {
    val selected = profiles.profiles.firstOrNull { it.selected }
    val model = state.chartModel(profiles)
    val point = state.brew.data.maxByOrNull { it.key }?.value
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.1f
    val accent = if (dark) Color(0xFF7C9DBF) else Color(0xFF71877E)
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val deleteProfile = profiles.profiles.firstOrNull { it.id == pendingDelete }
    if (deleteProfile != null) GeeFlowDialog(onDismissRequest = { pendingDelete = null }) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(Res.string.profile_list_delete), style = MaterialTheme.typography.titleLarge)
            Text(deleteProfile.name, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(if (deleteProfile.bound) Res.string.home_delete_bound else Res.string.home_delete_confirmation))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(CoreRes.string.common_cancel)) }
                TextButton(enabled = !deleteProfile.bound, modifier = Modifier.testTag("profile-delete-confirm"), onClick = {
                    onProfileEvent(ProfileListEvent.RemoveProfileClicked(deleteProfile.id))
                    pendingDelete = null
                }) { Text(stringResource(CoreRes.string.common_delete), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    Box(Modifier.fillMaxSize().geeFlowInsetsPadding()) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            val leftWidth = (maxWidth * .16f).coerceIn(148.dp, 190.dp)
            val rightWidth = (maxWidth * .25f).coerceIn(250.dp, 320.dp)
            Column(Modifier.fillMaxSize()) {
                TopBar(state.device, state.user.photoFileName, onEvent, compact = true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp))
                val sidePanelInset = 24.dp
                val metricsGap = 32.dp
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = metricsGap, bottom = metricsGap - sidePanelInset)
                        .testTag("home-metrics"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HomeMetric(stringResource(CoreRes.string.common_pressure), state.device.pressure ?: number(point?.pressure), "bar")
                    HomeMetric(stringResource(Res.string.home_flow), state.device.flowRate ?: number(point?.volumePerSecond), "ml/s")
                    HomeMetric(stringResource(CoreRes.string.common_brew_boiler), state.device.brewBoilerTemp ?: "—", "°")
                    HomeMetric(stringResource(CoreRes.string.common_steam_boiler), state.device.steamBoilerTemp ?: "—", "°")
                }
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(
                        Modifier.width(leftWidth).fillMaxHeight().padding(top = sidePanelInset),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val gaugeHeight = (this@BoxWithConstraints.maxHeight * .19f).coerceIn(120.dp, 144.dp)
                        MachineSummary(state.device.flowRate?.toFloatOrNull() ?: point?.volumePerSecond ?: 0f,
                            state.manual.flowRange.maximum, stringResource(Res.string.home_flow), "ml/s", "home-flow-gauge", accent, gaugeHeight,
                            onOpen = { onEvent(DeviceDashboardEvent.DialModeChanged(ManualPriority.Flow)) })
                        MachineSummary(state.device.pressure?.toFloatOrNull() ?: point?.pressure ?: 0f,
                            12f, stringResource(CoreRes.string.common_pressure), "bar", "home-pressure-gauge", accent, gaugeHeight,
                            onOpen = { onEvent(DeviceDashboardEvent.DialModeChanged(ManualPriority.Pressure)) })
                        Column(Modifier.weight(1f).coffeeSurface(MaterialTheme.shapes.medium).padding(8.dp)) {
                            Row(Modifier.fillMaxWidth().padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(Res.string.home_profiles), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text(profiles.profiles.size.toString(), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f))
                                IconButton(onClick = { onProfileEvent(ProfileListEvent.AddProfileClicked) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Add, stringResource(Res.string.home_add), Modifier.size(17.dp))
                                }
                            }
                            LazyColumn(Modifier.weight(1f)) {
                                items(profiles.profiles, key = { it.id }) { profile ->
                                    HomeProfileRow(profile, accent,
                                        onSelect = { onProfileEvent(ProfileListEvent.ProfileSelected(profile.id)) },
                                        onEdit = { onProfileEvent(ProfileListEvent.EditProfileClicked(profile.id)) },
                                        onDelete = { pendingDelete = profile.id })
                                    if (!profile.selected) HorizontalDivider(Modifier.padding(start = 36.dp, end = 10.dp),
                                        thickness = .5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .06f))
                                }
                            }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                            val historyDuration = if (profiles.showHistory) NavigationEnterMillis else NavigationExitMillis
                            val historyAmount = animateFloatAsState(
                                if (profiles.showHistory) 1f else 0f,
                                tween(historyDuration, easing = CoffeeMotionEasing),
                                label = "history-backdrop",
                            )
                            Box(Modifier.fillMaxSize()
                                .then(if (profiles.showHistory) Modifier.clearAndSetSemantics {} else Modifier)
                                .graphicsLayer {
                                    val radius = 9.dp.toPx() * historyAmount.value
                                    renderEffect = if (radius > .01f) BlurEffect(radius, radius, TileMode.Clamp) else null
                                    alpha = 1f - .12f * historyAmount.value
                                }) {
                                HomeManualDial(state.manual, state.brew.time, accent, onEvent, Modifier.fillMaxSize())
                            }
                            androidx.compose.animation.AnimatedVisibility(
                                visible = profiles.showHistory,
                                enter = fadeIn(tween(NavigationEnterMillis, easing = CoffeeMotionEasing)) +
                                    slideInVertically(tween(NavigationEnterMillis)) { it / 32 },
                                exit = fadeOut(tween(NavigationExitMillis, easing = CoffeeMotionEasing)) +
                                    slideOutVertically(tween(NavigationExitMillis)) { it / 32 },
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                NativeMaterialFade(this, NavigationEnterMillis, NavigationExitMillis) {
                                    HistoryOverlay(profiles, onProfileEvent)
                                }
                            }
                        }
                        HomeDock(state, profiles, onEvent, onProfileEvent, onShowCharts)
                    }
                    HomeBrewSummary(state, profiles, model, accent, dark, onProfileEvent,
                        Modifier.width(rightWidth).fillMaxHeight().padding(top = sidePanelInset).testTag("home-brew-summary"))
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun HomeBrewSummary(state: DeviceDashboardViewState, profiles: ProfileListViewState, model: BrewChartModel,
    accent: Color, dark: Boolean, onProfileEvent: (ProfileListEvent) -> Unit, modifier: Modifier) {
    val selected = profiles.profiles.firstOrNull { it.selected }
    val point = state.brew.data.maxByOrNull { it.key }?.value
    Column(modifier.coffeeGlass(MaterialTheme.shapes.medium, prominent = true)
        .background(Brush.linearGradient(if (dark) listOf(Color(0xB54D719D), Color(0x80334767), Color(0xB5162848))
            else listOf(Color(0xB57498A8), Color(0x806D8D9D), Color(0x4DC8DADD))), MaterialTheme.shapes.medium).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (profiles.showHistory) state.brew.name else selected?.name ?: stringResource(Res.string.home_select),
                style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (selected != null) IconButton(onClick = { onProfileEvent(ProfileListEvent.EditProfileClicked(selected.id)) }) {
                Icon(Icons.Outlined.MoreHoriz, stringResource(Res.string.device_dashboard_edit_profile), Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HomeMetric(stringResource(Res.string.home_weight), number(point?.weight), "g", small = true)
            HomeMetric(stringResource(Res.string.home_volume), number(point?.volume), "ml", small = true)
        }
        Spacer(Modifier.height(22.dp))
        Text(stringResource(CoreRes.string.common_pressure), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HomeCurve(model, false, accent, Modifier.fillMaxWidth().weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.home_duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BrewTime(state.brew.time)
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(Res.string.home_flow), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HomeCurve(model, true, accent, Modifier.fillMaxWidth().weight(.65f))
    }
}

private fun number(value: Float?): String = value?.let { ((it * 10).roundToInt() / 10f).toString() } ?: "—"

private enum class ProfileRowAction { Closed, Edit, Delete }

@Composable
private fun HomeProfileRow(profile: ProfileListViewState.Profile, accent: Color,
    onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val distance = with(LocalDensity.current) { 52.dp.toPx() }
    val swipe = remember(distance) { AnchoredDraggableState(ProfileRowAction.Closed, DraggableAnchors {
        ProfileRowAction.Closed at 0f
        ProfileRowAction.Edit at distance
        ProfileRowAction.Delete at -distance
    }) }
    val scope = rememberCoroutineScope()
    val direction by remember(swipe) { derivedStateOf { swipe.offset.compareTo(0f) } }
    val close = { scope.launch { swipe.animateTo(ProfileRowAction.Closed, tween(180)) }; Unit }
    Box(Modifier.fillMaxWidth().heightIn(min = 52.dp)
        .clip(MaterialTheme.shapes.small)
        .anchoredDraggable(swipe, Orientation.Horizontal,
            flingBehavior = AnchoredDraggableDefaults.flingBehavior(swipe, positionalThreshold = { it * .4f }, animationSpec = tween(200)))) {
        Box(Modifier.matchParentSize().drawWithContent {
            val offset = swipe.offset
            if (offset > 0f) clipRect(right = offset) { this@drawWithContent.drawContent() }
            else if (offset < 0f) clipRect(left = size.width + offset) { this@drawWithContent.drawContent() }
        }) {
            if (direction > 0) Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().width(52.dp)
                .background(Color(0xFF238450)).clickable(role = Role.Button) { close(); onEdit() }
                .testTag("profile-edit-${profile.id}"), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Edit, stringResource(Res.string.device_dashboard_edit_profile), tint = Color.White, modifier = Modifier.size(20.dp))
            }
            if (direction < 0) Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(52.dp)
                .background(MaterialTheme.colorScheme.error).clickable(role = Role.Button) { close(); onDelete() }
                .testTag("profile-delete-${profile.id}"), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Close, stringResource(Res.string.profile_list_delete), tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(20.dp))
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("profile-row-${profile.id}")
            .offset { IntOffset(swipe.offset.roundToInt(), 0) }
            .background(if (profile.selected) accent.copy(alpha = .13f) else Color.Transparent)
            .clickable(role = Role.Button) { if (direction == 0) onSelect() else close() }
            .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(profile.number.padStart(2, '0'), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f), modifier = Modifier.width(26.dp))
            Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (profile.selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HistoryOverlay(profiles: ProfileListViewState, onEvent: (ProfileListEvent) -> Unit) {
    val dismiss = { if (profiles.showHistory) onEvent(ProfileListEvent.HistoryClicked) }
    Box(Modifier.fillMaxSize()
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = dismiss),
        contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(.94f).fillMaxHeight(.92f).testTag("home-history-panel")
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .72f), MaterialTheme.shapes.large)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            .padding(top = 4.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.profile_list_show_history), style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = dismiss, modifier = Modifier.testTag("home-history-close")) {
                    Icon(Icons.Outlined.Close, stringResource(CoreRes.string.common_close))
                }
            }
            ProfileList(profiles.copy(showHistory = true), onEvent, Modifier.fillMaxWidth().weight(1f), glassBackground = false)
        }
    }
}

@Composable
private fun HomeMetric(label: String, value: String, unit: String, small: Boolean = false) {
    val ink = MaterialTheme.colorScheme.onSurface
    val display = Regex("-?\\d+(?:\\.\\d+)?").find(value)?.value ?: "—"
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(display, fontSize = if (small) 27.sp else 36.sp, lineHeight = if (small) 32.sp else 40.sp,
                fontWeight = FontWeight.Light, color = ink, modifier = Modifier.alignByBaseline())
            Text(unit, fontSize = 12.sp, color = ink.copy(alpha = .6f), modifier = Modifier.padding(start = 6.dp).alignByBaseline())
        }
    }
}

@Composable
private fun MachineSummary(value: Float, maxValue: Float, label: String, unit: String, tag: String,
    accent: Color, height: androidx.compose.ui.unit.Dp, onOpen: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth().height(height).testTag(tag).coffeeSurface(MaterialTheme.shapes.medium).clickable(onClick = onOpen).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(number(value), fontSize = 28.sp, fontWeight = FontWeight.Light, modifier = Modifier.alignByBaseline())
            Text(unit, fontSize = 12.sp, color = ink.copy(alpha = .5f), modifier = Modifier.padding(start = 5.dp).alignByBaseline())
        }
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val r = minOf(size.width * .46f, (size.height - 8.dp.toPx()).coerceAtLeast(1f))
            val c = Offset(size.width / 2f, size.height - 2.dp.toPx())
            drawArc(Brush.linearGradient(listOf(accent.copy(alpha = .06f), accent.copy(alpha = .6f))),
                180f, 180f * (value / maxValue).coerceIn(0f, 1f), true,
                topLeft = c - Offset(r, r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2))
            for (i in 0..48) {
                val a = (180 + i * 3.75) * PI / 180
                val v = Offset(cos(a).toFloat(), sin(a).toFloat())
                drawLine(ink.copy(alpha = .35f), c + v * r, c + v * (r + if (i % 8 == 0) 6.dp.toPx() else 2.dp.toPx()), .7.dp.toPx())
            }
            val a = (180 + 180 * (value / maxValue).coerceIn(0f, 1f)) * PI / 180
            drawLine(ink.copy(alpha = .6f), c, c + Offset(cos(a).toFloat(), sin(a).toFloat()) * r, .8.dp.toPx())
        }
    }
}

@Composable
private fun HomeCurve(model: BrewChartModel, flow: Boolean, accent: Color, modifier: Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = ink.copy(alpha = .45f))
    val measurer = rememberTextMeasurer()
    val points = model.measurements.entries.sortedBy { it.key }
    val targets = model.targets.entries.sortedBy { it.key }
    val maxTime = maxOf(40f, points.lastOrNull()?.key ?: 0f, targets.lastOrNull()?.key ?: 0f)
    val maxValue = if (flow) maxOf(4f, points.maxOfOrNull { it.value.volumePerSecond } ?: 0f) else 12f
    Canvas(modifier) {
        val l = 20.dp.toPx(); val t = 14.dp.toPx(); val w = (size.width - l - 4.dp.toPx()).coerceAtLeast(1f)
        val h = (size.height - t - 24.dp.toPx()).coerceAtLeast(1f)
        fun position(x: Float, y: Float) = Offset(l + x / maxTime * w, t + h - (y / maxValue).coerceIn(0f, 1f) * h)
        for (i in 0..3) {
            val y = t + h * i / 3
            drawLine(ink.copy(alpha = .12f), Offset(l, y), Offset(l + w, y), .5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx())))
            val label = measurer.measure((maxValue * (3-i) / 3).roundToInt().toString(), labelStyle)
            drawText(label, topLeft = Offset(0f, y - label.size.height / 2))
        }
        val actual = Path()
        points.forEachIndexed { i, entry ->
            val p = position(entry.key, if (flow) entry.value.volumePerSecond else entry.value.pressure)
            if (i == 0) actual.moveTo(p.x, p.y) else actual.lineTo(p.x, p.y)
        }
        if (points.isNotEmpty()) {
            val fill = Path().apply {
                addPath(actual)
                lineTo(position(points.last().key, 0f).x, t + h)
                lineTo(position(points.first().key, 0f).x, t + h)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(accent.copy(alpha = .28f), Color.Transparent), t, t + h))
            drawPath(actual, ink.copy(alpha = .85f), style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        val target = Path()
        targets.forEachIndexed { i, entry ->
            val p = position(entry.key, if (flow) entry.value.volumePerSecond else entry.value.pressure)
            if (i == 0) target.moveTo(p.x, p.y) else target.lineTo(p.x, p.y)
        }
        drawPath(target, ink.copy(alpha = .28f), style = Stroke(.9.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))))
        for (i in 1..4) {
            val text = measurer.measure("${(maxTime * i / 4).roundToInt()}s", labelStyle)
            drawText(text, topLeft = Offset(l + w * i / 4 - text.size.width / 2, t + h + 9.dp.toPx()))
        }
    }
}
