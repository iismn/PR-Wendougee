package app.geeflow.presentation.feature.device.add

import app.geeflow.data.device.model.Device

internal data class AddDeviceViewState(
    val method: Method = Method.NearbyDevices(),
    val dialog: Dialog? = null,
) {
    sealed interface Method {
        val changeMethodButtonVisible: Boolean

        data class NearbyDevices(
            override val changeMethodButtonVisible: Boolean = true,
            val showMissingPermissionMessage: Boolean = false,
            val devices: List<DeviceItem> = emptyList(),
        ) : Method

        data class QrCodeScanner(
            override val changeMethodButtonVisible: Boolean = true,
            val scanningEnabled: Boolean = true,
            val requestId: Int = 0,
            val nativeError: String? = null,
        ) : Method

        data class ManualInput(
            override val changeMethodButtonVisible: Boolean = true,
            val invalidCode: Boolean = false,
        ) : Method
    }

    data class DeviceItem(
        val id: String, // BLE peripheral id (transient — only valid until persisted)
        val name: String,
    )

    sealed interface Dialog {
        data class ModelSelection(val device: Device) : Dialog
        data class ExperimentalWarning(val device: Device) : Dialog
    }
}
