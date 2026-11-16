package app.geeflow.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BoilerControlCardTest {
    @Test
    fun arcTouchAndAccessibilityRespectTheBoilerRange() = runComposeUiTest {
        var requested = 94f
        setContent { MaterialTheme {
            ArcTemperatureDial(94f, 80f..100f, "Brew", { requested = it },
                Modifier.size(360.dp, 144.dp).testTag("arc"))
        } }
        onNodeWithTag("arc").performTouchInput { click(Offset(0f, 100f)) }
        runOnIdle { assertEquals(80f, requested) }
        onNodeWithTag("arc").performTouchInput { click(Offset(width.toFloat() - 1, 100f)) }
        runOnIdle { assertEquals(100f, requested) }
        onNodeWithTag("arc").performTouchInput { click(Offset(center.x, 20f)) }
        runOnIdle { assertEquals(90f, requested, 0.1f) }
        onNodeWithTag("arc").performSemanticsAction(SemanticsActions.SetProgress) { it(400f) }
        runOnIdle { assertEquals(100f, requested) }
        onNodeWithTag("arc").performSemanticsAction(SemanticsActions.SetProgress) { it(Float.NaN) }
        runOnIdle { assertEquals(100f, requested) }
    }

    @Test
    fun disabledBoilerDoesNotAcceptTemperatureChanges() = runComposeUiTest {
        var requests = 0
        setContent { MaterialTheme {
            ArcTemperatureDial(125f, 110f..140f, "Steam", { requests++ },
                Modifier.size(360.dp, 144.dp).testTag("arc"), enabled = false)
        } }
        onNodeWithTag("arc").assertIsNotEnabled().performTouchInput { click() }
        onNodeWithTag("arc").performSemanticsAction(SemanticsActions.SetProgress) { it(140f) }
        runOnIdle { assertEquals(0, requests) }
    }

    @Test
    fun cardSnapsToMachineSuppliedValuesIncludingFahrenheit() = runComposeUiTest {
        val requests = mutableListOf<String>()
        setContent { MaterialTheme {
            BoilerControlCard("Brew", 200f, "200", listOf("194", "197", "200", "203"),
                checked = true, onCheckedChanged = {}, onValueChanged = { requests += it },
                modifier = Modifier.size(430.dp, 420.dp))
        } }
        val dial = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        dial.performSemanticsAction(SemanticsActions.SetProgress) { it(198.5f) }
        dial.performSemanticsAction(SemanticsActions.SetProgress) { it(400f) }
        runOnIdle {
            assertTrue(requests.all { it in listOf("194", "197", "200", "203") })
            assertEquals("203", requests.last())
        }
    }
}
