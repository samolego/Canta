package io.github.samolego.canta

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.github.samolego.canta.data.proto.BootSnapshotProto
import io.github.samolego.canta.util.LogUtils
import io.github.samolego.canta.util.PackageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Snapshots the package states right when the boot completed, then starts the post-boot
 * analytics check (see [BootCheckWorker]), which may only run later. Both are no-ops while analytics is off.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Only after the snapshot is written, so the worker never runs before it.
                if (snapshot(app)) enqueueCheck(app)
            } catch (e: Exception) {
                LogUtils.w("BootReceiver", "Could not snapshot the package states: $e")
            } finally {
                pending.finish()
            }
        }
    }

    private fun enqueueCheck(context: Context) {
        val request = OneTimeWorkRequestBuilder<BootCheckWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        // A check from an earlier boot that is still waiting for a network does the same thing, so keep it.
        WorkManager.getInstance(context).enqueueUniqueWork(BootCheckWorker.NAME, ExistingWorkPolicy.KEEP, request)
    }

    /** Whether a snapshot was written. */
    private suspend fun snapshot(context: Context): Boolean {
        val deps = (context as CantaApplication).dependencies
        val pm = context.packageManager
        val snapshot = deps.packageTelemetry.snapshotAtBoot(
            bootId = deps.handler.bootId(),
            fingerprint = deps.handler.buildFingerprint(),
            stateOf = { pkg -> stateOf(pm, pkg) },
        )
        val file = BootCheckWorker.snapshotFile(context)
        if (snapshot == null) {
            file.delete()
            return false
        }
        file.writeBytes(ProtoBuf.encodeToByteArray(BootSnapshotProto.serializer(), snapshot))
        return true
    }

    /** Not installed for the current user (or not found at all) counts as uninstalled; null if it can't be read. */
    private fun stateOf(pm: PackageManager, pkg: String): PackageState? = try {
        val info = pm.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        PackageState(uninstalled = info.flags and ApplicationInfo.FLAG_INSTALLED == 0)
    } catch (e: PackageManager.NameNotFoundException) {
        PackageState(uninstalled = true)
    } catch (e: Exception) {
        null
    }
}
