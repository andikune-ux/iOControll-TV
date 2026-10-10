package dev.andikuneiocontroll.remote.controller

import android.content.Context
import dev.andikuneiocontroll.remote.discovery.DiscoveredTv
import dev.andikuneiocontroll.remote.protocol.ProtocolDetector
import dev.andikuneiocontroll.remote.protocol.TvCommand
import dev.andikuneiocontroll.remote.protocol.TvProtocol
import dev.andikuneiocontroll.remote.protocol.adb.AdbTvClient
import dev.andikuneiocontroll.remote.protocol.androidtv.AndroidTvRemoteClient
import dev.andikuneiocontroll.remote.protocol.androidtv.TlsHelper
import dev.andikuneiocontroll.remote.protocol.lg.LgWebOsClient
import dev.andikuneiocontroll.remote.protocol.philips.PhilipsClient
import dev.andikuneiocontroll.remote.protocol.roku.RokuEcpClient
import dev.andikuneiocontroll.remote.protocol.samsung.SamsungTizenClient
import dev.andikuneiocontroll.remote.protocol.vizio.VizioClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RemoteController(context: Context) {

    private val appContext = context.applicationContext

    // ==========================================================
    // PROTOKOL CLIENTS
    // ==========================================================
    private val androidTvV2Client: AndroidTvRemoteClient = AndroidTvRemoteClient(appContext)
    private val adbClient: AdbTvClient = AdbTvClient(appContext)
    private val rokuClient: TvProtocol = RokuEcpClient(appContext)
    private val samsungClient: TvProtocol = SamsungTizenClient(appContext)
    private val lgClient: TvProtocol = LgWebOsClient(appContext)
    private val philipsClient: TvProtocol = PhilipsClient(appContext)
    private val vizioClient: TvProtocol = VizioClient(appContext)

    // Urutan prioritas: v2 → ADB → brand-specific
    private val allProtocols: List<TvProtocol> = listOf(
        androidTvV2Client, // UTAMA untuk Google/Android TV
        adbClient,          // fallback
        rokuClient,
        samsungClient,
        lgClient,
        philipsClient,
        vizioClient
    )

    private var activeProtocol: TvProtocol? = null

    private val _connectionState = MutableStateFlow(RemoteConnectionState())
    val connectionState: StateFlow<RemoteConnectionState> = _connectionState.asStateFlow()

    // ==========================================================
    // CONNECT
    // ==========================================================

    suspend fun connect(
        tv: DiscoveredTv,
        pairingCode: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        if (activeProtocol != null) {
            disconnect()
        }

        val protocol = pickProtocol(tv)
        if (protocol == null) {
            onResult(false, "Protokol tidak didukung untuk ${tv.brand}")
            _connectionState.value = RemoteConnectionState(error = "Protokol tidak didukung")
            return
        }

        _connectionState.value = RemoteConnectionState(
            isConnecting = true,
            tv = tv,
            protocolName = protocol.protocolName
        )

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
        // Untuk Google TV / Android TV → v2 dulu
        if (tv.brand.uppercase() in listOf("ANDROID_TV", "GOOGLE_TV", "FIRE_TV")) {
            return androidTvV2Client
        }

        // Brand lain → cari yang cocok
        val candidates = allProtocols.filter { it.canHandle(tv) }
        if (candidates.isNotEmpty()) return candidates.first()

        val protocolId = ProtocolDetector.detect(tv)
        return when (protocolId) {
            "ANDROID_TV_V2" -> androidTvV2Client
            "ROKU_ECP" -> rokuClient
            "SAMSUNG_TIZEN" -> samsungClient
            "LG_WEBOS" -> lgClient
            "PHILIPS_JOINTSPACE" -> philipsClient
            "VIZIO_SMARTCAST" -> vizioClient
            else -> null
        }
    }

    // ==========================================================
    // PAIRING ADB (fallback lama)
    // ==========================================================

    suspend fun pair(
        host: String,
        pairingPort: Int,
        pairingCode: String
    ): Pair<Boolean, String> {
        return try {
            adbClient.pair(host, pairingPort, pairingCode)
                ?: (false to "Pairing tidak didukung protokol ini")
        } catch (e: Exception) {
            e.printStackTrace()
            false to "Error pairing: ${e.message ?: "Unknown"}"
        }
    }

    fun getPairingInfo(): Triple<Boolean, String, Int>? {
        return Triple(adbClient.needsPairing, adbClient.pairingHost, adbClient.pairingPort)
    }

    fun clearPairing(host: String? = null) {
        try {
            adbClient.clearPairing(host)
            TlsHelper.clearPairingData(appContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================================
    // DISCONNECT
    // ==========================================================

    suspend fun disconnect() {
        try {
            activeProtocol?.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        activeProtocol = null
        _connectionState.value = RemoteConnectionState()
    }

    // ==========================================================
    // SEND COMMAND
    // ==========================================================

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

    fun isConnected(): Boolean = activeProtocol?.isConnected() ?: false

    fun getActiveProtocolName(): String = activeProtocol?.protocolName ?: ""

    fun getConnectedTv(): DiscoveredTv? {
        val state = _connectionState.value
        return if (state.isConnected) state.tv else null
    }

    fun clearError() {
        val current = _connectionState.value
        _connectionState.value = current.copy(error = "")
    }

    // ==========================================================
    // QUICK COMMANDS
    // ==========================================================

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

data class RemoteConnectionState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val tv: DiscoveredTv? = null,
    val protocolName: String = "",
    val connectedAt: Long = 0L,
    val error: String = ""
)
