package app.geeflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

/** A compact instrument readout. The lit cells and decimal share one optical baseline. */
@Composable
fun InstrumentNumber(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 24.dp,
    glowColor: Color = color,
    glowStrength: Float = 0f,
) {
    val columns = (text.sumOf { if (it == '.') 2 else 6 } - 1).coerceAtLeast(1)
    Canvas(
        modifier.width(height * (columns / 7f)).height(height)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val pitch = min(placeable.height / 7f, placeable.width / columns.toFloat())
                val inkBottom = (placeable.height - 7 * pitch) / 2 + 6.78f * pitch
                layout(placeable.width, placeable.height, mapOf(FirstBaseline to inkBottom.roundToInt())) {
                    placeable.place(0, 0)
                }
            }
            .semantics { contentDescription = text },
    ) {
        val pitch = min(size.height / 7, size.width / columns)
        val left = (size.width - columns * pitch) / 2
        val top = (size.height - 7 * pitch) / 2
        var cursor = 0
        text.forEach { digit ->
            val rows = NumberCells[digit] ?: NumberCells.getValue('-')
            rows.forEachIndexed { row, bits ->
                bits.forEachIndexed { column, cell ->
                    if (cell == '1') {
                        val center = Offset(left + (cursor + column + 0.5f) * pitch, top + (row + 0.5f) * pitch)
                        if (glowStrength > 0f) {
                            drawCircle(
                                Brush.radialGradient(
                                    colors = listOf(
                                        glowColor.copy(alpha = 0.38f * glowStrength),
                                        glowColor.copy(alpha = 0.08f * glowStrength),
                                        Color.Transparent,
                                    ),
                                    center = center,
                                    radius = pitch * 1.6f,
                                ),
                                radius = pitch * 1.6f,
                                center = center,
                            )
                        }
                        drawCircle(color, pitch * 0.28f, center)
                    }
                }
            }
            cursor += if (digit == '.') 2 else 6
        }
    }
}

private val NumberCells = mapOf(
    '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '5' to listOf("11111", "10000", "10000", "11110", "00001", "00001", "11110"),
    '6' to listOf("01110", "10000", "10000", "11110", "10001", "10001", "01110"),
    '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '9' to listOf("01110", "10001", "10001", "01111", "00001", "00001", "01110"),
    '.' to listOf("0", "0", "0", "0", "0", "0", "1"),
    '-' to listOf("00000", "00000", "00000", "11111", "00000", "00000", "00000"),
)
