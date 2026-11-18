package app.geeflow.presentation.feature.user.settings.main

import androidx.navigation3.runtime.NavKey

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import app.geeflow.navigation.destination.UserList
import app.geeflow.navigation.destination.UserSettings
import app.geeflow.presentation.feature.user.settings.AboutSettings
import app.geeflow.presentation.feature.user.settings.AppearanceSettings
import app.geeflow.presentation.feature.user.settings.BrewingPreferencesSettings
import app.geeflow.presentation.feature.user.settings.ProfileSettings
import app.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.BackClicked
import app.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.ChangeUserClicked
import app.geeflow.presentation.feature.user.settings.main.UserSettingsEvent.ItemClicked
import app.geeflow.presentation.feature.user.settings.main.UserSettingsViewState.Item
import geeflow.shared.feature.user.settings.generated.resources.Res
import geeflow.shared.feature.user.settings.generated.resources.user_settings_about
import geeflow.shared.feature.user.settings.generated.resources.user_settings_appearance
import geeflow.shared.feature.user.settings.generated.resources.user_settings_brewing_preferences
import geeflow.shared.feature.user.settings.generated.resources.user_settings_profile
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class UserSettingsViewModel(
    private val getSelectedUserUseCase: GetSelectedUserUseCase,
) : BaseViewModel<UserSettingsViewState, Unit>(UserSettingsViewState()) {

    init {
        buildItems()
        launch {
            getSelectedUserUseCase().collect { user ->
                if (user != null) modify { copy(userName = user.name) }
            }
        }
    }

    fun handleEvent(event: UserSettingsEvent, settingsRoot: NavKey = UserSettings) = when (event) {
        BackClicked -> popTo(settingsRoot, inclusive = true)
        ChangeUserClicked -> navigateTo(UserList)
        is ItemClicked -> {
            popTo(settingsRoot, false)
            when (event.item) {
                is Item.Profile -> navigateTo(ProfileSettings)
                is Item.BrewingPreferences -> navigateTo(BrewingPreferencesSettings)
                is Item.AppearanceDisplay -> navigateTo(AppearanceSettings)
                is Item.About -> navigateTo(AboutSettings)
            }
        }
    }

    private fun buildItems() = launch {
        val items = listOf(
            Item.AppearanceDisplay(getString(Res.string.user_settings_appearance)),
            Item.BrewingPreferences(getString(Res.string.user_settings_brewing_preferences)),
            Item.About(getString(Res.string.user_settings_about)),
            Item.Profile(getString(Res.string.user_settings_profile)),
        )
        modify { copy(items = items) }
    }
}
