package dev.andikuneiocontroll.data

/**
 * Riwayat update aplikasi.
 * WAJIB update setiap build baru — tambah entri di paling atas.
 *
 * Format:
 *   data class UpdateEntry(version, date, title, changes: List<String>)
 */
object UpdateHistory {

    val entries: List<UpdateEntry> = listOf(
        UpdateEntry(
            version = "V1.00.000",
            date = "09-10-2026",
            title = "Inisialisasi + Fitur Inti",
            changes = listOf(
                "Setup project Android Kotlin + Jetpack Compose + MVVM",
                "Fitur File Manager Dual-Pane (terinspirasi X-plore)",
                "Fitur Server WiFi (port 23016) untuk transfer antar device",
                "Fitur Remote TV (terinspirasi Zank Remote) — dasar",
                "UI Adaptif: Mode TV (2 Pane) & Mode HP (1 Pane)",
                "Toolbar dinamis di Mode HP (kiri/kanan sesuai pane aktif)",
                "Keystore permanen auto-generate di GitHub Actions",
                "Fitur Backup Aman (14 bagian)",
                "Auto-generate debug.keystore.base64 di repo"
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
