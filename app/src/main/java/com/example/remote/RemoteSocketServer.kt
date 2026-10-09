package dev.andikuneiocontroll.remote

import android.content.Context
import android.media.AudioManager
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
import java.net.ServerSocket
import java.net.Socket

data class TvReceiverState(
    val isRunning: Boolean = false,
    val port: Int = 23017,
    val connectedClient: String? = null,
    val lastCommand: String = "",
    val cursorX: Float = 960f,
    val cursorY: Float = 540f
)

/**
 * RemoteSocketServer — Server di TV.
 *
 * V1.00.001 (Updated):
 * - Hapus pairing code (auto-accept, sesuai Zank Remote)
 * - Auto-start UDP listener via RemoteDiscovery
 * - Setiap client yang connect langsung dianggap terhubung
 */
class RemoteSocketServer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    // Discovery (UDP listener)
    val discovery = RemoteDiscovery(context, scope)

    private val _receiverState = MutableStateFlow(TvReceiverState())
    val receiverState: StateFlow<TvReceiverState> = _receiverState

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun startServer(port: Int = 23017) {
        if (serverSocket != null) return

        _receiverState.value = _receiverState.value.copy(
            isRunning = true,
            port = port,
            connectedClient = null
        )

        // Mulai UDP listener untuk auto-discovery
        discovery.startListener(remotePort = port)

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket(port)
                serverSocket = server

                while (isActive && !server.isClosed) {
                    try {
                        val client = server.accept()
                        launch(Dispatchers.IO) {
                            handleClient(client)
                        }
                    } catch (_: Exception) {
                        // Server closed
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

    fun stopServer() {
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            serverJob = null
            discovery.stopListener()
            _receiverState.value = _receiverState.value.copy(
                isRunning = false,
                connectedClient = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val clientAddress = socket.inetAddress.hostAddress ?: "Unknown"
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            // Auto-accept: langsung dianggap terhubung (tanpa pairing)
            _receiverState.value = _receiverState.value.copy(connectedClient = clientAddress)
            writer.println("CONNECT_OK")

            while (socket.isConnected && !socket.isClosed) {
                val line = reader.readLine() ?: break
                val parts = line.split("|")
                if (parts.isEmpty()) continue
                val cmd = parts[0]

                executeCommand(cmd, parts)
                writer.println("OK")
            }
        } catch (e: Exception) {
            // Client disconnected
        } finally {
            _receiverState.value = _receiverState.value.copy(connectedClient = null)
        }
    }

    private fun executeCommand(cmd: String, parts: List<String>) {
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

            "VOLUME_UP" -> audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
            "VOLUME_DOWN" -> audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
            "MUTE" -> audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_TOGGLE_MUTE,
                AudioManager.FLAG_SHOW_UI
            )

            "INPUT_TEXT" -> {
                if (parts.size >= 2) {
                    val text = parts[1]
                    accessibility?.inputText(text)
                }
            }
        }
    }
}
