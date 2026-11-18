package app.geeflow.domain.user.usecase

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.impl.UserSettingsRepositoryImpl
import app.geeflow.data.user.model.HomeDockAction
import app.geeflow.data.user.model.User
import io.mockk.every
import io.mockk.mockk
import java.nio.file.Files
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.*

class HomeDockOrderUseCaseTest {
    @Test
    fun separateUserOrdersSurviveADataStoreReloadAndNoUserCannotOverwriteThem() = runTest {
        val file = Files.createTempDirectory("dock-order").resolve("settings.preferences_pb")
        val selected = MutableStateFlow<User?>(User(id = 7, name = "A"))
        val users = mockk<UserRepository> { every { selectedUser } returns selected }
        var job = SupervisorJob()
        fun open() = PreferenceDataStoreFactory.createWithPath(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file.toString().toPath() })
        var store = open()
        var repository = UserSettingsRepositoryImpl(store)
        val firstOrder = HomeDockAction.defaultOrder.reversed()
        SetHomeDockOrderUseCase(repository, users)(firstOrder)
        selected.value = User(id = 8, name = "B")
        assertEquals(HomeDockAction.defaultOrder, GetHomeDockOrderUseCase(repository, users)().first())
        val secondOrder = listOf(HomeDockAction.Charts) + HomeDockAction.defaultOrder.filterNot { it == HomeDockAction.Charts }
        SetHomeDockOrderUseCase(repository, users)(secondOrder)
        selected.value = null
        SetHomeDockOrderUseCase(repository, users)(HomeDockAction.defaultOrder)
        job.cancelAndJoin()
        job = SupervisorJob()
        store = open()
        repository = UserSettingsRepositoryImpl(store)
        try {
            selected.value = User(id = 7, name = "A")
            assertEquals(firstOrder, GetHomeDockOrderUseCase(repository, users)().first())
            selected.value = User(id = 8, name = "B")
            assertEquals(secondOrder, GetHomeDockOrderUseCase(repository, users)().first())
        } finally { job.cancelAndJoin(); file.toFile().parentFile.deleteRecursively() }
    }

    @Test
    fun unknownAndRepeatedSavedKeysRecoverAndClearingOneUserPreservesAnother() = runTest {
        val file = Files.createTempDirectory("dock-invalid-order").resolve("settings.preferences_pb")
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.createWithPath(scope = CoroutineScope(job + Dispatchers.IO), produceFile = { file.toString().toPath() })
        val repository = UserSettingsRepositoryImpl(store)
        try {
            store.edit { it[stringPreferencesKey("home_dock_order_7")] = "scale,future-action,scale,charts" }
            val recovered = repository.homeDockOrder(7).first()
            assertEquals(listOf(HomeDockAction.Scale, HomeDockAction.Charts), recovered.take(2))
            assertEquals(HomeDockAction.entries.toSet(), recovered.toSet())
            assertEquals(HomeDockAction.entries.size, recovered.size)
            repository.setHomeDockOrder(8, HomeDockAction.defaultOrder.reversed())
            repository.clearUserSettings(7)
            assertEquals(HomeDockAction.defaultOrder, repository.homeDockOrder(7).first())
            assertEquals(HomeDockAction.defaultOrder.reversed(), repository.homeDockOrder(8).first())
        } finally { job.cancelAndJoin(); file.toFile().parentFile.deleteRecursively() }
    }
}
