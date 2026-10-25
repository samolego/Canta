package io.github.samolego.canta.data.preset

import io.github.samolego.canta.CANTA_PACKAGE_NAME
import io.github.samolego.canta.data.AppBlobStorage
import io.github.samolego.canta.data.proto.CantaPresetProto
import io.github.samolego.canta.data.proto.PresetsListProto
import io.github.samolego.canta.data.proto.ProtoBlobStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Saved presets, persisted as one protobuf-encoded [PresetsListProto] in
 * [storage]; platforms only choose where that is. Every mutation returns
 * whether it was persisted.
 */
class CantaPresetStore(storage: AppBlobStorage) {

    private val store = ProtoBlobStore(
        storage = storage,
        serializer = PresetsListProto.serializer(),
        default = PresetsListProto(),
        tag = "CantaPresetStore",
        // Presets created before UUIDs existed get one.
        migrate = { list ->
            list.copy(presets = list.presets.map {
                it.toPresetData().ensureUuid().toProto()
            })
        },
    )

    val presetsFlow: Flow<List<CantaPresetData>> =
        store.state.map { list -> list.presets.map { it.toPresetData() } }

    private suspend fun update(transform: (List<CantaPresetData>) -> List<CantaPresetData>): Boolean =
        store.update { list ->
            PresetsListProto(transform(list.presets.map { it.toPresetData() }).map { it.toProto() })
        }

    suspend fun savePreset(preset: CantaPresetData): Boolean =
        update { it + preset.ensureUuid() }

    suspend fun deletePreset(preset: CantaPresetData): Boolean =
        update { presets -> presets.filterNot { it.matches(preset) } }

    suspend fun updatePreset(oldPreset: CantaPresetData, newPreset: CantaPresetData): Boolean =
        update { presets -> presets.map { if (it.matches(oldPreset)) newPreset.ensureUuid() else it } }

    suspend fun setPresetApps(preset: CantaPresetData, newApps: Set<String>): Boolean =
        updatePreset(preset, preset.copy(apps = newApps))
}

private fun CantaPresetProto.toPresetData(): CantaPresetData = CantaPresetData(
    name = name,
    description = description,
    createdDate = createdDate,
    // Canta's own package is never a preset app: self-removal goes through
    // removeCantaAfterRun, and counting it would show a phantom app.
    apps = apps.toSet() - CANTA_PACKAGE_NAME,
    version = version.ifEmpty { "1.0" },
    uuid = uuid,
    folders = folders.toSet(),
    removeCantaAfterRun = removeCantaAfterRun,
)

private fun CantaPresetData.toProto(): CantaPresetProto = CantaPresetProto(
    name = name,
    description = description,
    createdDate = createdDate,
    apps = (apps - CANTA_PACKAGE_NAME).toList(),
    version = version,
    uuid = uuid,
    folders = folders.toList(),
    removeCantaAfterRun = removeCantaAfterRun,
)
