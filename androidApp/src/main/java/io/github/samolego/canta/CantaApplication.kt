package io.github.samolego.canta

import android.app.Application
import io.github.samolego.canta.core.AndroidCantaPlatform
import android.os.Build
import io.github.samolego.canta.core.CantaAppDependencies
import io.github.samolego.canta.util.FileCrashReportStore
import java.io.File

class CantaApplication : Application() {

    val platform: AndroidCantaPlatform by lazy { AndroidCantaPlatform(this) }

    val dependencies: CantaAppDependencies by lazy { provideAndroidDependencies(this, platform) }

    /** Crash report of the previous run, kept in private storage; never leaves the device by itself. */
    val crashReports: FileCrashReportStore by lazy { FileCrashReportStore(File(filesDir, "crash_report.txt")) }

    override fun onCreate() {
        super.onCreate()
        crashReports.installHandler(
            appVersion = BuildInfo.VERSION_NAME,
            platform = "android",
            osVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
        )
        // Force-initialize dependencies (settings/presets load) eagerly.
        dependencies.handler
    }
}
