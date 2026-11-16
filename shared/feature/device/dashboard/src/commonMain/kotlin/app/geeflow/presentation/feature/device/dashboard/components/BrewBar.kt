package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.FlowRate
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Pressure
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Volume
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Weight
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.WeightRate
import app.geeflow.presentation.feature.device.dashboard.model.ChartData
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.components.InstrumentNumber
import app.geeflow.ui.theme.GeeFlowTheme
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.common_avg
import geeflow.shared.core.ui.generated.resources.common_sec
import geeflow.shared.core.ui.generated.resources.unit_bar
import geeflow.shared.core.ui.generated.resources.unit_grams
import geeflow.shared.core.ui.generated.resources.unit_grams_per_second
import geeflow.shared.core.ui.generated.resources.unit_milliliters
import geeflow.shared.core.ui.generated.resources.unit_milliliters_per_second
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BrewBar(
    brew: Brew,
    isBrewing: Boolean,
    visibleCharts: Set<DashboardChartType>,
    onToggle: (DashboardChartType) -> Unit,
    modifier: Modifier = Modifier,
    onHeaderClick: (() -> Unit)? = null,
    showEditIcon: Boolean = false,
) {
    Column(modifier = modifier) {
        BrewHeader(brew = brew, onHeaderClick = onHeaderClick, showEditIcon = showEditIcon)
        Spacer(modifier = Modifier.height(12.dp))
        BrewStatistics(brew = brew, isBrewing = isBrewing, visibleCharts = visibleCharts, onToggle = onToggle)
    }
}

@Composable
internal fun BrewHeader(
    brew: Brew,
    modifier: Modifier = Modifier,
    onHeaderClick: (() -> Unit)? = null,
    showEditIcon: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .then(if (onHeaderClick != null) Modifier.clickable(onClick = onHeaderClick) else Modifier)
            .padding(start = 8.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
    ) {
        if (showEditIcon) EditIcon()
        Text(
            text = brew.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.weight(1f))
        BrewTime(brew.time)
    }
}

@Composable
internal fun BrewTime(seconds: Int, modifier: Modifier = Modifier) {
    Text(
        text = "$seconds ${stringResource(Res.string.common_sec)}",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
internal fun BrewStatistics(
    brew: Brew,
    isBrewing: Boolean,
    visibleCharts: Set<DashboardChartType>,
    onToggle: (DashboardChartType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastPoint = brew.data.values.lastOrNull()

    val currentPressure = if (!isBrewing && brew.data.isNotEmpty()) {
        brew.data.values.map { it.pressure }.average().takeIf { !it.isNaN() } ?: 0.0
    } else {
        lastPoint?.pressure?.toDouble() ?: 0.0
    }

    val currentFlow = if (!isBrewing && brew.data.isNotEmpty()) {
        brew.data.values.map { it.volumePerSecond }.average().takeIf { !it.isNaN() } ?: 0.0
    } else {
        lastPoint?.volumePerSecond?.toDouble() ?: 0.0
    }

    val currentWeightRate = if (!isBrewing && brew.data.isNotEmpty()) {
        brew.data.values.map { it.weightPerSecond }.average().takeIf { !it.isNaN() } ?: 0.0
    } else {
        lastPoint?.weightPerSecond?.toDouble() ?: 0.0
    }

    val totalWeight = lastPoint?.weight ?: 0f
    val totalVolume = lastPoint?.volume ?: 0f

    val averaged = !isBrewing && brew.data.isNotEmpty()
    val entries = listOf(
        ValueEntry(currentPressure, stringResource(Res.string.unit_bar), MaterialTheme.colorScheme.error, Pressure, averaged),
        ValueEntry(currentWeightRate, stringResource(Res.string.unit_grams_per_second), MaterialTheme.colorScheme.onSurface, WeightRate, averaged),
        ValueEntry(currentFlow, stringResource(Res.string.unit_milliliters_per_second), GeeFlowTheme.colors.water, FlowRate, averaged),
        ValueEntry(totalWeight.toDouble(), stringResource(Res.string.unit_grams), MaterialTheme.colorScheme.onSurface, Weight, false),
        ValueEntry(totalVolume.toDouble(), stringResource(Res.string.unit_milliliters), GeeFlowTheme.colors.waterVariant, Volume, false),
    )
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 560.dp) 5 else 3
        val numberHeight = if (columns == 5) 28.dp else 24.dp
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            entries.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { entry ->
                        StatisticValue(
                            entry = entry,
                            selected = entry.type in visibleCharts,
                            numberHeight = numberHeight,
                            onToggle = { onToggle(entry.type) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
@Composable
private fun EditIcon() {
    Icon(
        imageVector = Icons.Filled.Edit,
        contentDescription = null,
        modifier = Modifier.padding(end = 8.dp).size(16.dp),
    )
}

private data class ValueEntry(
    val value: Double,
    val unit: String,
    val color: Color,
    val type: DashboardChartType,
    val averaged: Boolean,
)

@Composable
private fun StatisticValue(
    entry: ValueEntry,
    selected: Boolean,
    numberHeight: Dp,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.1f
    val emphasis by animateFloatAsState(if (selected) 1f else 0.56f, tween(220), label = "metricEmphasis")
    val glow by animateFloatAsState(if (selected) 0.8f else 0.3f, tween(220), label = "metricGlow")
    val value = entry.value.format()
    val description = if (entry.averaged) {
        "${stringResource(Res.string.common_avg)} $value ${entry.unit}"
    } else {
        "$value ${entry.unit}"
    }
    Row(
        modifier = modifier
            .height(64.dp)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .clip(MaterialTheme.shapes.small)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        InstrumentNumber(
            text = value,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = emphasis),
            glowColor = if (dark) Color(0xFF83B9FF) else entry.color,
            glowStrength = glow,
            height = numberHeight,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            text = entry.unit,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(start = 6.dp, top = 8.dp),
        )
    }
}
private const val DecimalScale = 10

private fun Double.format() = ((this * DecimalScale).toInt() / DecimalScale.toDouble()).toString()

private val previewBrew = Brew(
    name = "Slayer shot",
    description = "Long pre-infusion at low pressure, then a slow declining ramp to 6 bar",
    time = 25,
    data = mapOf(
        1f to ChartData(
            pressure = 9.0f,
            weight = 36.0f,
            weightPerSecond = 2.5f,
            volume = 40.0f,
            volumePerSecond = 2.8f,
        ),
    ),
)

private val previewVisibleCharts = setOf(
    Pressure,
    FlowRate,
    WeightRate,
)

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    BrewBar(
        brew = previewBrew,
        isBrewing = true,
        visibleCharts = previewVisibleCharts,
        onToggle = {},
        onHeaderClick = {},
        showEditIcon = true,
    )
}
