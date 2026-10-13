package io.github.samolego.canta.data

/**
 * Minimal blob persistence behind the shared stores and caches.
 * Implemented per platform: `FileAppStorage` (Android, desktop) and
 * `LocalStorageAppStorage` (web).
 */
interface AppBlobStorage {
    /** The stored bytes, or `null` when nothing is stored yet. Called once at startup. */
    fun read(): ByteArray?

    /** Replaces the stored bytes; implementations that block move off the caller's thread. */
    suspend fun write(content: ByteArray)
}
