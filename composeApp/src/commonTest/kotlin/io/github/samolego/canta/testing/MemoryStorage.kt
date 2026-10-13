package io.github.samolego.canta.testing

import io.github.samolego.canta.data.AppBlobStorage
import kotlinx.coroutines.yield

/**
 * In-memory [AppBlobStorage] whose writes can be made to fail, or to suspend
 * ([yieldOnWrite]) like real storage does, so concurrent callers interleave.
 */
class MemoryStorage(var content: ByteArray? = null) : AppBlobStorage {
    var failWrites = false
    var yieldOnWrite = false
    var writes = 0
        private set

    override fun read(): ByteArray? = content

    override suspend fun write(content: ByteArray) {
        if (yieldOnWrite) yield()
        if (failWrites) throw IllegalStateException("disk full")
        writes++
        this.content = content
    }
}
