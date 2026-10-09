package dev.andikuneiocontroll.ui.remote

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikuneiocontroll.remote.controller.RemoteController
import dev.andikuneiocontroll.remote.protocol.TvCommand
import dev.andikuneiocontroll.ui.remote.components.RemoteAirMouse
import dev.andikuneiocontroll.ui.remote.components.RemoteDPad
import dev.andikuneiocontroll.ui.remote.components.RemoteGesture
import dev.andikuneiocontroll.ui.remote.components.RemoteGrid
import dev.andikuneiocontroll.ui.remote.components.RemoteHeader
import dev.andikuneiocontroll.ui.remote.components.RemoteMediaBar
import dev.andikuneiocontroll.ui.remote.components.RemoteMode
import dev.andikuneiocontroll.ui.remote.components.RemoteMouse
import dev.andikuneiocontroll.ui.remote.components.RemoteNavBar
import dev.andikuneiocontroll.ui.remote.components.RemoteQuickBar
import dev.andikuneiocontroll.ui.remote.components.RemoteTopBar
import dev.andikuneiocontroll.ui.remote.components.RemoteVolumeBar
import dev.andikuneiocontroll.ui.theme.DarkBgPrimary
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.TextPrimary
import kotlinx.coroutines.launch

/**
 * RemoteTvDialog — Container utama UI remote.
 *
 * Layout:
 * - Header (Back, Nama TV, Settings, Power)
 * - TopBar (Voice, Input, Cast, Kbd, Copy, TV)
 * - Area Kontrol (berubah sesuai mode: D-Pad / Grid / Mouse / Gesture / Air)
 * - NavBar (Home, Back, Recent, Mute)
 * - MediaBar (Play, Pause, Rew, Fwd, dll)
 * - VolumeBar (Vol+, Vol-, Ch+, Ch-)
 * - QuickBar (mode switcher + Shortcut + Exit)
 *
 * Volume Monitor Overlay muncul saat tombol Vol ditahan + gesture.
 */
@Composable
fun RemoteTvDialog(
    remoteController: RemoteController,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenTvList: () -> Unit = {},
    onOpenInputSource: () -> Unit = {},
    onOpenKeyboard: () -> Unit = {},
    onOpenCast: () -> Unit = {},
    onOpenShortcut: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val connectionState by remoteController.connectionState.collectAsState()

    // State
    var currentMode by remember { mutableStateOf(RemoteMode.DPAD) }
    var volumeMonitorVisible by remember { mutableStateOf(false) }
    var volumeLevel by remember { mutableStateOf(50) }  // 0-100

    // Paksa orientasi Portrait saat dialog dibuka
    DisposableEffect(Unit) {
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = original ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Helper: kirim command
    fun send(cmd: String, payload: String = "") {
        scope.launch { remoteController.sendCommand(cmd, payload) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBgPrimary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                RemoteHeader(
                    tvName = connectionState.tv?.displayName ?: "Belum Terhubung",
                    protocolName = connectionState.protocolName,
                    isConnected = connectionState.isConnected,
                    latencyMs = 0,
                    onBack = onDismiss,
                    onSettings = onOpenSettings,
                    onPower = { send(TvCommand.POWER) }
                )

                // Top Bar
                RemoteTopBar(
                    onVoice = { send(TvCommand.VOICE_START) },
                    onInput = onOpenInputSource,
                    onCast = onOpenCast,
                    onKeyboard = onOpenKeyboard,
                    onCopy = { /* TODO copy */ },
                    onTvList = onOpenTvList
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ============ AREA KONTROL (per mode) ============
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (currentMode) {
                        RemoteMode.DPAD -> RemoteDPad(
                            size = 200.dp,
                            onUp = { send(TvCommand.DPAD_UP) },
                            onDown = { send(TvCommand.DPAD_DOWN) },
                            onLeft = { send(TvCommand.DPAD_LEFT) },
                            onRight = { send(TvCommand.DPAD_RIGHT) },
                            onOk = { send(TvCommand.DPAD_OK) }
                        )

                        RemoteMode.GRID -> RemoteGrid(
                            onNumber = { n -> send("NUM_$n") },
                            onBackspace = { send(TvCommand.KEY_DELETE) },
                            onEnter = { send(TvCommand.KEY_ENTER) },
                            onColor = { c ->
                                val cmd = when (c) {
                                    "R" -> TvCommand.COLOR_RED
                                    "G" -> TvCommand.COLOR_GREEN
                                    "Y" -> TvCommand.COLOR_YELLOW
                                    else -> TvCommand.COLOR_BLUE
                                }
                                send(cmd)
                            },
                            onBackToRemote = { currentMode = RemoteMode.DPAD }
                        )

                        RemoteMode.MOUSE -> RemoteMouse(
                            onMove = { dx, dy -> send(TvCommand.MOUSE_MOVE, "$dx|$dy") },
                            onClick = { send(TvCommand.MOUSE_CLICK) },
                            onDoubleClick = { send(TvCommand.MOUSE_CLICK) }
                        )

                        RemoteMode.GESTURE -> RemoteGesture(
                            onUp = { send(TvCommand.DPAD_UP) },
                            onDown = { send(TvCommand.DPAD_DOWN) },
                            onLeft = { send(TvCommand.DPAD_LEFT) },
                            onRight = { send(TvCommand.DPAD_RIGHT) },
                            onTap = { send(TvCommand.DPAD_OK) }
                        )

                        RemoteMode.AIR_MOUSE -> RemoteAirMouse(
                            onMove = { dx, dy -> send(TvCommand.MOUSE_MOVE, "$dx|$dy") },
                            onClick = { send(TvCommand.MOUSE_CLICK) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nav Bar (Home, Back, Recent, Mute)
                RemoteNavBar(
                    onHome = { send(TvCommand.HOME) },
                    onBack = { send(TvCommand.BACK) },
                    onRecent = { send(TvCommand.RECENTS) },
                    onMute = { send(TvCommand.VOLUME_MUTE) }
                )

                // Media Bar
                RemoteMediaBar(
                    onPlay = { send(TvCommand.PLAY) },
                    onPause = { send(TvCommand.PAUSE) },
                    onStop = { send(TvCommand.STOP) },
                    onRewind = { send(TvCommand.REWIND) },
                    onForward = { send(TvCommand.FORWARD) },
                    onPrev = { send(TvCommand.PREVIOUS) },
                    onNext = { send(TvCommand.NEXT) }
                )

                // Volume + Channel Bar
                RemoteVolumeBar(
                    onVolUpTap = {
                        send(TvCommand.VOLUME_UP)
                        volumeLevel = (volumeLevel + 3).coerceAtMost(100)
                    },
                    onVolDownTap = {
                        send(TvCommand.VOLUME_DOWN)
                        volumeLevel = (volumeLevel - 3).coerceAtLeast(0)
                    },
                    onVolUpHold = { volumeMonitorVisible = true },
                    onVolDownHold = { volumeMonitorVisible = true },
                    onVolUpRelease = { volumeMonitorVisible = false },
                    onVolDownRelease = { volumeMonitorVisible = false },
                    onChannelUp = { send(TvCommand.CHANNEL_UP) },
                    onChannelDown = { send(TvCommand.CHANNEL_DOWN) }
                )

                // Quick Bar (mode switcher + shortcut + exit)
                RemoteQuickBar(
                    activeMode = currentMode,
                    onModeChanged = { currentMode = it },
                    onShortcut = onOpenShortcut,
                    onExit = onDismiss
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Volume Monitor Overlay (muncul saat vol ditahan)
            AnimatedVisibility(
                visible = volumeMonitorVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                VolumeMonitorOverlay(
                    level = volumeLevel,
                    onLevelChange = { newLevel ->
                        volumeLevel = newLevel
                        val cmd = if (newLevel > volumeLevel) {
                            TvCommand.VOLUME_UP
                        } else {
                            TvCommand.VOLUME_DOWN
                        }
                        send(cmd)
                    }
                )
            }
        }
    }
}

/**
 * VolumeMonitorOverlay — overlay monitor volume di tengah layar.
 * Muncul saat tombol Vol ditahan & user swipe.
 */
@Composable
private fun VolumeMonitorOverlay(
    level: Int,
    onLevelChange: (Int) -> Unit
) {
    var accumulatedDx by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { accumulatedDx = 0f },
                    onDragEnd = { accumulatedDx = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDx += dragAmount.x
                        // Setiap 10px gerakan = 1% volume
                        val delta = (accumulatedDx / 10f).toInt()
                        if (delta != 0) {
                            val newLevel = (level + delta).coerceIn(0, 100)
                            if (newLevel != level) {
                                onLevelChange(newLevel)
                                accumulatedDx = 0f
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkBgPrimary)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔊",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "VOLUME",
                color = StabiloCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$level%",
                color = StabiloLime,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(level / 100f)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(StabiloLime)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "← Geser kiri = turun\n→ Geser kanan = naik",
                color = TextPrimary.copy(alpha = 0.6f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
