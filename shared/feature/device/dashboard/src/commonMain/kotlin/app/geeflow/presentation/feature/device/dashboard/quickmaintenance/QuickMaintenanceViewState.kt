package app.geeflow.presentation.feature.device.dashboard.quickmaintenance

import app.geeflow.data.device.model.CleaningType

data class QuickMaintenanceViewState(
    val selectedType: CleaningType = CleaningType.Daily,
    val isStarting: Boolean = false,
    val canStart: Boolean = false,
    val isCleaning: Boolean = false,
    val flushRange: IntRange = IntRange.EMPTY,
    val restRange: IntRange = IntRange.EMPTY,
    val cycleRange: IntRange = IntRange.EMPTY,
    val flushProgress: Progress = Progress(0, 0),
    val restProgress: Progress = Progress(0, 0),
    val cycleProgress: Progress = Progress(0, 0),
) {
    data class Progress(
        val current: Int,
        val target: Int,
    )
}
