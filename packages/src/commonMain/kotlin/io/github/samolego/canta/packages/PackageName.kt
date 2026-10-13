package io.github.samolego.canta.packages

/**
 * Android's package name grammar: dot-separated Java-like identifiers. Apps
 * need at least one dot, but system packages don't (the framework is `android`).
 */
private val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*$")

/**
 * Whether [name] is a syntactically valid Android package name. Package names
 * are interpolated into device shell commands, so anything else (e.g. from an
 * imported preset) must be rejected before it gets there.
 */
fun isValidPackageName(name: String): Boolean = PACKAGE_NAME.matches(name)
