package app.geeflow.ui.theme

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CoffeeGlassTest {
    @Test
    fun contactLightingDoesNotInterceptRecipeTapsOrScrolling() = runComposeUiTest {
        var selections = 0
        lateinit var scroll: ScrollState
        setContent {
            MaterialTheme {
                CoffeeBackdrop(CoffeeDayPhase.Day) {
                    scroll = rememberScrollState()
                    Column(Modifier.size(240.dp, 160.dp).coffeeGlass(prominent = true)
                        .testTag("library").verticalScroll(scroll)) {
                        repeat(10) { index ->
                            Text("Recipe $index", Modifier.fillMaxWidth().height(48.dp)
                                .testTag("recipe-$index").clickable { selections++ })
                        }
                    }
                }
            }
        }
        onNodeWithTag("recipe-0").performTouchInput { click() }
        runOnIdle { assertEquals(1, selections) }
        onNodeWithTag("library").performTouchInput { swipeUp() }
        runOnIdle {
            assertTrue(scroll.value > 0, "The glass lighting observer must allow the library to scroll")
            assertEquals(1, selections, "Scrolling must not select another recipe")
        }
    }
}
