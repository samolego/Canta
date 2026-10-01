package io.github.samolego.canta.packages

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Every package on the device: installed ones and those uninstalled for the
 * current user (still on the system partition), in a single query.
 */
fun PackageManager.getAllPackages(): List<PackageInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()))
    } else {
        getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES)
    }

/** The package, installed or uninstalled for the user, or null if the device doesn't know it. */
fun PackageManager.getInfoForPackage(packageName: String): PackageInfo? =
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()))
        } else {
            getPackageInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

val ApplicationInfo.isSystem: Boolean
    get() = (flags and ApplicationInfo.FLAG_SYSTEM) != 0

/** A system app with an update installed over its factory version. */
val ApplicationInfo.hasSystemUpdate: Boolean
    get() = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

/** Uninstalling can first remove the update, resetting the app to its factory version. */
val ApplicationInfo.canResetToFactory: Boolean
    get() = isSystem && hasSystemUpdate

/** Installed for the current user (not "uninstalled for user 0"). */
val ApplicationInfo.isInstalledForUser: Boolean
    get() = (flags and ApplicationInfo.FLAG_INSTALLED) != 0
