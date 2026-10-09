package dev.andikuneiocontroll.data

object UpdateHistory {

    val entries: List<UpdateEntry> = listOf(
        UpdateEntry(
            version = "V1.00.003",
            date = "09-10-2026",
            title = "Cleanup + Integrasi Remote TV v2 Selesai",
            changes = listOf(
                "HAPUS 8 file lama (cleanup):",
                "  - remote/RemoteClient.kt (diganti RemoteController)",
                "  - remote/RemoteSocketServer.kt (diganti 6 protokol baru)",
                "  - remote/RemoteDiscovery.kt (diganti remote/discovery/*)",
                "  - ui/remote/ConnectionPaneView.kt (tidak dipakai)",
                "  - ui/components/RemoteTvDialog.kt (duplikat, diganti ui/remote/)",
                "  - ui/remote/components/RemoteTopBar.kt (duplikat, digabung ke RemoteBars.kt)",
                "  - ui/remote/components/RemoteNavBar.kt (duplikat, digabung ke RemoteBars.kt)",
                "  - ui/remote/RemoteControlView.kt (refer pairingCode lama)",
                "",
                "TAMBAH/FIX:",
                "MainActivity: flow Splash → Onboarding → Main dengan auto-connect",
                "MainViewModel: hapus referensi file lama, tambah autoConnectLastTv()",
                "RemoteHeader: klik nama TV buka Info TV",
                "RemoteTvDialog: tambah parameter onOpenInfoTv",
                "MainScreen: wire onOpenInfoTv + full timpa 3 bagian",
                "Fix: struktur kurung tutup MainScreen (bug extra brace)",
                "Fix: duplikasi RemoteTopBar/RemoteNavBar",
                "Fix: import KeyboardArrowUp/Down di RemoteBars",
                "Fix: mutableLongStateOf di RemoteModes",
                "Fix: RowScope extension di SizeButton",
                "Cleanup final: build tetap sukses"
            )
        ),
        UpdateEntry(
            version = "V1.00.002",
            date = "09-10-2026",
            title = "Remote TV v2 — 6 Protokol + UI Baru + Manual IP",
            changes = listOf(
                "TAMBAH 6 protokol TV:",
                "  - ADB Wi-Fi (port 5555) — Android TV, Fire TV",
                "  - Roku ECP (port 8060) — Roku TV",
                "  - Samsung Tizen (port 8001/8002) — Samsung Smart TV",
                "  - LG webOS SSAP (port 3000/3001) — LG Smart TV",
                "  - Philips JointSpace (port 1925/1926) — Philips TV",
                "  - Vizio SmartCast (port 9000) — Vizio TV",
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
                "  - ManualIpDialog (untuk TV tidak terdeteksi)",
                "  - PairingPinDialog (Android TV)",
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
                "  - OkHttp + okhttp-tls (WebSocket + TLS)",
                "  - Moshi (JSON parsing)",
                "  - Room (database TV)",
                "  - jmDNS (mDNS discovery)",
                "  - DataStore (pengaturan)"
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
                "TAMBAH 4 Penonton viewer: Video, Image, Audio, Text, Hex",
                "TAMBAH Auto-play video saat tap"
            )
        )
    )

    /**
     * Riwayat build gagal — untuk referensi di masa depan.
     */
    val buildFailureHistory: List<BuildFailureEntry> = listOf(
        BuildFailureEntry(
            buildNumber = "#1",
            date = "08-10-2026",
            error = "chmod: cannot access './gradlew': No such file or directory",
            cause = "File gradlew tidak ada di repo (belum di-generate)",
            solution = "Ganti workflow: pakai 'gradle assembleDebug' langsung, bukan './gradlew'"
        ),
        BuildFailureEntry(
            buildNumber = "#2-#4",
            date = "08-10-2026",
            error = "Unresolved reference 'R' di WifiFileServerService",
            cause = "Namespace 'dev.andikuneiocontroll' beda dengan package 'com.example'",
            solution = "Migrasi semua file dari com.example -> dev.andikuneiocontroll via sed workflow"
        ),
        BuildFailureEntry(
            buildNumber = "#5-#10",
            date = "08-10-2026",
            error = "12 files found with path 'META-INF/INDEX.LIST'",
            cause = "Library Netty (dari Ktor) punya banyak file duplikat",
            solution = "Tambah packaging.excludes di app/build.gradle.kts untuk META-INF/*"
        ),
        BuildFailureEntry(
            buildNumber = "#11",
            date = "08-10-2026",
            error = "WifiFileServerService.kt terpotong 25 baris",
            cause = "File kepotong saat paste (paste di GitHub gagal)",
            solution = "Timpa file dengan versi lengkap class WifiFileServerService"
        ),
        BuildFailureEntry(
            buildNumber = "#24",
            date = "09-10-2026",
            error = "Aplikasi tidak diinstal karena paket ini bentrok",
            cause = "Keystore baru tiap build -> signature beda dengan APK lama",
            solution = "Simpan keystore permanen sebagai debug.keystore.base64, decode saat build"
        ),
        BuildFailureEntry(
            buildNumber = "#27",
            date = "09-10-2026",
            error = "Unresolved reference 'height' di RemoteTvDialog.kt",
            cause = "Import androidx.compose.foundation.layout.height kurang",
            solution = "Tambah import height di RemoteTvDialog.kt"
        ),
        BuildFailureEntry(
            buildNumber = "#33-#34",
            date = "09-10-2026",
            error = "Conflicting overloads RemoteTopBar + RemoteNavBar",
            cause = "File RemoteTopBar.kt + RemoteNavBar.kt duplikat dengan RemoteBars.kt",
            solution = "Hapus 2 file lama (RemoteTopBar.kt + RemoteNavBar.kt)"
        ),
        BuildFailureEntry(
            buildNumber = "#35",
            date = "09-10-2026",
            error = "Unresolved KeyboardArrowUp/Down di RemoteBars.kt",
            cause = "Import KeyboardArrowUp + KeyboardArrowDown kurang",
            solution = "Tambah import KeyboardArrowUp + KeyboardArrowDown"
        ),
        BuildFailureEntry(
            buildNumber = "#35b",
            date = "09-10-2026",
            error = "Delegate error MutableState<Long> di RemoteModes.kt",
            cause = "var lastTapTime by remember { mutableStateOf(0L) } butuh mutableLongStateOf",
            solution = "Pakai mutableLongStateOf + import getValue/setValue"
        ),
        BuildFailureEntry(
            buildNumber = "#35c",
            date = "09-10-2026",
            error = "Unresolved reference 'weight' di RemoteSettingsScreen.kt",
            cause = "Modifier.weight() hanya bisa dipakai di RowScope",
            solution = "Jadikan SizeButton sebagai RowScope extension"
        ),
        BuildFailureEntry(
            buildNumber = "#36",
            date = "09-10-2026",
            error = "Expecting a top level declaration di MainScreen.kt (baris 586+)",
            cause = "Kurung tutup '}' berlebih di akhir fungsi MainScreen",
            solution = "Hapus 1 kurung tutup berlebih, pastikan total 4 tutup (when, Box, Scaffold, fun)"
        ),
        BuildFailureEntry(
            buildNumber = "#37",
            date = "09-10-2026",
            error = "Unresolved reference RemoteClient, RemoteSocketServer",
            cause = "5 file lama dihapus, tapi MainViewModel masih import",
            solution = "Timpa MainViewModel tanpa import + deklarasi file lama"
        )
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
