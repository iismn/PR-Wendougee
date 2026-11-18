package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.domain.ClockThemeSchedule
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SetClockThemeScheduleUseCase(private val repository: UserSettingsRepository, private val users: UserRepository) {
    suspend operator fun invoke(schedule: ClockThemeSchedule) {
        val userId = users.selectedUser.first()?.id ?: return
        repository.setClockThemeSchedule(userId, schedule)
    }
}
