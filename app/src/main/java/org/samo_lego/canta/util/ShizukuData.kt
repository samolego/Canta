package org.samo_lego.canta.util

import android.content.pm.PackageManager
import android.util.Log
import org.samo_lego.canta.SHIZUKU_PACKAGE_NAME
import org.samo_lego.canta.packageName
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import java.util.concurrent.CompletableFuture

class ShizukuData {
    companion object {
        private const val SHIZUKU_CODE = 0xCA07A

        private val isSui: Boolean = Sui.init(packageName)
        private val TAG: String = ShizukuData::class.java.simpleName
        private var shizukuPermissionFuture = CompletableFuture<Boolean>()
        private var binderStatus = Shizuku.pingBinder()

        fun init() {
            println("Shizuku permission init")
            // Print current thread name
            println("Current thread: ${Thread.currentThread().name}")
            Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                println("Shizuku permission result: $requestCode, $grantResult")
                if (requestCode == SHIZUKU_CODE) {
                    val granted = grantResult == PackageManager.PERMISSION_GRANTED
                    shizukuPermissionFuture.complete(granted)
                }
            }
            Shizuku.addBinderDeadListener { binderStatus = false }
            Shizuku.addBinderReceivedListener { binderStatus = true }
        }

        /**
         * Checks if the shizuku permission is granted. Call from main thread only!
         */
        fun checkShizukuPermission(): Boolean {
            println("Current thread: ${Thread.currentThread().name}")
            return if (!binderStatus || Shizuku.isPreV11() || Shizuku.shouldShowRequestPermissionRationale()) {
                val shouldShow = try {
                    Shizuku.shouldShowRequestPermissionRationale()
                } catch (e: Exception) {
                    Log.e(TAG, "Error while checking shizuku permission", e)
                    false
                }
                Log.i(
                    TAG,
                    "Shizuku permission result: ping: ${Shizuku.pingBinder()}, preV11: ${Shizuku.isPreV11()}, shouldShowRequestPermissionRationale: $shouldShow"
                )
                false
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED || isSui) {
                Log.i(
                    TAG,
                    "Shizuku permission result: ${Shizuku.checkSelfPermission()}, sui status: $isSui"
                )
                true
            } else {
                Log.i(TAG, "Requesting shizuku permission")
                Shizuku.requestPermission(SHIZUKU_CODE)

                // This hangs the process ...
                //val result = shizukuPermissionFuture.get()
                //shizukuPermissionFuture = CompletableFuture<Boolean>()

                //result
                false
            }
        }
    }



    fun checkShizukuActive(packageManager: PackageManager): ShizukuInfo {
        if (isSui) {
            return ShizukuInfo.ACTIVE
        }
        try {
            packageManager.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                return ShizukuInfo.ACTIVE
            }
            return ShizukuInfo.NOT_ACTIVE
        } catch (e: PackageManager.NameNotFoundException) {
            return ShizukuInfo.NOT_INSTALLED
        }
    }
}

/**
 * Enum class to represent Shizuku status.
 */
enum class ShizukuInfo {
    ACTIVE,
    NOT_ACTIVE,
    NOT_INSTALLED,
}
