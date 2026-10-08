package com.example.model

import java.io.File

enum class FileCategory {
    FOLDER, VIDEO, AUDIO, IMAGE, ARCHIVE, APK, DOCUMENT, CODE, OTHER
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val isArchive: Boolean = false,
    val isAppPackage: Boolean = false,
    val extension: String = "",
    val category: FileCategory = FileCategory.OTHER,
    val isSelected: Boolean = false
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "Folder"
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.2f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$size B"
            }
        }
}

data class DevicePeer(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int = 23016,
    val isSaved: Boolean = false,
    val customLabel: String = "",
    val defaultPath: String = "/storage/emulated/0",
    val password: String = "",
    val isOnline: Boolean = true,
    val isConnected: Boolean = false
)

data class ServerConfig(
    val port: Int = 23016,
    val isRunning: Boolean = false,
    val isReadOnly: Boolean = false,
    val password: String = "",
    val autoStart: Boolean = false,
    val selectedFiles: List<String> = emptyList()
)

data class RemoteCommand(
    val type: String,
    val payload: String = "",
    val dx: Float = 0f,
    val dy: Float = 0f,
    val keyCode: Int = 0,
    val pairingCode: String = ""
)

data class StorageCategoryInfo(
    val title: String,
    val sizeBytes: Long,
    val colorHex: Long,
    val fileCount: Int
)

sealed class ActiveViewer {
    object None : ActiveViewer()
    data class Video(val path: String, val name: String) : ActiveViewer()
    data class Image(val path: String, val name: String, val imageList: List<String>) : ActiveViewer()
    data class Audio(val path: String, val name: String) : ActiveViewer()
    data class Text(val path: String, val name: String, val text: String) : ActiveViewer()
    data class Hex(val path: String, val name: String, val hexPreview: String) : ActiveViewer()
    data class Archive(val path: String, val name: String) : ActiveViewer()
    object DiskMap : ActiveViewer()
    object Vault : ActiveViewer()
}
