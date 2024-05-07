package org.samo_lego.canta

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageInstaller
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.json.JSONObject
import org.lsposed.hiddenapibypass.HiddenApiBypass
import org.samo_lego.canta.extension.getInfoForPackage
import org.samo_lego.canta.ui.CantaApp
import org.samo_lego.canta.ui.theme.CantaTheme
import org.samo_lego.canta.util.BloatData
import org.samo_lego.canta.util.BloatUtils
import org.samo_lego.canta.util.ShizukuPackageInstallerUtils
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import java.io.File
import kotlin.properties.Delegates

const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
const val APP_NAME = "Canta"
const val packageName = "org.samo_lego.canta"

class MainActivity : ComponentActivity() {
    private lateinit var SETUP_THREAD: Thread
    private var isSui by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isSui = Sui.init(applicationContext.packageName)

        SETUP_THREAD = Thread {
            // Load app data file
            val uadList = File(filesDir, "uad_lists.json")
            val config = File(filesDir, "canta.conf")
            val bloatFetcher = BloatUtils()

            val uadLists =
                if (!uadList.exists() || !config.exists() || bloatFetcher.checkForUpdates(config)) {
                    uadList.createNewFile()

                    bloatFetcher.fetchBloatList(uadList, config)
                } else {
                    // Just read the file
                    JSONObject(uadList.readText())
                }

            // Parse json to map
            for (key in uadLists.keys()) {
                val json = uadLists.getJSONObject(key)
                val bloatData = BloatData.fromJson(json)

                //BLOAT_LIST[key] = bloatData
            }
        }

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
                                packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
                            startActivity(launchIntent)
                        },
                        uninstallApp = { uninstallApp(it) },
                        reinstallApp = { reinstallApp(it) },
                    )
                }
            }
        }
    }


    /*fun handleCall() {
        val method = ""
        when (method) {
            "checkShizukuActive" -> {


            }

            "checkShizukuPermission" -> result.success(checkShizukuPermission())
            "launchShizuku" -> {
                // Open shizuku app
                val launchIntent =
                    packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
                startActivity(launchIntent)
            }

            "uninstallApp" -> {
                val packageName = call.argument<String>("packageName")!!
                Log.i(APP_NAME, "Uninstalling '$packageName'")
                result.success(uninstallApp(packageName))
            }

            "reinstallApp" -> {
                val packageName = call.argument<String>("packageName")!!
                Log.i(APP_NAME, "Installing '$packageName'")
                result.success(reinstallApp(packageName))
            }

            "getAppInfo" -> {
                val packageName = call.argument<String>("packageName")!!
                val packageManager = packageManager
                Log.i(APP_NAME, "Getting info for '$packageName'")
                val packageInfo = getInfoForPackage(packageName, packageManager)
                val appInfo =
                    AppInfo.fromPackageInfo(packageInfo, packageManager, BLOAT_LIST)
                result.success(appInfo.toMap())
            }

            "getUninstalledApps" -> {
                Log.i(APP_NAME, "Getting uninstalled apps ...")
                result.success(getUninstalledPackages())
            }

            "getInstalledAppsInfo" -> {
                Log.i(APP_NAME, "Getting installed apps info ...")
                result.success(getInstalledAppsInfo())
            }

            else -> result.notImplemented()
        }
    }*/


    /**
     * Uninstalls app using Shizuku.
     * See <a href="https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/PackageManagerShellCommand.java;drc=bcb2b436bde55ee40050400783a9c083e77ce2fe;l=2144">PackageManagerShellCommand.java</a>
     * @param packageName package name of the app to uninstall
     */
    private fun uninstallApp(packageName: String): Boolean {
        val broadcastIntent = Intent("org.samo_lego.canta.UNINSTALL_RESULT_ACTION")
        val intent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            broadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val packageInstaller = getPackageInstaller()
        val packageInfo = packageManager.getInfoForPackage(packageName)

        val isSystem = (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        Log.i(APP_NAME, "Uninstalling '$packageName' [system: $isSystem]")

        // 0x00000004 = PackageManager.DELETE_SYSTEM_APP
        // 0x00000002 = PackageManager.DELETE_ALL_USERS
        val flags = if (isSystem) 0x00000004 else 0x00000002

        return try {
            HiddenApiBypass.invoke(
                PackageInstaller::class.java,
                packageInstaller,
                "uninstall",
                packageName,
                flags,
                intent.intentSender
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reinstalls app using Shizuku.
     * See <a href="https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/PackageManagerShellCommand.java;drc=bcb2b436bde55ee40050400783a9c083e77ce2fe;l=1408>PackageManagerShellCommand.java</a>
     * @param packageName package name of the app to reinstall (must preinstalled on the phone)
     */
    private fun reinstallApp(packageName: String): Boolean {
        val installReason = PackageManager.INSTALL_REASON_UNKNOWN
        val broadcastIntent = Intent("org.samo_lego.canta.INSTALL_RESULT_ACTION")
        val intent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            broadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
            iPackageInstaller, "com.android.shell", userId, this
        )
    }
}
