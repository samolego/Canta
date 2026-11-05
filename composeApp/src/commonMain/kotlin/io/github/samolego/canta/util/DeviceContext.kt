package io.github.samolego.canta.util

/**
 * Coarse description of the phone, sent once per analytics request: the
 * manufacturer, the ROM family and its major version, and the Android API
 * level. Never the model, build number, fingerprint or anything else that
 * could single a device out.
 */
data class DeviceContext(
    val brand: String,
    val rom: String,
    val romVersion: Int,
    val sdk: Int,
) {
    companion object {
        /** Used when the device can't be read. */
        val UNKNOWN = DeviceContext("unknown", "unknown", 0, 0)
    }

    /** Whether per-ROM scores can be asked for. */
    val hasRom: Boolean get() = rom != "unknown" && rom != "other"

    /** "HyperOS 1", or just "Android" for stock. */
    fun romLabel(): String {
        val name = romName(rom)
        return if (romVersion > 0 && name != "Android") "$name $romVersion" else name
    }
}

/** Display name of a ROM family ("hyperos" becomes "HyperOS"), "Android" for stock or unknown ones. */
fun romName(rom: String): String = when (rom) {
    "hyperos" -> "HyperOS"
    "miui" -> "MIUI"
    "oneui" -> "One UI"
    "coloros" -> "ColorOS"
    "realmeui" -> "realme UI"
    "originos" -> "OriginOS"
    "funtouch" -> "Funtouch OS"
    "magicos" -> "MagicOS"
    "emui" -> "EMUI"
    "oxygenos" -> "OxygenOS"
    "nothingos" -> "Nothing OS"
    else -> "Android"
}

/** The only system properties ever read to build a [DeviceContext]. */
val DEVICE_PROP_KEYS: Set<String> = setOf(
    "ro.product.manufacturer",
    "ro.build.version.sdk",
    "ro.mi.os.version.name",
    "ro.miui.ui.version.name",
    "ro.miui.ui.version.code",
    "ro.build.version.oneui",
    "ro.build.version.realmeui",
    "ro.build.version.opporom",
    "ro.vivo.os.version",
    "ro.vivo.os.name",
    "ro.build.version.magic",
    "ro.build.version.emui",
    "ro.oxygen.version",
    "ro.rom.version",
    "ro.build.nothing.version",
)

/**
 * Parses `getprop` output (`[key]: [value]` lines), keeping only [keys];
 * everything else is dropped immediately.
 */
fun parseGetprop(output: String, keys: Set<String> = DEVICE_PROP_KEYS): Map<String, String> {
    val result = mutableMapOf<String, String>()
    for (line in output.lineSequence()) {
        val l = line.trim()
        if (!l.startsWith("[")) continue
        val keyEnd = l.indexOf("]: [")
        if (keyEnd < 0 || !l.endsWith("]")) continue
        val key = l.substring(1, keyEnd)
        if (key in keys) result[key] = l.substring(keyEnd + 4, l.length - 1)
    }
    return result
}

/** First run of digits in [s] as an int, or null. */
private fun leadingNumber(s: String?): Int? {
    val digits = s?.dropWhile { !it.isDigit() }?.takeWhile { it.isDigit() } ?: return null
    return digits.toIntOrNull()
}

private fun miuiMajor(name: String?, code: String?): Int? {
    code?.trim()?.toIntOrNull()?.takeIf { it in 1..20 }?.let { return it }
    val digits = name?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() } ?: return null
    val n = digits.toIntOrNull() ?: return null
    if (n <= 20) return n
    return digits.take(2).toInt().takeIf { it <= 20 } ?: digits.take(1).toInt()
}

/** Builds the [DeviceContext] from system properties (see [DEVICE_PROP_KEYS]). */
fun deviceContextOf(props: Map<String, String>): DeviceContext {
    fun prop(key: String) = props[key]?.trim()?.takeIf { it.isNotEmpty() }

    val brand = prop("ro.product.manufacturer")?.lowercase() ?: "unknown"
    val sdk = prop("ro.build.version.sdk")?.toIntOrNull() ?: 0

    fun ctx(rom: String, version: Int?) = DeviceContext(brand, rom, version ?: 0, sdk)

    prop("ro.mi.os.version.name")?.let { return ctx("hyperos", leadingNumber(it)) }
    if (prop("ro.miui.ui.version.name") != null || prop("ro.miui.ui.version.code") != null) {
        return ctx("miui", miuiMajor(prop("ro.miui.ui.version.name"), prop("ro.miui.ui.version.code")))
    }
    prop("ro.build.version.oneui")?.let { v -> v.toIntOrNull()?.let { return ctx("oneui", it / 10000) } }
    prop("ro.build.version.realmeui")?.let { return ctx("realmeui", leadingNumber(it)) }
    prop("ro.build.version.opporom")?.let { return ctx("coloros", leadingNumber(it)) }
    prop("ro.vivo.os.version")?.let {
        val rom = if (prop("ro.vivo.os.name")?.contains("origin", ignoreCase = true) == true) "originos" else "funtouch"
        return ctx(rom, leadingNumber(it))
    }
    prop("ro.build.version.magic")?.let { return ctx("magicos", leadingNumber(it)) }
    prop("ro.build.version.emui")?.let { return ctx("emui", leadingNumber(it)) }
    prop("ro.oxygen.version")?.let { return ctx("oxygenos", leadingNumber(it)) }
    prop("ro.rom.version")?.takeIf { it.contains("oxygenos", ignoreCase = true) }
        ?.let { return ctx("oxygenos", leadingNumber(it)) }
    prop("ro.build.nothing.version")?.let { return ctx("nothingos", leadingNumber(it)) }
    return ctx("stock", 0)
}
