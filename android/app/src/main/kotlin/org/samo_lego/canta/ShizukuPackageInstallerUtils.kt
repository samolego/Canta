package org.samo_lego.canta

import android.content.pm.IPackageInstaller
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
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
    ): PackageInstaller {
        return PackageInstaller::class.java.getConstructor(
            IPackageInstaller::class.java,
            String::class.java,
            String::class.java,
            Int::class.javaPrimitiveType
        ).newInstance(installer, installerPackageName, null, userId)
    }
}