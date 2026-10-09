package dev.andikuneiocontroll.data

/**
 * Memory Knowledge & Instruction — iOControll Tv
 * WAJIB update setiap build kalau ada fitur/bug/perubahan baru.
 */
object AppKnowledge {

    const val APP_NAME = "iOControll Tv"
    const val PACKAGE_NAME = "dev.andikuneiocontroll"
    const val REPO_URL = "https://github.com/andikune-ux/iOControll-TV"
    const val REPO_ACTIONS = "https://github.com/andikune-ux/iOControll-TV/actions"
    const val CURRENT_VERSION = "V1.00.002"
    const val WIFI_SERVER_PORT = 23016
    const val TV_REMOTE_PORT = 6467
    const val ADB_PORT = 5555

    // ==========================================
    // BAGIAN 2 — PENGATURAN USER (template statis)
    // ==========================================
    val SETTINGS_MENU: List<String> = listOf(
        "1.  Server WiFi (Transfer File)",
        "2.  Remote TV (6 Protokol)",
        "3.  Pengaturan Remote (haptic, sound, sensitivitas)",
        "4.  Backup Aman",
        "5.  Tentang Aplikasi"
    )

    // ==========================================
    // BAGIAN 4 — MEMORY KNOWLEDGE & INSTRUCTION
    // ==========================================
    val MEMORY_KNOWLEDGE: List<String> = listOf(
        "=== BAGIAN 1 — ATURAN FORMAT RESPON ===",
        "1. Visualisasi folder (emoji + indentasi)",
        "2. Path lengkap file",
        "3. Nama file",
        "4. URL edit / URL new",
        "5. Kode timpa full",
        "6. Pesan commit",
        "",
        "=== BAGIAN 2 — ATURAN ANTI-TRUNCATION ===",
        "Kalau kode kepanjangan:",
        "1. Judul: 'BAGIAN 1 DARI N'",
        "2. Akhir: '(lanjut di BAGIAN berikutnya)'",
        "3. Awal: '(sambungan dari BAGIAN sebelumnya)'",
        "4. JANGAN potong di tengah fungsi",
        "",
        "=== BAGIAN 3 — ATURAN KERJA ===",
        "1. Konfirmasi dulu sebelum eksekusi",
        "2. Jujur kalau belum tahu",
        "3. JANGAN hapus fitur lama tanpa izin",
        "4. Lihat kode asli dulu sebelum timpa",
        "5. Kerjakan per batch",
        "6. JANGAN buka file sama 2x",
        "",
        "=== BAGIAN 4 — ATURAN SIGNATURE / KEYSTORE (WAJIB) ===",
        "JANGAN PERNAH ubah file-file ini:",
        "1. debug.keystore.base64 (root repo)",
        "2. Blok signingConfigs di app/build.gradle.kts",
        "3. Bagian keystore di .github/workflows/build.yml",
        "",
        "ATURAN INSTALL APK:",
        "- SELALU install dari GitHub Actions Artifacts",
        "- JANGAN install APK dari AI Studio",
        "- Alasan: signature beda -> Android tolak -> bentrok",
        "",
        "=== BAGIAN 5 — PRINSIP UTAMA ===",
        "1. KERJAKAN PER BATCH",
        "2. KONFIRMASI SEBELUM LANJUT",
        "3. JANGAN ASUMSI - LIHAT KODE ASLI DULU",
        "4. JANGAN HAPUS FITUR LAMA TANPA IZIN"
    )

    // ==========================================
    // BAGIAN 5 — STRUKTUR FOLDER APLIKASI
    // ==========================================
    val FOLDER_STRUCTURE: List<String> = listOf(
        "app/src/main/java/dev/andikuneiocontroll/",
        "├── MainActivity.kt          (flow Splash → Onboarding → Main)",
        "├── MainViewModel.kt         (viewmodel utama)",
        "├── data/local/              (Room + DataStore)",
        "│   ├── TvEntity.kt          (entity TV terdaftar)",
        "│   ├── TvDao.kt             (query database)",
        "│   ├── TvDatabase.kt        (instance Room)",
        "│   ├── TvRepository.kt      (wrapper akses)",
        "│   └── PrefsRepository.kt   (DataStore pengaturan)",
        "├── data/                    (knowledge & history)",
        "│   ├── AppKnowledge.kt",
        "│   └── UpdateHistory.kt",
        "├── filemanager/             (helper file)",
        "│   └── FileManagerHelper.kt",
        "├── model/",
        "│   └── Models.kt            (data class)",
        "├── remote/",
        "│   ├── controller/",
        "│   │   ├── RemoteController.kt      (facade 6 protokol)",
        "│   │   └── RemoteControllers.kt     (Voice, Keyboard, Mouse, Cast, Shortcut)",
        "│   ├── discovery/",
        "│   │   ├── DiscoveredTv.kt",
        "│   │   ├── MdnsDiscovery.kt",
        "│   │   ├── SsdpDiscovery.kt",
        "│   │   ├── TvDiscoveryManager.kt",
        "│   │   └── TvFilter.kt",
        "│   └── protocol/",
        "│       ├── TvProtocol.kt            (interface)",
        "│       ├── ProtocolDetector.kt",
        "│       ├── adb/                     (ADB Wi-Fi)",
        "│       │   ├── AdbProtocol.kt",
        "│       │   ├── AdbCrypto.kt",
        "│       │   ├── AdbClient.kt",
        "│       │   └── AdbTvClient.kt",
        "│       ├── roku/RokuEcpClient.kt",
        "│       ├── samsung/SamsungTizenClient.kt",
        "│       ├── lg/LgWebOsClient.kt",
        "│       ├── philips/PhilipsClient.kt",
        "│       └── vizio/VizioClient.kt",
        "├── server/                  (WiFi file server)",
        "│   ├── WifiFileServerService.kt",
        "│   ├── WifiHttpServer.kt",
        "│   └── DiscoveryManager.kt",
        "├── ui/",
        "│   ├── MainScreen.kt",
        "│   ├── SplashAndOnboarding.kt",
        "│   ├── remote/",
        "│   │   ├── RemoteTvDialog.kt        (container UI remote)",
        "│   │   ├── components/",
        "│   │   │   ├── HapticHelper.kt",
        "│   │   │   ├── RemoteButton.kt",
        "│   │   │   ├── RemoteHeader.kt",
        "│   │   │   ├── RemoteDPad.kt",
        "│   │   │   ├── RemoteBars.kt        (TopBar, NavBar, MediaBar, VolumeBar, QuickBar)",
        "│   │   │   └── RemoteModes.kt       (Grid, Mouse, Gesture, AirMouse)",
        "│   │   ├── dialogs/TvDialogs.kt     (Picker, Pairing, Info, Input, Shortcut, ManualIp)",
        "│   │   └── settings/RemoteSettingsScreen.kt",
        "│   ├── filemanager/",
        "│   ├── permissions/",
        "│   └── theme/",
        "├── util/",
        "│   └── BackupHelper.kt",
        "└── viewers/",
        "    └── Viewers.kt"
    )

    // ==========================================
    // BAGIAN 7 — DAFTAR FITUR
    // ==========================================
    val FEATURE_LIST: List<String> = listOf(
        "1. TRANSFER FILE DUAL-PANE (X-plore Style)",
        "   - 2 pane bersamaan di Mode TV, 1 pane di Mode HP",
        "   - Copy, Move, Rename, Delete, Compress, Extract",
        "   - Auto-play video, image viewer, vault enkripsi, disk map",
        "",
        "2. SERVER WiFi (Port 23016)",
        "   - Auto-scan perangkat di WiFi yang sama",
        "   - NSD/mDNS discovery untuk transfer file",
        "",
        "3. REMOTE TV (6 PROTOKOL)",
        "   - Protokol didukung:",
        "     * ADB Wi-Fi (port 5555) — Android TV, Fire TV",
        "     * Roku ECP (port 8060) — Roku TV",
        "     * Samsung Tizen (port 8001/8002) — Samsung Smart TV",
        "     * LG webOS SSAP (port 3000/3001) — LG Smart TV",
        "     * Philips JointSpace (port 1925/1926) — Philips TV",
        "     * Vizio SmartCast (port 9000) — Vizio TV",
        "   - Auto-detect protokol via ProtocolDetector",
        "   - Auto-scan mDNS + SSDP (bersamaan)",
        "   - Manual IP fallback untuk TV yang tidak terdeteksi",
        "",
        "4. UI REMOTE (5 MODE)",
        "   - D-Pad (default)",
        "   - Grid (angka 0-9 + RGBY)",
        "   - Mouse (touchpad kursor)",
        "   - Gesture (swipe = D-Pad virtual)",
        "   - Air Mouse (gyroscope)",
        "",
        "5. TOMBOL REMOTE LENGKAP",
        "   - Top Bar: Voice, Input, Cast, Keyboard, Copy, TV List",
        "   - Nav Bar: Home, Back, Recent, Mute",
        "   - Media Bar: Play, Pause, Stop, Rew, Fwd, Prev, Next",
        "   - Volume Bar: Vol+, Vol-, Ch+, Ch- (dengan gesture)",
        "   - Quick Bar: 5 mode switcher + Shortcut + Exit",
        "",
        "6. FITUR PENDUKUNG",
        "   - Volume Monitor Overlay (tap + gesture)",
        "   - Haptic Feedback (getar halus)",
        "   - Pengaturan Remote (haptic, sound, sensitivitas, ukuran tombol)",
        "   - Multi-TV Manager (simpan TV terdaftar di Room)",
        "   - Auto-connect ke TV terakhir",
        "   - Splash Screen + Onboarding 3 slide",
        "   - Backup Aman (14 bagian)",
        "",
        "7. KEYSTORE PERMANEN",
        "   - Auto-generate di GitHub Actions build pertama",
        "   - Disimpan sebagai debug.keystore.base64",
        "   - Update APK tanpa uninstall"
    )

    // ==========================================
    // BAGIAN 9 — RIWAYAT BUG DIPERBAIKI
    // ==========================================
    val FIXED_BUGS: List<String> = listOf(
        "[08-10-2026] WifiFileServerService.kt terpotong 25 baris -> restorasi full class",
        "[08-10-2026] Namespace com.example vs dev.andikuneiocontroll -> migrasi global",
        "[08-10-2026] META-INF/INDEX.LIST duplicate dari Netty -> packaging excludes",
        "[08-10-2026] Icon deprecated (Icons.Filled vs AutoMirrored) -> ganti AutoMirrored",
        "[08-10-2026] Missing import height di RemoteTvDialog.kt -> tambah import",
        "[08-10-2026] Keystore bentrok tiap build -> keystore permanen base64",
        "[08-10-2026] Pane Kanan buka ConnectionPaneView -> ganti jadi FilePaneView",
        "[08-10-2026] Toolbar Mode HP tidak dinamis -> MobileModeView dengan posisi adaptif",
        "[08-10-2026] Panah switch pane tidak berubah arah -> panah dinamis",
        "[08-10-2026] Dialog Server WiFi fullscreen -> popup kecil dengan auto-scan",
        "[09-10-2026] Conflicting overloads RemoteTopBar/RemoteNavBar -> hapus file lama",
        "[09-10-2026] Unresolved KeyboardArrowUp/Down -> tambah import di RemoteBars",
        "[09-10-2026] Delegate error MutableState<Long> -> pakai mutableLongStateOf",
        "[09-10-2026] Unresolved weight di SizeButton -> RowScope extension",
        "[09-10-2026] MainScreen masih pakai RemoteTvDialog lama -> ganti ke versi baru",
        "[09-10-2026] Discovery tidak deteksi Google TV -> tambah Manual IP fallback"
    )

    // ==========================================
    // BAGIAN 10 — RIWAYAT ERROR + SOLUSI
    // ==========================================
    val ERROR_HISTORY: List<String> = listOf(
        "[08-10-2026] - V1.00.001",
        "Error: Unresolved reference 'R' di WifiFileServerService",
        "File: app/src/main/java/com/example/server/WifiFileServerService.kt",
        "Solusi: Fix namespace, ganti com.example -> dev.andikuneiocontroll",
        "---",
        "[08-10-2026] - V1.00.001",
        "Error: 12 files found with path 'META-INF/INDEX.LIST'",
        "File: app/build.gradle.kts",
        "Solusi: Tambah packaging.excludes untuk META-INF/*",
        "---",
        "[08-10-2026] - V1.00.001",
        "Error: Aplikasi tidak diinstal karena paket ini bentrok",
        "File: debug.keystore.base64",
        "Solusi: Keystore permanen + uninstall APK lama sekali",
        "---",
        "[09-10-2026] - V1.00.002",
        "Error: Overload resolution ambiguity between candidates RemoteTopBar",
        "File: app/src/main/java/com/example/ui/remote/RemoteTvDialog.kt",
        "Solusi: Hapus file RemoteTopBar.kt + RemoteNavBar.kt lama (duplikat)",
        "---",
        "[09-10-2026] - V1.00.002",
        "Error: Unresolved reference 'KeyboardArrowUp' di RemoteBars.kt",
        "File: app/src/main/java/com/example/ui/remote/components/RemoteBars.kt",
        "Solusi: Tambah import KeyboardArrowUp + KeyboardArrowDown",
        "---",
        "[09-10-2026] - V1.00.002",
        "Error: Delegate error MutableState<Long> di RemoteModes.kt",
        "File: app/src/main/java/com/example/ui/remote/components/RemoteModes.kt",
        "Solusi: Pakai mutableLongStateOf, tambah import getValue/setValue",
        "---",
        "[09-10-2026] - V1.00.002",
        "Error: Unresolved reference 'weight' di RemoteSettingsScreen.kt",
        "File: app/src/main/java/com/example/ui/remote/settings/RemoteSettingsScreen.kt",
        "Solusi: SizeButton jadi RowScope extension"
    )

    // ==========================================
    // BAGIAN 13 — ATURAN KERAS
    // ==========================================
    val HARD_RULES: List<String> = listOf(
        "1. JANGAN ubah debug.keystore.base64",
        "2. JANGAN ubah signingConfigs di build.gradle.kts",
        "3. JANGAN ubah bagian keystore di build.yml",
        "4. Port WiFi server: 23016 (JANGAN diubah)",
        "5. Port TV Remote: 6467 (Android TV v2) / 5555 (ADB)",
        "6. Package: dev.andikuneiocontroll (JANGAN diubah)",
        "7. Install APK selalu dari GitHub Actions Artifacts",
        "8. Backup Aman harus update setiap build kalau ada fitur/bug baru"
    )

    val BUILD_ERROR_HISTORY: List<String> = listOf(
        "Build #1-#11: Gagal berturut-turut (namespace, keystore, META-INF)",
        "Build #36: SUKSES - keystore permanen + semua fix diterapkan",
        "Build v1.00.002: SUKSES - 6 protokol + UI Remote + Manual IP"
    )
}
