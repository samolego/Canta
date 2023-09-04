package org.samo_lego.canta

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream

class AppInfo(
    name: String?,
    private var packageName: String,
    private var icon: ByteArray,
    private var versionName: String,
    private var versionCode: Long,
    private var isSystemApp: Boolean,
) {

    private var name: String

    init {
        this.name = name ?: packageName.split(".").last()
    }


    fun toMap(): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        map["name"] = name
        map["package_name"] = packageName
        map["icon"] = icon
        map["version_name"] = versionName
        map["version_code"] = versionCode
        map["is_system_app"] = isSystemApp
        map["app_type"] = 0

        return map
    }

    companion object {
        fun fromPackageInfo(packageInfo: PackageInfo, packageManager: PackageManager): AppInfo {
            val icon = packageInfo.applicationInfo.loadIcon(packageManager)
            // Convert icon to byte array
            val drawableBytes = drawableToByteArray(icon)

            val isSystemApp =
                (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            return AppInfo(
                packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                packageInfo.packageName,
                drawableBytes,
                packageInfo.versionName,
                packageInfo.longVersionCode,
                isSystemApp
            )
        }


        /**
         * Taken from <a href="https://github.com/sharmadhiraj/installed_apps">sharmadhiraj/installed_apps</a>
         * @author sharmadhiraj
         */
        private fun drawableToByteArray(drawable: Drawable): ByteArray {
            val bitmap = drawableToBitmap(drawable)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        }

        /**
         * Taken from <a href="https://github.com/sharmadhiraj/installed_apps">sharmadhiraj/installed_apps</a>
         * @author sharmadhiraj
         */
        private fun drawableToBitmap(drawable: Drawable): Bitmap {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        }
    }
}
