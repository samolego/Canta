@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.samolego.canta.webadb

import kotlin.js.JsAny
import kotlin.js.Promise

/**
 * Interop layer for <a href="https://github.com/yume-chan/ya-webadb">ya-webadb</a> v2
 * (`@yume-chan/adb`, `@yume-chan/adb-daemon-webusb`, `@yume-chan/adb-credential-web`).
 *
 * Kotlin/Wasm does not support per-declaration imports, and `js()` may only be
 * used as the single expression of a package-level function. Accordingly, npm
 * modules are fetched with dynamic `import(...)` and their classes are created
 * inside `js()` snippets; the returned values are typed with the `external
 * interface`s below. These must stay interfaces: Kotlin/Wasm checks casts to
 * an `external class` with `instanceof <Name>` against a JS global, and none
 * of these types exist as globals (they are module-scoped or plain objects),
 * which fails with "ReferenceError: Adb is not defined".
 */

/** WebUSB root object (`navigator.usb`), or `null` if unsupported. */
internal fun navigatorUsb(): JsAny? = js("navigator.usb")

/**
 * Host OS identifier for tailoring USB diagnostics
 * (`navigator.userAgentData.platform`, falling back to `navigator.platform`),
 * or `null` when unavailable.
 */
internal fun navigatorPlatform(): String? =
    js("navigator.userAgentData?.platform ?? navigator.platform ?? null")


internal fun loadAdbModule(): Promise<JsAny> = js("import('@yume-chan/adb')")

internal fun loadWebUsbModule(): Promise<JsAny> = js("import('@yume-chan/adb-daemon-webusb')")

internal fun loadCredentialStoreModule(): Promise<JsAny> =
    js("import('@yume-chan/adb-credential-web')")


internal fun newWebUsbDeviceManager(
    module: JsAny,
    usb: JsAny,
): AdbDaemonWebUsbDeviceManager = js("new module.AdbDaemonWebUsbDeviceManager(usb)")

internal fun newCredentialStore(module: JsAny, appName: String): JsAny =
    js("new module.default(appName)")

internal fun newAdb(module: JsAny, transport: AdbDaemonTransport): Adb =
    js("new module.Adb(transport)")

internal fun authenticate(
    module: JsAny,
    options: JsAny,
): Promise<AdbDaemonTransport> = js("module.AdbDaemonTransport.authenticate(options)")

internal fun authenticateOptions(
    serial: String,
    connection: JsAny,
    credentialStore: JsAny,
): JsAny = js("({ serial: serial, connection: connection, credentialStore: credentialStore })")

/** `new Uint8Array(length)`, filled with [uint8ArraySet]. */
internal fun newUint8Array(length: Int): JsUint8Array = js("new Uint8Array(length)")

internal fun uint8ArraySet(array: JsUint8Array, index: Int, value: Byte) {
    js("array[index] = value")
}

/** A `ReadableStream` that yields [chunk] once, as `AdbSync.write` expects. */
internal fun singleChunkStream(chunk: JsUint8Array): JsAny =
    js("new ReadableStream({ start(controller) { controller.enqueue(chunk); controller.close(); } })")

/** `AdbSyncWriteOptions`; [mtimeSeconds] is a unix timestamp. */
internal fun syncWriteOptions(filename: String, file: JsAny, permission: Int, mtimeSeconds: Int): JsAny =
    js("({ filename: filename, file: file, permission: permission, mtime: mtimeSeconds })")

/**
 * An ADB device picked through the WebUSB chooser.
 */
external interface AdbDaemonWebUsbDevice : JsAny {
    val serial: String
    val name: String

    fun connect(): Promise<AdbDaemonWebUsbConnection>
}

/** The WebUSB device manager (created with the `navigator.usb` object). */
external interface AdbDaemonWebUsbDeviceManager : JsAny {
    fun requestDevice(): Promise<AdbDaemonWebUsbDevice?>
}

/** A connection to the device's ADB daemon over WebUSB. */
external interface AdbDaemonWebUsbConnection : JsAny

/** Direct ADB daemon transport; result of the RSA handshake. */
external interface AdbDaemonTransport : JsAny

/** Result of an awaited `shell,v2` command (`stdout`/`stderr` are decoded text). */
external interface WaitResult : JsAny {
    val stdout: String
    val stderr: String
    val exitCode: Int
}

/** The main ADB client. `subprocess.shellProtocol` runs device shell commands. */
external interface Adb : JsAny {
    val subprocess: AdbSubprocessService

    /** Opens the file sync service; [AdbSync.dispose] it when done. */
    fun sync(): Promise<AdbSync>

    fun close(): Promise<JsAny?>
}

/** File sync service (`sync:`), used to push files to the device. */
external interface AdbSync : JsAny {
    fun write(options: JsAny): Promise<JsAny?>
    fun dispose(): Promise<JsAny?>
}

/** A JS `Uint8Array`. */
external interface JsUint8Array : JsAny

/** Container for the shell protocol subprocess service. */
external interface AdbSubprocessService : JsAny {
    val shellProtocol: AdbShellProtocolSubprocessService?
}

/** Shell protocol layer (`shell,v2`). */
external interface AdbShellProtocolSubprocessService : JsAny {
    fun spawnWaitText(command: String): Promise<WaitResult>

    /** Starts [command] with streaming stdout/stderr ([AdbShellProtocolProcess]). */
    fun spawn(command: String): Promise<AdbShellProtocolProcess>
}

/** A running `shell,v2` process. */
external interface AdbShellProtocolProcess : JsAny {
    /** Resolves with the exit code once the process ends. */
    val exited: Promise<JsNumber>
}

/** Reads a byte stream as text chunks; see [stdoutTextReader]. */
external interface TextChunkReader : JsAny {
    /** The next decoded chunk, or null at the end of the stream. */
    fun read(): Promise<JsString?>
}

/**
 * Reads [process]'s stdout as UTF-8 text chunks. The decoder runs in
 * streaming mode, so a character split across chunks is decoded correctly.
 */
internal fun stdoutTextReader(process: AdbShellProtocolProcess): TextChunkReader = js("""({
    reader: process.stdout.getReader(),
    decoder: new TextDecoder(),
    read: async function () {
        const result = await this.reader.read();
        if (result.done) {
            const tail = this.decoder.decode();
            return tail.length > 0 ? tail : null;
        }
        return this.decoder.decode(result.value, { stream: true });
    }
})""")

/** Collects [process]'s whole stderr as text (also keeps the stream from backing up). */
internal fun stderrText(process: AdbShellProtocolProcess): Promise<JsString> =
    js("new Response(process.stderr).text()")
