package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import app.geeflow.presentation.feature.device.dashboard.components.BrewButtonState

/** Pure review fixture: taps only alter local UI; no brewing or persistence use case is invoked. */
@Composable
internal fun ManualRecipeDesignPreview(active: Boolean = false) {
    val fixture = remember { getMockDeviceDashboardViewState() }
    var dialog by remember { mutableStateOf<DeviceDashboardViewState.Dialog.ManualRecipe?>(
        if (active) null else DeviceDashboardViewState.Dialog.ManualRecipe(
            name = "Morning manual", description = "A gentle start, then a steady finish.")) }
    var manual by remember { mutableStateOf(fixture.manual.copy(active = active)) }
    Box(Modifier.fillMaxSize()) {
        ProfileHome(fixture.copy(manual = manual, brewButtonState = if (manual.active) BrewButtonState.Brewing else BrewButtonState.Idle), getMockProfileListViewState(), remember { SnackbarHostState() },
            onEvent = { event ->
                when (event) {
                    DeviceDashboardEvent.DialStartClicked -> manual = manual.copy(active = true)
                    DeviceDashboardEvent.DialStopClicked -> {
                        manual = manual.copy(active = false)
                        dialog = DeviceDashboardViewState.Dialog.ManualRecipe(name = "Morning manual")
                    }
                    else -> Unit
                }
            }, onProfileEvent = {}, onShowCharts = {})
        dialog?.let { value -> DeviceDashboardDialog(value, onEvent = { event ->
            dialog = when (event) {
                is DeviceDashboardEvent.ManualRecipeNameChanged -> dialog?.copy(name = event.value)
                is DeviceDashboardEvent.ManualRecipeDescriptionChanged -> dialog?.copy(description = event.value)
                DeviceDashboardEvent.ManualRecipeCancelled, DeviceDashboardEvent.ManualRecipeSaveClicked,
                DeviceDashboardEvent.DialogDismissed -> null
                else -> dialog
            }
        }) }
    }
}
