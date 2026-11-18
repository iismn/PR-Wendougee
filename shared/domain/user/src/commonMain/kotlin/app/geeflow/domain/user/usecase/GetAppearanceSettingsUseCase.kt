package app.geeflow.domain.user.usecase

import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.domain.user.model.AppearanceSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapNotNull
import org.koin.core.annotation.Factory

@Factory
class GetAppearanceSettingsUseCase(
    private val repository: UserSettingsRepository,
    private val userRepository: UserRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<AppearanceSettings> = userRepository.selectedUser
        .mapNotNull { it?.id }
        .flatMapLatest { userId ->
            combine(
                combine(
                    combine(repository.darkMode(userId), repository.clockThemeSchedule(userId)) { mode, schedule ->
                        mode to schedule
                    },
                    repository.appTheme(userId),
                    repository.paletteStyle(userId),
                    repository.customSeedColor(userId),
                    combine(repository.textSizePercent(userId), repository.screensaverSizePercent(userId)) { text, clock -> text to clock },
                ) { theme, appTheme, paletteStyle, customSeedColor, scales ->
                    AppearanceSettings(
                        themeMode = theme.first,
                        clockThemeSchedule = theme.second,
                        appTheme = appTheme,
                        paletteStyle = paletteStyle,
                        customSeedColor = customSeedColor,
                        textSizePercent = scales.first,
                        screensaverSizePercent = scales.second,
                    )
                },
                combine(
                    repository.fullScreenMode(userId),
                    repository.keepScreenOn(userId),
                    repository.keepScreenOnOnlyWhileCharging(userId),
                    repository.screensaverEnabled(userId),
                    repository.screensaverTimeoutMinutes(userId),
                ) { fullScreenMode, keepScreenOn, onlyWhileCharging, screensaverEnabled, screensaverTimeoutMinutes ->
                    ScreenSettings(
                        fullScreenMode = fullScreenMode,
                        keepScreenOn = keepScreenOn,
                        keepScreenOnOnlyWhileCharging = onlyWhileCharging,
                        screensaverEnabled = screensaverEnabled,
                        screensaverTimeoutMinutes = screensaverTimeoutMinutes,
                    )
                },
            ) { settings, screenSettings ->
                settings.copy(
                    fullScreenMode = screenSettings.fullScreenMode,
                    keepScreenOn = screenSettings.keepScreenOn,
                    keepScreenOnOnlyWhileCharging = screenSettings.keepScreenOnOnlyWhileCharging,
                    screensaverEnabled = screenSettings.screensaverEnabled,
                    screensaverTimeoutMinutes = screenSettings.screensaverTimeoutMinutes,
                )
            }
        }
}

private data class ScreenSettings(
    val fullScreenMode: Boolean,
    val keepScreenOn: Boolean,
    val keepScreenOnOnlyWhileCharging: Boolean,
    val screensaverEnabled: Boolean,
    val screensaverTimeoutMinutes: Int,
)
