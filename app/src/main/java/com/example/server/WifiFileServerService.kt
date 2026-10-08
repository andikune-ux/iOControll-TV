package dev.andikuneiocontroll.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.andikuneiocontroll.MainActivity
import dev.andikuneiocontroll.R
import dev.andikuneiocontroll.model.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WifiFileServerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var httpServer: WifiHttpServer
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): WifiFileServerService = this@WifiFileServerService
    }

    override fun onCreate() {
        super.onCreate()
        httpServer = WifiHttpServer(this, serviceScope)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val port = intent.getIntExtra(EXTRA_PORT, 23016)
                val readOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false)
                val password = intent.getStringExtra(EXTRA_PASSWORD) ?: ""
                val selectedFiles = intent.getStringArrayListExtra(EXTRA_SELECTED_FILES) ?: arrayListOf()

                val config = ServerConfig(
                    port = port,
                    isRunning = true,
                    isReadOnly = readOnly,
                    password = password,
                    selectedFiles = selectedFiles
                )

                startForegroundServer(config)
            }
            ACTION_STOP -> {
                stopForegroundServer()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundServer(config: ServerConfig) {
        httpServer.startServer(config) { running, urlOrMsg ->
            if (running) {
                _serverState.value = ServerState(isRunning = true, url = urlOrMsg, port = config.port)
                val notification = buildNotification(urlOrMsg)
                startForeground(NOTIFICATION_ID, notification)
            } else {
                _serverState.value = ServerState(isRunning = false, errorMessage = urlOrMsg)
                stopSelf()
            }
        }
    }

    private fun stopForegroundServer() {
        httpServer.stopServer()
        _serverState.value = ServerState(isRunning = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "iOControll Wi-Fi Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi status Wi-Fi file server berjalan di latar belakang"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(serverUrl: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WifiFileServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("iOControll Server Berjalan")
            .setContentText("Akses berkas: $serverUrl")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hentikan", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        httpServer.stopServer()
        serviceScope.cancel()
        _serverState.value = ServerState(isRunning = false)
    }

    data class ServerState(
        val isRunning: Boolean = false,
        val url: String = "",
        val port: Int = 23016,
        val errorMessage: String = ""
    )

    companion object {
        const val CHANNEL_ID = "iocontroll_server_channel"
        const val NOTIFICATION_ID = 23016

        const val ACTION_START = "dev.andikuneiocontroll.ACTION_START_SERVER"
        const val ACTION_STOP = "dev.andikuneiocontroll.ACTION_STOP_SERVER"

        const val EXTRA_PORT = "EXTRA_PORT"
        const val EXTRA_READ_ONLY = "EXTRA_READ_ONLY"
        const val EXTRA_PASSWORD = "EXTRA_PASSWORD"
        const val EXTRA_SELECTED_FILES = "EXTRA_SELECTED_FILES"

        private val _serverState = MutableStateFlow(ServerState())
        val serverState: StateFlow<ServerState> = _serverState
    }
}
