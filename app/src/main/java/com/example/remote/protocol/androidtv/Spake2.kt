package dev.andikuneiocontroll.remote.protocol.androidtv

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.SecureRandom

class Spake2(
    private val isClient: Boolean,
    private val myName: ByteArray,
    private val theirName: ByteArray,
    password: ByteArray
) {

    companion object {
        private val P: BigInteger = BigInteger.TWO.pow(255).subtract(BigInteger.valueOf(19))

        private val D: BigInteger = BigInteger.valueOf(-121665)
            .multiply(BigInteger.valueOf(121666).modInverse(P))
            .mod(P)

        private val I: BigInteger = BigInteger.TWO.modPow(
            P.subtract(BigInteger.ONE).divide(BigInteger.valueOf(4)),
            P
        )

        private val L: BigInteger = BigInteger.TWO.pow(252)
            .add(BigInteger("27742317777372353535851937790883648493"))

        private val M_HEX = "d048032c6ea0b6d697ddc2e86bda85a33adac920f1bf18e1b0c6d166a5cecdaf"
        private val N_HEX = "d3bfb518f44f3430f29d0c92af503865a1ed3281dc69b35dd868ba85f886c4ab"
        private val B_HEX = "5866666666666666666666666666666666666666666666666666666666666666"

        fun hexToBytes(hex: String): ByteArray {
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
    }

    private val random = SecureRandom()
    private val x: BigInteger = BigInteger(256, random).mod(L)
    private val w: BigInteger = derivePasswordScalar(password)
    private val M: ByteArray = hexToBytes(M_HEX)
    private val N: ByteArray = hexToBytes(N_HEX)
    private val B: ByteArray = hexToBytes(B_HEX)
    private val myPointX: ByteArray = scalarMult(x, B)

    private var myMessage: ByteArray = ByteArray(0)
    private var theirMessage: ByteArray = ByteArray(0)
    private var sharedKey: ByteArray = ByteArray(0)

    fun start(): ByteArray {
        return if (isClient) {
            val wM = scalarMult(w, M)
            val combined = pointAdd(myPointX, wM)
            myMessage = combined
            combined
        } else {
            val wN = scalarMult(w, N)
            val combined = pointAdd(myPointX, wN)
            myMessage = combined
            combined
        }
    }

    fun finish(theirMsg: ByteArray): ByteArray {
        theirMessage = theirMsg
        val shared = if (isClient) {
            val wN = scalarMult(w, N)
            val Y_minus_wN = pointSub(theirMsg, wN)
            scalarMult(x, Y_minus_wN)
        } else {
            val wM = scalarMult(w, M)
            val X_minus_wM = pointSub(theirMsg, wM)
            scalarMult(x, X_minus_wM)
        }
        val transcript = buildTranscript()
        sharedKey = hkdf(shared, transcript)
        return sharedKey
    }

    fun getSharedKey(): ByteArray = sharedKey
    fun getMyMessage(): ByteArray = myMessage

    fun computeConfirmationMac(key: ByteArray): ByteArray {
        val mac = HMac(SHA256Digest())
        mac.init(KeyParameter(key))
        mac.update(theirMessage, 0, theirMessage.size)
        val out = ByteArray(mac.macSize)
        mac.doFinal(out, 0)
        return out
    }

    private fun derivePasswordScalar(password: ByteArray): BigInteger {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password)
        return BigInteger(1, hash).mod(L)
    }

    private fun buildTranscript(): ByteArray {
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
    
    private fun scalarMult(scalar: BigInteger, point: ByteArray): ByteArray {
        val p = decodePoint(point) ?: return ByteArray(32)
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

    private fun pointAdd(p1: ByteArray, p2: ByteArray): ByteArray {
        val a = decodePoint(p1) ?: return ByteArray(32)
        val b = decodePoint(p2) ?: return ByteArray(32)
        return encodePoint(a.add(b))
    }

    private fun pointSub(p1: ByteArray, p2: ByteArray): ByteArray {
        val a = decodePoint(p1) ?: return ByteArray(32)
        val b = decodePoint(p2) ?: return ByteArray(32)
        return encodePoint(a.add(b.negate()))
    }

    private fun decodePoint(data: ByteArray): ExtendedPoint? {
        if (data.size != 32) return null
        val yBytes = data.copyOf()
        val signBit = (yBytes[31].toInt() and 0x80) shr 7
        yBytes[31] = (yBytes[31].toInt() and 0x7F).toByte()

        val y = BigInteger(1, yBytes.reversedArray())
        if (y >= P) return null

        val y2 = y.multiply(y).mod(P)
        val num = y2.subtract(BigInteger.ONE).mod(P)
        val den = D.multiply(y2).add(BigInteger.ONE).mod(P)
        val x2 = num.multiply(den.modInverse(P)).mod(P)
        var x = x2.modPow(
            (P.add(BigInteger.valueOf(3))).divide(BigInteger.valueOf(8)),
            P
        )
        if (x.multiply(x).subtract(x2).mod(P) != BigInteger.ZERO) {
            x = x.multiply(I).mod(P)
        }
        if (x.testBit(0) != (signBit == 1)) {
            x = P.subtract(x)
        }
        return ExtendedPoint(x, y, BigInteger.ONE, x.multiply(y).mod(P))
    }

    private fun encodePoint(p: ExtendedPoint): ByteArray {
        val zInv = p.z.modInverse(P)
        val x = p.x.multiply(zInv).mod(P)
        val y = p.y.multiply(zInv).mod(P)
        val yBytes = y.toByteArray().let {
            when {
                it.size > 32 -> it.copyOfRange(it.size - 32, it.size)
                it.size < 32 -> ByteArray(32 - it.size) + it
                else -> it
            }
        }.reversedArray()
        if (x.testBit(0)) {
            yBytes[31] = (yBytes[31].toInt() or 0x80).toByte()
        }
        return yBytes
    }

    data class ExtendedPoint(
        val x: BigInteger,
        val y: BigInteger,
        val z: BigInteger,
        val t: BigInteger
    ) {
        fun add(other: ExtendedPoint): ExtendedPoint {
            val a = this.y.subtract(this.x)
                .multiply(other.y.subtract(other.x)).mod(P)
            val b = this.y.add(this.x)
                .multiply(other.y.add(other.x)).mod(P)
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
        }
    }
}
