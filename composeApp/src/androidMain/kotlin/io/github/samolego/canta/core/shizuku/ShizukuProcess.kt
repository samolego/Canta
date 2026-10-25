package io.github.samolego.canta.core.shizuku

import rikka.shizuku.Shizuku
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Spawns processes running with Shizuku's identity (shell or root).
 *
 * `Shizuku.newProcess` is a private, deprecated API — it is looked up
 * reflectively. Callers must only use this after
 * [ShizukuPermission.isCantaAuthorized] succeeds.
 */
object ShizukuProcess {

    private val newProcessMethod = runCatching {
        Shizuku::class.java
            .getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            )
            .apply { isAccessible = true }
    }.getOrNull()

    /** Runs [command] directly (argv; no shell interpretation). */
    fun exec(vararg command: String): Process {
        val method = newProcessMethod
            ?: throw UnsupportedOperationException("Shizuku.newProcess is unavailable")
        return method.invoke(null, command, null, null) as Process
    }

    /**
     * Runs [command], waiting up to [timeoutMs]. Returns the exit code and
     * captured stderr (truncated), or null on timeout — the process is then
     * forcibly destroyed.
     */
    fun execForResult(timeoutMs: Long, vararg command: String): Pair<Int, String>? {
        val process = exec(*command)
        // Drain stderr concurrently so a full pipe buffer can't block the child.
        val stderr = AtomicReference(ByteArray(0))
        val reader = thread(start = true, isDaemon = true) {
            stderr.set(runCatching { process.errorStream.readBytes() }.getOrDefault(ByteArray(0)))
        }
        if (!awaitExit(process, timeoutMs)) {
            runCatching { process.destroyForcibly() }
            return null
        }
        reader.join(5_000)
        return process.exitValue() to stderr.get().decodeToString().trim().take(500)
    }

    /**
     * ShizukuRemoteProcess exposes `waitForTimeout`, not an override of
     * `Process.waitFor(long, TimeUnit)` — prefer the native method so we
     * wait on the service-side exit event instead of busy-polling.
     */
    private fun awaitExit(process: Process, timeoutMs: Long): Boolean =
        try {
            val m = process.javaClass.getMethod("waitForTimeout", Long::class.java, TimeUnit::class.java)
            m.invoke(process, timeoutMs, TimeUnit.MILLISECONDS) as Boolean
        } catch (_: Exception) {
            process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        }
}
