package org.samo_lego.canta

import android.content.pm.IPackageDeleteObserver
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.IBinder
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.SystemServiceHelper

class MainActivity : FlutterActivity() {
    private val CHANNEL = "org.samo_lego.canta/native"
    private val SHIZUKU_CODE = 0

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "checkShizukuRunning" -> result.success(Shizuku.pingBinder())
                "checkPermissions" -> {
                    val isGranted = if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                        checkSelfPermission(ShizukuProvider.PERMISSION) == PackageManager.PERMISSION_GRANTED
                    } else {
                        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                    }

                    if (!isGranted) {
                        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                            requestPermissions(arrayOf(ShizukuProvider.PERMISSION), SHIZUKU_CODE)
                        } else {
                            Shizuku.requestPermission(SHIZUKU_CODE)
                        }
                    }
                }

                "uninstallApp" -> {
                    val packageName = call.argument<String>("packageName")
                    if (packageName != null) {
                        uninstallApp(packageName)
                    }
                    result.success(true)
                }

                "reinstallApp" -> {
                    val packageName = call.argument<String>("packageName")
                    if (packageName != null) {
                        reinstallApp(packageName)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissions.forEachIndexed { index, permission ->
            if (permission == ShizukuProvider.PERMISSION) {
                onRequestPermissionResult(requestCode, grantResults[index])
            }
        }
    }

    private fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        val isGranted = grantResult == PackageManager.PERMISSION_GRANTED
        //Do stuff based on the result.

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
        if (!Shizuku.pingBinder()) {
            // Shizuku is not available, handle accordingly
            println("Shizuku is not available")
            return
        }
        // Call shizuku api
        val iPmClass = Class.forName("android.content.pm.IPackageManager")
        val iPmStub = Class.forName("android.content.pm.IPackageManager\$Stub")
        val asInterfaceMethod = iPmStub.getMethod("asInterface", IBinder::class.java)
        val grantRuntimePermissionMethod = iPmClass.getMethod(
            "deletePackageAsUser",
            String::class.java, /* package name */
            IPackageDeleteObserver::class.java, /* observer */
            Int::class.java, /* flags */
            Int::class.java, /* user id */
        )

        val iPmInstance = asInterfaceMethod.invoke(
            null,
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )

        grantRuntimePermissionMethod.invoke(
            iPmInstance,
            packageName, /* package name */
            null, /* observer */
            0x00000002, /* flags */
            0, /* user id */
        )
    }

    private fun reinstallApp(packageName: String) {
        TODO("Not yet implemented")
    }
}
