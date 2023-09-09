package org.samo_lego.canta

/**
 * See <a href="https://github.com/0x192/universal-android-debloater/wiki/FAQ#how-are-the-recommendations-chosen>recommendations</a>
 */
enum class AppType(type: String) {
    RECOMMENDED("Recommended"),
    ADVANCED("Advanced"),
    EXPERT("Expert"),
    UNSAFE("Unsafe"),
    UNKNOWN("Unknown"),
}