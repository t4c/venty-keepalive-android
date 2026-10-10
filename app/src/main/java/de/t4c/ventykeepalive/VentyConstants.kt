package de.t4c.ventykeepalive

import java.util.UUID

object VentyConstants {
    val SERVICE_UUID: UUID = UUID.fromString("00000000-5354-4f52-5a26-4249434b454c")
    val COMM_CHAR_UUID: UUID = UUID.fromString("00000001-5354-4f52-5a26-4249434b454c")
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val DEVICE_NAME_PREFIX = "S&B VY"

    // Command frame prefixes
    val STATUS_REQUEST_FRAME = byteArrayOf(
        0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00
    )

    fun createHeaterModeFrame(mode: Int): ByteArray {
        val frame = ByteArray(20)
        frame[0] = 0x01.toByte() // STATUS
        frame[1] = 0x20.toByte() // StatusWriteMask.HEATER (1 << 5)
        frame[11] = mode.toByte()
        return frame
    }

    fun createDisableVibrationFrame(): ByteArray {
        val frame = ByteArray(7)
        frame[0] = 0x06.toByte() // Command.BRIGHTNESS_VIBRATION
        frame[1] = 0x08.toByte() // BrightnessVibrationWriteMask.VIBRATION (1 << 3)
        frame[5] = 0x00.toByte() // 0 = disabled
        return frame
    }
}

enum class HeaterMode(val code: Int, val label: String) {
    STANDBY(0, "Standby"),
    HEATING(1, "Heating"),
    BOOST(2, "Boost"),
    SUPERBOOST(3, "Superboost");

    companion object {
        fun fromCode(code: Int): HeaterMode = entries.firstOrNull { it.code == code } ?: STANDBY
    }
}

data class VentyState(
    val connected: Boolean = false,
    val deviceName: String = "",
    val deviceAddress: String = "",
    val actualTemp: Float = 0.0f,
    val targetTemp: Float = 180.0f,
    val battery: Int = 0,
    val timerSeconds: Int = 120,
    val heaterMode: HeaterMode = HeaterMode.STANDBY,
    val lastResetTimeMillis: Long = 0,
    val resetCount: Int = 0,
    val statusMessage: String = "Idle"
)
