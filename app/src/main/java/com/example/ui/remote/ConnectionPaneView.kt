package dev.andikuneiocontroll.ui.remote

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikuneiocontroll.model.DevicePeer
import dev.andikuneiocontroll.remote.ClientConnectionState
import dev.andikuneiocontroll.remote.RemoteClient
import dev.andikuneiocontroll.remote.RemoteSocketServer
import dev.andikuneiocontroll.remote.TvAccessibilityService
import dev.andikuneiocontroll.remote.TvReceiverState
import dev.andikuneiocontroll.server.DiscoveryManager
import dev.andikuneiocontroll.server.WifiHttpServer
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
fun ConnectionPaneView(
    remoteClient: RemoteClient,
    remoteServer: RemoteSocketServer,
    discoveryManager: DiscoveryManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val receiverState by remoteServer.receiverState.collectAsState()
    val clientState by remoteClient.connectionState.collectAsState()
    val discoveredPeers = discoveryManager?.discoveredPeers?.collectAsState()?.value ?: emptyList()
    val isScanning = discoveryManager?.isScanning?.collectAsState()?.value ?: false

    val isConnected = clientState.isConnected || receiverState.connectedClient != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Bar Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isConnected) StabiloLime else StabiloYellow.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (isConnected) StabiloLime.copy(alpha = 0.2f) else StabiloYellow.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isConnected) StabiloLime else StabiloYellow,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "STATUS KONEKSI REMOTE",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isConnected) "Tahap II: Sudah Terhubung" else "Tahap I: Belum Terhubung",
                            color = if (isConnected) StabiloLime else StabiloYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                if (isConnected) {
                    Button(
                        onClick = { remoteClient.disconnect() },
                        colors = ButtonDefaults.buttonColors(containerColor = StabiloPink.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StabiloPink)
                    ) {
                        Text("Putuskan", color = StabiloPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!isConnected) {
            // ==========================================
            // TAHAP I: BELUM TERHUBUNG
            // ==========================================
            Tahap1BelumTerhubung(
                receiverState = receiverState,
                remoteClient = remoteClient,
                remoteServer = remoteServer,
                discoveryManager = discoveryManager,
                discoveredPeers = discoveredPeers,
                isScanning = isScanning
            )
        } else {
            // ==========================================
            // TAHAP II: SUDAH TERHUBUNG
            // ==========================================
            Tahap2SudahTerhubung(
                remoteClient = remoteClient,
                clientState = clientState,
                receiverState = receiverState
            )
        }
    }
}

@Composable
private fun Tahap1BelumTerhubung(
    receiverState: TvReceiverState,
    remoteClient: RemoteClient,
    remoteServer: RemoteSocketServer,
    discoveryManager: DiscoveryManager?,
    discoveredPeers: List<DevicePeer>,
    isScanning: Boolean
) {
    val context = LocalContext.current
    val myLocalIp = remember { WifiHttpServer.getDeviceIpAddress(context) }
    val defaultPrefix = remember(myLocalIp) {
        val parts = myLocalIp.split(".")
        if (parts.size >= 3) "${parts[0]}.${parts[1]}.${parts[2]}." else "192.168.1."
    }

    var inputIp by remember { mutableStateOf(defaultPrefix) }
    var inputCode by remember { mutableStateOf("") }
    var showHelp by remember { mutableStateOf(false) }

    val isAccessibilityActive = remember { TvAccessibilityService.isAccessibilityEnabled(context) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // CARD INFO TV LOKAL (Sangat Jelas & Terang)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = StabiloCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INFO PERANGKAT TV INI", color = StabiloCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    IconButton(onClick = { remoteServer.regeneratePairingCode() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Ganti Kode", tint = StabiloYellow)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detail IP & Port
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBgCardElevated, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ALAMAT IP TV:", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = receiverState.localIp,
                            color = StabiloCyan,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text("Port: ${receiverState.port} (Socket) / 23016 (HTTP)", color = TextMuted, fontSize = 11.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("KODE PAIRING:", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = if (receiverState.isAutoAccept) "BEBAS PIN" else receiverState.pairingCode,
                            color = StabiloLime,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle Auto-Accept Mode (Bebas PIN untuk pairing cepat tanpa gagal)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mode Pasangkan Otomatis (Tanpa PIN)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("Langsung terima koneksi HP di Wi-Fi yang sama tanpa perlu input kode", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = receiverState.isAutoAccept,
                        onCheckedChange = { remoteServer.toggleAutoAccept() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = StabiloLime
                        )
                    )
                }

                // Cek Aksesibilitas TV
                if (!isAccessibilityActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { TvAccessibilityService.openSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = StabiloYellow.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StabiloYellow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Accessibility, contentDescription = null, tint = StabiloYellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aktifkan Izin Aksesibilitas di Pengaturan TV", color = StabiloYellow, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // CARD UNTUK HP: HUBUNGKAN KE TV
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloLime.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HUBUNGKAN REMOTE HP KE TV", color = StabiloLime, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    // Tombol Pindai Otomatis
                    Button(
                        onClick = {
                            discoveryManager?.scanSubnetQuick { count ->
                                Toast.makeText(context, "Pindai selesai: $count perangkat ditemukan", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StabiloCyan.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StabiloCyan),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = StabiloCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Memindai...", color = StabiloCyan, fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = StabiloCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pindai TV Otomatis", color = StabiloCyan, fontSize = 11.sp)
                        }
                    }
                }

                // Daftar TV yang terdeteksi secara otomatis
                if (discoveredPeers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("TV Terdeteksi di Jaringan:", color = TextSecondary, fontSize = 11.sp)

                    discoveredPeers.forEach { peer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkBgCardElevated),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloLime.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tv, contentDescription = null, tint = StabiloLime)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(peer.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${peer.ip}:${peer.port}", color = StabiloCyan, fontSize = 11.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        remoteClient.connect(peer.ip, peer.port, inputCode) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StabiloLime),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Sambungkan", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Manual IP TV
                OutlinedTextField(
                    value = inputIp,
                    onValueChange = { inputIp = it },
                    label = { Text("Alamat IP TV (Lihat di layar TV)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloCyan,
                        focusedLabelColor = StabiloCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputCode,
                    onValueChange = { inputCode = it },
                    label = { Text("4-Digit Kode Pairing TV (Kosongkan jika Bebas PIN)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloLime,
                        focusedLabelColor = StabiloLime
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val target = inputIp.trim()
                        if (target.isNotEmpty()) {
                            remoteClient.connect(target, 23017, inputCode.trim()) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StabiloLime),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sambungkan Sekarang", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tombol Bantuan Pemecahan Masalah
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHelp = !showHelp },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = StabiloYellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (showHelp) "Sembunyikan Tips Pemecahan Masalah" else "Kenapa tidak bisa konek? Klik di sini",
                        color = StabiloYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (showHelp) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkBgCardElevated),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StabiloYellow.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Penyebab Umum & Solusi Pairing Gagal:", color = StabiloYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("1. Nama Wi-Fi Berbeda: Pastikan HP dan TV terhubung ke SSID yang sama (bukan data seluler).", color = TextSecondary, fontSize = 11.sp)
                            Text("2. Isolasi AP Router: Beberapa router memblokir komunikasi antar perangkat. Solusi: Gunakan fitur 'Hotspot Portabel' dari HP dan sambungkan TV ke hotspot HP tersebut.", color = TextSecondary, fontSize = 11.sp)
                            Text("3. Izin Aksesibilitas: Pastikan izin Aksesibilitas diaktifkan pada TV agar TV bisa merespons perintah remote.", color = TextSecondary, fontSize = 11.sp)
                            Text("4. Coba Mode Bebas PIN: Nyalakan saklar 'Mode Pasangkan Otomatis (Tanpa PIN)' di atas layar TV.", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tahap2SudahTerhubung(
    remoteClient: RemoteClient,
    clientState: ClientConnectionState,
    receiverState: TvReceiverState
) {
    val connectedTarget = clientState.targetIp.ifEmpty { receiverState.connectedClient ?: "Android TV" }
    val modeText = if (clientState.connectionMode != "None") clientState.connectionMode else receiverState.connectionType

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Kartu Status Aktif Terhubung
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, StabiloLime)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(StabiloLime.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = StabiloLime)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Android TV Terhubung", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "$connectedTarget ($modeText)", color = StabiloCyan, fontSize = 12.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(StabiloLime.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .border(1.dp, StabiloLime, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (clientState.latencyMs > 0) "${clientState.latencyMs}ms • ONLINE" else "ONLINE",
                            color = StabiloLime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Kontrol D-Pad Remote Zank Aktif
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243044))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "D-PAD KONTROL TV AKTIF",
                    color = StabiloCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tombol Cepat: Power, Mute, Volume
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StabiloMiniBtn(Icons.Default.PowerSettingsNew, "Power", StabiloPink) {
                        remoteClient.sendCommand("POWER")
                    }
                    StabiloMiniBtn(Icons.AutoMirrored.Filled.VolumeUp, "Vol +", StabiloCyan) {
                        remoteClient.sendCommand("VOLUME_UP")
                    }
                    StabiloMiniBtn(Icons.Default.VolumeDown, "Vol -", StabiloCyan) {
                        remoteClient.sendCommand("VOLUME_DOWN")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Lingkaran Besar D-Pad
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(DarkBgCardElevated)
                        .border(2.dp, StabiloCyan.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // UP
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.TopCenter)
                            .clickable { remoteClient.sendCommand("DPAD_UP") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Atas", tint = StabiloLime, modifier = Modifier.size(34.dp))
                    }

                    // DOWN
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.BottomCenter)
                            .clickable { remoteClient.sendCommand("DPAD_DOWN") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bawah", tint = StabiloLime, modifier = Modifier.size(34.dp))
                    }

                    // LEFT
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.CenterStart)
                            .clickable { remoteClient.sendCommand("DPAD_LEFT") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Kiri", tint = StabiloLime, modifier = Modifier.size(34.dp))
                    }

                    // RIGHT
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.CenterEnd)
                            .clickable { remoteClient.sendCommand("DPAD_RIGHT") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Kanan", tint = StabiloLime, modifier = Modifier.size(34.dp))
                    }

                    // OK Center
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(StabiloLime)
                            .clickable { remoteClient.sendCommand("DPAD_OK") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OK", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Baris Navigasi Bawah
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StabiloMiniBtn(Icons.AutoMirrored.Filled.ArrowBack, "Back", StabiloYellow) {
                        remoteClient.sendCommand("BACK")
                    }
                    StabiloMiniBtn(Icons.Default.Home, "Home", StabiloLime) {
                        remoteClient.sendCommand("HOME")
                    }
                    StabiloMiniBtn(Icons.Default.ViewCarousel, "Recent", StabiloCyan) {
                        remoteClient.sendCommand("RECENTS")
                    }
                }
            }
        }
    }
}

@Composable
private fun StabiloMiniBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(DarkBgCardElevated)
                .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
    }
}
