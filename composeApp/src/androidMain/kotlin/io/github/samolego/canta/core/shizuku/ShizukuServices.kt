package io.github.samolego.canta.core.shizuku

import android.os.IBinder
import android.os.Process
import io.github.samolego.canta.packages.SystemServices
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/** [SystemServices] through Shizuku: calls run with Shizuku's shell (or root) identity. */
object ShizukuServices : SystemServices {

    override fun service(name: String): IBinder = wrap(SystemServiceHelper.getSystemService(name))

    override fun wrap(binder: IBinder): IBinder = ShizukuBinderWrapper(binder)

    /**
     * The user (or profile) Canta runs in. Both root and shell may act across
     * users, so this works from secondary users and work profiles too.
     */
    override val userId: Int
        get() = Process.myUserHandle().hashCode()
}
