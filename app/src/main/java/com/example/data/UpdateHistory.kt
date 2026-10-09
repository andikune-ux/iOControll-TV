package dev.andikuneiocontroll.data

object UpdateHistory {

    val entries: List<UpdateEntry> = listOf(
        UpdateEntry(
            version = "V1.00.004",
            date = "10-10-2026",
            title = "ADB Pairing + Voice + Keyboard + Chromecast + Rotate",
            changes = listOf(
                "TAMBAH ADB Wireless Debugging pairing (Google TV, Android TV, Fire TV)",
                "  - Library libadb-android untuk TLS + SPAKE2 handshake",
                "  - Dialog AdbPairingDialog untuk input IP + Port + Code 6 digit",
                "  - AdbTvClient.needsPairing flag → auto-munculkan dialog",
                "  - Auto-connect setelah pairing sukses",
                "",
                "TAMBAH Voice Input ke TV",
                "  - VoiceDialog auto-launch Google Voice Recognizer",
                "  - Kirim hasil ke TV via INPUT_TEXT",
                "  - Auto-close setelah terkirim",
                "",
                "TAMBAH Keyboard Input ke TV",
                "  - KeyboardDialog untuk ketik teks manual",
                "  - Tombol Hapus (backspace) + Enter",
                "  - Multi-baris input (max 4 baris)",
                "",
                "TAMBAH Copy Text dari TV",
                "  - RemoteController.copyTextFromTv() placeholder",
                "  - Wire tombol Copy di TopBar",
                "",
                "TAMBAH Screen Rotate",
                "  - RemoteController.rotateScreen() untuk rotate TV display",
                "",
                "TAMBAH Firmware Info",
                "  - RemoteController.getFirmwareInfo() — hanya ADB support",
                "  - Tampil di InfoTvDialog",
                "",
                "TAMBAH Chromecast Detection",
                "  - MdnsDiscovery deteksi _googlecast._tcp",
                "  - Field hasChromecast di DiscoveredTv + TvEntity",
                "  - Badge di InfoTvDialog",
                "",
                "TAMBAH Input Auto-Switch (C5)",
                "  - Gabung dengan InputSourceDialog",
                "",
                "UPDATE FILES (22 file):",
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
                "  - TvDialogs.kt (InfoTvDialog +firmware +chromecast)",
                "  - MainScreen.kt (wire semua dialog)"
            )
        ),
        UpdateEntry(
            version = "V1.00.003",
            date = "09-10-2026",
            title = "Cleanup + Integrasi Remote TV v2 Selesai",
            changes = listOf(
                "HAPUS 8 file lama:",
                "  - remote/RemoteClient.kt, RemoteSocketServer.kt, RemoteDiscovery.kt",
                "  - ui/remote/ConnectionPaneView.kt, RemoteControlView.kt",
                "  - ui/components/RemoteTvDialog.kt (duplikat)",
                "  - ui/remote/components/RemoteTopBar.kt, RemoteNavBar.kt (duplikat)",
                "",
                "TAMBAH/FIX:",
                "  - MainActivity: flow Splash → Onboarding → Main",
                "  - MainViewModel: autoConnectLastTv()",
                "  - RemoteHeader: klik nama TV buka Info TV",
                "  - MainScreen: wire onOpenInfoTv",
                "  - Fix struktur kurung tutup MainScreen",
                "  - Fix import KeyboardArrowUp/Down",
                "  - Fix mutableLongStateOf",
                "  - Fix RowScope extension"
            )
        ),
        UpdateEntry(
            version = "V1.00.002",
            date = "09-10-2026",
            title = "Remote TV v2 — 6 Protokol + UI Baru + Manual IP",
            changes = listOf(
                "6 protokol: ADB, Roku, Samsung, LG, Philips, Vizio",
                "RemoteController + TvDiscoveryManager + TvFilter",
                "Room database (TvEntity, TvDao, TvDatabase, TvRepository)",
                "PrefsRepository (DataStore)",
                "UI Remote: D-Pad, Grid, Mouse, Gesture, Air Mouse",
                "Volume Monitor Overlay",
                "Semua dialog (Picker, Pairing, Info, Input, Shortcut, Manual IP)",
                "RemoteSettingsScreen + HapticHelper",
                "Splash + Onboarding",
                "Library: OkHttp, Moshi, Room, jmDNS, DataStore"
            )
        ),
        UpdateEntry(
            version = "V1.00.001",
            date = "08-10-2026",
            title = "File Manager + Setup Fondasi",
            changes = listOf(
                "Setup project Android Kotlin + Compose + MVVM",
                "File Manager Dual-Pane (X-plore style)",
                "Server WiFi (port 23016)",
                "UI Adaptif: Mode TV (2 Pane) & Mode HP (1 Pane)",
                "Keystore permanen auto-generate",
                "Backup Aman (14 bagian)",
                "Rainbow icon + Tooltip"
            )
        )
    )

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
        BuildFailureEntry("#38", "10-10-2026", "PENDING BUILD", "Build FASE A+B+C+D belum dijalankan", "Tunggu build test setelah commit semua file v1.00.004")
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
