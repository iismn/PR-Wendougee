package app.geeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class GeeFlowPaneScaffoldsTest {
    @Test
    fun embeddedSettingsScrollInAViewportBelowTheFixedTitle() = runComposeUiTest {
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(surfaceContainerLow = Color.White)) {
                GeeFlowDetailScaffold(title = "Boiler settings", expanded = true) { padding ->
                    Column(
                        Modifier.fillMaxSize().testTag("settings-viewport")
                            .verticalScroll(rememberScrollState()).padding(padding),
                    ) {
                        Box(Modifier.fillMaxWidth().height(300.dp).background(Color.Red).testTag("first-option"))
                        Text("Second option")
                        Box(Modifier.height(1800.dp))
                    }
                }
            }
        }
        val header = onNodeWithText("Boiler settings")
        val viewport = onNodeWithTag("settings-viewport")
        val initialHeader = header.getUnclippedBoundsInRoot()
        val initialViewport = viewport.getUnclippedBoundsInRoot()
        assertTrue(initialViewport.top > initialHeader.bottom)

        val pixels = viewport.captureToImage().toPixelMap()
        val x = pixels.width / 2
        fun greenAt(dp: Int) = pixels[x, (dp * density.density).toInt()].green
        assertTrue(greenAt(1) > .90f, "The scroll edge must blend into the opaque header")
        assertTrue(greenAt(14) in .2f..0.8f, "The scrolling content must fade through the boundary")
        assertTrue(greenAt(32) < .08f, "Content below the boundary must retain full color")

        val option = onNodeWithText("Second option")
        val initialOption = option.getUnclippedBoundsInRoot().top
        viewport.performTouchInput { swipeUp() }
        assertEquals(initialHeader, header.getUnclippedBoundsInRoot())
        assertEquals(initialViewport, viewport.getUnclippedBoundsInRoot())
        onNodeWithTag("first-option").assertIsNotDisplayed()
        assertTrue(option.getUnclippedBoundsInRoot().top < initialOption)
    }
}
