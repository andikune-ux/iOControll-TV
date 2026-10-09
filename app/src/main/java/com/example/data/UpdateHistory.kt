package dev.andikuneiocontroll.data

/**
 * Riwayat update aplikasi.
 * WAJIB update setiap build baru — tambah entri di paling atas.
 *
 * CATATAN UNTUK AI SELANJUTNYA:
 * - Format tanggal: DD-MM-YYYY
 * - Versi format: V{major}.{minor}.{patch}
 * - Tambah entri paling atas supaya urut dari terbaru
 * - Kalau ada fitur baru → tambah di FEATURE_LIST di AppKnowledge
 * - Kalau ada build gagal → tambah di buildFailureHistory
 */
object UpdateHistory {

    val entries: List<UpdateEntry> = listOf(
        UpdateEntry(
            version = "V1.00.004",
            date = "10-10-2026",
            title = "ADB Pairing + Voice + Keyboard + Chromecast + Rotate",
            changes = listOf(
                "TAMBAH ADB Wireless Debugging pairing (Google TV, Android TV, Fire TV)",
                "  - Library libadb-android untuk TLS + SPAKE2 handshake",
                "  - Dialog AdbPairingDialog input IP + Port + Code 6 digit",
                "  - AdbTvClient.needsPairing flag → auto-munculkan dialog",
                "  - Auto-connect setelah pairing sukses",
                "",
                "TAMBAH Voice Input ke TV",
                "  - VoiceDialog auto-launch Google Voice Recognizer",
                "  - Kirim hasil ke TV via INPUT_TEXT",
                "",
                "TAMBAH Keyboard Input ke TV",
                "  - KeyboardDialog ketik teks manual",
                "  - Tombol Hapus (backspace) + Enter",
                "  - Multi-baris input (max 4 baris)",
                "",
                "TAMBAH Copy Text dari TV (placeholder)",
                "TAMBAH Screen Rotate Command",
                "TAMBAH Firmware Info (hanya ADB support)",
                "",
                "TAMBAH Chromecast Detection",
                "  - MdnsDiscovery deteksi _googlecast._tcp",
                "  - Field hasChromecast di DiscoveredTv + TvEntity",
                "  - Badge di InfoTvDialog",
                "",
                "FILE DIUBAH (22 file):",
                "  - libs.versions.toml + app/build.gradle.kts (libadb-android)",
                "  - AdbPairing.kt (BARU)",
                "  - AdbClient.kt (support pairing)",
                "  - AdbTvClient.kt (needsPairing flag)",
                "  - TvProtocol.kt (pair + COPY_TEXT + ROTATE_SCREEN)",
                "  - RemoteController.kt (pair + copyText + rotateScreen + getFirmwareInfo)",
                "  - RemoteControllers.kt (full implementation)",
                "  - AdbPairingDialog.kt (BARU)",
                "  - VoiceDialog.kt (BARU)",
                "  - KeyboardDialog.kt (BARU)",
                "  - RemoteTvDialog.kt (onOpenVoice + onOpenCopy)",
                "  - MdnsDiscovery.kt (Chromecast flag)",
                "  - DiscoveredTv.kt (hasChromecast)",
                "  - TvEntity.kt (hasChromecast + firmwareVersion)",
                "  - TvRepository.kt (support field baru)",
                "  - TvDialogs.kt (InfoTvDialog + firmware + chromecast)",
                "  - MainScreen.kt (wire semua dialog)"
            )
        ),
        UpdateEntry(
            version = "V1.00.003",
            date = "09-10-2026",
            title = "Cleanup + Integrasi Remote TV v2 Selesai",
            changes = listOf(
                "HAPUS 8 file lama (cleanup):",
                "  - remote/RemoteClient.kt",
                "  - remote/RemoteSocketServer.kt",
                "  - remote/RemoteDiscovery.kt",
                "  - ui/remote/ConnectionPaneView.kt",
                "  - ui/components/RemoteTvDialog.kt (duplikat)",
                "  - ui/remote/components/RemoteTopBar.kt (duplikat)",
                "  - ui/remote/components/RemoteNavBar.kt (duplikat)",
                "  - ui/remote/RemoteControlView.kt",
                "",
                "TAMBAH/FIX:",
                "  - MainActivity: flow Splash → Onboarding → Main",
                "  - MainViewModel: hapus import file lama + autoConnectLastTv()",
                "  - RemoteHeader: klik nama TV buka Info TV",
                "  - MainScreen: wire onOpenInfoTv",
                "  - Fix struktur kurung tutup MainScreen (extra brace)",
                "  - Fix duplikasi RemoteTopBar/RemoteNavBar",
                "  - Fix import KeyboardArrowUp/Down di RemoteBars",
                "  - Fix mutableLongStateOf di RemoteModes",
                "  - Fix RowScope extension di SizeButton"
            )
        ),
        UpdateEntry(
            version = "V1.00.002",
            date = "09-10-2026",
            title = "Remote TV v2 — 6 Protokol + UI Baru + Manual IP",
            changes = listOf(
                "TAMBAH 6 protokol TV:",
                "  - ADB Wi-Fi (5555) — Android TV, Fire TV",
                "  - Roku ECP (8060)",
                "  - Samsung Tizen (8001/8002)",
                "  - LG webOS SSAP (3000/3001)",
                "  - Philips JointSpace (1925/1926)",
                "  - Vizio SmartCast (9000)",
                "",
                "TAMBAH core remote:",
                "  - RemoteController (facade otomatis pilih protokol)",
                "  - TvDiscoveryManager (mDNS + SSDP scan bersamaan)",
                "  - TvFilter (buang HP sendiri dari hasil scan)",
                "  - ProtocolDetector (auto-detect protokol)",
                "  - TvProtocol interface + TvCommand constants",
                "",
                "TAMBAH database:",
                "  - Room: TvEntity, TvDao, TvDatabase, TvRepository",
                "  - DataStore: PrefsRepository",
                "",
                "TAMBAH UI Remote baru:",
                "  - RemoteTvDialog (container utama)",
                "  - RemoteHeader (Back, Nama, Settings, Power)",
                "  - RemoteDPad (D-Pad bulat)",
                "  - RemoteGrid (angka + RGBY)",
                "  - RemoteMouse (touchpad)",
                "  - RemoteGesture (swipe = D-Pad)",
                "  - RemoteAirMouse (gyroscope)",
                "  - RemoteBars (TopBar, NavBar, MediaBar, VolumeBar, QuickBar)",
                "  - VolumeMonitorOverlay",
                "  - HapticHelper",
                "  - RemoteButton components",
                "",
                "TAMBAH dialog:",
                "  - TvPickerDialog (auto-scan + Manual IP)",
                "  - ManualIpDialog (TV tidak terdeteksi)",
                "  - PairingPinDialog (Android TV, Samsung, LG)",
                "  - InfoTvDialog (brand, IP, port, lupakan)",
                "  - InputSourceDialog (HDMI, AV, TV, USB)",
                "  - ShortcutDialog (YouTube, Netflix, dll)",
                "  - RemoteSettingsScreen (haptic, sound, sensitivitas)",
                "",
                "TAMBAH fitur:",
                "  - Splash Screen",
                "  - Onboarding 3 slide",
                "  - Auto-connect TV terakhir",
                "  - Multi-TV Manager",
                "  - Controllers: Voice, Keyboard, Mouse, Cast, Shortcut",
                "",
                "TAMBAH library:",
                "  - OkHttp + okhttp-tls",
                "  - Moshi (JSON)",
                "  - Room (database TV)",
                "  - jmDNS (mDNS discovery)",
                "  - DataStore"
            )
        ),
        UpdateEntry(
            version = "V1.00.001",
            date = "08-10-2026",
            title = "File Manager + Setup Fondasi",
            changes = listOf(
                "TAMBAH project Android Kotlin + Jetpack Compose + MVVM",
                "TAMBAH Fitur File Manager Dual-Pane (X-plore style)",
                "TAMBAH Server WiFi (port 23016) transfer antar device",
                "TAMBAH UI Adaptif: Mode TV (2 Pane) & Mode HP (1 Pane)",
                "TAMBAH Toolbar dinamis di Mode HP",
                "TAMBAH Keystore permanen auto-generate di GitHub Actions",
                "TAMBAH Backup Aman (14 bagian)",
                "TAMBAH Rainbow icon + Tooltip long-press",
                "TAMBAH File Manager: Copy, Move, Rename, Delete, Compress, Extract, Vault, Disk Map",
                "TAMBAH 5 viewer: Video, Image, Audio, Text, Hex",
                "TAMBAH Auto-play video saat tap"
            )
        )
    )

    /**
     * Riwayat build gagal — untuk referensi di masa depan.
     * Format: buildNumber, date, error, cause, solution
     */
    val buildFailureHistory: List<BuildFailureEntry> = listOf(
        BuildFailureEntry("#1", "08-10-2026", "gradlew tidak ada", "File gradlew belum di-generate", "Pakai 'gradle assembleDebug' langsung"),
        BuildFailureEntry("#2-#4", "08-10-2026", "Unresolved reference 'R'", "Namespace beda dengan package", "Migrasi com.example → dev.andikuneiocontroll"),
        BuildFailureEntry("#5-#10", "08-10-2026", "META-INF/INDEX.LIST duplicate", "Library Netty punya file duplikat", "Tambah packaging.excludes"),
        BuildFailureEntry("#11", "08-10-2026", "WifiFileServerService terpotong", "File kepotong saat paste", "Timpa file versi lengkap"),
        BuildFailureEntry("#24", "09-10-2026", "Paket bentrok", "Keystore baru tiap build", "Keystore permanen base64"),
        BuildFailureEntry("#27", "09-10-2026", "Unresolved 'height'", "Import height kurang", "Tambah import"),
        BuildFailureEntry("#33-34", "09-10-2026", "Conflicting overloads", "File duplikat", "Hapus RemoteTopBar.kt + RemoteNavBar.kt"),
        BuildFailureEntry("#35", "09-10-2026", "KeyboardArrowUp/Down unresolved", "Import kurang", "Tambah import"),
        BuildFailureEntry("#35b", "09-10-2026", "MutableState<Long> delegate error", "Pakai mutableStateOf(0L)", "Pakai mutableLongStateOf"),
        BuildFailureEntry("#35c", "09-10-2026", "'weight' unresolved", "Modifier.weight di luar RowScope", "SizeButton jadi RowScope extension"),
        BuildFailureEntry("#36", "09-10-2026", "Top level declaration", "Kurung tutup berlebih", "Hapus 1 '}'"),
        BuildFailureEntry("#37", "09-10-2026", "RemoteClient/Server unresolved", "File lama dihapus, import masih ada", "Hapus import di MainViewModel"),
        BuildFailureEntry("#38", "10-10-2026", "PENDING BUILD", "Build v1.00.004 belum dijalankan", "Tunggu build test setelah commit semua file")
    )
}

data class UpdateEntry(
    val version: String,
    val date: String,
    val title: String,
    val changes: List<String>
)

data class BuildFailureEntry(
    val buildNumber: String,
    val date: String,
    val error: String,
    val cause: String,
    val solution: String
)
