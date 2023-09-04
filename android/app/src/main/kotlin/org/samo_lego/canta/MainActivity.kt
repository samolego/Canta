package org.samo_lego.canta

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import android.os.Process
import android.util.Log
import android.widget.Toast
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.lsposed.hiddenapibypass.HiddenApiBypass
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
        return if (!Shizuku.pingBinder()) {
            false
        } else if (Shizuku.isPreV11()) {
            Toast.makeText(this, "Shizuku < 11 is not supported!", Toast.LENGTH_LONG).show()
            false
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        permissions.forEachIndexed { index, permission ->
            if (permission == ShizukuProvider.PERMISSION) {
                onRequestPermissionResult(requestCode, grantResults[index])
                println("Permission $permission granted: ${grantResults[index]}")
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

    private fun uninstallApp(packageName: String) {
        if (!checkShizukuPermission()) {
            // Shizuku is not available, handle accordingly
            return
        }

        val broadcastIntent = Intent("org.samo_lego.canta.UNINSTALL_RESULT_ACTION")
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            broadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val packageInstaller = getPackageInstaller()


        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_META_DATA
            )
        }

        val isSystem = (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        Log.d("Canta", "Uninstalling '$packageName' [system: $isSystem]")

        // 0x00000004 = PackageManager.DELETE_SYSTEM_APP
        // 0x00000002 = PackageManager.DELETE_ALL_USERS
        val flags = if (isSystem) 0x00000004 else 0x00000002

        try {
            HiddenApiBypass.invoke(
                PackageInstaller::class.java,
                packageInstaller,
                "uninstall",
                packageName,
                flags,
                intent.intentSender
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun getPackageInstaller(): PackageInstaller {
        val iPackageInstaller = ShizukuPackageInstallerUtils.getPrivilegedPackageInstaller()
        val root = Shizuku.getUid() == 0
        val userId = if (root) Process.myUserHandle().hashCode() else 0

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

        val packageInstaller = getPackageInstaller()
        packageInstaller.installExistingPackage(
            packageName,
            PackageManager.INSTALL_REASON_USER,
            null
        )
    }
}
