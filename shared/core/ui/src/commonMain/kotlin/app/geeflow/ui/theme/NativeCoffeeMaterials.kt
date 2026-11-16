package app.geeflow.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection

/** Implemented by the iOS host. Bounds are window coordinates in UIKit points. */
interface NativeCoffeeMaterials {
    fun setPhase(phase: Int)
    fun update(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double)
    fun press(key: String, pressed: Boolean)
    fun remove(key: String)
}

val LocalNativeCoffeeMaterials = staticCompositionLocalOf<NativeCoffeeMaterials?> { null }

private var nextMaterialId = 0L

@Composable
internal fun Modifier.nativeCoffeeGlass(host: NativeCoffeeMaterials, shape: Shape): Modifier {
    val key = remember(host) { "glass-${nextMaterialId++}" }
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    DisposableEffect(host, key) { onDispose { host.remove(key) } }
    return onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        val outline = shape.createOutline(
            Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()), direction, density,
        )
        val radius = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
        val scale = density.density.toDouble()
        host.update(key, bounds.left / scale, bounds.top / scale,
            bounds.width / scale, bounds.height / scale, radius / scale)
    }.clip(shape).pointerInput(host, key) {
        try {
            awaitPointerEventScope {
                var wasPressed = false
                while (true) {
                    val pressed = awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed }
                    if (pressed != wasPressed) {
                        wasPressed = pressed
                        host.press(key, pressed)
                    }
                }
            }
        } finally { host.press(key, false) }
    }
}
