package app.geeflow.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import app.geeflow.ui.animations.BackwardTransition
import app.geeflow.ui.animations.ForwardTransition
import app.geeflow.ui.animations.NavigationEnterMillis
import app.geeflow.ui.animations.NavigationExitMillis
import app.geeflow.ui.animations.NavigationFadeEasing
import app.geeflow.ui.animations.paneSwapTransition
import app.geeflow.ui.animations.SwapEnterMillis
import app.geeflow.ui.animations.SwapExitMillis
import app.geeflow.ui.animations.SwapEnterDelayMillis
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class NativeMaterialFadeTest {
    @Test
    fun incomingGlassStartsHiddenAndRapidSwapsLeaveOnlyTheSettledLayer() = runComposeUiTest {
        mainClock.autoAdvance = false
        var page by mutableStateOf(false)
        val host = RecordingMaterials()
        setContent {
            CompositionLocalProvider(LocalNativeCoffeeMaterials provides host) { MaterialTheme {
                AnimatedContent(page, transitionSpec = { paneSwapTransition(forward = targetState) }) {
                    NativeMaterialFade(this, SwapEnterMillis, SwapExitMillis, SwapEnterDelayMillis) {
                        Box(Modifier.size(if (it) 240.dp else 200.dp, 80.dp).coffeeGlass())
                    }
                }
            } }
        }
        waitForIdle()
        val oldKey = host.frames.single()
        runOnIdle { assertEquals(1f, host.opacity.getValue(oldKey)); page = true }
        mainClock.advanceTimeByFrame()
        waitForIdle()
        val incoming = host.frames.single { it != oldKey }
        runOnIdle { assertEquals(0f,host.firstFrameOpacity.getValue(incoming), "A newly created UIKit view must not flash at full opacity") }
        mainClock.advanceTimeBy(70)
        waitForIdle()
        runOnIdle { assertTrue(host.opacity.getValue(oldKey) < 1f) }
        repeat(8) {
            runOnIdle { page = !page }
            mainClock.advanceTimeBy(40)
        }
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        runOnIdle {
            assertEquals(1,host.frames.size, "Outgoing native glass must be disposed after repeated history/back transitions")
            assertEquals(host.frames,host.opacity.keys)
            assertEquals(1f,host.opacity.values.single())
        }
    }

    @Test
    fun foregroundGlassSlidesInAndOutWhileHomeRemainsAnchored() = runComposeUiTest {
        mainClock.autoAdvance = false
        var page by mutableStateOf(false)
        val host = RecordingMaterials()
        setContent {
            CompositionLocalProvider(LocalNativeCoffeeMaterials provides host) {
                MaterialTheme {
                    AnimatedContent(page, transitionSpec = {
                        if (targetState) ForwardTransition
                        else BackwardTransition.apply { targetContentZIndex = -1f }
                    }) {
                        NativeMaterialFade(this, NavigationEnterMillis, NavigationExitMillis, fadeEasing = NavigationFadeEasing) {
                            Box(Modifier.size(if (it) 320.dp else 300.dp, 80.dp).coffeeGlass())
                        }
                    }
                }
            }
        }
        waitForIdle()
        val home = host.frames.single()
        val homeX = host.position.getValue(home)

        runOnIdle { page = true }
        mainClock.advanceTimeBy(96)
        waitForIdle()
        val foreground = host.frames.single { it != home }
        runOnIdle {
            assertEquals(homeX, host.position.getValue(home))
            assertTrue(host.position.getValue(foreground) > homeX)
            assertTrue(host.opacity.getValue(foreground) in 0f..<1f)
        }
        mainClock.advanceTimeBy(NavigationEnterMillis.toLong())
        waitForIdle()
        val foregroundX = host.position.getValue(foreground)

        runOnIdle { page = false }
        mainClock.advanceTimeBy(160)
        waitForIdle()
        runOnIdle {
            val returningHome = host.frames.single { it != foreground }
            assertEquals(homeX, host.position.getValue(returningHome))
            assertTrue(host.position.getValue(foreground) > foregroundX)
            assertTrue(host.opacity.getValue(foreground) in 0f..<1f)
        }
        mainClock.advanceTimeBy(NavigationExitMillis.toLong())
        waitForIdle()
        runOnIdle {
            assertEquals(homeX, host.position.values.single())
            assertEquals(1f, host.opacity.values.single())
        }
    }

    private class RecordingMaterials : NativeCoffeeMaterials {
        val frames = mutableSetOf<String>()
        val opacity = mutableMapOf<String,Float>()
        val position = mutableMapOf<String, Double>()
        val firstFrameOpacity = mutableMapOf<String,Float>()
        override fun setPhase(phase: Int) = Unit
        override fun update(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double) {
            position[key] = x
            if (frames.add(key)) firstFrameOpacity[key] = opacity[key] ?: 1f
        }
        override fun opacity(key: String, value: Float) { opacity[key] = value }
        override fun press(key: String, pressed: Boolean) = Unit
        override fun remove(key: String) {
            frames.remove(key)
            opacity.remove(key)
            position.remove(key)
        }
    }
}
