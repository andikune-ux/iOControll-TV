package dev.andikuneiocontroll.remote.protocol.androidtv

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Spake2 — Implementasi SPAKE2 (Symmetric Password-Authenticated Key Exchange).
 *
 * Standar: RFC 9382
 * Curve  : Ed25519
 * Hash   : SHA-256
 * KDF    : HKDF-SHA256
 *
 * Dipakai Android TV Remote v2 untuk pairing.
 * Password = kode 6 digit dari TV (contoh: "A86E48").
 *
 * ⚠️ CATATAN:
 * Implementasi ini best-effort berdasarkan RFC 9382 + referensi.
 * Kalau saat test pairing gagal, kemungkinan konstanta M/N atau
 * format hash perlu disesuaikan. Kirim log error ke saya.
 */
class Spake2(
    private val isClient: Boolean,     // true = HP, false = TV
    private val myName: ByteArray,     // "atvremote" (identitas)
    private val theirName: ByteArray,  // "atvremote" (identitas lawan)
    password: ByteArray
) {

    // ==========================================================
    // CONSTANTS (dari RFC 9382 — Ed25519)
    // ==========================================================

    // Mask point M (untuk client = HP)
    private val M: ByteArray = hexToBytes(
        "d048032c6ea0b6d697ddc2e86bda85a33adac920f1bf18e1b0c6d166a5cecdaf"
    )

    // Mask point N (untuk server = TV)
    private val N: ByteArray = hexToBytes(
        "d3bfb518f44f3430f29d0c92af503865a1ed3281dc69b35dd868ba85f886c4ab"
    )

    // Ed25519 curve constants
    private val P: BigInteger = BigInteger.TWO.pow(255).subtract(BigInteger.valueOf(19))
    private val D: BigInteger = BigInteger("-121665")
        .multiply(BigInteger("121666").modInverse(P))
        .mod(P)
    private val I: BigInteger = BigInteger.TWO.modPow(
        P.subtract(BigInteger.ONE).divide(BigInteger.valueOf(4)),
        P
    )

    // Base point B (Ed25519)
    private val B: ByteArray = hexToBytes(
        "5866666666666666666666666666666666666666666666666666666666666666"
    )

    // ==========================================================
    // STATE
    // ==========================================================

    private val random = SecureRandom()

    // Scalar x (acak)
    private val x: BigInteger = BigInteger(256, random).mod(
        BigInteger.TWO.pow(252).add(BigInteger("27742317777372353535851937790883648493"))
    )

    // Password scalar w
    private val w: BigInteger = derivePasswordScalar(password)

    // Point X = x * B (untuk client)
    // Point Y = y * B (untuk server) — kita pakai x juga
    private val myPointX: ByteArray = scalarMultBase(x)

    // Message pertama yang dikirim ke lawan
    private var myMessage: ByteArray = ByteArray(0)

    // Message dari lawan
    private var theirMessage: ByteArray = ByteArray(0)

    // Shared key (hasil akhir)
    private var sharedKey: ByteArray = ByteArray(0)

    // ==========================================================
    // PUBLIC API
    // ==========================================================

    /**
     * Mulai SPAKE2 — generate message pertama.
     * Untuk client:
     *   msg = x * B + w * M
     * Untuk server:
     *   msg = y * B + w * N
     */
    fun start(): ByteArray {
        return if (isClient) {
            // client: X + w*M
            val wM = scalarMult(w, M)
            val combined = pointAdd(myPointX, wM)
            myMessage = combined
            combined
        } else {
            // server: Y + w*N
            val wN = scalarMult(w, N)
            val combined = pointAdd(myPointX, wN)
            myMessage = combined
            combined
        }
    }

    /**
     * Finish SPAKE2 — hitung shared key dari message lawan.
     *
     * Client:
     *   K = x * (Y - w*N)
     * Server:
     *   K = y * (X - w*M)
     *
     * Lalu KDF(K, transcript) → shared key
     */
    fun finish(theirMsg: ByteArray): ByteArray {
        theirMessage = theirMsg

        // Hitung shared point
        val shared = if (isClient) {
            // client: x * (Y - w*N)
            val wN = scalarMult(w, N)
            val Y_minus_wN = pointSub(theirMsg, wN)
            scalarMult(x, Y_minus_wN)
        } else {
            // server: y * (X - w*M)
            val wM = scalarMult(w, M)
            val X_minus_wM = pointSub(theirMsg, wM)
            scalarMult(x, X_minus_wM)
        }

        // KDF: HKDF-SHA256 dengan transcript
        val transcript = buildTranscript()
        sharedKey = hkdf(shared, transcript)
        return sharedKey
    }

    fun getSharedKey(): ByteArray = sharedKey
    fun getMyMessage(): ByteArray = myMessage

    /**
     * Verify confirmation MAC.
     * Digunakan untuk konfirmasi bahwa kedua pihak punya key yang sama.
     */
    fun computeConfirmationMac(key: ByteArray): ByteArray {
        // HMAC-SHA256 dari transcript dengan key
        val digest = MessageDigest.getInstance("SHA-256")
        val mac = org.bouncycastle.crypto.macs.HMac(SHA256Digest())
        mac.init(org.bouncycastle.crypto.params.KeyParameter(key))
        mac.update(theirMessage, 0, theirMessage.size)
        val out = ByteArray(mac.macSize)
        mac.doFinal(out, 0)
        return out
    }

    // ==========================================================
    // INTERNAL: Crypto primitives
    // ==========================================================

    private fun derivePasswordScalar(password: ByteArray): BigInteger {
        // w = SHA-256(password) sebagai scalar
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password)
        return BigInteger(1, hash).mod(
            BigInteger.TWO.pow(252).add(BigInteger("27742317777372353535851937790883648493"))
        )
    }

    private fun buildTranscript(): ByteArray {
        // Transcript = myName || theirName || myMessage || theirMessage || password?
        // Untuk simplicity: myName || theirName || X || Y
        val buffer = ByteBuffer.allocate(
            myName.size + theirName.size + myMessage.size + theirMessage.size
        )
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(myName)
        buffer.put(theirName)
        buffer.put(myMessage)
        buffer.put(theirMessage)
        return buffer.array()
    }

    private fun hkdf(ikm: ByteArray, info: ByteArray, length: Int = 32): ByteArray {
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(ikm, null, info))
        val output = ByteArray(length)
        hkdf.generateBytes(output, 0, length)
        return output
    }

    // ==========================================================
    // ED25519 POINT ARITHMETIC (simplified)
    // ==========================================================

    /**
     * Scalar multiplication: result = scalar * point
     * Point dalam bentuk compressed (32 bytes little-endian).
     */
    private fun scalarMult(scalar: BigInteger, point: ByteArray): ByteArray {
        // Decode point → extended coords
        val p = decodePoint(point) ?: return ByteArray(32)

        // Scalar mul (double-and-add sederhana)
        var result = ExtendedPoint.identity()
        var base = p
        var k = scalar

        while (k > BigInteger.ZERO) {
            if (k.testBit(0)) {
                result = result.add(base)
            }
            base = base.double()
            k = k.shiftRight(1)
        }

        return encodePoint(result)
    }

    private fun scalarMultBase(scalar: BigInteger): ByteArray {
        return scalarMult(scalar, B)
    }

    private fun pointAdd(p1: ByteArray, p2: ByteArray): ByteArray {
        val a = decodePoint(p1) ?: return ByteArray(32)
        val b = decodePoint(p2) ?: return ByteArray(32)
        return encodePoint(a.add(b))
    }

    private fun pointSub(p1: ByteArray, p2: ByteArray): ByteArray {
        val a = decodePoint(p1) ?: return ByteArray(32)
        val b = decodePoint(p2) ?: return ByteArray(32)
        // p1 - p2 = p1 + (-p2)
        val negB = b.negate()
        return encodePoint(a.add(negB))
    }

    /**
     * Decode compressed Ed25519 point (32 bytes) → ExtendedPoint.
     * Format: y-coordinate (255 bit LE) + sign bit x
     */
    private fun decodePoint(data: ByteArray): ExtendedPoint? {
        if (data.size != 32) return null
        val yBytes = data.copyOf()
        val signBit = (yBytes[31].toInt() and 0x80) shr 7
        yBytes[31] = (yBytes[31].toInt() and 0x7F).toByte()

        val y = BigInteger(1, yBytes.reversedArray())
        if (y >= P) return null

        // x² = (y² - 1) / (d*y² + 1)
        val y2 = y.multiply(y).mod(P)
        val num = y2.subtract(BigInteger.ONE).mod(P)
        val den = D.multiply(y2).add(BigInteger.ONE).mod(P)
        val x2 = num.multiply(den.modInverse(P)).mod(P)
        var x = x2.modPow((P.add(BigInteger.valueOf(3))).divide(BigInteger.valueOf(8)), P)

        if (x.multiply(x).subtract(x2).mod(P) != BigInteger.ZERO) {
            x = x.multiply(I).mod(P)
        }
        if (x.testBit(0) != (signBit == 1)) {
            x = P.subtract(x)
        }

        return ExtendedPoint(x, y, BigInteger.ONE, x.multiply(y).mod(P))
    }

    /**
     * Encode ExtendedPoint → compressed 32 bytes.
     */
    private fun encodePoint(p: ExtendedPoint): ByteArray {
        val zInv = p.z.modInverse(P)
        val x = p.x.multiply(zInv).mod(P)
        val y = p.y.multiply(zInv).mod(P)

        val yBytes = y.toByteArray().let {
            if (it.size > 32) it.copyOfRange(it.size - 32, it.size)
            else if (it.size < 32) ByteArray(32 - it.size) + it
            else it
        }.reversedArray()

        if (x.testBit(0)) {
            yBytes[31] = (yBytes[31].toInt() or 0x80).toByte()
        }

        return yBytes
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = (
                (Character.digit(hex[i], 16) shl 4) +
                Character.digit(hex[i + 1], 16)
            ).toByte()
            i += 2
        }
        return data
    }

    /**
     * Extended coordinate representation untuk Ed25519.
     * (X, Y, Z, T) di mana x = X/Z, y = Y/Z, T = XY/Z
     */
    data class ExtendedPoint(
        val x: BigInteger,
        val y: BigInteger,
        val z: BigInteger,
        val t: BigInteger
    ) {
        fun add(other: ExtendedPoint): ExtendedPoint {
            val a = this.y.subtract(this.x).multiply(other.y.subtract(other.x)).mod(P)
            val b = this.y.add(this.x).multiply(other.y.add(other.x)).mod(P)
            val c = this.t.multiply(other.t).multiply(D).multiply(BigInteger.TWO).mod(P)
            val d = this.z.multiply(other.z).multiply(BigInteger.TWO).mod(P)
            val e = b.subtract(a).mod(P)
            val f = d.subtract(c).mod(P)
            val g = d.add(c).mod(P)
            val h = b.add(a).mod(P)

            return ExtendedPoint(
                x = e.multiply(f).mod(P),
                y = g.multiply(h).mod(P),
                z = f.multiply(g).mod(P),
                t = e.multiply(h).mod(P)
            )
        }

        fun double(): ExtendedPoint = add(this)

        fun negate(): ExtendedPoint = ExtendedPoint(
            x = P.subtract(x).mod(P),
            y = y,
            z = z,
            t = P.subtract(t).mod(P)
        )

        companion object {
            fun identity(): ExtendedPoint = ExtendedPoint(
                x = BigInteger.ZERO,
                y = BigInteger.ONE,
                z = BigInteger.ONE,
                t = BigInteger.ZERO
            )

            private val P: BigInteger = BigInteger.TWO.pow(255)
                .subtract(BigInteger.valueOf(19))
            private val D: BigInteger = BigInteger("-121665")
                .multiply(BigInteger("121666").modInverse(P))
                .mod(P)
        }
    }
}
