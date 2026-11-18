package app.geeflow.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import app.geeflow.ui.components.CoffeeEnterMillis
import app.geeflow.ui.components.CoffeeExitMillis
import app.geeflow.ui.components.CoffeeModalFrame
import app.geeflow.ui.components.CoffeeModalHost
import app.geeflow.ui.components.CoffeePopup
import app.geeflow.ui.components.DialogCaptureHost
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class NativeModalBackdropTest {
    @Test
    fun popupsBlurTheRetainedNativeSceneWithoutRegisteringGlassInItsWindow() = runComposeUiTest {
        mainClock.autoAdvance = false
        val host = RecordingMaterials()
        var shown by mutableStateOf(false)
        setContent {
            CompositionLocalProvider(LocalNativeCoffeeMaterials provides host) {
                MaterialTheme {
                    Box(Modifier.size(640.dp, 480.dp)) {
                        DialogCaptureHost {
                            CoffeeModalHost {
                                Box(Modifier.size(220.dp, 90.dp).testTag("background-glass").coffeeGlass())
                                if (shown) CoffeePopup(onDismissRequest = { shown = false }) {
                                    Box(Modifier.size(180.dp, 100.dp).testTag("popup-glass").coffeeGlass()) {
                                        Text("Popup content")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        val backgroundKey = host.frames.keys.single()
        val backgroundFrame = host.frames.getValue(backgroundKey)
        assertEquals(0f, host.blurProgress, .001f)

        repeat(3) {
            runOnIdle { shown = true }
            mainClock.advanceTimeBy(CoffeeEnterMillis.toLong() + 160)
            waitForIdle()
            onNodeWithTag("popup-glass").assertIsDisplayed()
            onNodeWithText("Popup content").assertIsDisplayed()
            runOnIdle {
                assertEquals(1f, host.blurProgress, .001f, "The UIKit background must blur with the modal")
                assertEquals(setOf(backgroundKey), host.frames.keys,
                    "A popup in another window must not add native glass to the main window")
                assertEquals(backgroundFrame, host.frames.getValue(backgroundKey),
                    "The background glass must remain in place instead of disappearing")
                assertEquals(1f, host.opacities.getValue(backgroundKey), .001f,
                    "Background panels remain present behind the native blur")
            }
            runOnIdle { shown = false }
            mainClock.advanceTimeBy(CoffeeExitMillis.toLong() + 160)
            waitForIdle()
            onNodeWithTag("popup-glass").assertDoesNotExist()
            runOnIdle {
                assertEquals(0f, host.blurProgress, .001f)
                assertEquals(setOf(backgroundKey), host.frames.keys,
                    "Repeated close/open must leave no orphan glass views")
            }
        }
    }

    @Test
    fun overlappingModalProgressUsesTheStrongestBlurAndDisposingTheHostClearsIt() = runComposeUiTest {
        mainClock.autoAdvance = false
        val host = RecordingMaterials()
        val strong = Animatable(1f)
        val weak = Animatable(.4f)
        var showStrong by mutableStateOf(true)
        var mounted by mutableStateOf(true)
        setContent {
            CompositionLocalProvider(LocalNativeCoffeeMaterials provides host) {
                MaterialTheme {
                    if (mounted) CoffeeModalHost {
                        Box(Modifier.size(200.dp, 80.dp).coffeeGlass())
                        if (showStrong) CoffeeModalFrame(progress = strong) { Text("First popup") }
                        CoffeeModalFrame(progress = weak) { Text("Second popup") }
                    }
                }
            }
        }
        waitForIdle()
        runOnIdle {
            assertEquals(1f, host.blurProgress, .001f)
            showStrong = false
        }
        mainClock.advanceTimeBy(CoffeeExitMillis.toLong() + 160)
        waitForIdle()
        runOnIdle {
            assertEquals(.4f, host.blurProgress, .001f,
                "Closing one popup must preserve the blur requested by another")
            mounted = false
        }
        mainClock.advanceTimeByFrame()
        waitForIdle()
        runOnIdle {
            assertEquals(0f, host.blurProgress, .001f,
                "Removing the host must not leave a blur effect over the next screen")
            assertTrue(host.frames.isEmpty())
        }
    }

    private data class Frame(val x: Double, val y: Double, val width: Double, val height: Double, val radius: Double)

    private class RecordingMaterials : NativeCoffeeMaterials {
        val frames = mutableMapOf<String, Frame>()
        val opacities = mutableMapOf<String, Float>()
        var blurProgress = 0f
        override fun setPhase(phase: Int) = Unit
        override fun update(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double) {
            frames[key] = Frame(x, y, width, height, radius)
        }
        override fun opacity(key: String, value: Float) { opacities[key] = value }
        override fun backdropBlur(progress: Float) { blurProgress = progress }
        override fun press(key: String, pressed: Boolean) = Unit
        override fun remove(key: String) { frames.remove(key); opacities.remove(key) }
    }
}
