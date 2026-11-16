package app.geeflow.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow

/** Standard content material: no glass rim or lens over graphs and editing content. */
@Composable
fun Modifier.coffeeSurface(shape: Shape = MaterialTheme.shapes.medium): Modifier {
    val backdrop = LocalCoffeeBackdrop.current
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.1f
    val fill = if (dark) Color(0xFF101723).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.78f)
    if (backdrop == null) return clip(shape).background(fill)
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = { blur(16.dp.toPx()) },
        highlight = null,
        shadow = null,
        onDrawSurface = { drawRect(fill) },
    ).clip(shape)
}

/**
 * Regular glass for navigation and controls, following Apple's material hierarchy.
 * This shared Compose renderer samples the live backdrop; it is not a UIKit effect.
 * Text-bearing sidebars use more frosting. Children use flat selection fills so
 * glass is never stacked on glass. The narrow optical edge is not a painted border.
 */
@Composable
fun Modifier.coffeeGlass(
    shape: Shape = MaterialTheme.shapes.medium,
    prominent: Boolean = false,
): Modifier {
    val backdrop = LocalCoffeeBackdrop.current
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.1f
    val tint = if (dark) Color(0xFF101722) else Color(0xFFFAFCFF)
    val tintAlpha = if (prominent) { if (dark) 0.58f else 0.56f } else { if (dark) 0.38f else 0.36f }
    var pressed by remember { mutableStateOf(false) }
    val illumination by animateFloatAsState(if (pressed) 1f else 0f, tween(180), label = "glassContact")
    if (backdrop == null) return clip(shape).background(tint.copy(alpha = tintAlpha))
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(if (prominent) 28.dp.toPx() else 18.dp.toPx())
            lens(
                refractionHeight = 3.dp.toPx(),
                refractionAmount = (5 + 2 * illumination).dp.toPx(),
                depthEffect = false,
            )
        },
        highlight = {
            Highlight(
                width = 0.35.dp,
                blurRadius = 0.65.dp,
                style = HighlightStyle.Default(
                    color = Color.White.copy(alpha = (if (dark) 0.16f else 0.22f) + illumination * 0.10f),
                    blendMode = BlendMode.SrcOver,
                    angle = 45f,
                    falloff = 2f,
                ),
            )
        },
        shadow = {
            Shadow(radius = if (prominent) 16.dp else 8.dp,
                color = Color.Black.copy(alpha = if (dark) 0.20f else 0.08f))
        },
        onDrawSurface = {
            drawRect(tint.copy(alpha = tintAlpha))
            if (illumination > 0f) drawRect(Color.White.copy(alpha = illumination * 0.06f))
        },
    ).clip(shape).pointerInput(Unit) {
        // Observe contact without consuming scrolling, swipes or button events.
        awaitPointerEventScope {
            while (true) pressed = awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed }
        }
    }
}
