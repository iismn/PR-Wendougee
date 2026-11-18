package app.geeflow.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import app.geeflow.ui.theme.*
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TextSizeTest {
    @Test
    fun appAndPopupKeepTheChosenSizeAndSystemTextScaling() = runComposeUiTest {
        var screenScale = 0f
        var popupScale = 0f
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, 1.25f)) {
                GeeFlowTheme(false, coffeeColorScheme(CoffeeDayPhase.Day), textSizePercent = 140) {
                    screenScale = LocalDensity.current.fontScale
                    // Platform popup windows provide their own density. Restore the app scale.
                    CompositionLocalProvider(LocalDensity provides Density(2f, 1f)) {
                        CoffeeModalFrame { popupScale = LocalDensity.current.fontScale }
                    }
                }
            }
        }
        runOnIdle {
            assertEquals(1.75f, screenScale)
            assertEquals(screenScale, popupScale)
        }
    }
}
