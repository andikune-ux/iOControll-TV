package dev.andikuneiocontroll.util

import android.content.Context
import android.os.Build
import android.os.Environment
import dev.andikuneiocontroll.data.AppKnowledge
import dev.andikuneiocontroll.data.UpdateHistory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {

    private const val ROOT_FOLDER = "IOremote TV"
    private const val BACKUP_FOLDER = "Backup Aman"

    fun getBackupDir(): File {
        val root = Environment.getExternalStorageDirectory()
        return File(root, "$ROOT_FOLDER/$BACKUP_FOLDER")
    }

    fun generateFileName(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID"))
        return "Backup Aman-iOControllTv-${sdf.format(Date())}.TXT"
    }

    fun createBackup(context: Context): File? {
        return try {
            val dir = getBackupDir()
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, generateFileName())
            file.writeText(buildBackupContent(context))
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildBackupContent(context: Context): String {
        val now = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val sb = StringBuilder()

        // ==========================================
        // BAGIAN 1 — HEADER METADATA
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("BACKUP AMAN - ${AppKnowledge.APP_NAME}")
        sb.appendLine("============================================================")
        sb.appendLine("Tanggal Export  : $now")
        sb.appendLine("Versi Aplikasi  : ${AppKnowledge.CURRENT_VERSION}")
        sb.appendLine("Package Name    : ${AppKnowledge.PACKAGE_NAME}")
        sb.appendLine("Device          : ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("Android Version : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        sb.appendLine("============================================================")
        sb.appendLine()

        // ==========================================
        // BAGIAN 2 — PENGATURAN USER SAAT INI
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("PENGATURAN USER SAAT INI")
        sb.appendLine("============================================================")
        sb.appendLine("WiFi Server Port    : ${AppKnowledge.WIFI_SERVER_PORT}")
        sb.appendLine("Tema                : Dark (Stabilo)")
        sb.appendLine("Mode Default (HP)   : Pane Kiri (File Manager)")
        sb.appendLine("Orientasi           : Auto (Portrait / Landscape)")
        sb.appendLine("Auto-scan Device    : Aktif")
        sb.appendLine()

        // ==========================================
        // BAGIAN 3 — IDENTITAS PROYEK
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("IDENTITAS PROYEK")
        sb.appendLine("============================================================")
        sb.appendLine("Nama Aplikasi   : ${AppKnowledge.APP_NAME}")
        sb.appendLine("Platform        : Android (HP & TV)")
        sb.appendLine("Bahasa          : Kotlin")
        sb.appendLine("UI Framework    : Jetpack Compose + Material 3")
        sb.appendLine("Arsitektur      : MVVM")
        sb.appendLine("Package Name    : ${AppKnowledge.PACKAGE_NAME}")
        sb.appendLine("Repo GitHub     : ${AppKnowledge.REPO_URL}")
        sb.appendLine()

        // ==========================================
        // BAGIAN 4 — MEMORY KNOWLEDGE & INSTRUCTION
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("MEMORY KNOWLEDGE & INSTRUCTION")
        sb.appendLine("Untuk AI baru yang membaca backup ini")
        sb.appendLine("============================================================")
        AppKnowledge.MEMORY_KNOWLEDGE.forEach { sb.appendLine(it) }
        sb.appendLine()

        // ==========================================
        // BAGIAN 5 — STRUKTUR FOLDER
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("STRUKTUR FOLDER APLIKASI")
        sb.appendLine("============================================================")
        AppKnowledge.FOLDER_STRUCTURE.forEach { sb.appendLine(it) }
        sb.appendLine()

        // ==========================================
        // BAGIAN 6 — MENU SETTINGS
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("MENU SETTINGS")
        sb.appendLine("============================================================")
        AppKnowledge.SETTINGS_MENU.forEach { sb.appendLine(it) }
        sb.appendLine()

        // ==========================================
        // BAGIAN 7 — DAFTAR FITUR
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("DAFTAR FITUR APLIKASI")
        sb.appendLine("============================================================")
        AppKnowledge.FEATURE_LIST.forEach { sb.appendLine(it) }
        sb.appendLine()

        // ==========================================
        // BAGIAN 8 — RIWAYAT UPDATE
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("RIWAYAT UPDATE (VERSI)")
        sb.appendLine("============================================================")
        UpdateHistory.entries.forEach { entry ->
            sb.appendLine("┌─ ${entry.version} (${entry.date})")
            sb.appendLine("│ ${entry.title}")
            sb.appendLine("│")
            entry.changes.forEach { change ->
                sb.appendLine("│ • $change")
            }
            sb.appendLine("└─")
            sb.appendLine()
        }

        // ==========================================
        // BAGIAN 9 — RIWAYAT BUG DIPERBAIKI
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("RIWAYAT BUG DIPERBAIKI")
        sb.appendLine("============================================================")
        AppKnowledge.FIXED_BUGS.forEach { sb.appendLine("✅ $it") }
        sb.appendLine()

        // ==========================================
        // BAGIAN 10 — RIWAYAT ERROR + SOLUSI
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("RIWAYAT ERROR + SOLUSI")
        sb.appendLine("============================================================")
        AppKnowledge.ERROR_HISTORY.forEach { sb.appendLine(it) }
        sb.appendLine()

        // ==========================================
        // BAGIAN 11 — RIWAYAT BUILD GITHUB ACTIONS
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("RIWAYAT BUILD GITHUB ACTIONS")
        sb.appendLine("============================================================")
        sb.appendLine("Actions URL : ${AppKnowledge.REPO_ACTIONS}")
        sb.appendLine()
        AppKnowledge.BUILD_ERROR_HISTORY.forEach { sb.appendLine(it) }
        sb.appendLine()

        // ==========================================
        // BAGIAN 12 — LINK GITHUB & PATH FILE
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("LINK GITHUB & PATH FILE")
        sb.appendLine("============================================================")
        sb.appendLine("Repo Utama  : ${AppKnowledge.REPO_URL}")
        sb.appendLine("Actions     : ${AppKnowledge.REPO_ACTIONS}")
        sb.appendLine("RAW         : https://raw.githubusercontent.com/andikune-ux/iOControll-TV/main/")
        sb.appendLine("BLOB        : https://github.com/andikune-ux/iOControll-TV/blob/main/")
        sb.appendLine("LINK EDIT   : https://github.com/andikune-ux/iOControll-TV/edit/main/{path}")
        sb.appendLine("LINK NEW    : https://github.com/andikune-ux/iOControll-TV/new/main/{path}")
        sb.appendLine()
        sb.appendLine("PATH BACKUP AMAN:")
        sb.appendLine("/sdcard/${ROOT_FOLDER}/${BACKUP_FOLDER}/")
        sb.appendLine()

        // ==========================================
        // BAGIAN 13 — ATURAN KERAS
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("ATURAN KERAS (KEYSTORE, DLL)")
        sb.appendLine("============================================================")
        AppKnowledge.HARD_RULES.forEach { sb.appendLine("⚠️ $it") }
        sb.appendLine()

        // ==========================================
        // BAGIAN 14 — FULL SOURCE CODE (opsional)
        // ==========================================
        sb.appendLine("============================================================")
        sb.appendLine("FULL SOURCE CODE")
        sb.appendLine("============================================================")
        sb.appendLine("Untuk menghemat ukuran file, source code TIDAK disertakan.")
        sb.appendLine("Akses langsung di GitHub:")
        sb.appendLine(AppKnowledge.REPO_URL)
        sb.appendLine()

        // END
        sb.appendLine("============================================================")
        sb.appendLine("END OF BACKUP")
        sb.appendLine("============================================================")

        return sb.toString()
    }
}
