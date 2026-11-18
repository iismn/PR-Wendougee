package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.HomeDockAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.koin.core.annotation.Factory

@Factory
class GetHomeDockOrderUseCase(private val repository: UserSettingsRepository, private val users: UserRepository) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<HomeDockAction>> = users.selectedUser.flatMapLatest { user ->
        user?.let { repository.homeDockOrder(it.id) } ?: flowOf(HomeDockAction.defaultOrder)
    }
}
