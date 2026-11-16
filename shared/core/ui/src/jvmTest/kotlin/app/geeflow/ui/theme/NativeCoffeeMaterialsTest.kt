package app.geeflow.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NativeCoffeeMaterialsTest {
    @Test
    fun nativeMaterialTracksLayoutContactAndDisposalWithoutInterceptingClicks() = runComposeUiTest {
        val frames = mutableMapOf<String, List<Double>>()
        val contact = mutableListOf<Boolean>()
        var reportedPhase = -1
        var clicked = 0
        var shown by mutableStateOf(true)
        val host = object : NativeCoffeeMaterials {
            override fun setPhase(phase: Int) { reportedPhase = phase }
            override fun update(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double) {
                frames[key] = listOf(x, y, width, height, radius)
            }
            override fun press(key: String, pressed: Boolean) { contact += pressed }
            override fun remove(key: String) { frames.remove(key) }
        }
        setContent {
            CompositionLocalProvider(LocalNativeCoffeeMaterials provides host) {
                MaterialTheme {
                    CoffeeBackdrop(CoffeeDayPhase.Day) {
                        if (shown) Box(Modifier.size(200.dp, 80.dp).coffeeGlass().testTag("glass").clickable { clicked++ })
                    }
                }
            }
        }
        runOnIdle {
            assertEquals(CoffeeDayPhase.Day.ordinal, reportedPhase)
            assertEquals(1, frames.size)
            assertEquals(200.0, frames.values.single()[2], 0.01)
            assertEquals(80.0, frames.values.single()[3], 0.01)
        }
        onNodeWithTag("glass").performTouchInput { click() }
        runOnIdle { assertEquals(1, clicked); assertTrue(true in contact); assertTrue(false in contact); shown = false }
        runOnIdle { assertTrue(frames.isEmpty()) }
    }
}
