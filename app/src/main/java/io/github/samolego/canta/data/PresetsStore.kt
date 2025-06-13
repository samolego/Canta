package io.github.samolego.canta.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by
        preferencesDataStore(name = "canta_presets")

class PresetsStore(private val context: Context) {

    val latestCommitHashFlow: Flow<String> =
            context.dataStore.data.map { preferences ->
                preferences[KEY_LATEST_BLOAT_COMMIT_HASH] ?: ""
            }

    suspend fun setLatestCommitHash(hash: String) {
        context.dataStore.edit { preferences -> preferences[KEY_LATEST_BLOAT_COMMIT_HASH] = hash }
    }

    suspend fun getLatestCommitHash(): String {
        return latestCommitHashFlow.firstOrNull() ?: ""
    }

    companion object {
        private val KEY_LATEST_BLOAT_COMMIT_HASH = stringPreferencesKey("latest_bloat_commit_hash")
    }
}
