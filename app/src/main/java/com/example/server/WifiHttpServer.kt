package com.example.server

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.example.model.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder

class WifiHttpServer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    var currentConfig: ServerConfig = ServerConfig()
        private set

    val isRunning: Boolean
        get() = serverSocket?.isClosed == false && serverJob?.isActive == true

    fun startServer(config: ServerConfig, onStatusChange: (Boolean, String) -> Unit) {
        if (isRunning) {
            stopServer()
        }
        currentConfig = config.copy(isRunning = true)

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket(config.port)
                serverSocket = server
                val localIp = getDeviceIpAddress(context)
                val url = "http://$localIp:${config.port}"

                withContext(Dispatchers.Main) {
                    onStatusChange(true, url)
                }

                while (isActive && !server.isClosed) {
                    try {
                        val clientSocket = server.accept()
                        launch(Dispatchers.IO) {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!server.isClosed) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    currentConfig = currentConfig.copy(isRunning = false)
                    onStatusChange(false, "Error: ${e.message}")
                }
            }
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            serverJob = null
            currentConfig = currentConfig.copy(isRunning = false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { s ->
                val input = s.getInputStream()
                val reader = BufferedReader(InputStreamReader(input))
                val output = BufferedOutputStream(s.getOutputStream())

                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0].uppercase()
                val rawUrl = parts[1]

                // Read headers
                val headers = mutableMapOf<String, String>()
                var headerLine: String?
                while (reader.readLine().also { headerLine = it } != null) {
                    if (headerLine.isNullOrBlank()) break
                    val colonIdx = headerLine!!.indexOf(":")
                    if (colonIdx != -1) {
                        headers[headerLine!!.substring(0, colonIdx).trim().lowercase()] =
                            headerLine!!.substring(colonIdx + 1).trim()
                    }
                }

                val uriParts = rawUrl.split("?", limit = 2)
                val path = uriParts[0]
                val queryParams = parseQueryParams(if (uriParts.size > 1) uriParts[1] else "")

                // Password authentication check if configured
                if (currentConfig.password.isNotEmpty()) {
                    val auth = queryParams["key"] ?: headers["x-auth-key"]
                    if (auth != currentConfig.password) {
                        sendUnauthorized(output)
                        return
                    }
                }

                when {
                    path == "/" || path == "/index.html" -> {
                        serveWebPage(output, queryParams["path"] ?: "/storage/emulated/0")
                    }
                    path == "/api/status" -> {
                        serveStatus(output)
                    }
                    path == "/api/files" -> {
                        val requestedPath = queryParams["path"] ?: "/storage/emulated/0"
                        serveFileListJson(output, requestedPath)
                    }
                    path == "/download" || path.startsWith("/file") -> {
                        val filePath = queryParams["path"] ?: ""
                        serveFileDownload(output, filePath, headers["range"])
                    }
                    method == "POST" && path == "/upload" -> {
                        if (currentConfig.isReadOnly) {
                            sendResponse(output, 403, "Forbidden", "text/plain", "Server is read-only".toByteArray())
                        } else {
                            val uploadPath = queryParams["path"] ?: "/storage/emulated/0"
                            val fileName = queryParams["filename"] ?: "upload_${System.currentTimeMillis()}"
                            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                            handleRawFileUpload(input, output, uploadPath, fileName, contentLength)
                        }
                    }
                    else -> {
                        sendResponse(output, 404, "Not Found", "text/plain", "Not Found".toByteArray())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseQueryParams(queryString: String): Map<String, String> {
        if (queryString.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        val pairs = queryString.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx != -1) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[key] = value
            }
        }
        return map
    }

    private fun serveStatus(output: BufferedOutputStream) {
        val json = JSONObject().apply {
            put("app", "iOControll Tv")
            put("version", "1.0")
            put("readOnly", currentConfig.isReadOnly)
            put("port", currentConfig.port)
            put("customSharing", currentConfig.selectedFiles.isNotEmpty())
            put("selectedCount", currentConfig.selectedFiles.size)
        }
        sendResponse(output, 200, "OK", "application/json", json.toString().toByteArray())
    }

    private fun serveFileListJson(output: BufferedOutputStream, targetPath: String) {
        val json = JSONObject()
        val array = JSONArray()

        if (currentConfig.selectedFiles.isNotEmpty()) {
            // Custom sharing: only expose selected files
            for (p in currentConfig.selectedFiles) {
                val f = File(p)
                if (f.exists()) {
                    val item = JSONObject().apply {
                        put("name", f.name)
                        put("path", f.absolutePath)
                        put("isDirectory", f.isDirectory)
                        put("size", if (f.isDirectory) 0 else f.length())
                        put("lastModified", f.lastModified())
                    }
                    array.put(item)
                }
            }
            json.put("path", "Shared Files Only")
            json.put("files", array)
        } else {
            val dir = File(targetPath)
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles() ?: emptyArray()
                for (f in files) {
                    val item = JSONObject().apply {
                        put("name", f.name)
                        put("path", f.absolutePath)
                        put("isDirectory", f.isDirectory)
                        put("size", if (f.isDirectory) 0 else f.length())
                        put("lastModified", f.lastModified())
                    }
                    array.put(item)
                }
                json.put("path", dir.absolutePath)
                json.put("parent", dir.parent ?: "")
                json.put("files", array)
            } else {
                json.put("error", "Directory not found")
                json.put("files", array)
            }
        }

        sendResponse(output, 200, "OK", "application/json", json.toString().toByteArray())
    }

    private fun serveFileDownload(output: BufferedOutputStream, filePath: String, rangeHeader: String?) {
        val file = File(filePath)
        if (!file.exists() || file.isDirectory) {
            sendResponse(output, 404, "Not Found", "text/plain", "File not found".toByteArray())
            return
        }

        // Check if custom sharing restriction applies
        if (currentConfig.selectedFiles.isNotEmpty() && !currentConfig.selectedFiles.contains(file.absolutePath)) {
            // check if it's inside one of the selected folders
            val isInside = currentConfig.selectedFiles.any { selected ->
                file.absolutePath.startsWith(selected)
            }
            if (!isInside) {
                sendResponse(output, 403, "Forbidden", "text/plain", "File not shared".toByteArray())
                return
            }
        }

        val totalLength = file.length()
        var start = 0L
        var end = totalLength - 1

        val isRange = rangeHeader != null && rangeHeader.startsWith("bytes=")
        if (isRange) {
            val range = rangeHeader!!.substring(6).split("-")
            start = range[0].toLongOrNull() ?: 0L
            if (range.size > 1 && range[1].isNotEmpty()) {
                end = range[1].toLongOrNull() ?: end
            }
            if (end >= totalLength) end = totalLength - 1
        }

        val contentLength = end - start + 1
        val mimeType = getMimeType(file.extension)

        val headerSb = StringBuilder()
        if (isRange) {
            headerSb.append("HTTP/1.1 206 Partial Content\r\n")
            headerSb.append("Content-Range: bytes $start-$end/$totalLength\r\n")
        } else {
            headerSb.append("HTTP/1.1 200 OK\r\n")
        }
        headerSb.append("Content-Type: $mimeType\r\n")
        headerSb.append("Content-Length: $contentLength\r\n")
        headerSb.append("Content-Disposition: inline; filename=\"${file.name}\"\r\n")
        headerSb.append("Accept-Ranges: bytes\r\n")
        headerSb.append("Access-Control-Allow-Origin: *\r\n")
        headerSb.append("Connection: close\r\n\r\n")

        output.write(headerSb.toString().toByteArray())

        FileInputStream(file).use { fis ->
            if (start > 0) fis.skip(start)
            val buffer = ByteArray(64 * 1024)
            var bytesToRead = contentLength
            while (bytesToRead > 0) {
                val read = fis.read(buffer, 0, minOf(buffer.size.toLong(), bytesToRead).toInt())
                if (read <= 0) break
                output.write(buffer, 0, read)
                bytesToRead -= read
            }
            output.flush()
        }
    }

    private fun handleRawFileUpload(
        input: java.io.InputStream,
        output: BufferedOutputStream,
        uploadDir: String,
        fileName: String,
        length: Int
    ) {
        val destFile = File(uploadDir, fileName)
        FileOutputStream(destFile).use { fos ->
            val buffer = ByteArray(64 * 1024)
            var bytesLeft = length
            while (bytesLeft > 0) {
                val read = input.read(buffer, 0, minOf(buffer.size, bytesLeft))
                if (read <= 0) break
                fos.write(buffer, 0, read)
                bytesLeft -= read
            }
        }
        sendResponse(output, 200, "OK", "application/json", "{\"status\":\"success\",\"file\":\"$fileName\"}".toByteArray())
    }

    private fun serveWebPage(output: BufferedOutputStream, currentDir: String) {
        val dir = File(currentDir)
        val files = if (currentConfig.selectedFiles.isNotEmpty()) {
            currentConfig.selectedFiles.map { File(it) }.filter { it.exists() }
        } else {
            (dir.listFiles() ?: emptyArray()).toList()
        }

        val html = buildString {
            append("<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1'>")
            append("<title>iOControll Tv - Wi-Fi File Transfer</title>")
            append("<style>")
            append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f1f5f9; margin: 0; padding: 20px; }")
            append(".header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #334155; padding-bottom: 15px; margin-bottom: 20px; }")
            append(".title { font-size: 24px; font-weight: bold; color: #06b6d4; }")
            append(".badge { background: #1e293b; padding: 6px 12px; border-radius: 6px; font-size: 13px; color: #f59e0b; }")
            append(".path { background: #1e293b; padding: 10px; border-radius: 8px; margin-bottom: 15px; word-break: break-all; color: #94a3b8; }")
            append(".list { list-style: none; padding: 0; margin: 0; }")
            append(".item { display: flex; align-items: center; justify-content: space-between; padding: 12px; background: #1e293b; margin-bottom: 8px; border-radius: 8px; }")
            append(".item a { color: #f1f5f9; text-decoration: none; font-weight: 500; display: flex; align-items: center; gap: 10px; flex: 1; }")
            append(".btn { background: #06b6d4; color: #000; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; text-decoration: none; font-weight: bold; }")
            append(".upload-box { background: #1e293b; padding: 15px; border-radius: 8px; margin-bottom: 20px; border: 1px dashed #475569; }")
            append("</style></head><body>")
            append("<div class='header'>")
            append("<div class='title'>⚡ iOControll Tv - File Sharing</div>")
            append("<div class='badge'>${if (currentConfig.isReadOnly) "Read-Only" else "Full Access"}</div>")
            append("</div>")

            append("<div class='path'>Direktori: ${dir.absolutePath}</div>")

            if (!currentConfig.isReadOnly && currentConfig.selectedFiles.isEmpty()) {
                append("<div class='upload-box'>")
                append("<form method='POST' action='/upload?path=${URLEncoder.encode(dir.absolutePath, "UTF-8")}' enctype='multipart/form-data'>")
                append("<label>Unggah Berkas ke folder ini: </label>")
                append("<input type='file' name='file' style='margin: 10px 0;'> ")
                append("<input type='submit' class='btn' value='Unggah'>")
                append("</form></div>")
            }

            append("<ul class='list'>")
            if (dir.parentFile != null && currentConfig.selectedFiles.isEmpty()) {
                append("<li class='item'><a href='/?path=${URLEncoder.encode(dir.parentFile!!.absolutePath, "UTF-8")}'>📁 .. (Kembali ke folder atas)</a></li>")
            }
            for (f in files) {
                append("<li class='item'>")
                if (f.isDirectory) {
                    append("<a href='/?path=${URLEncoder.encode(f.absolutePath, "UTF-8")}'>📁 ${f.name}</a>")
                } else {
                    val encoded = URLEncoder.encode(f.absolutePath, "UTF-8")
                    append("<a href='/download?path=$encoded' target='_blank'>📄 ${f.name}</a>")
                    append("<a href='/download?path=$encoded' class='btn' download>Unduh (${f.length() / 1024} KB)</a>")
                }
                append("</li>")
            }
            append("</ul></body></html>")
        }

        sendResponse(output, 200, "OK", "text/html; charset=UTF-8", html.toByteArray())
    }

    private fun sendResponse(
        output: BufferedOutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: ByteArray
    ) {
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray())
        output.write(body)
        output.flush()
    }

    private fun sendUnauthorized(output: BufferedOutputStream) {
        val body = "{\"error\":\"Password diperlukan (masukkan query ?key=PASSWORD)\"}".toByteArray()
        sendResponse(output, 401, "Unauthorized", "application/json", body)
    }

    private fun getMimeType(ext: String): String {
        return when (ext.lowercase()) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            "apk" -> "application/vnd.android.package-archive"
            "zip" -> "application/zip"
            "json" -> "application/json"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    companion object {
        fun getDeviceIpAddress(context: Context): String {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val ipInt = wifiManager.connectionInfo.ipAddress
                if (ipInt != 0) {
                    return Formatter.formatIpAddress(ipInt)
                }
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr is InetAddress) {
                            val host = addr.hostAddress ?: ""
                            if (host.contains(".") && !host.contains(":")) {
                                return host
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "127.0.0.1"
        }
    }
}
