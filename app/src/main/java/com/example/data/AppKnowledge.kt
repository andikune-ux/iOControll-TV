package dev.andikuneiocontroll.data

/**
 * Memory Knowledge & Instruction — iOControll Tv
 *
 * File ini berisi SEMUA memory, aturan kerja, dan informasi penting
 * yang diberikan user (Andi) ke AI selama pengerjaan project.
 *
 * WAJIB update setiap build kalau ada fitur/aturan baru.
 */
object AppKnowledge {

    // ==========================================================
    // IDENTITAS APLIKASI
    // ==========================================================
    const val APP_NAME = "iOControll Tv"
    const val PACKAGE_NAME = "dev.andikuneiocontroll"
    const val REPO_URL = "https://github.com/andikune-ux/iOControll-TV"
    const val REPO_ACTIONS = "https://github.com/andikune-ux/iOControll-TV/actions"
    const val CURRENT_VERSION = "V1.00.003"
    const val WIFI_SERVER_PORT = 23016
    const val TV_REMOTE_PORT = 6467
    const val ADB_PORT = 5555

    // ==========================================================
    // IDENTITAS USER
    // ==========================================================
    const val USER_NAME = "Andi"
    const val USER_EMAIL = "andikune@gmail.com"
    const val USER_DEVICE = "HP Android (bukan PC)"
    const val USER_TV = "Xiaomi Google TV (IP: 192.168.0.103)"
    const val USER_WORK_MODE = "GitHub Mobile (tidak pakai PC)"

    // ==========================================================
    // BAGIAN 1 — ATURAN FORMAT RESPON (WAJIB)
    // ==========================================================
    val FORMAT_RULES: List<String> = listOf(
        "=== ATURAN FORMAT RESPON ===",
        "Elemen wajib setiap respon:",
        "1. Visualisasi folder (emoji + indentasi)",
        "2. Path lengkap file",
        "3. Nama file",
        "4. URL edit / URL new",
        "5. Kode timpa FULL (jangan instruksi baris-per-baris)",
        "6. Pesan commit (contoh: 'Update MainActivity.kt')",
        "",
        "ATURAN:",
        "- Pakai code block (3 backtick) untuk path/URL/nama file",
        "- Bahasa Indonesia yang mudah dipahami",
        "- 1 chat = 1 fitur utuh kalau muat",
        "- Kalau kode tidak muat 1 bubble → tulis 'jangan commit dulu, masih ada sambungan'",
        "- JANGAN nolak buka link GitHub publik",
        "- JANGAN gabung link 1 file dengan link lain — pisah per file",
        "- Kalau file panjang (>300 baris) → bagi jadi 3-7 BAGIAN kecil",
        "- Kalau paste gagal di HP → bagi jadi lebih banyak BAGIAN",
        "- User pakai HP — JANGAN kasih instruksi yang butuh PC",
        "- User minta KODE TIMPA FULL, bukan instruksi edit manual baris-per-baris"
    )

    // ==========================================================
    // BAGIAN 2 — ATURAN ANTI-TRUNCATION
    // ==========================================================
    val ANTI_TRUNCATION_RULES: List<String> = listOf(
        "=== ATURAN ANTI-TRUNCATION ===",
        "Kalau kode kepanjangan:",
        "1. Judul: 'BAGIAN 1 DARI N'",
        "2. Akhir: '(lanjut di BAGIAN berikutnya)'",
        "3. Awal: '(sambungan dari BAGIAN sebelumnya)'",
        "4. JANGAN potong di tengah fungsi",
        "5. JANGAN bilang 'kode dilanjut di chat berikutnya'",
        "6. Kalau paste gagal di HP → bagi jadi lebih banyak BAGIAN",
        "7. Kalau file >400 baris → otomatis bagi jadi beberapa BAGIAN",
        "8. Saat user ketik 'lanjut' → kirim bagian berikutnya",
        "9. Tutup kurung harus PAS — kelebihan 1 '}' bikin error build"
    )

    // ==========================================================
    // BAGIAN 3 — ATURAN KERJA
    // ==========================================================
    val WORK_RULES: List<String> = listOf(
        "=== ATURAN KERJA ===",
        "1. Konfirmasi dulu sebelum eksekusi",
        "2. Jujur kalau belum tahu — jangan menebak",
        "3. JANGAN hapus fitur lama tanpa izin",
        "4. Lihat kode asli dulu sebelum timpa",
        "5. Kerjakan per batch",
        "6. JANGAN buka file sama 2x tanpa alasan",
        "7. Konfirmasi sebelum lanjut",
        "8. Kalau build gagal — tunggu error, jangan asumsi",
        "9. Kalau user bilang 'timpa full' — kirim FULL kode, bukan instruksi edit",
        "10. Kalau user bilang 'pecah jadi N' — bagi kode jadi N bagian"
    )

    // ==========================================================
    // BAGIAN 4 — ATURAN SIGNATURE / KEYSTORE (WAJIB)
    // ==========================================================
    val HARD_RULES: List<String> = listOf(
        "=== ATURAN KERAS SIGNATURE / KEYSTORE ===",
        "JANGAN PERNAH ubah file-file ini:",
        "1. debug.keystore.base64 (root repo)",
        "2. Blok signingConfigs di app/build.gradle.kts",
        "3. Bagian keystore di .github/workflows/build.yml",
        "",
        "ATURAN INSTALL APK:",
        "- SELALU install dari GitHub Actions Artifacts",
        "- JANGAN install APK dari AI Studio",
        "- Alasan: signature beda → Android tolak → bentrok",
        "",
        "ATURAN KALAU PAKAI AI STUDIO:",
        "- Tekankan di prompt: 'JANGAN ubah debug.keystore, signingConfigs, workflow signing'",
        "- AI Studio cuma boleh EDIT kode Kotlin/XML saja",
        "- Build tetap via GitHub Actions",
        "",
        "RIWAYAT KEJADIAN:",
        "- 08-10-2026: Keystore baru tiap build → bentrok install",
        "- Solusi: Simpan keystore permanen sebagai debug.keystore.base64"
    )

    // ==========================================================
    // BAGIAN 5 — IDENTITAS REPO & URL
    // ==========================================================
    val REPO_URLS: List<String> = listOf(
        "Repo Utama  : https://github.com/andikune-ux/iOControll-TV",
        "Actions     : https://github.com/andikune-ux/iOControll-TV/actions",
        "RAW         : https://raw.githubusercontent.com/andikune-ux/iOControll-TV/main/",
        "BLOB        : https://github.com/andikune-ux/iOControll-TV/blob/main/",
        "LINK EDIT   : https://github.com/andikune-ux/iOControll-TV/edit/main/{path}",
        "LINK NEW    : https://github.com/andikune-ux/iOControll-TV/new/main/{path}"
    )

    // ==========================================================
    // BAGIAN 6 — STRUKTUR FOLDER APLIKASI
    // ==========================================================
    val FOLDER_STRUCTURE: List<String> = listOf(
        "app/src/main/java/dev/andikuneiocontroll/",
        "├── MainActivity.kt          (flow Splash → Onboarding → Main)",
        "├── MainViewModel.kt         (viewmodel utama)",
        "├── data/local/              (Room + DataStore)",
        "│   ├── TvEntity.kt",
        "│   ├── TvDao.kt",
        "│   ├── TvDatabase.kt",
        "│   ├── TvRepository.kt",
        "│   └── PrefsRepository.kt",
        "├── data/                    (knowledge & history)",
        "│   ├── AppKnowledge.kt",
        "│   └── UpdateHistory.kt",
        "├── filemanager/",
        "│   └── FileManagerHelper.kt",
        "├── model/Models.kt",
        "├── remote/",
        "│   ├── controller/",
        "│   │   ├── RemoteController.kt",
        "│   │   └── RemoteControllers.kt",
        "│   ├── discovery/",
        "│   │   ├── DiscoveredTv.kt",
        "│   │   ├── MdnsDiscovery.kt",
        "│   │   ├── SsdpDiscovery.kt",
        "│   │   ├── TvDiscoveryManager.kt",
        "│   │   └── TvFilter.kt",
        "│   └── protocol/",
        "│       ├── TvProtocol.kt",
        "│       ├── ProtocolDetector.kt",
        "│       ├── adb/ (AdbProtocol, AdbCrypto, AdbClient, AdbTvClient)",
        "│       ├── roku/RokuEcpClient.kt",
        "│       ├── samsung/SamsungTizenClient.kt",
        "│       ├── lg/LgWebOsClient.kt",
        "│       ├── philips/PhilipsClient.kt",
        "│       └── vizio/VizioClient.kt",
        "├── server/",
        "│   ├── WifiFileServerService.kt",
        "│   ├── WifiHttpServer.kt",
        "│   └── DiscoveryManager.kt",
        "├── ui/",
        "│   ├── MainScreen.kt",
        "│   ├── SplashAndOnboarding.kt",
        "│   ├── remote/",
        "│   │   ├── RemoteTvDialog.kt",
        "│   │   ├── components/",
        "│   │   │   ├── HapticHelper.kt",
        "│   │   │   ├── RemoteButton.kt",
        "│   │   │   ├── RemoteHeader.kt",
        "│   │   │   ├── RemoteDPad.kt",
        "│   │   │   ├── RemoteBars.kt",
        "│   │   │   └── RemoteModes.kt",
        "│   │   ├── dialogs/TvDialogs.kt",
        "│   │   └── settings/RemoteSettingsScreen.kt",
        "│   ├── filemanager/",
        "│   ├── permissions/",
        "│   └── theme/",
        "├── util/",
        "│   └── BackupHelper.kt",
        "└── viewers/",
        "    └── Viewers.kt"
    )

    // ==========================================================
    // BAGIAN 7 — DAFTAR FITUR LENGKAP
    // ==========================================================
    val FEATURE_LIST: List<String> = listOf(
        "=== TRANSFER FILE ===",
        "1. File Manager Dual-Pane (X-plore style)",
        "   - 2 pane bersamaan di Mode TV, 1 pane di Mode HP",
        "   - Copy, Move, Rename, Delete, Compress, Extract",
        "   - Auto-play video, image viewer",
        "   - Vault enkripsi, Disk Map",
        "2. Server WiFi (port 23016)",
        "   - Auto-scan perangkat di WiFi yang sama",
        "   - NSD/mDNS discovery",
        "",
        "=== REMOTE TV (6 PROTOKOL) ===",
        "1. ADB Wi-Fi (port 5555) — Android TV, Fire TV",
        "2. Roku ECP (port 8060) — Roku TV",
        "3. Samsung Tizen (port 8001/8002) — Samsung Smart TV",
        "4. LG webOS SSAP (port 3000/3001) — LG Smart TV",
        "5. Philips JointSpace (port 1925/1926) — Philips TV",
        "6. Vizio SmartCast (port 9000) — Vizio TV",
        "",
        "=== UI REMOTE (5 MODE) ===",
        "1. D-Pad (default) — bulat seperti remote fisik",
        "2. Grid — angka 0-9 + tombol warna RGBY",
        "3. Mouse — touchpad kursor",
        "4. Gesture — swipe = D-Pad virtual",
        "5. Air Mouse — gyroscope",
        "",
        "=== TOMBOL REMOTE ===",
        "- Top Bar: Voice, Input, Cast, Keyboard, Copy, TV List",
        "- Nav Bar: Home, Back, Recent, Mute",
        "- Media Bar: Play, Pause, Stop, Rew, Fwd, Prev, Next",
        "- Volume Bar: Vol+, Vol-, Ch+, Ch- (dengan gesture)",
        "- Quick Bar: 5 mode switcher + Shortcut + Exit",
        "",
        "=== FITUR PENDUKUNG ===",
        "- Volume Monitor Overlay (tap 0-30, hold 0-100% via gesture)",
        "- Haptic Feedback (getar halus)",
        "- Pengaturan Remote (haptic, sound, sensitivitas, ukuran tombol)",
        "- Multi-TV Manager (simpan TV terdaftar di Room)",
        "- Auto-connect ke TV terakhir",
        "- Splash Screen + Onboarding 3 slide",
        "- Manual IP fallback (untuk TV tidak terdeteksi)",
        "- Backup Aman (14 bagian TXT)",
        "",
        "=== KEYSTORE PERMANEN ===",
        "- Auto-generate di GitHub Actions build pertama",
        "- Disimpan sebagai debug.keystore.base64",
        "- Update APK tanpa uninstall"
    )

    // ==========================================================
    // BAGIAN 8 — PENGATURAN USER (Aturan Detail)
    // ==========================================================
    val USER_PREFERENCES: List<String> = listOf(
        "=== PREFERENSI USER ===",
        "1. Kode harus KODE TIMPA FULL — bukan instruksi edit manual",
        "2. Kalau kode panjang → pecah jadi N bagian",
        "3. Kalau user bilang 'pecah jadi 3' → bagi 3 bagian",
        "4. User pakai GitHub Mobile (HP), JANGAN asumsi pakai PC",
        "5. Kalau ada yang salah → perbaiki, jangan ngotot",
        "6. Konfirmasi pemahaman SEBELUM eksekusi",
        "7. Kalau build error → tunggu error, jangan tebak",
        "",
        "=== ATURAN UI ===",
        "1. Tombol icon (bukan emoji)",
        "2. Long-press 1 detik → tooltip muncul",
        "3. Rainbow icon warna berputar smooth (Cyan→Lime→Yellow→Pink)",
        "4. Toolbar mode HP pindah kiri/kanan sesuai pane aktif",
        "5. Remote TV buka jendela BARU full screen (bukan popup)",
        "6. Dialog kecil untuk popup (Server WiFi, dll)",
        "",
        "=== ATURAN VOLUME ===",
        "1. Tap Vol+/Vol- → naik/turun 1 level (0-30), tanpa monitor",
        "2. Tahan Vol+/Vol- → monitor volume muncul, gesture bebas arah (0-100%)",
        "3. Geser kanan/atas = naik, geser kiri/bawah = turun",
        "4. Lepas tombol → monitor hilang",
        "5. Haptic getar halus saat volume berubah",
        "",
        "=== ATURAN REMOTE TV ===",
        "1. TV harus support protokol (Android TV, Roku, Samsung, LG, dll)",
        "2. TIDAK butuh install aplikasi di TV (kecuali pakai custom protocol)",
        "3. Kalau auto-scan gagal → pakai Manual IP",
        "4. Pairing code hanya untuk Android TV, Samsung, LG, Vizio",
        "5. Roku & Philips tidak butuh pairing"
    )

    // ==========================================================
    // BAGIAN 9 — ATURAN KHUSUS PROJECT INI
    // ==========================================================
    val PROJECT_SPECIFIC_RULES: List<String> = listOf(
        "=== ATURAN KHUSUS iOControll Tv ===",
        "1. Port WiFi Server: 23016 (JANGAN diubah)",
        "2. Port TV Remote: 6467 (Android TV v2) / 5555 (ADB)",
        "3. Package: dev.andikuneiocontroll (JANGAN diubah)",
        "4. Backup Aman path: /sdcard/IOremote TV/Backup Aman/",
        "5. Backup Aman harus update setiap build",
        "6. Format file backup: Backup Aman-iOControllTv-DD-MM-YYYY.TXT",
        "7. Backup berisi 14 bagian (Header, Pengaturan, Identitas, Memory, Struktur, Menu, Fitur, Riwayat Update, Riwayat Bug, Riwayat Error, Riwayat Build, Link GitHub, Aturan Keras, Full Source)",
        "",
        "=== ATURAN UPDATE KNOWLEDGE ===",
        "Setiap kali ada fitur/bug/perubahan baru:",
        "1. Tambah entri di UpdateHistory.kt",
        "2. Tambah di AppKnowledge.kt (FIXED_BUGS atau FEATURE_LIST)",
        "3. Tambah di BuildFailureHistory kalau build gagal",
        "4. Update version di AppKnowledge CURRENT_VERSION"
    )

    // ==========================================================
    // BAGIAN 10 — RIWAYAT BUG DIPERBAIKI
    // ==========================================================
    val FIXED_BUGS: List<String> = listOf(
        "[08-10-2026] WifiFileServerService.kt terpotong → restorasi full class",
        "[08-10-2026] Namespace com.example → migrasi ke dev.andikuneiocontroll",
        "[08-10-2026] META-INF/INDEX.LIST duplicate → packaging excludes",
        "[08-10-2026] Icon deprecated → ganti AutoMirrored",
        "[08-10-2026] Missing import height di RemoteTvDialog",
        "[08-10-2026] Keystore bentrok tiap build → keystore permanen",
        "[08-10-2026] Pane Kanan buka ConnectionPaneView → ganti FilePaneView",
        "[08-10-2026] Toolbar Mode HP tidak dinamis → MobileModeView adaptif",
        "[08-10-2026] Panah switch pane tidak berubah arah",
        "[08-10-2026] Dialog Server WiFi fullscreen → popup kecil",
        "[09-10-2026] Conflicting overloads RemoteTopBar/NavBar → hapus file lama",
        "[09-10-2026] Unresolved KeyboardArrowUp/Down → tambah import",
        "[09-10-2026] MutableState<Long> delegate error → mutableLongStateOf",
        "[09-10-2026] Unresolved weight di SizeButton → RowScope extension",
        "[09-10-2026] MainScreen pakai RemoteTvDialog lama → ganti versi baru",
        "[09-10-2026] Discovery tidak deteksi Google TV → Manual IP fallback",
        "[09-10-2026] Kurung tutup berlebih di MainScreen → hapus 1 '}'",
        "[09-10-2026] MainViewModel masih import file lama → hapus import",
        "[09-10-2026] File lama duplikat (RemoteTopBar, RemoteNavBar) → hapus"
    )

    // ==========================================================
    // BAGIAN 11 — RIWAYAT BUILD GAGAL
    // ==========================================================
    val BUILD_ERROR_HISTORY: List<String> = listOf(
        "#1     : gradlew tidak ada → pakai gradle langsung",
        "#2-#4  : Unresolved reference 'R' → migrasi namespace",
        "#5-#10 : META-INF duplicate → packaging excludes",
        "#11    : WifiFileServerService terpotong → timpa full",
        "#24    : Keystore bentrok → keystore permanen base64",
        "#27    : Missing import height → tambah import",
        "#33-34 : Conflicting overloads → hapus file duplikat",
        "#35    : KeyboardArrowUp/Down → tambah import",
        "#35b   : MutableState<Long> → mutableLongStateOf",
        "#35c   : weight unresolved → RowScope extension",
        "#36    : Top level declaration → hapus '}' berlebih",
        "#37    : RemoteClient/Server unresolved → hapus import"
    )

    // ==========================================================
    // BAGIAN 12 — ATURAN KERJA SAMA DENGAN AI BARU
    // ==========================================================
    val AI_HANDOVER_RULES: List<String> = listOf(
        "=== KALAU USER GANTI AI ===",
        "AI baru WAJIB:",
        "1. Baca AppKnowledge.kt terlebih dahulu",
        "2. Pahami struktur aplikasi + isi kode",
        "3. Ikuti aturan format respon",
        "4. Ikuti aturan anti-truncation",
        "5. Ikuti aturan link GitHub",
        "6. Konfirmasi dulu sebelum eksekusi",
        "7. Jangan menebak — tanya user kalau tidak tahu",
        "8. User pakai HP (GitHub Mobile) — JANGAN asumsi PC",
        "9. Kirim KODE TIMPA FULL, bukan instruksi edit manual",
        "10. Kalau user bilang 'pecah jadi N' → bagi jadi N bagian"
    )

    // ==========================================================
    // BAGIAN 13 — PENGATURAN USER (template statis)
    // ==========================================================
    val SETTINGS_MENU: List<String> = listOf(
        "1. Server WiFi (Transfer File)",
        "2. Remote TV (6 Protokol)",
        "3. Pengaturan Remote (haptic, sound, sensitivitas, ukuran tombol)",
        "4. Backup Aman",
        "5. Tentang Aplikasi"
    )
}
