@file:Suppress("TooManyFunctions")

package app.geeflow.data.user.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.geeflow.data.user.UserSettingsRepository
import app.geeflow.data.user.model.AppPaletteStyle
import app.geeflow.data.user.model.AppTheme
import app.geeflow.domain.ClockThemeSchedule
import app.geeflow.data.user.model.HomeDockAction
import app.geeflow.data.user.model.ChartType
import app.geeflow.data.user.model.TemperatureUnit
import app.geeflow.data.user.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class UserSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : UserSettingsRepository {

    override fun homeDockOrder(userId: Long): Flow<List<HomeDockAction>> = dataStore.data.map { preferences ->
        HomeDockAction.decode(preferences[homeDockOrderKey(userId)])
    }

    override suspend fun setHomeDockOrder(userId: Long, order: List<HomeDockAction>) {
        dataStore.edit { it[homeDockOrderKey(userId)] = HomeDockAction.encode(order) }
    }

    override fun visibleCharts(userId: Long): Flow<Set<ChartType>> = dataStore.data.map { preferences ->
        preferences[visibleChartsKey(userId)]
            ?.mapNotNull { ChartType.entries.find { entry -> entry.name == it } }
            ?.toSet()
            ?: setOf(ChartType.PRESSURE, ChartType.FLOW_RATE, ChartType.WEIGHT_RATE)
    }

    override fun darkMode(userId: Long): Flow<ThemeMode> = dataStore.data.map { preferences ->
        preferences[darkModeKey(userId)]
            ?.let { ThemeMode.entries.find { entry -> entry.name == it } }
            ?: ThemeMode.CLOCK
    }

    override fun clockThemeSchedule(userId: Long): Flow<ClockThemeSchedule> = dataStore.data.map { preferences ->
        ClockThemeSchedule.decode(preferences[clockThemeScheduleKey(userId)])
    }

    override suspend fun setClockThemeSchedule(userId: Long, schedule: ClockThemeSchedule) {
        dataStore.edit { it[clockThemeScheduleKey(userId)] = schedule.encode() }
    }

    override fun appTheme(userId: Long): Flow<AppTheme> = dataStore.data.map { preferences ->
        preferences[appThemeKey(userId)]
            ?.let { AppTheme.entries.find { entry -> entry.name == it } }
            ?: AppTheme.ESPRESSO
    }

    override fun paletteStyle(userId: Long): Flow<AppPaletteStyle> = dataStore.data.map { preferences ->
        preferences[paletteStyleKey(userId)]
            ?.let { AppPaletteStyle.entries.find { entry -> entry.name == it } }
            ?: AppPaletteStyle.TONAL_SPOT
    }

    override fun customSeedColor(userId: Long): Flow<Int> = dataStore.data.map { preferences ->
        preferences[customSeedColorKey(userId)] ?: DefaultCustomSeedColor
    }

    override fun textSizePercent(userId: Long): Flow<Int> = dataStore.data.map { preferences ->
        (preferences[textSizePercentKey(userId)] ?: 120).coerceIn(100, 160)
    }

    override fun fullScreenMode(userId: Long): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[fullScreenModeKey(userId)] ?: false
    }

    override fun keepScreenOn(userId: Long): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[keepScreenOnKey(userId)] ?: false
    }

    override fun keepScreenOnOnlyWhileCharging(userId: Long): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[keepScreenOnOnlyWhileChargingKey(userId)] ?: false
    }

    override fun screensaverEnabled(userId: Long): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[screensaverEnabledKey(userId)] ?: false
    }

    override fun screensaverTimeoutMinutes(userId: Long): Flow<Int> = dataStore.data.map { preferences ->
        preferences[screensaverTimeoutMinutesKey(userId)] ?: DefaultScreensaverTimeoutMinutes
    }

    override fun screensaverSizePercent(userId: Long): Flow<Int> = dataStore.data.map { preferences ->
        (preferences[screensaverSizePercentKey(userId)] ?: 100).coerceIn(60, 200)
    }

    override fun autoConnect(userId: Long): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[autoConnectKey(userId)] ?: true
    }

    override fun temperatureUnit(userId: Long): Flow<TemperatureUnit> = dataStore.data.map { preferences ->
        preferences[temperatureUnitKey(userId)]
            ?.let { TemperatureUnit.entries.find { entry -> entry.name == it } }
            ?: TemperatureUnit.CELSIUS
    }

    override suspend fun setVisibleCharts(userId: Long, types: Set<ChartType>) {
        dataStore.edit { preferences ->
            preferences[visibleChartsKey(userId)] = types.map { it.name }.toSet()
        }
    }

    override suspend fun setDarkMode(userId: Long, mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[darkModeKey(userId)] = mode.name
        }
    }

    override suspend fun setAppTheme(userId: Long, theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[appThemeKey(userId)] = theme.name
        }
    }

    override suspend fun setPaletteStyle(userId: Long, style: AppPaletteStyle) {
        dataStore.edit { preferences ->
            preferences[paletteStyleKey(userId)] = style.name
        }
    }

    override suspend fun setCustomSeedColor(userId: Long, colorArgb: Int) {
        dataStore.edit { preferences ->
            preferences[customSeedColorKey(userId)] = colorArgb
        }
    }

    override suspend fun setTextSizePercent(userId: Long, percent: Int) {
        dataStore.edit { preferences -> preferences[textSizePercentKey(userId)] = percent.coerceIn(100, 160) }
    }

    override suspend fun setFullScreenMode(userId: Long, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[fullScreenModeKey(userId)] = enabled
        }
    }

    override suspend fun setKeepScreenOn(userId: Long, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[keepScreenOnKey(userId)] = enabled
        }
    }

    override suspend fun setKeepScreenOnOnlyWhileCharging(userId: Long, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[keepScreenOnOnlyWhileChargingKey(userId)] = enabled
        }
    }

    override suspend fun setScreensaverEnabled(userId: Long, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[screensaverEnabledKey(userId)] = enabled
        }
    }

    override suspend fun setScreensaverTimeoutMinutes(userId: Long, minutes: Int) {
        dataStore.edit { preferences ->
            preferences[screensaverTimeoutMinutesKey(userId)] = minutes
        }
    }

    override suspend fun setScreensaverSizePercent(userId: Long, percent: Int) {
        dataStore.edit { it[screensaverSizePercentKey(userId)] = percent.coerceIn(60, 200) }
    }

    override suspend fun setAutoConnect(userId: Long, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[autoConnectKey(userId)] = enabled
        }
    }

    override suspend fun setTemperatureUnit(userId: Long, unit: TemperatureUnit) {
        dataStore.edit { preferences ->
            preferences[temperatureUnitKey(userId)] = unit.name
        }
    }

    override suspend fun clearUserSettings(userId: Long) {
        dataStore.edit { preferences ->
            preferences.remove(homeDockOrderKey(userId))
            preferences.remove(visibleChartsKey(userId))
            preferences.remove(darkModeKey(userId))
            preferences.remove(clockThemeScheduleKey(userId))
            preferences.remove(appThemeKey(userId))
            preferences.remove(fullScreenModeKey(userId))
            preferences.remove(textSizePercentKey(userId))
            preferences.remove(keepScreenOnKey(userId))
            preferences.remove(keepScreenOnOnlyWhileChargingKey(userId))
            preferences.remove(screensaverEnabledKey(userId))
            preferences.remove(screensaverTimeoutMinutesKey(userId))
            preferences.remove(screensaverSizePercentKey(userId))
            preferences.remove(autoConnectKey(userId))
            preferences.remove(temperatureUnitKey(userId))
            preferences.remove(paletteStyleKey(userId))
            preferences.remove(customSeedColorKey(userId))
        }
    }

    private fun key(name: String, userId: Long) = "${name}_$userId"

    private fun homeDockOrderKey(userId: Long) = stringPreferencesKey(key("home_dock_order", userId))
    private fun visibleChartsKey(userId: Long) = stringSetPreferencesKey(key("visible_charts", userId))
    private fun darkModeKey(userId: Long) = stringPreferencesKey(key("dark_mode", userId))
    private fun clockThemeScheduleKey(userId: Long) = stringPreferencesKey(key("clock_theme_schedule", userId))
    private fun appThemeKey(userId: Long) = stringPreferencesKey(key("app_theme", userId))
    private fun paletteStyleKey(userId: Long) = stringPreferencesKey(key("palette_style", userId))
    private fun customSeedColorKey(userId: Long) = intPreferencesKey(key("custom_seed_color", userId))
    private fun textSizePercentKey(userId: Long) = intPreferencesKey(key("text_size_percent", userId))
    private fun fullScreenModeKey(userId: Long) = booleanPreferencesKey(key("full_screen_mode", userId))
    private fun keepScreenOnKey(userId: Long) = booleanPreferencesKey(key("keep_screen_on", userId))
    private fun keepScreenOnOnlyWhileChargingKey(userId: Long) =
        booleanPreferencesKey(key("keep_screen_on_only_while_charging", userId))
    private fun screensaverEnabledKey(userId: Long) = booleanPreferencesKey(key("screensaver_enabled", userId))
    private fun screensaverTimeoutMinutesKey(userId: Long) = intPreferencesKey(key("screensaver_timeout_minutes", userId))
    private fun screensaverSizePercentKey(userId: Long) = intPreferencesKey(key("screensaver_size_percent", userId))
    private fun autoConnectKey(userId: Long) = booleanPreferencesKey(key("auto_connect", userId))
    private fun temperatureUnitKey(userId: Long) = stringPreferencesKey(key("temperature_unit", userId))

    companion object {
        private const val DefaultCustomSeedColor = 0xFF1E88E5.toInt()
        private const val DefaultScreensaverTimeoutMinutes = 5
    }
}
