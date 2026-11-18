package app.geeflow.presentation.feature.device.dashboard.quickmaintenance

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import app.geeflow.data.device.model.MaintenanceSettings
import app.geeflow.data.device.model.CleaningProgram
import app.geeflow.data.device.model.CleaningType
import app.geeflow.domain.device.usecase.GetDeviceConstraintsUseCase
import app.geeflow.domain.device.usecase.SaveMaintenanceSettingsUseCase
import app.geeflow.domain.device.usecase.CleaningStatus
import app.geeflow.domain.device.usecase.GetCleaningStatusUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.ObserveMaintenanceSettingsUseCase
import app.geeflow.domain.device.usecase.StartCleaningUseCase
import app.geeflow.domain.device.usecase.StopCleaningUseCase
import app.geeflow.navigation.NavEvent.Back
import app.geeflow.navigation.NavEvent.To
import app.geeflow.navigation.destination.DeviceSettings
import app.geeflow.navigation.destination.DeviceSettings.EntryPoint
import app.geeflow.presentation.feature.device.dashboard.QuickMaintenance
import app.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceEvent.CloseClicked
import app.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceEvent.MoreSettingsClicked
import app.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceEvent.ToggleCleaningClicked
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.error_generic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.milliseconds

@KoinViewModel
internal class QuickMaintenanceViewModel(
    @InjectedParam private val arguments: QuickMaintenance,
    private val getCleaningStatus: GetCleaningStatusUseCase,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val observeMaintenance: ObserveMaintenanceSettingsUseCase,
    private val startCleaning: StartCleaningUseCase,
    private val stopCleaning: StopCleaningUseCase,
    private val getConstraints: GetDeviceConstraintsUseCase,
    private val saveMaintenance: SaveMaintenanceSettingsUseCase,
) : BaseViewModel<QuickMaintenanceViewState, QuickMaintenanceViewModelEvent>(
    QuickMaintenanceViewState(selectedType = arguments.type),
) {

    private var stateJob: Job? = null
    private var cleaningJob: Job? = null

    private var settings: MaintenanceSettings? = null
    private var latestStatus: CleaningStatus? = null
    private var latestState: DeviceState? = null
    private val drafts = mutableMapOf<CleaningType, CleaningProgram>()

    init {
        stateJob = launch {
            val limits = getConstraints(arguments.deviceId)
            modify { copy(flushRange = limits.cleaningTimeRange, restRange = limits.cleaningRestRange,
                cycleRange = limits.cleaningCountRange) }
            combine(
                flow = cleaningStatus(),
                flow2 = observeDeviceState(arguments.deviceId),
                flow3 = observeMaintenance(arguments.deviceId),
                transform = { status, state, saved -> Triple(status, state, saved) },
            ).collect { (status, state, saved) ->
                settings = saved
                latestStatus = status
                latestState = state
                maintenanceStateChanged(status, state)
            }
        }
        cleaningJob = launch {
            if (!cleaningStatus().first().inProgress) return@launch
            awaitCleaningEnd()
        }
    }

    fun handleEvent(event: QuickMaintenanceEvent) = when (event) {
        is QuickMaintenanceEvent.TypeSelected -> {
            if (!viewState.value.isStarting && !viewState.value.isCleaning) {
                modify { copy(selectedType = event.type) }
                latestStatus?.let { status -> latestState?.let { maintenanceStateChanged(status, it) } }
            }
            Unit
        }
        is QuickMaintenanceEvent.FlushChanged -> editProgram { copy(flushSeconds = event.seconds.coerceIn(viewState.value.flushRange)) }
        is QuickMaintenanceEvent.RestChanged -> editProgram { copy(restSeconds = event.seconds.coerceIn(viewState.value.restRange)) }
        is QuickMaintenanceEvent.CyclesChanged -> editProgram { copy(cycles = event.count.coerceIn(viewState.value.cycleRange)) }
        ToggleCleaningClicked -> toggleCleaning()
        CloseClicked -> if (!viewState.value.isStarting) close() else Unit
        is MoreSettingsClicked -> {
            close()
            navigate(To(DeviceSettings(arguments.deviceId, if (event.isExpanded) EntryPoint.Maintenance else null)))
        }
    }

    private fun editProgram(edit: CleaningProgram.() -> CleaningProgram) {
        val state = viewState.value
        if (state.isStarting || state.isCleaning || state.flushRange.isEmpty() || state.restRange.isEmpty() || state.cycleRange.isEmpty()) return
        val program = drafts[state.selectedType] ?: settings?.program(state.selectedType) ?: return
        drafts[state.selectedType] = program.edit()
        latestStatus?.let { status -> latestState?.let { maintenanceStateChanged(status, it) } }
    }

    private fun toggleCleaning() {
        if (viewState.value.isStarting) return
        if (viewState.value.isCleaning) {
            cleaningJob?.cancel()
            launch { stopCleaning(arguments.deviceId) }
            return
        }
        if (!viewState.value.canStart) return
        val type = viewState.value.selectedType
        val saved = settings ?: return
        val program = drafts[type] ?: saved.program(type)
        val updated = if (type == CleaningType.Daily) saved.copy(daily = program) else saved.copy(deep = program)
        modify { copy(isStarting = true) }
        cleaningJob = launchCatching(onError = {
            modify { copy(isStarting = false) }
            launch { emitEvent(QuickMaintenanceViewModelEvent.ShowSnackbar(getString(Res.string.error_generic))) }
        }) {
            saveMaintenance(arguments.deviceId, updated)
            startCleaning(arguments.deviceId, type)
            withTimeout(START_STATUS_TIMEOUT_MILLIS.milliseconds) { cleaningStatus().first { it.inProgress } }
            modify { copy(isStarting = false) }
            awaitCleaningEnd()
        }
    }

    private suspend fun awaitCleaningEnd() {
        cleaningStatus().first { !it.inProgress }
        close()
    }

    private fun cleaningStatus() = getCleaningStatus(arguments.deviceId)

    private fun maintenanceStateChanged(status: CleaningStatus, state: DeviceState) {
        if (state.connectionStatus != ConnectionStatus.Connected) {
            close()
            return
        }

        val program = drafts[viewState.value.selectedType] ?: settings?.program(viewState.value.selectedType)
        modify {
            copy(
                canStart = program != null && state.brewStatus == DeviceState.BrewStatus.Idle && !state.hasError,
                isCleaning = status.inProgress,
                flushProgress = QuickMaintenanceViewState.Progress(
                    current = status.flush.current,
                    target = if (status.inProgress) status.flush.target else program?.flushSeconds ?: 0,
                ),
                restProgress = QuickMaintenanceViewState.Progress(
                    current = status.rest.current,
                    target = if (status.inProgress) status.rest.target else program?.restSeconds ?: 0,
                ),
                cycleProgress = QuickMaintenanceViewState.Progress(
                    current = status.cycle.current,
                    target = if (status.inProgress) status.cycle.target else program?.cycles ?: 0,
                ),
            )
        }
    }

    /** Dropping both subscriptions first: a later emission reaching [close] again would pop the dashboard too. */
    private fun close() {
        stateJob?.cancel()
        cleaningJob?.cancel()
        navigate(Back)
    }
    private companion object {
        const val START_STATUS_TIMEOUT_MILLIS = 5000L
    }
}
