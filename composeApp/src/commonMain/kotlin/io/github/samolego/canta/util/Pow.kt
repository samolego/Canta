package io.github.samolego.canta.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.coroutines.coroutineContext

private const val CHECK_EVERY = 4096

/** Lowercase hex SHA-256 of the UTF-8 [payload]. */
fun payloadHash(payload: String): String =
    SHA256().digest(payload.encodeToByteArray()).toHex()

/** Hashes below this (read as the first 8 bytes, big-endian) solve the work for [nEvents] events. */
fun powTarget(nEvents: Int): ULong {
    require(nEvents > 0) { "nEvents must be positive" }
    // floor(2^64 / d) == floor((2^64 - 1) / d) as d = 10000 * n is never a power of two.
    return ULong.MAX_VALUE / (TelemetryConfig.POW_K.toULong() * nEvents.toULong())
}

/**
 * Smallest `x >= 0` for which
 * `sha256(challenge + ":" + payloadHash(payload) + ":" + x)` is below [powTarget].
 * Runs on [Dispatchers.Default] and is cancellable.
 */
suspend fun solvePow(challenge: String, payload: String, nEvents: Int): Long =
    withContext(Dispatchers.Default) {
        val ctx = coroutineContext
        val target = powTarget(nEvents)
        val prefix = "$challenge:${payloadHash(payload)}:".encodeToByteArray()
        // Room for the prefix plus up to 19 decimal digits.
        val buf = prefix.copyOf(prefix.size + 20)
        val sha = SHA256()
        var x = 0L
        while (true) {
            if (x % CHECK_EVERY == 0L) ctx.ensureActive()
            val len = writeDecimal(buf, prefix.size, x)
            sha.update(buf, 0, len)
            val d = sha.digest()
            var head = 0UL
            for (i in 0 until 8) head = (head shl 8) or (d[i].toULong() and 0xFFUL)
            if (head < target) return@withContext x
            x++
        }
        @Suppress("UNREACHABLE_CODE")
        0L
    }

/** Writes [value] (non-negative) as ASCII digits at [offset]; returns the end index. */
private fun writeDecimal(buf: ByteArray, offset: Int, value: Long): Int {
    if (value == 0L) {
        buf[offset] = '0'.code.toByte()
        return offset + 1
    }
    var digits = 0
    var v = value
    while (v > 0) { digits++; v /= 10 }
    v = value
    for (i in offset + digits - 1 downTo offset) {
        buf[i] = ('0'.code + (v % 10).toInt()).toByte()
        v /= 10
    }
    return offset + digits
}

private const val HEX = "0123456789abcdef"

private fun ByteArray.toHex(): String = buildString(size * 2) {
    for (b in this@toHex) {
        val i = b.toInt() and 0xFF
        append(HEX[i shr 4]).append(HEX[i and 0xF])
    }
}
