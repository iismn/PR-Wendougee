package app.geeflow.domain.device.usecase

import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.SupportedDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseDeviceCodeUseCaseTest {
    private val parse = ParseDeviceCodeUseCase(ParseDeviceQrCodeUseCase())

    @Test
    fun `manual address accepts common separator formats without inventing a model`() {
        listOf("aabbccddeeff", "aa:bb:cc:dd:ee:ff", "AA-BB-CC-DD-EE-FF", "  AA BB CC DD EE FF  ").forEach {
            val device = parse(it)
            assertEquals(DeviceConnection.Ble("AA:BB:CC:DD:EE:FF", "AA:BB:CC:DD:EE:FF"), device?.connection)
            assertNull(device?.modelId)
        }
    }

    @Test
    fun `advertised LITA name preserves model choice and hardware address`() {
        val device = parse("LITA_BA_AABBCCDDEEFF")
        assertEquals(SupportedDevice.LitaBA.id, device?.modelId)
        assertEquals("manual-name", device?.modelSource)
        assertEquals("AA:BB:CC:DD:EE:FF", (device?.connection as? DeviceConnection.Ble)?.macAddress)
    }

    @Test
    fun `pasted QR url uses the same parser as the camera`() {
        val url = "https://machine/?data=%7B%22blemac%22%3A%22aabbccddeeff%22%2C%22name%22%3A%22LITA%20BA%22%7D"
        assertEquals(ParseDeviceQrCodeUseCase()(url), parse("  $url  "))
    }

    @Test
    fun `raw QR json keeps names containing URL special characters`() {
        val device = parse("""{"blemac":"aabbccddeeff","name":"LITA BA + A&B"}""")
        assertEquals("LITA BA + A&B", device?.name)
        assertEquals(SupportedDevice.LitaBA.id, device?.modelId)
    }

    @Test
    fun `serial numbers and malformed codes cannot register a device`() {
        listOf("", "123456", "serial-AABBCCDDEEFF", "AA:BB:CC:DD:EE:GG", "AABBCCDDEEFF00", "https://machine/", "{}").forEach {
            assertNull(parse(it), it)
        }
    }
}
