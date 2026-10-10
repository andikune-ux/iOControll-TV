package dev.andikuneiocontroll.data

object UpdateHistory {

    val entries: List<UpdateEntry> = listOf(
        UpdateEntry(
            version = "V1.00.005",
            date = "10-10-2026",
            title = "Android TV Remote v2 — Pairing PIN (Seperti Zank Remote)",
            changes = listOf(
                "TAMBAH Android TV Remote v2 — connect TV tanpa Developer Mode",
                "  - Cukup PIN 6 digit dari TV (seperti Zank Remote / Remote ATV)",
                "  - Tidak perlu aktifkan Wireless Debugging di TV",
                "  - Tidak perlu input IP+Port manual",
                "  - Tidak perlu install aplikasi di TV",
                "",
                "FILE BARU (10 file):",
                "  - app/src/main/proto/remotemessage.proto (protobuf schema remote)",
                "  - app/src/main/proto/pairingmessage.proto (protobuf schema pairing)",
                "  - remote/protocol/androidtv/TlsHelper.kt (TLS cert self-signed)",
                "  - remote/protocol/androidtv/Spake2.kt (crypto SPAKE2 Ed25519)",
                "  - remote/protocol/androidtv/AndroidTvPairingClient.kt (pairing flow)",
                "  - remote/protocol/androidtv/AndroidTvRemoteClient.kt (remote control v2)",
                "",
                "FILE DIUBAH:",
                "  - gradle/libs.versions.toml (tambah protobuf plugin + BouncyCastle)",
                "  - app/build.gradle.kts (config protobuf + dependencies)",
                "  - remote/controller/RemoteController.kt (prioritas v2, fallback ADB)",
                "  - ui/MainScreen.kt (route pairing v2 ke PairingPinDialog)",
                "",
                "CARA KERJA BARU:",
                "1. Buka app → tap Remote TV → scan TV",
                "2. Tap TV → TV tampilkan PIN 6 digit otomatis",
                "3. Masukkan PIN di HP (cukup 1 kolom)",
                "4. Pairing sukses → cert disimpan",
                "5. Koneksi berikutnya: OTOMATIS (tanpa PIN lagi)",
                "",
                "PROTOKOL PRIORITAS BARU:",
                "  1. Android TV Remote v2 (UTAMA — Google TV / Android TV)",
                "  2. ADB Wireless Debugging (fallback — Fire TV / Android lama)",
                "  3. Roku, Samsung, LG, Philips, Vizio (brand-specific)",
                "",
                "SISA PEKERJAAN (harus dikerjakan setelah build sukses):",
                "  - Build test v1.00.005",
                "  - Test pairing ke TV Xiaomi (192.168.0.103)",
                "  - Kalau SPAKE2 gagal → debug konstanta M/N",
                "  - Kalau pairing berhasil → test D-Pad, Volume, Mouse"
            )
        ),
        UpdateEntry(
            version = "V1.00.004",
            date = "10-10-2026",
            title = "ADB Pairing + Voice + Keyboard + Chromecast + Rotate",
            changes = listOf(
                "TAMBAH ADB Wireless Debugging pairing",
                "TAMBAH Voice Input (Google Voice → TV)",
                "TAMBAH Keyboard Input (IME → TV)",
                "TAMBAH Copy Text dari TV (placeholder)",
                "TAMBAH Screen Rotate Command",
                "TAMBAH Firmware Info",
                "TAMBAH Chromecast Detection (mDNS _googlecast)",
                "TAMBAH Input Auto-Switch",
                "FILE DIUBAH (22 file)"
            )
        ),
        UpdateEntry(
            version = "V1.00.003",
            date = "09-10-2026",
            title = "Cleanup + Integrasi Remote TV v2 Selesai",
            changes = listOf(
                "HAPUS 8 file lama (cleanup)",
                "MainActivity: flow Splash → Onboarding → Main",
                "MainViewModel: autoConnectLastTv()",
                "RemoteHeader: klik nama TV buka Info TV",
                "Fix struktur kurung tutup MainScreen",
                "Fix duplikasi RemoteTopBar/NavBar",
                "Fix import KeyboardArrowUp/Down",
                "Fix mutableLongStateOf",
                "Fix RowScope extension"
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
        BuildFailureEntry("#38", "10-10-2026", "libadb-android tidak ditemukan", "Salah group ID", "Pakai com.github.MuntashirAkon + JitPack"),
        BuildFailureEntry("#38b", "10-10-2026", "'pair' override error + AdbPairingDialog 404", "AdbTvClient.pair tidak override + file belum commit", "Tambah 'override' + commit AdbPairingDialog"),
        BuildFailureEntry("#38c", "10-10-2026", "AppKnowledge kurung tutup berlebih", "Paste error", "Timpa full AppKnowledge"),
        BuildFailureEntry("#39", "10-10-2026", "PENDING BUILD — v1.00.005", "Belum dijalankan", "Tunggu build test Android TV Remote v2")
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
