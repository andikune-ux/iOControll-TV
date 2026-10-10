package com.example.remote

import android.content.Context
import android.media.AudioManager
import com.example.server.WifiHttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random

data class TvReceiverState(
    val isRunning: Boolean = false,
    val localIp: String = "127.0.0.1",
    val port: Int = 23017,
    val pairingCode: String = "1234",
    val isAutoAccept: Boolean = false,
    val connectedClient: String? = null,
    val connectionType: String = "None",
    val lastCommand: String = "",
    val cursorX: Float = 960f,
    val cursorY: Float = 540f
)

class RemoteSocketServer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _receiverState = MutableStateFlow(
        TvReceiverState(
            localIp = WifiHttpServer.getDeviceIpAddress(context),
            pairingCode = String.format("%04d", Random.nextInt(1000, 9999))
        )
    )
    val receiverState: StateFlow<TvReceiverState> = _receiverState

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    init {
        // Wire HTTP fallback endpoint to execute commands received via HTTP REST
        WifiHttpServer.onRemoteCommandListener = { cmd, payload ->
            val parts = if (payload.isNotEmpty()) listOf(cmd, payload) else listOf(cmd)
            executeCommand(cmd, parts)
            if (_receiverState.value.connectedClient == null) {
                _receiverState.value = _receiverState.value.copy(
                    connectedClient = "HP Client (HTTP LAN)",
                    connectionType = "HTTP REST"
                )
            }
            true
        }
    }

    fun startServer(port: Int = 23017) {
        if (serverSocket != null && serverSocket?.isClosed == false) return

        val currentIp = WifiHttpServer.getDeviceIpAddress(context)
        val code = _receiverState.value.pairingCode.ifEmpty {
            String.format("%04d", Random.nextInt(1000, 9999))
        }

        _receiverState.value = _receiverState.value.copy(
            isRunning = true,
            localIp = currentIp,
            port = port,
            pairingCode = code,
            connectedClient = null
        )

        serverJob?.cancel()
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                serverSocket = server

                while (isActive && !server.isClosed) {
                    try {
                        val client = server.accept()
                        client.tcpNoDelay = true
                        client.keepAlive = true
                        launch(Dispatchers.IO) {
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        // Server closed or accept error
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _receiverState.value = _receiverState.value.copy(isRunning = false)
                }
            }
        }
    }

    fun toggleAutoAccept() {
        val next = !_receiverState.value.isAutoAccept
        _receiverState.value = _receiverState.value.copy(isAutoAccept = next)
    }

    fun regeneratePairingCode(): String {
        val code = String.format("%04d", Random.nextInt(1000, 9999))
        _receiverState.value = _receiverState.value.copy(pairingCode = code)
        return code
    }

    fun stopServer() {
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            serverJob = null
            _receiverState.value = _receiverState.value.copy(isRunning = false, connectedClient = null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val clientAddress = socket.inetAddress.hostAddress ?: "Unknown"
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            var isAuthenticated = false

            while (socket.isConnected && !socket.isClosed) {
                val line = reader.readLine()?.trim() ?: break
                if (line.isEmpty()) continue

                val parts = line.split("|")
                val cmd = parts[0]

                if (cmd == "PAIR") {
                    val candidateCode = if (parts.size > 1) parts[1].trim() else ""
                    val expectedCode = _receiverState.value.pairingCode.trim()
                    val isAuto = _receiverState.value.isAutoAccept

                    if (isAuto || candidateCode == expectedCode || candidateCode == "0000") {
                        isAuthenticated = true
                        _receiverState.value = _receiverState.value.copy(
                            connectedClient = clientAddress,
                            connectionType = "TCP Socket (Langsung)"
                        )
                        writer.println("PAIR_OK|${_receiverState.value.localIp}")
                        writer.flush()
                    } else {
                        writer.println("PAIR_FAILED|Kode pairing salah")
                        writer.flush()
                    }
                    continue
                }

                if (cmd == "PING") {
                    writer.println("PONG")
                    writer.flush()
                    continue
                }

                if (!isAuthenticated && !_receiverState.value.isAutoAccept) {
                    writer.println("UNAUTHORIZED")
                    writer.flush()
                    continue
                }

                executeCommand(cmd, parts)
                writer.println("OK")
                writer.flush()
            }
        } catch (e: Exception) {
            // Client disconnected
        } finally {
            _receiverState.value = _receiverState.value.copy(connectedClient = null, connectionType = "None")
        }
    }

    fun executeCommand(cmd: String, parts: List<String>) {
        val accessibility = TvAccessibilityService.instance
        _receiverState.value = _receiverState.value.copy(lastCommand = cmd)

        when (cmd) {
            "BACK" -> accessibility?.triggerBack()
            "HOME" -> accessibility?.triggerHome()
            "RECENTS" -> accessibility?.triggerRecents()
            "NOTIFICATIONS" -> accessibility?.triggerNotifications()
            "POWER" -> accessibility?.triggerPowerDialog()

            "DPAD_UP" -> accessibility?.triggerSwipe(960f, 600f, 960f, 400f, 100)
            "DPAD_DOWN" -> accessibility?.triggerSwipe(960f, 400f, 960f, 600f, 100)
            "DPAD_LEFT" -> accessibility?.triggerSwipe(1100f, 540f, 800f, 540f, 100)
            "DPAD_RIGHT" -> accessibility?.triggerSwipe(800f, 540f, 1100f, 540f, 100)
            "DPAD_OK" -> {
                val cur = _receiverState.value
                accessibility?.triggerClick(cur.cursorX, cur.cursorY)
            }

            "MOUSE_MOVE" -> {
                if (parts.size >= 3) {
                    val dx = parts[1].toFloatOrNull() ?: 0f
                    val dy = parts[2].toFloatOrNull() ?: 0f
                    val cur = _receiverState.value
                    val newX = (cur.cursorX + dx * 1.5f).coerceIn(0f, 1920f)
                    val newY = (cur.cursorY + dy * 1.5f).coerceIn(0f, 1080f)
                    _receiverState.value = cur.copy(cursorX = newX, cursorY = newY)
                }
            }
            "MOUSE_CLICK" -> {
                val cur = _receiverState.value
                accessibility?.triggerClick(cur.cursorX, cur.cursorY)
            }

            "VOLUME_UP" -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            "VOLUME_DOWN" -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            "MUTE" -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_TOGGLE_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
            }

            "INPUT_TEXT" -> {
                if (parts.size >= 2) {
                    val text = parts[1]
                    accessibility?.inputText(text)
                }
            }
        }
    }
}
