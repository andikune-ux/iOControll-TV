package dev.andikuneiocontroll.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.DarkDivider
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow
import dev.andikuneiocontroll.ui.theme.TextMuted

@Composable
fun MobileModeView(
    isLeftPaneVisible: Boolean,
    onTogglePane: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRemote: () -> Unit,
    onOpenConnectionDialog: () -> Unit,
    isConnected: Boolean,
    paneContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    Row(modifier = Modifier.fillMaxSize()) {
        // Jika PANE KANAN aktif -> Toolbar di KIRI
        if (!isLeftPaneVisible) {
            MobileToolbar(
                isLeftPaneVisible = isLeftPaneVisible,
                onOpenSettings = onOpenSettings,
                onTogglePane = onTogglePane,
                onFullscreen = {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                },
                onOpenRemote = onOpenRemote,
                onOpenConnectionDialog = onOpenConnectionDialog,
                isConnected = isConnected
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            paneContent()
        }

        // Jika PANE KIRI aktif -> Toolbar di KANAN
        if (isLeftPaneVisible) {
            MobileToolbar(
                isLeftPaneVisible = isLeftPaneVisible,
                onOpenSettings = onOpenSettings,
                onTogglePane = onTogglePane,
                onFullscreen = {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                },
                onOpenRemote = onOpenRemote,
                onOpenConnectionDialog = onOpenConnectionDialog,
                isConnected = isConnected
            )
        }
    }
}

@Composable
private fun MobileToolbar(
    isLeftPaneVisible: Boolean,
    onOpenSettings: () -> Unit,
    onTogglePane: () -> Unit,
    onFullscreen: () -> Unit,
    onOpenRemote: () -> Unit,
    onOpenConnectionDialog: () -> Unit,
    isConnected: Boolean
) {
    Column(
        modifier = Modifier
            .width(50.dp)
            .fillMaxHeight()
            .background(DarkBgCard)
            .border(width = 1.dp, color = DarkDivider),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Gerigi -> Pengaturan
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Pengaturan",
                tint = StabiloCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Panah Switch Pane
        IconButton(onClick = onTogglePane) {
            Icon(
                imageVector = if (isLeftPaneVisible) {
                    Icons.AutoMirrored.Filled.ArrowForward
                } else {
                    Icons.AutoMirrored.Filled.ArrowBack
                },
                contentDescription = if (isLeftPaneVisible) "Ke Pane Kanan" else "Ke Pane Kiri",
                tint = StabiloLime,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Fullscreen -> Paksa Landscape
        IconButton(onClick = onFullscreen) {
            Icon(
                Icons.Default.Fullscreen,
                contentDescription = "Paksa Rotasi ke TV",
                tint = StabiloYellow,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Dot Status Koneksi
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DarkBgCardElevated)
                .border(
                    1.dp,
                    if (isConnected) Color(0xFF22C55E).copy(alpha = 0.6f) else TextMuted.copy(alpha = 0.5f),
                    CircleShape
                )
                .clickable(onClick = onOpenConnectionDialog),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) Color(0xFF22C55E) else TextMuted)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Remote TV -> Buka jendela remote
        IconButton(onClick = onOpenRemote) {
            Icon(
                Icons.Default.SettingsRemote,
                contentDescription = "Remote TV",
                tint = StabiloPink,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
