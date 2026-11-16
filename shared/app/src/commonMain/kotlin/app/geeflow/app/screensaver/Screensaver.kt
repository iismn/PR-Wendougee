package app.geeflow.app.screensaver

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import app.geeflow.platform.rememberFormattedTime
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview

@Composable
internal fun Screensaver(onDismiss: () -> Unit, modifier: Modifier = Modifier) =
    Screensaver(rememberFormattedTime(), onDismiss, modifier)

@Composable
internal fun Screensaver(formattedTime: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val digits = remember(formattedTime) {
        TimeDigitsRegex.find(formattedTime)?.value?.replace('.', ':') ?: formattedTime
    }
    val period = remember(formattedTime) {
        val value = formattedTime.lowercase()
        when {
            value.contains("am") || value.contains("a.m.") -> "AM"
            value.contains("pm") || value.contains("p.m.") -> "PM"
            else -> ""
        }
    }
    BoxWithConstraints(
        modifier.fillMaxSize().background(Color.Black).pointerInput(onDismiss) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.pressed }) {
                        event.changes.forEach { it.consume() }
                        onDismiss()
                    }
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        val clockWidth = minOf(maxWidth * .72f, maxHeight * 2f, 820.dp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DotClock(digits, formattedTime, Modifier.width(clockWidth))
            if (period.isNotEmpty()) Text(period, style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = .65f))
        }
    }
}

/** Clock-only monochrome dot renderer. Brew measurements continue to use Pretendard. */
@Composable
private fun DotClock(value: String, spokenTime: String, modifier: Modifier = Modifier) {
    val glyphs = remember(value) { value.mapNotNull { character ->
        if (character == ':') Colon else character.digitToIntOrNull()?.let { Digits[it] }
    } }
    val columns = glyphs.sumOf { it.first().length } + (glyphs.size - 1).coerceAtLeast(0)
    val aspect = (columns + 4f) / 11f
    Canvas(modifier.aspectRatio(aspect).semantics { contentDescription = spokenTime }) {
        val pitch = size.width / (columns + 4f)
        val radius = pitch * .18f
        var xColumn = 2f
        glyphs.forEach { glyph ->
            glyph.forEachIndexed { row, cells -> cells.forEachIndexed { column, cell ->
                if (cell == '1') {
                    val center = Offset((xColumn + column + .5f) * pitch, (row + 2.5f) * pitch)
                    val halo = radius * 5f
                    drawCircle(Brush.radialGradient(
                        0f to Color.White.copy(alpha = .22f),
                        .3f to Color.White.copy(alpha = .10f),
                        1f to Color.Transparent,
                        center = center, radius = halo), halo, center)
                    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .35f), Color.Transparent),
                        center, radius * 2f), radius * 2f, center)
                    drawCircle(Color.White.copy(alpha = .92f), radius, center)
                }
            } }
            xColumn += glyph.first().length + 1f
        }
    }
}

private val Digits = listOf(
    listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    listOf("11111", "10000", "10000", "11110", "00001", "00001", "11110"),
    listOf("01110", "10000", "10000", "11110", "10001", "10001", "01110"),
    listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    listOf("01110", "10001", "10001", "01111", "00001", "00001", "01110"),
)
private val Colon = listOf("0", "0", "1", "0", "1", "0", "0")
private val TimeDigitsRegex = Regex("""\d{1,2}[:.]\d{2}(?:[:.]\d{2})?""")

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() = Screensaver("09:41", {})
