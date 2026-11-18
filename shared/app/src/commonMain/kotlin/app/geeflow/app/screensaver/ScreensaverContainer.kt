package app.geeflow.app.screensaver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import app.geeflow.ui.components.LocalShowScreensaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.Duration

@Composable
internal fun ScreensaverContainer(
    enabled: Boolean,
    timeoutMinutes: Int,
    clockSizePercent: Int = 100,
    modifier: Modifier = Modifier,
    timeout: Duration = timeoutMinutes.coerceAtLeast(1).minutes,
    timeSource: TimeSource = TimeSource.Monotonic,
    content: @Composable () -> Unit,
) {
    var isScreensaverVisible by remember { mutableStateOf(false) }
    var lastInteraction by remember(timeSource) { mutableStateOf(timeSource.markNow()) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, timeSource) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lastInteraction = timeSource.markNow()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(enabled, timeout, timeSource) {
        if (!enabled) {
            isScreensaverVisible = false
            return@LaunchedEffect
        }
        while (true) {
            delay(1.seconds)
            if (!isScreensaverVisible && lastInteraction.elapsedNow() >= timeout) {
                isScreensaverVisible = true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled, timeSource) {
                    if (!enabled) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            // Mouse/stylus hover and telemetry-driven redraws are not user activity.
                            if (event.changes.any { it.pressed || it.previousPressed }) {
                                lastInteraction = timeSource.markNow()
                            }
                        }
                    }
                },
        ) {
            CompositionLocalProvider(LocalShowScreensaver provides { isScreensaverVisible = true }) { content() }
        }

        AnimatedVisibility(
            visible = isScreensaverVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Screensaver(
                sizePercent = clockSizePercent,
                onDismiss = {
                    lastInteraction = timeSource.markNow()
                    isScreensaverVisible = false
                },
            )
        }
    }
}
