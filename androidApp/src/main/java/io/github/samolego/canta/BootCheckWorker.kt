package io.github.samolego.canta

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.samolego.canta.data.proto.BootSnapshotProto
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.File

/**
 * Reports what became of the pending uninstalls once the phone has booted, without
 * needing Shizuku (it isn't running yet). Uses the package states [BootReceiver] captured
 * when the boot completed, as this may run much later (it waits for a network).
 * Does nothing while analytics is off.
 */
class BootCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val file = snapshotFile(applicationContext)
        val snapshot = runCatching {
            if (file.exists()) ProtoBuf.decodeFromByteArray(BootSnapshotProto.serializer(), file.readBytes()) else null
        }.getOrNull()?.takeIf { it.bootId.isNotEmpty() }
        if (snapshot == null) {
            file.delete()
            return Result.success()
        }
        val deps = (applicationContext as CantaApplication).dependencies
        val delivered = deps.packageTelemetry.resolveAfterBoot(snapshot, deps.handler.bootId())
        if (!delivered) return Result.retry()
        file.delete()
        return Result.success()
    }

    companion object {
        const val NAME = "boot_check"

        /** Kept out of backups: it only makes sense for the boot it was taken in. */
        fun snapshotFile(context: Context) = File(context.noBackupFilesDir, "boot_snapshot.pb")
    }
}
