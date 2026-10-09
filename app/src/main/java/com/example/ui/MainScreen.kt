package dev.andikuneiocontroll.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.andikuneiocontroll.MainViewModel
import dev.andikuneiocontroll.filemanager.FileManagerHelper
import dev.andikuneiocontroll.model.ActiveViewer
import dev.andikuneiocontroll.model.FileItem
import dev.andikuneiocontroll.remote.discovery.DiscoveredTv
import dev.andikuneiocontroll.remote.protocol.TvCommand
import dev.andikuneiocontroll.ui.components.MobileModeView
import dev.andikuneiocontroll.ui.components.RainbowRemoteIcon
import dev.andikuneiocontroll.ui.components.SettingsDialog
import dev.andikuneiocontroll.ui.components.StabiloTooltipButton
import dev.andikuneiocontroll.ui.filemanager.BatchRenameDialog
import dev.andikuneiocontroll.ui.filemanager.FileContextMenuDialog
import dev.andikuneiocontroll.ui.filemanager.FilePaneView
import dev.andikuneiocontroll.ui.filemanager.RenameDialog
import dev.andikuneiocontroll.ui.filemanager.WifiShareDialog
import dev.andikuneiocontroll.ui.permissions.PermissionHandlerView
import dev.andikuneiocontroll.ui.remote.RemoteTvDialog
import dev.andikuneiocontroll.ui.remote.dialogs.AdbPairingDialog
import dev.andikuneiocontroll.ui.remote.dialogs.InfoTvDialog
import dev.andikuneiocontroll.ui.remote.dialogs.InputSourceDialog
import dev.andikuneiocontroll.ui.remote.dialogs.KeyboardDialog
import dev.andikuneiocontroll.ui.remote.dialogs.ManualIpDialog
import dev.andikuneiocontroll.ui.remote.dialogs.PairingPinDialog
import dev.andikuneiocontroll.ui.remote.dialogs.ShortcutDialog
import dev.andikuneiocontroll.ui.remote.dialogs.TvPickerDialog
import dev.andikuneiocontroll.ui.remote.dialogs.VoiceDialog
import dev.andikuneiocontroll.ui.remote.settings.RemoteSettingsScreen
import dev.andikuneiocontroll.ui.theme.DarkBgCard
import dev.andikuneiocontroll.ui.theme.DarkBgCardElevated
import dev.andikuneiocontroll.ui.theme.DarkBgPrimary
import dev.andikuneiocontroll.ui.theme.DarkDivider
import dev.andikuneiocontroll.ui.theme.StabiloCyan
import dev.andikuneiocontroll.ui.theme.StabiloLime
import dev.andikuneiocontroll.ui.theme.StabiloPink
import dev.andikuneiocontroll.ui.theme.StabiloYellow
import dev.andikuneiocontroll.ui.theme.TextMuted
import dev.andikuneiocontroll.ui.theme.TextPrimary
import dev.andikuneiocontroll.ui.theme.TextSecondary
import dev.andikuneiocontroll.viewers.AudioPlayerDialog
import dev.andikuneiocontroll.viewers.DiskMapDialog
import dev.andikuneiocontroll.viewers.HexViewerDialog
import dev.andikuneiocontroll.viewers.ImageViewerDialog
import dev.andikuneiocontroll.viewers.TextViewerDialog
import dev.andikuneiocontroll.viewers.VaultDialog
import dev.andikuneiocontroll.viewers.VideoPlayerDialog
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val activity = context as? android.app.Activity
    val scope = rememberCoroutineScope()

    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
        )
    }

    if (!hasStoragePermission) {
        PermissionHandlerView(
            onAllGranted = {
                hasStoragePermission = true
                viewModel.loadPane1(FileManagerHelper.getDefaultStoragePath())
                viewModel.loadPane2(FileManagerHelper.getDefaultStoragePath())
            }
        )
        return
    }

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isLeftPaneVisible by remember { mutableStateOf(true) }

    val pane1Path by viewModel.pane1Path.collectAsState()
    val pane1Items by viewModel.pane1Items.collectAsState()
    val pane2Path by viewModel.pane2Path.collectAsState()
    val pane2Items by viewModel.pane2Items.collectAsState()
    val activeViewer by viewModel.activeViewer.collectAsState()
    val serverConfig by viewModel.serverConfig.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()
    val showWifiDialog by viewModel.showWifiShareDialog.collectAsState()
    val storageStats by viewModel.storageCategories.collectAsState()
    val totalStorageBytes by viewModel.totalStorageBytes.collectAsState()

    val discoveredTvs by viewModel.discoveredTvs.collectAsState()
    val savedTvs by viewModel.savedTvs.collectAsState()
    val isScanningTv by viewModel.isScanningTv.collectAsState()

    var contextMenuItem by remember { mutableStateOf<FileItem?>(null) }
    var renameTargetItem by remember { mutableStateOf<FileItem?>(null) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var showLauncherOverlay by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showRemoteDialog by remember { mutableStateOf(false) }
    var showTvPickerDialog by remember { mutableStateOf(false) }
    var showManualIpDialog by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var showAdbPairingDialog by remember { mutableStateOf(false) }
    var showInfoTvDialog by remember { mutableStateOf(false) }
    var showInputSourceDialog by remember { mutableStateOf(false) }
    var showShortcutDialog by remember { mutableStateOf(false) }
    var showRemoteSettings by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showKeyboardDialog by remember { mutableStateOf(false) }

    var pairingTvName by remember { mutableStateOf("") }
    var pairingTv by remember { mutableStateOf<DiscoveredTv?>(null) }
    var pairingError by remember { mutableStateOf("") }
    var isPairingSubmitting by remember { mutableStateOf(false) }

    var adbPairingHost by remember { mutableStateOf("") }
    var adbPairingError by remember { mutableStateOf("") }
    var isAdbPairingSubmitting by remember { mutableStateOf(false) }

    var voiceStatus by remember { mutableStateOf("") }
    var isVoiceSending by remember { mutableStateOf(false) }
    var keyboardStatus by remember { mutableStateOf("") }
    var isKeyboardSending by remember { mutableStateOf(false) }

    val pane1SelectedCount = remember(pane1Items) { pane1Items.count { it.isSelected } }
    val pane2SelectedCount = remember(pane2Items) { pane2Items.count { it.isSelected } }
    val anySelected = pane1SelectedCount > 0 || pane2SelectedCount > 0
    val isServerRunning = serverConfig.isRunning

    LaunchedEffect(showTvPickerDialog) {
        if (showTvPickerDialog) {
            viewModel.scanTvs()
        }
    }

    val connectToTvWithPairingCheck: (DiscoveredTv) -> Unit = { tv ->
        pairingTvName = tv.displayName
        pairingTv = tv
        isPairingSubmitting = true
        viewModel.connectToTv(tv, "") { success, message ->
            isPairingSubmitting = false
            if (success) {
                showTvPickerDialog = false
                showManualIpDialog = false
                showPairingDialog = false
                showAdbPairingDialog = false
                showRemoteDialog = true
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } else if (message.contains("PAIRING_NEEDED", ignoreCase = true)) {
                showTvPickerDialog = false
                showManualIpDialog = false
                adbPairingHost = tv.ip
                adbPairingError = ""
                showAdbPairingDialog = true
            } else if (message.contains("PIN", ignoreCase = true)) {
                showTvPickerDialog = false
                showManualIpDialog = false
                showPairingDialog = true
            } else {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = DarkBgPrimary,
    bottomBar = {
        if (anySelected) {
            val isPane1 = pane1SelectedCount > 0
            StabiloMultiSelectionBar(
                selectedCount = if (isPane1) pane1SelectedCount else pane2SelectedCount,
                onCopy = { viewModel.copyToOppositePane(fromPane1 = isPane1, isMove = false) },
                onMove = { viewModel.copyToOppositePane(fromPane1 = isPane1, isMove = true) },
                onWifiShare = { viewModel.shareSelectedOverWifi(fromPane1 = isPane1) },
                onCompress = { viewModel.compressSelected(fromPane1 = isPane1) },
                onVault = { viewModel.setViewer(ActiveViewer.Vault) },
                onDelete = { viewModel.deleteSelected(fromPane1 = isPane1) },
                onBatchRename = { showBatchRenameDialog = true },
                onClear = {
                    if (isPane1) viewModel.clearSelectPane1() else viewModel.clearSelectPane2()
                }
            )
        }
    }
) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        if (isLandscape) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        FilePaneView(
                            paneTitle = "PANE KIRI • PENYIMPANAN INTERNAL",
                            currentPath = pane1Path,
                            items = pane1Items,
                            selectedCount = pane1SelectedCount,
                            onNavigate = { viewModel.loadPane1(it) },
                            onNavigateUp = { viewModel.navigateUpPane1() },
                            onItemClick = { viewModel.openFile(it) },
                            onItemLongClick = { contextMenuItem = it },
                            onToggleSelect = { viewModel.toggleSelectPane1(it) },
                            onSelectAll = { viewModel.selectAllPane1() },
                            onClearSelection = { viewModel.clearSelectPane1() },
                            onSwitchStorage = { label ->
                                when (label) {
                                    "Penyimpanan Internal" -> viewModel.loadPane1(FileManagerHelper.getDefaultStoragePath())
                                    "Aplikasi (APK)" -> viewModel.loadPane1("APPLICATIONS")
                                    else -> viewModel.setViewer(ActiveViewer.Vault)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        modifier = Modifier
                            .width(60.dp)
                            .fillMaxHeight()
                            .background(DarkBgCard)
                            .border(width = 1.dp, color = DarkDivider),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        StabiloTooltipButton(
                            icon = Icons.Default.Settings,
                            tooltip = "Pengaturan",
                            tint = StabiloCyan,
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.padding(top = 12.dp),
                            buttonSize = 42.dp,
                            iconSize = 26.dp
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            StabiloTooltipButton(
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                tooltip = "D-Pad Kanan",
                                tint = StabiloLime,
                                onClick = { viewModel.sendTvCommand(TvCommand.DPAD_RIGHT) },
                                buttonSize = 42.dp,
                                iconSize = 26.dp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            StabiloTooltipButton(
                                icon = Icons.Default.FullscreenExit,
                                tooltip = "Mode HP",
                                tint = StabiloYellow,
                                onClick = {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    Toast.makeText(context, "Beralih ke Mode HP", Toast.LENGTH_SHORT).show()
                                },
                                buttonSize = 42.dp,
                                iconSize = 26.dp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            StabiloTooltipButton(
                                icon = Icons.Default.Wifi,
                                tooltip = "Server WiFi",
                                tint = if (isServerRunning) Color(0xFF22C55E) else StabiloLime,
                                onClick = { viewModel.setShowWifiShareDialog(true) },
                                buttonSize = 40.dp,
                                iconSize = 20.dp
                            )
                        }

                        RainbowRemoteIcon(
                            onClick = {
                                if (viewModel.remoteController.isConnected()) {
                                    showRemoteDialog = true
                                } else {
                                    showTvPickerDialog = true
                                }
                            },
                            modifier = Modifier.padding(bottom = 14.dp),
                            buttonSize = 42.dp,
                            iconSize = 26.dp,
                            tooltip = "Remote TV"
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        FilePaneView(
                            paneTitle = "PANE KANAN • PENYIMPANAN INTERNAL",
                            currentPath = pane2Path,
                            items = pane2Items,
                            selectedCount = pane2SelectedCount,
                            onNavigate = { viewModel.loadPane2(it) },
                            onNavigateUp = { viewModel.navigateUpPane2() },
                            onItemClick = { viewModel.openFile(it) },
                            onItemLongClick = { contextMenuItem = it },
                            onToggleSelect = { viewModel.toggleSelectPane2(it) },
                            onSelectAll = { viewModel.selectAllPane2() },
                            onClearSelection = { viewModel.clearSelectPane2() },
                            onSwitchStorage = { label ->
                                when (label) {
                                    "Penyimpanan Internal" -> viewModel.loadPane2(FileManagerHelper.getDefaultStoragePath())
                                    "Aplikasi (APK)" -> viewModel.loadPane2("APPLICATIONS")
                                    else -> viewModel.setViewer(ActiveViewer.Vault)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = DarkBgCardElevated,
                        border = BorderStroke(1.5.dp, StabiloLime),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { showLauncherOverlay = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = "Launcher",
                                tint = StabiloLime,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        } else {
            MobileModeView(
                isLeftPaneVisible = isLeftPaneVisible,
                onTogglePane = { isLeftPaneVisible = !isLeftPaneVisible },
                onOpenSettings = { showSettingsDialog = true },
                onOpenWifiServer = { viewModel.setShowWifiShareDialog(true) },
                onOpenRemote = {
                    if (viewModel.remoteController.isConnected()) {
                        showRemoteDialog = true
                    } else {
                        showTvPickerDialog = true
                    }
                },
                isConnected = isServerRunning,
                paneContent = {
                    if (isLeftPaneVisible) {
                        FilePaneView(
                            paneTitle = "PANE KIRI (MODE HP) • FILE MANAGER",
                            currentPath = pane1Path,
                            items = pane1Items,
                            selectedCount = pane1SelectedCount,
                            onNavigate = { viewModel.loadPane1(it) },
                            onNavigateUp = { viewModel.navigateUpPane1() },
                            onItemClick = { viewModel.openFile(it) },
                            onItemLongClick = { contextMenuItem = it },
                            onToggleSelect = { viewModel.toggleSelectPane1(it) },
                            onSelectAll = { viewModel.selectAllPane1() },
                            onClearSelection = { viewModel.clearSelectPane1() },
                            onSwitchStorage = { label ->
                                when (label) {
                                    "Penyimpanan Internal" -> viewModel.loadPane1(FileManagerHelper.getDefaultStoragePath())
                                    "Aplikasi (APK)" -> viewModel.loadPane1("APPLICATIONS")
                                    else -> viewModel.setViewer(ActiveViewer.Vault)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        FilePaneView(
                            paneTitle = "PANE KANAN (MODE HP) • FILE MANAGER",
                            currentPath = pane2Path,
                            items = pane2Items,
                            selectedCount = pane2SelectedCount,
                            onNavigate = { viewModel.loadPane2(it) },
                            onNavigateUp = { viewModel.navigateUpPane2() },
                            onItemClick = { viewModel.openFile(it) },
                            onItemLongClick = { contextMenuItem = it },
                            onToggleSelect = { viewModel.toggleSelectPane2(it) },
                            onSelectAll = { viewModel.selectAllPane2() },
                            onClearSelection = { viewModel.clearSelectPane2() },
                            onSwitchStorage = { label ->
                                when (label) {
                                    "Penyimpanan Internal" -> viewModel.loadPane2(FileManagerHelper.getDefaultStoragePath())
                                    "Aplikasi (APK)" -> viewModel.loadPane2("APPLICATIONS")
                                    else -> viewModel.setViewer(ActiveViewer.Vault)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            )
        }
    }
    }

    // ==========================================================
// SEMUA DIALOG & OVERLAY
// ==========================================================

if (showSettingsDialog) {
    SettingsDialog(
        context = context,
        onDismiss = { showSettingsDialog = false }
    )
}

if (showRemoteDialog) {
    RemoteTvDialog(
        remoteController = viewModel.remoteController,
        onDismiss = { showRemoteDialog = false },
        onOpenSettings = { showRemoteSettings = true },
        onOpenTvList = {
            showRemoteDialog = false
            showTvPickerDialog = true
        },
        onOpenInputSource = { showInputSourceDialog = true },
        onOpenKeyboard = { showKeyboardDialog = true },
        onOpenCast = {
            Toast.makeText(context, "Screen Cast akan segera hadir", Toast.LENGTH_SHORT).show()
        },
        onOpenShortcut = { showShortcutDialog = true },
        onOpenInfoTv = { showInfoTvDialog = true },
        onOpenVoice = { showVoiceDialog = true },
        onOpenCopy = {
            Toast.makeText(context, "Copy Text dari TV belum tersedia", Toast.LENGTH_SHORT).show()
        }
    )
}

if (showRemoteSettings) {
    RemoteSettingsScreen(
        onDismiss = { showRemoteSettings = false }
    )
}

if (showTvPickerDialog) {
    TvPickerDialog(
        discoveredTvs = discoveredTvs,
        savedTvs = savedTvs,
        isScanning = isScanningTv,
        onRefresh = { viewModel.scanTvs() },
        onSelectDiscovered = { tv -> connectToTvWithPairingCheck(tv) },
        onSelectSaved = { tv ->
            showTvPickerDialog = false
            pairingTvName = tv.displayName
            isPairingSubmitting = true
            viewModel.connectToSavedTv(tv) { success, message ->
                isPairingSubmitting = false
                if (success) {
                    showRemoteDialog = true
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                } else {
                    val discovered = DiscoveredTv(
                        deviceId = tv.deviceId,
                        name = tv.displayName,
                        ip = tv.ipAddress,
                        port = tv.port,
                        brand = tv.brand,
                        protocol = tv.protocol,
                        hasChromecast = tv.hasChromecast
                    )
                    pairingTv = discovered
                    if (message.contains("PAIRING_NEEDED", ignoreCase = true)) {
                        adbPairingHost = tv.ipAddress
                        adbPairingError = ""
                        showAdbPairingDialog = true
                    } else {
                        showPairingDialog = true
                    }
                }
            }
        },
        onDeleteSaved = { tv -> viewModel.forgetTv(tv) },
        onManualIp = {
            showTvPickerDialog = false
            showManualIpDialog = true
        },
        onDismiss = { showTvPickerDialog = false }
    )
}

if (showManualIpDialog) {
    ManualIpDialog(
        onSubmit = { ip, port ->
            showManualIpDialog = false
            val manualTv = DiscoveredTv(
                deviceId = "manual_${ip}_$port",
                name = "TV Manual ($ip)",
                ip = ip,
                port = port,
                brand = "ANDROID_TV",
                protocol = "ANDROID_TV_V2"
            )
            connectToTvWithPairingCheck(manualTv)
        },
        onDismiss = { showManualIpDialog = false }
    )
}

if (showAdbPairingDialog) {
    AdbPairingDialog(
        tvName = pairingTvName,
        defaultHost = adbPairingHost,
        isSubmitting = isAdbPairingSubmitting,
        errorMessage = adbPairingError,
        onPair = { host, port, code ->
            isAdbPairingSubmitting = true
            adbPairingError = ""
            scope.launch {
                val result = viewModel.remoteController.pair(host, port, code)
                isAdbPairingSubmitting = false
                if (result.first) {
                    showAdbPairingDialog = false
                    val tv = pairingTv
                    if (tv != null) {
                        connectToTvWithPairingCheck(tv)
                    }
                } else {
                    adbPairingError = result.second
                }
            }
        },
        onCancel = {
            showAdbPairingDialog = false
            adbPairingError = ""
        }
    )
}

if (showPairingDialog) {
    PairingPinDialog(
        tvName = pairingTvName,
        isSubmitting = isPairingSubmitting,
        errorMessage = pairingError,
        onSubmit = { pin ->
            val tv = pairingTv
            if (tv != null) {
                isPairingSubmitting = true
                pairingError = ""
                viewModel.connectToTv(tv, pin) { success, message ->
                    isPairingSubmitting = false
                    if (success) {
                        showPairingDialog = false
                        showTvPickerDialog = false
                        showRemoteDialog = true
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    } else {
                        pairingError = message
                    }
                }
            }
        },
        onCancel = {
            showPairingDialog = false
            pairingError = ""
            pairingTv = null
        }
    )
}

if (showInfoTvDialog) {
    val state = viewModel.remoteController.connectionState.collectAsState().value
    val savedTv = savedTvs.firstOrNull { it.deviceId == state.tv?.deviceId }
    InfoTvDialog(
        tvName = state.tv?.displayName ?: "",
        brand = state.tv?.brand ?: "UNKNOWN",
        protocolName = state.protocolName,
        ipAddress = state.tv?.ip ?: "",
        port = state.tv?.port ?: 0,
        isConnected = state.isConnected,
        hasChromecast = state.tv?.hasChromecast == true || (savedTv?.hasChromecast == true),
        firmwareVersion = savedTv?.firmwareVersion ?: "",
        onReconnect = {
            showInfoTvDialog = false
            val tv = state.tv
            if (tv != null) {
                connectToTvWithPairingCheck(tv)
            }
        },
        onForget = {
            showInfoTvDialog = false
            val state2 = viewModel.remoteController.connectionState.value
            val tvId = state2.tv?.deviceId ?: ""
            val found = savedTvs.firstOrNull { it.deviceId == tvId }
            if (found != null) viewModel.forgetTv(found)
        },
        onDismiss = { showInfoTvDialog = false }
    )
}

if (showInputSourceDialog) {
    InputSourceDialog(
        onSelect = { cmd ->
            showInputSourceDialog = false
            viewModel.sendTvCommand(cmd)
        },
        onDismiss = { showInputSourceDialog = false }
    )
}

if (showShortcutDialog) {
    val shortcuts = listOf(
        "YouTube" to "com.google.android.youtube.tv",
        "Netflix" to "com.netflix.ninja",
        "Prime Video" to "com.amazon.amazonvideo.livingroom",
        "Disney+" to "com.disney.disneyplus",
        "Spotify" to "com.spotify.tv.android",
        "VLC" to "org.videolan.vlc",
        "Plex" to "com.plexapp.android",
        "Chrome" to "com.android.chrome"
    )
    ShortcutDialog(
        shortcuts = shortcuts,
        onLaunch = { _, pkg ->
            showShortcutDialog = false
            viewModel.sendTvCommand(TvCommand.LAUNCH_APP, pkg)
        },
        onDismiss = { showShortcutDialog = false }
    )
}

if (showVoiceDialog) {
    VoiceDialog(
        isSending = isVoiceSending,
        statusMessage = voiceStatus,
        onResult = { text ->
            isVoiceSending = true
            voiceStatus = "Mengirim: \"$text\""
            scope.launch {
                viewModel.sendTvCommand(TvCommand.INPUT_TEXT, text)
                isVoiceSending = false
                voiceStatus = "Terkirim: \"$text\""
                kotlinx.coroutines.delay(1500)
                showVoiceDialog = false
                voiceStatus = ""
            }
        },
        onDismiss = {
            showVoiceDialog = false
            voiceStatus = ""
        }
    )
}

if (showKeyboardDialog) {
    KeyboardDialog(
        isSending = isKeyboardSending,
        statusMessage = keyboardStatus,
        onSendText = { text ->
            isKeyboardSending = true
            keyboardStatus = "Mengirim…"
            scope.launch {
                viewModel.sendTvCommand(TvCommand.INPUT_TEXT, text)
                isKeyboardSending = false
                keyboardStatus = "Terkirim"
            }
        },
        onBackspace = {
            scope.launch {
                viewModel.sendTvCommand(TvCommand.KEY_DELETE)
            }
        },
        onEnter = {
            scope.launch {
                viewModel.sendTvCommand(TvCommand.KEY_ENTER)
            }
        },
        onDismiss = {
            showKeyboardDialog = false
            keyboardStatus = ""
        }
    )
}

    if (showLauncherOverlay) {
        StabiloLauncherDialog(
            onOpenApps = { viewModel.loadPane1("APPLICATIONS"); showLauncherOverlay = false },
            onOpenDiskMap = { viewModel.showDiskMap(); showLauncherOverlay = false },
            onOpenWifiShare = { viewModel.setShowWifiShareDialog(true); showLauncherOverlay = false },
            onDismiss = { showLauncherOverlay = false }
        )
    }

    if (showWifiDialog) {
        WifiShareDialog(
            serverConfig = serverConfig,
            serverUrl = serverUrl,
            discoveredPeers = discoveredPeers,
            onToggleServer = { config -> viewModel.toggleServer(config) },
            onConnectPeer = { peer ->
                viewModel.loadPane2("http://${peer.ip}:${peer.port}")
                viewModel.setShowWifiShareDialog(false)
                Toast.makeText(context, "Terhubung ke ${peer.name.ifBlank { peer.ip }}", Toast.LENGTH_SHORT).show()
            },
            onSavePeer = { _, _, _, _ -> },
            onDismiss = { viewModel.setShowWifiShareDialog(false) }
        )
    }

    contextMenuItem?.let { item ->
        FileContextMenuDialog(
            item = item,
            onView = { viewModel.openFile(item) },
            onCopy = { viewModel.copyToOppositePane(fromPane1 = true, isMove = false) },
            onMove = { viewModel.copyToOppositePane(fromPane1 = true, isMove = true) },
            onDelete = { viewModel.deleteSelected(fromPane1 = true) },
            onCompress = { viewModel.compressSelected(fromPane1 = true) },
            onExtract = { viewModel.extractArchive(item) },
            onRename = { renameTargetItem = item },
            onShare = { FileManagerHelper.shareFiles(context, listOf(java.io.File(item.path))) },
            onHexView = { viewModel.openHexViewer(item) },
            onDismiss = { contextMenuItem = null }
        )
    }

    renameTargetItem?.let { item ->
        RenameDialog(
            initialName = item.name,
            onRename = { newName -> viewModel.renameFileItem(item, newName) },
            onDismiss = { renameTargetItem = null }
        )
    }

    if (showBatchRenameDialog) {
        BatchRenameDialog(
            fileCount = pane1SelectedCount,
            onBatchRename = { prefix, suffix -> viewModel.batchRenameSelected(prefix, suffix) },
            onDismiss = { showBatchRenameDialog = false }
        )
    }

    when (val viewer = activeViewer) {
        is ActiveViewer.Video -> VideoPlayerDialog(viewer.path, viewer.name, onDismiss = { viewModel.closeViewer() })
        is ActiveViewer.Image -> ImageViewerDialog(viewer.path, viewer.name, viewer.imageList, onDismiss = { viewModel.closeViewer() })
        is ActiveViewer.Audio -> AudioPlayerDialog(viewer.path, viewer.name, onDismiss = { viewModel.closeViewer() })
        is ActiveViewer.Text -> TextViewerDialog(viewer.name, viewer.text, onDismiss = { viewModel.closeViewer() })
        is ActiveViewer.Hex -> HexViewerDialog(viewer.name, viewer.hexPreview, onDismiss = { viewModel.closeViewer() })
        is ActiveViewer.DiskMap -> DiskMapDialog(storageStats, totalStorageBytes, onDismiss = { viewModel.closeViewer() })
        is ActiveViewer.Vault -> VaultDialog(
            onEncrypt = { pwd -> viewModel.encryptSelectedToVault(pwd) },
            onDecrypt = { pwd -> contextMenuItem?.let { viewModel.decryptVaultFile(it.path, pwd) } },
            isDecryptMode = contextMenuItem?.path?.endsWith(".vault") == true,
            onDismiss = { viewModel.closeViewer() }
        )
        ActiveViewer.None -> {}
        else -> {}
    }
}

@Composable
fun ConnectionStatusDot(isConnected: Boolean, onClick: () -> Unit) {
    val dotColor = if (isConnected) Color(0xFF22C55E) else TextMuted
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(DarkBgCardElevated)
            .border(1.dp, dotColor.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
    }
}

@Composable
fun StabiloMultiSelectionBar(
    selectedCount: Int,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onWifiShare: () -> Unit,
    onCompress: () -> Unit,
    onVault: () -> Unit,
    onDelete: () -> Unit,
    onBatchRename: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBgCardElevated),
        border = BorderStroke(1.dp, StabiloLime.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCopy,
                colors = ButtonDefaults.buttonColors(containerColor = StabiloLime),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ContentCopy, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Salin", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = onMove,
                colors = ButtonDefaults.buttonColors(containerColor = StabiloCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.DriveFileMove, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pindah", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = onWifiShare,
                colors = ButtonDefaults.buttonColors(containerColor = StabiloYellow),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Wifi, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WiFi", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            IconButton(onClick = onCompress) {
                Icon(Icons.Default.Archive, "Zip", tint = StabiloCyan)
            }
            IconButton(onClick = onVault) {
                Icon(Icons.Default.Lock, "Vault", tint = StabiloYellow)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Hapus", tint = StabiloPink)
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, "Batal", tint = TextSecondary)
            }
        }
    }
}

@Composable
fun StabiloLauncherDialog(
    onOpenApps: () -> Unit,
    onOpenDiskMap: () -> Unit,
    onOpenWifiShare: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = BorderStroke(1.dp, StabiloLime.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Launcher Overlay iOControll",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = StabiloLime
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onOpenApps,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                    border = BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Apps, null, tint = StabiloCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Jelajahi Aplikasi (Ekstrak APK)", color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOpenDiskMap,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                    border = BorderStroke(1.dp, StabiloYellow.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.PieChart, null, tint = StabiloYellow)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Peta Penggunaan Memori (Disk Map)", color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOpenWifiShare,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                    border = BorderStroke(1.dp, StabiloLime.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Wifi, null, tint = StabiloLime)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Server Berkas Wi-Fi", color = TextPrimary)
                }
            }
        }
    }
}
