package io.github.samolego.canta.helper

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.IBinder
import android.os.Looper
import android.os.UserHandle
import io.github.samolego.canta.packages.AppIcon
import io.github.samolego.canta.packages.HelperFraming
import io.github.samolego.canta.packages.PROTOCOL_VERSION
import io.github.samolego.canta.packages.PackageDetails
import io.github.samolego.canta.packages.OperationResult
import io.github.samolego.canta.packages.PackageDetailsList
import io.github.samolego.canta.packages.PackageOperations
import io.github.samolego.canta.packages.SystemServices
import io.github.samolego.canta.packages.getAllPackages
import io.github.samolego.canta.packages.inDisplayOrder
import io.github.samolego.canta.packages.loadIconBitmap
import io.github.samolego.canta.packages.readSafely
import io.github.samolego.canta.packages.toPackageDetails
import java.io.ByteArrayOutputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.system.exitProcess

/**
 * Entry point of the on-device ADB helper, started by desktop/web Canta:
 *
 *     CLASSPATH=<apk> app_process /system/bin io.github.samolego.canta.helper.Main [--user <id>] <command>
 *
 * It runs as the shell user with the real Android framework, so labels and
 * icons come from [PackageManager] exactly as on the Android build. Results
 * go to stdout as one [HelperFraming] line per message; diagnostics go to
 * stderr, so stdout only ever carries protocol data.
 *
 * `--user <id>` picks the Android user or profile to act on (default 0): its
 * packages are listed and its copies uninstalled or reinstalled.
 *
 * Commands:
 *  - `info`: a [PackageDetailsList] with every installed and uninstalled package
 *  - `icons <sizePx>`: every package's PNG icon as its own [AppIcon] line, in
 *    display order (see [inDisplayOrder]), streamed as each one is ready
 *  - `uninstall [--reset] <package>...` / `reinstall <package>...`: one
 *    [OperationResult] line per package, as each operation completes
 *  - `version`: the [PROTOCOL_VERSION]
 */
object Main {

    private const val DEFAULT_USER_ID = 0

    @JvmStatic
    fun main(args: Array<String>) {
        val exitCode = try {
            run(args)
        } catch (e: Throwable) {
            System.err.println("canta-helper: ${e.stackTraceToString()}")
            1
        }
        // The framework leaves non-daemon threads behind; exit explicitly.
        exitProcess(exitCode)
    }

    private fun run(allArgs: Array<String>): Int {
        var args = allArgs.toList()
        var userId = DEFAULT_USER_ID
        if (args.firstOrNull() == "--user") {
            userId = args.getOrNull(1)?.toIntOrNull() ?: return usage()
            args = args.drop(2)
        }
        when (args.firstOrNull()) {
            "version" -> println(PROTOCOL_VERSION)
            "info" -> {
                val packages = userContext(userId).packageManager.readPackages().map { it.second }
                writeLine(
                    HelperFraming.encodeLine(
                        PackageDetailsList.serializer(),
                        PackageDetailsList(PROTOCOL_VERSION, packages)
                    )
                )
            }

            "icons" -> {
                val sizePx = args.getOrNull(1)?.toIntOrNull() ?: return usage()
                userContext(userId).packageManager.streamIcons(sizePx)
            }
            "uninstall" -> {
                val reset = args.getOrNull(1) == "--reset"
                val packageNames = args.drop(if (reset) 2 else 1)
                val operations = PackageOperations(DirectServices(userId), userContext(userId))
                packageNames.forEach { writeResult(operations.uninstall(it, reset)) }
            }
            "reinstall" -> {
                val operations = PackageOperations(DirectServices(userId), userContext(userId))
                args.drop(1).forEach { writeResult(operations.reinstall(it)) }
            }

            else -> return usage()
        }
        return 0
    }

    private fun usage(): Int {
        System.err.println(
            "usage: canta-helper [--user <id>] (info | icons <sizePx> | uninstall [--reset] <package>... | reinstall <package>... | version)"
        )
        return 2
    }

    /** Rendering icons and loading labels is per-package work; spread it over the cores. */
    private val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors().coerceAtLeast(2))

    /** Every package with its [PackageDetails], read in parallel; unreadable ones are skipped. */
    private fun PackageManager.readPackages(): List<Pair<PackageInfo, PackageDetails>> =
        getAllPackages()
            .map { info -> pool.submit(Callable { readSafely(info) { info to info.toPackageDetails(this) } }) }
            .mapNotNull { it.get() }

    /**
     * Writes every icon in display order, each as soon as it (and those before
     * it) are rendered, so the client can show the top of its list first.
     */
    private fun PackageManager.streamIcons(sizePx: Int) {
        val packages = readPackages()
        val appInfos = packages.associate { (info, details) -> details.packageName to info.applicationInfo }
        val pending = packages.map { it.second }.inDisplayOrder().map { details ->
            val appInfo = appInfos[details.packageName]
            details.packageName to pool.submit(Callable { appInfo?.let { loadIconBitmap(it, sizePx)?.toPng() } })
        }
        for ((packageName, png) in pending) {
            val bytes = png.get() ?: continue
            writeLine(HelperFraming.encodeLine(AppIcon.serializer(), AppIcon(packageName, bytes)))
        }
    }

    private fun writeResult(result: OperationResult) =
        writeLine(HelperFraming.encodeLine(OperationResult.serializer(), result))

    /** Writes one protocol line and flushes it, so streamed results arrive immediately. */
    private fun writeLine(line: String) {
        println(line)
        System.out.flush()
    }

    private fun Bitmap.toPng(): ByteArray =
        ByteArrayOutputStream().also { compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

    /**
     * Obtains the system [Context] the same way `app_process` tools like
     * scrcpy do. `ActivityThread` is hidden API, but hidden API restrictions
     * are not enforced for `app_process` programs.
     */
    @SuppressLint("PrivateApi")
    private fun systemContext(): Context {
        if (Looper.getMainLooper() == null) {
            @Suppress("DEPRECATION")
            Looper.prepareMainLooper()
        }
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val activityThread = activityThreadClass.getMethod("systemMain").invoke(null)
        return activityThreadClass.getMethod("getSystemContext").invoke(activityThread) as Context
    }

    /**
     * A context whose [PackageManager] answers for user [userId]: the
     * `android` package's context, created as that user. `createPackageContextAsUser`
     * is hidden API, which is not restricted for `app_process` programs.
     */
    @SuppressLint("PrivateApi")
    private fun userContext(userId: Int): Context {
        val system = systemContext()
        if (userId == DEFAULT_USER_ID) return system
        return Context::class.java
            .getMethod("createPackageContextAsUser", String::class.java, Int::class.javaPrimitiveType, UserHandle::class.java)
            .invoke(system, "android", 0, UserHandle::class.java.getMethod("of", Int::class.javaPrimitiveType).invoke(null, userId)) as Context
    }

    /**
     * The helper already runs as the shell user: system services are used
     * directly, acting on user [userId].
     */
    private class DirectServices(override val userId: Int) : SystemServices {
        @SuppressLint("PrivateApi")
        override fun service(name: String): IBinder =
            Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java).invoke(null, name) as IBinder

        override fun wrap(binder: IBinder): IBinder = binder
    }
}
