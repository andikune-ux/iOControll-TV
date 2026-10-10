package dev.andikuneiocontroll.remote.protocol.androidtv

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.File
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
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
 * TlsHelper — Helper TLS untuk Android TV Remote v2.
 *
 * V3.1:
 * - Eksplisit pakai TLSv1.2 (Android TV Remote Service cuma support 1.2)
 * - Fix fallback negotiation
 */
object TlsHelper {

    private const val TAG = "TlsHelper"

    private const val DEFAULT_CLIENT_NAME = "iOControll Tv"

    // v3 → paksa regenerate cert
    private const val KEYSTORE_FILE = "atv_client_v3.p12"
    private const val KEYSTORE_PASSWORD = "iocontroll_atv"
    private const val CERT_ALIAS = "atv_client"
    private const val CERT_VALIDITY_YEARS = 10

    private const val CERT_FILE = "atv_server_cert.pem"

    private const val PREFS_NAME = "atv_pairing_prefs"
    private const val KEY_CLIENT_NAME = "client_name"

    @Volatile
    private var initialized = false

    fun init() {
        if (!initialized) {
            try {
                Security.removeProvider("BC")
            } catch (_: Exception) {}
            Security.addProvider(BouncyCastleProvider())
            initialized = true
        }
    }

    fun getOrCreateClientName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_CLIENT_NAME, null)
        if (!existing.isNullOrBlank()) return existing

        prefs.edit().putString(KEY_CLIENT_NAME, DEFAULT_CLIENT_NAME).apply()
        Log.d(TAG, "client_name diset: $DEFAULT_CLIENT_NAME")
        return DEFAULT_CLIENT_NAME
    }

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
                Log.w(TAG, "Keystore lama rusak, regenerate: ${e.message}")
                keyStoreFile.delete()
            }
        }

        keyStore.load(null, null)
        val keyPair = generateRsaKeyPair()
        val cert = generateSelfSignedCert(context, keyPair)

        keyStore.setKeyEntry(
            CERT_ALIAS,
            keyPair.private,
            KEYSTORE_PASSWORD.toCharArray(),
            arrayOf(cert)
        )

        keyStoreFile.outputStream().use { output ->
            keyStore.store(output, KEYSTORE_PASSWORD.toCharArray())
        }

        Log.d(TAG, "Keystore baru, subject='${cert.subjectX500Principal.name}'")
        return keyStore
    }

    fun getClientCertificate(context: Context): X509Certificate? {
        return try {
            val keyStore = getOrCreateKeyStore(context)
            keyStore.getCertificate(CERT_ALIAS) as? X509Certificate
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

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
     * Build SSLContext untuk TLS handshake ke TV.
     * FIX: eksplisit TLSv1.2 (Android TV Remote Service butuh versi ini).
     */
    fun buildSslContext(context: Context): SSLContext {
        init()
        val keyStore = getOrCreateKeyStore(context)

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray())

        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        // Coba TLSv1.2 dulu (paling kompatibel dengan Android TV)
        val sslContext = try {
            SSLContext.getInstance("TLSv1.2")
        } catch (e: Exception) {
            Log.w(TAG, "TLSv1.2 tidak tersedia, fallback ke TLS: ${e.message}")
            SSLContext.getInstance("TLS")
        }

        sslContext.init(kmf.keyManagers, trustAllCerts, SecureRandom())
        return sslContext
    }

    fun saveServerCertificate(context: Context, cert: X509Certificate) {
        try {
            File(context.filesDir, CERT_FILE).writeBytes(cert.encoded)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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

    fun hasPairingData(context: Context): Boolean {
        return File(context.filesDir, CERT_FILE).exists()
    }

    fun clearPairingData(context: Context) {
        try {
            File(context.filesDir, CERT_FILE).delete()
            File(context.filesDir, KEYSTORE_FILE).delete()
            File(context.filesDir, "atv_client.p12").delete()
            File(context.filesDir, "atv_client_v2.p12").delete()
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_CLIENT_NAME).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun certToBase64(cert: X509Certificate): String {
        return Base64.encodeToString(cert.encoded, Base64.NO_WRAP)
    }

    // ==========================================================
    // INTERNAL
    // ==========================================================

    private fun generateRsaKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, SecureRandom())
        return generator.generateKeyPair()
    }

    private fun generateSelfSignedCert(context: Context, keyPair: KeyPair): X509Certificate {
        val clientName = getOrCreateClientName(context)

        val now = System.currentTimeMillis()
        val startDate = Date(now - 24 * 60 * 60 * 1000L)
        val endDate = Date(now + CERT_VALIDITY_YEARS.toLong() * 365 * 24 * 60 * 60 * 1000L)

        val subject = X500Name("CN=$clientName, O=androidtvremote2, OU=Android, C=US")
        val serial = BigInteger.valueOf(now)

        val builder = JcaX509v3CertificateBuilder(
            subject, serial, startDate, endDate, subject, keyPair.public
        )

        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))
        builder.addExtension(
            Extension.keyUsage, true,
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
