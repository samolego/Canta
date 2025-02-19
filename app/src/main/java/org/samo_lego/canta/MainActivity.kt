package org.samo_lego.canta

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageInstaller
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.lsposed.hiddenapibypass.HiddenApiBypass
import org.samo_lego.canta.extension.getInfoForPackage
import org.samo_lego.canta.ui.CantaApp
import org.samo_lego.canta.ui.theme.CantaTheme
import org.samo_lego.canta.util.LogUtils
import org.samo_lego.canta.util.ShizukuPackageInstallerUtils
import rikka.shizuku.Shizuku

const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
const val APP_NAME = "Canta"
const val packageName = "org.samo_lego.canta"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)

        setContent {
            CantaTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    CantaApp(
                            launchShizuku = {
                                // Open shizuku app
                                val launchIntent =
                                        packageManager.getLaunchIntentForPackage(
                                                SHIZUKU_PACKAGE_NAME
                                        )
                                startActivity(launchIntent)
                            },
                            uninstallApp = { uninstallApp(it) },
                            reinstallApp = { reinstallApp(it) },
                    )
                }
            }
        }
    }

    /**
     * Uninstalls app using Shizuku. See <a
     * href="https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/PackageManagerShellCommand.java;drc=bcb2b436bde55ee40050400783a9c083e77ce2fe;l=2144">PackageManagerShellCommand.java</a>
     * @param packageName package name of the app to uninstall
     */
    private fun uninstallApp(packageName: String): Boolean {
        val broadcastIntent = Intent("org.samo_lego.canta.UNINSTALL_RESULT_ACTION")
        val intent =
                PendingIntent.getBroadcast(
                        applicationContext,
                        0,
                        broadcastIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val packageInstaller = getPackageInstaller()
        val packageInfo = packageManager.getInfoForPackage(packageName)

        val isSystem = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        LogUtils.i(APP_NAME, "Uninstalling '$packageName' [system: $isSystem]")

        // 0x00000004 = PackageManager.DELETE_SYSTEM_APP
        // 0x00000002 = PackageManager.DELETE_ALL_USERS
        val flags = if (isSystem) 0x00000004 else 0x00000002

        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                PackageInstaller::class
                        .java
                        .getDeclaredMethod(
                                "uninstall",
                                String::class.java,
                                Int::class.javaPrimitiveType,
                                PendingIntent::class.java
                        )
                        .invoke(packageInstaller, packageName, flags, intent)
                // packageInstaller.uninstall(packageName, flags, intent.intentSender)
            } else {
                HiddenApiBypass.invoke(
                        PackageInstaller::class.java,
                        packageInstaller,
                        "uninstall",
                        packageName,
                        flags,
                        intent.intentSender
                )
            }
            true
        } catch (e: Exception) {
            LogUtils.e(APP_NAME, "Failed to uninstall '$packageName'")
            LogUtils.e(APP_NAME, "Error: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Reinstalls app using Shizuku. See <a
     * href="https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/PackageManagerShellCommand.java;drc=bcb2b436bde55ee40050400783a9c083e77ce2fe;l=1408>PackageManagerShellCommand.java</a>
     * @param packageName package name of the app to reinstall (must preinstalled on the phone)
     */
    private fun reinstallApp(packageName: String): Boolean {
        val installReason = PackageManager.INSTALL_REASON_UNKNOWN
        val broadcastIntent = Intent("org.samo_lego.canta.INSTALL_RESULT_ACTION")
        val intent =
                PendingIntent.getBroadcast(
                        applicationContext,
                        0,
                        broadcastIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        LogUtils.i(APP_NAME, "Reinstalling '$packageName'")

        // PackageManager.INSTALL_ALL_WHITELIST_RESTRICTED_PERMISSIONS
        val installFlags = 0x00400000

        return try {
            HiddenApiBypass.invoke(
                    IPackageInstaller::class.java,
                    ShizukuPackageInstallerUtils.getPrivilegedPackageInstaller(),
                    "installExistingPackage",
                    packageName,
                    installFlags,
                    installReason,
                    intent.intentSender,
                    0,
                    null
            )
            true
        } catch (e: Exception) {
            LogUtils.e(APP_NAME, "Failed to reinstall '$packageName'")
            LogUtils.e(APP_NAME, "Error: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun getPackageInstaller(): PackageInstaller {
        val iPackageInstaller = ShizukuPackageInstallerUtils.getPrivilegedPackageInstaller()
        val root = Shizuku.getUid() == 0
        val userId = if (root) android.os.Process.myUserHandle().hashCode() else 0

        // The reason for use "com.android.shell" as installer package under adb is that
        // getMySessions will check installer package's owner
        return ShizukuPackageInstallerUtils.createPackageInstaller(
                iPackageInstaller,
                "com.android.shell",
                userId,
                this
        )
    }
}
