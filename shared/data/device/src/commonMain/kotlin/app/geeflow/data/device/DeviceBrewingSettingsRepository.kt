package app.geeflow.data.device

import app.geeflow.data.device.model.DeviceBrewingSettings
import kotlinx.coroutines.flow.Flow

interface DeviceBrewingSettingsRepository {
    fun observe(deviceId: Long): Flow<DeviceBrewingSettings>
    suspend fun save(deviceId: Long, settings: DeviceBrewingSettings)
    fun observeManualRecipePrompt(deviceId: Long): Flow<Boolean>
    suspend fun setManualRecipePrompt(deviceId: Long, enabled: Boolean)
    suspend fun remove(deviceId: Long)
}
