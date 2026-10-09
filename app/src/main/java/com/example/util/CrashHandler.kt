package dev.andikuneiocontroll.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CrashHandler — Tangkap crash, simpan log ke file, tampil saat app dibuka lagi.
 *
 * Cara kerja:
 * 1. Install di Application.onCreate() atau MainActivity.onCreate()
 * 2. Saat app crash → simpan stack trace ke filesDir/crashes/crash_TIMESTAMP.txt
 * 3. Saat app dibuka lagi → cek file crash, kalau ada → tampil dialog
 */
class CrashHandler(
    private val context: Context,
    private val previousHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashLog(throwable)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val dir = File(context.filesDir, "crashes")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "crash_$timestamp.txt")
        file.writeText(buildLogContent(throwable, timestamp))
    }

    private fun buildLogContent(throwable: Throwable, timestamp: String): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()

        return buildString {
            appendLine("========================================")
            appendLine("CRASH LOG — iOControll Tv")
            appendLine("========================================")
            appendLine("Tanggal : $timestamp")
            appendLine("Device  : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Package : ${context.packageName}")
            appendLine("========================================")
            appendLine()
            appendLine("STACK TRACE:")
            appendLine(sw.toString())
            appendLine()
            appendLine("========================================")
            appendLine("END OF CRASH LOG")
            appendLine("========================================")
        }
    }

    companion object {
        fun getCrashDir(context: Context): File = File(context.filesDir, "crashes")

        fun getLatestCrashFile(context: Context): File? {
            val dir = getCrashDir(context)
            if (!dir.exists()) return null
            return dir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("crash_") }
                ?.maxByOrNull { it.lastModified() }
        }

        fun getAllCrashFiles(context: Context): List<File> {
            val dir = getCrashDir(context)
            if (!dir.exists()) return emptyList()
            return dir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("crash_") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }

        fun deleteCrashFile(file: File): Boolean {
            return try {
                file.delete()
            } catch (e: Exception) {
                false
            }
        }

        fun deleteAllCrashFiles(context: Context): Boolean {
            return try {
                getCrashDir(context).listFiles()?.forEach { it.delete() }
                true
            } catch (e: Exception) {
                false
            }
        }

        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(context.applicationContext)
            )
        }
    }
}
