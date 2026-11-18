package app.geeflow.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

internal val LocalCoffeeBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** The gradients sit behind the shared screens, so translucent tiles reveal the same light. */
@Composable
internal fun CoffeeBackdrop(
    phase: CoffeeDayPhase,
    modifier: Modifier = Modifier,
    ambient: CoffeeAmbientColors = coffeeAmbientColors(phase),
    content: @Composable BoxScope.() -> Unit,
) {
    val nativeMaterials = LocalNativeCoffeeMaterials.current
    if (nativeMaterials != null) {
        SideEffect {
            nativeMaterials.setPhase(phase.ordinal)
            nativeMaterials.setAmbientColors(ambient)
        }
        // UIKit draws the ambient layer below UIVisualEffectView. Drawing it in the
        // foreground Compose canvas would cover the native glass completely.
        CompositionLocalProvider(LocalCoffeeBackdrop provides null) {
            Box(modifier, content = content)
        }
        return
    }
    val base = animateColorAsState(ambient.base, tween(1_200), label = "ambientBase")
    val glow = animateColorAsState(ambient.glow, tween(1_200), label = "ambientGlow")
    val cool = animateColorAsState(ambient.cool, tween(1_200), label = "ambientCool")
    val backdrop = rememberLayerBackdrop()
    CompositionLocalProvider(LocalCoffeeBackdrop provides backdrop) {
        Box(modifier) {
            Box(Modifier.matchParentSize().layerBackdrop(backdrop).drawBehind {
                drawCoffeeAmbient(CoffeeAmbientColors(base.value, glow.value, cool.value))
            })
            content()
        }
    }
}

@Composable
fun CoffeeAmbientPreview(
    ambient: CoffeeAmbientColors,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.drawBehind { drawCoffeeAmbient(ambient) }, content = content)
}

private fun DrawScope.drawCoffeeAmbient(ambient: CoffeeAmbientColors) {
    drawRect(ambient.base)
    drawRect(Brush.radialGradient(
        colors = listOf(ambient.glow, Color.Transparent),
        center = Offset(size.width * .95f, size.height * .05f),
        radius = size.width * .82f,
    ))
    drawRect(Brush.radialGradient(
        colors = listOf(ambient.cool, Color.Transparent),
        center = Offset(size.width * .08f, size.height * .95f),
        radius = size.width * .68f,
    ))
}
