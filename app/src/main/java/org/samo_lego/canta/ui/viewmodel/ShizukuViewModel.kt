package org.samo_lego.canta.ui.viewmodel

import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import org.samo_lego.canta.SHIZUKU_PACKAGE_NAME
import org.samo_lego.canta.packageName
import org.samo_lego.canta.util.ShizukuInfo
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import java.util.concurrent.CompletableFuture

private const val SHIZUKU_DEAD_BINDER = 0xDEAD

class ShizukuViewModel : ViewModel() {
    private val isSui: Boolean = Sui.init(packageName)

    companion object {
        private val TAG: String = ShizukuViewModel::class.java.simpleName
        const val SHIZUKU_CODE = 0xCA07A
        var shizukuPermissionFuture = CompletableFuture<Boolean>()
        var binderStatus = Shizuku.pingBinder()
            set(value) {
                field = value
                println("Binder status: $value")
            }
    }

    fun checkShizukuPermission(): Boolean {
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

            val result = shizukuPermissionFuture.get()
            shizukuPermissionFuture = CompletableFuture<Boolean>()

            result
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