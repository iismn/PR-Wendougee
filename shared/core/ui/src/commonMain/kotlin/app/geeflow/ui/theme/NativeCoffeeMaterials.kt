package app.geeflow.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
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
    fun setAmbientColors(colors: CoffeeAmbientColors) = Unit
    fun update(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double)
    fun press(key: String, pressed: Boolean)
    fun remove(key: String)
    fun opacity(key: String, value: Float) = Unit
    /** Matches the modal blur on Compose content without removing the underlying UIKit panels. */
    fun backdropBlur(progress: Float) = Unit
    fun mask(key: String, path: String) = Unit
}

/** Compact M/L/Q/Z path in density-normalized local points; iOS masks the actual blur view. */
interface NativeMaterialShape : Shape {
    fun nativeMask(size: Size, density: Density): String
}

val LocalNativeCoffeeMaterials = staticCompositionLocalOf<NativeCoffeeMaterials?> { null }

private var nextMaterialId = 0L

@Composable
internal fun Modifier.nativeCoffeeGlass(host: NativeCoffeeMaterials, shape: Shape): Modifier {
    val key = remember(host) { "glass-${nextMaterialId++}" }
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val opacity = LocalNativeMaterialOpacity.current
    var materialSize by remember { mutableStateOf(IntSize.Zero) }
    DisposableEffect(host, key) { onDispose { host.remove(key) } }
    SideEffect {
        host.opacity(key, opacity())
        host.mask(key, (shape as? NativeMaterialShape)?.nativeMask(Size(materialSize.width.toFloat(), materialSize.height.toFloat()), density).orEmpty())
    }
    LaunchedEffect(host, key, opacity) {
        snapshotFlow { opacity() }.collect { host.opacity(key, it) }
    }
    return onGloballyPositioned { coordinates ->
        materialSize = coordinates.size
        val bounds = coordinates.boundsInWindow(clipBounds = false)
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
