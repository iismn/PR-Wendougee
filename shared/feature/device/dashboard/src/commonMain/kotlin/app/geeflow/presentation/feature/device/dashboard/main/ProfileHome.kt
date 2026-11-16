package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    Box(Modifier.fillMaxSize().geeFlowInsetsPadding()) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
            val leftWidth = (maxWidth * .18f).coerceIn(166.dp, 220.dp)
            val rightWidth = (maxWidth * .255f).coerceIn(250.dp, 320.dp)
            Column(Modifier.fillMaxSize()) {
                TopBar(state.device, state.user.photoFileName, onEvent, compact = true,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp))
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth().padding(start = leftWidth + 24.dp, end = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    HomeMetric(stringResource(CoreRes.string.common_pressure), state.device.pressure ?: number(point?.pressure), "bar")
                    HomeMetric(stringResource(Res.string.home_flow), number(point?.volumePerSecond), "ml/s")
                    HomeMetric(stringResource(CoreRes.string.common_brew_boiler), state.device.brewBoilerTemp ?: "—", "°")
                    HomeMetric(stringResource(CoreRes.string.common_steam_boiler), state.device.steamBoilerTemp ?: "—", "°")
                }
                Spacer(Modifier.height(26.dp))
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.width(leftWidth).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PressureSummary(state.device.pressure?.toFloatOrNull() ?: point?.pressure ?: 0f, accent,
                            onOpen = { onEvent(DeviceDashboardEvent.QuickSettingsClicked) })
                        Column(Modifier.weight(1f).coffeeSurface(MaterialTheme.shapes.medium).padding(8.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(Res.string.home_profiles), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(profiles.profiles.size.toString(), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f))
                            }
                            LazyColumn(Modifier.weight(1f)) {
                            items(profiles.profiles, key = { it.id }) { profile ->
                                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("profile-row-${profile.id}")
                                    .clip(MaterialTheme.shapes.small)
                                    .background(if (profile.selected) accent.copy(alpha = .13f) else Color.Transparent)
                                    .clickable(role = Role.Button) { onProfileEvent(ProfileListEvent.ProfileSelected(profile.id)) }
                                    .padding(start = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(profile.number.padStart(2, '0'), style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f), modifier = Modifier.width(26.dp))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (profile.selected) FontWeight.SemiBold else FontWeight.Normal)
                                    }
                                    if (profile.selected) IconButton(onClick = { onProfileEvent(ProfileListEvent.EditProfileClicked(profile.id)) },
                                        modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Outlined.Edit, stringResource(Res.string.device_dashboard_edit_profile), Modifier.size(16.dp))
                                    } else Spacer(Modifier.width(8.dp))
                                }
                                if (!profile.selected) HorizontalDivider(Modifier.padding(start = 36.dp, end = 10.dp),
                                    thickness = .5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .06f))
                            }
                            }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledTonalButton(onClick = { onProfileEvent(ProfileListEvent.AddProfileClicked) },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface)) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.home_add), style = MaterialTheme.typography.labelMedium)
                        }
                        if (profiles.showHistory) ProfileList(profiles, onProfileEvent,
                            Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp))
                        else ProfileOrbit(profiles, selected, accent, state.device.isBrewing,
                            state.brew.time, model.targets.keys.maxOrNull() ?: 0f, onProfileEvent, onEvent, onShowCharts,
                            Modifier.fillMaxWidth().weight(1f))
                        Row(Modifier.testTag("profile-home-dock").coffeeGlass(CircleShape).padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            HomeIcon(Icons.Outlined.Apps, stringResource(Res.string.home_profiles), !profiles.showHistory) {
                                if (profiles.showHistory) onProfileEvent(ProfileListEvent.HistoryClicked)
                            }
                            HomeIcon(Icons.Outlined.ShowChart, stringResource(Res.string.dashboard_tab_details), onClick = onShowCharts)
                            HomeIcon(Icons.Outlined.History, stringResource(Res.string.profile_list_show_history), profiles.showHistory) {
                                onProfileEvent(ProfileListEvent.HistoryClicked)
                            }
                            HomeIcon(Icons.Outlined.Tune, stringResource(Res.string.free_control_screen_title)) { onEvent(DeviceDashboardEvent.FlowControlClicked) }
                            HomeIcon(Icons.Outlined.Settings, stringResource(Res.string.settings_quick_title)) { onEvent(DeviceDashboardEvent.QuickSettingsClicked) }
                            VerticalDivider(Modifier.padding(horizontal = 5.dp).height(22.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
                            DashboardBrewButton(state, onEvent, glassBackground = false)
                        }
                    }
                    Column(Modifier.width(rightWidth).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.fillMaxWidth().weight(1f).coffeeGlass(MaterialTheme.shapes.medium, prominent = true)
                            .background(Brush.linearGradient(if (dark) listOf(Color(0xB54D719D), Color(0x80334767), Color(0xB5162848))
                                else listOf(Color(0xB57498A8), Color(0x806D8D9D), Color(0x4DC8DADD))), MaterialTheme.shapes.medium).padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (profiles.showHistory) state.brew.name else selected?.name ?: stringResource(Res.string.home_select),
                                    style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f))
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
                            Text(stringResource(CoreRes.string.common_pressure), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HomeCurve(model, false, accent, Modifier.fillMaxWidth().weight(1f))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(Res.string.home_duration), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                BrewTime(state.brew.time)
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(stringResource(Res.string.home_flow), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HomeCurve(model, true, accent, Modifier.fillMaxWidth().weight(.65f))
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private fun number(value: Float?): String = value?.let { ((it * 10).roundToInt() / 10f).toString() } ?: "—"

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
private fun HomeIcon(icon: ImageVector, label: String, selected: Boolean = false, onClick: () -> Unit) {
    IconButton(onClick, Modifier.size(42.dp).background(
        if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)) {
        Icon(icon, label, Modifier.size(20.dp), tint = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PressureSummary(pressure: Float, accent: Color, onOpen: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth().coffeeSurface(MaterialTheme.shapes.medium).clickable(onClick = onOpen).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(Res.string.home_machine), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(number(pressure), fontSize = 33.sp, fontWeight = FontWeight.Light, modifier = Modifier.alignByBaseline())
            Text("bar", fontSize = 12.sp, color = ink.copy(alpha = .5f), modifier = Modifier.padding(start = 5.dp).alignByBaseline())
        }
        Canvas(Modifier.fillMaxWidth().height(92.dp)) {
            val r = size.width * .46f
            val c = Offset(size.width / 2f, size.height - 2.dp.toPx())
            drawArc(Brush.linearGradient(listOf(accent.copy(alpha = .06f), accent.copy(alpha = .6f))),
                180f, 180f * (pressure / 12f).coerceIn(0f, 1f), true,
                topLeft = c - Offset(r, r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2))
            for (i in 0..48) {
                val a = (180 + i * 3.75) * PI / 180
                val v = Offset(cos(a).toFloat(), sin(a).toFloat())
                drawLine(ink.copy(alpha = .35f), c + v * r, c + v * (r + if (i % 8 == 0) 6.dp.toPx() else 2.dp.toPx()), .7.dp.toPx())
            }
            val a = (180 + 180 * (pressure / 12f).coerceIn(0f, 1f)) * PI / 180
            drawLine(ink.copy(alpha = .6f), c, c + Offset(cos(a).toFloat(), sin(a).toFloat()) * r, .8.dp.toPx())
        }
    }
}

@Composable
private fun ProfileOrbit(
    state: ProfileListViewState, selected: ProfileListViewState.Profile?, accent: Color,
    brewing: Boolean, seconds: Int, duration: Float,
    onProfileEvent: (ProfileListEvent) -> Unit, onEvent: (DeviceDashboardEvent) -> Unit,
    onShowCharts: () -> Unit, modifier: Modifier,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val progress by animateFloatAsState(if (brewing && duration > 0) (seconds / duration).coerceIn(0f, 1f) else 1f, tween(450))
    BoxWithConstraints(modifier.clipToBounds()) {
        val diameter = minOf(maxWidth * .57f, maxHeight * .65f)
        val radius = diameter / 2
        val left = (maxWidth - diameter) / 2 + 8.dp
        val top = (maxHeight - diameter) * .33f
        val cx = left + radius
        val cy = top + radius
        Box(Modifier.offset(left, top).size(diameter).testTag("profile-orbit-selected").coffeeGlass(CircleShape)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = .44f), accent.copy(alpha = .12f))), CircleShape)
            .clickable(enabled = selected != null, role = Role.Button) { selected?.let { onProfileEvent(ProfileListEvent.EditProfileClicked(it.id)) } },
            contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(11.dp)) {
                val r = size.minDimension / 2f - 1.dp.toPx()
                for (i in 0 until 120) {
                    val a = (i * 3.0 - 90) * PI / 180
                    val v = Offset(cos(a).toFloat(), sin(a).toFloat())
                    drawLine(ink.copy(alpha = if (i % 10 == 0) .4f else .22f), center + v * r,
                        center + v * (r - if (i % 10 == 0) 5.dp.toPx() else 2.dp.toPx()), .65.dp.toPx())
                }
                val a = (progress * 360 - 90) * PI / 180
                val v = Offset(cos(a).toFloat(), sin(a).toFloat())
                drawLine(ink.copy(alpha = .6f), center + v * (r - 3.dp.toPx()), center + v * (r - 40.dp.toPx()), .8.dp.toPx())
            }
            AnimatedContent(selected?.id, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(180)) }) { id ->
                val profile = state.profiles.firstOrNull { it.id == id }
                Column(Modifier.padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(if (brewing) Res.string.home_brewing else Res.string.home_selected),
                        style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = .5f))
                    Text(profile?.name ?: stringResource(Res.string.home_select), fontSize = 29.sp,
                        lineHeight = 33.sp, fontWeight = FontWeight.Light, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (brewing) Text("$seconds s", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(cx.toPx(), cy.toPx())
            val inner = radius.toPx() + 10.dp.toPx()
            val outer = inner + 40.dp.toPx()
            listOf(-44f, -14f, 16f).forEach { angle ->
                val path = Path().apply {
                    val a = angle * PI / 180
                    moveTo(c.x + cos(a).toFloat() * outer, c.y + sin(a).toFloat() * outer)
                    arcTo(Rect(c - Offset(outer, outer), c + Offset(outer, outer)), angle, 28f, false)
                    val end = (angle + 28) * PI / 180
                    lineTo(c.x + cos(end).toFloat() * inner, c.y + sin(end).toFloat() * inner)
                    arcTo(Rect(c - Offset(inner, inner), c + Offset(inner, inner)), angle + 28, -28f, false)
                    close()
                }
                drawPath(path, accent.copy(alpha = .35f))
            }
        }
        val actions = listOf(Triple(Icons.Outlined.Settings, stringResource(Res.string.settings_quick_title), { onEvent(DeviceDashboardEvent.QuickSettingsClicked) }),
            Triple(Icons.Outlined.WaterDrop, stringResource(Res.string.free_control_screen_title), { onEvent(DeviceDashboardEvent.FlowControlClicked) }),
            Triple(Icons.Outlined.ShowChart, stringResource(Res.string.dashboard_tab_details), onShowCharts))
        actions.forEachIndexed { index, action ->
            val a = (-30 + index * 30) * PI / 180
            val r = radius + 30.dp
            IconButton(action.third, Modifier.offset(cx + r * cos(a).toFloat() - 22.dp, cy + r * sin(a).toFloat() - 22.dp).size(44.dp)) {
                Icon(action.first, action.second, Modifier.size(17.dp))
            }
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
