package io.github.samolego.canta.packages

import android.content.Intent
import android.content.IntentSender
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * An [IntentSender] that delivers its intent in-process, for receiving
 * `PackageInstaller` results without a `BroadcastReceiver`. This works in the
 * app and in `app_process`, which can't receive broadcasts.
 *
 * It's a plain [Binder] speaking the `IIntentSender` protocol: `send()` is
 * its first transaction and, on every supported Android version, starts
 * with `int code, Intent intent`. That avoids linking against the hidden
 * `IIntentSender.Stub`.
 */
internal class LocalIntentSender : Binder() {

    private companion object {
        const val DESCRIPTOR = "android.content.IIntentSender"
    }

    private val result = CompletableFuture<Intent>()


    val intentSender: IntentSender by lazy {
        allowHiddenApis()
        val target = Class.forName("$DESCRIPTOR\$Stub").getMethod("asInterface", IBinder::class.java).invoke(null, this)
        IntentSender::class.java.getConstructor(Class.forName(DESCRIPTOR)).newInstance(target)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code != FIRST_CALL_TRANSACTION) return super.onTransact(code, data, reply, flags)
        data.enforceInterface(DESCRIPTOR)
        data.readInt() // result code, unused by PackageInstaller
        if (data.readInt() != 0) result.complete(Intent.CREATOR.createFromParcel(data))
        return true
    }

    /** The delivered intent, or null if none arrived within [timeoutMs]. */
    fun await(timeoutMs: Long): Intent? =
        runCatching { result.get(timeoutMs, TimeUnit.MILLISECONDS) }.getOrNull()
}
