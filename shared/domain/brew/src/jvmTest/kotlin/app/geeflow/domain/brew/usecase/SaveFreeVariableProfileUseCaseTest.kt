package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.FreeHandControlMode
import app.geeflow.data.brew.model.FreeHandRecording
import app.geeflow.data.brew.model.FreeHandSample
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import app.geeflow.domain.exception.RecordingNoVolumeException
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SaveFreeVariableProfileUseCaseTest {
    private val profiles = mockk<BrewProfileRepository>(relaxUnitFun = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))
    private val getSelected = GetSelectedUserUseCase(users)
    private val point = BrewDataPoint(9f, 36f, 40f, 2f, 1f)

    init {
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when recording has volume then stores recording and volume finish condition`() = runTest {
        val recording = FreeHandRecording(FreeHandControlMode.Pressure, listOf(FreeHandSample(0, point)))
        val session = BrewSession(userId = 1, recording = recording)
        val useCase = SaveFreeVariableProfileUseCase(profiles, getSelected)

        useCase(session, "Recorded")

        verify(
            exactly = 1
        ) {
            profiles.addBrewProfile(
                BrewProfile(
                    userId = 7,
                    name = "Recorded",
                    description = "",
                    finishCondition = Condition.Volume(40f),
                    program = BrewProgram.Recording(recording)
                )
            )
        }
    }

    @Test
    fun `when recording has no volume then rejects saving`() = runTest {
        val recording =
            FreeHandRecording(FreeHandControlMode.Pressure, listOf(FreeHandSample(0, point.copy(volume = 0f))))
        val useCase = SaveFreeVariableProfileUseCase(profiles, getSelected)

        val result = runCatching { useCase(BrewSession(userId = 7, recording = recording), "Recorded") }

        assertTrue(result.exceptionOrNull() is RecordingNoVolumeException)
        verify(exactly = 0) { profiles.addBrewProfile(any()) }
    }

    @Test
    fun `completed pressure and flow recordings keep their original measurements with named metadata`() = runTest {
        val saved = mutableListOf<BrewProfile>()
        every { profiles.addBrewProfile(capture(saved)) } answers { Unit }
        val useCase = SaveFreeVariableProfileUseCase(profiles, getSelected)
        val recordings = FreeHandControlMode.entries.map { mode ->
            FreeHandRecording(mode, listOf(
                FreeHandSample(0, point.copy(pressure = 2f, volume = 1f)),
                FreeHandSample(500, point.copy(pressure = 6f, volume = 12f)),
                FreeHandSample(1000, point),
            ))
        }

        recordings.forEach { recording ->
            useCase(BrewSession(userId = 1, mode = BrewMode.Freehand, recording = recording),
                "  Morning recipe  ", "  Gentle start\nSweet finish  ")
        }

        assertEquals(2, saved.size)
        saved.zip(recordings).forEach { (profile, recording) ->
            assertEquals(7L, profile.userId, "Saving follows the currently selected recipe owner")
            assertEquals("Morning recipe", profile.name)
            assertEquals("Gentle start\nSweet finish", profile.description)
            assertEquals(recording, profile.recording, "Saving must not replace measured samples with UI target values")
            assertEquals(Condition.Volume(40f), profile.finishCondition)
        }
    }

    @Test
    fun `blank recipe name is rejected without creating a profile`() = runTest {
        val recording = FreeHandRecording(FreeHandControlMode.Pressure, listOf(FreeHandSample(0, point)))
        val useCase = SaveFreeVariableProfileUseCase(profiles, getSelected)

        val result = runCatching { useCase(BrewSession(userId = 7, recording = recording), " \t\n ", "Description") }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        verify(exactly = 0) { profiles.addBrewProfile(any()) }
    }

    @Test
    fun `an active recording cannot be saved before the extraction ends`() = runTest {
        val recording = FreeHandRecording(FreeHandControlMode.Flow, listOf(FreeHandSample(0, point)))
        val useCase = SaveFreeVariableProfileUseCase(profiles, getSelected)

        val result = runCatching {
            useCase(BrewSession(userId = 7, mode = BrewMode.Freehand, inProgress = true, recording = recording),
                "Still running", "Incomplete extraction")
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        verify(exactly = 0) { profiles.addBrewProfile(any()) }
    }

    @Test
    fun `missing or invalid recording data is rejected before persisting`() = runTest {
        val useCase = SaveFreeVariableProfileUseCase(profiles, getSelected)
        val recordings = listOf(
            null,
            FreeHandRecording(FreeHandControlMode.Pressure, emptyList()),
            FreeHandRecording(FreeHandControlMode.Pressure, listOf(FreeHandSample(500, point))),
            FreeHandRecording(FreeHandControlMode.Pressure, listOf(FreeHandSample(0, point), FreeHandSample(0, point))),
            FreeHandRecording(FreeHandControlMode.Flow, listOf(FreeHandSample(0, point.copy(flowRate = Float.NaN)))),
        )

        recordings.forEach { recording ->
            val result = runCatching { useCase(BrewSession(userId = 7, recording = recording), "Invalid") }
            assertTrue(result.isFailure, "Malformed recordings cannot become replayable recipes")
        }

        verify(exactly = 0) { profiles.addBrewProfile(any()) }
    }

    @Test
    fun `no selected user prevents a recipe being assigned to a default owner`() = runTest {
        selected.value = null
        val recording = FreeHandRecording(FreeHandControlMode.Pressure, listOf(FreeHandSample(0, point)))
        val useCase = SaveFreeVariableProfileUseCase(profiles, getSelected)

        val result = runCatching { useCase(BrewSession(userId = 7, recording = recording), "Recorded") }

        assertTrue(result.isFailure)
        verify(exactly = 0) { profiles.addBrewProfile(any()) }
    }

}
