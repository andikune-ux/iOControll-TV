package dev.andikuneiocontroll.remote.protocol.androidtv

import android.content.Context
import android.util.Base64
import java.io.File
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

/**
 * TlsHelper — Helper untuk TLS cert Android TV Remote v2.
 *
 * Digunakan untuk:
 * 1. Generate self-signed cert di HP (client cert)
 * 2. Simpan / muat cert dari file
 * 3. Build SSLContext untuk TLS handshake ke TV
 *
 * Android TV Remote v2 memakai TLS dengan self-signed cert
 * pada kedua sisi (TV & HP). Setelah pairing sukses, cert
 * HP disimpan oleh TV dan sebaliknya → koneksi berikutnya auto-trust.
 */
object TlsHelper {

    private const val CERT_FILE = "atv_client_cert.pem"
    private const val KEY_FILE = "atv_client_key.pem"
    private const val KEYSTORE_FILE = "atv_client.p12"
    private const val KEYSTORE_PASSWORD = "iocontroll_atv"
    private const val CERT_ALIAS = "atv_client"
    private const val CERT_VALIDITY_YEARS = 10

    @Volatile
    private var initialized = false

    /**
     * Init BouncyCastle provider.
     */
    fun init() {
        if (!initialized) {
            try {
                Security.removeProvider("BC")
            } catch (_: Exception) {}
            Security.addProvider(BouncyCastleProvider())
            initialized = true
        }
    }

    /**
     * Dapatkan atau buat keystore client.
     * Keystore ini menyimpan cert + private key untuk TLS.
     */
    fun getOrCreateKeyStore(context: Context): KeyStore {
        init()
        val keyStoreFile = File(context.filesDir, KEYSTORE_FILE)

        val keyStore = KeyStore.getInstance("PKCS12")
        if (keyStoreFile.exists()) {
            try {
                keyStoreFile.inputStream().use { input ->
                    keyStore.load(input, KEYSTORE_PASSWORD.toCharArray())
                }
                return keyStore
            } catch (e: Exception) {
                // Corrupt — regenerate
                keyStoreFile.delete()
            }
        }

        // Generate baru
        keyStore.load(null, null)
        val keyPair = generateRsaKeyPair()
        val cert = generateSelfSignedCert(keyPair)

        keyStore.setKeyEntry(
            CERT_ALIAS,
            keyPair.private,
            KEYSTORE_PASSWORD.toCharArray(),
            arrayOf(cert)
        )

        // Simpan ke file
        keyStoreFile.outputStream().use { output ->
            keyStore.store(output, KEYSTORE_PASSWORD.toCharArray())
        }

        return keyStore
    }

    /**
     * Ambil client certificate dari keystore.
     */
    fun getClientCertificate(context: Context): X509Certificate? {
        return try {
            val keyStore = getOrCreateKeyStore(context)
            keyStore.getCertificate(CERT_ALIAS) as? X509Certificate
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Ambil client private key dari keystore.
     */
    fun getClientPrivateKey(context: Context): PrivateKey? {
        return try {
            val keyStore = getOrCreateKeyStore(context)
            keyStore.getKey(CERT_ALIAS, KEYSTORE_PASSWORD.toCharArray()) as? PrivateKey
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Build SSLContext untuk TLS handshake.
     * - Pakai client cert dari keystore
     * - Trust semua server cert (TV pakai self-signed)
     */
    fun buildSslContext(context: Context): SSLContext {
        init()
        val keyStore = getOrCreateKeyStore(context)

        // KeyManager dari client cert
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray())

        // TrustManager trust-all (TV self-signed)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, trustAllCerts, SecureRandom())
        return sslContext
    }

    /**
     * Simpan cert dari TV (setelah pairing).
     */
    fun saveServerCertificate(context: Context, cert: X509Certificate) {
        try {
            val file = File(context.filesDir, CERT_FILE)
            file.writeBytes(cert.encoded)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Muat cert TV yang tersimpan.
     */
    fun loadServerCertificate(context: Context): X509Certificate? {
        return try {
            val file = File(context.filesDir, CERT_FILE)
            if (!file.exists()) return null
            val certFactory = java.security.cert.CertificateFactory.getInstance("X.509")
            file.inputStream().use { input ->
                certFactory.generateCertificate(input) as X509Certificate
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Cek apakah sudah pernah pairing dengan TV.
     */
    fun hasPairingData(context: Context): Boolean {
        return File(context.filesDir, CERT_FILE).exists()
    }

    /**
     * Hapus semua data pairing.
     */
    fun clearPairingData(context: Context) {
        try {
            File(context.filesDir, CERT_FILE).delete()
            File(context.filesDir, KEYSTORE_FILE).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Ambil cert sebagai Base64 (untuk dikirim ke TV).
     */
    fun certToBase64(cert: X509Certificate): String {
        return Base64.encodeToString(cert.encoded, Base64.NO_WRAP)
    }

    // ==========================================================
    // INTERNAL: Generate cert & key
    // ==========================================================

    private fun generateRsaKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, SecureRandom())
        return generator.generateKeyPair()
    }

    private fun generateSelfSignedCert(keyPair: KeyPair): X509Certificate {
        val now = System.currentTimeMillis()
        val startDate = Date(now - 24 * 60 * 60 * 1000L) // kemarin
        val endDate = Date(now + CERT_VALIDITY_YEARS.toLong() * 365 * 24 * 60 * 60 * 1000L)

        val subject = X500Name("CN=atvremote, O=androidtvremote2, OU=Android, C=US")

        val serial = BigInteger.valueOf(now)

        val builder = JcaX509v3CertificateBuilder(
            subject,
            serial,
            startDate,
            endDate,
            subject,
            keyPair.public
        )

        // Basic constraints — bukan CA
        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))
        // Key usage — digital signature + key encipherment
        builder.addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment)
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider("BC")
            .build(keyPair.private)

        return JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(builder.build(signer))
    }
}
