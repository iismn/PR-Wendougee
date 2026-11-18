package app.geeflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.ui.theme.coffeeSurface
import geeflow.shared.core.ui.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.*

/** The same bounded temperature control is used in quick settings and machine settings. */
@Composable
fun BoilerControlCard(
    title: String,
    actualTemp: Float,
    selectedTemp: String,
    values: List<String>,
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    embedded: Boolean = false,
) {
    val numeric = remember(values) { values.mapNotNull { value -> value.toFloatOrNull()?.let { it to value } }.sortedBy { it.first } }
    val range = (numeric.firstOrNull()?.first ?: 0f)..(numeric.lastOrNull()?.first ?: 0f)
    val selected = selectedTemp.toFloatOrNull() ?: range.start
    // Targets can be prepared while the heater is off; the switch is a separate choice.
    val interactive = enabled && numeric.size > 1
    var inputShown by remember { mutableStateOf(false) }
    // Machine limits arrive asynchronously after opening the popup. A local
    // function reference can compare equal while retaining its initial captures.
    val currentOptions by rememberUpdatedState(numeric)
    val currentEditable by rememberUpdatedState(interactive)
    val currentOnValueChanged by rememberUpdatedState(onValueChanged)
    val select: (Float) -> Unit = remember {
        { value ->
            if (currentEditable && value.isFinite()) {
                currentOptions.minByOrNull { abs(it.first - value) }?.let { currentOnValueChanged(it.second) }
            }
        }
    }
    val ink = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.then(if (embedded) Modifier else Modifier.coffeeSurface(MaterialTheme.shapes.large))
        .padding(if (embedded) 12.dp else 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChanged, enabled = enabled,
                modifier = Modifier.semantics { contentDescription = title })
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.clickable(enabled = interactive) { inputShown = true }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.Bottom) {
            Text(selectedTemp, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Light,
                color = if (checked) ink else secondary, modifier = Modifier.alignByBaseline())
            Text("°", fontSize = 12.sp, color = secondary, modifier = Modifier.padding(start = 4.dp).alignByBaseline())
            Text(stringResource(Res.string.boiler_target), style = MaterialTheme.typography.bodySmall,
                color = secondary, modifier = Modifier.padding(start = 12.dp).alignByBaseline())
        }
        Spacer(Modifier.height(14.dp))
        ArcTemperatureDial(
            value = selected, valueRange = range, label = title, enabled = interactive,
            onValueChange = select, modifier = Modifier.semantics { testTag = "temperature-dial-$title" }.fillMaxWidth().height(144.dp),
        )
        Spacer(Modifier.height(if (embedded) 16.dp else 22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BoilerMetric(stringResource(Res.string.boiler_current), "${(actualTemp * 10).roundToInt() / 10f}°", Modifier.weight(1f))
            BoilerMetric(stringResource(Res.string.boiler_range), if (numeric.isEmpty()) "—" else "${numeric.first().second}–${numeric.last().second}°", Modifier.weight(1f))
            BoilerMetric(stringResource(Res.string.boiler_state), stringResource(if (checked) Res.string.common_on else Res.string.common_off), Modifier.weight(1f))
        }
    }
    if (inputShown) GeeFlowDialog(onDismissRequest = { inputShown = false }) {
        Column(Modifier.widthIn(max = 380.dp).padding(24.dp)) {
            GeeFlowDialogTopBar(title, onCloseClick = { inputShown = false })
            GeeFlowInputPad(valueRange = range, unit = "°", allowDecimal = false,
                onConfirm = { select(it); inputShown = false }, onBack = { inputShown = false })
        }
    }
}

@Composable
private fun BoilerMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcTemperatureDial(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    unit: String = "°",
) {
    val span = valueRange.endInclusive - valueRange.start
    val interactive = enabled && span > 0f
    val bounded = value.coerceIn(valueRange)
    val fraction = if (span > 0f) (bounded - valueRange.start) / span else 0f
    val onChange by rememberUpdatedState(onValueChange)
    val ink = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.32f)
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val measurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodySmall
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val dragged by interaction.collectIsDraggedAsState()
    val illuminated = interactive && (pressed || dragged)
    val glow by animateFloatAsState(if (illuminated) 1f else 0f,
        tween(if (illuminated) 100 else 420), label = "temperature-ruler-contact")
    val glowColor = MaterialTheme.colorScheme.primary
    // Material Slider handles gestures, keyboard and accessibility for the arc.
    Slider(
        value = bounded,
        onValueChange = { requested ->
            if (interactive && requested.isFinite()) onChange(requested.coerceIn(valueRange))
        },
        enabled = interactive,
        interactionSource = interaction,
        valueRange = if (span > 0f) valueRange else 0f..1f,
        modifier = modifier.padding(horizontal = 24.dp).semantics {
            contentDescription = label
            stateDescription = "${(bounded * 10).roundToInt() / 10f} $unit"
        },
        thumb = { Spacer(Modifier.size(0.dp)) },
        track = {
            Canvas(Modifier.fillMaxWidth().height(144.dp)) {
                val radius = size.width.coerceAtLeast(1f) / (2f * sin(35f * PI.toFloat() / 180f))
                val center = Offset(size.width / 2, 34.dp.toPx() + radius)
                fun angle(f: Float): Float = asin((2f * f.coerceIn(0f, 1f) - 1f) * sin(35f * PI.toFloat() / 180f)) - PI.toFloat() / 2f
                fun point(f: Float, r: Float = radius): Offset {
                    val radians = angle(f)
                    return center + Offset(cos(radians), sin(radians)) * r
                }
                fun arc(r: Float, color: Color, stroke: Stroke) = drawArc(color, -125f, 70f, false,
                    topLeft = center - Offset(r, r), size = Size(2 * r, 2 * r), style = stroke)
                arc(radius + 20.dp.toPx(), subtle, Stroke(0.6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 3.dp.toPx()))))
                for (i in 0..24) {
                    val tick = i / 24f
                    val from = point(tick, radius + 8.dp.toPx())
                    val to = point(tick, radius + (if (i % 4 == 0) 16.dp else 11.dp).toPx())
                    if (glow > .001f) {
                        drawLine(glowColor.copy(alpha = .10f * glow), from, to, 8.dp.toPx(), StrokeCap.Round)
                        drawLine(glowColor.copy(alpha = .24f * glow), from, to, 3.dp.toPx(), StrokeCap.Round)
                    }
                    drawLine(subtle.copy(alpha = subtle.alpha + .5f * glow), from, to, 1.dp.toPx(), StrokeCap.Round)
                }
                arc(radius, ink.copy(alpha = ink.alpha * 0.22f), Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                drawArc(ink, -125f, angle(fraction) * 180f / PI.toFloat() + 125f, false, topLeft = center - Offset(radius, radius),
                    size = Size(radius * 2, radius * 2), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(ink, 2.5.dp.toPx(), point(0f))
                drawCircle(ink, 2.5.dp.toPx(), point(1f))
                drawCircle(ink, 5.dp.toPx(), point(fraction))
                drawLine(ink, point(fraction, radius + 10.dp.toPx()), point(fraction, radius + 21.dp.toPx()), 1.5.dp.toPx(), StrokeCap.Round)
                fun number(f: Float, number: Int, color: Color) {
                    val layout = measurer.measure(number.toString(), textStyle.copy(color = color))
                    val origin = point(f) + Offset(-layout.size.width / 2f, 12.dp.toPx())
                    drawText(layout, topLeft = origin)
                }
                number(0f, valueRange.start.roundToInt(), subtle)
                number(1f, valueRange.endInclusive.roundToInt(), subtle)
                if (fraction in 0.14f..0.86f) number(fraction, bounded.roundToInt(), ink)
            }
        },
    )
}
