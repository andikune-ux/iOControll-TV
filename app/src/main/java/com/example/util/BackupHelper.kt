package dev.andikuneiocontroll.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {

    private const val BACKUP_DIR_NAME = "IOremote TV"
    private const val BACKUP_SUBDIR = "Backup Aman"

    fun getBackupDir(): File {
        val root = Environment.getExternalStorageDirectory()
        return File(root, "$BACKUP_DIR_NAME/$BACKUP_SUBDIR")
    }

    fun generateBackupFileName(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale("id", "ID"))
        return "Backup Aman-iOControllTv-${sdf.format(Date())}.TXT"
    }

    fun createBackup(context: Context): File? {
        return try {
            val dir = getBackupDir()
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, generateBackupFileName())

            val content = buildString {
                appendLine("============================================================")
                appendLine("BACKUP AMAN - iOControll Tv")
                appendLine("============================================================")
                appendLine("Tanggal Export  : ${SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
                appendLine("Versi Aplikasi  : V1.00.000")
                appendLine("Package Name    : dev.andikuneiocontroll")
                appendLine("============================================================")
                appendLine()
                appendLine("PENGATURAN USER SAAT INI")
                appendLine("============================================================")
                appendLine("- Server WiFi Port : 23016")
                appendLine("- Server Aktif     : (cek aplikasi)")
                appendLine()
                appendLine("============================================================")
                appendLine("FILE & STRUKTUR")
                appendLine("============================================================")
                appendLine("Source Code : https://github.com/andikune-ux/iOControll-TV")
                appendLine()
                appendLine("============================================================")
                appendLine("END OF BACKUP")
                appendLine("============================================================")
            }

            file.writeText(content)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
