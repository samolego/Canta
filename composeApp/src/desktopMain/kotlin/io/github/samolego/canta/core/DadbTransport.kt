package io.github.samolego.canta.core

import dadb.AdbShellPacket
import dadb.Dadb
import io.github.samolego.canta.core.adb.AdbTransport
import io.github.samolego.canta.core.adb.ShellCommandException
import io.github.samolego.canta.core.adb.ShellResult
import io.github.samolego.canta.core.adb.splitLines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okio.Buffer

/**
 * [AdbTransport] over the currently selected dadb [connection]. dadb calls
 * block, so they run on [Dispatchers.IO].
 */
class DadbTransport : AdbTransport {

    @Volatile
    var connection: Dadb? = null

    override suspend fun shell(command: String): ShellResult = withContext(Dispatchers.IO) {
        val response = connected().shell(command)
        ShellResult(response.exitCode, response.output, response.errorOutput)
    }

    override fun shellLines(command: String): Flow<String> = flow {
        connected().openShell(command).use { stream ->
            // Only decode up to the last newline: a newline byte never occurs
            // inside a multi-byte UTF-8 character, so no character is split.
            var pending = ByteArray(0)
            val stderr = StringBuilder()
            while (true) {
                when (val packet = stream.read()) {
                    is AdbShellPacket.StdOut -> {
                        val bytes = pending + packet.payload
                        val end = bytes.lastIndexOf('\n'.code.toByte()) + 1
                        if (end > 0) emit(bytes.decodeToString(0, end))
                        pending = bytes.copyOfRange(end, bytes.size)
                    }
                    is AdbShellPacket.StdError -> stderr.append(packet.payload.decodeToString())
                    is AdbShellPacket.Exit -> {
                        if (pending.isNotEmpty()) emit(pending.decodeToString())
                        val exitCode = packet.payload[0].toInt()
                        if (exitCode != 0) throw ShellCommandException(command, exitCode, stderr.toString())
                        return@use
                    }
                }
            }
        }
    }.splitLines().flowOn(Dispatchers.IO)

    override suspend fun push(bytes: ByteArray, remotePath: String, mode: Int) {
        withContext(Dispatchers.IO) {
            connected().push(Buffer().write(bytes), remotePath, mode, System.currentTimeMillis())
        }
    }

    private fun connected(): Dadb = connection ?: error("No ADB device connected")
}
