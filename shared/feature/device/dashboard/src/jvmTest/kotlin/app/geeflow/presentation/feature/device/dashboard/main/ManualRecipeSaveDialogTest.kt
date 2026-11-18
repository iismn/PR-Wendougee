package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import app.geeflow.ui.theme.GeeFlowThemePreview
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ManualRecipeSaveDialogTest {
    @Test
    fun typingNameAndDescriptionEnablesSaveWhileCancelOnlyDismisses() = runComposeUiTest {
        var model by mutableStateOf(DeviceDashboardViewState.Dialog.ManualRecipe())
        val events = mutableListOf<DeviceDashboardEvent>()
        val savedText = mutableListOf<Pair<String, String>>()
        setContent { GeeFlowThemePreview(false) {
            ManualRecipeSaveContent(model) { event ->
                events += event
                if (event == DeviceDashboardEvent.ManualRecipeSaveClicked) savedText += model.name to model.description
                model = when (event) {
                    is DeviceDashboardEvent.ManualRecipeNameChanged -> model.copy(name = event.value)
                    is DeviceDashboardEvent.ManualRecipeDescriptionChanged -> model.copy(description = event.value)
                    else -> model
                }
            }
        } }
        onNodeWithTag("manual-recipe-save").assertIsNotEnabled()
        onNodeWithTag("manual-recipe-name").performTextInput("Morning manual")
        onNodeWithTag("manual-recipe-description").performTextInput("Slow preinfusion\nSteady finish")
        waitForIdle()
        runOnIdle {
            assertEquals("Morning manual", model.name)
            assertEquals("Slow preinfusion\nSteady finish", model.description)
        }
        onNodeWithTag("manual-recipe-save").assertIsEnabled().performClick()
        runOnIdle {
            assertEquals(1, events.count { it == DeviceDashboardEvent.ManualRecipeSaveClicked })
            assertEquals(listOf("Morning manual" to "Slow preinfusion\nSteady finish"), savedText,
                "Save must observe the latest text in both fields")
        }
        onNodeWithTag("manual-recipe-cancel").performClick()
        runOnIdle {
            assertEquals(DeviceDashboardEvent.ManualRecipeCancelled, events.last())
            assertEquals(1, events.count { it == DeviceDashboardEvent.ManualRecipeSaveClicked }, "Cancel must not issue another save")
        }
    }

    @Test
    fun invalidRecordingCanBeCancelledAndSavingCannotBeEditedOrDismissed() = runComposeUiTest {
        var model by mutableStateOf(DeviceDashboardViewState.Dialog.ManualRecipe(name = "No volume",
            canSave = false, errorMessage = "No extraction recording"))
        val events = mutableListOf<DeviceDashboardEvent>()
        setContent { GeeFlowThemePreview(false) { ManualRecipeSaveContent(model, events::add) } }
        onNodeWithTag("manual-recipe-error").assertTextEquals("No extraction recording")
        onNodeWithTag("manual-recipe-save").assertIsNotEnabled()
        onNodeWithTag("manual-recipe-cancel").assertIsEnabled().performClick()
        runOnIdle {
            assertEquals(listOf<DeviceDashboardEvent>(DeviceDashboardEvent.ManualRecipeCancelled), events)
            model = model.copy(canSave = true, errorMessage = null, saving = true)
        }
        onNodeWithTag("manual-recipe-name").assertIsNotEnabled()
        onNodeWithTag("manual-recipe-description").assertIsNotEnabled()
        onNodeWithTag("manual-recipe-save").assertIsNotEnabled()
        onNodeWithTag("manual-recipe-cancel").assertIsNotEnabled()
        onNodeWithTag("manual-recipe-close").assertIsNotEnabled()
    }

    @Test
    fun theBarePlayIconBecomesARedSquareWithLocalGlowAndKeepsStopSemantics() = runComposeUiTest {
        var state by mutableStateOf(HomeManualState(ready = true, connected = true))
        val events = mutableListOf<DeviceDashboardEvent>()
        setContent { MaterialTheme(colorScheme = darkColorScheme(onSurface = Color.White)) {
            Box(Modifier.size(48.dp).background(Color.Black)) { ManualTransportButton(state, events::add) }
        } }
        val button = onNodeWithTag("home-manual-start-stop")
        val idle = button.captureToImage().toPixelMap()
        val cx = idle.width / 2
        fun brightness(c: Color) = maxOf(c.red, c.green, c.blue)
        assertTrue(brightness(idle[cx, (idle.height * .13f).toInt()]) < .01f,
            "The play glyph must have no filled circular button behind it")
        button.assertIsEnabled().performClick()
        runOnIdle { assertEquals(listOf<DeviceDashboardEvent>(DeviceDashboardEvent.DialStartClicked), events); state = state.copy(active = true) }
        val active = button.captureToImage().toPixelMap()
        val center = active[cx, active.height / 2]
        assertTrue(center.red > .9f && center.green < .4f && center.blue < .45f, "Stop must be a bright red square")
        assertTrue(active[cx, (active.height * .18f).toInt()].red > .02f, "The square must illuminate its immediate surroundings")
        assertTrue(brightness(active[1,1]) < .01f, "The surrounding button surface must stay clear")
        button.assertIsEnabled().performClick()
        runOnIdle {
            assertEquals(DeviceDashboardEvent.DialStopClicked, events.last())
            state = state.copy(operation = ManualOperation.Stopping)
        }
        button.assertIsNotEnabled()
    }
}
