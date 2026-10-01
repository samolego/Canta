package io.github.samolego.canta.data

import io.github.samolego.canta.data.proto.AppSettingsProto
import io.github.samolego.canta.data.proto.ProtoBlobStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileAppStorageTest {

    private val directory = Files.createTempDirectory("canta-storage").toFile().apply { deleteOnExit() }

    @Test
    fun writesAndReadsBackWithoutLeftoverTempFiles() = runTest {
        val file = directory.resolve("nested/app_settings.pb")
        val storage = FileAppStorage(file)
        assertNull(storage.read())

        storage.write(byteArrayOf(1, 2, 3))

        assertContentEquals(byteArrayOf(1, 2, 3), storage.read())
        assertEquals(listOf("app_settings.pb"), file.parentFile.list()!!.toList(), "no temp files left behind")
    }

    @Test
    fun storeUpdatesThroughRealFilesSurviveReload() = runTest {
        val file = directory.resolve("settings.pb")
        val store = ProtoBlobStore(
            FileAppStorage(file),
            AppSettingsProto.serializer(),
            AppSettingsProto(),
            "test"
        )

        (1..20).map { async { store.update { it.copy(latestBloatCommitHash = it.latestBloatCommitHash + "x") } } }.awaitAll()

        val reloaded = ProtoBlobStore(
            FileAppStorage(file),
            AppSettingsProto.serializer(),
            AppSettingsProto(),
            "test"
        )
        assertEquals(20, reloaded.state.value.latestBloatCommitHash.length)
        assertTrue(directory.list()!!.none { it.endsWith(".tmp") })
    }
}
