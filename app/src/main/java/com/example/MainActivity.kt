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
import dev.andikuneiocontroll.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

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

private sealed interface AppState {
    data object Splash : AppState
    data object Onboarding : AppState
    data object Main : AppState
}

@Composable
private fun AppNavigationFlow(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PrefsRepository(context) }

    var currentState by remember { mutableStateOf<AppState>(AppState.Splash) }
    var onboardingDone by remember { mutableStateOf<Boolean?>(null) }

    // Cek onboarding selesai atau belum
    LaunchedEffect(Unit) {
        onboardingDone = prefs.getOnboardingDoneOnce()
    }

    when (currentState) {
        AppState.Splash -> {
            SplashScreen(
                onFinished = {
                    currentState = when {
                        onboardingDone == true -> AppState.Main
                        onboardingDone == false -> AppState.Onboarding
                        else -> AppState.Main  // fallback kalau null
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
}
