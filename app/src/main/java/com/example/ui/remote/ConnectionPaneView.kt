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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikuneiocontroll.remote.DiscoveredTv
import dev.andikuneiocontroll.remote.RemoteClient
import dev.andikuneiocontroll.remote.RemoteSocketServer
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.DarkBgPrimary
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow
import dev.andikuneiocontroll.ui.theme.TextPrimary
import dev.andikuneiocontroll.ui.theme.TextSecondary

@Composable
fun ConnectionPaneView(
    remoteClient: RemoteClient,
    remoteServer: RemoteSocketServer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clientState by remoteClient.connectionState.collectAsState()
    val discoveredTvs by remoteClient.discovery.discoveredTvs.collectAsState()
    val isScanning by remoteClient.discovery.isScanning.collectAsState()

    // Auto-scan saat pertama kali dibuka
    LaunchedEffect(Unit) {
        remoteClient.startAutoScan()
    }

    val isConnected = clientState.isConnected

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header status
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
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (isConnected) StabiloLime else StabiloYellow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "STATUS KONEKSI",
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isConnected) {
                            "Terhubung: ${clientState.targetName.ifBlank { clientState.targetIp }}"
                        } else {
                            "Belum Terhubung"
                        },
                        color = if (isConnected) StabiloLime else StabiloYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                if (isConnected) {
                    Button(
                        onClick = { remoteClient.disconnect() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StabiloPink.copy(alpha = 0.2f)
                        ),
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
            Tahap1AutoScan(
                tvs = discoveredTvs,
                isScanning = isScanning,
                onRefresh = { remoteClient.startAutoScan() },
                onConnect = { tv ->
                    remoteClient.connectToTv(tv) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } else {
            Tahap2SudahTerhubung(
                remoteClient = remoteClient,
                connectedName = clientState.targetName.ifBlank { clientState.targetIp },
                onDisconnect = { remoteClient.disconnect() }
            )
        }
    }
}

@Composable
private fun Tahap1AutoScan(
    tvs: List<DiscoveredTv>,
    isScanning: Boolean,
    onRefresh: () -> Unit,
    onConnect: (DiscoveredTv) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Card status scan
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
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = StabiloYellow,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(38.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Sensors,
                            contentDescription = null,
                            tint = StabiloYellow,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isScanning) "Mencari Perangkat TV…" else "Pencarian Selesai",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Pastikan TV Android Anda membuka aplikasi iOControll Tv\ndan terhubung ke WiFi yang sama",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = StabiloLime),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Scan Ulang",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Daftar TV ditemukan
        if (tvs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkBgCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StabiloLime.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "TV DITEMUKAN (${tvs.size})",
                        color = StabiloLime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((tvs.size * 80).dp.coerceAtMost(400.dp)),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tvs, key = { it.deviceId }) { tv ->
                            TvItem(tv = tv, onClick = { onConnect(tv) })
                        }
                    }
                }
            }
        } else if (!isScanning) {
            // Tidak ada TV ditemukan
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkBgCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF243044))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tidak ada TV ditemukan",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Cek koneksi WiFi & pastikan aplikasi iOControll Tv terbuka di TV",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TvItem(
    tv: DiscoveredTv,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkBgCardElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.3f)),
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
                    .background(StabiloPink.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Tv,
                    contentDescription = null,
                    tint = StabiloPink,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tv.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "${tv.ip} • Port ${tv.remotePort}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Icon(
                Icons.Default.Wifi,
                contentDescription = "Hubungkan",
                tint = StabiloLime,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun Tahap2SudahTerhubung(
    remoteClient: RemoteClient,
    connectedName: String,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Info TV
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloLime.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(StabiloLime.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = StabiloLime)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TV Terhubung",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = connectedName,
                        color = StabiloCyan,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(StabiloLime.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .border(1.dp, StabiloLime, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "ONLINE",
                        color = StabiloLime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // D-Pad Kontrol
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
                    text = "D-PAD KONTROL TV",
                    color = StabiloCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Top buttons: Power, Vol+
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniBtn(Icons.Default.PowerSettingsNew, "Power", StabiloPink) {
                        remoteClient.sendCommand("POWER")
                    }
                    MiniBtn(Icons.AutoMirrored.Filled.VolumeUp, "Vol +", StabiloCyan) {
                        remoteClient.sendCommand("VOLUME_UP")
                    }
                    MiniBtn(Icons.Default.VolumeDown, "Vol -", StabiloCyan) {
                        remoteClient.sendCommand("VOLUME_DOWN")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // D-Pad Circle
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(DarkBgCardElevated)
                        .border(2.dp, StabiloCyan.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.TopCenter)
                            .clickable { remoteClient.sendCommand("DPAD_UP") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, "Atas", tint = StabiloLime, modifier = Modifier.size(32.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.BottomCenter)
                            .clickable { remoteClient.sendCommand("DPAD_DOWN") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, "Bawah", tint = StabiloLime, modifier = Modifier.size(32.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.CenterStart)
                            .clickable { remoteClient.sendCommand("DPAD_LEFT") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Kiri", tint = StabiloLime, modifier = Modifier.size(32.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.CenterEnd)
                            .clickable { remoteClient.sendCommand("DPAD_RIGHT") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, "Kanan", tint = StabiloLime, modifier = Modifier.size(32.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(StabiloLime)
                            .clickable { remoteClient.sendCommand("DPAD_OK") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "OK",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom nav
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniBtn(Icons.AutoMirrored.Filled.ArrowBack, "Back", StabiloYellow) {
                        remoteClient.sendCommand("BACK")
                    }
                    MiniBtn(Icons.Default.Home, "Home", StabiloLime) {
                        remoteClient.sendCommand("HOME")
                    }
                    MiniBtn(Icons.Default.ViewCarousel, "Recent", StabiloCyan) {
                        remoteClient.sendCommand("RECENTS")
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniBtn(
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
