package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.User
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SetTextSizePercentUseCaseTest {
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))
    private val users = mockk<UserRepository> { every { selectedUser } returns selected }
    private val settings = mockk<UserSettingsRepository>(relaxUnitFun = true)

    @Test
    fun textSizeIsBoundedAndSavedToTheSelectedProfile() = runTest {
        val change = SetTextSizePercentUseCase(settings, users)
        change(140)
        change(999)
        change(1)
        coVerifySequence {
            settings.setTextSizePercent(7, 140)
            settings.setTextSizePercent(7, 160)
            settings.setTextSizePercent(7, 100)
        }
    }

    @Test
    fun noProfileDoesNotWriteAnotherUsersSettings() = runTest {
        selected.value = null
        SetTextSizePercentUseCase(settings, users)(140)
        coVerify { settings wasNot Called }
    }
}
