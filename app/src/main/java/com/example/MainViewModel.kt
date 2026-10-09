package dev.andikuneiocontroll

import android.app.Application
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.andikuneiocontroll.data.local.PrefsRepository
import dev.andikuneiocontroll.data.local.TvEntity
import dev.andikuneiocontroll.data.local.TvRepository
import dev.andikuneiocontroll.filemanager.FileManagerHelper
import dev.andikuneiocontroll.model.ActiveViewer
import dev.andikuneiocontroll.model.DevicePeer
import dev.andikuneiocontroll.model.FileCategory
import dev.andikuneiocontroll.model.FileItem
import dev.andikuneiocontroll.model.ServerConfig
import dev.andikuneiocontroll.model.StorageCategoryInfo
import dev.andikuneiocontroll.remote.RemoteClient
import dev.andikuneiocontroll.remote.RemoteSocketServer
import dev.andikuneiocontroll.remote.controller.RemoteController
import dev.andikuneiocontroll.remote.discovery.DiscoveredTv
import dev.andikuneiocontroll.remote.discovery.TvDiscoveryManager
import dev.andikuneiocontroll.server.DiscoveryManager
import dev.andikuneiocontroll.server.WifiFileServerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // ==========================================================
    // FILE MANAGER STATE (tidak berubah)
    // ==========================================================

    private val _pane1Path = MutableStateFlow(FileManagerHelper.getDefaultStoragePath())
    val pane1Path: StateFlow<String> = _pane1Path.asStateFlow()

    private val _pane1Items = MutableStateFlow<List<FileItem>>(emptyList())
    val pane1Items: StateFlow<List<FileItem>> = _pane1Items.asStateFlow()

    private val _pane2Path = MutableStateFlow(FileManagerHelper.getDefaultStoragePath())
    val pane2Path: StateFlow<String> = _pane2Path.asStateFlow()

    private val _pane2Items = MutableStateFlow<List<FileItem>>(emptyList())
    val pane2Items: StateFlow<List<FileItem>> = _pane2Items.asStateFlow()

    private val _activePaneIndex = MutableStateFlow(0)
    val activePaneIndex: StateFlow<Int> = _activePaneIndex.asStateFlow()

    private val _activeViewer = MutableStateFlow<ActiveViewer>(ActiveViewer.None)
    val activeViewer: StateFlow<ActiveViewer> = _activeViewer.asStateFlow()

    private val _serverConfig = MutableStateFlow(ServerConfig())
    val serverConfig: StateFlow<ServerConfig> = _serverConfig.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    val discoveryManager = DiscoveryManager(context, viewModelScope)
    val discoveredPeers: StateFlow<List<DevicePeer>> = discoveryManager.discoveredPeers

    // === Sistem lama (masih dipakai MainScreen versi lama) ===
    val remoteClient = RemoteClient(context, viewModelScope)
    val remoteServer = RemoteSocketServer(context, viewModelScope)

    // ==========================================================
    // REMOTE TV BARU
    // ==========================================================

    /** Controller utama untuk kontrol TV (6 protokol). */
    val remoteController = RemoteController(context)

    /** Discovery mDNS + SSDP untuk deteksi TV. */
    val tvDiscoveryManager = TvDiscoveryManager(context, viewModelScope)

    /** Room database untuk simpan TV terdaftar. */
    val tvRepository = TvRepository(context)

    /** DataStore untuk pengaturan. */
    val prefsRepository = PrefsRepository(context)

    // State daftar TV tersimpan (auto-update dari Room)
    private val _savedTvs = MutableStateFlow<List<TvEntity>>(emptyList())
    val savedTvs: StateFlow<List<TvEntity>> = _savedTvs.asStateFlow()

    // State daftar TV ditemukan (via discovery)
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = tvDiscoveryManager.discoveredTvs

    // State loading scan
    val isScanningTv: StateFlow<Boolean> = tvDiscoveryManager.isScanning

    // ==========================================================
    // OTHER STATE
    // ==========================================================

    private val _storageCategories = MutableStateFlow<List<StorageCategoryInfo>>(emptyList())
    val storageCategories: StateFlow<List<StorageCategoryInfo>> = _storageCategories.asStateFlow()

    private val _totalStorageBytes = MutableStateFlow(1L)
    val totalStorageBytes: StateFlow<Long> = _totalStorageBytes.asStateFlow()

    private val _showWifiShareDialog = MutableStateFlow(false)
    val showWifiShareDialog: StateFlow<Boolean> = _showWifiShareDialog.asStateFlow()

    private val _rightPaneMode = MutableStateFlow(0)
    val rightPaneMode: StateFlow<Int> = _rightPaneMode.asStateFlow()

    // ==========================================================
    // INIT
    // ==========================================================

    init {
        loadPane1(FileManagerHelper.getDefaultStoragePath())
        loadPane2(FileManagerHelper.getDefaultStoragePath())
        discoveryManager.startDiscovery(23016)
        remoteServer.startServer(23017)

        // Observe background server state
        viewModelScope.launch {
            WifiFileServerService.serverState.collect { s ->
                _serverConfig.value = _serverConfig.value.copy(isRunning = s.isRunning, port = s.port)
                _serverUrl.value = s.url
            }
        }

        // Observe daftar TV tersimpan (Room)
        viewModelScope.launch {
            tvRepository.getAllTvs().collect { list ->
                _savedTvs.value = list
            }
        }

        // Auto-start scan TV saat ViewModel dibuat
        tvDiscoveryManager.startScan(5000L)
    }

    // ==========================================================
    // REMOTE TV METHODS
    // ==========================================================

    /** Scan TV secara manual (dipanggil dari tombol). */
    fun scanTvs() {
        tvDiscoveryManager.startScan(5000L)
    }

    /** Stop scan TV. */
    fun stopScanTvs() {
        tvDiscoveryManager.stopScan()
    }

    /**
     * Connect ke TV yang ditemukan.
     * Setelah sukses, simpan ke Room.
     */
    fun connectToTv(
        tv: DiscoveredTv,
        pairingCode: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            remoteController.connect(tv, pairingCode) { success, message ->
                if (success) {
                    // Simpan ke Room
                    viewModelScope.launch {
                        tvRepository.saveOrUpdateFromDiscovery(
                            deviceId = tv.deviceId,
                            originalName = tv.name,
                            ipAddress = tv.ip,
                            port = tv.port,
                            brand = tv.brand,
                            protocol = tv.protocol,
                            modelName = tv.modelName
                        )
                    }
                    onResult(true, message)
                } else {
                    onResult(false, message)
                }
            }
        }
    }

    /** Connect ke TV tersimpan (dari Room). */
    fun connectToSavedTv(
        tv: TvEntity,
        onResult: (Boolean, String) -> Unit
    ) {
        val discovered = DiscoveredTv(
            deviceId = tv.deviceId,
            name = tv.displayName,
            ip = tv.ipAddress,
            port = tv.port,
            brand = tv.brand,
            protocol = tv.protocol,
            modelName = tv.modelName
        )
        connectToTv(discovered, onResult = onResult)
    }

    /** Disconnect dari TV. */
    fun disconnectTv() {
        viewModelScope.launch {
            remoteController.disconnect()
        }
    }

    /** Hapus TV dari Room. */
    fun forgetTv(tv: TvEntity) {
        viewModelScope.launch {
            tvRepository.deleteTv(tv)
            Toast.makeText(context, "TV \"${tv.displayName}\" dilupakan", Toast.LENGTH_SHORT).show()
        }
    }

    /** Kirim command ke TV. */
    fun sendTvCommand(command: String, payload: String = "") {
        viewModelScope.launch {
            remoteController.sendCommand(command, payload)
        }
    }

    // ==========================================================
    // FILE MANAGER METHODS (tidak berubah)
    // ==========================================================

    fun loadPane1(path: String) {
        viewModelScope.launch {
            _pane1Path.value = path
            val items = if (path == "APPLICATIONS") {
                FileManagerHelper.listInstalledApps(context)
            } else {
                FileManagerHelper.listFiles(path)
            }
            _pane1Items.value = items
        }
    }

    fun loadPane2(path: String) {
        viewModelScope.launch {
            _pane2Path.value = path
            val items = if (path == "APPLICATIONS") {
                FileManagerHelper.listInstalledApps(context)
            } else {
                FileManagerHelper.listFiles(path)
            }
            _pane2Items.value = items
        }
    }

    fun navigateUpPane1() {
        val current = File(_pane1Path.value)
        current.parentFile?.let { parent ->
            if (parent.exists() && parent.canRead()) {
                loadPane1(parent.absolutePath)
            }
        }
    }

    fun navigateUpPane2() {
        val current = File(_pane2Path.value)
        current.parentFile?.let { parent ->
            if (parent.exists() && parent.canRead()) {
                loadPane2(parent.absolutePath)
            }
        }
    }

    fun toggleSelectPane1(item: FileItem) {
        _pane1Items.value = _pane1Items.value.map {
            if (it.path == item.path) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun toggleSelectPane2(item: FileItem) {
        _pane2Items.value = _pane2Items.value.map {
            if (it.path == item.path) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun selectAllPane1() {
        _pane1Items.value = _pane1Items.value.map { it.copy(isSelected = true) }
    }

    fun clearSelectPane1() {
        _pane1Items.value = _pane1Items.value.map { it.copy(isSelected = false) }
    }

    fun selectAllPane2() {
        _pane2Items.value = _pane2Items.value.map { it.copy(isSelected = true) }
    }

    fun clearSelectPane2() {
        _pane2Items.value = _pane2Items.value.map { it.copy(isSelected = false) }
    }

    fun switchActivePane() {
        _activePaneIndex.value = if (_activePaneIndex.value == 0) 1 else 0
    }

    fun toggleRightPaneMode() {
        _rightPaneMode.value = if (_rightPaneMode.value == 0) 1 else 0
    }

    fun setShowWifiShareDialog(show: Boolean) {
        _showWifiShareDialog.value = show
    }

    fun setViewer(viewer: ActiveViewer) {
        _activeViewer.value = viewer
    }

    fun closeViewer() {
        _activeViewer.value = ActiveViewer.None
    }

    fun copyToOppositePane(fromPane1: Boolean, isMove: Boolean = false) {
        viewModelScope.launch {
            val selectedFiles = if (fromPane1) {
                _pane1Items.value.filter { it.isSelected }.map { File(it.path) }
            } else {
                _pane2Items.value.filter { it.isSelected }.map { File(it.path) }
            }

            if (selectedFiles.isEmpty()) {
                Toast.makeText(context, "Pilih setidaknya satu berkas terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val targetDirPath = if (fromPane1) _pane2Path.value else _pane1Path.value
            val targetDir = File(targetDirPath)

            if (!targetDir.exists() || !targetDir.isDirectory) {
                Toast.makeText(context, "Folder penerima tidak ada, Coba lagi", Toast.LENGTH_LONG).show()
                return@launch
            }

            val ok = FileManagerHelper.copyOrMove(selectedFiles, targetDir, isMove)
            if (ok) {
                Toast.makeText(
                    context,
                    if (isMove) "Berhasil dipindahkan ke ${targetDir.name}" else "Berhasil disalin ke ${targetDir.name}",
                    Toast.LENGTH_SHORT
                ).show()
                loadPane1(_pane1Path.value)
                loadPane2(_pane2Path.value)
            } else {
                Toast.makeText(context, "Gagal menyalin/memindahkan beberapa berkas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareSelectedOverWifi(fromPane1: Boolean) {
        val selected = if (fromPane1) {
            _pane1Items.value.filter { it.isSelected }.take(100).map { it.path }
        } else {
            _pane2Items.value.filter { it.isSelected }.take(100).map { it.path }
        }

        if (selected.isEmpty()) {
            Toast.makeText(context, "Pilih file terlebih dahulu (maksimal 100)", Toast.LENGTH_SHORT).show()
            return
        }

        val config = _serverConfig.value.copy(
            selectedFiles = selected,
            isReadOnly = true,
            isRunning = true
        )
        toggleServer(config)
        _showWifiShareDialog.value = true
        Toast.makeText(context, "Membagikan ${selected.size} berkas via Wi-Fi (Read-Only)", Toast.LENGTH_SHORT).show()
    }

    fun toggleServer(config: ServerConfig) {
        val intent = Intent(context, WifiFileServerService::class.java).apply {
            action = if (config.isRunning) WifiFileServerService.ACTION_START else WifiFileServerService.ACTION_STOP
            putExtra(WifiFileServerService.EXTRA_PORT, config.port)
            putExtra(WifiFileServerService.EXTRA_READ_ONLY, config.isReadOnly)
            putExtra(WifiFileServerService.EXTRA_PASSWORD, config.password)
            putStringArrayListExtra(WifiFileServerService.EXTRA_SELECTED_FILES, ArrayList(config.selectedFiles))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun openFile(item: FileItem) {
        if (item.isDirectory) {
            loadPane1(item.path)
            return
        }
        val file = File(item.path)
        when (item.category) {
            FileCategory.VIDEO -> _activeViewer.value = ActiveViewer.Video(item.path, item.name)
            FileCategory.IMAGE -> {
                val imageList = _pane1Items.value.filter { it.category == FileCategory.IMAGE }.map { it.path }
                _activeViewer.value = ActiveViewer.Image(item.path, item.name, imageList)
            }
            FileCategory.AUDIO -> _activeViewer.value = ActiveViewer.Audio(item.path, item.name)
            FileCategory.ARCHIVE -> {
                if (item.path.endsWith(".vault")) {
                    _activeViewer.value = ActiveViewer.Vault
                } else {
                    loadPane1(item.path)
                }
            }
            FileCategory.DOCUMENT, FileCategory.CODE -> {
                viewModelScope.launch {
                    val text = FileManagerHelper.readText(file)
                    _activeViewer.value = ActiveViewer.Text(item.path, item.name, text)
                }
            }
            else -> {
                if (item.path.endsWith(".vault")) {
                    _activeViewer.value = ActiveViewer.Vault
                } else {
                    openHexViewer(item)
                }
            }
        }
    }

    fun openHexViewer(item: FileItem) {
        viewModelScope.launch {
            val hex = FileManagerHelper.readHex(File(item.path))
            _activeViewer.value = ActiveViewer.Hex(item.path, item.name, hex)
        }
    }

    fun showDiskMap() {
        viewModelScope.launch {
            val root = File(FileManagerHelper.getDefaultStoragePath())
            val categories = FileManagerHelper.analyzeStorage(root)
            _storageCategories.value = categories
            _totalStorageBytes.value = categories.sumOf { it.sizeBytes }.coerceAtLeast(1L)
            _activeViewer.value = ActiveViewer.DiskMap
        }
    }

    fun deleteSelected(fromPane1: Boolean) {
        viewModelScope.launch {
            val toDelete = if (fromPane1) {
                _pane1Items.value.filter { it.isSelected }.map { File(it.path) }
            } else {
                _pane2Items.value.filter { it.isSelected }.map { File(it.path) }
            }
            FileManagerHelper.deleteFiles(toDelete)
            Toast.makeText(context, "${toDelete.size} berkas dihapus", Toast.LENGTH_SHORT).show()
            loadPane1(_pane1Path.value)
            loadPane2(_pane2Path.value)
        }
    }

    fun compressSelected(fromPane1: Boolean) {
        viewModelScope.launch {
            val toCompress = if (fromPane1) {
                _pane1Items.value.filter { it.isSelected }.map { File(it.path) }
            } else {
                _pane2Items.value.filter { it.isSelected }.map { File(it.path) }
            }
            if (toCompress.isEmpty()) return@launch

            val parent = toCompress.first().parentFile ?: File(FileManagerHelper.getDefaultStoragePath())
            val destZip = File(parent, "Arsip_${System.currentTimeMillis()}.zip")
            val ok = FileManagerHelper.compressToZip(toCompress, destZip)
            if (ok) {
                Toast.makeText(context, "Berhasil dikompres ke ${destZip.name}", Toast.LENGTH_SHORT).show()
                loadPane1(_pane1Path.value)
                loadPane2(_pane2Path.value)
            }
        }
    }

    fun extractArchive(fileItem: FileItem) {
        viewModelScope.launch {
            val archive = File(fileItem.path)
            val destDir = File(archive.parentFile, archive.nameWithoutExtension)
            val ok = FileManagerHelper.extractZip(archive, destDir)
            if (ok) {
                Toast.makeText(context, "Ekstrak berhasil ke ${destDir.name}", Toast.LENGTH_SHORT).show()
                loadPane1(_pane1Path.value)
                loadPane2(_pane2Path.value)
            } else {
                Toast.makeText(context, "Gagal mengekstrak arsip", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun createFolderInActivePane(name: String) {
        viewModelScope.launch {
            val parent = File(_pane1Path.value)
            FileManagerHelper.createFolder(parent, name)
            loadPane1(_pane1Path.value)
        }
    }

    fun renameFileItem(item: FileItem, newName: String) {
        viewModelScope.launch {
            val target = File(item.path)
            FileManagerHelper.renameFile(target, newName)
            loadPane1(_pane1Path.value)
            loadPane2(_pane2Path.value)
        }
    }

    fun batchRenameSelected(prefix: String, suffix: String) {
        viewModelScope.launch {
            val selected = _pane1Items.value.filter { it.isSelected }.map { File(it.path) }
            val count = FileManagerHelper.batchRename(selected, prefix, suffix)
            Toast.makeText(context, "$count berkas diubah namanya", Toast.LENGTH_SHORT).show()
            loadPane1(_pane1Path.value)
        }
    }

    fun encryptSelectedToVault(password: String) {
        viewModelScope.launch {
            val selected = _pane1Items.value.filter { it.isSelected }.map { File(it.path) }
            if (selected.isEmpty()) return@launch

            val parent = selected.first().parentFile ?: File(FileManagerHelper.getDefaultStoragePath())
            val vaultFile = File(parent, "Vault_${System.currentTimeMillis()}.vault")
            val ok = FileManagerHelper.encryptToVault(selected, vaultFile, password)
            if (ok) {
                Toast.makeText(context, "Berkas dienkripsi ke ${vaultFile.name}", Toast.LENGTH_LONG).show()
                loadPane1(_pane1Path.value)
            } else {
                Toast.makeText(context, "Gagal mengenkripsi vault", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun decryptVaultFile(vaultPath: String, password: String) {
        viewModelScope.launch {
            val vaultFile = File(vaultPath)
            val targetDir = File(vaultFile.parentFile, vaultFile.nameWithoutExtension)
            val ok = FileManagerHelper.decryptFromVault(vaultFile, targetDir, password)
            if (ok) {
                Toast.makeText(context, "Vault dibuka kuncinya ke ${targetDir.name}", Toast.LENGTH_LONG).show()
                loadPane1(_pane1Path.value)
            } else {
                Toast.makeText(context, "Kata sandi salah atau vault rusak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        discoveryManager.stopDiscovery()
        remoteClient.disconnect()
        remoteServer.stopServer()
        tvDiscoveryManager.destroy()
    }
}
