package io.github.samolego.canta.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class to hold information about an app.
 */
@Parcelize
data class AppInfo(
    private val appName: String?,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val isUninstalled: Boolean,
    val isDisabled: Boolean,
    val bloatData: BloatData?
) : Parcelable {

    val name: String
        get() = appName ?: packageName.substringAfterLast('.')
    val removalInfo: RemovalRecommendation?
        get() = bloatData?.removal
    val description: String?
        get() = bloatData?.description


    companion object {
        fun fromPackageInfo(
            packageInfo: PackageInfo,
            packageManager: PackageManager,
            isUninstalled: Boolean,
            bloatList: Map<String, BloatData> = emptyMap()
        ): AppInfo {
            val bloatData = bloatList[packageInfo.packageName]

            val isSystemApp =
                (packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val isDisabled = try {
                !packageManager.getApplicationInfo(packageInfo.packageName, 0).enabled
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

            val versionName = packageInfo.versionName ?: "unknown"

            return AppInfo(
                appName = packageInfo.applicationInfo!!.loadLabel(packageManager).toString(),
                packageName = packageInfo.packageName,
                versionName = versionName,
                versionCode = packageInfo.longVersionCode,
                isSystemApp = isSystemApp,
                isUninstalled = isUninstalled,
                isDisabled = isDisabled,
                bloatData = bloatData,
            )
        }
    }
}
