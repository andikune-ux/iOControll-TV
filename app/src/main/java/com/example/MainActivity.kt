package dev.andikuneiocontroll

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.andikuneiocontroll.ui.MainScreen
import dev.andikuneiocontroll.ui.theme.MyApplicationTheme
import dev.andikuneiocontroll.util.CrashHandler
import dev.andikuneiocontroll.util.DeviceTypeHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install crash handler
        try {
            CrashHandler.install(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Set orientasi berdasarkan device type
        // - TV → Landscape
        // - HP  → Portrait
        requestedOrientation = try {
            DeviceTypeHelper.getPreferredOrientation(this)
        } catch (e: Exception) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0F1D)
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
