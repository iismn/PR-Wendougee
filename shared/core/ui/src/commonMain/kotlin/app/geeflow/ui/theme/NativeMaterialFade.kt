package app.geeflow.ui.theme

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import app.geeflow.ui.components.CoffeeEnterMillis
import app.geeflow.ui.components.CoffeeExitMillis
import app.geeflow.ui.components.CoffeeMotionEasing

internal val LocalNativeMaterialOpacity = staticCompositionLocalOf<() -> Float> { { 1f } }

/** UIKit materials are outside Compose's layer tree, so they need the same fade explicitly. */
@Composable
fun NativeMaterialFade(
    scope: AnimatedVisibilityScope,
    enterMillis: Int = CoffeeEnterMillis,
    exitMillis: Int = CoffeeExitMillis,
    enterDelayMillis: Int = 0,
    fadeEasing: Easing = CoffeeMotionEasing,
    content: @Composable () -> Unit,
) {
    val opacity = scope.transition.animateFloat(transitionSpec = {
        if (targetState == EnterExitState.Visible) tween(enterMillis, enterDelayMillis, fadeEasing)
        else tween(exitMillis, easing = fadeEasing)
    }, label = "native-material-opacity") { if (it == EnterExitState.Visible) 1f else 0f }
    val readOpacity = remember(opacity) { { opacity.value } }
    NativeMaterialOpacity(readOpacity, content)
}

@Composable
fun NativeMaterialOpacity(opacity: () -> Float, content: @Composable () -> Unit) {
    val parent = LocalNativeMaterialOpacity.current
    val combined = remember(parent, opacity) { { parent() * opacity() } }
    CompositionLocalProvider(LocalNativeMaterialOpacity provides combined, content = content)
}
