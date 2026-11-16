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
import androidx.compose.ui.unit.sp
import app.geeflow.platform.rememberFormattedTime
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview

@Composable
internal fun Screensaver(onDismiss: () -> Unit, modifier: Modifier = Modifier) =
    Screensaver(rememberFormattedTime(), onDismiss, modifier)

@Composable
internal fun Screensaver(formattedTime: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val time = remember(formattedTime) { parseScreensaverTime(formattedTime) }
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
        val clockWidth = minOf(maxWidth * .324f, maxHeight * .87f, 336.dp)
        Box(Modifier.width(clockWidth)) {
            DotClock(time.digits, formattedTime, Modifier.fillMaxWidth())
            Column(Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
                horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.1.sp, lineHeight = 11.7.sp)
                Text("AM", style = labelStyle, color = Color.White.copy(alpha = if (time.isAm) .8f else .2f))
                Text("PM", style = labelStyle, color = Color.White.copy(alpha = if (time.isAm) .2f else .8f))
            }
        }
    }
}

/** Clock-only monochrome dot renderer. Brew measurements continue to use Pretendard. */
@Composable
private fun DotClock(value: String, spokenTime: String, modifier: Modifier = Modifier) {
    val glyphs = remember(value) { value.mapNotNull { character ->
        when (character) { ':' -> Colon; '-' -> Dash; else -> character.digitToIntOrNull()?.let { Digits[it] } }
    } }
    val columns = glyphs.sumOf { it.first().length } + (glyphs.size - 1).coerceAtLeast(0)
    val aspect = (columns + 4f) / 11f
    Canvas(modifier.aspectRatio(aspect).semantics { contentDescription = spokenTime }) {
        val pitch = size.width / (columns + 4f)
        val radius = pitch * .16f
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
private val Dash = listOf("00000", "00000", "00000", "11111", "00000", "00000", "00000")

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
private fun Preview() = Screensaver("09:41", {})
