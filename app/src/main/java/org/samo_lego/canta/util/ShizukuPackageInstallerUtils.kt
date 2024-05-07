package org.samo_lego.canta.util

import android.app.Activity
import android.content.Context
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.InvocationTargetException

/**
 * Taken from <a href="https://github.com/depau/fdroid_shizuku_privileged_extension/blob/main/app/src/main/java/org/fdroid/fdroid/privileged/ShizukuPackageInstallerUtils.kt">FDroid Priv</a>.
 */
object ShizukuPackageInstallerUtils {
    private val PACKAGE_MANAGER: IPackageManager by lazy {
        // This is needed to access hidden methods in IPackageManager
        HiddenApiBypass.addHiddenApiExemptions(
            "Landroid/content/pm"
        )

        IPackageManager.Stub.asInterface(
            ShizukuBinderWrapper(
                SystemServiceHelper.getSystemService(
                    "package"
                )
            )
        )
    }

    fun getPrivilegedPackageInstaller(): IPackageInstaller {
        val packageInstaller: IPackageInstaller = PACKAGE_MANAGER.packageInstaller
        return IPackageInstaller.Stub.asInterface(ShizukuBinderWrapper(packageInstaller.asBinder()))
    }

    /**
     * Taken from https://github.com/RikkaApps/Shizuku-API/blob/01e08879d58a5cb11a333535c6ddce9f7b7c88ff/demo/src/main/java/rikka/shizuku/demo/util/PackageInstallerUtils.java#L15
     * @author RikkaW
     */
    @Throws(
        NoSuchMethodException::class,
        IllegalAccessException::class,
        InvocationTargetException::class,
        InstantiationException::class,
    )
    fun createPackageInstaller(
        installer: IPackageInstaller?,
        installerPackageName: String?,
        userId: Int,
        activity: Activity,
    ): PackageInstaller {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            return PackageInstaller::class.java.getConstructor(
                IPackageInstaller::class.java,
                String::class.java,
                String::class.java,
                Int::class.javaPrimitiveType
            ).newInstance(installer, installerPackageName, null, userId)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PackageInstaller::class.java.getConstructor(
                IPackageInstaller::class.java, String::class.java, Int::class.java
            )
                .newInstance(installer, installerPackageName, userId)
        } else {
            return PackageInstaller::class.java.getConstructor(
                Context::class.java,
                PackageManager::class.java,
                IPackageInstaller::class.java,
                String::class.java,
                Int::class.javaPrimitiveType
            )
                .newInstance(
                    activity,
                    activity.packageManager,
                    installer,
                    installerPackageName,
                    userId
                )
        }
    }
}