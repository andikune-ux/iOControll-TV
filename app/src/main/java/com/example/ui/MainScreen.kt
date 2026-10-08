package dev.andikuneiocontroll.ui

import android.app.Activity
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dev.andikuneiocontroll.ui.filemanager.BatchRenameDialog
import dev.andikuneiocontroll.ui.filemanager.FileContextMenuDialog
import dev.andikuneiocontroll.ui.filemanager.FilePaneView
import dev.andikuneiocontroll.ui.filemanager.RenameDialog
import dev.andikuneiocontroll.ui.filemanager.WifiShareDialog
import dev.andikuneiocontroll.ui.permissions.PermissionHandlerView
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

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current

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

    // State untuk Mode HP: default = Pane Kiri
    var isLeftPaneVisible by remember { mutableStateOf(true) }

    // Observables
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

    var contextMenuItem by remember { mutableStateOf<FileItem?>(null) }
    var renameTargetItem by remember { mutableStateOf<FileItem?>(null) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var showLauncherOverlay by remember { mutableStateOf(false) }
    var showConnectionDialog by remember { mutableStateOf(false) }

    val pane1SelectedCount = remember(pane1Items) { pane1Items.count { it.isSelected } }
    val pane2SelectedCount = remember(pane2Items) { pane2Items.count { it.isSelected } }
    val anySelected = pane1SelectedCount > 0 || pane2SelectedCount > 0
    val isServerRunning = serverConfig.isRunning

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
                // ============ MODE TV (LANDSCAPE) - 2 PANE FILE MANAGER ============
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Pane Kiri
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

                        // Toolbar Tengah (Garis Pembatas Vertikal)
                        Column(
                            modifier = Modifier
                                .width(60.dp)
                                .fillMaxHeight()
                                .background(DarkBgCard)
                                .border(width = 1.dp, color = DarkDivider),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Icon Gerigi (Atas)
                            IconButton(
                                onClick = { viewModel.setShowWifiShareDialog(true) },
                                modifier = Modifier.padding(top = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Pengaturan",
                                    tint = StabiloCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Tengah: D-Pad, Fullscreen, Status Koneksi
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        viewModel.remoteClient.sendCommand("DPAD_RIGHT")
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Panah D-Pad",
                                        tint = StabiloLime,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                IconButton(
                                    onClick = {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.FullscreenExit,
                                        contentDescription = "Fullscreen Toggle",
                                        tint = StabiloYellow,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Indikator Status Koneksi (Dot)
                                ConnectionStatusDot(
                                    isConnected = isServerRunning,
                                    onClick = { showConnectionDialog = true }
                                )
                            }

                            // Icon Remote (Bawah)
                            IconButton(
                                onClick = { showConnectionDialog = true },
                                modifier = Modifier.padding(bottom = 14.dp)
                            ) {
                                Icon(
                                    Icons.Default.Tv,
                                    contentDescription = "Remote TV",
                                    tint = StabiloPink,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // Pane Kanan (File Manager - pane2)
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

                    // Overlay Icon Launcher di sudut kanan atas
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
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
                                    contentDescription = "Launcher Overlay",
                                    tint = StabiloLime,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // ============ MODE HP (PORTRAIT) - 1 PANE FILE MANAGER ============
                Row(modifier = Modifier.fillMaxSize()) {
                    // Area Konten Pane
                    Box(modifier = Modifier.weight(1f)) {
                        if (isLeftPaneVisible) {
                            // Pane Kiri
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
                            // Pane Kanan
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

                    // Toolbar Kanan (Dropdown)
                    Column(
                        modifier = Modifier
                            .width(50.dp)
                            .fillMaxHeight()
                            .background(DarkBgCard)
                            .border(width = 1.dp, color = DarkDivider),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Icon Gerigi
                        IconButton(
                            onClick = { viewModel.setShowWifiShareDialog(true) }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                tint = StabiloCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Switch Pane
                        IconButton(
                            onClick = { isLeftPaneVisible = !isLeftPaneVisible }
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Switch Pane",
                                tint = StabiloLime,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Fullscreen (Paksa Landscape)
                        IconButton(
                            onClick = {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Paksa Rotasi ke TV",
                                tint = StabiloYellow,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Indikator Status Koneksi
                        ConnectionStatusDot(
                            isConnected = isServerRunning,
                            onClick = { showConnectionDialog = true }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Remote Icon
                        IconButton(
                            onClick = { showConnectionDialog = true }
                        ) {
                            Icon(
                                Icons.Default.Tv,
                                contentDescription = "Remote Control",
                                tint = StabiloPink,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // OVERLAYS & DIALOGS
    // ==========================================

    // Dialog Status Koneksi (Simple)
    if (showConnectionDialog) {
        ConnectionInfoDialog(
            isConnected = isServerRunning,
            serverUrl = serverUrl,
            peerCount = discoveredPeers.size,
            onOpenRemote = {
                showConnectionDialog = false
                Toast.makeText(context, "Fitur Remote TV akan tersedia di update berikutnya", Toast.LENGTH_SHORT).show()
            },
            onOpenWifiSettings = {
                showConnectionDialog = false
                viewModel.setShowWifiShareDialog(true)
            },
            onDismiss = { showConnectionDialog = false }
        )
    }

    // Launcher Overlay Dialog
    if (showLauncherOverlay) {
        StabiloLauncherDialog(
            onOpenApps = {
                viewModel.loadPane1("APPLICATIONS")
                showLauncherOverlay = false
            },
            onOpenDiskMap = {
                viewModel.showDiskMap()
                showLauncherOverlay = false
            },
            onOpenWifiShare = {
                viewModel.setShowWifiShareDialog(true)
                showLauncherOverlay = false
            },
            onDismiss = { showLauncherOverlay = false }
        )
    }

    // Wi-Fi File Sharing Dialog
    if (showWifiDialog) {
        WifiShareDialog(
            serverConfig = serverConfig,
            serverUrl = serverUrl,
            discoveredPeers = discoveredPeers,
            onToggleServer = { viewModel.toggleServer(it) },
            onConnectPeer = { peer ->
                viewModel.loadPane2("http://${peer.ip}:${peer.port}")
                viewModel.setShowWifiShareDialog(false)
            },
            onSavePeer = { _, label, _, _ ->
                Toast.makeText(context, "Perangkat $label tersimpan", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { viewModel.setShowWifiShareDialog(false) }
        )
    }

    // Context Menu Dialog
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

    // Rename Dialog
    renameTargetItem?.let { item ->
        RenameDialog(
            initialName = item.name,
            onRename = { newName -> viewModel.renameFileItem(item, newName) },
            onDismiss = { renameTargetItem = null }
        )
    }

    // Batch Rename Dialog
    if (showBatchRenameDialog) {
        BatchRenameDialog(
            fileCount = pane1SelectedCount,
            onBatchRename = { prefix, suffix -> viewModel.batchRenameSelected(prefix, suffix) },
            onDismiss = { showBatchRenameDialog = false }
        )
    }

    // Active Viewers
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

// ==========================================
// COMPONENT: Status Koneksi Dot (Kecil)
// ==========================================
@Composable
fun ConnectionStatusDot(
    isConnected: Boolean,
    onClick: () -> Unit
) {
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
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}

// ==========================================
// DIALOG: Info Status Koneksi
// ==========================================
@Composable
fun ConnectionInfoDialog(
    isConnected: Boolean,
    serverUrl: String,
    peerCount: Int,
    onOpenRemote: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBgCard),
            border = BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Status Koneksi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = StabiloCyan
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF22C55E) else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (isConnected) "Server Aktif" else "Belum Terhubung",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isConnected && serverUrl.isNotEmpty()) {
                    Text("URL: $serverUrl", color = TextSecondary, fontSize = 12.sp)
                }
                Text("Perangkat ditemukan: $peerCount", color = TextSecondary, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onOpenWifiSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = StabiloLime),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Server WiFi", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onOpenRemote,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBgCardElevated),
                    border = BorderStroke(1.dp, StabiloPink.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = StabiloPink)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remote TV", color = TextPrimary)
                }
            }
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBgCardElevated),
        border = BorderStroke(1.dp, StabiloLime.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCopy,
                colors = ButtonDefaults.buttonColors(containerColor = StabiloLime),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Salin", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = onMove,
                colors = ButtonDefaults.buttonColors(containerColor = StabiloCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pindah", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = onWifiShare,
                colors = ButtonDefaults.buttonColors(containerColor = StabiloYellow),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WiFi", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            IconButton(onClick = onCompress) {
                Icon(Icons.Default.Archive, contentDescription = "Zip", tint = StabiloCyan)
            }

            IconButton(onClick = onVault) {
                Icon(Icons.Default.Lock, contentDescription = "Vault", tint = StabiloYellow)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = StabiloPink)
            }

            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Batal", tint = TextSecondary)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
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
                    Icon(Icons.Default.Apps, contentDescription = null, tint = StabiloCyan)
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
                    Icon(Icons.Default.PieChart, contentDescription = null, tint = StabiloYellow)
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
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = StabiloLime)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Server Berkas Wi-Fi (X-plore Style)", color = TextPrimary)
                }
            }
        }
    }
}
