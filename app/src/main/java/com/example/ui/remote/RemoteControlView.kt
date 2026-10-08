package com.example.ui.remote

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Airplay
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.remote.ClientConnectionState
import com.example.remote.RemoteClient
import com.example.remote.RemoteSocketServer
import com.example.remote.TvAccessibilityService
import com.example.remote.TvReceiverState

@Composable
fun RemoteControlView(
    remoteClient: RemoteClient,
    remoteServer: RemoteSocketServer,
    isTvMode: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val receiverState by remoteServer.receiverState.collectAsState()
    val clientState by remoteClient.connectionState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: D-Pad, 1: Touchpad, 2: Air Mouse, 3: Gamepad, 4: Keyboard, 5: Media, 6: TV Server Mode

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tv,
                        contentDescription = null,
                        tint = if (clientState.isConnected || receiverState.connectedClient != null) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isTvMode) "TV Receiver (Zank Server)" else "Remote Client",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (clientState.isConnected)
                                "Terhubung ke ${clientState.targetIp}"
                            else if (receiverState.connectedClient != null)
                                "Client: ${receiverState.connectedClient}"
                            else
                                "Tahap I: Belum terhubung",
                            color = if (clientState.isConnected || receiverState.connectedClient != null) Color(0xFF10B981) else Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                if (!isTvMode && !clientState.isConnected) {
                    var showConnectDialog by remember { mutableStateOf(false) }
                    Button(
                        onClick = { showConnectDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                        modifier = Modifier.testTag("connect_tv_btn")
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hubungkan", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (showConnectDialog) {
                        ConnectToTvDialog(
                            onConnect = { ip, port, code ->
                                remoteClient.connect(ip, port, code) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                                showConnectDialog = false
                            },
                            onDismiss = { showConnectDialog = false }
                        )
                    }
                } else if (!isTvMode && clientState.isConnected) {
                    IconButton(
                        onClick = { remoteClient.disconnect() },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFF43F5E))
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = "Putuskan")
                    }
                }
            }
        }

        // Feature Navigation Tabs (D-Pad, Touchpad, Air Mouse, Gamepad, Keyboard, Media, TV Server)
        val tabs = listOf("D-Pad", "Touchpad", "Air Mouse", "Gamepad", "Keyboard", "Media", "Server TV")
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF0F172A),
            contentColor = Color(0xFF06B6D4),
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) Color(0xFF06B6D4) else Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            when (selectedTab) {
                0 -> DPadControlLayout(remoteClient)
                1 -> TouchpadLayout(remoteClient)
                2 -> AirMouseLayout(remoteClient, clientState)
                3 -> GamepadLayout(remoteClient)
                4 -> KeyboardInputLayout(remoteClient)
                5 -> MediaControlLayout(remoteClient)
                6 -> TvServerSettingsLayout(remoteServer, receiverState)
            }
        }
    }
}

@Composable
fun DPadControlLayout(client: RemoteClient) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Quick Navigation Bar: Power, Mute, Volume
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RemoteRoundButton(icon = Icons.Default.PowerSettingsNew, label = "Power", color = Color(0xFFF43F5E)) {
                client.sendCommand("POWER")
            }
            RemoteRoundButton(icon = Icons.AutoMirrored.Filled.VolumeMute, label = "Mute", color = Color(0xFF94A3B8)) {
                client.sendCommand("MUTE")
            }
            RemoteRoundButton(icon = Icons.AutoMirrored.Filled.VolumeUp, label = "Vol +", color = Color(0xFF06B6D4)) {
                client.sendCommand("VOLUME_UP")
            }
            RemoteRoundButton(icon = Icons.Default.VolumeDown, label = "Vol -", color = Color(0xFF06B6D4)) {
                client.sendCommand("VOLUME_DOWN")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large D-Pad Controller
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
                .border(2.dp, Color(0xFF334155), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // UP
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.TopCenter)
                    .clickable { client.sendCommand("DPAD_UP") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Atas", tint = Color.White, modifier = Modifier.size(36.dp))
            }

            // DOWN
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.BottomCenter)
                    .clickable { client.sendCommand("DPAD_DOWN") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bawah", tint = Color.White, modifier = Modifier.size(36.dp))
            }

            // LEFT
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.CenterStart)
                    .clickable { client.sendCommand("DPAD_LEFT") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Kiri", tint = Color.White, modifier = Modifier.size(36.dp))
            }

            // RIGHT
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.CenterEnd)
                    .clickable { client.sendCommand("DPAD_RIGHT") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Kanan", tint = Color.White, modifier = Modifier.size(36.dp))
            }

            // CENTER OK BUTTON
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF06B6D4))
                    .clickable { client.sendCommand("DPAD_OK") },
                contentAlignment = Alignment.Center
            ) {
                Text("OK", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // System Navigation Bar: Back, Home, Recents
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteRoundButton(icon = Icons.AutoMirrored.Filled.ArrowBack, label = "Kembali") {
                client.sendCommand("BACK")
            }
            RemoteRoundButton(icon = Icons.Default.Home, label = "Home") {
                client.sendCommand("HOME")
            }
            RemoteRoundButton(icon = Icons.Default.ViewCarousel, label = "Recent") {
                client.sendCommand("RECENTS")
            }
        }
    }
}

@Composable
fun TouchpadLayout(client: RemoteClient) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Geser jari untuk menggerakkan kursor, ketuk untuk klik:",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            client.sendCommand("MOUSE_CLICK")
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        client.sendMouseMove(dragAmount.x, dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Mouse, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Touchpad Virtual", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Left / Right Click buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { client.sendCommand("MOUSE_CLICK") },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Klik Kiri")
            }
            Button(
                onClick = { client.sendCommand("BACK") },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Klik Kanan (Kembali)")
            }
        }
    }
}

@Composable
fun AirMouseLayout(client: RemoteClient, state: ClientConnectionState) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Airplay,
            contentDescription = null,
            tint = if (state.isAirMouseActive) Color(0xFF10B981) else Color(0xFF06B6D4),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Air Mouse Control (Gyroscope)",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Gerakkan ponsel di udara untuk mengarahkan kursor TV secara real-time.",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                if (state.isAirMouseActive) {
                    client.disableAirMouse()
                } else {
                    val ok = client.enableAirMouse()
                    if (!ok) {
                        Toast.makeText(context, "Sensor Gyroscope/Accelerometer tidak ditemukan pada perangkat ini", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isAirMouseActive) Color(0xFFF43F5E) else Color(0xFF06B6D4)
            ),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (state.isAirMouseActive) "Matikan Air Mouse" else "Aktifkan Air Mouse",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { client.sendCommand("MOUSE_CLICK") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(60.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("KLIK KURSOR (TRIGGER)", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GamepadLayout(client: RemoteClient) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Shoulder Buttons (L1, R1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { client.sendCommand("DPAD_LEFT") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
            ) {
                Text("L1")
            }
            Text("GAMEPAD MODE", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
            Button(
                onClick = { client.sendCommand("DPAD_RIGHT") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
            ) {
                Text("R1")
            }
        }

        // Main Controller Arena: Left D-Pad + Right Action Buttons (X, Y, A, B)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Directional Pad
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { client.sendCommand("DPAD_UP") }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White)
                }
                Row {
                    IconButton(onClick = { client.sendCommand("DPAD_LEFT") }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF334155), CircleShape)
                    )
                    IconButton(onClick = { client.sendCommand("DPAD_RIGHT") }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = Color.White)
                    }
                }
                IconButton(onClick = { client.sendCommand("DPAD_DOWN") }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White)
                }
            }

            // Action Buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = { client.sendCommand("DPAD_UP") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Text("Y", fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { client.sendCommand("DPAD_LEFT") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Text("X", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Button(
                        onClick = { client.sendCommand("DPAD_RIGHT") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Text("B", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Button(
                    onClick = { client.sendCommand("DPAD_OK") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Text("A", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }

        // Start / Select
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { client.sendCommand("BACK") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
            ) {
                Text("SELECT")
            }
            Spacer(modifier = Modifier.width(24.dp))
            Button(
                onClick = { client.sendCommand("HOME") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
            ) {
                Text("START")
            }
        }
    }
}

@Composable
fun KeyboardInputLayout(client: RemoteClient) {
    var textInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Ketik teks untuk dikirim langsung ke TV:",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Ketik kata kunci pencarian / teks...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (textInput.isNotEmpty()) {
                    client.sendCommand("INPUT_TEXT", textInput)
                    textInput = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kirim Teks ke TV", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MediaControlLayout(client: RemoteClient) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Kontrol Media & Audio TV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { client.sendCommand("DPAD_LEFT") },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Sebelumnya", tint = Color.White, modifier = Modifier.size(36.dp))
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF06B6D4))
                    .clickable { client.sendCommand("DPAD_OK") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Putar/Jeda", tint = Color.Black, modifier = Modifier.size(44.dp))
            }

            IconButton(
                onClick = { client.sendCommand("DPAD_RIGHT") },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Berikutnya", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteRoundButton(icon = Icons.AutoMirrored.Filled.VolumeMute, label = "Bisu") {
                client.sendCommand("MUTE")
            }
            RemoteRoundButton(icon = Icons.Default.VolumeDown, label = "Vol -") {
                client.sendCommand("VOLUME_DOWN")
            }
            RemoteRoundButton(icon = Icons.AutoMirrored.Filled.VolumeUp, label = "Vol +") {
                client.sendCommand("VOLUME_UP")
            }
        }
    }
}

@Composable
fun TvServerSettingsLayout(server: RemoteSocketServer, state: TvReceiverState) {
    val context = LocalContext.current
    val isAccessibilityActive = remember { TvAccessibilityService.isAccessibilityEnabled(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Konfigurasi Server TV (Mode Penerima)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Aktifkan mode ini jika perangkat ini adalah Android TV atau bertindak sebagai penerima remote.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kode Pairing TV Anda:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text(
                        text = state.pairingCode,
                        color = Color(0xFF06B6D4),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = 4.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Port Remote: ${state.port}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text(
                    text = "Status: ${if (state.connectedClient != null) "Terhubung ke ${state.connectedClient}" else "Menunggu koneksi dari HP..."}",
                    color = if (state.connectedClient != null) Color(0xFF10B981) else Color(0xFFF59E0B),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accessibility Service check & guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Accessibility,
                        contentDescription = null,
                        tint = if (isAccessibilityActive) Color(0xFF10B981) else Color(0xFFF43F5E)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAccessibilityActive) "Aksesibilitas Aktif" else "Aksesibilitas Diperlukan",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Diperlukan untuk simulasi klik mouse, navigasi DPAD, Home, dan Back pada Android TV.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { TvAccessibilityService.openSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text("Buka Pengaturan Aksesibilitas")
                }
            }
        }
    }
}

@Composable
fun RemoteRoundButton(
    icon: ImageVector,
    label: String,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF334155), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
    }
}

@Composable
fun ConnectToTvDialog(
    onConnect: (String, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Hubungkan ke Android TV",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Masukkan alamat IP TV dan 4-digit kode pairing yang tampil di layar TV:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address TV (misal: 192.168.1.15)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("4-Digit Kode Pairing TV") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Batal", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (ip.isNotEmpty() && code.isNotEmpty()) {
                                onConnect(ip.trim(), 23017, code.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                    ) {
                        Text("Hubungkan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
