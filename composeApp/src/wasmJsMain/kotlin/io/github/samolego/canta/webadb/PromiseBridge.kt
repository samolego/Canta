@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.samolego.canta.webadb

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

/**
 * Bridges a JS [Promise] to a suspending function.
 */
internal suspend fun <T : JsAny?> Promise<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        then(
            onFulfilled = { value ->
                continuation.resume(value)
                null
            },
            onRejected = { error ->
                continuation.resumeWithException(Exception(jsErrorString(error)))
                null
            }
        )
    }

/**
 * Best-effort human readable description of a JS rejection value.
 */
private fun jsErrorString(error: JsAny): String =
    js("error && error.message ? String(error.message) : String(error)")
