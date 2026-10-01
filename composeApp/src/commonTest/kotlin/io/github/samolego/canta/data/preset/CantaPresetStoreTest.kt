@file:OptIn(ExperimentalSerializationApi::class)

package io.github.samolego.canta.data.preset

import io.github.samolego.canta.data.proto.CantaPresetProto
import io.github.samolego.canta.data.proto.PresetsListProto
import io.github.samolego.canta.testing.MemoryStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CantaPresetStoreTest {

    @Test
    fun presetsFromBeforeUuidsGetOneThatSurvivesReload() = runTest {
        val legacy = PresetsListProto(
            listOf(
                CantaPresetProto(name = "A", createdDate = 1, apps = listOf("com.a")),
                CantaPresetProto(name = "B", createdDate = 2, apps = listOf("com.b")),
            )
        )
        val storage = MemoryStorage(ProtoBuf.encodeToByteArray(PresetsListProto.serializer(), legacy))

        val store = CantaPresetStore(storage)
        val uuids = store.presetsFlow.first().map { it.uuid }
        assertTrue(uuids.all { it.isNotEmpty() })
        assertEquals(2, uuids.toSet().size)

        // The UUIDs the UI already holds are the ones persisted with the next update.
        store.savePreset(
            newPreset(
                "C",
                "",
                setOf("com.c")
            )
        )
        val reloaded = CantaPresetStore(storage).presetsFlow.first()
        assertEquals(listOf("A", "B", "C"), reloaded.map { it.name })
        assertEquals(uuids, reloaded.take(2).map { it.uuid })
    }

    @Test
    fun presetsImportedTwiceAreEditedAndDeletedIndependently() = runTest {
        val json = exportPresetToJson(
            newPreset(
                "Shared",
                "",
                setOf("com.a")
            )
        )
        val first = importPresetFromJson(json)!!
        val second = importPresetFromJson(json)!!
        val store = CantaPresetStore(MemoryStorage())
        store.savePreset(first)
        store.savePreset(second)

        store.setPresetApps(second, setOf("com.b"))
        assertEquals(listOf(setOf("com.a"), setOf("com.b")), store.presetsFlow.first().map { it.apps })

        store.deletePreset(first)
        val remaining = store.presetsFlow.first()
        assertEquals(listOf(second.uuid), remaining.map { it.uuid })
        assertNotEquals(first.uuid, second.uuid)
    }
}
