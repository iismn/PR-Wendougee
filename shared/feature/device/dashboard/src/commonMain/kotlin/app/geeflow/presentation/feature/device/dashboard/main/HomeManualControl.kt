package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.device.model.DeviceConstraints
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.ProfilingCapabilities
import app.geeflow.data.device.model.TargetRange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ManualPriority { Pressure, Flow }
enum class ManualOperation { None, Starting, Stopping }

data class HomeManualState(
    val mode: ManualPriority = ManualPriority.Pressure,
    val pressure: Float = 0f,
    val flow: Float = 0f,
    val pressureRange: TargetRange = TargetRange(0f, 12f, .1f),
    val flowRange: TargetRange = TargetRange(0f, 12f, .1f),
    val ready: Boolean = false,
    val connected: Boolean = false,
    val flowSupported: Boolean = false,
    val busy: Boolean = false,
    val active: Boolean = false,
    val operation: ManualOperation = ManualOperation.None,
) {
    val range get() = if (mode == ManualPriority.Pressure) pressureRange else flowRange
    val target get() = if (mode == ManualPriority.Pressure) pressure else flow
    val canTurn get() = ready && connected && !busy && operation == ManualOperation.None
    val canSelectMode get() = canTurn && !active
    val canStart get() = canTurn && !active
}

/** Draft rotation is local. Only an explicit start or an already active freehand shot writes targets. */
internal class HomeManualControl(
    private val scope: CoroutineScope,
    private val start: suspend (ManualPriority) -> Unit,
    private val setTarget: suspend (ManualPriority, Float) -> Unit,
    private val stop: suspend () -> Unit,
    private val onError: (Throwable) -> Unit,
    private val onStartFailed: () -> Unit = {},
) {
    private val mutable = MutableStateFlow(HomeManualState())
    val state = mutable.asStateFlow()
    private val commands = Mutex()
    private data class TargetUpdate(val generation: Long, val mode: ManualPriority, val value: Float)
    private val targets = Channel<TargetUpdate>(Channel.CONFLATED)
    private var writeInterval = 100L
    private var operationGeneration = 0L
    private var shotGeneration = 0L
    private var targetGeneration = 0L

    init {
        scope.launch {
            for (first in targets) {
                delay(writeInterval)
                var latest = first
                while (true) latest = targets.tryReceive().getOrNull() ?: break
                try {
                    commands.withLock {
                        val current = mutable.value
                        if (latest.generation == targetGeneration && current.canTurn && current.active && latest.mode == current.mode) {
                            setTarget(latest.mode, latest.value)
                        }
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    if (latest.generation == targetGeneration) report(error)
                }
            }
        }
    }

    fun configure(capabilities: ProfilingCapabilities, constraints: DeviceConstraints) {
        val pressure = capabilities.livePressure[PressureLocation.Pump] ?: capabilities.livePressure.values.firstOrNull()
        val flow = capabilities.liveFlow
        writeInterval = capabilities.minimumWriteIntervalMillis.coerceAtLeast(100L)
        fun TargetRange.limit(range: ClosedFloatingPointRange<Float>, cap: Float = maximum) = copy(
            minimum = maxOf(0f, minimum, range.start), maximum = minOf(maximum, range.endInclusive, cap))
        val pressureRange = pressure?.limit(constraints.pressureRange, 12f)
        val flowRange = flow?.limit(constraints.flowRange)
        val validPressure = pressureRange != null && pressureRange.maximum > pressureRange.minimum
        val validFlow = flowRange != null && flowRange.maximum > flowRange.minimum
        mutable.value = mutable.value.copy(ready = validPressure, flowSupported = validFlow,
            pressureRange = pressureRange ?: TargetRange(0f, 12f, .1f),
            flowRange = flowRange ?: TargetRange(0f, 12f, .1f))
    }

    fun updateDevice(device: DeviceState) {
        val active = device.brewStatus == DeviceState.BrewStatus.FreeVariable
        val connected = device.connectionStatus == DeviceState.ConnectionStatus.Connected
        val current = mutable.value
        val mode = if (active) { if (device.freeHandControlMode == FreeHandControlMode.Flow) ManualPriority.Flow else ManualPriority.Pressure } else current.mode
        if (current.active != active || current.connected != connected || current.mode != mode) targetGeneration++
        if (current.connected && !connected) {
            operationGeneration++
            shotGeneration++
        }
        val finishedOperation = !connected || (active && current.operation == ManualOperation.Starting) ||
            (device.brewStatus == DeviceState.BrewStatus.Idle && current.operation == ManualOperation.Stopping)
        mutable.value = current.copy(connected = connected, active = active,
            busy = device.brewStatus != DeviceState.BrewStatus.Idle && !active,
            mode = mode,
            operation = if (finishedOperation) ManualOperation.None else current.operation)
    }

    fun select(mode: ManualPriority) {
        val current = mutable.value
        // This protocol cannot switch regulators in the middle of an active shot.
        if (current.canSelectMode && (mode != ManualPriority.Flow || current.flowSupported)) mutable.value = current.copy(mode = mode)
    }

    fun turn(value: Float) {
        val current = mutable.value
        if (!current.canTurn || !value.isFinite()) return
        val target = current.range.quantize(value)
        mutable.value = if (current.mode == ManualPriority.Pressure) current.copy(pressure = target) else current.copy(flow = target)
        if (current.active) targets.trySend(TargetUpdate(targetGeneration, current.mode, target))
    }

    fun startShot() {
        val current = mutable.value
        if (!current.canStart) return
        val request = ++operationGeneration
        val shot = ++shotGeneration
        targetGeneration++
        mutable.value = current.copy(operation = ManualOperation.Starting)
        scope.launch {
            var started = false
            try {
                val accepted = commands.withLock {
                    val latest = mutable.value
                    if (request != operationGeneration || !latest.connected || latest.busy || latest.active || latest.operation != ManualOperation.Starting) false
                    else {
                        start(current.mode)
                        started = true
                        val afterStart = mutable.value
                        if (request == operationGeneration && afterStart.connected && !afterStart.busy &&
                            (!afterStart.active || afterStart.mode == current.mode)) setTarget(current.mode, current.target)
                        true
                    }
                }
                if (!accepted) {
                    if (request == operationGeneration && mutable.value.operation == ManualOperation.Starting) {
                        mutable.value = mutable.value.copy(operation = ManualOperation.None)
                    }
                    if (shot == shotGeneration) onStartFailed()
                    return@launch
                }
                awaitTelemetry(ManualOperation.Starting, request)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (shot != shotGeneration) return@launch
                onStartFailed()
                if (started) commands.withLock {
                    if (shot == shotGeneration && mutable.value.connected && !mutable.value.busy) runCatching { stop() }
                }
                if (shot == shotGeneration) {
                    if (request == operationGeneration) mutable.value = mutable.value.copy(operation = ManualOperation.None)
                    report(error)
                }
            }
        }
    }

    fun stopShot() {
        val current = mutable.value
        if ((!current.active && current.operation != ManualOperation.Starting) || current.operation == ManualOperation.Stopping) return
        val request = ++operationGeneration
        targetGeneration++
        mutable.value = current.copy(operation = ManualOperation.Stopping)
        scope.launch {
            try {
                val accepted = commands.withLock {
                    if (request != operationGeneration || !mutable.value.connected || mutable.value.operation != ManualOperation.Stopping) false
                    else { stop(); true }
                }
                if (accepted) awaitTelemetry(ManualOperation.Stopping, request)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (request == operationGeneration) {
                    mutable.value = mutable.value.copy(operation = ManualOperation.None)
                    report(error)
                }
            }
        }
    }

    private suspend fun awaitTelemetry(operation: ManualOperation, request: Long) {
        delay(5000)
        if (request == operationGeneration && mutable.value.operation == operation) {
            mutable.value = mutable.value.copy(operation = ManualOperation.None)
            if (operation == ManualOperation.Starting) onStartFailed()
            onError(IllegalStateException("Machine status confirmation timed out"))
        }
    }

    private fun report(error: Throwable) {
        if (error is CancellationException) throw error
        onError(error)
    }
}
