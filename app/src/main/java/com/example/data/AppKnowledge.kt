package dev.andikuneiocontroll.data

/**
 * Memory Knowledge & Instruction — iOControll Tv
 *
 * File ini berisi SEMUA memory, aturan kerja, dan informasi penting
 * yang diberikan user (Andi) ke AI selama pengerjaan project.
 *
 * === PESAN UNTUK AI ASSISTANT SELANJUTNYA ===
 * Kalau AI saat ini kena limit dan digantikan AI baru:
 * 1. BACA file AppKnowledge.kt + UpdateHistory.kt INI DULU sebelum kerja
 * 2. Ikuti SEMUA aturan di file ini (FORMAT_RULES, WORK_RULES, HARD_RULES)
 * 3. User pakai GitHub Mobile (HP), JANGAN asumsi PC
 * 4. Kirim KODE TIMPA FULL — bukan instruksi edit manual
 * 5. Batas paste di GitHub Chrome HP: MAX 400 baris / 12.000 karakter per bubble
 * 6. Kalau kode >400 baris → PECAH jadi beberapa BAGIAN (judul: "BAGIAN 1 DARI N")
 * 7. Setelah user paste, MINTA user scroll cek akhir file — pastikan kurung tutup lengkap
 * 8. Kalau build gagal — tunggu error dari user, JANGAN asumsi
 * 9. Update UpdateHistory.kt + AppKnowledge.kt SETIAP kali ada perubahan
 * 10. KERJAKAN PER KATEGORI FILE, bukan per fitur
 * =============================================
 */
object AppKnowledge {

    // ==========================================================
    // IDENTITAS APLIKASI
    // ==========================================================
    const val APP_NAME = "iOControll Tv"
    const val PACKAGE_NAME = "dev.andikuneiocontroll"
    const val REPO_URL = "https://github.com/andikune-ux/iOControll-TV"
    const val REPO_ACTIONS = "https://github.com/andikune-ux/iOControll-TV/actions"
    const val CURRENT_VERSION = "V1.00.004"
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
    const val USER_WORK_MODE = "GitHub Mobile (Chrome HP — BUKAN PC)"
    const val USER_EDITOR_LIMIT = "Max 400 baris / 12.000 karakter per paste"

    // ==========================================================
    // PESAN UNTUK AI SELANJUTNYA
    // ==========================================================
    val MESSAGE_FOR_NEXT_AI: List<String> = listOf(
        "=== PESAN UNTUK AI ASSISTANT SELANJUTNYA ===",
        "Halo AI baru. Kamu melanjutkan project 'iOControll Tv' milik user Andi.",
        "",
        "ATURAN WAJIB:",
        "1. Baca AppKnowledge.kt + UpdateHistory.kt SEBELUM mulai kerja",
        "2. Ikuti FORMAT_RULES, ANTI_TRUNCATION_RULES, WORK_RULES, HARD_RULES",
        "3. User pakai GitHub Mobile di HP (Chrome) — JANGAN asumsi PC",
        "4. Kirim KODE TIMPA FULL — bukan instruksi edit baris-per-baris",
        "5. Batas paste: MAX 400 baris / 12.000 karakter per bubble",
        "6. Kode >400 baris → PECAH jadi beberapa BAGIAN",
        "7. Setelah paste, cek akhir file — pastikan kurung tutup lengkap",
        "8. Kalau build gagal — tunggu error dari user, JANGAN asumsi",
        "9. Update UpdateHistory.kt + AppKnowledge.kt SETIAP ada perubahan",
        "10. KERJAKAN PER KATEGORI FILE, bukan per fitur",
        "",
        "SISA PEKERJAAN (per V1.00.004 — 10-10-2026):",
        "- Build test v1.00.004 belum dijalankan",
        "- Test APK ke TV Xiaomi (192.168.0.103)",
        "- Aktifkan Developer Options + Wireless Debugging di TV Xiaomi",
        "- Kalau ADB gagal → implementasi Android TV Remote v2 (TLS + protobuf)",
        "- Test semua fitur remote",
        "",
        "FITUR PLACEHOLDER:",
        "- Copy Text dari TV (return null)",
        "- Screen Cast (MediaProjection placeholder)",
        "- Firmware Info (hanya ADB support)"
    )

    // ==========================================================
    // BAGIAN 1 — ATURAN FORMAT RESPON
    // ==========================================================
    val FORMAT_RULES: List<String> = listOf(
        "=== ATURAN FORMAT RESPON ===",
        "1. Visualisasi folder (emoji + indentasi)",
        "2. Path lengkap file",
        "3. Nama file",
        "4. URL edit / URL new",
        "5. Kode timpa FULL",
        "6. Pesan commit",
        "",
        "ATURAN:",
        "- Pakai code block untuk path/URL/nama file",
        "- Bahasa Indonesia yang mudah dipahami",
        "- 1 chat = 1 fitur utuh kalau muat",
        "- Kalau tidak muat 1 bubble → tulis 'jangan commit dulu'",
        "- JANGAN nolak buka link GitHub publik",
        "- JANGAN gabung link 1 file dengan link lain",
        "- File >400 baris → bagi jadi 3-7 BAGIAN kecil",
        "- User pakai HP — JANGAN kasih instruksi PC",
        "- User minta KODE TIMPA FULL"
    )

    // ==========================================================
    // BAGIAN 2 — ATURAN ANTI-TRUNCATION
    // ==========================================================
    val ANTI_TRUNCATION_RULES: List<String> = listOf(
        "=== ATURAN ANTI-TRUNCATION ===",
        "BATAS PASTE GitHub Chrome HP: MAX 400 baris / 12.000 karakter per bubble",
        "",
        "1. Judul: 'BAGIAN 1 DARI N'",
        "2. Akhir: '(lanjut di BAGIAN berikutnya)'",
        "3. Awal: '(sambungan dari BAGIAN sebelumnya)'",
        "4. JANGAN potong di tengah fungsi",
        "5. JANGAN bilang 'kode dilanjut di chat berikutnya'",
        "6. Paste gagal di HP → bagi jadi lebih banyak BAGIAN",
        "7. File >400 baris → otomatis bagi jadi beberapa BAGIAN",
        "8. Saat user ketik 'lanjut' → kirim bagian berikutnya",
        "9. Tutup kurung harus PAS — kelebihan 1 '}' bikin error build",
        "10. Setelah paste, cek akhir file — pastikan kurung tutup lengkap"
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
        "9. Kalau user bilang 'timpa full' — kirim FULL kode",
        "10. Kalau user bilang 'pecah jadi N' — bagi kode jadi N bagian",
        "11. KERJAKAN PER KATEGORI FILE, bukan per fitur"
    )

    // ==========================================================
    // BAGIAN 4 — ATURAN SIGNATURE / KEYSTORE
    // ==========================================================
    val HARD_RULES: List<String> = listOf(
        "=== ATURAN KERAS SIGNATURE / KEYSTORE ===",
        "JANGAN PERNAH ubah:",
        "1. debug.keystore.base64",
        "2. signingConfigs di app/build.gradle.kts",
        "3. keystore di build.yml",
        "",
        "ATURAN INSTALL APK:",
        "- SELALU install dari GitHub Actions Artifacts",
        "- JANGAN install APK dari AI Studio",
        "",
        "ATURAN KALAU PAKAI AI STUDIO:",
        "- JANGAN ubah debug.keystore, signingConfigs, workflow signing",
        "- AI Studio cuma boleh EDIT kode Kotlin/XML",
        "- Build tetap via GitHub Actions"
    )

    // ==========================================================
    // BAGIAN 5 — URL REPO
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
// BAGIAN 6 — STRUKTUR FOLDER
// ==========================================================
val FOLDER_STRUCTURE: List<String> = listOf(
    "app/src/main/java/dev/andikuneiocontroll/",
    "├── MainActivity.kt          (flow Splash → Onboarding → Main)",
    "├── MainViewModel.kt         (viewmodel utama + autoConnectLastTv)",
    "├── data/local/              (Room + DataStore)",
    "│   ├── TvEntity.kt          (+ hasChromecast + firmwareVersion)",
    "│   ├── TvDao.kt",
    "│   ├── TvDatabase.kt",
    "│   ├── TvRepository.kt      (+ hasChromecast)",
    "│   └── PrefsRepository.kt",
    "├── data/                    (knowledge & history)",
    "│   ├── AppKnowledge.kt      (file ini)",
    "│   └── UpdateHistory.kt     (+ buildFailureHistory)",
    "├── filemanager/FileManagerHelper.kt",
    "├── model/Models.kt",
    "├── remote/",
    "│   ├── controller/",
    "│   │   ├── RemoteController.kt      (+ pair + copyText + rotateScreen)",
    "│   │   └── RemoteControllers.kt     (Voice, Keyboard, Mouse, Cast, Shortcut)",
    "│   ├── discovery/",
    "│   │   ├── DiscoveredTv.kt          (+ hasChromecast)",
    "│   │   ├── MdnsDiscovery.kt         (deteksi _googlecast)",
    "│   │   ├── SsdpDiscovery.kt",
    "│   │   ├── TvDiscoveryManager.kt",
    "│   │   └── TvFilter.kt",
    "│   └── protocol/",
    "│       ├── TvProtocol.kt            (+ pair + COPY_TEXT + ROTATE_SCREEN)",
    "│       ├── ProtocolDetector.kt",
    "│       ├── adb/",
    "│       │   ├── AdbProtocol.kt",
    "│       │   ├── AdbCrypto.kt",
    "│       │   ├── AdbPairing.kt        (BARU — Wireless Debugging pairing)",
    "│       │   ├── AdbClient.kt         (+ pair support)",
    "│       │   └── AdbTvClient.kt       (+ needsPairing flag)",
    "│       ├── roku/RokuEcpClient.kt",
    "│       ├── samsung/SamsungTizenClient.kt",
    "│       ├── lg/LgWebOsClient.kt",
    "│       ├── philips/PhilipsClient.kt",
    "│       └── vizio/VizioClient.kt",
    "├── server/ (WifiFileServerService, WifiHttpServer, DiscoveryManager)",
    "├── ui/",
    "│   ├── MainScreen.kt        (FINAL — wire semua dialog)",
    "│   ├── SplashAndOnboarding.kt",
    "│   ├── remote/",
    "│   │   ├── RemoteTvDialog.kt        (+ onOpenVoice + onOpenCopy)",
    "│   │   ├── components/",
    "│   │   │   ├── HapticHelper.kt",
    "│   │   │   ├── RemoteButton.kt",
    "│   │   │   ├── RemoteHeader.kt      (+ onNameClick)",
    "│   │   │   ├── RemoteDPad.kt",
    "│   │   │   ├── RemoteBars.kt",
    "│   │   │   └── RemoteModes.kt",
    "│   │   ├── dialogs/",
    "│   │   │   ├── TvDialogs.kt         (Picker, Pairing, Info, Input, Shortcut, ManualIp)",
    "│   │   │   ├── AdbPairingDialog.kt  (BARU)",
    "│   │   │   ├── VoiceDialog.kt       (BARU)",
    "│   │   │   └── KeyboardDialog.kt    (BARU)",
    "│   │   └── settings/RemoteSettingsScreen.kt",
    "│   ├── filemanager/",
    "│   ├── permissions/",
    "│   └── theme/",
    "├── util/BackupHelper.kt",
    "└── viewers/Viewers.kt"
)

// ==========================================================
// BAGIAN 7 — DAFTAR FITUR LENGKAP
// ==========================================================
val FEATURE_LIST: List<String> = listOf(
    "=== TRANSFER FILE ===",
    "1. File Manager Dual-Pane (X-plore style)",
    "2. Server WiFi (port 23016) + NSD/mDNS discovery",
    "3. Copy, Move, Rename, Delete, Compress, Extract",
    "4. Auto-play video, Image viewer, Vault, Disk Map",
    "",
    "=== REMOTE TV (6 PROTOKOL) ===",
    "1. ADB Wi-Fi (5555) + Wireless Debugging pairing (Android 11+)",
    "2. Roku ECP (8060)",
    "3. Samsung Tizen (8001/8002)",
    "4. LG webOS SSAP (3000/3001)",
    "5. Philips JointSpace (1925/1926)",
    "6. Vizio SmartCast (9000)",
    "",
    "=== UI REMOTE (5 MODE) ===",
    "1. D-Pad (default)",
    "2. Grid (angka 0-9 + RGBY)",
    "3. Mouse (touchpad)",
    "4. Gesture (swipe = D-Pad)",
    "5. Air Mouse (gyroscope)",
    "",
    "=== TOMBOL REMOTE ===",
    "- Top Bar: Voice, Input, Cast, Keyboard, Copy, TV List",
    "- Nav Bar: Home, Back, Recent, Mute",
    "- Media Bar: Play, Pause, Stop, Rew, Fwd, Prev, Next",
    "- Volume Bar: Vol+, Vol-, Ch+, Ch- (dengan gesture)",
    "- Quick Bar: 5 mode switcher + Shortcut + Exit",
    "",
    "=== FITUR PENDUKUNG ===",
    "- ADB Wireless Debugging Pairing",
    "- Voice Input (Google Voice → TV)",
    "- Keyboard Input (IME → TV)",
    "- Chromecast Detection (_googlecast._tcp)",
    "- Firmware Info (ADB)",
    "- Screen Rotate Command",
    "- Copy Text dari TV (placeholder)",
    "- Volume Monitor Overlay",
    "- Haptic Feedback",
    "- Remote Settings",
    "- Multi-TV Manager (Room)",
    "- Auto-connect TV terakhir",
    "- Splash + Onboarding",
    "- Manual IP Fallback",
    "- Backup Aman (14 bagian)"
)

// ==========================================================
// BAGIAN 8 — PREFERENSI USER
// ==========================================================
val USER_PREFERENCES: List<String> = listOf(
    "=== PREFERENSI USER ===",
    "1. Kode harus KODE TIMPA FULL — bukan instruksi edit manual",
    "2. Kalau kode panjang → pecah jadi N bagian",
    "3. User pakai GitHub Mobile (HP Chrome), JANGAN asumsi PC",
    "4. Batas paste di GitHub Chrome HP: MAX 400 baris / 12.000 karakter",
    "5. KERJAKAN PER KATEGORI FILE — bukan per fitur",
    "6. Kalau ada yang salah → perbaiki, jangan ngotot",
    "7. Konfirmasi pemahaman SEBELUM eksekusi",
    "8. Kalau build error → tunggu error, jangan tebak",
    "",
    "=== ATURAN UI ===",
    "1. Tombol icon (bukan emoji)",
    "2. Long-press 1 detik → tooltip muncul",
    "3. Rainbow icon warna berputar smooth",
    "4. Toolbar mode HP pindah kiri/kanan sesuai pane",
    "5. Remote TV buka jendela BARU full screen",
    "",
    "=== ATURAN VOLUME ===",
    "1. Tap Vol+/Vol- → naik/turun 1 level (0-30)",
    "2. Tahan Vol+/Vol- → monitor volume + gesture bebas arah (0-100%)",
    "3. Geser kanan/atas = naik, geser kiri/bawah = turun",
    "4. Lepas tombol → monitor hilang"
)

// ==========================================================
// BAGIAN 9 — ATURAN PROJECT
// ==========================================================
val PROJECT_SPECIFIC_RULES: List<String> = listOf(
    "=== ATURAN KHUSUS iOControll Tv ===",
    "1. Port WiFi Server: 23016 (JANGAN diubah)",
    "2. Port TV Remote: 6467 (Android TV v2) / 5555 (ADB)",
    "3. Package: dev.andikuneiocontroll (JANGAN diubah)",
    "4. Backup Aman path: /sdcard/IOremote TV/Backup Aman/",
    "5. Backup Aman format: Backup Aman-iOControllTv-DD-MM-YYYY.TXT",
    "6. Backup berisi 14 bagian",
    "7. Update knowledge SETIAP build",
    "8. TIDAK ADA fitur nice-to-have yang akan dikerjakan"
)
    // ==========================================================
    // BAGIAN 10 — RIWAYAT BUG DIPERBAIKI
    // ==========================================================
    val FIXED_BUGS: List<String> = listOf(
        "[08-10-2026] WifiFileServerService.kt terpotong → restorasi",
        "[08-10-2026] Namespace com.example → dev.andikuneiocontroll",
        "[08-10-2026] META-INF/INDEX.LIST duplicate → packaging excludes",
        "[08-10-2026] Icon deprecated → AutoMirrored",
        "[08-10-2026] Missing import height",
        "[08-10-2026] Keystore bentrok → permanen base64",
        "[08-10-2026] Pane Kanan ConnectionPaneView → FilePaneView",
        "[08-10-2026] Toolbar Mode HP tidak dinamis",
        "[08-10-2026] Panah switch pane tidak berubah arah",
        "[08-10-2026] Dialog Server WiFi fullscreen → popup",
        "[09-10-2026] Conflicting overloads RemoteTopBar/NavBar",
        "[09-10-2026] Unresolved KeyboardArrowUp/Down",
        "[09-10-2026] MutableState<Long> → mutableLongStateOf",
        "[09-10-2026] Unresolved 'weight' → RowScope extension",
        "[09-10-2026] MainScreen pakai RemoteTvDialog lama",
        "[09-10-2026] Discovery tidak deteksi Google TV → Manual IP",
        "[09-10-2026] Kurung tutup berlebih di MainScreen",
        "[09-10-2026] MainViewModel masih import file lama",
        "[09-10-2026] File lama duplikat (RemoteTopBar, RemoteNavBar)",
        "[10-10-2026] ADB Pairing untuk Google TV → libadb-android",
        "[10-10-2026] Voice Input placeholder → full implementation",
        "[10-10-2026] Keyboard Input placeholder → full implementation",
        "[10-10-2026] Chromecast detection via mDNS _googlecast",
        "[10-10-2026] libadb-android group salah → ganti ke JitPack com.github.MuntashirAkon",
        "[10-10-2026] 'pair' hides member TvProtocol → tambah override modifier",
        "[10-10-2026] Kurung tutup berlebih di AppKnowledge.kt → timpa full"
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
        "#37    : RemoteClient/Server unresolved → hapus import",
        "#38    : libadb-android tidak ditemukan → tambah JitPack repo + fix group",
        "#38b   : 'pair' override + AdbPairingDialog 404 + AppKnowledge MEMORY_KNOWLEDGE",
        "#38c   : AppKnowledge kurung tutup berlebih → timpa full"
    )

    // ==========================================================
    // BAGIAN 12 — ATURAN AI HANDOVER
    // ==========================================================
    val AI_HANDOVER_RULES: List<String> = listOf(
        "=== KALAU USER GANTI AI ===",
        "AI baru WAJIB:",
        "1. Baca AppKnowledge.kt + UpdateHistory.kt terlebih dahulu",
        "2. Pahami struktur aplikasi + isi kode",
        "3. Ikuti FORMAT_RULES, ANTI_TRUNCATION_RULES, WORK_RULES",
        "4. User pakai HP (GitHub Mobile) — JANGAN asumsi PC",
        "5. Kirim KODE TIMPA FULL, bukan instruksi edit manual",
        "6. Batas paste GitHub Chrome HP: MAX 400 baris / 12.000 karakter",
        "7. Kalau kode >400 baris → PECAH jadi beberapa BAGIAN",
        "8. Konfirmasi dulu sebelum eksekusi",
        "9. Jangan menebak — tanya user kalau tidak tahu",
        "10. Update UpdateHistory.kt + AppKnowledge.kt SETIAP ada perubahan",
        "11. KERJAKAN PER KATEGORI FILE, bukan per fitur"
    )

    // ==========================================================
    // BAGIAN 13 — SISA PEKERJAAN
    // ==========================================================
    val PENDING_WORK: List<String> = listOf(
        "=== SISA PEKERJAAN V1.00.004 ===",
        "",
        "WAJIB (belum dikerjakan):",
        "1. Build test v1.00.004 (setelah commit AppKnowledge + AdbPairingDialog)",
        "2. Test APK ke TV Xiaomi (IP: 192.168.0.103)",
        "3. Aktifkan Developer Options + Wireless Debugging di TV Xiaomi",
        "4. Kalau ADB gagal → implementasi Android TV Remote v2 (TLS + protobuf)",
        "5. Test semua fitur (D-Pad, Volume, Mouse, Voice, Keyboard)",
        "",
        "PLACEHOLDER (UI ada, fungsi belum jalan):",
        "- Copy Text dari TV (RemoteController.copyTextFromTv return null)",
        "- Screen Cast (MediaProjection placeholder)",
        "- Firmware Info (hanya ADB support)"
    )

    // ==========================================================
    // BAGIAN 14 — PENGATURAN USER
    // ==========================================================
    val SETTINGS_MENU: List<String> = listOf(
        "1. Server WiFi (Transfer File)",
        "2. Remote TV (6 Protokol + ADB Pairing)",
        "3. Pengaturan Remote (haptic, sound, sensitivitas, ukuran tombol)",
        "4. Backup Aman",
        "5. Tentang Aplikasi"
    )

    // ==========================================================
    // COMPATIBILITY ALIASES (untuk BackupHelper)
    // ==========================================================
    val MEMORY_KNOWLEDGE: List<String> =
        MESSAGE_FOR_NEXT_AI +
        FORMAT_RULES +
        ANTI_TRUNCATION_RULES +
        WORK_RULES +
        HARD_RULES +
        AI_HANDOVER_RULES

    val ERROR_HISTORY: List<String> =
        FIXED_BUGS +
        BUILD_ERROR_HISTORY
}
