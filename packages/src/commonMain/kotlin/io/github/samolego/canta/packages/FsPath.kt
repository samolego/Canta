package io.github.samolego.canta.packages

/*
 * Validation for device filesystem paths stored in presets and removed by
 * preset runs. Paths come from UI text fields or imported preset JSON and end
 * up in privileged deletion (`rm -rf` under Shizuku or `adb shell`), so
 * [isSafeDeletionPath] additionally requires the normalized path to sit
 * strictly inside a user-storage root.
 */

// Anything a POSIX shell could misinterpret (quotes, `$`, backticks,
// separators, globbing, escapes) plus ASCII control characters.
private val FORBIDDEN_CHARS = Regex("""['"`\\$;&|<>*?\[\]{}\p{C}]""")

/** Maximal path length the Linux kernel accepts (`PATH_MAX`). */
private const val PATH_MAX = 4096

/**
 * Roots a preset may delete inside: user-visible storage and the world-writable
 * tmp directory. `/data` itself is deliberately absent — it contains
 * `/data/system` and every app's private directory.
 */
private val DELETABLE_ROOTS = listOf("/sdcard", "/storage", "/data/media", "/data/local/tmp")

/**
 * Normalizes [path] to an absolute form free of `.`/`..` and duplicate
 * slashes, or returns null when it is not acceptable as a deletion target:
 * non-absolute, over `PATH_MAX`, escaping above `/`, or containing characters
 * matched by [FORBIDDEN_CHARS]. Trailing slashes are dropped.
 */
fun normalizeFsPath(path: String): String? {
    if (!path.startsWith('/') || path.length > PATH_MAX) return null
    val segments = ArrayDeque<String>()
    for (segment in path.removePrefix("/").split('/')) {
        when (segment) {
            // Empty segments come from consecutive (or trailing) slashes.
            "", "." -> {}
            ".." -> if (segments.isEmpty()) return null else segments.removeLast()
            else -> if (!FORBIDDEN_CHARS.containsMatchIn(segment)) segments.addLast(segment) else return null
        }
    }
    // The filesystem root itself is never a valid target.
    return if (segments.isEmpty()) null else "/" + segments.joinToString("/")
}

/** Whole-volume and per-user roots, including aliases of /sdcard. */
private val VOLUME_ROOTS = listOf(
    Regex("/storage/[^/]+"),
    Regex("/storage/emulated/[^/]+"),
    Regex("/storage/self/primary"),
    Regex("/data/media/[^/]+"),
)

/**
 * Whether [path] may be deleted by a preset run. Only normalized paths strictly
 * inside [DELETABLE_ROOTS] qualify — the roots themselves are refused (wiping
 * all of `/sdcard` is never a preset action), and so is anything under
 * `/system`, `/vendor`, `/data/data`, etc.
 */
fun isSafeDeletionPath(path: String): Boolean {
    val normalized = normalizeFsPath(path) ?: return false
    if (VOLUME_ROOTS.any { it.matches(normalized) }) return false
    return DELETABLE_ROOTS.any { normalized.startsWith("$it/") }
}

/** Single-quotes [arg] for POSIX shells; embedded quotes are closed and re-escaped. */
fun shellQuote(arg: String): String = "'" + arg.replace("'", "'\"'\"'") + "'"
