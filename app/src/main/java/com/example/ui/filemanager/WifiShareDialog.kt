package dev.andikuneiocontroll.ui.filemanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikuneiocontroll.model.DevicePeer
import dev.andikuneiocontroll.model.ServerConfig
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.DarkBgPrimary
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow
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
    var port by remember { mutableStateOf(serverConfig.port.toString()) }
    var isReadOnly by remember { mutableStateOf(serverConfig.isReadOnly) }
    var password by remember { mutableStateOf(serverConfig.password) }
    var autoStart by remember { mutableStateOf(serverConfig.autoStart) }

    var selectedPeerForSave by remember { mutableStateOf<DevicePeer?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = DarkBgPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(StabiloCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = StabiloCyan, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Wi-Fi File Sharing (X-plore)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Transfer file nirkabel lokal berkecepatan tinggi",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Big Server Status & Toggle Switch Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (serverConfig.isRunning) StabiloLime.copy(alpha = 0.12f) else DarkBgCard
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (serverConfig.isRunning) StabiloLime else Color(0xFF243044)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(if (serverConfig.isRunning) StabiloLime else DarkBgCardElevated)
                                .border(2.dp, if (serverConfig.isRunning) Color.White else StabiloCyan.copy(alpha = 0.4f), CircleShape)
                                .clickable {
                                    val newConfig = serverConfig.copy(
                                        port = port.toIntOrNull() ?: 23016,
                                        isReadOnly = isReadOnly,
                                        password = password,
                                        autoStart = autoStart,
                                        isRunning = !serverConfig.isRunning
                                    )
                                    onToggleServer(newConfig)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (serverConfig.isRunning) Color.Black else TextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (serverConfig.isRunning) "SERVER SEDANG AKTIF" else "SERVER NONAKTIF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (serverConfig.isRunning) StabiloLime else TextSecondary
                        )

                        if (serverConfig.isRunning && serverUrl.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Akses browser atau perangkat lain di:",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(DarkBgCardElevated, RoundedCornerShape(8.dp))
                                    .border(1.dp, StabiloCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = serverUrl,
                                    color = StabiloCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Configuration Section
                Text("Konfigurasi Server", fontWeight = FontWeight.Bold, color = StabiloYellow, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port Server (Default: 23016)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloCyan,
                        focusedLabelColor = StabiloCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Kata Sandi Akses (Opsional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloLime,
                        focusedLabelColor = StabiloLime
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Akses Hanya-Baca (Read-only)", color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text("Perangkat lain tidak dapat menghapus atau mengunggah berkas", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isReadOnly,
                        onCheckedChange = { isReadOnly = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = StabiloLime
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-start saat Wi-Fi terhubung", color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text("Otomatis menyalakan server saat tersambung ke jaringan lokal", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = autoStart,
                        onCheckedChange = { autoStart = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = StabiloCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Discovered Devices Section (NSD / mDNS)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Devices, contentDescription = null, tint = StabiloYellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Perangkat di Jaringan (${discoveredPeers.size})",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ketuk untuk jelajahi file. Tekan simpan untuk menyimpan label & sandi.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (discoveredPeers.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkBgCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243044)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Mencari perangkat iOControll Tv lain di jaringan Wi-Fi ini...",
                            color = TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    discoveredPeers.forEach { peer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onConnectPeer(peer) },
                            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloLime.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = StabiloLime)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (peer.customLabel.isNotEmpty()) peer.customLabel else peer.name,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "http://${peer.ip}:${peer.port}",
                                            color = StabiloCyan,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                IconButton(onClick = { selectedPeerForSave = peer }) {
                                    Icon(
                                        if (peer.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Simpan Perangkat",
                                        tint = StabiloYellow
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Device Dialog
    selectedPeerForSave?.let { peer ->
        AddDeviceDialog(
            peer = peer,
            onSave = { label, pwd, path ->
                onSavePeer(peer, label, pwd, path)
                selectedPeerForSave = null
            },
            onDismiss = { selectedPeerForSave = null }
        )
    }
}

@Composable
fun AddDeviceDialog(
    peer: DevicePeer,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(peer.customLabel.ifEmpty { peer.name }) }
    var password by remember { mutableStateOf(peer.password) }
    var defaultPath by remember { mutableStateOf(peer.defaultPath) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloLime),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Simpan Perangkat (Add Device)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label Perangkat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Kata Sandi Perangkat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = defaultPath,
                    onValueChange = { defaultPath = it },
                    label = { Text("Path Default") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                        Text("Batal", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(label, password, defaultPath) },
                        colors = ButtonDefaults.buttonColors(containerColor = StabiloLime)
                    ) {
                        Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
