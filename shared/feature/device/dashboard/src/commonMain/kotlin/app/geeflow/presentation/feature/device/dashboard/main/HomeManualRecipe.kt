package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.exception.RecordingNoVolumeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Instant

/** Associates an actual telemetry recording with a start requested from this particular home dial. */
internal class HomeManualRecipeCapture {
    private data class Requested(
        val userId: Long,
        val mode: FreeHandControlMode,
        val previousStart: Instant?,
        val armedGeneration: Long,
        var ownedStart: Instant? = null,
        var completed: BrewSession? = null,
        var observedActivity: Boolean = false,
    )
    private var requested: Requested? = null
    private var generation = 0L
    private var connected = false
    private var status = DeviceState.BrewStatus.Idle
    val pending: Boolean get() = requested != null
    val ownerId: Long? get() = requested?.userId

    fun arm(mode: ManualPriority, userId: Long, previous: BrewSession?) {
        requested = Requested(userId,
            if (mode == ManualPriority.Pressure) FreeHandControlMode.Pressure else FreeHandControlMode.Flow,
            previous?.startTime, generation)
    }

    fun abandon() { requested = null }

    /** Status and the final brew-session emission are independent collectors and can arrive in either order. */
    fun updateDevice(device: DeviceState): BrewSession? {
        generation++
        connected = device.connectionStatus == DeviceState.ConnectionStatus.Connected
        status = device.brewStatus
        if (!connected || status !in setOf(DeviceState.BrewStatus.Idle, DeviceState.BrewStatus.FreeVariable)) {
            abandon()
            return null
        }
        requested?.let { run ->
            if (status == DeviceState.BrewStatus.FreeVariable) {
                if (device.freeHandControlMode != run.mode) { abandon(); return null }
                run.observedActivity = true
            }
        }
        return finishIfConfirmed()
    }

    /** A genuinely confirmed short shot may finish before the session observer records any sample. */
    fun takeMissingRecording(): BrewSession? {
        val run = requested ?: return null
        if (!connected || status != DeviceState.BrewStatus.Idle || !run.observedActivity || run.completed != null) return null
        requested = null
        // This cannot be saved: it only lets the user dismiss a finished shot with an explicit explanation.
        return BrewSession(userId = run.userId, mode = BrewMode.Freehand)
    }

    fun updateSession(session: BrewSession): BrewSession? {
        val run = requested ?: return null
        val start = session.startTime ?: return null
        if (start == run.previousStart) return null
        if (session.userId != run.userId || session.mode != BrewMode.Freehand ||
            session.recording?.controlMode?.let { it != run.mode } == true) {
            // Never attach a different user's shot, a profile, or another regulator's recording.
            if (session.inProgress || run.ownedStart == start) abandon()
            return null
        }
        if (run.ownedStart == null) {
            if (!session.inProgress) return null // A cached finished shot is not a newly started extraction.
            run.ownedStart = start
        } else if (run.ownedStart != start) {
            if (session.inProgress) abandon()
            return null
        }
        if (!session.inProgress) run.completed = session.copy(
            dataPoints = session.dataPoints.toMap(),
            recording = session.recording?.let { it.copy(samples = it.samples.toList()) },
        )
        return finishIfConfirmed()
    }

    private fun finishIfConfirmed(): BrewSession? {
        val run = requested ?: return null
        val completed = run.completed ?: return null
        if (!connected || status != DeviceState.BrewStatus.Idle || generation <= run.armedGeneration) return null
        requested = null
        return completed
    }
}

/** Validate the frozen actual measurements, never a synthetic curve reconstructed from dial positions. */
internal fun manualRecipeProblem(session: BrewSession): Throwable? = runCatching {
    check(!session.inProgress) { "Extraction has not finished" }
    val recording = requireNotNull(session.recording) { "No free hand recording available" }
    recording.playbackPoints()
    if (recording.samples.last().data.volume <= 0f) throw RecordingNoVolumeException()
}.exceptionOrNull()

/** Holds the completed recording until explicit Save or Cancel; an error leaves the form retryable. */
internal class HomeManualRecipeSave(
    private val scope: CoroutineScope,
    private val persist: suspend (BrewSession, String, String) -> Unit,
    private val errorMessage: suspend (Throwable) -> String,
    private val onSaved: suspend () -> Unit,
) {
    private val mutable = MutableStateFlow<DeviceDashboardViewState.Dialog.ManualRecipe?>(null)
    val draft = mutable.asStateFlow()
    private var recording: BrewSession? = null
    val pending: Boolean get() = recording != null
    val ownerId: Long? get() = recording?.userId

    fun offer(session: BrewSession, canSave: Boolean, message: String? = null) {
        if (mutable.value?.saving == true) return
        recording = session
        mutable.value = DeviceDashboardViewState.Dialog.ManualRecipe(canSave = canSave, errorMessage = message)
    }

    fun setValidationMessage(message: String) {
        val current = mutable.value ?: return
        if (!current.canSave && !current.saving) mutable.value = current.copy(errorMessage = message)
    }

    fun nameChanged(value: String) { mutable.value?.takeUnless { it.saving }?.let { mutable.value = it.copy(name = value) } }
    fun descriptionChanged(value: String) { mutable.value?.takeUnless { it.saving }?.let { mutable.value = it.copy(description = value) } }
    fun cancel(): Boolean {
        if (mutable.value?.saving == true) return false
        recording = null
        mutable.value = null
        return true
    }

    fun save() {
        val form = mutable.value ?: return
        val session = recording ?: return
        if (form.saving || !form.canSave || form.name.isBlank()) return
        mutable.value = form.copy(saving = true, errorMessage = null)
        scope.launch {
            try {
                persist(session, form.name.trim(), form.description.trim())
                recording = null
                mutable.value = null
                onSaved()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                mutable.value = form.copy(saving = false, errorMessage = errorMessage(error))
            }
        }
    }
}
