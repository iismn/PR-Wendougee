package app.geeflow.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow

/** Captures the live ambient layer; text and chart data are drawn after the optical effects. */
@Composable
fun Modifier.coffeeGlass(
    shape: Shape = MaterialTheme.shapes.medium,
    prominent: Boolean = false,
): Modifier {
    val backdrop = LocalCoffeeBackdrop.current
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.1f
    val tint = if (dark) Color(0xFF071126) else Color.White
    val tintAlpha = if (prominent) {
        if (dark) 0.20f else 0.28f
    } else {
        if (dark) 0.48f else 0.62f
    }
    if (backdrop == null) return clip(shape).background(tint.copy(alpha = tintAlpha))
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(if (prominent) 18.dp.toPx() else 10.dp.toPx())
            lens(
                refractionHeight = if (prominent) 22.dp.toPx() else 10.dp.toPx(),
                refractionAmount = if (prominent) 44.dp.toPx() else 18.dp.toPx(),
                depthEffect = true,
            )
        },
        highlight = {
            Highlight(
                width = if (prominent) 1.5.dp else 0.7.dp,
                blurRadius = 0.6.dp,
                style = HighlightStyle.Default(
                    color = Color.White.copy(alpha = if (dark) 0.44f else 0.90f),
                    angle = 35f,
                ),
            )
        },
        shadow = {
            Shadow(radius = if (prominent) 20.dp else 8.dp, color = Color(0xFF10223F).copy(alpha = 0.09f))
        },
        onDrawSurface = {
            drawRect(tint.copy(alpha = tintAlpha))
            drawRect(Brush.linearGradient(listOf(
                Color.White.copy(alpha = if (dark) 0.045f else 0.24f),
                Color.Transparent,
                Color.White.copy(alpha = 0.025f),
            )))
        },
    ).clip(shape)
}
