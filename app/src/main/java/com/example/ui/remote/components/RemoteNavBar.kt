package dev.andikuneiocontroll.ui.remote.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow

/**
 * RemoteNavBar — Bar navigasi bawah.
 *
 * Tombol:
 * - Home (ke home TV)
 * - Back (kembali di TV)
 * - Recent (recent apps TV)
 * - Mute (matikan suara TV)
 */
@Composable
fun RemoteNavBar(
    onHome: () -> Unit,
    onBack: () -> Unit,
    onRecent: () -> Unit,
    onMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RemoteIconLabelButton(
            icon = Icons.Default.Home,
            label = "Home",
            accentColor = StabiloLime,
            onClick = onHome,
            size = RemoteButtonSize.LARGE,
            hapticType = 1
        )

        RemoteIconLabelButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = "Back",
            accentColor = StabiloYellow,
            onClick = onBack,
            size = RemoteButtonSize.LARGE,
            hapticType = 1
        )

        RemoteIconLabelButton(
            icon = Icons.Default.ViewCarousel,
            label = "Recent",
            accentColor = StabiloCyan,
            onClick = onRecent,
            size = RemoteButtonSize.LARGE,
            hapticType = 1
        )

        RemoteIconLabelButton(
            icon = Icons.Default.VolumeOff,
            label = "Mute",
            accentColor = StabiloPink,
            onClick = onMute,
            size = RemoteButtonSize.LARGE,
            hapticType = 1
        )
    }
}
