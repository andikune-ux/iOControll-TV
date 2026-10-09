package dev.andikuneiocontroll.remote.protocol.adb

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AdbProtocol — Konstanta & builder pesan ADB.
 *
 * Format header (24 bytes):
 * - command  : 4 byte ASCII (misal "CNXN")
 * - arg0     : 4 byte LE
 * - arg1     : 4 byte LE
 * - dataLen  : 4 byte LE
 * - checksum : 4 byte LE (sum of payload bytes)
 * - magic    : 4 byte LE (command XOR 0xFFFFFFFF)
 */
object AdbProtocol {

    // Command codes
    const val CMD_CNXN = 0x4E584E43  // "CNXN"
    const val CMD_AUTH = 0x48545541  // "AUTH"
    const val CMD_OPEN = 0x4E45504F  // "OPEN"
    const val CMD_OKAY = 0x59414B4F  // "OKAY"
    const val CMD_CLSE = 0x45534C43  // "CLSE"
    const val CMD_WRTE = 0x45545257  // "WRTE"
    const val CMD_SYNC = 0x434E5953  // "SYNC"

    // AUTH types
    const val AUTH_TYPE_TOKEN = 1
    const val AUTH_TYPE_SIGNATURE = 2
    const val AUTH_TYPE_RSA_KEY = 3

    // Version
    const val ADB_VERSION = 0x01000000
    const val ADB_MAX_DATA = 256 * 1024

    // Default ADB port
    const val DEFAULT_PORT = 5555

    /**
     * Buat header 24 byte.
     */
    fun buildHeader(
        command: Int,
        arg0: Int,
        arg1: Int,
        dataLength: Int,
        checksum: Int
    ): ByteArray {
        val magic = command xor -0x1  // XOR 0xFFFFFFFF
        val buffer = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(command)
        buffer.putInt(arg0)
        buffer.putInt(arg1)
        buffer.putInt(dataLength)
        buffer.putInt(checksum)
        buffer.putInt(magic)
        return buffer.array()
    }

    /**
     * Hitung checksum: sum of bytes (32-bit overflow ignored).
     */
    fun calculateChecksum(data: ByteArray): Int {
        var sum = 0
        for (b in data) {
            sum += (b.toInt() and 0xFF)
        }
        return sum
    }

    /**
     * Buat pesan CNXN (connect).
     */
    fun buildCnxn(publicKey: ByteArray): ByteArray {
        val payloadText = "host::features=shell_v2,cmd,stat_v2,ls_v2,fixed_push_mkdir,apex,abb_exec,abb,abb_install,pairing_flow,shell_nohup,shell_abort,dev_usb"
        val payload = (payloadText + "\u0000").toByteArray(Charsets.UTF_8)
        val payloadWithKey = payload + publicKey

        val checksum = calculateChecksum(payloadWithKey)
        val header = buildHeader(
            CMD_CNXN,
            ADB_VERSION,
            ADB_MAX_DATA,
            payloadWithKey.size,
            checksum
        )

        return header + payloadWithKey
    }

    /**
     * Buat pesan AUTH (untuk signature / public key).
     */
    fun buildAuth(type: Int, data: ByteArray): ByteArray {
        val checksum = calculateChecksum(data)
        val header = buildHeader(
            CMD_AUTH,
            type,
            0,
            data.size,
            checksum
        )
        return header + data
    }

    /**
     * Buat pesan OPEN (buat shell session).
     */
    fun buildOpen(localId: Int, destination: String): ByteArray {
        val payload = (destination + "\u0000").toByteArray(Charsets.UTF_8)
        val checksum = calculateChecksum(payload)
        val header = buildHeader(
            CMD_OPEN,
            localId,
            0,
            payload.size,
            checksum
        )
        return header + payload
    }

    /**
     * Buat pesan WRTE (kirim data).
     */
    fun buildWrite(localId: Int, remoteId: Int, data: ByteArray): ByteArray {
        val checksum = calculateChecksum(data)
        val header = buildHeader(
            CMD_WRTE,
            localId,
            remoteId,
            data.size,
            checksum
        )
        return header + data
    }

    /**
     * Buat pesan OKAY.
     */
    fun buildOkay(localId: Int, remoteId: Int): ByteArray {
        return buildHeader(CMD_OKAY, localId, remoteId, 0, 0)
    }

    /**
     * Buat pesan CLSE (close).
     */
    fun buildClose(localId: Int, remoteId: Int): ByteArray {
        return buildHeader(CMD_CLSE, localId, remoteId, 0, 0)
    }

    /**
     * Parse header dari ByteArray 24 byte.
     */
    fun parseHeader(data: ByteArray): AdbHeader {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val command = buffer.int
        val arg0 = buffer.int
        val arg1 = buffer.int
        val dataLength = buffer.int
        val checksum = buffer.int
        val magic = buffer.int
        return AdbHeader(command, arg0, arg1, dataLength, checksum, magic)
    }

    /**
     * Cek apakah magic valid (command XOR magic == 0xFFFFFFFF).
     */
    fun isMagicValid(header: AdbHeader): Boolean {
        return (header.command xor header.magic) == -0x1
    }
}

data class AdbHeader(
    val command: Int,
    val arg0: Int,
    val arg1: Int,
    val dataLength: Int,
    val checksum: Int,
    val magic: Int
)
