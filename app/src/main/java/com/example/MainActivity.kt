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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.andikuneiocontroll.data.local.PrefsRepository
import dev.andikuneiocontroll.ui.MainScreen
import dev.andikuneiocontroll.ui.OnboardingScreen
import dev.andikuneiocontroll.ui.SplashScreen
import dev.andikuneiocontroll.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0F1D)
                ) {
                    AppNavigationFlow(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Flow utama aplikasi:
 * - Cek onboarding sudah selesai?
 *   - Belum → Splash → Onboarding → MainScreen
 *   - Sudah → Splash → MainScreen
 */
private sealed interface AppState {
    data object Splash : AppState
    data object Onboarding : AppState
    data object Main : AppState
}

@Composable
private fun AppNavigationFlow(viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = remember { PrefsRepository(context) }

    var currentState by remember { mutableStateOf<AppState>(AppState.Splash) }

    // Cek onboarding saat pertama buka
    LaunchedEffect(Unit) {
        val onboardingDone = prefs.getOnboardingDoneOnce()
        // State awal = Splash, setelah itu tentukan
        // SplashScreen akan memanggil onFinished()
        // jadi kita simpan flag dulu
        onboardingDoneFlag = onboardingDone
    }

    when (currentState) {
        AppState.Splash -> {
            SplashScreen(
                onFinished = {
                    currentState = if (onboardingDoneFlag) {
                        AppState.Main
                    } else {
                        AppState.Onboarding
                    }
                }
            )
        }

        AppState.Onboarding -> {
            OnboardingScreen(
                onFinished = {
                    // Tandai onboarding selesai
                    kotlinx.coroutines.MainScope().launch {
                        prefs.setOnboardingDone(true)
                    }
                    currentState = AppState.Main
                }
            )
        }

        AppState.Main -> {
            MainScreen(viewModel = viewModel)
        }
    }
}

// Flag global (sederhana) untuk menyimpan hasil cek onboarding
private var onboardingDoneFlag: Boolean = false
