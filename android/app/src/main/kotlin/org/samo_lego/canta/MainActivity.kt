package org.samo_lego.canta

import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "org.samo_lego.canta/native"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "uninstallApp" -> {
                    val packageName = call.argument<String>("packageName")
                    if (packageName != null) {
                        uninstallApp(packageName)
                    }
                    result.success(true)
                }

                "getUninstalledApps" -> result.success(getUninstalledPackages())
                "getInstalledApps" -> result.success(getInstalledPackages())
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun getUninstalledPackages(): List<String> {
        val pm = packageManager
        val unp =
            pm.getInstalledPackages(PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()))
                .map { app -> app.packageName }
                .toSet()
        val inp = getInstalledPackages().toSet()

        // Loop through unp, then put all the packages that are not in inp into a list
        return (unp - inp).toList().sorted()
    }

    private fun getInstalledPackages(): List<String> {
        return packageManager.getInstalledPackages(
            PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        )
            .map { app -> app.packageName }
    }

    private fun uninstallApp(packageName: String) {
        println("Hello from Kotlin!. Uninstalling $packageName")
    }
}
