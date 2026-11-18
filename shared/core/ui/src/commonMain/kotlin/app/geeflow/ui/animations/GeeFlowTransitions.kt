package app.geeflow.ui.animations

import app.geeflow.ui.components.CoffeeMotionEasing
import app.geeflow.ui.components.CoffeeEnterMillis
import app.geeflow.ui.components.CoffeeExitMillis
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

private const val EnterDurationMillis = CoffeeEnterMillis
private const val ExitDurationMillis = CoffeeExitMillis
private const val SlideFraction = 14
private const val PageSlideFraction = 32
private const val PopSlideFraction = 8
const val SwapEnterMillis = 220
const val SwapExitMillis = 180
const val SwapEnterDelayMillis = 135
const val NavigationEnterMillis = 340
const val NavigationExitMillis = 420
val NavigationFadeEasing = CubicBezierEasing(0.42f, 0f, 0.68f, 1f)

/** Let the outgoing charts or dial mostly fade before displaying the next pane. */
fun paneSwapTransition(forward: Boolean): ContentTransform =
    pageTransition(forward, SwapEnterMillis, SwapExitMillis, SwapEnterDelayMillis).apply {
        targetContentZIndex = if (forward) 1f else -1f
    }

val ForwardTransition: ContentTransform
    get() = pageTransition(forward = true, fadeEasing = NavigationFadeEasing)

val BackwardTransition: ContentTransform
    get() = pageTransition(
        forward = false,
        slideFraction = PopSlideFraction,
        fadeEasing = NavigationFadeEasing,
    )

/** Navigation progress is the fraction of the page width travelled by the finger. */
fun predictiveBackTransition(fromRight: Boolean): ContentTransform =
    fadeIn(tween(NavigationEnterMillis, easing = NavigationFadeEasing)) togetherWith
        (fadeOut(tween(NavigationExitMillis, easing = NavigationFadeEasing)) +
            slideOutHorizontally(tween(NavigationExitMillis, easing = LinearEasing)) {
                if (fromRight) -it else it
            })

private fun pageTransition(
    forward: Boolean,
    enterMillis: Int = NavigationEnterMillis,
    exitMillis: Int = NavigationExitMillis,
    enterDelayMillis: Int = 0,
    fadeEasing: Easing = CoffeeMotionEasing,
    slideFraction: Int = PageSlideFraction,
): ContentTransform {
    val enter = fadeIn(tween(enterMillis, enterDelayMillis, fadeEasing))
    val exit = fadeOut(tween(exitMillis, easing = fadeEasing))
    // Only the foreground page moves, so the home dial stays anchored.
    return if (forward) {
        val slide = slideInHorizontally(tween(enterMillis, enterDelayMillis)) { it / slideFraction }
        (enter + slide) togetherWith exit
    } else {
        val slide = slideOutHorizontally(tween(exitMillis)) { it / slideFraction }
        enter togetherWith (exit + slide)
    }
}

fun slideTransform(enterFromRight: Boolean): ContentTransform {
    val direction = if (enterFromRight) 1 else -1
    val enter = fadeIn(tween(EnterDurationMillis, easing = CoffeeMotionEasing)) +
        slideInHorizontally(tween(EnterDurationMillis, easing = CoffeeMotionEasing)) { direction * it / SlideFraction }
    val exit = fadeOut(tween(ExitDurationMillis, easing = CoffeeMotionEasing)) +
        slideOutHorizontally(tween(ExitDurationMillis, easing = CoffeeMotionEasing)) { -direction * it / SlideFraction }
    return enter togetherWith exit
}
