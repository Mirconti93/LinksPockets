package it.linkspockets.data.settings

import it.linkspockets.domain.model.AppTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class SharedPreferencesThemeRepositoryTest {
    @Test
    fun defaultsToLightAndRestoresCommittedThemeInNewRepository() = runTest {
        val preferences = MemorySharedPreferences()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = SharedPreferencesThemeRepository(preferences, dispatcher)
        assertEquals(AppTheme.LIGHT, repository.observeTheme().first())
        repository.setTheme(AppTheme.DARK)
        assertEquals(AppTheme.DARK, repository.observeTheme().first())
        val reopened = SharedPreferencesThemeRepository(preferences, dispatcher)
        assertEquals(AppTheme.DARK, reopened.observeTheme().first())
        reopened.setTheme(AppTheme.LIGHT)
        assertEquals(AppTheme.LIGHT, SharedPreferencesThemeRepository(preferences, dispatcher).observeTheme().first())
        assertEquals(2, preferences.commits)
    }

    @Test
    fun unknownOrCorruptPreferenceFallsBackToLight() = runTest {
        val preferences = MemorySharedPreferences()
        val dispatcher = StandardTestDispatcher(testScheduler)
        listOf("UNKNOWN", 42).forEach { value ->
            preferences.values["app_theme"] = value
            assertEquals(AppTheme.LIGHT, SharedPreferencesThemeRepository(preferences, dispatcher).observeTheme().first())
        }
    }

    @Test
    fun failedCommitPreservesPreviousThemeAndAllowsRetry() = runTest {
        val preferences = MemorySharedPreferences()
        val repository = SharedPreferencesThemeRepository(preferences, StandardTestDispatcher(testScheduler))
        preferences.canCommit = false
        assertFailsWith<IllegalStateException> { repository.setTheme(AppTheme.DARK) }
        assertEquals(AppTheme.LIGHT, repository.observeTheme().first())
        preferences.canCommit = true
        repository.setTheme(AppTheme.DARK)
        assertEquals(AppTheme.DARK, repository.observeTheme().first())
    }
}
