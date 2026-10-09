package dev.andikuneiocontroll.data

/**
 * Memory Knowledge & Instruction — iOControll Tv
 *
 * WAJIB update setiap build kalau ada:
 * - Fitur baru
 * - Bug diperbaiki
 * - Error + solusi baru
 * - Perubahan struktur
 * - Aturan baru
 */
object AppKnowledge {

    const val APP_NAME = "iOControll Tv"
    const val PACKAGE_NAME = "dev.andikuneiocontroll"
    const val REPO_URL = "https://github.com/andikune-ux/iOControll-TV"
    const val REPO_ACTIONS = "https://github.com/andikune-ux/iOControll-TV/actions"
    const val CURRENT_VERSION = "V1.00.000"
    const val WIFI_SERVER_PORT = 23016

    // ==========================================
    // BAGIAN 2 — PENGATURAN USER (template statis)
    // ==========================================
    val SETTINGS_MENU: List<String> = listOf(
        "1.  Lokasi & Waktu",
        "2.  Pengaturan Tampilan",
        "3.  Server WiFi (Transfer File)",
        "4.  Remote TV",
        "5.  Keamanan",
        "6.  Backup Aman",
        "7.  Tentang Aplikasi",
        "8.  Opsi Developer"
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
        "- SELALU install dari GitHub Release",
        "- JANGAN install APK dari AI Studio",
        "- Alasan: signature beda -> Android tolak -> bentrok",
        "",
        "=== BAGIAN 5 — PRINSIP UTAMA ===",
        "1. KERJAKAN PER BATCH",
        "2. JANGAN BUKA FILE YANG SAMA BERKALI-KALI",
        "3. KONFIRMASI SEBELUM LANJUT",
        "4. JANGAN ASUMSI - LIHAT KODE ASLI DULU",
        "5. JANGAN HAPUS FITUR LAMA TANPA IZIN"
    )

    // ==========================================
    // BAGIAN 5 — STRUKTUR FOLDER APLIKASI
    // ==========================================
    val FOLDER_STRUCTURE: List<String> = listOf(
        "app/src/main/java/dev/andikuneiocontroll/",
        "├── MainActivity.kt",
        "├── MainViewModel.kt",
        "├── data/",
        "│   ├── AppKnowledge.kt",
        "│   └── UpdateHistory.kt",
        "├── filemanager/",
        "│   └── FileManagerHelper.kt",
        "├── model/",
        "│   └── Models.kt",
        "├── remote/",
        "│   ├── RemoteClient.kt",
        "│   └── RemoteSocketServer.kt",
        "├── server/",
        "│   ├── DiscoveryManager.kt",
        "│   ├── WifiFileServerService.kt",
        "│   └── WifiHttpServer.kt",
        "├── ui/",
        "│   ├── MainScreen.kt",
        "│   ├── components/",
        "│   │   ├── MobileModeView.kt",
        "│   │   └── RemoteTvDialog.kt",
        "│   ├── filemanager/",
        "│   │   ├── FilePaneView.kt",
        "│   │   └── WifiShareDialog.kt",
        "│   ├── permissions/",
        "│   │   └── PermissionHandlerView.kt",
        "│   ├── remote/",
        "│   │   └── ConnectionPaneView.kt",
        "│   └── theme/",
        "│       ├── Color.kt",
        "│       ├── Theme.kt",
        "│       └── Type.kt",
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
        "   - 2 pane bersamaan di Mode TV",
        "   - 1 pane di Mode HP dengan switch toolbar",
        "   - Copy, Move, Rename, Delete, Compress, Extract",
        "   - Auto-play video saat tap",
        "   - Auto-play image viewer",
        "   - Vault enkripsi",
        "   - Disk Map",
        "",
        "2. SERVER WiFi (Port 23016)",
        "   - Auto-scan perangkat di WiFi yang sama",
        "   - NSD/mDNS discovery",
        "   - Popup dialog, bukan fullscreen",
        "   - Read-only / full access mode",
        "",
        "3. REMOTE TV (Zank Remote Style)",
        "   - D-Pad, Home, Back, Recent",
        "   - Mouse touchpad",
        "   - Keyboard input",
        "   - Media control",
        "   - Jendela baru full screen",
        "",
        "4. UI ADAPTIF",
        "   - Mode TV: 2 Pane + toolbar tengah vertikal",
        "   - Mode HP: 1 Pane + toolbar dinamis (kiri/kanan)",
        "   - Fullscreen: force rotasi ke landscape",
        "",
        "5. BACKUP AMAN",
        "   - Ekspor 14 bagian ke TXT",
        "   - Lokasi: /sdcard/IOremote TV/Backup Aman/",
        "",
        "6. KEYSTORE PERMANEN",
        "   - Auto-generate di GitHub Actions build pertama",
        "   - Disimpan sebagai debug.keystore.base64",
        "   - Update APK tanpa uninstall"
    )

    // ==========================================
    // BAGIAN 9 — RIWAYAT BUG DIPERBAIKI
    // ==========================================
    val FIXED_BUGS: List<String> = listOf(
        "[09-10-2026] WifiFileServerService.kt terpotong 25 baris -> restorasi full class",
        "[09-10-2026] Namespace com.example vs dev.andikuneiocontroll -> migrasi global",
        "[09-10-2026] META-INF/INDEX.LIST duplicate dari Netty -> packaging excludes",
        "[09-10-2026] Icon deprecated (Icons.Filled vs AutoMirrored) -> ganti AutoMirrored",
        "[09-10-2026] Missing import height di RemoteTvDialog.kt -> tambah import",
        "[09-10-2026] Keystore bentrok tiap build -> keystore permanen base64",
        "[09-10-2026] Pane Kanan buka ConnectionPaneView -> ganti jadi FilePaneView",
        "[09-10-2026] Toolbar Mode HP tidak dinamis -> MobileModeView dengan posisi adaptif",
        "[09-10-2026] Panah switch pane tidak berubah arah -> panah dinamis",
        "[09-10-2026] Dialog Server WiFi fullscreen -> popup kecil dengan auto-scan"
    )

    // ==========================================
    // BAGIAN 10 — RIWAYAT ERROR + SOLUSI
    // ==========================================
    val ERROR_HISTORY: List<String> = listOf(
        "[09-10-2026] - V1.00.000",
        "Error: Unresolved reference 'R' di WifiFileServerService",
        "File: app/src/main/java/com/example/server/WifiFileServerService.kt",
        "Solusi: Fix namespace, ganti com.example -> dev.andikuneiocontroll",
        "---",
        "[09-10-2026] - V1.00.000",
        "Error: 12 files found with path 'META-INF/INDEX.LIST'",
        "File: app/build.gradle.kts",
        "Solusi: Tambah packaging.excludes untuk META-INF/*",
        "---",
        "[09-10-2026] - V1.00.000",
        "Error: Aplikasi tidak diinstal karena paket ini bentrok",
        "File: debug.keystore.base64",
        "Solusi: Keystore permanen + uninstall APK lama sekali",
        "---",
        "[09-10-2026] - V1.00.000",
        "Error: Unresolved reference 'height' di RemoteTvDialog.kt",
        "File: app/src/main/java/com/example/ui/components/RemoteTvDialog.kt",
        "Solusi: Tambah import androidx.compose.foundation.layout.height"
    )

    // ==========================================
    // BAGIAN 13 — ATURAN KERAS
    // ==========================================
    val HARD_RULES: List<String> = listOf(
        "1. JANGAN ubah debug.keystore.base64",
        "2. JANGAN ubah signingConfigs di build.gradle.kts",
        "3. JANGAN ubah bagian keystore di build.yml",
        "4. Port WiFi server: 23016 (JANGAN diubah)",
        "5. Package: dev.andikuneiocontroll (JANGAN diubah)",
        "6. Install APK selalu dari GitHub Actions Artifacts",
        "7. Backup Aman harus update setiap build kalau ada fitur/bug baru"
    )

    val BUILD_ERROR_HISTORY: List<String> = listOf(
        "Build #1-#11: Gagal berturut-turut (namespace, keystore, META-INF)",
        "Build #36: SUKSES - keystore permanen + semua fix diterapkan"
    )
}
