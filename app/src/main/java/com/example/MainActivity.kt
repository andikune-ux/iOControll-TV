package dev.andikuneiocontroll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.andikuneiocontroll.data.local.PrefsRepository
import dev.andikuneiocontroll.ui.MainScreen
import dev.andikuneiocontroll.ui.OnboardingScreen
import dev.andikuneiocontroll.ui.SplashScreen
import dev.andikuneiocontroll.ui.components.CrashLogDialog
import dev.andikuneiocontroll.ui.theme.MyApplicationTheme
import dev.andikuneiocontroll.util.CrashHandler
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install crash handler — WAJIB sebelum setContent
        CrashHandler.install(this)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0F1D)
                ) {
                    AppRoot(viewModel = viewModel)
                }
            }
        }
    }
}

private sealed interface AppState {
    data object Splash : AppState
    data object Onboarding : AppState
    data object Main : AppState
}

/**
 * AppRoot — root composable yang menampilkan:
 * 1. Crash dialog (kalau ada crash sebelumnya)
 * 2. Flow: Splash → Onboarding → Main
 *
 * CRASH DIALOG: ditampilkan paling atas (overlay), tidak bisa dismiss
 * kecuali user tap tombol Salin/Hapus/Tutup.
 */
@Composable
private fun AppRoot(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PrefsRepository(context) }

    var currentState by remember { mutableStateOf<AppState>(AppState.Splash) }
    var onboardingDone by remember { mutableStateOf<Boolean?>(null) }

    // Crash state
    var crashContent by remember { mutableStateOf("") }
    var crashFileName by remember { mutableStateOf("") }
    var crashFile by remember { mutableStateOf<File?>(null) }
    var showCrashDialog by remember { mutableStateOf(false) }

    // Cek crash log SEKALI saat app dibuka
    LaunchedEffect(Unit) {
        // 1. Cek crash log DULU (biar langsung muncul)
        try {
            val file = CrashHandler.getLatestCrashFile(context)
            if (file != null && file.exists()) {
                crashContent = try {
                    file.readText()
                } catch (e: Exception) {
                    "(Gagal baca log: ${e.message})"
                }
                crashFileName = file.name
                crashFile = file
                showCrashDialog = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Cek onboarding
        onboardingDone = prefs.getOnboardingDoneOnce()
    }

    // ==============================
    // FLOW UTAMA
    // ==============================
    when (currentState) {
        AppState.Splash -> {
            SplashScreen(
                onFinished = {
                    scope.launch {
                        val autoConnect = prefs.getAutoConnectOnce()
                        if (autoConnect) {
                            viewModel.autoConnectLastTv()
                        }
                        currentState = when {
                            onboardingDone == true -> AppState.Main
                            onboardingDone == false -> AppState.Onboarding
                            else -> AppState.Main
                        }
                    }
                }
            )
        }

        AppState.Onboarding -> {
            OnboardingScreen(
                onFinished = {
                    scope.launch { prefs.setOnboardingDone(true) }
                    currentState = AppState.Main
                }
            )
        }

        AppState.Main -> {
            MainScreen(viewModel = viewModel)
        }
    }

    // ==============================
    // CRASH DIALOG — OVERLAY PALING ATAS
    // ==============================
    // Ditampilkan SETELAH frame lain render, tidak akan hilang karena recompose.
    if (showCrashDialog) {
        CrashLogDialog(
            crashContent = crashContent,
            crashFileName = crashFileName,
            onDelete = {
                try {
                    crashFile?.let { CrashHandler.deleteCrashFile(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                showCrashDialog = false
                crashFile = null
            },
            onDismiss = {
                // Tutup dialog, tapi JANGAN hapus file
                // Biar user bisa lihat lagi kalau restart
                showCrashDialog = false
            }
        )
    }
}
