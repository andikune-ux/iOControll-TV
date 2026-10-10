package dev.andikuneiocontroll.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.andikuneiocontroll.remote.protocol.androidtv.TlsHelper
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow
import dev.andikuneiocontroll.ui.theme.TextPrimary
import dev.andikuneiocontroll.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

// ==========================================================
// 1. PIN DIALOG
// ==========================================================
private const val DEVELOPER_PIN = "140399"

@Composable
fun DeveloperPinDialog(
    onPinCorrect: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = BorderStroke(1.dp, StabiloLime.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Build, null, tint = StabiloLime, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Opsi Developer", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Masukkan PIN 6 digit untuk lanjut",
                    color = TextSecondary, fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() }.take(6)
                        pin = filtered
                        error = ""
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloLime,
                        focusedLabelColor = StabiloLime,
                        cursorColor = StabiloLime
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error, color = StabiloPink, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            if (pin == DEVELOPER_PIN) {
                                onPinCorrect()
                            } else {
                                error = "PIN salah"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StabiloLime),
                        shape = RoundedCornerShape(10.dp),
                        enabled = pin.length == 6,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Buka", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================================
// 2. DEVELOPER OPTIONS MENU
// ==========================================================
@Composable
fun DeveloperOptionsDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    var showDiagnostic by remember { mutableStateOf(false) }
    var clientName by remember { mutableStateOf(TlsHelper.getOrCreateClientName(context)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = BorderStroke(1.dp, StabiloLime.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, null, tint = StabiloLime, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Opsi Developer", color = StabiloLime, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Info singkat
                Text("client_name: $clientName", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text("cert: atv_client_v3.p12", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                Spacer(modifier = Modifier.height(16.dp))

                // Tombol Diagnostic TV
                Button(
                    onClick = { showDiagnostic = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StabiloLime),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Search, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diagnostic TV (Scan Port)", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tombol Reset Pairing
                Button(
                    onClick = {
                        TlsHelper.clearPairingData(context)
                        Toast.makeText(context, "✅ Pairing data dihapus", Toast.LENGTH_SHORT).show()
                        clientName = TlsHelper.getOrCreateClientName(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                    border = BorderStroke(1.dp, StabiloPink.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = StabiloPink, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Pairing Data", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showDiagnostic) {
            TvDiagnosticDialog(onDismiss = { showDiagnostic = false })
        }
    }
}

// ==========================================================
// 3. TV DIAGNOSTIC (SCAN PORT)
// ==========================================================
private data class PortInfo(val port: Int, val label: String)

private val PORTS_TO_SCAN = listOf(
    PortInfo(6467, "Android TV Remote v2 (pairing)"),
    PortInfo(6466, "Android TV Remote (command)"),
    PortInfo(5555, "ADB over WiFi"),
    PortInfo(8009, "Chromecast"),
    PortInfo(4321, "CetusPlay"),
    PortInfo(7000, "AirPlay"),
    PortInfo(1900, "SSDP"),
    PortInfo(80, "HTTP"),
    PortInfo(8080, "HTTP Alt"),
    PortInfo(3000, "LG webOS"),
    PortInfo(8001, "Samsung Tizen"),
    PortInfo(9000, "Vizio SmartCast")
)

private data class PortResult(
    val port: Int,
    val label: String,
    val status: String,
    val ms: Long
)

@Composable
fun TvDiagnosticDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var ipInput by remember { mutableStateOf("192.168.") }
    var isScanning by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<PortResult>() }

    Dialog(onDismissRequest = { if (!isScanning) onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = StabiloCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Diagnostic TV", color = StabiloCyan, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, enabled = !isScanning) {
                        Icon(Icons.Default.Close, "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    label = { Text("IP TV") },
                    placeholder = { Text("192.168.0.103") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloCyan,
                        focusedLabelColor = StabiloCyan,
                        cursorColor = StabiloCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            results.clear()
                            isScanning = true
                            scope.launch {
                                val ip = ipInput.trim()
                                PORTS_TO_SCAN.forEach { info ->
                                    val r = scanPort(ip, info.port, 3000)
                                    results.add(
                                        PortResult(info.port, info.label, r.first, r.second)
                                    )
                                }
                                isScanning = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StabiloCyan),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isScanning && ipInput.count { it == '.' } == 3,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Search, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isScanning) "Scan…" else "Scan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val text = buildString {
                                appendLine("📊 Diagnostic TV — ${ipInput.trim()}")
                                appendLine("─".repeat(40))
                                results.forEach { r ->
                                    val statusEmoji = when (r.status) {
                                        "OPEN" -> "✅"
                                        "CLOSED" -> "❌"
                                        else -> "⏱"
                                    }
                                    appendLine("Port ${r.port}  $statusEmoji ${r.status.padEnd(7)} ${r.ms}ms  (${r.label})")
                                }
                            }
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Diagnostic", text))
                            Toast.makeText(context, "✅ Hasil di-copy", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                        border = BorderStroke(1.dp, StabiloLime.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = results.isNotEmpty() && !isScanning,
                        modifier = Modifier.height(46.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, tint = StabiloLime, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hasil scan
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (results.isEmpty() && !isScanning) {
                        Text(
                            "Tap \"Scan\" untuk memeriksa port di TV",
                            color = TextSecondary, fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    results.forEach { r ->
                        PortResultRow(r)
                    }
                }
            }
        }
    }
}

@Composable
private fun PortResultRow(r: PortResult) {
    val (color, bg) = when (r.status) {
        "OPEN" -> StabiloLime to StabiloLime.copy(alpha = 0.12f)
        "CLOSED" -> StabiloPink to StabiloPink.copy(alpha = 0.12f)
        else -> StabiloYellow to StabiloYellow.copy(alpha = 0.12f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(8.dp).clip(CircleShape).background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${r.port}  ${r.status}",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(r.label, color = TextSecondary, fontSize = 10.sp)
        }
        Text("${r.ms}ms", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

/**
 * Scan satu port — return Pair<status, elapsedMs>.
 */
private suspend fun scanPort(ip: String, port: Int, timeoutMs: Int): Pair<String, Long> {
    return withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            val elapsed = System.currentTimeMillis() - start
            try { socket.close() } catch (_: Exception) {}
            "OPEN" to elapsed
        } catch (e: SocketTimeoutException) {
            "TIMEOUT" to timeoutMs.toLong()
        } catch (e: Exception) {
            "CLOSED" to (System.currentTimeMillis() - start)
        }
    }
}
