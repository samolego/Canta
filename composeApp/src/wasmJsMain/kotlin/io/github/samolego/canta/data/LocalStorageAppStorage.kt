@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.samolego.canta.data

import kotlin.io.encoding.Base64
import kotlin.js.ExperimentalWasmJsInterop

/**
 * [AppBlobStorage] backed by `localStorage`. `localStorage` only holds
 * strings, so the bytes are stored base64-encoded.
 */
class LocalStorageAppStorage(
    private val key: String,
) : AppBlobStorage {

    override fun read(): ByteArray? = localStorageGet(key)?.let { Base64.decode(it) }

    override suspend fun write(content: ByteArray) {
        localStorageSet(key, Base64.encode(content))
    }
}

private fun localStorageGet(key: String): String? = js("localStorage.getItem(key)")

private fun localStorageSet(key: String, value: String) {
    js("localStorage.setItem(key, value)")
}
