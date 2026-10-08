package com.example.filemanager

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.StorageCategoryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object FileManagerHelper {

    fun getDefaultStoragePath(): String {
        return Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
    }

    fun getSecondaryStoragePath(context: Context): String? {
        val externalDirs = context.getExternalFilesDirs(null)
        if (externalDirs.size > 1 && externalDirs[1] != null) {
            val fullPath = externalDirs[1].absolutePath
            val parts = fullPath.split("/Android")
            if (parts.isNotEmpty()) return parts[0]
        }
        return null
    }

    suspend fun listFiles(path: String, isApkOrZipMode: Boolean = false): List<FileItem> = withContext(Dispatchers.IO) {
        val file = File(path)
        if (path.endsWith(".zip", ignoreCase = true) || path.endsWith(".apk", ignoreCase = true)) {
            return@withContext listZipContents(file)
        }

        if (!file.exists() || !file.isDirectory) {
            return@withContext emptyList()
        }

        val fileList = file.listFiles() ?: return@withContext emptyList()
        val items = fileList.map { f ->
            val ext = f.extension.lowercase()
            val category = detectCategory(f.isDirectory, ext)
            val isArchive = ext in listOf("zip", "rar", "7z", "tar", "gz")
            val isApk = ext == "apk"

            FileItem(
                name = f.name,
                path = f.absolutePath,
                isDirectory = f.isDirectory,
                size = if (f.isDirectory) 0L else f.length(),
                lastModified = f.lastModified(),
                isArchive = isArchive,
                isAppPackage = isApk,
                extension = ext,
                category = category,
                isSelected = false
            )
        }

        // Folders first, then alphabetically
        items.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    fun listInstalledApps(context: Context): List<FileItem> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.mapNotNull { appInfo ->
            val apkFile = File(appInfo.sourceDir)
            if (apkFile.exists()) {
                val appLabel = pm.getApplicationLabel(appInfo).toString()
                FileItem(
                    name = "$appLabel (${appInfo.packageName}).apk",
                    path = apkFile.absolutePath,
                    isDirectory = false,
                    size = apkFile.length(),
                    lastModified = apkFile.lastModified(),
                    isArchive = true,
                    isAppPackage = true,
                    extension = "apk",
                    category = FileCategory.APK
                )
            } else null
        }.sortedBy { it.name.lowercase() }
    }

    private fun listZipContents(zipFile: File): List<FileItem> {
        val result = mutableListOf<FileItem>()
        try {
            val zf = ZipFile(zipFile)
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val ext = entry.name.substringAfterLast('.', "").lowercase()
                result.add(
                    FileItem(
                        name = entry.name,
                        path = "${zipFile.absolutePath}!/${entry.name}",
                        isDirectory = entry.isDirectory,
                        size = entry.size,
                        lastModified = entry.time,
                        isArchive = false,
                        isAppPackage = false,
                        extension = ext,
                        category = detectCategory(entry.isDirectory, ext)
                    )
                )
            }
            zf.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    private fun detectCategory(isDirectory: Boolean, ext: String): FileCategory {
        if (isDirectory) return FileCategory.FOLDER
        return when (ext) {
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "ts" -> FileCategory.VIDEO
            "mp3", "m4a", "aac", "wav", "flac", "ogg", "wma" -> FileCategory.AUDIO
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> FileCategory.IMAGE
            "zip", "rar", "7z", "tar", "gz", "bz2" -> FileCategory.ARCHIVE
            "apk", "xapk", "apks" -> FileCategory.APK
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub" -> FileCategory.DOCUMENT
            "json", "xml", "kt", "java", "html", "css", "js", "py", "c", "cpp" -> FileCategory.CODE
            else -> FileCategory.OTHER
        }
    }

    suspend fun copyOrMove(
        sources: List<File>,
        targetDir: File,
        isMove: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        if (!targetDir.exists()) {
            val created = targetDir.mkdirs()
            if (!created && !targetDir.exists()) return@withContext false
        }
        var allSuccess = true
        for (src in sources) {
            val dest = File(targetDir, src.name)
            if (src.isDirectory) {
                val ok = copyDirectory(src, dest)
                if (ok && isMove) {
                    src.deleteRecursively()
                }
                if (!ok) allSuccess = false
            } else {
                try {
                    src.inputStream().use { input ->
                        dest.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (isMove) {
                        src.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    allSuccess = false
                }
            }
        }
        allSuccess
    }

    private fun copyDirectory(sourceDir: File, targetDir: File): Boolean {
        if (!targetDir.exists()) targetDir.mkdirs()
        val files = sourceDir.listFiles() ?: return true
        for (file in files) {
            val targetFile = File(targetDir, file.name)
            if (file.isDirectory) {
                if (!copyDirectory(file, targetFile)) return false
            } else {
                try {
                    file.inputStream().use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    return false
                }
            }
        }
        return true
    }

    suspend fun createFolder(parent: File, name: String): Boolean = withContext(Dispatchers.IO) {
        val newFolder = File(parent, name)
        newFolder.mkdirs()
    }

    suspend fun deleteFiles(files: List<File>): Boolean = withContext(Dispatchers.IO) {
        var allDeleted = true
        for (f in files) {
            val ok = if (f.isDirectory) f.deleteRecursively() else f.delete()
            if (!ok) allDeleted = false
        }
        allDeleted
    }

    suspend fun renameFile(target: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        val dest = File(target.parentFile, newName)
        target.renameTo(dest)
    }

    suspend fun batchRename(files: List<File>, prefix: String, suffix: String): Int = withContext(Dispatchers.IO) {
        var renamedCount = 0
        files.forEachIndexed { index, file ->
            val ext = if (file.extension.isNotEmpty()) ".${file.extension}" else ""
            val base = file.nameWithoutExtension
            val newName = "$prefix$base$suffix$ext"
            val dest = File(file.parentFile, newName)
            if (file.renameTo(dest)) {
                renamedCount++
            }
        }
        renamedCount
    }

    suspend fun compressToZip(sources: List<File>, destZip: File): Boolean = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(FileOutputStream(destZip)).use { zos ->
                for (src in sources) {
                    addToZip(src, src.name, zos)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addToZip(file: File, entryName: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            val files = file.listFiles() ?: return
            for (child in files) {
                addToZip(child, "$entryName/${child.name}", zos)
            }
        } else {
            val entry = ZipEntry(entryName)
            zos.putNextEntry(entry)
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }

    suspend fun extractZip(zipFile: File, targetDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val destPath = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        destPath.mkdirs()
                    } else {
                        destPath.parentFile?.mkdirs()
                        FileOutputStream(destPath).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun readText(file: File): String = withContext(Dispatchers.IO) {
        try {
            file.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Gagal membaca teks: ${e.message}"
        }
    }

    suspend fun readHex(file: File, maxBytes: Int = 2048): String = withContext(Dispatchers.IO) {
        try {
            val buffer = ByteArray(maxBytes)
            val bytesRead = file.inputStream().use { it.read(buffer) }
            if (bytesRead <= 0) return@withContext "File kosong"
            
            val sb = StringBuilder()
            for (i in 0 until bytesRead step 16) {
                sb.append(String.format("%08X  ", i))
                for (j in 0 until 16) {
                    if (i + j < bytesRead) {
                        sb.append(String.format("%02X ", buffer[i + j]))
                    } else {
                        sb.append("   ")
                    }
                    if (j == 7) sb.append(" ")
                }
                sb.append(" |")
                for (j in 0 until 16) {
                    if (i + j < bytesRead) {
                        val b = buffer[i + j].toInt().toChar()
                        sb.append(if (b in ' '..'~') b else '.')
                    }
                }
                sb.append("|\n")
            }
            sb.toString()
        } catch (e: Exception) {
            "Gagal membaca file: ${e.message}"
        }
    }

    // Vault AES-256 Encryption / Decryption
    suspend fun encryptToVault(sourceFiles: List<File>, destVault: File, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // First compress to a temporary zip
            val tempZip = File(destVault.parentFile, ".temp_${System.currentTimeMillis()}.zip")
            val compressed = compressToZip(sourceFiles, tempZip)
            if (!compressed) return@withContext false

            val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }

            val keySpec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

            val zipBytes = tempZip.readBytes()
            val encryptedBytes = cipher.doFinal(zipBytes)

            FileOutputStream(destVault).use { fos ->
                fos.write("VAULT256".toByteArray()) // magic header
                fos.write(salt)
                fos.write(iv)
                fos.write(encryptedBytes)
            }
            tempZip.delete()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun decryptFromVault(vaultFile: File, targetDir: File, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileBytes = vaultFile.readBytes()
            val magic = String(fileBytes.sliceArray(0..7))
            if (magic != "VAULT256") return@withContext false

            val salt = fileBytes.sliceArray(8..23)
            val iv = fileBytes.sliceArray(24..35)
            val cipherBytes = fileBytes.sliceArray(36 until fileBytes.size)

            val keySpec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val decryptedZip = cipher.doFinal(cipherBytes)

            val tempZip = File(targetDir, ".temp_unvault_${System.currentTimeMillis()}.zip")
            tempZip.writeBytes(decryptedZip)
            val ok = extractZip(tempZip, targetDir)
            tempZip.delete()
            ok
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Disk Space Map Calculation
    suspend fun analyzeStorage(root: File): List<StorageCategoryInfo> = withContext(Dispatchers.IO) {
        var videoBytes = 0L
        var videoCount = 0
        var audioBytes = 0L
        var audioCount = 0
        var imageBytes = 0L
        var imageCount = 0
        var apkBytes = 0L
        var apkCount = 0
        var docBytes = 0L
        var docCount = 0
        var archiveBytes = 0L
        var archiveCount = 0
        var otherBytes = 0L
        var otherCount = 0

        fun scan(f: File, depth: Int) {
            if (depth > 6) return
            val files = f.listFiles() ?: return
            for (child in files) {
                if (child.isDirectory) {
                    if (!child.name.startsWith(".")) {
                        scan(child, depth + 1)
                    }
                } else {
                    val len = child.length()
                    when (detectCategory(false, child.extension.lowercase())) {
                        FileCategory.VIDEO -> { videoBytes += len; videoCount++ }
                        FileCategory.AUDIO -> { audioBytes += len; audioCount++ }
                        FileCategory.IMAGE -> { imageBytes += len; imageCount++ }
                        FileCategory.APK -> { apkBytes += len; apkCount++ }
                        FileCategory.DOCUMENT -> { docBytes += len; docCount++ }
                        FileCategory.ARCHIVE -> { archiveBytes += len; archiveCount++ }
                        else -> { otherBytes += len; otherCount++ }
                    }
                }
            }
        }

        scan(root, 0)

        listOf(
            StorageCategoryInfo("Video", videoBytes, 0xFFE11D48, videoCount),
            StorageCategoryInfo("Audio", audioBytes, 0xFF06B6D4, audioCount),
            StorageCategoryInfo("Foto/Gambar", imageBytes, 0xFF10B981, imageCount),
            StorageCategoryInfo("Aplikasi (APK)", apkBytes, 0xFFF59E0B, apkCount),
            StorageCategoryInfo("Dokumen", docBytes, 0xFF3B82F6, docCount),
            StorageCategoryInfo("Arsip (ZIP/RAR)", archiveBytes, 0xFF8B5CF6, archiveCount),
            StorageCategoryInfo("Lainnya", otherBytes, 0xFF64748B, otherCount)
        )
    }

    fun shareFiles(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        val uris = files.map { file ->
            FileProvider.getUriForFile(
                context,
                "dev.andikuneiocontroll.fileprovider",
                file
            )
        }

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(intent, "Bagikan Berkas via iOControll")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
