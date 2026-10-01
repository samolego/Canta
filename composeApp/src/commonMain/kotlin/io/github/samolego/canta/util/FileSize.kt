package io.github.samolego.canta.util

import kotlin.math.roundToLong

private val UNITS = listOf("B", "kB", "MB", "GB", "TB")

/**
 * Formats a byte count for display with SI units (1 kB = 1000 B), matching
 * Android's `Formatter.formatShortFileSize`: e.g. `980 B`, `1.5 MB`, `12 MB`.
 * One decimal is shown below 10 units, none above.
 */
fun formatFileSize(bytes: Long): String {
    if (bytes < 1000) return "$bytes B"
    var value = bytes / 1000.0
    var unit = 1
    while (true) {
        // Round before choosing the unit, so 999_999 B is "1.0 MB", not "1000 kB".
        val text = if (value < 9.95) {
            val tenths = (value * 10).roundToLong()
            "${tenths / 10}.${tenths % 10}"
        } else {
            val whole = value.roundToLong()
            if (whole >= 1000 && unit < UNITS.lastIndex) {
                value /= 1000
                unit++
                continue
            }
            whole.toString()
        }
        return "$text ${UNITS[unit]}"
    }
}
