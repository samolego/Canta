package io.github.samolego.canta.data.app

/** How risky removing a package is, as classified by the UAD bloat list. */
enum class RemovalLevel {
    RECOMMENDED,
    ADVANCED,
    EXPERT,
    UNSAFE;

    companion object {
        fun byNameIgnoreCaseOrNull(input: String): RemovalLevel? {
            return entries.firstOrNull { it.name.equals(input, true) }
        }
    }
}
