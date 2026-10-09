package dev.andikuneiocontroll.ui.remote.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.TextPrimary
import dev.andikuneiocontroll.ui.theme.TextSecondary

/**
 * RemoteHeader — Header atas remote TV.
 *
 * Layout:
 * [⬅️ Back]  [Nama TV + Status]  [⚙️ Settings] [🔴 Power]
 */
@Composable
fun RemoteHeader(
    tvName: String,
    protocolName: String,
    isConnected: Boolean,
    latencyMs: Int = 0,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onPower: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBgCard)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tombol Back (kembali ke file manager)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = StabiloCyan,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Nama TV + Status
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = tvName.ifBlank { "Belum Terhubung" },
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Dot status
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF22C55E) else StabiloPink)
                )
                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = buildString {
                        append(if (isConnected) "Terhubung" else "Terputus")
                        if (protocolName.isNotBlank()) {
                            append(" · ")
                            append(protocolName)
                        }
                        if (isConnected && latencyMs > 0) {
                            append(" · ")
                            append("${latencyMs}ms")
                        }
                    },
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Tombol Settings
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Pengaturan",
                tint = StabiloLime,
                modifier = Modifier.size(20.dp)
            )
        }

        // Tombol Power (merah)
        IconButton(
            onClick = onPower,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Power TV",
                tint = StabiloPink,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
