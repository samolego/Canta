package io.github.samolego.canta.data.proto

import io.github.samolego.canta.testing.MemoryStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtoBlobStoreTest {

    private fun store(storage: MemoryStorage) =
        ProtoBlobStore(storage, AppSettingsProto.serializer(), AppSettingsProto(), "test")

    @Test
    fun failedWriteLeavesStateUnchanged() = runTest {
        val storage = MemoryStorage().apply { failWrites = true }
        val store = store(storage)

        assertFalse(store.update { it.copy(authEnabled = true) })
        assertEquals(false, store.state.value.authEnabled)
    }

    @Test
    fun updatesPersistAndSurviveReload() = runTest {
        val storage = MemoryStorage()
        assertTrue(store(storage).update { it.copy(bloatListUrl = "u") })
        assertEquals("u", store(storage).state.value.bloatListUrl)
    }

    @Test
    fun concurrentUpdatesAreNotLost() = runTest {
        // Writes suspend, so without serialization updates would interleave and overwrite each other.
        val storage = MemoryStorage().apply { yieldOnWrite = true }
        val store = store(storage)

        (1..50).map { async { store.update { it.copy(latestBloatCommitHash = it.latestBloatCommitHash + "x") } } }.awaitAll()

        assertEquals(50, store.state.value.latestBloatCommitHash.length)
        assertEquals(50, storage.writes)
    }

    @Test
    fun unreadableDataFallsBackToDefaultWithoutOverwriting() = runTest {
        val garbage = byteArrayOf(0x7F, 0x7F, 0x7F)
        val storage = MemoryStorage(garbage)
        val store = ProtoBlobStore(
            storage,
            AppSettingsProto.serializer(),
            AppSettingsProto(authEnabled = true),
            "test"
        )

        assertEquals(true, store.state.value.authEnabled)
        assertEquals(0, storage.writes)
    }
}
