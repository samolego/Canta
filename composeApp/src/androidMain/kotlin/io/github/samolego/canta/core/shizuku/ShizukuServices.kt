package io.github.samolego.canta.core.shizuku

import android.os.IBinder
import android.os.Process
import io.github.samolego.canta.packages.SystemServices
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/** [SystemServices] through Shizuku: calls run with Shizuku's shell (or root) identity. */
object ShizukuServices : SystemServices {

    override fun service(name: String): IBinder = wrap(SystemServiceHelper.getSystemService(name))

    override fun wrap(binder: IBinder): IBinder = ShizukuBinderWrapper(binder)

    /** Shizuku running as root acts on the app's own user; as shell, on user 0. */
    override val userId: Int
        get() = if (Shizuku.getUid() == 0) Process.myUserHandle().hashCode() else 0
}
