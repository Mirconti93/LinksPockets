package it.linkspockets.data.settings

import android.content.SharedPreferences
import it.linkspockets.domain.model.AppTheme
import it.linkspockets.domain.repository.ThemeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SharedPreferencesThemeRepository(
    private val preferences: SharedPreferences,
    private val dispatcher: CoroutineDispatcher,
) : ThemeRepository {
    private val theme = MutableStateFlow(readTheme())

    override fun observeTheme(): Flow<AppTheme> = theme.asStateFlow()

    override suspend fun setTheme(theme: AppTheme) {
        withContext(dispatcher) {
            check(preferences.edit().putString(THEME_KEY, theme.name).commit()) {
                "Unable to save the theme preference"
            }
            this@SharedPreferencesThemeRepository.theme.value = theme
        }
    }

    private fun readTheme(): AppTheme {
        val stored = try {
            preferences.getString(THEME_KEY, null)
        } catch (_: ClassCastException) {
            null
        }
        return AppTheme.entries.firstOrNull { it.name == stored } ?: AppTheme.LIGHT
    }

    private companion object {
        const val THEME_KEY = "app_theme"
    }
}
