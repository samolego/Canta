@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.samolego.canta.core

import io.github.samolego.canta.core.adb.AdbTransport
import io.github.samolego.canta.core.adb.ShellCommandException
import io.github.samolego.canta.core.adb.ShellResult
import io.github.samolego.canta.core.adb.splitLines
import io.github.samolego.canta.util.currentTimeMillis
import io.github.samolego.canta.webadb.Adb
import io.github.samolego.canta.webadb.await
import io.github.samolego.canta.webadb.newUint8Array
import io.github.samolego.canta.webadb.singleChunkStream
import io.github.samolego.canta.webadb.stderrText
import io.github.samolego.canta.webadb.stdoutTextReader
import io.github.samolego.canta.webadb.syncWriteOptions
import io.github.samolego.canta.webadb.uint8ArraySet
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** [AdbTransport] over the currently selected ya-webadb [connection] (WebUSB). */
class WebAdbTransport : AdbTransport {

    var connection: Adb? = null

    override suspend fun shell(command: String): ShellResult {
        val result = shellProtocol().spawnWaitText(command).await()
        return ShellResult(result.exitCode, result.stdout, result.stderr)
    }

    override fun shellLines(command: String): Flow<String> = flow {
        val process = shellProtocol().spawn(command).await()
        // Start draining stderr right away so it can't stall the process.
        val stderr = stderrText(process)
        val stdout = stdoutTextReader(process)
        while (true) {
            val chunk = stdout.read().await() ?: break
            emit(chunk.toString())
        }
        val exitCode = process.exited.await().toInt()
        if (exitCode != 0) throw ShellCommandException(command, exitCode, stderr.await().toString())
    }.splitLines()

    override suspend fun push(bytes: ByteArray, remotePath: String, mode: Int) {
        val chunk = newUint8Array(bytes.size)
        bytes.forEachIndexed { index, byte -> uint8ArraySet(chunk, index, byte) }
        val mtime = (currentTimeMillis() / 1000).toInt()

        val sync = connected().sync().await()
        try {
            sync.write(syncWriteOptions(remotePath, singleChunkStream(chunk), mode, mtime)).await()
        } finally {
            sync.dispose().await()
        }
    }

    private fun connected(): Adb = connection ?: error("No WebUSB device connected")

    private fun shellProtocol() =
        connected().subprocess.shellProtocol ?: error("Device does not support the shell,v2 protocol")
}
