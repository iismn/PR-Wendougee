package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class SetScreensaverSizePercentUseCase(
    private val repository: UserSettingsRepository,
    private val users: UserRepository,
) {
    suspend operator fun invoke(percent: Int) {
        val userId = users.selectedUser.first()?.id ?: return
        repository.setScreensaverSizePercent(userId, percent.coerceIn(60, 200))
    }
}
