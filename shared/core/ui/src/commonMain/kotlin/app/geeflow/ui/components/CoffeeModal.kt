package app.geeflow.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import app.geeflow.ui.theme.NativeMaterialOpacity
import app.geeflow.ui.theme.LocalNativeCoffeeMaterials

val CoffeeMotionEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
const val CoffeeEnterMillis = 320
const val CoffeeExitMillis = 240

private class ModalBackdropState(private val scope: CoroutineScope) {
    private val layers = mutableStateMapOf<Any, Float>()
    val progress: Float get() = layers.values.maxOrNull() ?: 0f
    fun update(key: Any, progress: Float) { layers[key] = progress }
    fun remove(key: Any) {
        val current = layers[key] ?: return
        scope.launch {
            Animatable(current).animateTo(0f, tween(CoffeeExitMillis, easing = CoffeeMotionEasing)) {
                layers[key] = value
            }
            layers.remove(key)
        }
    }
}

val LocalAppTextDensity = staticCompositionLocalOf<Density?> { null }

private val LocalModalBackdrop = staticCompositionLocalOf<ModalBackdropState?> { null }

/** Blur the screen underneath separate dialog scene layers, never the dialog's text. */
@Composable
fun CoffeeModalHost(content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = remember(scope) { ModalBackdropState(scope) }
    val inspection = LocalInspectionMode.current
    val nativeMaterials = LocalNativeCoffeeMaterials.current
    LaunchedEffect(state, nativeMaterials) {
        try {
            snapshotFlow { state.progress }.collect { nativeMaterials?.backdropBlur(it) }
        } finally {
            // Clear after the collector stops, so a queued frame cannot restore a stale blur.
            nativeMaterials?.backdropBlur(0f)
        }
    }
    CompositionLocalProvider(LocalModalBackdrop provides state) {
        Box(Modifier.fillMaxSize().graphicsLayer {
            val radius = if (inspection) 0f else 18.dp.toPx() * state.progress
            renderEffect = if (radius > 0.01f) BlurEffect(radius, radius, TileMode.Clamp) else null
        }) { content() }
    }
}

/** Shared entrance and backdrop for navigation popups and ordinary dialogs. */
@Composable
fun CoffeeModalFrame(
    progress: Animatable<Float, AnimationVector1D>? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val entrance = progress ?: remember { Animatable(0f) }
    val backdrop = LocalModalBackdrop.current
    val key = remember { Any() }
    DisposableEffect(backdrop, key) {
        onDispose { backdrop?.remove(key) }
    }
    LaunchedEffect(entrance, backdrop, key) {
        snapshotFlow { entrance.value }.collect { backdrop?.update(key, it) }
    }
    LaunchedEffect(entrance) {
        if (progress == null) entrance.animateTo(1f, tween(CoffeeEnterMillis, easing = CoffeeMotionEasing))
    }
    CompositionLocalProvider(LocalDensity provides (LocalAppTextDensity.current ?: LocalDensity.current)) {
    NativeMaterialOpacity({ entrance.value }) {
    Box(modifier.graphicsLayer {
        val value = entrance.value
        alpha = value
        scaleX = 0.96f + 0.04f * value
        scaleY = scaleX
        translationY = 16.dp.toPx() * (1f - value)
    }) {
        // Dialogs own a separate window. Their local bounds must never become root UIKit glass.
        CompositionLocalProvider(LocalNativeCoffeeMaterials provides null) { content() }
    }
    }
    }
}


@Composable
fun CoffeePopup(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    progress: Animatable<Float, AnimationVector1D>? = null,
    content: @Composable () -> Unit,
) {
    val capture = LocalDialogCapture.current
    if (capture != null) {
        CaptureDialog(capture) { CoffeeModalFrame(progress, modifier, content) }
    } else {
        Dialog(onDismissRequest, platformModalProperties(properties, progress != null)) {
            if (!properties.usePlatformDefaultWidth) {
                var panelBounds by remember { mutableStateOf<Rect?>(null) }
                val dismiss by rememberUpdatedState(onDismissRequest)
                EdgeSwipeBack(enabled = true, onBack = onDismissRequest, modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().pointerInput(properties.dismissOnClickOutside) {
                        if (properties.dismissOnClickOutside) detectTapGestures { position ->
                            if (panelBounds?.contains(position) == false) dismiss()
                        }
                    }, contentAlignment = Alignment.Center) {
                        CoffeeModalFrame(progress, modifier.onGloballyPositioned { panelBounds = it.boundsInParent() }, content)
                    }
                }
            } else CoffeeModalFrame(progress, modifier, content)
        }
    }
}

internal expect fun platformModalProperties(properties: DialogProperties, managedTransition: Boolean): DialogProperties
