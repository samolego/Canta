package io.github.samolego.canta.core

import io.github.samolego.canta.util.LogUtils
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

/**
 * Web (WASM) [CantaPlatform], using browser APIs directly.
 */
class WebCantaPlatform : AdbCantaPlatform(TAG) {

    private companion object {
        const val TAG = "WebCantaPlatform"
    }

    override fun openUrl(url: String) {
        try {
            jsWindowOpen(url)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to open URL '$url'", e)
        }
    }

    override fun copyToClipboard(text: String) {
        try {
            jsCopyText(text)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to copy to clipboard", e)
        }
    }

    override fun readClipboard(): String? = null
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsWindowOpen(url: String) {
    js("window.open(url, '_blank', 'noopener')")
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsCopyText(text: String) {
    // writeText returns a Promise; report rejections instead of dropping them.
    js("navigator.clipboard.writeText(text).catch(e => console.error('Clipboard write failed', e))")
}