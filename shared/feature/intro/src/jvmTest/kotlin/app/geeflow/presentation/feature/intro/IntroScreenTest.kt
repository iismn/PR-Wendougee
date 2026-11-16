package app.geeflow.presentation.feature.intro

import androidx.compose.ui.test.*
import app.geeflow.ui.components.DialogCaptureHost
import app.geeflow.ui.theme.GeeFlowThemePreview
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class IntroScreenTest {
    @Test fun createProfileOpensTheFormAndRequiresNameAndAgreement() = runComposeUiTest {
        val events = mutableListOf<IntroEvent>()
        setContent { GeeFlowThemePreview(false) { DialogCaptureHost { IntroScreenContent(events::add) } } }
        onNodeWithTag("intro-name").assertDoesNotExist()
        onNodeWithTag("intro-create-profile").performClick()
        onNodeWithTag("intro-confirm").assertIsNotEnabled()
        onNodeWithTag("intro-name").performTextInput("LITA")
        onNodeWithTag("intro-name").assertTextContains("LITA")
        onNodeWithTag("intro-confirm").assertIsNotEnabled()
        onNode(isToggleable()).assertIsOff().performClick()
        onNode(isToggleable()).assertIsOn()
        onNodeWithTag("intro-confirm").assertIsEnabled().performClick()
        runOnIdle { assertEquals(listOf<IntroEvent>(IntroEvent.ConfirmClicked("LITA")), events) }
    }
}
