package app.geeflow.presentation.feature.device.dashboard.quickmaintenance

import androidx.lifecycle.viewModelScope
import app.geeflow.data.device.model.*
import app.geeflow.domain.device.usecase.*
import app.geeflow.presentation.feature.device.dashboard.QuickMaintenance
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class QuickMaintenanceViewModelTest {
    @Test
    fun draftSurvivesTelemetryAndTypeChangesAndIsSavedOnlyWhenStarting() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val saved = MutableStateFlow(MaintenanceSettings(CleaningProgram(5, 5, 3), CleaningProgram(8, 10, 6)))
        val device = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))
        val progress = CleaningStatus.Progress(0,0)
        val status = MutableStateFlow(CleaningStatus(false, progress, progress, progress))
        val getStatus = mockk<GetCleaningStatusUseCase>()
        val observe = mockk<ObserveDeviceStateUseCase>()
        val observeSettings = mockk<ObserveMaintenanceSettingsUseCase>()
        val constraints = mockk<GetDeviceConstraintsUseCase>()
        val save = mockk<SaveMaintenanceSettingsUseCase>()
        val start = mockk<StartCleaningUseCase>()
        val stop = mockk<StopCleaningUseCase>()
        every { getStatus(1L) } returns status
        every { observe(1L) } returns device
        every { observeSettings(1L) } returns saved
        coEvery { constraints(1L) } returns DeviceConstraints(0..110, 0..140, 0..12, 0..120,
            1..60, 0..60, 1..20, 0f..12f, 0f..12f)
        coEvery { save(1L, any()) } coAnswers { saved.value = secondArg() }
        coEvery { start(1L, any()) } coAnswers { status.value = status.value.copy(inProgress = true) }
        val vm = QuickMaintenanceViewModel(QuickMaintenance(1L), getStatus, observe, observeSettings, start, stop, constraints, save)
        try {
            runCurrent()
            vm.handleEvent(QuickMaintenanceEvent.FlushChanged(17))
            vm.handleEvent(QuickMaintenanceEvent.RestChanged(9))
            vm.handleEvent(QuickMaintenanceEvent.CyclesChanged(7))
            device.value = device.value.copy(pressure = 0.1f)
            runCurrent()
            assertEquals(17, vm.viewState.value.flushProgress.target)
            vm.handleEvent(QuickMaintenanceEvent.TypeSelected(CleaningType.Deep))
            assertEquals(8, vm.viewState.value.flushProgress.target)
            vm.handleEvent(QuickMaintenanceEvent.TypeSelected(CleaningType.Daily))
            assertEquals(17, vm.viewState.value.flushProgress.target)
            coVerify(exactly = 0) { save(any(), any()) }
            coVerify(exactly = 0) { start(any(), any()) }
            vm.handleEvent(QuickMaintenanceEvent.ToggleCleaningClicked)
            runCurrent()
            coVerifyOrder {
                save(1L, match { it.daily == CleaningProgram(17, 9, 7) && it.deep == CleaningProgram(8, 10, 6) })
                start(1L, CleaningType.Daily)
            }
            vm.handleEvent(QuickMaintenanceEvent.FlushChanged(50))
            assertEquals(CleaningProgram(17,9,7), saved.value.daily)
            assertTrue(vm.viewState.value.isCleaning)
        } finally {
            vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }
}
