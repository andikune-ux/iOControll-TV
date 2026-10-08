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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikuneiocontroll.remote.RemoteClient
import dev.andikuneiocontroll.remote.RemoteSocketServer
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val receiverState by remoteServer.receiverState.collectAsState()
    val clientState by remoteClient.connectionState.collectAsState()

    // Status terhubung riil atau manual simulasi agar user bisa langsung berpindah tahap I & II
    var isSimulatedConnected by remember { mutableStateOf(false) }
    val isConnected = clientState.isConnected || receiverState.connectedClient != null || isSimulatedConnected

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Card Pane Kanan
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isConnected) StabiloLime.copy(alpha = 0.5f) else StabiloYellow.copy(alpha = 0.4f)
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
                            .size(36.dp)
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
                            text = "PANE KANAN • STATUS KONEKSI",
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

                // Quick Toggle Button Tahap I <-> Tahap II
                Button(
                    onClick = {
                        if (isConnected) {
                            remoteClient.disconnect()
                            isSimulatedConnected = false
                        } else {
                            isSimulatedConnected = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) StabiloPink.copy(alpha = 0.2f) else StabiloLime.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isConnected) StabiloPink else StabiloLime
                    )
                ) {
                    Text(
                        text = if (isConnected) "Putuskan" else "Hubungkan",
                        color = if (isConnected) StabiloPink else StabiloLime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!isConnected) {
            // ==========================================
            // TAHAP I: BELUM TERHUBUNG
            // ==========================================
            Tahap1BelumTerhubung(
                pairingCode = receiverState.pairingCode,
                port = receiverState.port,
                onConnectManual = { ip, code ->
                    remoteClient.connect(ip, 23017, code) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onSimulateConnected = {
                    isSimulatedConnected = true
                }
            )
        } else {
            // ==========================================
            // TAHAP II: SUDAH TERHUBUNG
            // ==========================================
            Tahap2SudahTerhubung(
                remoteClient = remoteClient,
                connectedIp = clientState.targetIp.ifEmpty { receiverState.connectedClient ?: "192.168.1.105 (Smart TV 4K)" },
                onDisconnect = {
                    remoteClient.disconnect()
                    isSimulatedConnected = false
                }
            )
        }
    }
}

@Composable
private fun Tahap1BelumTerhubung(
    pairingCode: String,
    port: Int,
    onConnectManual: (String, String) -> Unit,
    onSimulateConnected: () -> Unit
) {
    var inputIp by remember { mutableStateOf("192.168.1.") }
    var inputCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Card Belum Terhubung
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243044))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(StabiloYellow.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Sensors,
                        contentDescription = null,
                        tint = StabiloYellow,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Menunggu Pasangan Perangkat TV",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Buka iOControll Tv di perangkat Android TV untuk mendapatkan kode pairing, atau masukkan IP TV di bawah:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Kode Pairing TV Lokal
                Row(
                    modifier = Modifier
                        .background(DarkBgCardElevated, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kode Pairing Perangkat Ini:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = pairingCode,
                        color = StabiloLime,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = 3.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Form Sambung Manual ke TV
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243044))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Hubungkan ke TV Manual",
                    color = StabiloCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inputIp,
                    onValueChange = { inputIp = it },
                    label = { Text("Alamat IP TV (misal: 192.168.1.15)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloCyan,
                        focusedLabelColor = StabiloCyan,
                        cursorColor = StabiloCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputCode,
                    onValueChange = { inputCode = it },
                    label = { Text("4-Digit Kode Pairing TV") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloLime,
                        focusedLabelColor = StabiloLime,
                        cursorColor = StabiloLime
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (inputIp.isNotEmpty() && inputCode.isNotEmpty()) {
                                onConnectManual(inputIp.trim(), inputCode.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StabiloLime),
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hubungkan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSimulateConnected,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StabiloCyan)
                    ) {
                        Text("Simulasi Tahap II", color = StabiloCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tahap2SudahTerhubung(
    remoteClient: RemoteClient,
    connectedIp: String,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Info Sambungan TV
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloLime.copy(alpha = 0.4f))
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
                                .size(40.dp)
                                .background(StabiloLime.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = StabiloLime)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Android TV Terhubung", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(text = connectedIp, color = StabiloCyan, fontSize = 12.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(StabiloLime.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .border(1.dp, StabiloLime, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("LATENSI: 4ms • ONLINE", color = StabiloLime, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Kontrol D-Pad & Remote Langsung di Pane Kanan
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

                // Quick buttons: Power, Mute, Volume
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

                Spacer(modifier = Modifier.height(16.dp))

                // Big D-Pad Circle
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(DarkBgCardElevated)
                        .border(2.dp, StabiloCyan.copy(alpha = 0.3f), CircleShape),
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
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Atas", tint = StabiloLime, modifier = Modifier.size(32.dp))
                    }

                    // DOWN
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.BottomCenter)
                            .clickable { remoteClient.sendCommand("DPAD_DOWN") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bawah", tint = StabiloLime, modifier = Modifier.size(32.dp))
                    }

                    // LEFT
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.CenterStart)
                            .clickable { remoteClient.sendCommand("DPAD_LEFT") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Kiri", tint = StabiloLime, modifier = Modifier.size(32.dp))
                    }

                    // RIGHT
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.CenterEnd)
                            .clickable { remoteClient.sendCommand("DPAD_RIGHT") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Kanan", tint = StabiloLime, modifier = Modifier.size(32.dp))
                    }

                    // OK Center
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(StabiloLime)
                            .clickable { remoteClient.sendCommand("DPAD_OK") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OK", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Navigation Row
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
                .size(46.dp)
                .clip(CircleShape)
                .background(DarkBgCardElevated)
                .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
    }
}
