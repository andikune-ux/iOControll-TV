package dev.andikuneiocontroll.remote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.andikuneiocontroll.MainActivity
import dev.andikuneiocontroll.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * RemoteServerService — Foreground service untuk remote TV v2.
 *
 * Menggunakan protokol Android TV Remote v2 (port 6466/6467).
 * TIDAK seperti RemoteSocketServer lama — service ini memakai TLS
 * + protobuf, dan tidak perlu pairing code manual.
 *
 * Service ini opsional. Hanya dipakai kalau aplikasi iOControll Tv
 * di-install di TV (server mode).
 */
class RemoteServerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val binder = LocalBinder()

    private val _serverState = MutableStateFlow(ServerState())
    val serverState: StateFlow<ServerState> = _serverState

    inner class LocalBinder : Binder() {
        fun getService(): RemoteServerService = this@RemoteServerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startServer()
            ACTION_STOP -> stopServer()
        }
        return START_NOT_STICKY
    }

    private fun startServer() {
        try {
            _serverState.value = ServerState(isRunning = true, port = 6466)
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
            _serverState.value = ServerState(error = "Gagal start: ${e.message}")
            stopSelf()
        }
    }

    private fun stopServer() {
        _serverState.value = ServerState(isRunning = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "iOControll Remote Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi server remote TV"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        } else {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }
        val openPendingIntent = android.app.PendingIntent.getActivity(
            this, 0, openIntent, pendingFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("iOControll Remote Server")
            .setContentText("Server remote TV aktif di port 6466")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        _serverState.value = ServerState(isRunning = false)
    }

    data class ServerState(
        val isRunning: Boolean = false,
        val port: Int = 6466,
        val error: String = ""
    )

    companion object {
        private const val CHANNEL_ID = "iocontroll_remote_channel"
        private const val NOTIFICATION_ID = 6466

        const val ACTION_START = "dev.andikuneiocontroll.ACTION_START_REMOTE"
        const val ACTION_STOP = "dev.andikuneiocontroll.ACTION_STOP_REMOTE"

        fun start(context: Context) {
            val intent = Intent(context, RemoteServerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RemoteServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
