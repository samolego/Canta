package org.samo_lego.canta.ui.viewmodel

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.samo_lego.canta.SHIZUKU_PACKAGE_NAME
import org.samo_lego.canta.extension.getInstalledPackages
import org.samo_lego.canta.packageName
import org.samo_lego.canta.util.ShizukuInfo
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import java.util.concurrent.CompletableFuture

private const val SHIZUKU_CODE = 0xCA07A

class ShizukuViewModel : ViewModel() {
    private var isSui: Boolean = Sui.init(packageName)
    private var shizukuPermissionFuture = CompletableFuture<Boolean>()

    init {
        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_CODE) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                shizukuPermissionFuture.complete(granted)
            }
        }
    }

    suspend fun checkShizukuPermission(): Boolean {
        return if (!Shizuku.pingBinder() || Shizuku.isPreV11() || Shizuku.shouldShowRequestPermissionRationale()) {
            false
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED || isSui) {
            true
        } else {
            Shizuku.requestPermission(SHIZUKU_CODE)

            withContext(Dispatchers.IO) {
                val result = shizukuPermissionFuture.get()
                shizukuPermissionFuture = CompletableFuture<Boolean>()

                result
            }
        }
    }

    fun checkShizukuActive(packageManager: PackageManager): ShizukuInfo {
        if (isSui) {
            return ShizukuInfo.ACTIVE
        }
        val shizukuInstalled =
            packageManager.getInstalledPackages()
                .any { app -> app.packageName == SHIZUKU_PACKAGE_NAME }

        if (shizukuInstalled) {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                return ShizukuInfo.ACTIVE
            }
            return ShizukuInfo.NOT_ACTIVE
        }

        return ShizukuInfo.NOT_INSTALLED
    }
}