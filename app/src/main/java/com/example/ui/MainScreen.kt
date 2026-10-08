package com.example.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Environment
import android.widget.Toast
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
import androidx.compose.material.icons.filled.SettingsRemote
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
import com.example.MainViewModel
import com.example.filemanager.FileManagerHelper
import com.example.model.ActiveViewer
import com.example.model.FileItem
import com.example.ui.filemanager.BatchRenameDialog
import com.example.ui.filemanager.FileContextMenuDialog
import com.example.ui.filemanager.FilePaneView
import com.example.ui.filemanager.RenameDialog
import com.example.ui.filemanager.WifiShareDialog
import com.example.ui.permissions.PermissionHandlerView
import com.example.ui.remote.ConnectionPaneView
import com.example.ui.theme.DarkBgCard
import com.example.ui.theme.DarkBgCardElevated
import com.example.ui.theme.DarkBgPrimary
import com.example.ui.theme.DarkDivider
import com.example.ui.theme.StabiloCyan
import com.example.ui.theme.StabiloLime
import com.example.ui.theme.StabiloPink
import com.example.ui.theme.StabiloYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewers.AudioPlayerDialog
import com.example.viewers.DiskMapDialog
import com.example.viewers.HexViewerDialog
import com.example.viewers.ImageViewerDialog
import com.example.viewers.TextViewerDialog
import com.example.viewers.VaultDialog
import com.example.viewers.VideoPlayerDialog

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current

    // Cek izin penyimpanan awal
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

    // Logika responsif dengan LocalConfiguration.current.orientation sesuai spesifikasi
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // State untuk Mode HP (Portrait): kontrol tampilan Pane Kiri vs Pane Kanan
    var isLeftPaneVisible by remember { mutableStateOf(true) }

    // Observables dari ViewModel
    val pane1Path by viewModel.pane1Path.collectAsState()
    val pane1Items by viewModel.pane1Items.collectAsState()
    val activeViewer by viewModel.activeViewer.collectAsState()
    val serverConfig by viewModel.serverConfig.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()
    val showWifiDialog by viewModel.showWifiShareDialog.collectAsState()
    val storageStats by viewModel.storageCategories.collectAsState()
    val totalStorageBytes by viewModel.totalStorageBytes.collectAsState()

    // Dialog state
    var contextMenuItem by remember { mutableStateOf<FileItem?>(null) }
    var renameTargetItem by remember { mutableStateOf<FileItem?>(null) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var showLauncherOverlay by remember { mutableStateOf(false) }

    val pane1SelectedCount = remember(pane1Items) { pane1Items.count { it.isSelected } }
    val anySelected = pane1SelectedCount > 0

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBgPrimary,
        bottomBar = {
            if (anySelected) {
                StabiloMultiSelectionBar(
                    selectedCount = pane1SelectedCount,
                    onCopy = { viewModel.copyToOppositePane(fromPane1 = true, isMove = false) },
                    onMove = { viewModel.copyToOppositePane(fromPane1 = true, isMove = true) },
                    onWifiShare = { viewModel.shareSelectedOverWifi(fromPane1 = true) },
                    onCompress = { viewModel.compressSelected(fromPane1 = true) },
                    onVault = { viewModel.setViewer(ActiveViewer.Vault) },
                    onDelete = { viewModel.deleteSelected(fromPane1 = true) },
                    onBatchRename = { showBatchRenameDialog = true },
                    onClear = { viewModel.clearSelectPane1() }
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
                // =========================================================================
                // --- A. MODE TV (LANDSCAPE) - TAMPILAN 2 PANE ---
                // Mengikuti struktur kode & hierarki Jetpack Compose sesuai spesifikasi
                // =========================================================================
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // 1. Pane Kiri (Penyimpanan Internal, Folder, File, Foto, Video)
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

                        // 2. Toolbar Tengah (Garis Pembatas Vertikal Fisik di Tengah)
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
                                onClick = {
                                    viewModel.setShowWifiShareDialog(true)
                                    Toast.makeText(context, "Pengaturan & Wi-Fi Sharing", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.padding(top = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Pengaturan",
                                    tint = StabiloCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            // Area Tengah: Panah D-Pad & Tombol Fullscreen
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Panah D-Pad
                                IconButton(
                                    onClick = {
                                        viewModel.remoteClient.sendCommand("DPAD_RIGHT")
                                        Toast.makeText(context, "Navigasi D-Pad", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Panah D-Pad",
                                        tint = StabiloLime,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Tombol Fullscreen (Rotasi ke Portrait jika diperlukan)
                                IconButton(
                                    onClick = {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        Toast.makeText(context, "Kembali ke Mode Portrait (HP)", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.FullscreenExit,
                                        contentDescription = "Fullscreen Toggle",
                                        tint = StabiloYellow,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // Icon Remote (Bawah)
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Remote TV Aktif di Pane Kanan", Toast.LENGTH_SHORT).show()
                                },
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

                        // 3. Pane Kanan (Tampilkan status koneksi Tahap I / Tahap II)
                        Box(modifier = Modifier.weight(1f)) {
                            ConnectionPaneView(
                                remoteClient = viewModel.remoteClient,
                                remoteServer = viewModel.remoteServer,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // 4. Overlay Icon Launcher di sudut kanan atas
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DarkBgCardElevated,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, StabiloLime),
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
                // =========================================================================
                // --- B. MODE HP (PORTRAIT) - TAMPILAN 1 PANE ---
                // Sesuai skema: 1 Pane ditampilkan bergantian, Toolbar di sisi kanan
                // =========================================================================
                Row(modifier = Modifier.fillMaxSize()) {
                    // 1. Area Konten Pane (Hanya 1 yang ditampilkan berdasarkan state isLeftPaneVisible)
                    Box(modifier = Modifier.weight(1f)) {
                        if (isLeftPaneVisible) {
                            // SKENARIO 1: TAMPILAN PANE KIRI (Penyimpanan Internal, Folder, File)
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
                            // SKENARIO 2: TAMPILAN PANE KANAN (Status Koneksi Tahap I / Tahap II)
                            ConnectionPaneView(
                                remoteClient = viewModel.remoteClient,
                                remoteServer = viewModel.remoteServer,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // 2. Toolbar Kanan (Dropdown / Vertical Bar di Sisi Kanan)
                    Column(
                        modifier = Modifier
                            .width(50.dp)
                            .fillMaxHeight()
                            .background(DarkBgCard)
                            .border(width = 1.dp, color = DarkDivider),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // [Icon Gerigi]
                        IconButton(
                            onClick = {
                                viewModel.setShowWifiShareDialog(true)
                                Toast.makeText(context, "Buka Pengaturan Wi-Fi", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                tint = StabiloCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Tombol Switch Pane: Mengubah state isLeftPaneVisible (Panah D-Pad / Switch)
                        IconButton(
                            onClick = {
                                isLeftPaneVisible = !isLeftPaneVisible
                                val label = if (isLeftPaneVisible) "Beralih ke Pane Kiri (Penyimpanan)" else "Beralih ke Pane Kanan (Status Koneksi)"
                                Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Switch Pane",
                                tint = StabiloLime,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // [Tombol Fullscreen] (Paksa Rotasi ke TV / Landscape)
                        IconButton(
                            onClick = {
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                Toast.makeText(context, "Memaksa Rotasi ke Mode TV (Landscape)", Toast.LENGTH_SHORT).show()
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

                        // [Icon Remote]
                        IconButton(
                            onClick = {
                                isLeftPaneVisible = false // Buka Pane Kanan (Remote & Koneksi)
                                Toast.makeText(context, "Membuka Panel Remote TV", Toast.LENGTH_SHORT).show()
                            }
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
    // OVERLAYS & DIALOGS SISTEM
    // ==========================================

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
            onSavePeer = { peer, label, pwd, path ->
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

    // Active Viewers Dialogs
    when (val viewer = activeViewer) {
        is ActiveViewer.Video -> {
            VideoPlayerDialog(
                filePath = viewer.path,
                fileName = viewer.name,
                onDismiss = { viewModel.closeViewer() }
            )
        }
        is ActiveViewer.Image -> {
            ImageViewerDialog(
                filePath = viewer.path,
                fileName = viewer.name,
                allImages = viewer.imageList,
                onDismiss = { viewModel.closeViewer() }
            )
        }
        is ActiveViewer.Audio -> {
            AudioPlayerDialog(
                filePath = viewer.path,
                fileName = viewer.name,
                onDismiss = { viewModel.closeViewer() }
            )
        }
        is ActiveViewer.Text -> {
            TextViewerDialog(
                fileName = viewer.name,
                content = viewer.text,
                onDismiss = { viewModel.closeViewer() }
            )
        }
        is ActiveViewer.Hex -> {
            HexViewerDialog(
                fileName = viewer.name,
                hexContent = viewer.hexPreview,
                onDismiss = { viewModel.closeViewer() }
            )
        }
        is ActiveViewer.DiskMap -> {
            DiskMapDialog(
                categoryStats = storageStats,
                totalSizeBytes = totalStorageBytes,
                onDismiss = { viewModel.closeViewer() }
            )
        }
        is ActiveViewer.Vault -> {
            VaultDialog(
                onEncrypt = { pwd -> viewModel.encryptSelectedToVault(pwd) },
                onDecrypt = { pwd ->
                    contextMenuItem?.let { viewModel.decryptVaultFile(it.path, pwd) }
                },
                isDecryptMode = contextMenuItem?.path?.endsWith(".vault") == true,
                onDismiss = { viewModel.closeViewer() }
            )
        }
        ActiveViewer.None -> {}
        else -> {}
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
        border = androidx.compose.foundation.BorderStroke(1.dp, StabiloLime.copy(alpha = 0.5f)),
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
                Text("WiFi Share", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            border = androidx.compose.foundation.BorderStroke(1.dp, StabiloLime.copy(alpha = 0.5f)),
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, StabiloCyan.copy(alpha = 0.4f)),
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, StabiloYellow.copy(alpha = 0.4f)),
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, StabiloLime.copy(alpha = 0.4f)),
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
