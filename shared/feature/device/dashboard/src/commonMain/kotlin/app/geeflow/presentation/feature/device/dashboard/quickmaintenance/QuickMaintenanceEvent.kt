package app.geeflow.presentation.feature.device.dashboard.quickmaintenance

import app.geeflow.data.device.model.CleaningType

sealed interface QuickMaintenanceEvent {
    data class TypeSelected(val type: CleaningType) : QuickMaintenanceEvent
    data class FlushChanged(val seconds: Int) : QuickMaintenanceEvent
    data class RestChanged(val seconds: Int) : QuickMaintenanceEvent
    data class CyclesChanged(val count: Int) : QuickMaintenanceEvent
    data object ToggleCleaningClicked : QuickMaintenanceEvent
    data class MoreSettingsClicked(val isExpanded: Boolean) : QuickMaintenanceEvent
    data object CloseClicked : QuickMaintenanceEvent
}
