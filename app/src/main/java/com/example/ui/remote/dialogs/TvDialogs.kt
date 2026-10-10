package dev.andikuneiocontroll.ui.remote.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.andikuneiocontroll.data.local.TvEntity
import dev.andikuneiocontroll.remote.discovery.DiscoveredTv
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow
import dev.andikuneiocontroll.ui.theme.TextMuted
import dev.andikuneiocontroll.ui.theme.TextPrimary
import dev.andikuneiocontroll.ui.theme.TextSecondary

// ==========================================================
// 1. TV PICKER DIALOG
// ==========================================================
@Composable
fun TvPickerDialog(
    discoveredTvs: List<DiscoveredTv>,
    savedTvs: List<TvEntity>,
    isScanning: Boolean,
    onRefresh: () -> Unit,
    onSelectDiscovered: (DiscoveredTv) -> Unit,
    onSelectSaved: (TvEntity) -> Unit,
    onDeleteSaved: (TvEntity) -> Unit,
    onManualIp: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloLime.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tv, null, tint = StabiloLime, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Pilih TV", color = StabiloLime, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = StabiloLime),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(Icons.Default.Refresh, null, tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isScanning) "Mencari TV…" else "Scan Ulang",
                        color = Color.Black, fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onManualIp,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.Link, null, tint = StabiloCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hubungkan via IP Manual", color = StabiloCyan, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (savedTvs.isNotEmpty()) {
                        item {
                            Text(
                                "TERSIMPAN", color = StabiloCyan, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }
                        items(savedTvs, key = { "saved_${it.id}" }) { tv ->
                            SavedTvItem(
                                tv = tv,
                                onClick = { onSelectSaved(tv) },
                                onDelete = { onDeleteSaved(tv) }
                            )
                        }
                    }

                    if (discoveredTvs.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "DITEMUKAN (${discoveredTvs.size})",
                                color = StabiloYellow, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }
                        items(discoveredTvs, key = { "disc_${it.deviceId}" }) { tv ->
                            DiscoveredTvItem(tv = tv, onClick = { onSelectDiscovered(tv) })
                        }
                    }

                    if (!isScanning && savedTvs.isEmpty() && discoveredTvs.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Wifi, null, tint = TextMuted, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Tidak ada TV ditemukan",
                                    color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Cek koneksi WiFi\natau gunakan IP Manual",
                                    color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center
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
private fun SavedTvItem(
    tv: TvEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkBgCardElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(StabiloCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Tv, null, tint = StabiloCyan, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tv.displayName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${tv.brand} · ${tv.ipAddress}", color = TextSecondary, fontSize = 11.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, "Hapus", tint = StabiloPink, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun DiscoveredTvItem(tv: DiscoveredTv, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkBgCardElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, StabiloYellow.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(StabiloYellow.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Tv, null, tint = StabiloYellow, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tv.displayName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${tv.brand} · ${tv.ip}", color = TextSecondary, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .background(StabiloYellow.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .border(1.dp, StabiloYellow, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("BARU", color = StabiloYellow, fontWeight = FontWeight.Bold, fontSize = 9.sp)
            }
        }
    }
}
// ==========================================================
// 2. PAIRING CODE DIALOG
// ==========================================================
@Composable
fun PairingPinDialog(
    tvName: String,
    isSubmitting: Boolean = false,
    errorMessage: String = "",
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloLime.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Link, null, tint = StabiloLime, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Pairing", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Lihat layar TV \"$tvName\".\nMasukkan kode yang tampil di bawah:",
                    color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { input ->
                        // Izinkan huruf & angka, maksimal 6 karakter, uppercase otomatis
                        val filtered = input.filter { it.isLetterOrDigit() }.uppercase()
                        if (filtered.length <= 6) code = filtered
                    },
                    label = { Text("Kode 6 karakter") },
                    placeholder = { Text("Contoh: DF0C4B") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloLime,
                        focusedLabelColor = StabiloLime,
                        cursorColor = StabiloLime
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = StabiloPink, fontSize = 11.sp, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Batal", color = TextPrimary)
                    }
                    Button(
                        onClick = { if (code.length >= 6) onSubmit(code) },
                        colors = ButtonDefaults.buttonColors(containerColor = StabiloLime),
                        shape = RoundedCornerShape(10.dp),
                        enabled = code.length >= 6 && !isSubmitting,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text("Hubungkan", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// 3. INFO TV DIALOG
// ==========================================================
@Composable
fun InfoTvDialog(
    tvName: String,
    brand: String,
    protocolName: String,
    ipAddress: String,
    port: Int,
    isConnected: Boolean,
    hasChromecast: Boolean = false,
    firmwareVersion: String = "",
    onReconnect: () -> Unit,
    onForget: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = StabiloCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Info TV", color = StabiloCyan, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoRow("Nama", tvName.ifBlank { "-" })
                InfoRow("Brand", brand.ifBlank { "UNKNOWN" })
                InfoRow("Protokol", protocolName.ifBlank { "-" })
                InfoRow("IP", ipAddress.ifBlank { "-" })
                InfoRow("Port", if (port > 0) port.toString() else "-")
                InfoRow("Status", if (isConnected) "Terhubung" else "Terputus")

                if (firmwareVersion.isNotBlank()) {
                    InfoRow("Firmware", firmwareVersion)
                }

                if (hasChromecast) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StabiloLime.copy(alpha = 0.15f))
                            .border(1.dp, StabiloLime.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Cast, null, tint = StabiloLime, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Chromecast Built-in Tersedia",
                            color = StabiloLime, fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onReconnect,
                        colors = ButtonDefaults.buttonColors(containerColor = StabiloLime),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Sambung Ulang", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onForget,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Lupakan", color = StabiloPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ==========================================================
// 4. INPUT SOURCE DIALOG
// ==========================================================
@Composable
fun InputSourceDialog(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sources = listOf(
        Triple("HDMI 1", "INPUT_HDMI1", Icons.Default.VideoSettings),
        Triple("HDMI 2", "INPUT_HDMI2", Icons.Default.VideoSettings),
        Triple("HDMI 3", "INPUT_HDMI3", Icons.Default.VideoSettings),
        Triple("HDMI 4", "INPUT_HDMI4", Icons.Default.VideoSettings),
        Triple("AV 1", "INPUT_AV1", Icons.Default.Usb),
        Triple("AV 2", "INPUT_AV2", Icons.Default.Usb),
        Triple("TV / Tuner", "INPUT_TV", Icons.Default.LiveTv),
        Triple("USB", "INPUT_USB", Icons.Default.Usb)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloYellow.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cast, null, tint = StabiloYellow)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Input Source", color = StabiloYellow, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                sources.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { item ->
                            val label = item.first
                            val cmd = item.second
                            val icon = item.third
                            Button(
                                onClick = { onSelect(cmd) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, StabiloYellow.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f).height(60.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(icon, null, tint = StabiloYellow, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        label, color = TextPrimary, fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        if (row.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
// ==========================================================
// 5. SHORTCUT DIALOG
// ==========================================================
@Composable
fun ShortcutDialog(
    shortcuts: List<Pair<String, String>>,
    onLaunch: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloPink.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SettingsRemote, null, tint = StabiloPink)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Shortcut App", color = StabiloPink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(shortcuts, key = { it.second }) { item ->
                        val name = item.first
                        val packageName = item.second
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onLaunch(name, packageName) },
                            colors = CardDefaults.cardColors(containerColor = DarkBgCardElevated),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(StabiloPink.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.SettingsRemote,
                                        null,
                                        tint = StabiloPink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    name,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// 6. MANUAL IP DIALOG
// ==========================================================
@Composable
fun ManualIpDialog(
    onSubmit: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var ipInput by remember { mutableStateOf("192.168.") }
    var portInput by remember { mutableStateOf("6467") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, null, tint = StabiloCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Hubungkan via IP Manual",
                        color = StabiloCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Masukkan IP Address TV Anda.\nCek di: Setelan TV → Jaringan → Status.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = ipInput,
                    onValueChange = {
                        ipInput = it
                        errorMessage = ""
                    },
                    label = { Text("IP Address TV") },
                    placeholder = { Text("Contoh: 192.168.0.103") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloCyan,
                        focusedLabelColor = StabiloCyan,
                        cursorColor = StabiloCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = portInput,
                    onValueChange = {
                        portInput = it
                        errorMessage = ""
                    },
                    label = { Text("Port (default: 6467)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloLime,
                        focusedLabelColor = StabiloLime,
                        cursorColor = StabiloLime
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = StabiloPink, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Batal", color = TextPrimary)
                    }

                    Button(
                        onClick = {
                            val ip = ipInput.trim()
                            val port = portInput.trim().toIntOrNull() ?: 6467
                            if (ip.count { it == '.' } != 3 || ip.endsWith(".")) {
                                errorMessage = "Format IP tidak valid"
                            } else {
                                onSubmit(ip, port)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StabiloCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Hubungkan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
