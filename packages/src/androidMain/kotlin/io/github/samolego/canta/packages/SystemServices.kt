package io.github.samolego.canta.packages

import android.os.IBinder

/**
 * How privileged code reaches system services. The Android app goes through
 * Shizuku (binders wrapped so calls run as Shizuku's shell/root identity);
 * the ADB helper already runs as the shell user and uses them directly.
 */
interface SystemServices {
    /** The binder of system service [name], e.g. `"package"`. */
    fun service(name: String): IBinder

    /** Wraps a binder obtained from a system service the same way as [service]. */
    fun wrap(binder: IBinder): IBinder

    /** The Android user to act on. */
    val userId: Int
}
