package app.geeflow.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

internal val LocalCoffeeBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** The gradients sit behind the shared screens, so translucent tiles reveal the same light. */
@Composable
internal fun CoffeeBackdrop(
    phase: CoffeeDayPhase,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val base = animateColorAsState(coffeeColorScheme(phase).background, tween(1_200), label = "ambientBase")
    val glow = animateColorAsState(
        when (phase) {
            CoffeeDayPhase.Morning -> Color(0x99FF865D)
            CoffeeDayPhase.Day -> Color(0xBAFF5A36)
            CoffeeDayPhase.Evening -> Color(0xE61A3771)
            CoffeeDayPhase.Night -> Color(0xE60B2B68)
        }, tween(1_200), label = "ambientGlow",
    )
    val cool = animateColorAsState(
        if (phase.dark) Color(0xB80B142D) else Color(0xD9D8E9FF),
        tween(1_200), label = "ambientCool",
    )
    val backdrop = rememberLayerBackdrop()
    CompositionLocalProvider(LocalCoffeeBackdrop provides backdrop) {
        Box(modifier) {
            Box(Modifier.matchParentSize().layerBackdrop(backdrop).drawBehind {
                drawRect(base.value)
                drawRect(Brush.radialGradient(
                    colors = listOf(glow.value, Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.05f),
                    radius = size.width * 0.82f,
                ))
                drawRect(Brush.radialGradient(
                    colors = listOf(cool.value, Color.Transparent),
                    center = Offset(size.width * 0.08f, size.height * 0.95f),
                    radius = size.width * 0.68f,
                ))
            })
            content()
        }
    }
}
