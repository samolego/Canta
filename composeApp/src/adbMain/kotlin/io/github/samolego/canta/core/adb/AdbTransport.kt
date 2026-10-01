package io.github.samolego.canta.core.adb

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Output of a device shell command. */
data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String = "",
)

/** A streamed shell command ([AdbTransport.shellLines]) exited unsuccessfully. */
class ShellCommandException(command: String, exitCode: Int, stderr: String) :
    Exception("'$command' exited with $exitCode: ${stderr.take(500)}")

/**
 * The per-platform part of talking to a device over ADB: desktop implements it
 * with dadb, web with ya-webadb. Everything built on top (package listing,
 * the on-device helper) is shared.
 */
interface AdbTransport {
    /** Runs [command] in the device shell. Throws when the connection fails. */
    suspend fun shell(command: String): ShellResult

    /**
     * Runs [command] and emits its stdout line by line as the device produces
     * it. The flow fails with [ShellCommandException] on a non-zero exit code.
     */
    fun shellLines(command: String): Flow<String>

    /** Writes [bytes] to [remotePath] on the device with the given unix [mode]. */
    suspend fun push(bytes: ByteArray, remotePath: String, mode: Int)
}

/**
 * Re-chunks text into lines (without their `\n` / `\r\n`). A final line
 * without a trailing newline is emitted when the upstream completes.
 */
fun Flow<String>.splitLines(): Flow<String> = flow {
    val pending = StringBuilder()
    collect { chunk ->
        pending.append(chunk)
        var newline = pending.indexOf('\n')
        while (newline >= 0) {
            emit(pending.substring(0, newline).removeSuffix("\r"))
            pending.deleteRange(0, newline + 1)
            newline = pending.indexOf('\n')
        }
    }
    if (pending.isNotEmpty()) emit(pending.toString())
}
