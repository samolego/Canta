package io.github.samolego.canta.util.shizuku

import android.content.pm.PackageManager
import io.github.samolego.canta.SHIZUKU_PACKAGE_NAME
import io.github.samolego.canta.packageName
import io.github.samolego.canta.util.LogUtils
import rikka.shizuku.Shizuku
import rikka.sui.Sui

class ShizukuPermission {
    companion object {
        private const val SHIZUKU_CODE = 0xCA07A

        private val isSui: Boolean = Sui.init(packageName)
        private val TAG: String = ShizukuPermission::class.java.simpleName
        private var binderStatus = Shizuku.pingBinder()

        init {
            Shizuku.addBinderDeadListener { binderStatus = false }
            Shizuku.addBinderReceivedListener { binderStatus = true }
        }

        /** Checks if the shizuku permission is granted. Call from main thread only! */
        fun requestShizukuPermission(onPermissionResult: (Int) -> Unit) {
            if (!checkRequirements()) {
                LogUtils.i(
                        TAG,
                        "Shizuku permission result: ping: ${Shizuku.pingBinder()}, preV11: ${Shizuku.isPreV11()}"
                )
                onPermissionResult(PackageManager.PERMISSION_DENIED)
            } else if (isPermissionGranted()) {
                LogUtils.i(
                        TAG,
                        "Shizuku permission result: ${Shizuku.checkSelfPermission()}, sui status: $isSui"
                )
                onPermissionResult(PackageManager.PERMISSION_GRANTED)
            } else {
                LogUtils.i(TAG, "Requesting shizuku permission")
                Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                    if (requestCode == SHIZUKU_CODE) {
                        onPermissionResult(grantResult)
                    }
                }
                Shizuku.requestPermission(SHIZUKU_CODE)
            }
        }

        private fun checkRequirements(): Boolean {
            return binderStatus && !Shizuku.isPreV11() && !Shizuku.shouldShowRequestPermissionRationale()
        }

        private fun isPermissionGranted(): Boolean {
            return isSui || Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }

        fun isCantaAuthorized(): Boolean {
            return checkRequirements() && isPermissionGranted()
        }

        fun checkShizukuActive(packageManager: PackageManager): ShizukuStatus {
            if (isSui) {
                return ShizukuStatus.ACTIVE
            }
            try {
                packageManager.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
                if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                    return ShizukuStatus.ACTIVE
                }
                return ShizukuStatus.NOT_ACTIVE
            } catch (e: PackageManager.NameNotFoundException) {
                return ShizukuStatus.NOT_INSTALLED
            }
        }
    }
}

/** Enum class to represent Shizuku status. */
enum class ShizukuStatus {
    ACTIVE,
    NOT_ACTIVE,
    NOT_INSTALLED,
}
