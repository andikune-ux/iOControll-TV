package dev.andikuneiocontroll.remote.protocol.adb

import android.content.Context
import android.util.Base64
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AdbCrypto — Helper untuk RSA key generation, signing, dan persistence.
 *
 * ADB menggunakan RSA 2048-bit untuk autentikasi.
 * Saat connect pertama kali, TV akan menampilkan prompt "Allow USB debugging?"
 * dan user harus tap "Always allow".
 */
object AdbCrypto {

    private const val KEY_SIZE = 2048
    private const val KEY_FILE_PRIVATE = "adb_private.key"
    private const val KEY_FILE_PUBLIC = "adb_public.key"

    /**
     * Load atau generate key pair.
     */
    fun getOrCreateKeyPair(context: Context): KeyPair {
        val privateFile = File(context.filesDir, KEY_FILE_PRIVATE)
        val publicFile = File(context.filesDir, KEY_FILE_PUBLIC)

        return if (privateFile.exists() && publicFile.exists()) {
            try {
                loadKeyPair(privateFile, publicFile)
            } catch (e: Exception) {
                // File rusak — regenerate
                privateFile.delete()
                publicFile.delete()
                generateAndSave(context)
            }
        } else {
            generateAndSave(context)
        }
    }

    private fun generateAndSave(context: Context): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(KEY_SIZE)
        val keyPair = generator.generateKeyPair()

        val privateFile = File(context.filesDir, KEY_FILE_PRIVATE)
        val publicFile = File(context.filesDir, KEY_FILE_PUBLIC)

        privateFile.writeBytes(keyPair.private.encoded)
        publicFile.writeBytes(keyPair.public.encoded)

        return keyPair
    }

    private fun loadKeyPair(privateFile: File, publicFile: File): KeyPair {
        val keyFactory = KeyFactory.getInstance("RSA")

        val privateBytes = privateFile.readBytes()
        val privateSpec = PKCS8EncodedKeySpec(privateBytes)
        val privateKey: PrivateKey = keyFactory.generatePrivate(privateSpec)

        val publicBytes = publicFile.readBytes()
        val publicSpec = X509EncodedKeySpec(publicBytes)
        val publicKey: PublicKey = keyFactory.generatePublic(publicSpec)

        return KeyPair(publicKey, privateKey)
    }

    /**
     * Dapatkan public key dalam format ADB (Android binary format).
     *
     * Format: Base64(struct { uint32 len; bytes modulus; uint32 e; })
     * Dipakai untuk CNXN payload.
     */
    fun getAdbPublicKey(keyPair: KeyPair): ByteArray {
        val rsaPublic = keyPair.public as RSAPublicKey
        val modulus = rsaPublic.modulus
        val exponent = rsaPublic.publicExponent

        val modulusBytes = modulus.toByteArray()
        val exponentBytes = exponent.toByteArray()

        // ADB format: uint32 len; bytes modulus; uint32 exponent
        val buffer = ByteBuffer.allocate(4 + modulusBytes.size + 4 + exponentBytes.size)
            .order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(modulusBytes.size)
        buffer.put(modulusBytes)
        buffer.putInt(exponentBytes.size)
        buffer.put(exponentBytes)

        return buffer.array()
    }

    /**
     * Dapatkan public key dalam format string (untuk display).
     */
    fun getAdbPublicKeyString(keyPair: KeyPair): String {
        val adbKey = getAdbPublicKey(keyPair)
        return Base64.encodeToString(adbKey, Base64.NO_WRAP)
    }

    /**
     * Sign token dari TV dengan private key.
     *
     * TV akan kirim AUTH TOKEN (20 byte random), lalu kita sign dengan RSA
     * dan kirim balik sebagai AUTH SIGNATURE.
     */
    fun signToken(keyPair: KeyPair, token: ByteArray): ByteArray {
        val signature = Signature.getInstance("SHA1withRSA")
        signature.initSign(keyPair.private)
        signature.update(token)
        return signature.sign()
    }

    /**
     * Verifikasi signature (untuk testing).
     */
    fun verifySignature(keyPair: KeyPair, token: ByteArray, signatureBytes: ByteArray): Boolean {
        return try {
            val signature = Signature.getInstance("SHA1withRSA")
            signature.initVerify(keyPair.public)
            signature.update(token)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Hapus key pair (untuk reset autentikasi).
     */
    fun clearKeys(context: Context) {
        File(context.filesDir, KEY_FILE_PRIVATE).delete()
        File(context.filesDir, KEY_FILE_PUBLIC).delete()
    }

    /**
     * Cek apakah key pair sudah ada.
     */
    fun hasKeys(context: Context): Boolean {
        return File(context.filesDir, KEY_FILE_PRIVATE).exists() &&
                File(context.filesDir, KEY_FILE_PUBLIC).exists()
    }
}
