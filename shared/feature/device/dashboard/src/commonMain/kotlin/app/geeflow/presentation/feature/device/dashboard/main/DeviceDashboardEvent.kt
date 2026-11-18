package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.data.user.model.HomeDockAction
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.PhaseTransition
import app.geeflow.data.device.model.CleaningType
import app.geeflow.presentation.feature.device.dashboard.model.ChartData

sealed interface DeviceDashboardEvent {
    data class DockOrderChanged(val order: List<HomeDockAction>) : DeviceDashboardEvent
    data class MaintenanceReminderOpened(val type: CleaningType) : DeviceDashboardEvent
    data class ToggleChartVisibility(val type: DeviceDashboardViewState.DashboardChartType) : DeviceDashboardEvent
    data object UserClicked : DeviceDashboardEvent
    data object DeviceClicked : DeviceDashboardEvent
    data object HotWaterClicked : DeviceDashboardEvent
    data object ManualBrewClicked : DeviceDashboardEvent
    data object FlowControlClicked : DeviceDashboardEvent
    data object ScaleClicked : DeviceDashboardEvent
    data class DialModeChanged(val mode: ManualPriority) : DeviceDashboardEvent
    data class DialTargetChanged(val value: Float) : DeviceDashboardEvent
    data class ManualRecipeNameChanged(val value: String) : DeviceDashboardEvent
    data class ManualRecipeDescriptionChanged(val value: String) : DeviceDashboardEvent
    data object ManualRecipeSaveClicked : DeviceDashboardEvent
    data object ManualRecipeCancelled : DeviceDashboardEvent
    data object DialStartClicked : DeviceDashboardEvent
    data object DialStopClicked : DeviceDashboardEvent
    data object StopBrewClicked : DeviceDashboardEvent
    data object BrewClicked : DeviceDashboardEvent
    data object QuickSettingsClicked : DeviceDashboardEvent
    data object QuickSettingsLongPressed : DeviceDashboardEvent
    data object CleaningClicked : DeviceDashboardEvent
    data object SettingsClicked : DeviceDashboardEvent
    data object ConnectionButtonClicked : DeviceDashboardEvent
    data object BrewDescriptionClicked : DeviceDashboardEvent
    data object EditProfileClicked : DeviceDashboardEvent
    data object DialogDismissed : DeviceDashboardEvent
    data object PermissionDialogResumed : DeviceDashboardEvent
    data object OpenSystemSettingsClicked : DeviceDashboardEvent
    data object Resumed : DeviceDashboardEvent
    data object AlarmClicked : DeviceDashboardEvent
    data class ProfileSelected(val id: String?) : DeviceDashboardEvent
    data class HistoryBrewSelected(
        val name: String,
        val durationSeconds: Int,
        val data: Map<Float, ChartData>,
        val targetData: Map<Float, ChartData>,
        val phaseProgram: BrewProgram? = null,
        val phaseTransitions: List<PhaseTransition> = emptyList(),
    ) : DeviceDashboardEvent
}
