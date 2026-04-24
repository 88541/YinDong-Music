package com.yindong.music.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

private val THEME_KEY = stringPreferencesKey("selected_theme_id")
private const val PREFS_NAME = "app_theme_prefs"
private const val PREF_KEY = "current_theme_id"
private const val DEFAULT_THEME_ID = "light_glass"

object ThemeManager {

    private val _themeState = MutableStateFlow(AppThemes[1])
    val themeState: StateFlow<AppTheme> = _themeState.asStateFlow()

    fun getSavedThemeId(context: Context): String {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREF_KEY, DEFAULT_THEME_ID) ?: DEFAULT_THEME_ID
        } catch (_: Exception) {
            DEFAULT_THEME_ID
        }
    }

    suspend fun saveThemeId(context: Context, themeId: String) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_KEY, themeId)
                .apply()
            context.applicationContext.themeDataStore.edit { prefs ->
                prefs[THEME_KEY] = themeId
            }
            val newTheme = getThemeById(themeId)
            _themeState.value = newTheme
        } catch (_: Exception) {}
    }

    suspend fun initTheme(context: Context) {
        val savedId = getSavedThemeId(context)
        _themeState.value = getThemeById(savedId)
        val themeFromDataStore = getThemeFlowSafe(context)
        if (themeFromDataStore != null) {
            _themeState.value = themeFromDataStore
        }
    }

    suspend fun getThemeFlowSafe(context: Context): AppTheme? {
        return withTimeoutOrNull(1.seconds) {
            try {
                context.themeDataStore.data
                    .map { preferences ->
                        val id = preferences[THEME_KEY]
                        if (!id.isNullOrEmpty()) getThemeById(id) else null
                    }
                    .catch { emit(null) }
                    .first()
            } catch (_: Exception) { null }
        }
    }

    fun getCurrentTheme(context: Context): AppTheme {
        return _themeState.value
    }

    suspend fun resetToDefault(context: Context) {
        saveThemeId(context, DEFAULT_THEME_ID)
    }
}
