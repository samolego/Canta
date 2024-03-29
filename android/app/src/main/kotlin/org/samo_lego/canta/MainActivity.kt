package org.samo_lego.canta

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageInstaller
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : FlutterActivity() {
    private val LOGGER_TAG = "Canta"
    private val CHANNEL = BuildConfig.APPLICATION_ID + "/native"
    private val SHIZUKU_CODE = 0xCA07A
    private val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
    private val BLOAT_LIST: HashMap<String, BloatData> = HashMap()
    private val flutterExecutor: ExecutorService = Executors.newCachedThreadPool()
    private lateinit var SETUP_THREAD: Thread
    private var shizukuPermissionFuture = CompletableFuture<Boolean>()
    private val isSui = Sui.init(BuildConfig.APPLICATION_ID)

    // main
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                BLOAT_LIST[key] = bloatData
            }
        }
        SETUP_THREAD.start()

        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                shizukuPermissionFuture.complete(granted)
            }
        }
    }


    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger, CHANNEL
        ).setMethodCallHandler { call, result ->
            flutterExecutor.submit {
                when (call.method) {
                    "checkShizukuActive" -> {
                        if (isSui) {
                            result.success(true)
                        } else {
                            val shizukuInstalled =
                                getInstalledPackages().any { app -> app.packageName == SHIZUKU_PACKAGE_NAME }

                            if (shizukuInstalled) {
                                result.success(Shizuku.pingBinder() && !Shizuku.isPreV11())
                            } else {
                                result.success(null)
                            }
                        }

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
                        Log.i(LOGGER_TAG, "Uninstalling '$packageName'")
                        result.success(uninstallApp(packageName))
                    }

                    "reinstallApp" -> {
                        val packageName = call.argument<String>("packageName")!!
                        Log.i(LOGGER_TAG, "Installing '$packageName'")
                        result.success(reinstallApp(packageName))
                    }

                    "getAppInfo" -> {
                        val packageName = call.argument<String>("packageName")!!
                        val packageManager = packageManager
                        Log.i(LOGGER_TAG, "Getting info for '$packageName'")
                        val packageInfo = getInfoForPackage(packageName, packageManager)
                        val appInfo =
                            AppInfo.fromPackageInfo(packageInfo, packageManager, BLOAT_LIST)
                        result.success(appInfo.toMap())
                    }

                    "getUninstalledApps" -> {
                        Log.i(LOGGER_TAG, "Getting uninstalled apps ...")
                        result.success(getUninstalledPackages())
                    }

                    "getInstalledAppsInfo" -> {
                        Log.i(LOGGER_TAG, "Getting installed apps info ...")
                        result.success(getInstalledAppsInfo())
                    }

                    else -> result.notImplemented()
                }
            }
        }
    }

    private fun getInfoForPackage(
        packageName: String,
        packageManager: PackageManager
    ): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
    }

    private fun checkShizukuPermission(): Boolean {
        return if (!Shizuku.pingBinder() || Shizuku.isPreV11() || Shizuku.shouldShowRequestPermissionRationale()) {
            false
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED || isSui) {
            true
        } else {
            Shizuku.requestPermission(SHIZUKU_CODE)

            val result = shizukuPermissionFuture.get()
            shizukuPermissionFuture = CompletableFuture<Boolean>()

            result
        }
    }

    private fun getUninstalledPackages(): List<String> {
        val pm = packageManager
        val flags = PackageManager.MATCH_UNINSTALLED_PACKAGES
        val unp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageInfoFlags.of(flags.toLong()))
                .map { app -> app.packageName }
        } else {
            pm.getInstalledApplications(flags)
                .map { app -> app.packageName }
        }.toSet()
        val inp = getInstalledPackages().map { app -> app.packageName }.toSet()

        return (unp - inp).toList().sorted()
    }

    private fun getInstalledPackages(): List<PackageInfo> {
        val flags = PackageManager.GET_META_DATA

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(
                PackageInfoFlags.of(flags.toLong())
            )
        } else {
            packageManager.getInstalledPackages(flags)
        }
    }

    private fun getInstalledAppsInfo(): List<Map<String, Any?>> {
        val installedApps = getInstalledPackages()
        SETUP_THREAD.join()
        return installedApps.map { app ->
            AppInfo.fromPackageInfo(app, packageManager, BLOAT_LIST).toMap()
        }
    }


    /**
     * Uninstalls app using Shizuku.
     * See <a href="https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/PackageManagerShellCommand.java;drc=bcb2b436bde55ee40050400783a9c083e77ce2fe;l=2144">PackageManagerShellCommand.java</a>
     * @param packageName package name of the app to uninstall
     */
    private fun uninstallApp(packageName: String): Boolean {
        if (!checkShizukuPermission()) {
            // Shizuku is not available, handle accordingly
            return false
        }

        val broadcastIntent = Intent("org.samo_lego.canta.UNINSTALL_RESULT_ACTION")
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            broadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val packageInstaller = getPackageInstaller()
        val packageInfo = getInfoForPackage(packageName, packageManager)

        val isSystem = (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        Log.i(LOGGER_TAG, "Uninstalling '$packageName' [system: $isSystem]")

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
        if (!checkShizukuPermission()) {
            // Shizuku is not available, handle accordingly
            return false
        }

        val installReason = PackageManager.INSTALL_REASON_UNKNOWN
        val broadcastIntent = Intent("org.samo_lego.canta.INSTALL_RESULT_ACTION")
        val intent = PendingIntent.getBroadcast(
            context,
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
