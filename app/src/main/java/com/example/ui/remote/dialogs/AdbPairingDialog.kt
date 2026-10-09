package dev.andikuneiocontroll.ui.remote.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
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
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow
import dev.andikuneiocontroll.ui.theme.TextPrimary
import dev.andikuneiocontroll.ui.theme.TextSecondary

@Composable
fun AdbPairingDialog(
    tvName: String,
    defaultHost: String = "",
    isSubmitting: Boolean = false,
    errorMessage: String = "",
    onPair: (host: String, port: Int, code: String) -> Unit,
    onCancel: () -> Unit
) {
    var hostInput by remember { mutableStateOf(defaultHost.ifBlank { "192.168." }) }
    var portInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StabiloCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Link, null, tint = StabiloCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Pairing Wireless Debugging",
                            color = StabiloCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            tvName.ifBlank { "Google TV" },
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBgCardElevated)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = StabiloYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Cara dapat kode pairing:",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "1. TV → Setelan → Tentang → tap Build 7x\n" +
                                "2. Developer Options → Wireless Debugging → ON\n" +
                                "3. Tap \"Pair device with pairing code\"\n" +
                                "4. TV tampilkan IP + Port + Code",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = hostInput,
                    onValueChange = {
                        hostInput = it
                        localError = ""
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
                        portInput = it.filter { c -> c.isDigit() }
                        localError = ""
                    },
                    label = { Text("Port Pairing (5 digit)") },
                    placeholder = { Text("Contoh: 37251") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloYellow,
                        focusedLabelColor = StabiloYellow,
                        cursorColor = StabiloYellow
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it.filter { c -> c.isDigit() }.take(6)
                        localError = ""
                    },
                    label = { Text("Kode Pairing (6 digit)") },
                    placeholder = { Text("Contoh: 123456") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StabiloLime,
                        focusedLabelColor = StabiloLime,
                        cursorColor = StabiloLime
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                val errorToShow = localError.ifBlank { errorMessage }
                if (errorToShow.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        errorToShow,
                        color = StabiloPink,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Batal", color = TextPrimary)
                    }

                    Button(
                        onClick = {
                            val host = hostInput.trim()
                            val port = portInput.trim().toIntOrNull() ?: 0
                            val code = codeInput.trim()

                            if (host.count { it == '.' } != 3 || host.endsWith(".")) {
                                localError = "Format IP tidak valid"
                            } else if (port <= 0 || port > 65535) {
                                localError = "Port tidak valid (5 digit)"
                            } else if (code.length != 6) {
                                localError = "Kode harus 6 digit"
                            } else {
                                onPair(host, port, code)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StabiloCyan),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text("Pairing", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
