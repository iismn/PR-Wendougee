package app.geeflow.presentation.feature.device.dashboard.quickscale

data class QuickScaleViewState(
    val machineConnected: Boolean = false,
    val enabled: Boolean = false,
    val searching: Boolean = false,
    val devices: List<Scale> = emptyList(),
    val busy: Boolean = false,
    val busyName: String? = null,
    val error: Boolean = false,
) {
    data class Scale(val name: String, val connected: Boolean = false)
}

sealed interface QuickScaleEvent {
    data class EnabledChanged(val enabled: Boolean) : QuickScaleEvent
    data class ConnectionClicked(val name: String) : QuickScaleEvent
    data object SearchClicked : QuickScaleEvent
    data object MoreSettingsClicked : QuickScaleEvent
    data object CloseClicked : QuickScaleEvent
}
