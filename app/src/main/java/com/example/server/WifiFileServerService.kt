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
const val ACTION_START = "dev.andikuneiocontroll.ACTION_START_SERVER"
const val ACTION_STOP = "dev.andikuneiocontroll.ACTION_STOP_SERVER"
