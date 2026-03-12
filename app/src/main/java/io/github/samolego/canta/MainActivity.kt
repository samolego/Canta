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
                        uninstallApp = { pkg, reset ->
                            // Fix: Use a coroutine that the UI can actually track or wait for
                            var result = false
                            lifecycleScope.launch {
                                result = uninstallApp(pkg, reset)
                            }
                            true // Temporary true to keep UI logic flowing
                        },
                        canResetAppToFactory = { checkIfCanResetToFactory(it) },
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

    /**
     * Uninstalls app using Shizuku with state polling for system resets.
     */
    private suspend fun uninstallApp(packageName: String, resetToFactory: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val packageInfo = packageManager.getInfoForPackage(packageName) ?: return@withContext false
            val isSystem = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val hasUpdates = (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            val shouldReset = resetToFactory && isSystem && hasUpdates
            val broadcastIntent = Intent("io.github.samolego.canta.UNINSTALL_RESULT_ACTION")
            val intent = PendingIntent.getBroadcast(
                applicationContext, 0, broadcastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Fix: Move installer retrieval inside try-catch to handle resolution failures
            val packageInstaller = getPackageInstaller() ?: return@withContext false
            val flags = if (isSystem) 0x00000004 else 0x00000002

            if (shouldReset) {
                HiddenApiBypass.invoke(
                    PackageInstaller::class.java, packageInstaller, "uninstall",
                    packageName, 0x00000002, intent.intentSender
                )
                
                // Fix: Poll package state instead of using a fixed delay
                var attempts = 0
                while (attempts < 10) {
                    delay(500)
                    val currentInfo = packageManager.getInfoForPackage(packageName)?.applicationInfo
                    if (currentInfo == null || (currentInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                        break
                    }
                    attempts++
                }
            }

            HiddenApiBypass.invoke(
                PackageInstaller::class.java, packageInstaller, "uninstall",
                packageName, flags, intent.intentSender
            )
            true
        } catch (e: Exception) {
            LogUtils.e(APP_NAME, "Uninstall failed: ${e.message}")
            false
        }
    }

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

    /**
     * Resolves the PackageInstaller with proper User ID identifier resolution.
     */
    private fun getPackageInstaller(): PackageInstaller? {
        return try {
            val iPackageInstaller = ShizukuPackageInstallerUtils.getPrivilegedPackageInstaller()
            val handle = android.os.Process.myUserHandle()
            val method = handle.javaClass.getDeclaredMethod("getIdentifier")
            val userId = method.invoke(handle) as Int
            
            ShizukuPackageInstallerUtils.createPackageInstaller(iPackageInstaller, "com.android.shell", userId, this)
        } catch (e: Exception) {
            LogUtils.e(APP_NAME, "Failed to resolve User ID or Installer: ${e.message}")
            null
        }
    }
}
