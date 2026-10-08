package dev.andikuneiocontroll.ui.filemanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.andikuneiocontroll.model.DevicePeer
import dev.andikuneiocontroll.model.ServerConfig
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.TextMuted
import dev.andikuneiocontroll.ui.theme.TextPrimary
import dev.andikuneiocontroll.ui.theme.TextSecondary

@Composable
fun WifiShareDialog(
    serverConfig: ServerConfig,
    serverUrl: String,
    discoveredPeers: List<DevicePeer>,
    onToggleServer: (ServerConfig) -> Unit,
    onConnectPeer: (DevicePeer) -> Unit,
    onSavePeer: (DevicePeer, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    // Auto-aktifkan server saat dialog dibuka
    LaunchedEffect(Unit) {
        if (!serverConfig.isRunning) {
            onToggleServer(
                serverConfig.copy(isRunning = true, isReadOnly = false, password = "")
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(4.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = BorderStroke(1.dp, StabiloLime.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (serverConfig.isRunning) StabiloLime.copy(alpha = 0.2f) else DarkBgCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (serverConfig.isRunning) Icons.Default.WifiTethering else Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (serverConfig.isRunning) StabiloLime else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Server WiFi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = StabiloLime
                        )
                        Text(
                            if (serverConfig.isRunning) "Aktif • Mencari perangkat..." else "Menyalakan server...",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // URL Server (jika aktif)
                if (serverConfig.isRunning && serverUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBgCardElevated)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StabiloLime,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                serverUrl,
                                fontSize = 12.sp,
                                color = StabiloCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Title daftar perangkat
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Devices,
                        contentDescription = null,
                        tint = StabiloCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Perangkat iOControll Tv di Sekitar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Daftar Perangkat / Loading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 240.dp)
                ) {
                    if (discoveredPeers.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = StabiloLime,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Mencari perangkat…",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Pastikan perangkat lain membuka aplikasi iOControll Tv\n& terhubung ke WiFi yang sama",
                                color = TextMuted,
                                fontSize = 10.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(discoveredPeers) { peer ->
                                PeerItem(
                                    peer = peer,
                                    onClick = { onConnectPeer(peer) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeerItem(
    peer: DevicePeer,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkBgCardElevated),
        border = BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(StabiloPink.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Devices,
                    contentDescription = null,
                    tint = StabiloPink,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    peer.name.ifBlank { "Perangkat iOControll" },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    "${peer.ip} • Port ${peer.port}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Hubungkan",
                tint = StabiloLime,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
