package dev.andikuneiocontroll.ui.remote.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikuneiocontroll.data.local.PrefsRepository
import dev.andikuneiocontroll.ui.remote.components.HapticHelper
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.DarkBgPrimary
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow
import dev.andikuneiocontroll.ui.theme.TextPrimary
import dev.andikuneiocontroll.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun RemoteSettingsScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PrefsRepository(context) }

    val hapticEnabled by prefs.isHapticEnabled.collectAsState(initial = true)
    val soundEnabled by prefs.isSoundEnabled.collectAsState(initial = false)
    val autoConnect by prefs.autoConnect.collectAsState(initial = true)

    var airMouseSens by remember { mutableFloatStateOf(50f) }
    var mouseSens by remember { mutableFloatStateOf(40f) }
    var buttonSize by remember { mutableFloatStateOf(1f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBgPrimary)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBgCard)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = StabiloCyan)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Settings, null, tint = StabiloLime)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Pengaturan Remote",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // HAPTIC
                    SettingToggleRow(
                        title = "Haptic Feedback",
                        subtitle = "Getar halus saat tombol ditekan",
                        checked = hapticEnabled,
                        onCheckedChange = { checked ->
                            HapticHelper.enabled = checked
                            scope.launch { prefs.setHapticEnabled(checked) }
                        },
                        accentColor = StabiloLime
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // SOUND
                    SettingToggleRow(
                        title = "Sound Effect",
                        subtitle = "Bunyi saat tombol ditekan",
                        checked = soundEnabled,
                        onCheckedChange = { checked ->
                            scope.launch { prefs.setSoundEnabled(checked) }
                        },
                        accentColor = StabiloCyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // AUTO CONNECT
                    SettingToggleRow(
                        title = "Auto-Connect",
                        subtitle = "Otomatis connect ke TV terakhir",
                        checked = autoConnect,
                        onCheckedChange = { checked ->
                            scope.launch { prefs.setAutoConnect(checked) }
                        },
                        accentColor = StabiloYellow
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // SLIDER AIR MOUSE
                    SettingSlider(
                        title = "Sensitivitas Air Mouse",
                        value = airMouseSens,
                        onValueChange = {
                            airMouseSens = it
                            scope.launch { prefs.setAirMouseSensitivity(it.toInt()) }
                        },
                        accentColor = StabiloCyan
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // SLIDER MOUSE
                    SettingSlider(
                        title = "Sensitivitas Mouse",
                        value = mouseSens,
                        onValueChange = {
                            mouseSens = it
                            scope.launch { prefs.setMouseSensitivity(it.toInt()) }
                        },
                        accentColor = StabiloPink
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // UKURAN TOMBOL
                    Text(
                        "Ukuran Tombol",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SizeButton("Kecil", buttonSize == 0f, StabiloCyan) {
                            buttonSize = 0f
                            scope.launch { prefs.setButtonSize("SMALL") }
                        }
                        SizeButton("Sedang", buttonSize == 1f, StabiloLime) {
                            buttonSize = 1f
                            scope.launch { prefs.setButtonSize("MEDIUM") }
                        }
                        SizeButton("Besar", buttonSize == 2f, StabiloYellow) {
                            buttonSize = 2f
                            scope.launch { prefs.setButtonSize("LARGE") }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // RESET
                    Button(
                        onClick = {
                            scope.launch {
                                prefs.setHapticEnabled(true)
                                prefs.setSoundEnabled(false)
                                prefs.setAutoConnect(true)
                                prefs.setAirMouseSensitivity(50)
                                prefs.setMouseSensitivity(40)
                                prefs.setButtonSize("MEDIUM")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Reset ke Default", color = StabiloPink)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkBgCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = DarkBgCardElevated
                )
            )
        }
    }
}

@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkBgCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${value.toInt()}%", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = accentColor.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
private fun RowScope.SizeButton(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) accentColor else DarkBgCardElevated
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.weight(1f).height(44.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.Black else TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}
