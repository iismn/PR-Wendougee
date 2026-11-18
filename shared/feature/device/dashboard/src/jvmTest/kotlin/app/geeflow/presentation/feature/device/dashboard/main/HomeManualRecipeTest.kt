package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.data.brew.model.*
import app.geeflow.data.device.model.DeviceState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeManualRecipeTest {
    private val idle = DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected)
    private val pressure = idle.copy(brewStatus = DeviceState.BrewStatus.FreeVariable,
        freeHandControlMode = FreeHandControlMode.Pressure)
    private val point = BrewDataPoint(7f, 15f, 20f, 2f, 1f)
    private val start = Instant.parse("2026-11-17T12:00:00Z")
    private fun session(active: Boolean, mode: FreeHandControlMode = FreeHandControlMode.Pressure,
        timestamp: Instant = start, user: Long = 7) = BrewSession(userId = user, mode = BrewMode.Freehand,
        startTime = timestamp, inProgress = active, elapsedSeconds = 2,
        dataPoints = mapOf(0f to point.copy(volume = 0f), 2f to point),
        recording = FreeHandRecording(mode, listOf(FreeHandSample(0, point.copy(volume = 0f)), FreeHandSample(2000, point))))

    @Test
    fun ownedRecordingWaitsForBothCompletionAndConnectedIdleInEitherCollectorOrder() {
        for (deviceFirst in listOf(false, true)) {
            val capture = HomeManualRecipeCapture()
            capture.updateDevice(idle)
            capture.arm(ManualPriority.Pressure, 7, null)
            capture.updateDevice(pressure)
            assertNull(capture.updateSession(session(true)))
            val frozen = session(false)
            val completed = if (deviceFirst) {
                assertNull(capture.updateDevice(idle))
                capture.updateSession(frozen)
            } else {
                assertNull(capture.updateSession(frozen), "A finished observer emission alone must not confirm an unacknowledged stop")
                capture.updateDevice(idle)
            }
            assertEquals(frozen, completed)
            assertFalse(capture.pending)
            assertNull(capture.updateSession(frozen), "Repeated idle/session frames must not reopen the save form")
        }
    }

    @Test
    fun cachedProfilesForeignUsersOtherModesAndDisconnectedShotsNeverBecomeOwnedRecipes() {
        val old = session(false)
        val capture = HomeManualRecipeCapture()
        capture.updateDevice(idle)
        capture.arm(ManualPriority.Pressure, 7, old)
        assertNull(capture.updateSession(old))
        assertNull(capture.updateSession(old.copy(inProgress = true)), "The baseline recording cannot be captured again")
        assertNull(capture.updateSession(session(false, timestamp = start + kotlin.time.Duration.parse("1s"))))
        assertTrue(capture.pending, "A cached completed recording cannot confirm a new start")
        capture.abandon()
        capture.arm(ManualPriority.Pressure, 7, null)
        assertNull(capture.updateSession(session(true).copy(mode = BrewMode.Profile)))
        assertFalse(capture.pending)
        capture.arm(ManualPriority.Pressure, 7, null)
        assertNull(capture.updateSession(session(true, user = 8)))
        assertFalse(capture.pending)
        capture.arm(ManualPriority.Pressure, 7, null)
        capture.updateDevice(pressure.copy(freeHandControlMode = FreeHandControlMode.Flow))
        assertFalse(capture.pending)
        capture.arm(ManualPriority.Pressure, 7, null)
        capture.updateDevice(pressure)
        capture.updateSession(session(true))
        capture.updateSession(session(false))
        assertNull(capture.updateDevice(idle.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected)))
        capture.updateDevice(idle)
        assertNull(capture.updateSession(session(false)))
        assertFalse(capture.pending)
    }

    @Test
    fun flowAndMissingOrInvalidRecordingsPreserveTheActualCompletedShotForAnExplicitForm() {
        val capture = HomeManualRecipeCapture()
        capture.updateDevice(idle)
        capture.arm(ManualPriority.Flow, 7, null)
        capture.updateDevice(pressure.copy(freeHandControlMode = FreeHandControlMode.Flow))
        capture.updateSession(session(true, FreeHandControlMode.Flow))
        capture.updateDevice(idle)
        val flow = assertNotNull(capture.updateSession(session(false, FreeHandControlMode.Flow)))
        assertEquals(FreeHandControlMode.Flow, flow.recording?.controlMode)
        assertNull(manualRecipeProblem(flow))
        val zero = flow.copy(recording = flow.recording!!.copy(samples = flow.recording!!.samples.map { it.copy(data = it.data.copy(volume = 0f)) }))
        assertNotNull(manualRecipeProblem(zero))
        assertNotNull(manualRecipeProblem(flow.copy(recording = null)))
        assertNotNull(manualRecipeProblem(flow.copy(recording = flow.recording!!.copy(samples = listOf(FreeHandSample(0, point.copy(pressure = Float.NaN)))))))
        capture.arm(ManualPriority.Pressure, 7, null)
        assertNull(capture.takeMissingRecording(), "A start command without confirmed activity must not create a recipe form")
        capture.updateDevice(pressure)
        assertNull(capture.takeMissingRecording(), "A running shot must not be presented as finished")
        capture.updateDevice(idle)
        val missing = assertNotNull(capture.takeMissingRecording())
        assertNull(missing.recording)
        assertNotNull(manualRecipeProblem(missing))
        assertFalse(capture.pending)
    }

    @Test
    fun cancelNeverPersistsAndSaveCapturesFieldsOnceWhileFailuresRetainAReusableDraft() = runTest {
        val calls = mutableListOf<Triple<BrewSession, String, String>>()
        var succeed = false
        val deferred = CompletableDeferred<Unit>()
        var saved = 0
        val coordinator = HomeManualRecipeSave(backgroundScope, { recording, name, description ->
            calls += Triple(recording, name, description)
            deferred.await()
            check(succeed) { "disk failed" }
        }, { it.message.orEmpty() }, { saved++ })
        val recording = session(false)
        coordinator.offer(recording, true)
        coordinator.nameChanged("Discarded")
        assertTrue(coordinator.cancel())
        coordinator.save(); runCurrent()
        assertTrue(calls.isEmpty())
        coordinator.offer(recording, true)
        coordinator.save(); runCurrent()
        assertTrue(calls.isEmpty(), "A blank recipe name must not create an unnamed profile")
        coordinator.nameChanged("  Morning  ")
        coordinator.descriptionChanged("  Slow finish  ")
        coordinator.save(); coordinator.save(); runCurrent()
        assertEquals(1, calls.size)
        assertEquals(Triple(recording, "Morning", "Slow finish"), calls.single())
        assertTrue(coordinator.draft.value!!.saving)
        assertFalse(coordinator.cancel(), "Cancel cannot pretend an in-flight save was undone")
        coordinator.nameChanged("Changed while saving")
        deferred.complete(Unit); runCurrent()
        assertEquals("disk failed", coordinator.draft.value!!.errorMessage)
        assertEquals("  Morning  ", coordinator.draft.value!!.name)
        assertFalse(coordinator.draft.value!!.saving)
        succeed = true
        coordinator.save(); runCurrent()
        assertEquals(2, calls.size)
        assertEquals(1, saved)
        assertNull(coordinator.draft.value)
        assertFalse(coordinator.pending)
        coordinator.offer(recording.copy(recording = null), false, "No data")
        coordinator.nameChanged("Invalid")
        coordinator.save(); runCurrent()
        assertEquals(2, calls.size)
        assertTrue(coordinator.cancel())
    }
}
