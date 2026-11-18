package app.geeflow.presentation.feature.intro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.ui.components.*
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Logo
import app.geeflow.ui.modifier.geeFlowInsetsPadding
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.core.ui.generated.resources.common_user_name
import geeflow.shared.feature.intro.generated.resources.Res
import geeflow.shared.feature.intro.generated.resources.intro_screen_set_up
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import geeflow.shared.core.ui.generated.resources.Res as UiRes

@Composable
fun IntroScreen(navigator: Navigator) {
    val viewModel = koinViewModel<IntroViewModel>()
    IntroScreenContent(onEvent = viewModel::handleEvent)
    NavigatorEffect(navigator, viewModel.navEvent)
}

@Composable
internal fun IntroScreenContent(onEvent: (IntroEvent) -> Unit = {}, initialFormShown: Boolean = false) = CoffeeWorkspaceTheme {
    var formShown by rememberSaveable { mutableStateOf(initialFormShown) }
    Box(Modifier.fillMaxSize().geeFlowInsetsPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 440.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(rememberVectorPainter(GeeFlowIcon.Logo), contentDescription = null,
                modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(24.dp))
            Text("Wendougee", fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center, maxLines = 1)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { formShown = true }, modifier = Modifier.testTag("intro-create-profile").height(CoffeeUi.touch)) {
                Text(stringResource(Res.string.intro_screen_set_up), style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (formShown) GeeFlowDialog(onDismissRequest = { formShown = false }) {
        Column(Modifier.widthIn(max = 400.dp).fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(24.dp)) {
            GeeFlowDialogTopBar(stringResource(Res.string.intro_screen_set_up), onCloseClick = { formShown = false })
            Spacer(Modifier.height(20.dp))
            Form(onEvent)
        }
    }
}

@Composable
private fun Form(onEvent: (IntroEvent) -> Unit) {
    val name = rememberTextFieldState()
    var agreed by rememberSaveable { mutableStateOf(false) }
    val canConfirm = name.text.isNotEmpty() && agreed
    val confirm = { if (canConfirm) onEvent(IntroEvent.ConfirmClicked(name.text.toString())) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GeeFlowOutlinedTextField(
            state = name, modifier = Modifier.testTag("intro-name").fillMaxWidth(),
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            onKeyboardAction = { confirm() },
            autoFocus = true,
            label = { Text(stringResource(UiRes.string.common_user_name)) },
        )
        GeeFlowAgreementCheckbox(checked = agreed, onCheckedChange = { agreed = it },
            modifier = Modifier.testTag("intro-agreement").fillMaxWidth())
        Button(onClick = confirm, enabled = canConfirm,
            modifier = Modifier.testTag("intro-confirm").height(CoffeeUi.touch).widthIn(min = 120.dp)) {
            Icon(Icons.Filled.Check, null, Modifier.size(CoffeeUi.icon))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(UiRes.string.common_confirm))
        }
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
fun IntroDesignPreview(formShown: Boolean = false) = IntroScreenContent(initialFormShown = formShown)
