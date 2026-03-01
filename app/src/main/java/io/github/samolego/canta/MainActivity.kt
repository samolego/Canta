package io.github.samolego.canta

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageInstaller
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import io.github.samolego.canta.extension.getInfoForPackage
import io.github.samolego.canta.ui.CantaApp
import io.github.samolego.canta.ui.theme.CantaTheme
import io.github.samolego.canta.util.LogUtils
import io.github.samolego.canta.util.shizuku.ShizukuPackageInstallerUtils
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku

const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
const val APP_NAME = "Canta"
const val packageName = "io.github.samolego.canta"

class MainActivity : FragmentActivity() {

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
                        uninstallApp = { packageName, resetToFactory ->
                            uninstallApp(packageName, resetToFactory)
                        },
                        canResetAppToFactory = { packageName ->
                            checkIfCanResetToFactory(packageName)
                        },
                        reinstallApp = { reinstallApp(it) },
                        closeApp = { finishAndRemoveTask() },
                    )
                }
            }
        }
    }

    private fun checkIfCanResetToFactory(packageName: String): Boolean {
        val appInfo = packageManager.getInfoForPackage(packageName)?.applicationInfo ?: return false
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val hasUpdates = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        return isSystem && hasUpdates
    }

    private fun uninstallApp(packageName: String, resetToFactory: Boolean = false): Boolean {
        val packageInfo = packageManager.getInfoForPackage(packageName) ?: return false
        val isSystem = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val hasUpdates = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

        LogUtils.i(APP_NAME, "Uninstalling '$packageName' [system: $isSystem, hasUpdates: $hasUpdates, resetFirst: $resetToFactory]")
        
        val broadcastIntent = Intent("io.github.samolego.canta.UNINSTALL_RESULT_ACTION")
        val intent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            broadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val packageInstaller = getPackageInstaller()

        // First, if resetToFactory is true and app has updates, uninstall just the updates
        if (resetToFactory && isSystem && hasUpdates) {
            try {
                LogUtils.i(APP_NAME, "Removing updates for system app '$packageName'")
                
                // Use DELETE_ALL_USERS only to remove updates but keep system app
                HiddenApiBypass.invoke(
                    PackageInstaller::class.java,
                    packageInstaller,
                    "uninstall",
                    packageName,
                    0x00000002, // DELETE_ALL_USERS only
                    intent.intentSender
                )
                
                LogUtils.i(APP_NAME, "Successfully removed updates for '$packageName'")
                
                // Give it a moment to process
                Thread.sleep(500)
                
            } catch (e: Exception) {
                LogUtils.e(APP_NAME, "Failed to remove updates: ${e.message}")
            }
        }

        // Now uninstall the app completely
        // For system apps, we need DELETE_SYSTEM_APP flag
        val uninstallFlags = if (isSystem) {
            0x00000004 // DELETE_SYSTEM_APP
        } else {
            0x00000002 // DELETE_ALL_USERS for user apps
        }

        return try {
            HiddenApiBypass.invoke(
                PackageInstaller::class.java,
                packageInstaller,
                "uninstall",
                packageName,
                uninstallFlags,
                intent.intentSender
            )
            LogUtils.i(APP_NAME, "Successfully uninstalled '$packageName'")
            true
        } catch (e: Exception) {
            LogUtils.e(APP_NAME, "Failed to uninstall '$packageName': ${e.message}")
            false
        }
    }

    private fun reinstallApp(packageName: String): Boolean {
        val installReason = PackageManager.INSTALL_REASON_UNKNOWN
        val broadcastIntent = Intent("io.github.samolego.canta.INSTALL_RESULT_ACTION")
        val intent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            broadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        LogUtils.i(APP_NAME, "Reinstalling '$packageName'")
        
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
            LogUtils.i(APP_NAME, "Successfully reinstalled '$packageName'")
            true
        } catch (e: Exception) {
            LogUtils.e(APP_NAME, "Failed to reinstall '$packageName': ${e.message}")
            false
        }
    }

    private fun getPackageInstaller(): PackageInstaller {
        val iPackageInstaller = ShizukuPackageInstallerUtils.getPrivilegedPackageInstaller()
        val root = Shizuku.getUid() == 0
        val userId = if (root) android.os.Process.myUserHandle().hashCode() else 0

        return ShizukuPackageInstallerUtils.createPackageInstaller(
            iPackageInstaller,
            "com.android.shell",
            userId,
            this
        )
    }
}
