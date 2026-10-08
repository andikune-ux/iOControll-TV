package dev.andikuneiocontroll.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionHandlerView(
    onAllGranted: () -> Unit
) {
    val context = LocalContext.current
    var hasManageStorage by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
        )
    }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Continue to app
        onAllGranted()
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasManageStorage = Environment.isExternalStorageManager()
            if (hasManageStorage) {
                requestRemainingPermissions(multiplePermissionLauncher)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D31)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF06B6D4),
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Izin Akses iOControll Tv",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Untuk menjalankan fungsi Transfer File Wi-Fi X-plore dan Remote TV Zank secara maksimal, mohon berikan izin berikut:",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                PermissionFeatureRow(
                    icon = Icons.Default.Folder,
                    title = "Akses Penuh Penyimpanan (File Manager)",
                    desc = "Diperlukan untuk membaca, menyalin, memindahkan, dan mengompres file pada memori HP/TV."
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionFeatureRow(
                    icon = Icons.Default.Wifi,
                    title = "Jaringan & Wi-Fi Lokal",
                    desc = "Diperlukan untuk menjalankan server HTTP file transfer dan mendeteksi perangkat TV."
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionFeatureRow(
                    icon = Icons.Default.Notifications,
                    title = "Layanan Latar Belakang & Notifikasi",
                    desc = "Menjaga server file transfer tetap aktif dan memungkinkan kontrol cepat dari notifikasi."
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionFeatureRow(
                    icon = Icons.Default.Bluetooth,
                    title = "Bluetooth & Sambungan TV",
                    desc = "Mendukung konektivitas nirkabel cadangan ke TV."
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                manageStorageLauncher.launch(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                manageStorageLauncher.launch(intent)
                            }
                        } else {
                            requestRemainingPermissions(multiplePermissionLauncher)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("grant_permissions_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Berikan Izin & Lanjutkan",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

private fun requestRemainingPermissions(launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
    val list = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.READ_MEDIA_IMAGES)
        list.add(Manifest.permission.READ_MEDIA_VIDEO)
        list.add(Manifest.permission.READ_MEDIA_AUDIO)
        list.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        list.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    list.add(Manifest.permission.ACCESS_WIFI_STATE)
    list.add(Manifest.permission.CHANGE_WIFI_STATE)

    launcher.launch(list.toTypedArray())
}

@Composable
private fun PermissionFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(desc, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
    }
}
