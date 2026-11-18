package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import app.geeflow.ui.components.CoffeePopup
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.CoffeeWorkspaceTheme
import geeflow.shared.core.ui.generated.resources.common_cancel
import geeflow.shared.core.ui.generated.resources.common_close
import geeflow.shared.core.ui.generated.resources.common_save
import geeflow.shared.feature.device.dashboard.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun ManualRecipeSaveDialog(model: DeviceDashboardViewState.Dialog.ManualRecipe,
    onEvent: (DeviceDashboardEvent) -> Unit) {
    val dismiss = { if (!model.saving) onEvent(DeviceDashboardEvent.ManualRecipeCancelled) }
    CoffeePopup(onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnBackPress = !model.saving, dismissOnClickOutside = !model.saving),
        modifier = Modifier.padding(24.dp)) {
        ManualRecipeSaveContent(model, onEvent)
    }
}

/** Uses state-based inputs so the iPad's native keyboard keeps its editing session. */
@Composable
internal fun ManualRecipeSaveContent(model: DeviceDashboardViewState.Dialog.ManualRecipe,
    onEvent: (DeviceDashboardEvent) -> Unit) = CoffeeWorkspaceTheme {
    val name = rememberTextFieldState(initialText = model.name)
    val description = rememberTextFieldState(initialText = model.description)
    val currentModel by rememberUpdatedState(model)
    val event by rememberUpdatedState(onEvent)
    LaunchedEffect(name) {
        snapshotFlow { name.text.toString() }.collect {
            if (it != currentModel.name) event(DeviceDashboardEvent.ManualRecipeNameChanged(it))
        }
    }
    LaunchedEffect(description) {
        snapshotFlow { description.text.toString() }.collect {
            if (it != currentModel.description) event(DeviceDashboardEvent.ManualRecipeDescriptionChanged(it))
        }
    }
    Column(Modifier.testTag("manual-recipe-dialog").widthIn(max = 600.dp).fillMaxWidth()
        .clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface)
        .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.home_manual_recipe_save),
                style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { onEvent(DeviceDashboardEvent.ManualRecipeCancelled) }, enabled = !model.saving,
                modifier = Modifier.testTag("manual-recipe-close")) {
                Icon(Icons.Default.Close, stringResource(CoreRes.string.common_close), Modifier.size(20.dp))
            }
        }
        GeeFlowOutlinedTextField(name, Modifier.fillMaxWidth().testTag("manual-recipe-name"),
            label = { Text(stringResource(Res.string.free_control_rename_dialog_name)) },
            lineLimits = TextFieldLineLimits.SingleLine, enabled = !model.saving, autoFocus = true)
        GeeFlowOutlinedTextField(description, Modifier.fillMaxWidth().testTag("manual-recipe-description"),
            label = { Text(stringResource(Res.string.home_manual_recipe_description)) },
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 5), enabled = !model.saving)
        model.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("manual-recipe-error"))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onEvent(DeviceDashboardEvent.ManualRecipeCancelled) }, enabled = !model.saving,
                modifier = Modifier.height(48.dp).testTag("manual-recipe-cancel")) {
                Text(stringResource(CoreRes.string.common_cancel))
            }
            Button(onClick = {
                // Flush IME edits synchronously before the view model snapshots the recipe.
                onEvent(DeviceDashboardEvent.ManualRecipeNameChanged(name.text.toString()))
                onEvent(DeviceDashboardEvent.ManualRecipeDescriptionChanged(description.text.toString()))
                onEvent(DeviceDashboardEvent.ManualRecipeSaveClicked)
            },
                enabled = model.canSave && !model.saving && name.text.isNotBlank(),
                modifier = Modifier.height(48.dp).widthIn(min = 96.dp).testTag("manual-recipe-save")) {
                if (model.saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
                else Text(stringResource(CoreRes.string.common_save))
            }
        }
    }
}
