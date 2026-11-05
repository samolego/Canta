package io.github.samolego.canta.util

import io.github.samolego.canta.data.app.AppInfo

/** Package that always gets a bootloop verdict (overall and for the device), for trying out the warnings. */
const val FAKE_BOOTLOOP_PACKAGE = "com.android.egg"

/** FNV-1a, so the result is the same on every platform and run. */
private fun hashOf(s: String): Long {
    var h = 0xcbf29ce484222325uL
    for (c in s) h = (h xor c.code.toULong()) * 0x100000001b3uL
    return (h shr 1).toLong()
}

private fun fakeScore(pkg: String, verdict: Verdict, seed: Long, uninstalls: IntRange): PackageScore {
    val exact = if (verdict == Verdict.INSUFFICIENT) 3 + (seed % 27).toInt()
    else uninstalls.first + (seed % (uninstalls.last - uninstalls.first + 1)).toInt()
    // Like the server: floored to hundreds, so 0 means fewer than 100.
    val count = exact / 100 * 100
    return PackageScore(pkg, count, smallBatch = seed % 5 == 0L, verdict, "2026-10-29")
}

/** A verdict to show on the device row that differs from [overall]. */
private fun differentFrom(overall: Verdict) = when (overall) {
    Verdict.BOOTLOOP -> Verdict.SAFE
    Verdict.BREAKS -> Verdict.BOOTLOOP
    Verdict.SAFE -> Verdict.BREAKS
    Verdict.UNCLEAR -> Verdict.SAFE
    else -> null
}

/**
 * Made-up community scores for the system apps among [apps], for developing
 * and testing the UI without a server. Deterministic per package name; every
 * verdict occurs, and the apps with a verdict are spread over every case of
 * [chooseVerdict]: version row only, family row only, family mostly from another version (information only), both, overall mostly from
 * another ROM (information only), overall mixed or mostly from [device]'s ROM.
 * [FAKE_BOOTLOOP_PACKAGE] is always an actionable bootloop.
 */
fun fakeScores(apps: List<AppInfo>, device: DeviceContext?): Map<String, CommunityScore> {
    val known = device?.takeIf { it.hasRom }
    val label = (device ?: DeviceContext.UNKNOWN).romLabel()
    val familyLabel = known?.let { romName(it.rom) }.orEmpty()
    val ownRom = known?.rom ?: "hyperos"
    val otherRom = if (ownRom == "oneui") "miui" else "oneui"
    val result = mutableMapOf<String, CommunityScore>()
    for (app in apps) {
        if (!app.isSystemApp) continue
        val pkg = app.packageName
        val seed = hashOf(pkg)
        if (pkg == FAKE_BOOTLOOP_PACKAGE) {
            result[pkg] = CommunityScore(
                fakeScore(pkg, Verdict.BOOTLOOP, seed, 200..2000).copy(mainRom = ownRom, mainRomShare = 0.6),
                fakeScore(pkg, Verdict.BOOTLOOP, seed shr 3, 30..400),
                label,
                fakeScore(pkg, Verdict.BOOTLOOP, seed shr 5, 30..800),
                known?.rom,
                familyLabel,
                known?.romVersion ?: 0,
            )
            continue
        }
        val overallVerdict = when (seed % 12) {
            0L -> continue // nothing known about this one
            1L -> Verdict.BOOTLOOP
            2L, 3L -> Verdict.BREAKS
            4L, 5L, 6L -> Verdict.SAFE
            7L, 8L -> Verdict.UNCLEAR
            else -> Verdict.INSUFFICIENT
        }
        val other = differentFrom(overallVerdict)
        // Which rows exist: version only, family only, both, other ROM's data, mixed, own ROM's.
        val case = if (other == null) listOf(2, 3, 5)[((seed / 12) % 3).toInt()] else ((seed / 12) % 7).toInt()
        val mainRom = when (case) {
            2 -> otherRom
            3 -> otherRom
            else -> ownRom
        }
        val share = when (case) {
            2 -> 0.9
            3 -> 0.5
            else -> 0.9
        }
        val deviceRow = if (case == 0 || case == 4) fakeScore(pkg, other!!, seed shr 3, 30..400) else null
        val ownVersion = known?.romVersion?.takeIf { it > 0 } ?: 1
        val familyRow = when (case) {
            1 -> fakeScore(pkg, other!!, seed shr 5, 30..800).copy(mainVersion = ownVersion, mainVersionShare = 0.6)
            4 -> fakeScore(pkg, overallVerdict, seed shr 5, 30..800).copy(mainVersion = ownVersion, mainVersionShare = 0.5)
            // Mostly another version of the same ROM: information only.
            6 -> fakeScore(pkg, other!!, seed shr 5, 30..800)
                .copy(mainVersion = if (ownVersion == 1) 2 else 1, mainVersionShare = 0.9)
            else -> null
        }
        result[pkg] = CommunityScore(
            fakeScore(pkg, overallVerdict, seed, 30..2000).copy(mainRom = mainRom, mainRomShare = share),
            deviceRow,
            label,
            familyRow,
            known?.rom,
            familyLabel,
            known?.romVersion ?: 0,
        )
    }
    return result
}
