package io.github.samolego.canta.data.preset

import io.github.samolego.canta.packages.isValidPackageName
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Instant

private val presetJson = Json { prettyPrint = true }

fun exportPresetToJson(preset: CantaPresetData): String {
    val json =
        buildJsonObject {
            put("name", preset.name)
            put("description", preset.description)
            put("createdDate", preset.createdDate)
            put("version", preset.version)
            put(
                "apps",
                buildJsonArray {
                    preset.apps.sorted().forEach { add(JsonPrimitive(it)) }
                }
            )
        }
    return presetJson.encodeToString(JsonObject.serializer(), json)
}

/**
 * Import a preset from a JSON string. Supports:
 *  - New format: `{ "apps": ["pkg1", "pkg2"] }`
 *  - Legacy format: `{ "apps": [ { "packageName": "pkg1" } ] }`
 * Package names that do not pass [packageExists] are dropped.
 */
suspend fun importPresetFromJson(
    jsonString: String,
    packageExists: suspend (String) -> Boolean = { true }
): CantaPresetData? {
    return try {
        val root = presetJson.parseToJsonElement(jsonString).jsonObject
        val apps = mutableSetOf<String>()
        val appsElement = root["apps"] ?: return null
        val appsArray = appsElement.jsonArray

        for (element in appsArray) {
            val packageName =
                try {
                    // New format: array of package-name strings
                    element.jsonPrimitive.content
                } catch (e: Exception) {
                    // Legacy format: array of { "packageName": "..." } objects
                    element.jsonObject["packageName"]?.jsonPrimitive?.content
                }
            // Imported presets are untrusted: package names end up in shell commands.
            if (packageName == null || !isValidPackageName(packageName) || !packageExists(packageName)) continue
            apps.add(packageName)
        }

        CantaPresetData(
            name = root["name"]?.jsonPrimitive?.content ?: return null,
            description = root["description"]?.jsonPrimitive?.content ?: "",
            createdDate = root["createdDate"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            apps = apps,
            version = root["version"]?.jsonPrimitive?.content ?: "1.0",
            uuid = generateUuid()
        )
    } catch (_: Exception) {
        null
    }
}

private val PRESET_DATE_FORMAT = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); day(); chars(", "); year()
    char(' '); hour(); char(':'); minute()
}

/**
 * Formats a preset's creation time in the device's time zone, e.g.
 * `Sep 29, 2026 13:07`. Month names are English: common code has no
 * locale-aware date formatter.
 */
fun formatPresetDate(timestamp: Long): String =
    PRESET_DATE_FORMAT.format(
        Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    )
