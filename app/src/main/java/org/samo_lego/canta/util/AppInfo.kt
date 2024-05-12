package org.samo_lego.canta.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
    val bloatData: BloatData?
) : Parcelable {

    val name: String
        get() = appName ?: packageName.split(".").last()
    val removalInfo: RemovalRecommendation?
        get() = bloatData?.removal
    val installInfo: InstallData?
        get() = bloatData?.installData
    val description: String?
        get() = bloatData?.description

    fun getIcon(packageManager: PackageManager): Drawable {
        return packageManager.getApplicationIcon(packageName)
    }


    companion object {
        fun fromPackageInfo(
            packageInfo: PackageInfo,
            packageManager: PackageManager,
            isUninstalled: Boolean,
            bloatList: Map<String, BloatData> = emptyMap()
        ): AppInfo {
            val bloatData = bloatList[packageInfo.packageName]

            val isSystemApp =
                (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val versionName = packageInfo.versionName ?: "unknown"

            return AppInfo(
                packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                packageInfo.packageName,
                versionName,
                packageInfo.longVersionCode,
                isSystemApp,
                isUninstalled,
                bloatData,
            )
        }
    }
}
