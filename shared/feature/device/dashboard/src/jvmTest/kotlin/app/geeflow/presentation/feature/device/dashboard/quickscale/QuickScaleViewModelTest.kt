package app.geeflow.presentation.feature.device.dashboard.quickscale

import androidx.lifecycle.viewModelScope
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.SmartScale
import app.geeflow.domain.device.usecase.*
import app.geeflow.navigation.NavEvent
import app.geeflow.presentation.feature.device.dashboard.QuickScale
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class QuickScaleViewModelTest {
    private class Fixture {
        val device = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))
        val found = MutableStateFlow(listOf(SmartScale("Scale A", false)))
        val observe = mockk<ObserveDeviceStateUseCase>()
        val scales = mockk<ObserveFoundScalesUseCase>()
        val enable = mockk<SetSmartScaleConnectivityUseCase>(relaxed = true)
        val request = mockk<RequestSmartScaleListUseCase>(relaxed = true)
        val connect = mockk<ConnectSmartScaleUseCase>(relaxed = true)
        val disconnect = mockk<DisconnectSmartScaleUseCase>(relaxed = true)
        init { every { observe(1L) } returns device; every { scales(1L) } returns found }
        fun model() = QuickScaleViewModel(QuickScale(1L), observe, scales, enable, request, connect, disconnect)
    }

    @Test fun openingAndClosingOnlyObserveAndNeverChangeScaleConnectivity() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f = Fixture()
        f.device.value = f.device.value.copy(smartScaleEnabled = true, smartScale = SmartScale("Connected scale", true))
        val vm = f.model()
        try {
            runCurrent()
            assertEquals(listOf("Connected scale", "Scale A"), vm.viewState.value.devices.map { it.name })
            assertTrue(vm.viewState.value.devices.first().connected)
            vm.handleEvent(QuickScaleEvent.CloseClicked)
            assertEquals(NavEvent.Back, vm.navEvent.first())
            coVerify(exactly = 0) { f.enable(any(), any()); f.request(any()); f.connect(any(), any()); f.disconnect(any()) }
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }

    @Test fun commandsRequireAnExplicitActionAndWaitForAcknowledgementWithoutDuplicates() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f = Fixture(); val vm = f.model()
        try {
            runCurrent()
            vm.handleEvent(QuickScaleEvent.SearchClicked)
            vm.handleEvent(QuickScaleEvent.ConnectionClicked("Scale A"))
            runCurrent()
            coVerify(exactly = 0) { f.request(any()); f.connect(any(), any()) }
            vm.handleEvent(QuickScaleEvent.EnabledChanged(true)); runCurrent()
            assertTrue(vm.viewState.value.busy)
            vm.handleEvent(QuickScaleEvent.EnabledChanged(true)); runCurrent()
            coVerify(exactly = 1) { f.enable(1L, true) }
            f.device.value = f.device.value.copy(smartScaleEnabled = true); runCurrent()
            assertFalse(vm.viewState.value.busy)
            vm.handleEvent(QuickScaleEvent.ConnectionClicked("Unknown")); runCurrent()
            coVerify(exactly = 0) { f.connect(any(), any()) }
            vm.handleEvent(QuickScaleEvent.ConnectionClicked("Scale A")); runCurrent()
            vm.handleEvent(QuickScaleEvent.ConnectionClicked("Scale A")); runCurrent()
            coVerify(exactly = 1) { f.connect(1L, "Scale A") }
            f.device.value = f.device.value.copy(smartScale = SmartScale("Scale A", true)); runCurrent()
            assertFalse(vm.viewState.value.busy)
            assertTrue(vm.viewState.value.devices.single().connected)
            vm.handleEvent(QuickScaleEvent.ConnectionClicked("Scale A")); runCurrent()
            coVerify(exactly = 1) { f.disconnect(1L) }
            f.device.value = f.device.value.copy(smartScale = null); runCurrent()
            vm.handleEvent(QuickScaleEvent.SearchClicked); runCurrent()
            coVerify(exactly = 1) { f.request(1L) }
            f.device.value = f.device.value.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected); runCurrent()
            vm.handleEvent(QuickScaleEvent.ConnectionClicked("Scale A")); runCurrent()
            coVerify(exactly = 1) { f.connect(1L, "Scale A") }
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }

    @Test fun failedConfirmationLeavesAnErrorAndAllowsRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val f = Fixture(); f.device.value = f.device.value.copy(smartScaleEnabled = true)
        val vm = f.model()
        try {
            runCurrent(); vm.handleEvent(QuickScaleEvent.ConnectionClicked("Scale A")); runCurrent()
            advanceTimeBy(10_001); runCurrent()
            assertFalse(vm.viewState.value.busy)
            assertTrue(vm.viewState.value.error)
            vm.handleEvent(QuickScaleEvent.ConnectionClicked("Scale A")); runCurrent()
            coVerify(exactly = 2) { f.connect(1L, "Scale A") }
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
}
