package io.github.samolego.canta.data.proto

import io.github.samolego.canta.data.AppBlobStorage
import io.github.samolego.canta.util.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * A single protobuf-encoded value of type [T] persisted in [storage], exposed
 * as [state]. Shared by the settings and preset stores.
 *
 * Updates are serialized, and [state] only changes once the write succeeded,
 * so memory and storage never disagree. A value that can't be read falls
 * back to [default] without overwriting what is stored.
 *
 * @param migrate applied to the loaded value (in memory; persisted with the
 *   next update), e.g. to fill in fields older versions didn't write.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class ProtoBlobStore<T>(
    private val storage: AppBlobStorage,
    private val serializer: KSerializer<T>,
    default: T,
    private val tag: String,
    migrate: (T) -> T = { it },
) {
    private val mutex = Mutex()

    private val _state = MutableStateFlow(migrate(load(default)))
    val state: StateFlow<T> = _state.asStateFlow()

    private fun load(default: T): T {
        val bytes = try {
            storage.read()
        } catch (e: Exception) {
            LogUtils.e(tag, "Failed to read stored data; using defaults", e)
            null
        } ?: return default
        return try {
            ProtoBuf.decodeFromByteArray(serializer, bytes)
        } catch (e: Exception) {
            LogUtils.e(tag, "Failed to parse stored data; using defaults", e)
            default
        }
    }

    /**
     * Applies [transform] and persists the result. Returns false, leaving
     * [state] unchanged, if it couldn't be written.
     */
    suspend fun update(transform: (T) -> T): Boolean = mutex.withLock {
        val next = transform(_state.value)
        try {
            val bytes = ProtoBuf.encodeToByteArray(serializer, next)
            storage.write(bytes)
            _state.value = next
            true
        } catch (e: Exception) {
            LogUtils.e(tag, "Failed to persist data", e)
            false
        }
    }
}
