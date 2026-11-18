package app.geeflow.presentation.feature.device.settings.main

data class DeviceSettingsViewState(
    val items: List<Item> = emptyList(),
) {
    sealed interface Item {
        val name: String

        data class Brewing(override val name: String) : Item
        data class Maintenance(override val name: String) : Item
        data class Connectivity(override val name: String) : Item
    }
}
