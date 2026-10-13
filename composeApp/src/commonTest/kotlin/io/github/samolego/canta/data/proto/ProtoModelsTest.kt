@file:OptIn(ExperimentalSerializationApi::class)

package io.github.samolego.canta.data.proto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Guards byte compatibility with the proto3 files written by the former
 * protobuf-javalite stores. The expected bytes are hand-encoded proto3 wire
 * format for the original `settings.proto` / `presets.proto`.
 */
class ProtoModelsTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    // confirm_before_uninstall (2) = true, bloat_list_url (5) = "u";
    // every other field is zero-valued and therefore absent.
    private val settingsBytes = bytes(0x10, 0x01, 0x2A, 0x01, 0x75)
    private val settings = AppSettingsProto(confirmBeforeUninstall = true, bloatListUrl = "u")

    // PresetsList { presets (1) = CantaPreset { name = "a", created_date = 5, apps = ["x", "y"] } }
    private val presetsBytes = bytes(
        0x0A, 0x0B,
        0x0A, 0x01, 0x61,
        0x18, 0x05,
        0x22, 0x01, 0x78,
        0x22, 0x01, 0x79,
    )
    private val presets = PresetsListProto(
        listOf(CantaPresetProto(name = "a", createdDate = 5, apps = listOf("x", "y")))
    )

    @Test
    fun decodesLegacySettings() {
        assertEquals(settings, ProtoBuf.decodeFromByteArray(AppSettingsProto.serializer(), settingsBytes))
    }

    @Test
    fun encodesSettingsLikeProto3() {
        assertContentEquals(settingsBytes, ProtoBuf.encodeToByteArray(AppSettingsProto.serializer(), settings))
    }

    @Test
    fun retiredFieldTenIsIgnored() {
        // disable_adb_helper (10) = true, written by development builds
        assertEquals(AppSettingsProto(), ProtoBuf.decodeFromByteArray(AppSettingsProto.serializer(), bytes(0x50, 0x01)))
    }


    @Test
    fun decodesLegacyPresets() {
        assertEquals(presets, ProtoBuf.decodeFromByteArray(PresetsListProto.serializer(), presetsBytes))
    }

    @Test
    fun encodesPresetsLikeProto3() {
        assertContentEquals(presetsBytes, ProtoBuf.encodeToByteArray(PresetsListProto.serializer(), presets))
    }
}
