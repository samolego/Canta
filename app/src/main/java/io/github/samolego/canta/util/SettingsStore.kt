package io.github.samolego.canta.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

// Create a DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "canta_settings")

class SettingsStore(private val context: Context) {

    // Auto update bloat list preference
    val autoUpdateBloatListFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_UPDATE_BLOAT_LIST] != false
    }

    suspend fun setAutoUpdateBloatList(autoUpdate: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_UPDATE_BLOAT_LIST] = autoUpdate
        }
    }

    // Confirm before uninstall preference
    val confirmBeforeUninstallFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_CONFIRM_BEFORE_UNINSTALL] != false
    }

    suspend fun setConfirmBeforeUninstall(confirm: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CONFIRM_BEFORE_UNINSTALL] = confirm
        }
    }

    // Confirm before uninstall preference
    val disableRiskDialogFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DISABLE_RISK_DIALOG] == true
    }

    suspend fun setDisableRiskDialog(disable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DISABLE_RISK_DIALOG] = disable
        }
    }

    // Latest commit hash for bloat list
    val latestCommitHashFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LATEST_BLOAT_COMMIT_HASH] ?: ""
    }

    suspend fun setLatestCommitHash(hash: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LATEST_BLOAT_COMMIT_HASH] = hash
        }
    }

    suspend fun getLatestCommitHash(): String {
        return latestCommitHashFlow.firstOrNull() ?: ""
    }


    companion object {
        private val KEY_AUTO_UPDATE_BLOAT_LIST = booleanPreferencesKey("auto_update_bloat_list")
        private val KEY_CONFIRM_BEFORE_UNINSTALL = booleanPreferencesKey("confirm_before_uninstall")
        private val KEY_DISABLE_RISK_DIALOG = booleanPreferencesKey("disable_risk_dialog")
        private val KEY_LATEST_BLOAT_COMMIT_HASH = stringPreferencesKey("latest_bloat_commit_hash")
    }
}
