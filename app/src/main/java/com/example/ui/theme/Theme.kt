package dev.andikuneiocontroll.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = StabiloLime,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E3812),
    onPrimaryContainer = StabiloLime,
    secondary = StabiloCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0C2B38),
    onSecondaryContainer = StabiloCyan,
    tertiary = StabiloYellow,
    onTertiary = Color.Black,
    background = DarkBgPrimary,
    onBackground = TextPrimary,
    surface = DarkBgCard,
    onSurface = TextPrimary,
    surfaceVariant = DarkBgCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkDivider
)

private val LightColorScheme = darkColorScheme(
    primary = StabiloLime,
    onPrimary = Color.Black,
    background = DarkBgPrimary,
    surface = DarkBgCard
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
