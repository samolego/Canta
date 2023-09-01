package org.samo_lego.canta

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.IPackageInstaller
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import android.widget.Toast
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

class MainActivity : FlutterActivity() {
    private val CHANNEL = "org.samo_lego.canta/native"
    private val SHIZUKU_CODE = 0xCA07A


    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger, CHANNEL
        ).setMethodCallHandler { call, result ->
            when (call.method) {
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
                else -> result.notImplemented()
            }
        }
    }

    private fun checkShizukuPermission(): Boolean {
        println("Checking permission.")
        return if (!Shizuku.pingBinder()) {
            false
        } else if (Shizuku.isPreV11()) {
            Toast.makeText(this, "Shizuku < 11 is not supported!", Toast.LENGTH_LONG).show()
            false
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Using shizuku!", Toast.LENGTH_SHORT).show()
            true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(
                this,
                "You denied the permission for Shizuku. Please enable it in app.",
                Toast.LENGTH_LONG
            ).show()
            false
        } else {
            Shizuku.requestPermission(SHIZUKU_CODE)

            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestDeletePermissions() {
        val canDelete =
            Shizuku.checkRemotePermission(Manifest.permission.DELETE_PACKAGES) == PackageManager.PERMISSION_GRANTED

        /*val iPmClass = Class.forName("android.content.pm.IPackageManager")
        val iPmStub = Class.forName("android.content.pm.IPackageManager\$Stub")
        val asInterfaceMethod = iPmStub.getMethod("asInterface", IBinder::class.java)

        val grantRuntimePermissionMethod = iPmClass.getMethod(
            "grantRuntimePermission",
            String::class.java, /* package name */
            String::class.java, /* permission name */
            Int::class.java, /* user id */
        )

        val iPmInstance = asInterfaceMethod.invoke(
            null, ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )
        grantRuntimePermissionMethod.invoke(
            iPmInstance, "org.samo_lego.canta", Manifest.permission.DELETE_PACKAGES, 0
        )
        val manager = IPackageManager.Stub.asInterface(
            ShizukuBinderWrapper(
                SystemServiceHelper.getSystemService("package")
            )
        )*/
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
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
        val unp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()))
                .map { app -> app.packageName }
        } else {
            pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
                .map { app -> app.packageName }
        }.toSet()
        val inp = getInstalledPackages().toSet()

        return (unp - inp).toList().sorted()
    }

    private fun getInstalledPackages(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(
                PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        }.map { app -> app.packageName }
    }

    @SuppressLint("MissingPermission")
    private fun uninstallApp(packageName: String) {
        if (!checkShizukuPermission()) {
            // Shizuku is not available, handle accordingly
            return
        }

        requestDeletePermissions()

        val installer = ShizukuPackageInstallerUtils.getPrivilegedPackageInstaller()
        val broadcastIntent = Intent("org.samo_lego.canta.ACTION_UNINSTALL")
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            broadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val packageInstaller = getPackageInstaller(installer)
        packageInstaller.uninstall(packageName, intent.intentSender)
        packageInstaller.installExistingPackage(
            packageName,
            PackageManager.INSTALL_REASON_USER,
            null
        )
    }

    private fun getPackageInstaller(iPackageInstaller: IPackageInstaller): PackageInstaller {
        val userId = 0

        // The reason for use "com.android.shell" as installer package under adb is that
        // getMySessions will check installer package's owner
        return ShizukuPackageInstallerUtils.createPackageInstaller(
            iPackageInstaller, "com.android.shell", userId
        )
    }

    @SuppressLint("MissingPermission")
    private fun reinstallApp(packageName: String) {
        if (!checkShizukuPermission()) {
            // Shizuku is not available, handle accordingly
            return
        }

        val installer = ShizukuPackageInstallerUtils.getPrivilegedPackageInstaller()

        val packageInstaller = getPackageInstaller(installer)
        packageInstaller.installExistingPackage(
            packageName,
            PackageManager.INSTALL_REASON_USER,
            null
        )
    }
}
