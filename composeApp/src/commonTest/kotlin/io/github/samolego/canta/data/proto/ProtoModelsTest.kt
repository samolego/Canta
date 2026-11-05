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
    fun analyticsFieldsRoundTrip() {
        val value = AppSettingsProto(
            analyticsEnabled = true,
            onboardingVersion = 1,
            pendingReboot = listOf(
                PendingUninstallProto("com.example.bloat", "boot-1", "2026-10-26", "11+", 1_700_000_000_000),
                PendingUninstallProto(pkg = "x", batchBucket = "2-10"),
            ),
        )
        val encoded = ProtoBuf.encodeToByteArray(AppSettingsProto.serializer(), value)
        assertEquals(value, ProtoBuf.decodeFromByteArray(AppSettingsProto.serializer(), encoded))
    }

    @Test
    fun analyticsFieldsUseNumbers11To13() {
        // analytics_enabled (11) = true, onboarding_version (12) = 1
        val decoded = ProtoBuf.decodeFromByteArray(AppSettingsProto.serializer(), bytes(0x58, 0x01, 0x60, 0x01))
        assertEquals(AppSettingsProto(analyticsEnabled = true, onboardingVersion = 1), decoded)
    }

    @Test
    fun pendingEntryWithoutNewFieldsDecodesWithDefaults() {
        // pkg (1) = "p", boot_id (2) = "b": an entry written before survived/fingerprint existed
        val old = bytes(0x0A, 0x01, 0x70, 0x12, 0x01, 0x62)
        val decoded = ProtoBuf.decodeFromByteArray(PendingUninstallProto.serializer(), old)
        assertEquals(PendingUninstallProto(pkg = "p", bootId = "b"), decoded)
        assertEquals(false, decoded.survived)
        assertEquals("", decoded.buildFingerprint)
    }

    @Test
    fun pendingSurvivedAndFingerprintRoundTripAtNumbers10And11() {
        val value = PendingUninstallProto(pkg = "p", survived = true, buildFingerprint = "f")
        val encoded = ProtoBuf.encodeToByteArray(PendingUninstallProto.serializer(), value)
        assertEquals(value, ProtoBuf.decodeFromByteArray(PendingUninstallProto.serializer(), encoded))
        // survived (10) = true, build_fingerprint (11) = "f"
        val decoded = ProtoBuf.decodeFromByteArray(PendingUninstallProto.serializer(), bytes(0x50, 0x01, 0x5A, 0x01, 0x66))
        assertEquals(PendingUninstallProto(survived = true, buildFingerprint = "f"), decoded)
    }

    @Test
    fun pendingDeviceKeyUsesNumber12AndDefaultsToBlank() {
        val value = PendingUninstallProto(pkg = "p", deviceKey = "k")
        val encoded = ProtoBuf.encodeToByteArray(PendingUninstallProto.serializer(), value)
        assertEquals(value, ProtoBuf.decodeFromByteArray(PendingUninstallProto.serializer(), encoded))
        // device_key (12) = "k"
        val decoded = ProtoBuf.decodeFromByteArray(PendingUninstallProto.serializer(), bytes(0x62, 0x01, 0x6B))
        assertEquals(PendingUninstallProto(deviceKey = "k"), decoded)
        // an entry from before the field existed
        val old = ProtoBuf.decodeFromByteArray(PendingUninstallProto.serializer(), bytes(0x0A, 0x01, 0x70))
        assertEquals("", old.deviceKey)
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
