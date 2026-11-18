package app.geeflow.domain.device.usecase

import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceManufacturer
import app.geeflow.data.device.model.SupportedDevice
import org.koin.core.annotation.Factory

/** Manual entry accepts the QR contents or the machine's Bluetooth hardware address. */
@Factory
class ParseDeviceCodeUseCase(private val parseQrCode: ParseDeviceQrCodeUseCase) {
    operator fun invoke(input: String): Device? {
        val code = input.trim()
        if (code.isEmpty()) return null
        if ('?' in code) return parseQrCode(code)
        if (code.startsWith('{')) return parseQrCode.parseJson(code)

        val plainMac = code.replace(Regex("[:\\-\\s]"), "")
        val rawMac = if (plainMac.matches(Regex("[0-9a-fA-F]{12}"))) plainMac else {
            val match = Regex("([0-9a-fA-F]{12})$").find(code) ?: return null
            val prefix = code.substring(0, match.range.first)
            if (listOf("wendougee", "wdg", "lita").none { prefix.contains(it, ignoreCase = true) }) return null
            match.value
        }
        val mac = rawMac.uppercase().chunked(2).joinToString(":")
        val model = SupportedDevice.fromDeviceName(code)
        return Device(
            name = model?.let { "${it.manufacturer} ${it.model}" } ?: DeviceManufacturer.WENDOUGEE,
            connection = DeviceConnection.Ble(peripheralId = mac, macAddress = mac),
            manufacturer = DeviceManufacturer.WENDOUGEE,
            model = model?.model.orEmpty(),
            modelId = model?.id,
            modelSource = model?.let { "manual-name" },
        )
    }
}
