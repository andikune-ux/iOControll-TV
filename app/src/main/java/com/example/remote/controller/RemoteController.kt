package dev.andikuneiocontroll.remote.controller

import android.content.Context
import dev.andikuneiocontroll.remote.discovery.DiscoveredTv
import dev.andikuneiocontroll.remote.protocol.ProtocolDetector
import dev.andikuneiocontroll.remote.protocol.TvCommand
import dev.andikuneiocontroll.remote.protocol.TvProtocol
import dev.andikuneiocontroll.remote.protocol.adb.AdbTvClient
import dev.andikuneiocontroll.remote.protocol.lg.LgWebOsClient
import dev.andikuneiocontroll.remote.protocol.philips.PhilipsClient
import dev.andikuneiocontroll.remote.protocol.roku.RokuEcpClient
import dev.andikuneiocontroll.remote.protocol.samsung.SamsungTizenClient
import dev.andikuneiocontroll.remote.protocol.vizio.VizioClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


/**
 * RemoteController — Facade untuk semua protokol TV.
 *
 * Tugas:
 * 1. Terima DiscoveredTv, pilih protokol yang cocok
 * 2. Forward command ke protokol aktif
 * 3. Track status koneksi
 * 4. Handle disconnect
 */
class RemoteController(context: Context) {

    private val appContext = context.applicationContext

    // Semua protokol yang tersedia
    private val adbClient: TvProtocol = AdbTvClient(appContext)
    private val rokuClient: TvProtocol = RokuEcpClient(appContext)
    private val samsungClient: TvProtocol = SamsungTizenClient(appContext)
    private val lgClient: TvProtocol = LgWebOsClient(appContext)
    private val philipsClient: TvProtocol = PhilipsClient(appContext)
    private val vizioClient: TvProtocol = VizioClient(appContext)

    private val allProtocols: List<TvProtocol> = listOf(
        adbClient,
        rokuClient,
        samsungClient,
        lgClient,
        philipsClient,
        vizioClient
    )

    // Protokol yang sedang aktif
    private var activeProtocol: TvProtocol? = null

    // State koneksi
    private val _connectionState = MutableStateFlow(RemoteConnectionState())
    val connectionState: StateFlow<RemoteConnectionState> = _connectionState.asStateFlow()

    // ==========================================
    // CONNECT
    // ==========================================

    suspend fun connect(
        tv: DiscoveredTv,
        pairingCode: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        // Disconnect dulu kalau ada yang aktif
        if (activeProtocol != null) {
            disconnect()
        }

        // Pilih protokol yang cocok
        val protocol = pickProtocol(tv)
        if (protocol == null) {
            onResult(false, "Protokol tidak didukung untuk ${tv.brand}")
            _connectionState.value = RemoteConnectionState(
                isConnected = false,
                error = "Protokol tidak didukung"
            )
            return
        }

        // Update state: connecting
        _connectionState.value = RemoteConnectionState(
            isConnecting = true,
            tv = tv,
            protocolName = protocol.protocolName
        )

        // Connect
        protocol.connect(tv, pairingCode) { success, message ->
            if (success) {
                activeProtocol = protocol
                _connectionState.value = RemoteConnectionState(
                    isConnected = true,
                    tv = tv,
                    protocolName = protocol.protocolName,
                    connectedAt = System.currentTimeMillis()
                )
            } else {
                activeProtocol = null
                _connectionState.value = RemoteConnectionState(
                    isConnected = false,
                    tv = tv,
                    protocolName = protocol.protocolName,
                    error = message
                )
            }
            onResult(success, message)
        }
    }

    private fun pickProtocol(tv: DiscoveredTv): TvProtocol? {
        // Coba cari yang canHandle
        val candidates = allProtocols.filter { it.canHandle(tv) }
        if (candidates.isNotEmpty()) {
            return candidates.first()
        }

        // Fallback: pakai ProtocolDetector
        val protocolId = ProtocolDetector.detect(tv)
        return when (protocolId) {
            "ANDROID_TV_V2" -> adbClient
            "ROKU_ECP" -> rokuClient
            "SAMSUNG_TIZEN" -> samsungClient
            "LG_WEBOS" -> lgClient
            "PHILIPS_JOINTSPACE" -> philipsClient
            "VIZIO_SMARTCAST" -> vizioClient
            else -> null
        }
    }

    // ==========================================
    // DISCONNECT
    // ==========================================

    suspend fun disconnect() {
        try {
            activeProtocol?.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        activeProtocol = null
        _connectionState.value = RemoteConnectionState()
    }

    // ==========================================
    // SEND COMMAND
    // ==========================================

    suspend fun sendCommand(command: String, payload: String = ""): Boolean {
        val protocol = activeProtocol ?: return false
        if (!protocol.isConnected()) return false

        return try {
            protocol.sendCommand(command, payload)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isConnected(): Boolean {
        return activeProtocol?.isConnected() ?: false
    }

    fun getActiveProtocolName(): String {
        return activeProtocol?.protocolName ?: ""
    }

    fun getConnectedTv(): DiscoveredTv? {
        val state = _connectionState.value
        return if (state.isConnected) state.tv else null
    }

    fun clearError() {
        val current = _connectionState.value
        _connectionState.value = current.copy(error = "")
    }
    // ==========================================
// PAIRING (khusus ADB Wireless Debugging)
// ==========================================

/**
 * Pair ke TV (khusus ADB Wireless Debugging).
 * Setelah sukses, panggil connect() lagi.
 */
suspend fun pair(
    host: String,
    pairingPort: Int,
    pairingCode: String
): Pair<Boolean, String> {
    return try {
        // Cari protokol yang support pairing (AdbTvClient)
        val adbProtocol = allProtocols.firstOrNull { it is AdbTvClient } as? AdbTvClient
        if (adbProtocol == null) {
            return false to "Protokol ADB tidak tersedia"
        }
        adbProtocol.pair(host, pairingPort, pairingCode)
    } catch (e: Exception) {
        e.printStackTrace()
        false to "Error pairing: ${e.message ?: "Unknown"}"
    }
}

/**
 * Cek apakah protokol aktif butuh pairing.
 * Return: (needsPairing, host, port) — null kalau tidak butuh
 */
fun getPairingInfo(): Triple<Boolean, String, Int>? {
    val adbProtocol = allProtocols.firstOrNull { it is AdbTvClient } as? AdbTvClient
        ?: return null
    return Triple(adbProtocol.needsPairing, adbProtocol.pairingHost, adbProtocol.pairingPort)
}

/**
 * Reset pairing untuk TV tertentu.
 */
fun clearPairing(host: String? = null) {
    val adbProtocol = allProtocols.firstOrNull { it is AdbTvClient } as? AdbTvClient
    adbProtocol?.clearPairing(host)
}

    // ==========================================
    // QUICK COMMANDS
    // ==========================================

    suspend fun dpadUp() = sendCommand(TvCommand.DPAD_UP)
    suspend fun dpadDown() = sendCommand(TvCommand.DPAD_DOWN)
    suspend fun dpadLeft() = sendCommand(TvCommand.DPAD_LEFT)
    suspend fun dpadRight() = sendCommand(TvCommand.DPAD_RIGHT)
    suspend fun dpadOk() = sendCommand(TvCommand.DPAD_OK)
    suspend fun home() = sendCommand(TvCommand.HOME)
    suspend fun back() = sendCommand(TvCommand.BACK)
    suspend fun power() = sendCommand(TvCommand.POWER)
    suspend fun volumeUp() = sendCommand(TvCommand.VOLUME_UP)
    suspend fun volumeDown() = sendCommand(TvCommand.VOLUME_DOWN)
    suspend fun volumeMute() = sendCommand(TvCommand.VOLUME_MUTE)
    suspend fun inputText(text: String) = sendCommand(TvCommand.INPUT_TEXT, text)
}

/**
 * State koneksi remote.
 */
data class RemoteConnectionState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val tv: DiscoveredTv? = null,
    val protocolName: String = "",
    val connectedAt: Long = 0L,
    val error: String = ""
)
