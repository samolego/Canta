package io.github.samolego.canta.data.bloat

import io.github.samolego.canta.data.CantaSettings
import io.github.samolego.canta.testing.MemoryStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BloatRepositoryTest {

    private val listJson = """{"com.a": {"description": "A", "removal": "Recommended"}}"""
    private val cachedJson = """{"com.cached": {"description": "C", "removal": "Expert"}}"""

    /** Serves the commits API and the list; records requested URLs. */
    private class Server(var sha: String? = "new", var listOk: Boolean = true, val list: String) {
        val requests = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            val url = request.url.toString()
            requests += url
            when {
                "api.github.com" in url && sha != null -> respond("""[{"sha": "$sha"}]""")
                "raw.githubusercontent.com" in url && listOk -> respond(list)
                else -> respondError(HttpStatusCode.ServiceUnavailable)
            }
        })
        val listDownloads get() = requests.count { "raw.githubusercontent.com" in it }
    }

    private suspend fun settings(latestHash: String = "", autoUpdate: Boolean = true) =
        CantaSettings(MemoryStorage()).apply {
            setLatestCommitHash(latestHash)
            setAutoUpdateBloatList(autoUpdate)
        }

    @Test
    fun downloadsAndCachesWhenNothingIsCached() = runTest {
        val server = Server(list = listJson)
        val cache = MemoryStorage()
        val settings = settings()

        val bloat = BloatRepository(server.client, cache, settings).load()

        assertEquals(setOf("com.a"), bloat.keys)
        assertEquals(listJson, cache.content?.decodeToString())
        assertEquals("new", settings.latestCommitHashFlow.first())
    }

    @Test
    fun usesCacheWhenUpToDate() = runTest {
        val server = Server(sha = "same", list = listJson)
        val bloat = BloatRepository(server.client, MemoryStorage(cachedJson.encodeToByteArray()), settings("same")).load()

        assertEquals(setOf("com.cached"), bloat.keys)
        assertEquals(0, server.listDownloads)
    }

    @Test
    fun keepsCacheWhenDownloadFails() = runTest {
        val server = Server(sha = "newer", listOk = false, list = listJson)
        val cache = MemoryStorage(cachedJson.encodeToByteArray())

        val bloat = BloatRepository(server.client, cache, settings("old")).load()

        assertEquals(setOf("com.cached"), bloat.keys)
        assertEquals(cachedJson, cache.content?.decodeToString())
    }

    @Test
    fun replacesCorruptCache() = runTest {
        val server = Server(sha = "same", list = listJson)
        val cache = MemoryStorage("{not json".encodeToByteArray())

        val bloat = BloatRepository(server.client, cache, settings("same")).load()

        assertEquals(setOf("com.a"), bloat.keys)
        assertEquals(listJson, cache.content?.decodeToString())
    }

    @Test
    fun noNetworkWhenAutoUpdateIsOffAndCacheExists() = runTest {
        val server = Server(list = listJson)
        BloatRepository(server.client, MemoryStorage(cachedJson.encodeToByteArray()), settings(autoUpdate = false)).load()
        assertTrue(server.requests.isEmpty())
    }
}
