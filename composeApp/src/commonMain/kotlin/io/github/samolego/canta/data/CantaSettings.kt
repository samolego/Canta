package io.github.samolego.canta.data

import io.github.samolego.canta.data.bloat.DEFAULT_BLOAT_COMMITS_URL
import io.github.samolego.canta.data.bloat.DEFAULT_BLOAT_URL
import io.github.samolego.canta.data.proto.AppSettingsProto
import io.github.samolego.canta.data.proto.ProtoBlobStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Settings used when nothing has been stored yet. */
private val DEFAULT_SETTINGS = AppSettingsProto(
    autoUpdateBloatList = true,
    confirmBeforeUninstall = true,
)

/**
 * App settings, persisted as one protobuf-encoded [AppSettingsProto] in
 * [storage]; platforms only choose where that is. The URL flows already
 * resolve empty values to the defaults.
 */
class CantaSettings(storage: AppBlobStorage) {

    private val store =
        ProtoBlobStore(storage, AppSettingsProto.serializer(), DEFAULT_SETTINGS, "CantaSettings")

    private fun <T> field(read: (AppSettingsProto) -> T): Flow<T> = store.state.map(read)

    private suspend fun update(transform: (AppSettingsProto) -> AppSettingsProto) {
        store.update(transform)
    }

    val autoUpdateBloatListFlow = field { it.autoUpdateBloatList }
    val confirmBeforeUninstallFlow = field { it.confirmBeforeUninstall }
    val disableRiskDialogFlow = field { it.disableRiskDialog }
    val latestCommitHashFlow = field { it.latestBloatCommitHash }
    val bloatListUrlFlow = field { it.bloatListUrl.ifEmpty { DEFAULT_BLOAT_URL } }
    val commitsUrlFlow = field { it.commitsUrl.ifEmpty { DEFAULT_BLOAT_COMMITS_URL } }
    val allowUnsafeUninstallsFlow = field { it.allowUnsafeUninstalls }
    val hideSuccessDialogFlow = field { it.hideSuccessDialog }
    val authEnabledFlow = field { it.authEnabled }

    suspend fun setAutoUpdateBloatList(autoUpdate: Boolean) = update { it.copy(autoUpdateBloatList = autoUpdate) }
    suspend fun setConfirmBeforeUninstall(needsConfirm: Boolean) = update { it.copy(confirmBeforeUninstall = needsConfirm) }
    suspend fun setDisableRiskDialog(disable: Boolean) = update { it.copy(disableRiskDialog = disable) }
    suspend fun setLatestCommitHash(hash: String) = update { it.copy(latestBloatCommitHash = hash) }
    suspend fun setBloatListUrl(url: String) = update { it.copy(bloatListUrl = url) }
    suspend fun setCommitsUrl(url: String) = update { it.copy(commitsUrl = url) }
    suspend fun setAllowUnsafeUninstalls(allow: Boolean) = update { it.copy(allowUnsafeUninstalls = allow) }
    suspend fun setHideSuccessDialog(hide: Boolean) = update { it.copy(hideSuccessDialog = hide) }
    suspend fun setAuthEnabled(enabled: Boolean) = update { it.copy(authEnabled = enabled) }
}
