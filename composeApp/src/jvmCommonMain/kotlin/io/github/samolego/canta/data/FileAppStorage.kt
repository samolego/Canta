package io.github.samolego.canta.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * [AppBlobStorage] backed by a single file, on JVM targets (Android, desktop).
 * Writes go to a temporary file that is then atomically moved over the
 * target, so a crash mid-write never leaves a truncated file behind.
 */
class FileAppStorage(
    private val file: File,
) : AppBlobStorage {

    override fun read(): ByteArray? = if (file.exists()) file.readBytes() else null

    override suspend fun write(content: ByteArray) {
        withContext(Dispatchers.IO) {
            // The temp file must be on the same filesystem for the atomic move.
            val directory = checkNotNull(file.absoluteFile.parentFile) { "$file has no parent directory" }
                .apply { mkdirs() }
            // A fresh temp file per write, so concurrent writers never share one.
            val tmp = File.createTempFile(file.name, ".tmp", directory)
            tmp.writeBytes(content)
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
