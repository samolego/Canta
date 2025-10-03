package io.github.samolego.canta.data

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import io.github.samolego.canta.util.DEFAULT_BLOAT_COMMITS
import io.github.samolego.canta.util.DEFAULT_BLOAT_URL
import kotlinx.coroutines.flow.map

// Proto DataStore instance
private val Context.dataStore: DataStore<AppSettings> by
        dataStore(fileName = "app_settings.pb", serializer = AppSettingsSerializer)

class SettingsStore private constructor(context: Context) {

    private val dataStore = context.dataStore
    val autoUpdateBloatListFlow = dataStore.data.map { it.autoUpdateBloatList }
    val confirmBeforeUninstallFlow = dataStore.data.map { it.confirmBeforeUninstall }
    val disableRiskDialogFlow = dataStore.data.map { it.disableRiskDialog }
    val latestCommitHashFlow = dataStore.data.map { it.latestBloatCommitHash }
    val bloatListUrlFlow = dataStore.data.map {
        it.bloatListUrl.let {
            if (it.isEmpty()) {
                DEFAULT_BLOAT_URL
            }
            it
        }
    }
    val commitsUrlFlow = dataStore.data.map {
        it.commitsUrl.let {
            if (it.isEmpty()) {
                DEFAULT_BLOAT_COMMITS
            }
            it
        }
    }


    suspend fun setAutoUpdateBloatList(autoUpdate: Boolean) {
        dataStore.updateData { it.toBuilder().setAutoUpdateBloatList(autoUpdate).build() }
    }

    suspend fun setConfirmBeforeUninstall(needsConfirm: Boolean) {
        dataStore.updateData { it.toBuilder().setConfirmBeforeUninstall(needsConfirm).build() }
    }

    suspend fun setDisableRiskDialog(disable: Boolean) {
        dataStore.updateData { it.toBuilder().setDisableRiskDialog(disable).build() }
    }

    suspend fun setLatestCommitHash(hash: String) {
        dataStore.updateData { it.toBuilder().setLatestBloatCommitHash(hash).build() }
    }

    suspend fun setBloatListUrl(url: String) {
        dataStore.updateData { it.toBuilder().setBloatListUrl(url).build() }
    }

    suspend fun setCommitsUrl(url: String) {
        dataStore.updateData { it.toBuilder().setCommitsUrl(url).build() }
    }



    companion object {
        @SuppressLint("StaticFieldLeak") @Volatile private var INSTANCE: SettingsStore? = null

        fun initialize(appContext: Context) {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = SettingsStore(appContext)
                }
            }
        }

        fun getInstance(): SettingsStore {
            return INSTANCE
                    ?: throw IllegalStateException(
                            "SettingsStore has not been initialized. Call initialize() in MyApplication.onCreate()."
                    )
        }
    }
}
