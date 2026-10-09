package dev.andikuneiocontroll.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.DarkBgPrimary
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.TextPrimary
import dev.andikuneiocontroll.ui.theme.TextSecondary
import kotlinx.coroutines.delay

// ==========================================================
// 1. SPLASH SCREEN
// ==========================================================
@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    val progress by animateFloatAsState(
        targetValue = (step + 1) / 5f,
        animationSpec = tween(400),
        label = "splash"
    )

    val steps = listOf(
        "Memeriksa izin…",
        "Memeriksa koneksi WiFi…",
        "Menyiapkan database…",
        "Mencari TV terakhir…",
        "Membuka Remote TV…"
    )

    LaunchedEffect(Unit) {
        for (i in steps.indices) {
            step = i
            delay(600)
        }
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(DarkBgCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsRemote,
                    contentDescription = null,
                    tint = StabiloLime,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "iOControll Tv",
                color = StabiloLime,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Transfer File & Remote TV",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(StabiloLime)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = steps.getOrNull(step) ?: "Membuka…",
                color = StabiloCyan,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================================
// 2. ONBOARDING SCREEN (3 slide)
// ==========================================================
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    var page by remember { mutableStateOf(0) }

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.SettingsRemote,
            title = "Selamat Datang di\niOControll Tv",
            subtitle = "Kontrol TV Anda dari HP dengan mudah.\nTidak perlu install aplikasi di TV.",
            accentColor = StabiloLime
        ),
        OnboardingPage(
            icon = Icons.Default.Wifi,
            title = "Pastikan HP & TV\ndi WiFi yang Sama",
            subtitle = "Aktifkan WiFi di TV dan HP Anda.\nAplikasi akan otomatis mencari TV di jaringan.",
            accentColor = StabiloCyan
        ),
        OnboardingPage(
            icon = Icons.Default.Cast,
            title = "Pairing 1x Saja,\nSelanjutnya Auto-Connect",
            subtitle = "Saat pertama connect, TV akan tampilkan\nPIN atau prompt 'Allow'. Setelah itu,\nkoneksi otomatis tersimpan.",
            accentColor = StabiloPink
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val currentPage = pages[page]

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(currentPage.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = currentPage.icon,
                    contentDescription = null,
                    tint = currentPage.accentColor,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = currentPage.title,
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentPage.subtitle,
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Dots indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == page) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == page) currentPage.accentColor
                                else TextSecondary.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tombol Navigasi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (page > 0) {
                    Button(
                        onClick = { page-- },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkBgCardElevated
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text("Sebelumnya", color = TextPrimary)
                    }
                }

                Button(
                    onClick = {
                        if (page < pages.lastIndex) page++ else onFinished()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentPage.accentColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(
                        if (page < pages.lastIndex) "Lanjut →" else "Mulai",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (page < pages.lastIndex) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onFinished,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Lewati", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accentColor: Color
)
