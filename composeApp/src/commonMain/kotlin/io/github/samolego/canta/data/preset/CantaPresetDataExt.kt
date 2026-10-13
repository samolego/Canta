package io.github.samolego.canta.data.preset

import io.github.samolego.canta.util.currentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun generateUuid(): String = Uuid.random().toString()

/** A new preset of [apps], created now. */
fun newPreset(name: String, description: String, apps: Set<String>): CantaPresetData =
    CantaPresetData(
        name = name,
        description = description,
        createdDate = currentTimeMillis(),
        apps = apps,
        uuid = generateUuid(),
    )

/*
 * Presets are matched by UUID; presets created before UUIDs existed fall back
 * to name + creation date.
 */

internal fun CantaPresetData.ensureUuid(): CantaPresetData =
    if (uuid.isEmpty()) copy(uuid = generateUuid()) else this

internal fun CantaPresetData.matches(other: CantaPresetData): Boolean =
    if (uuid.isNotEmpty() && other.uuid.isNotEmpty()) {
        uuid == other.uuid
    } else {
        name == other.name && createdDate == other.createdDate
    }
