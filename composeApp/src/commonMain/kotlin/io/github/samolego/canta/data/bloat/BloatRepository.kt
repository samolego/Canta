package io.github.samolego.canta.data.bloat

import io.github.samolego.canta.data.AppBlobStorage
import io.github.samolego.canta.data.CantaSettings
import io.github.samolego.canta.data.app.AppBadgeInfo
import io.github.samolego.canta.util.LogUtils
import io.github.samolego.canta.util.currentTimeMillis
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val DEFAULT_BLOAT_URL =
    "https://raw.githubusercontent.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation/main/resources/assets/uad_lists.json"
const val DEFAULT_BLOAT_COMMITS_URL =
    "https://api.github.com/repos/Universal-Debloater-Alliance/universal-android-debloater-next-generation/commits?path=resources%2Fassets%2Fuad_lists.json"

/** An app's entry in the UAD bloat list. */
data class BloatData(
    internal val description: String?,
    internal val badgeInfo: AppBadgeInfo?,
)

/**
 * Provides the UAD bloat list, cached in [cache] and refreshed from GitHub
 * when a newer commit exists (and the user allows auto-updates).
 *
 * A usable cached list is never thrown away because of a failed download,
 * and a corrupt cache is replaced by a fresh download.
 */
class BloatRepository(
    private val client: HttpClient,
    private val cache: AppBlobStorage,
    private val settings: CantaSettings,
) {
    private companion object {
        const val TAG = "BloatRepository"
    }

    suspend fun load(): Map<String, BloatData> {
        val cached = readCache()
        if (cached == null) {
            LogUtils.i(TAG, "No cached bloat list; downloading it")
        } else if (!settings.autoUpdateBloatListFlow.first()) {
            LogUtils.i(TAG, "Bloat list auto-update is off; using the cached list")
            return cached
        }

        // Empty when unknown (offline, rate limited, ...).
        val remoteHash = fetchLatestHash()
        if (cached != null) {
            if (remoteHash.isEmpty()) {
                LogUtils.w(TAG, "Couldn't check for bloat list updates; using the cached list")
                return cached
            }
            val needsUpdate = remoteHash != settings.latestCommitHashFlow.first()
            LogUtils.i(TAG, "Bloat list needs update: $needsUpdate (commit ${remoteHash.take(7)})")
            if (!needsUpdate) return cached
        }

        val start = currentTimeMillis()
        val raw = fetchBloatList() ?: return cached.orEmpty()
        val parsed = runCatching { parseBloatList(raw) }
            .onFailure { LogUtils.e(TAG, "Downloaded bloat list is invalid", it) }
            .getOrNull() ?: return cached.orEmpty()

        runCatching { cache.write(raw.encodeToByteArray()) }
            .onFailure { LogUtils.e(TAG, "Failed to cache the bloat list", it) }
        if (remoteHash.isNotEmpty()) settings.setLatestCommitHash(remoteHash)
        LogUtils.i(TAG, "Updated bloat list in ${currentTimeMillis() - start}ms (${parsed.size} entries, commit ${remoteHash.take(7)})")
        return parsed
    }

    /** The cached list, or null if there is none or it can't be parsed. */
    private fun readCache(): Map<String, BloatData>? =
        runCatching { cache.read()?.decodeToString()?.let(::parseBloatList) }
            .onFailure { LogUtils.w(TAG, "Ignoring unreadable bloat list cache: $it") }
            .getOrNull()

    private suspend fun fetchLatestHash(): String =
        runCatching { parseLatestHash(client.get(settings.commitsUrlFlow.first()).bodyAsText()) }
            .onFailure { LogUtils.e(TAG, "Failed to check for bloat list updates", it) }
            .getOrDefault("")

    private suspend fun fetchBloatList(): String? =
        runCatching { client.get(settings.bloatListUrlFlow.first()).bodyAsText() }
            .onFailure { LogUtils.e(TAG, "Failed to download the bloat list", it) }
            .getOrNull()
}

/** Parses a raw bloat-list JSON string into a map of package name -> [BloatData]. */
private fun parseBloatList(json: String): Map<String, BloatData> =
    Json.parseToJsonElement(json).jsonObject
        .mapNotNull { (packageName, entry) -> (entry as? JsonObject)?.let { packageName to it.toBloatData() } }
        .toMap()

private fun JsonObject.toBloatData() = BloatData(
    description = this["description"]?.jsonPrimitive?.content,
    badgeInfo = this["removal"]?.jsonPrimitive?.content?.let { AppBadgeInfo.byNameIgnoreCaseOrNull(it) },
)

/**
 * The latest commit hash from a GitHub commits API response: a JSON array
 * whose first element has a "sha" string. Returns "" when the payload is
 * empty, malformed or has no usable sha, so callers treat it as "unknown"
 * instead of a bogus hash.
 */
private fun parseLatestHash(commits: String): String {
    if (commits.isBlank()) return ""
    return try {
        Json.parseToJsonElement(commits)
            .jsonArray
            .firstOrNull()
            ?.jsonObject
            ?.get("sha")
            ?.jsonPrimitive
            ?.takeIf { it.isString }
            ?.content
            ?.takeIf { it.isNotBlank() }
            .orEmpty()
    } catch (_: Exception) {
        ""
    }
}
