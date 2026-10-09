package dev.andikuneiocontroll.ui.remote.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow
import dev.andikuneiocontroll.ui.theme.TextPrimary
import dev.andikuneiocontroll.ui.theme.TextSecondary

// ==========================================================
// 1. TOP BAR
// ==========================================================
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
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RemoteSquareButton(Icons.Default.Mic, "Voice", StabiloCyan, onVoice, hapticType = 1)
        RemoteSquareButton(Icons.Default.VideoSettings, "Input", StabiloYellow, onInput, hapticType = 1)
        RemoteSquareButton(Icons.Default.Cast, "Cast", StabiloLime, onCast, hapticType = 1)
        RemoteSquareButton(Icons.Default.Keyboard, "Kbd", StabiloCyan, onKeyboard, hapticType = 1)
        RemoteSquareButton(Icons.Default.ContentCopy, "Copy", StabiloYellow, onCopy, hapticType = 1)
        RemoteSquareButton(Icons.Default.Tv, "TV", StabiloPink, onTvList, hapticType = 1)
    }
}

// ==========================================================
// 2. NAV BAR
// ==========================================================
@Composable
fun RemoteNavBar(
    onHome: () -> Unit,
    onBack: () -> Unit,
    onRecent: () -> Unit,
    onMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RemoteIconLabelButton(Icons.Default.Home, "Home", StabiloLime, onHome, size = RemoteButtonSize.LARGE, hapticType = 1)
        RemoteIconLabelButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", StabiloYellow, onBack, size = RemoteButtonSize.LARGE, hapticType = 1)
        RemoteIconLabelButton(Icons.Default.ViewCarousel, "Recent", StabiloCyan, onRecent, size = RemoteButtonSize.LARGE, hapticType = 1)
        RemoteIconLabelButton(Icons.Default.VolumeOff, "Mute", StabiloPink, onMute, size = RemoteButtonSize.LARGE, hapticType = 1)
    }
}

// ==========================================================
// 3. MEDIA BAR
// ==========================================================
@Composable
fun RemoteMediaBar(
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteButton(Icons.Default.SkipPrevious, "Prev", StabiloCyan, onPrev, size = RemoteButtonSize.SMALL)
        RemoteButton(Icons.Default.RadioButtonChecked, "Rewind", StabiloYellow, onRewind, size = RemoteButtonSize.SMALL)
        RemoteButton(Icons.Default.PlayArrow, "Play", StabiloLime, onPlay, size = RemoteButtonSize.MEDIUM, hapticType = 1)
        RemoteButton(Icons.Default.Pause, "Pause", StabiloCyan, onPause, size = RemoteButtonSize.SMALL)
        RemoteButton(Icons.Default.Stop, "Stop", StabiloPink, onStop, size = RemoteButtonSize.SMALL)
        RemoteButton(Icons.Default.RadioButtonChecked, "Forward", StabiloYellow, onForward, size = RemoteButtonSize.SMALL)
        RemoteButton(Icons.Default.SkipNext, "Next", StabiloCyan, onNext, size = RemoteButtonSize.SMALL)
    }
}

// ==========================================================
// 4. VOLUME BAR (2 tombol terpisah + gesture)
// ==========================================================
@Composable
fun RemoteVolumeBar(
    onVolUpTap: () -> Unit,
    onVolDownTap: () -> Unit,
    onVolUpHold: () -> Unit,
    onVolDownHold: () -> Unit,
    onVolUpRelease: () -> Unit,
    onVolDownRelease: () -> Unit,
    onChannelUp: () -> Unit,
    onChannelDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Volume rocker (2 tombol vertikal)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Vol+
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                    .background(DarkBgCardElevated)
                    .border(1.dp, StabiloLime.copy(alpha = 0.5f), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        HapticHelper.medium(context)
                        onVolUpTap()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VolumeUp, "Vol +", tint = StabiloLime, modifier = Modifier.size(24.dp))
            }

            Box(modifier = Modifier.size(6.dp))

            // Vol-
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(DarkBgCardElevated)
                    .border(1.dp, StabiloLime.copy(alpha = 0.5f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        HapticHelper.medium(context)
                        onVolDownTap()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VolumeDown, "Vol -", tint = StabiloLime, modifier = Modifier.size(24.dp))
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
            Text("Volume", color = TextSecondary, fontSize = 10.sp)
        }

        // Channel rocker (2 tombol vertikal)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                    .background(DarkBgCardElevated)
                    .border(1.dp, StabiloCyan.copy(alpha = 0.5f), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        HapticHelper.medium(context)
                        onChannelUp()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowUp, "CH +", tint = StabiloCyan, modifier = Modifier.size(24.dp))
            }

            Box(modifier = Modifier.size(6.dp))

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .background(DarkBgCardElevated)
                    .border(1.dp, StabiloCyan.copy(alpha = 0.5f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        HapticHelper.medium(context)
                        onChannelDown()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "CH -", tint = StabiloCyan, modifier = Modifier.size(24.dp))
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(4.dp))
            Text("Channel", color = TextSecondary, fontSize = 10.sp)
        }
    }
}

// ==========================================================
// 5. QUICK BAR — switcher mode
// ==========================================================
@Composable
fun RemoteQuickBar(
    activeMode: RemoteMode,
    onModeChanged: (RemoteMode) -> Unit,
    onShortcut: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeButton(RemoteMode.DPAD, activeMode, onModeChanged, Icons.Default.TouchApp, "D-Pad", StabiloLime)
        ModeButton(RemoteMode.GRID, activeMode, onModeChanged, Icons.Default.GridView, "Grid", StabiloYellow)
        ModeButton(RemoteMode.MOUSE, activeMode, onModeChanged, Icons.Default.Mouse, "Mouse", StabiloCyan)
        ModeButton(RemoteMode.GESTURE, activeMode, onModeChanged, Icons.Default.ScreenRotation, "Gest", StabiloYellow)
        ModeButton(RemoteMode.AIR_MOUSE, activeMode, onModeChanged, Icons.Default.Sensors, "Air", StabiloCyan)

        // Shortcut
        RemoteSquareButton(
            icon = Icons.Default.Home,
            label = "Shortcut",
            accentColor = StabiloPink,
            onClick = onShortcut,
            hapticType = 1
        )

        // Exit
        RemoteSquareButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = "Exit",
            accentColor = StabiloPink,
            onClick = onExit,
            hapticType = 2
        )
    }
}

@Composable
private fun ModeButton(
    mode: RemoteMode,
    activeMode: RemoteMode,
    onModeChanged: (RemoteMode) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color
) {
    val context = LocalContext.current
    val isActive = mode == activeMode

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isActive) accentColor else DarkBgCardElevated)
                .border(
                    1.dp,
                    if (isActive) accentColor else accentColor.copy(alpha = 0.4f),
                    CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    HapticHelper.medium(context)
                    onModeChanged(mode)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(3.dp))
        Text(
            text = label,
            color = if (isActive) accentColor else TextSecondary,
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Enum mode remote.
 */
enum class RemoteMode(val label: String) {
    DPAD("D-Pad"),
    GRID("Grid"),
    MOUSE("Mouse"),
    GESTURE("Gesture"),
    AIR_MOUSE("Air Mouse")
}
