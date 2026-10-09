package dev.andikuneiocontroll.data

/**
 * Riwayat update aplikasi.
 * WAJIB update setiap build baru — tambah entri di paling atas.
 */
object UpdateHistory {

    val entries: List<UpdateEntry> = listOf(
        UpdateEntry(
            version = "V1.00.002",
            date = "09-10-2026",
            title = "Remote TV v2 — 6 Protokol + UI Baru + Manual IP",
            changes = listOf(
                "Tambah 6 protokol TV: ADB Wi-Fi, Roku ECP, Samsung Tizen, LG webOS, Philips JointSpace, Vizio SmartCast",
                "Tambah RemoteController (facade otomatis pilih protokol)",
                "Tambah TvDiscoveryManager (mDNS + SSDP scan bersamaan)",
                "Tambah TvFilter (buang HP sendiri dari hasil scan)",
                "Tambah Room database untuk simpan TV terdaftar",
                "Tambah PrefsRepository (DataStore pengaturan)",
                "Tambah UI Remote baru: D-Pad, Grid, Mouse, Gesture, Air Mouse",
                "Tambah Volume Monitor Overlay (tap + gesture)",
                "Tambah TvPickerDialog dengan tombol IP Manual",
                "Tambah ManualIpDialog untuk koneksi TV yang tidak terdeteksi",
                "Tambah Pairing PIN Dialog untuk Android TV",
                "Tambah Info TV Dialog (brand, IP, port, lupakan TV)",
                "Tambah Input Source Dialog (HDMI, AV, TV, USB)",
                "Tambah Shortcut Dialog (YouTube, Netflix, dll)",
                "Tambah RemoteSettingsScreen (haptic, sound, sensitivitas)",
                "Tambah HapticHelper (getar tombol)",
                "Tambah Splash Screen + Onboarding 3 slide",
                "Tambah flow: Splash → Onboarding → MainScreen",
                "Tambah AppKnowledge + UpdateHistory untuk Backup Aman"
            )
        ),
        UpdateEntry(
            version = "V1.00.001",
            date = "08-10-2026",
            title = "File Manager + Setup Fondasi",
            changes = listOf(
                "Setup project Android Kotlin + Jetpack Compose + MVVM",
                "Fitur File Manager Dual-Pane (terinspirasi X-plore)",
                "Fitur Server WiFi (port 23016) untuk transfer antar device",
                "UI Adaptif: Mode TV (2 Pane) & Mode HP (1 Pane)",
                "Toolbar dinamis di Mode HP (kiri/kanan sesuai pane aktif)",
                "Keystore permanen auto-generate di GitHub Actions",
                "Fitur Backup Aman (14 bagian)",
                "Auto-generate debug.keystore.base64 di repo",
                "Rainbow icon + Tooltip long-press"
            )
        )
    )
}

data class UpdateEntry(
    val version: String,
    val date: String,
    val title: String,
    val changes: List<String>
)
