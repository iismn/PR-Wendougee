package app.geeflow.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Seven-row dot digits, drawn at the requested height rather than relying on a system font. */
@Composable
fun DotMatrixNumber(value: Int, color: Color, modifier: Modifier = Modifier, glowColor: Color = color) =
    DotMatrixNumber(value.toString(), color, modifier, glowColor)

/** Decimal points occupy one column and minus signs occupy three. */
@Composable
fun DotMatrixNumber(value: String, color: Color, modifier: Modifier = Modifier, glowColor: Color = color) {
    DotMatrix(value, value, color, glowColor, clock = false, modifier = modifier)
}

@Composable
fun DotMatrixClock(value: String, spokenTime: String, modifier: Modifier = Modifier) {
    DotMatrix(value, spokenTime, Color.White.copy(alpha = .92f), Color.White, clock = true, modifier = modifier)
}

@Composable
private fun DotMatrix(
    value: String,
    label: String,
    color: Color,
    glowColor: Color,
    clock: Boolean,
    modifier: Modifier,
) {
    val matrix = remember(value, clock) { dotMatrix(value, clock) }
    val lights = remember { mutableStateOf(matrix.dots.map { DotLight(it, 1f, 1f) }) }
    val progress = remember { mutableFloatStateOf(1f) }

    LaunchedEffect(matrix) {
        val visible = lights.value.associate { it.dot to it.brightness(progress.floatValue) }
        val next = (visible.keys + matrix.dots).mapNotNull { dot ->
            val from = visible[dot] ?: 0f
            val to = if (dot in matrix.dots) 1f else 0f
            if (from == 0f && to == 0f) return@mapNotNull null
            val horizontal = (dot.column + matrix.columns / 2).toFloat() / (matrix.columns - 1).coerceAtLeast(1)
            val wave = .65f * horizontal.coerceIn(0f, 1f) + .35f * dot.row / 6f
            DotLight(dot, from, to, delay = wave * .36f)
        }
        if (next.all { it.from == it.to }) return@LaunchedEffect

        // Keep the visible light levels when a new value arrives mid-transition.
        lights.value = next
        progress.floatValue = 0f
        animate(0f, 1f, animationSpec = tween(210, easing = LinearEasing)) { fraction, _ ->
            progress.floatValue = fraction
        }
        lights.value = matrix.dots.map { DotLight(it, 1f, 1f) }
    }

    val padding = if (clock) 4 else 0
    val width = matrix.columns + padding
    val height = 7 + padding
    Canvas(
        modifier.aspectRatio(width.toFloat() / height, matchHeightConstraintsFirst = !clock)
            .semantics { contentDescription = label },
    ) {
        val pitch = minOf(size.width / width, size.height / height)
        val radius = pitch * if (clock) .16f else .22f
        val fraction = progress.floatValue
        for (light in lights.value) {
            val level = light.brightness(fraction)
            if (level <= .005f) continue
            val center = Offset(
                size.width / 2f + light.dot.column * pitch,
                size.height / 2f + (light.dot.row - 3) * pitch,
            )
            val halo = radius * 5f
            drawCircle(
                Brush.radialGradient(
                    0f to glowColor.copy(alpha = level * if (clock) .22f else .28f),
                    .3f to glowColor.copy(alpha = level * if (clock) .10f else .12f),
                    1f to Color.Transparent,
                    center = center, radius = halo,
                ),
                halo, center,
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(glowColor.copy(alpha = level * if (clock) .35f else .5f), Color.Transparent),
                    center, radius * 2f,
                ),
                radius * 2f, center,
            )
            drawCircle(color.copy(alpha = color.alpha * level), radius * (.75f + .25f * level), center)
        }
    }
}

private data class DotCell(val column: Int, val row: Int)

private data class DotLayout(val columns: Int, val dots: Set<DotCell>)

private data class DotLight(val dot: DotCell, val from: Float, val to: Float, val delay: Float = 0f) {
    fun brightness(progress: Float): Float {
        val swept = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
        // A small immediate response keeps fast dial changes visible as the sweep catches up.
        return from + (to - from) * (.2f * progress + .8f * swept)
    }
}

private fun dotMatrix(value: String, clock: Boolean): DotLayout {
    val glyphs = value.mapNotNull { character ->
        when (character) {
            '.' -> DotPoint.takeUnless { clock }
            ':' -> DotColon.takeIf { clock }
            '-' -> if (clock) DotDash else DotMinus
            ' ' -> DotSpace.takeUnless { clock }
            else -> character.digitToIntOrNull()?.let(DotDigits::get)
        }
    }
    val columns = (glyphs.sumOf { it.first().length } + glyphs.size - 1).coerceAtLeast(1)
    val dots = buildSet {
        var x = -columns / 2
        for (glyph in glyphs) {
            glyph.forEachIndexed { row, cells ->
                cells.forEachIndexed { column, cell ->
                    if (cell == '1') add(DotCell(x + column, row))
                }
            }
            x += glyph.first().length + 1
        }
    }
    return DotLayout(columns, dots)
}

private val DotColon = listOf("0", "0", "1", "0", "1", "0", "0")
private val DotDash = listOf("00000", "00000", "00000", "11111", "00000", "00000", "00000")
private val DotPoint = listOf("0", "0", "0", "0", "0", "0", "1")
private val DotMinus = listOf("000", "000", "000", "111", "000", "000", "000")
private val DotSpace = List(7) { "0" }

private val DotDigits = listOf(
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
