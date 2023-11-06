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
    private val icon: ByteArray,
    private val versionName: String,
    private val versionCode: Long,
    private val isSystemApp: Boolean,
    bloatData: BloatData?
) {

    private val name: String
    private val removalInfo: RemovalRecommendation?
    private val installInfo: InstallData?
    private val description: String?

    init {
        this.name = name ?: packageName.split(".").last()
        this.installInfo = bloatData?.installData
        this.removalInfo = bloatData?.removal
        this.description = bloatData?.description
    }


    fun toMap(): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        map["name"] = name
        map["package_name"] = packageName
        map["icon"] = icon
        map["version_name"] = versionName
        map["version_code"] = versionCode
        map["is_system_app"] = isSystemApp
        map["install_info"] = installInfo?.ordinal
        map["removal_info"] = removalInfo?.ordinal
        map["description"] = description

        return map
    }

    companion object {
        fun fromPackageInfo(
            packageInfo: PackageInfo,
            packageManager: PackageManager,
            bloatList: Map<String, BloatData>
        ): AppInfo {
            val icon = packageInfo.applicationInfo.loadIcon(packageManager)
            // Convert icon to byte array
            val drawableBytes = drawableToByteArray(icon)

            val bloatData = bloatList[packageInfo.packageName]


            val isSystemApp =
                (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val versionName = packageInfo.versionName ?: "unknown"

            return AppInfo(
                packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                packageInfo.packageName,
                drawableBytes,
                versionName,
                packageInfo.longVersionCode,
                isSystemApp,
                bloatData
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
