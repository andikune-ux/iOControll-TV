package dev.andikuneiocontroll.ui.remote.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow

/**
 * RemoteTopBar — Toolbar atas remote.
 *
 * Tombol:
 * - Voice (Google Voice command)
 * - Input (HDMI/AV/TV picker)
 * - Cast (Screen cast dari HP ke TV)
 * - Keyboard (input text ke TV)
 * - Copy (salin teks dari TV)
 * - TV (buka TV picker / multi-TV manager)
 */
@Composable
fun RemoteTopBar(
    onVoice: () -> Unit,
    onInput: () -> Unit,
    onCast: () -> Unit,
    onKeyboard: () -> Unit,
    onCopy: () -> Unit,
    onTvList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RemoteSquareButton(
            icon = Icons.Default.Mic,
            label = "Voice",
            accentColor = StabiloCyan,
            onClick = onVoice,
            hapticType = 1
        )

        RemoteSquareButton(
            icon = Icons.Default.VideoSettings,
            label = "Input",
            accentColor = StabiloYellow,
            onClick = onInput,
            hapticType = 1
        )

        RemoteSquareButton(
            icon = Icons.Default.Cast,
            label = "Cast",
            accentColor = StabiloLime,
            onClick = onCast,
            hapticType = 1
        )

        RemoteSquareButton(
            icon = Icons.Default.Keyboard,
            label = "Keyboard",
            accentColor = StabiloCyan,
            onClick = onKeyboard,
            hapticType = 1
        )

        RemoteSquareButton(
            icon = Icons.Default.ContentCopy,
            label = "Copy",
            accentColor = StabiloYellow,
            onClick = onCopy,
            hapticType = 1
        )

        RemoteSquareButton(
            icon = Icons.Default.Tv,
            label = "TV",
            accentColor = StabiloPink,
            onClick = onTvList,
            hapticType = 1
        )
    }
}
