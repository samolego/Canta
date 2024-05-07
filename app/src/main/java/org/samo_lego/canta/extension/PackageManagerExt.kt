package org.samo_lego.canta.extension

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import org.samo_lego.canta.util.AppInfo


fun PackageManager.getUninstalledPackages(): List<PackageInfo> {
    val flags = PackageManager.MATCH_UNINSTALLED_PACKAGES

    // Get uninstalled packages + installed packages
    val uninstalledPackages = getPackages(flags).toSet()

    val installed = getInstalledPackages().map { it.packageName }
    val minus = uninstalledPackages.filter { !installed.contains(it.packageName) }

    // Return only apps that have been uninstalled
    return minus.toList()
}

fun PackageManager.getInstalledPackages(): List<PackageInfo> {
    val flags = PackageManager.GET_META_DATA
    return getPackages(flags)
}

fun PackageManager.getPackages(flags: Int): List<PackageInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getInstalledPackages(
            PackageManager.PackageInfoFlags.of(flags.toLong())
        )
    } else {
        this.getInstalledPackages(flags)
    }
}

fun PackageManager.getInstalledAppsInfo(): List<AppInfo> {
    val installedApps = getInstalledPackages()
    return installedApps.map { app ->
        AppInfo.fromPackageInfo(app, this)
    }.sortedBy { it.name }
}

fun PackageManager.getUninstalledAppsInfo(): List<AppInfo> {
    val uninstalledApps = getUninstalledPackages()
    return uninstalledApps.map { app ->
        AppInfo.fromPackageInfo(app, this)
    }.sortedBy { it.name }
}


fun PackageManager.getInfoForPackage(
    packageName: String,
): PackageInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        )
    } else {
        this.getPackageInfo(
            packageName,
            PackageManager.GET_META_DATA
        )
    }
}
