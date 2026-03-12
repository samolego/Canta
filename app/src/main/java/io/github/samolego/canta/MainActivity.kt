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
import androidx.lifecycle.lifecycleScope
import io.github.samolego.canta.extension.getInfoForPackage
import io.github.samolego.canta.ui.CantaApp
import io.github.samolego.canta.ui.theme.CantaTheme
import io.github.samolego.canta.util.LogUtils
import io.github.samolego.canta.util.shizuku.ShizukuPackageInstallerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                            // Launching coroutine here so the UI can await the result properly
                            lifecycleScope.launch {
                                uninstallApp(packageName, resetToFactory)
                            }
                            true // Return true to satisfy the initial lambda
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

    /**
     * Checks if an app can be reset to factory version.
     * @param packageName package name of the app to check
     * @return true if the app is a system app with updates
     */
    private fun checkIfCanResetToFactory(packageName: String): Boolean {
        val appInfo = packageManager.getInfoForPackage(packageName)?.applicationInfo ?: return false
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val hasUpdates = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        return isSystem && hasUpdates
    }

    /**
     * Uninstalls app using Shizuku.
     * @param packageName package name of the app to uninstall
     * @param resetToFactory whether to reset system app to factory version before uninstall
     * @return true if the operation was successfully initiated
     */
    private suspend fun uninstallApp(packageName: String, resetToFactory: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val packageInfo = packageManager.getInfoForPackage(packageName) ?: return@withContext false
        val isSystem = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val hasUpdates = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

        val shouldReset = resetToFactory && isSystem && hasUpdates
        val broadcastIntent = Intent("io.github.samolego.canta.UNINSTALL_RESULT_ACTION")
        val intent = PendingIntent.getBroadcast(
            applicationContext, 0, broadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val packageInstaller = getPackageInstaller()
        val flags = if (isSystem) 0x00000004 else 0x00000002

        try {
            if (shouldReset) {
                HiddenApiBypass.invoke(
                    PackageInstaller::class.java, packageInstaller, "uninstall",
                    packageName, 0x00000002, intent.intentSender
                )
                delay(1000) // Non-blocking delay on IO thread
            }

            HiddenApiBypass.invoke(
                PackageInstaller::class.java, packageInstaller, "uninstall",
                packageName, flags, intent.intentSender
            )
            true
        } catch (e: Exception) {
            LogUtils.e(APP_NAME, "Uninstall failed for $packageName: ${e.message}")
            false
        }
    }

    /**
     * Reinstalls app using Shizuku.
     * @param packageName package name of the app to reinstall
     */
    private fun reinstallApp(packageName: String): Boolean {
        val broadcastIntent = Intent("io.github.samolego.canta.INSTALL_RESULT_ACTION")
        val intent = PendingIntent.getBroadcast(
            applicationContext, 0, broadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            HiddenApiBypass.invoke(
                IPackageInstaller::class.java, ShizukuPackageInstallerUtils.getPrivilegedPackageInstaller(),
                "installExistingPackage", packageName, 0x00400000, 0, intent.intentSender, 0, null
            )
            true
        } catch (e: Exception) {
            LogUtils.e(APP_NAME, "Reinstall failed: ${e.message}")
            false
        }
    }

    private fun getPackageInstaller(): PackageInstaller {
        val iPackageInstaller = ShizukuPackageInstallerUtils.getPrivilegedPackageInstaller()
        
        val userId = try {
            val handle = android.os.Process.myUserHandle()
            val method = handle.javaClass.getDeclaredMethod("getIdentifier")
            method.invoke(handle) as Int
        } catch (e: Exception) {
            0
        }

        return ShizukuPackageInstallerUtils.createPackageInstaller(iPackageInstaller, "com.android.shell", userId, this)
    }
}
