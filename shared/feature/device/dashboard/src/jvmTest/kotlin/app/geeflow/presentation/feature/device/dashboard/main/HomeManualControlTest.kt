package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.PressureLocation
import app.geeflow.data.device.model.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeManualControlTest {
    private val limits = DeviceConstraints(0..110, 0..140, 0..12, 0..120, 1..60, 0..60, 1..20, 0f..12f, 0f..8f)
    private val capabilities = ProfilingCapabilities(livePressure = mapOf(PressureLocation.Pump to TargetRange(0f,12f,.1f)),
        liveFlow = TargetRange(0f,8f,.1f))
    private val idle = DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected)

    @Test
    fun rotationAndModeSelectionAreDraftsAndRespectMachineLimits() = runTest {
        val writes = mutableListOf<String>()
        val control = HomeManualControl(backgroundScope, { writes += "start" }, { _,_ -> writes += "target" }, { writes += "stop" }, { throw it })
        control.configure(capabilities, limits)
        control.updateDevice(idle)
        control.turn(6.13f)
        assertEquals(6.1f, control.state.value.pressure)
        control.select(ManualPriority.Flow)
        control.turn(100f)
        assertEquals(8f, control.state.value.flow)
        control.turn(Float.NaN)
        assertEquals(8f, control.state.value.flow)
        control.select(ManualPriority.Pressure)
        control.turn(-100f)
        assertEquals(0f, control.state.value.pressure)
        advanceTimeBy(1000)
        assertTrue(writes.isEmpty(), "Drafting a target must never start a pump or write a register")
        control.configure(capabilities.copy(liveFlow = null), limits)
        control.select(ManualPriority.Flow)
        assertEquals(ManualPriority.Pressure, control.state.value.mode)
        control.updateDevice(idle.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected))
        control.startShot()
        runCurrent()
        assertTrue(writes.isEmpty())
    }

    @Test
    fun explicitStartOrdersCommandsAndActiveRotationIsConflatedWhileStopDropsPendingTargets() = runTest {
        val writes = mutableListOf<String>()
        lateinit var control: HomeManualControl
        val active = idle.copy(brewStatus = DeviceState.BrewStatus.FreeVariable, freeHandControlMode = FreeHandControlMode.Pressure)
        control = HomeManualControl(backgroundScope, { writes += "start:$it"; control.updateDevice(active) },
            { mode,value -> writes += "$mode:$value" }, { writes += "stop"; control.updateDevice(idle) }, { throw it })
        control.configure(capabilities, limits); control.updateDevice(idle); control.turn(6f)
        control.startShot(); runCurrent()
        assertEquals(listOf("start:Pressure","Pressure:6.0"),writes)
        control.turn(7f); control.turn(8f); control.turn(9f)
        advanceTimeBy(250); runCurrent()
        assertEquals("Pressure:9.0", writes.last())
        assertEquals(3,writes.size)
        control.select(ManualPriority.Flow)
        assertEquals(ManualPriority.Pressure,control.state.value.mode, "An unsupported live regulator switch must not be simulated")
        control.turn(10f); control.stopShot(); runCurrent(); advanceTimeBy(250); runCurrent()
        assertEquals("stop", writes.last())
        assertEquals(4,writes.size)
        assertFalse(control.state.value.active)
        control.updateDevice(idle.copy(brewStatus = DeviceState.BrewStatus.Profile))
        control.turn(11f); control.startShot(); advanceTimeBy(250); runCurrent()
        assertEquals(4,writes.size, "The dial must not take control of a profile shot")
    }

    @Test
    fun flowStartUsesTheFlowTargetAndFailedTargetWriteStopsTheNewShot() = runTest {
        val writes = mutableListOf<String>()
        val errors = mutableListOf<Throwable>()
        val control = HomeManualControl(backgroundScope, { writes += "start:$it" },
            { mode,value -> writes += "$mode:$value"; error("Write failed") }, { writes += "stop" }, errors::add)
        control.configure(capabilities, limits); control.updateDevice(idle)
        control.select(ManualPriority.Flow); control.turn(4.2f); control.startShot(); runCurrent()
        assertEquals(listOf("start:Flow","Flow:4.2","stop"),writes)
        assertEquals(1,errors.size)
        assertEquals(ManualOperation.None,control.state.value.operation)
    }
    @Test
    fun stoppingBeforeTheQueuedStartRunsAbandonsCaptureAndAllowsTheNextStart() = runTest {
        val writes = mutableListOf<String>()
        val capture = HomeManualRecipeCapture()
        lateinit var control: HomeManualControl
        control = HomeManualControl(backgroundScope,
            { writes += "start"; control.updateDevice(idle.copy(brewStatus = DeviceState.BrewStatus.FreeVariable)) },
            { _, _ -> writes += "target" },
            { writes += "stop"; control.updateDevice(idle) }, { throw it }, capture::abandon)
        control.configure(capabilities, limits)
        control.updateDevice(idle)
        capture.arm(ManualPriority.Pressure, 7, null)
        control.startShot()
        control.stopShot()
        runCurrent()
        assertFalse(capture.pending, "An aborted queued start must not permanently block recipe capture")
        assertEquals(listOf("stop"), writes, "Stopping a queued start must not issue a start or target command")
        assertTrue(control.state.value.canStart)
        capture.arm(ManualPriority.Pressure, 7, null)
        control.startShot()
        runCurrent()
        assertEquals(listOf("stop", "start", "target"), writes)
        assertTrue(control.state.value.active)
    }

    @Test
    fun aPreviousStartTimeoutCannotFailTheNextStart() = runTest {
        val errors = mutableListOf<Throwable>()
        var failedStarts = 0
        var starts = 0
        lateinit var control: HomeManualControl
        val active = idle.copy(brewStatus = DeviceState.BrewStatus.FreeVariable)
        control = HomeManualControl(backgroundScope,
            { if (++starts == 1) control.updateDevice(active) }, { _, _ -> }, {}, errors::add, { failedStarts++ })
        control.configure(capabilities, limits)
        control.updateDevice(idle)
        control.startShot()
        runCurrent()
        control.updateDevice(idle)
        advanceTimeBy(4900)
        control.startShot()
        runCurrent()
        advanceTimeBy(200)
        runCurrent()
        assertEquals(ManualOperation.Starting, control.state.value.operation)
        assertEquals(0, failedStarts, "The previous shot's timer must not abandon the new capture")
        assertTrue(errors.isEmpty())
        advanceTimeBy(4800)
        runCurrent()
        assertEquals(ManualOperation.None, control.state.value.operation)
        assertEquals(1, failedStarts, "The new request must still have its own confirmation timeout")
        assertEquals(1, errors.size)
    }

    @Test
    fun aPreviousStopTimeoutCannotFailTheNextStop() = runTest {
        val errors = mutableListOf<Throwable>()
        var stops = 0
        lateinit var control: HomeManualControl
        val active = idle.copy(brewStatus = DeviceState.BrewStatus.FreeVariable)
        control = HomeManualControl(backgroundScope,
            { control.updateDevice(active) }, { _, _ -> },
            { if (++stops == 1) control.updateDevice(idle) }, errors::add)
        control.configure(capabilities, limits)
        control.updateDevice(idle)
        control.startShot()
        runCurrent()
        control.stopShot()
        runCurrent()
        advanceTimeBy(4900)
        control.startShot()
        runCurrent()
        control.stopShot()
        runCurrent()
        advanceTimeBy(200)
        runCurrent()
        assertEquals(ManualOperation.Stopping, control.state.value.operation)
        assertTrue(errors.isEmpty(), "An acknowledged old Stop must not time out a newer Stop")
        control.updateDevice(idle)
        advanceTimeBy(5000)
        runCurrent()
        assertTrue(control.state.value.canStart)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun delayedFailureFromAnOldStartCannotStopOrAbandonTheNextShot() = runTest {
        val writes = mutableListOf<String>()
        val errors = mutableListOf<Throwable>()
        val failFirstTarget = CompletableDeferred<Unit>()
        var targetWrites = 0
        var failedStarts = 0
        lateinit var control: HomeManualControl
        val active = idle.copy(brewStatus = DeviceState.BrewStatus.FreeVariable)
        control = HomeManualControl(backgroundScope,
            { writes += "start"; control.updateDevice(active) },
            { _, value ->
                writes += "target:$value"
                if (++targetWrites == 1) { failFirstTarget.await(); error("Old target write failed") }
            }, { writes += "stop" }, errors::add, { failedStarts++ })
        control.configure(capabilities, limits)
        control.updateDevice(idle)
        control.turn(4f)
        control.startShot()
        runCurrent()
        control.updateDevice(idle)
        control.turn(7f)
        control.startShot()
        runCurrent()
        failFirstTarget.complete(Unit)
        runCurrent()
        assertEquals(listOf("start", "target:4.0", "start", "target:7.0"), writes)
        assertEquals(0, failedStarts)
        assertTrue(errors.isEmpty())
        assertTrue(control.state.value.active)
    }

    @Test
    fun pendingRotationFromThePreviousShotCannotWriteIntoTheNextShot() = runTest {
        val writes = mutableListOf<Float>()
        lateinit var control: HomeManualControl
        val active = idle.copy(brewStatus = DeviceState.BrewStatus.FreeVariable)
        control = HomeManualControl(backgroundScope,
            { control.updateDevice(active) }, { _, value -> writes += value },
            { control.updateDevice(idle) }, { throw it })
        control.configure(capabilities, limits)
        control.updateDevice(idle)
        control.turn(4f)
        control.startShot()
        runCurrent()
        control.turn(9f)
        runCurrent()
        control.stopShot()
        runCurrent()
        control.turn(3f)
        control.startShot()
        runCurrent()
        advanceTimeBy(200)
        runCurrent()
        assertEquals(listOf(4f, 3f), writes, "A queued dial value belongs only to the shot that produced it")
    }

}
