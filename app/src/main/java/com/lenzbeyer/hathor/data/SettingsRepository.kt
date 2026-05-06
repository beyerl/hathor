package com.lenzbeyer.hathor.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class Settings(
    val outputFolderUri: String? = null,
    val maxParallel: Int = 2,
    val stripCosmetic: Boolean = true,
    val ytDlpVersion: String? = null,
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private object Keys {
        val OUTPUT_FOLDER = stringPreferencesKey("output_folder_uri")
        val MAX_PARALLEL  = intPreferencesKey("max_parallel")
        val STRIP_COSMETIC= booleanPreferencesKey("strip_cosmetic")
        val YTDLP_VERSION = stringPreferencesKey("ytdlp_version")
    }

    val settings: Flow<Settings> = ctx.settingsDataStore.data.map { p ->
        Settings(
            outputFolderUri = p[Keys.OUTPUT_FOLDER],
            maxParallel     = (p[Keys.MAX_PARALLEL] ?: 2).coerceIn(1, 3),
            stripCosmetic   = p[Keys.STRIP_COSMETIC] ?: true,
            ytDlpVersion    = p[Keys.YTDLP_VERSION],
        )
    }

    suspend fun setOutputFolder(uri: String?) = ctx.settingsDataStore.edit {
        if (uri == null) it.remove(Keys.OUTPUT_FOLDER) else it[Keys.OUTPUT_FOLDER] = uri
    }
    suspend fun setMaxParallel(n: Int) = ctx.settingsDataStore.edit {
        it[Keys.MAX_PARALLEL] = n.coerceIn(1, 3)
    }
    suspend fun setStripCosmetic(v: Boolean) = ctx.settingsDataStore.edit {
        it[Keys.STRIP_COSMETIC] = v
    }
    suspend fun setYtDlpVersion(version: String?) = ctx.settingsDataStore.edit {
        if (version == null) it.remove(Keys.YTDLP_VERSION) else it[Keys.YTDLP_VERSION] = version
    }
}
