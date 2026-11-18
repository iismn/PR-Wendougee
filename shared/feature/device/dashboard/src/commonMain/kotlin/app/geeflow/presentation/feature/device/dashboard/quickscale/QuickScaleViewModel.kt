package app.geeflow.presentation.feature.device.dashboard.quickscale

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.device.usecase.*
import app.geeflow.navigation.NavEvent
import app.geeflow.navigation.destination.DeviceSettings
import app.geeflow.presentation.feature.device.dashboard.QuickScale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/** A scale-only sheet. Opening and closing it never changes accessory connectivity. */
@KoinViewModel
internal class QuickScaleViewModel(
    @InjectedParam private val arguments: QuickScale,
    private val observeDevice: ObserveDeviceStateUseCase,
    private val observeScales: ObserveFoundScalesUseCase,
    private val setConnectivity: SetSmartScaleConnectivityUseCase,
    private val requestScales: RequestSmartScaleListUseCase,
    private val connectScale: ConnectSmartScaleUseCase,
    private val disconnectScale: DisconnectSmartScaleUseCase,
) : BaseViewModel<QuickScaleViewState, Unit>(QuickScaleViewState()) {
    init {
        launch {
            combine(observeDevice(arguments.deviceId), observeScales(arguments.deviceId)) { device, found ->
                val connected = device.smartScale?.takeIf { it.isConnected }
                val all = (listOfNotNull(connected) + found).distinctBy { it.name }
                device to all.map { QuickScaleViewState.Scale(it.name, it.name == connected?.name) }
            }.collect { (device, scales) ->
                modify { copy(machineConnected = device.connectionStatus == DeviceState.ConnectionStatus.Connected,
                    enabled = device.smartScaleEnabled, searching = device.smartScaleSearchActive,
                    devices = if (device.smartScaleEnabled) scales else emptyList()) }
            }
        }
    }

    fun handleEvent(event: QuickScaleEvent) {
        val state = viewState.value
        when (event) {
            QuickScaleEvent.CloseClicked -> navigate(NavEvent.Back)
            QuickScaleEvent.MoreSettingsClicked -> {
                navigate(NavEvent.Back)
                navigate(NavEvent.To(DeviceSettings(arguments.deviceId, DeviceSettings.EntryPoint.Connectivity)))
            }
            is QuickScaleEvent.EnabledChanged -> if (state.enabled != event.enabled) {
                command(action = { setConnectivity(arguments.deviceId, event.enabled) },
                    confirmed = { it.smartScaleEnabled == event.enabled })
            }
            QuickScaleEvent.SearchClicked -> if (state.enabled && !state.searching) {
                command(action = { requestScales(arguments.deviceId) })
            }
            is QuickScaleEvent.ConnectionClicked -> {
                if (!state.enabled) return
                val scale = state.devices.firstOrNull { it.name == event.name } ?: return
                command(name = scale.name, action = {
                    if (scale.connected) disconnectScale(arguments.deviceId)
                    else connectScale(arguments.deviceId, scale.name)
                }, confirmed = {
                    if (scale.connected) it.smartScale?.isConnected != true
                    else it.smartScale?.let { connected -> connected.isConnected && connected.name == scale.name } == true
                })
            }
        }
    }

    private fun command(name: String? = null, action: suspend () -> Unit,
        confirmed: ((DeviceState) -> Boolean)? = null) {
        if (!viewState.value.machineConnected || viewState.value.busy) return
        modify { copy(busy = true, busyName = name, error = false) }
        launch {
            try {
                action()
                if (confirmed != null) withTimeout(10_000L) {
                    val final = observeDevice(arguments.deviceId).first {
                        it.connectionStatus != DeviceState.ConnectionStatus.Connected || confirmed(it)
                    }
                    check(final.connectionStatus == DeviceState.ConnectionStatus.Connected)
                }
            } catch (error: Throwable) {
                currentCoroutineContext().ensureActive()
                modify { copy(error = true) }
            } finally {
                modify { copy(busy = false, busyName = null) }
            }
        }
    }
}
