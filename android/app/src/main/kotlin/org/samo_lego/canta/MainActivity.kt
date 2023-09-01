package org.samo_lego.canta

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import android.os.Process
import android.permission.IPermissionManager
import android.util.Log
import android.widget.Toast
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.SystemServiceHelper

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

    private val permissionManager by lazy {
        // If HiddenApiBypass is not active, we get NoSuchMethodException later
        HiddenApiBypass.addHiddenApiExemptions(
            "Landroid/permission"
        )
        IPermissionManager.Stub.asInterface(
            ShizukuBinderWrapper(
                SystemServiceHelper.getSystemService(
                    "permissionmgr"
                )
            )
        )
    }

    private fun requestDeletePermissions() {
        /*val ipm = IPermissionManager.Stub.asInterface(ShizukuBinderWrapper(SystemServiceHelper.getSystemService("permissionmgr")))
        ipm.grantRuntimePermission(
            "org.samo_lego.canta",
            Manifest.permission.DELETE_PACKAGES,
            UserHandle.getUserHandleForUid(0)
        )*/

        permissionManager.grantRuntimePermission(
            "org.samo_lego.canta",
            Manifest.permission.DELETE_PACKAGES,
            0
        )
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

    @SuppressLint("MissingPermission")
    private fun uninstallApp(packageName: String) {
        if (!checkShizukuPermission()) {
            // Shizuku is not available, handle accordingly
            return
        }

        //requestDeletePermissions()

        val broadcastIntent = Intent("org.samo_lego.canta.ACTION_UNINSTALL")
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            broadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val packageInstaller = getPackageInstaller()
        Log.d("Canta", "Uninstalling '$packageName'")

        packageInstaller.uninstall(packageName, intent.intentSender)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            packageInstaller.uninstallExistingPackage(packageName, intent.intentSender)
        }
    }

    private fun getPackageInstaller(): PackageInstaller {
        val iPackageInstaller = ShizukuPackageInstallerUtils.getPrivilegedPackageInstaller()
        val root = Shizuku.getUid() == 0
        val userId = if (root) Process.myUserHandle().hashCode() else 0

        // The reason for use "com.android.shell" as installer package under adb is that
        // getMySessions will check installer package's owner
        val installerName = "com.android.shell"

        return ShizukuPackageInstallerUtils.createPackageInstaller(
            iPackageInstaller, installerName, userId
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
