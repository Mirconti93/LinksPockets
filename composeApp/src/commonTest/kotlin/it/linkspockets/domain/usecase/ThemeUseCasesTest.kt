package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.AppTheme
import it.linkspockets.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ThemeUseCasesTest {
    private val repository = FakeThemeRepository()

    @Test
    fun observesSavedThemeAndPassesThemeChangesToRepository() = runTest {
        val observe = ObserveThemeUseCase(repository)
        val set = SetThemeUseCase(repository)
        assertEquals(AppTheme.LIGHT, observe().first())
        set(AppTheme.DARK)
        assertEquals(AppTheme.DARK, observe().first())
        set(AppTheme.LIGHT)
        assertEquals(AppTheme.LIGHT, observe().first())
    }

    @Test
    fun propagatesStorageFailureWithoutChangingTheme() = runTest {
        repository.fail = true
        assertFailsWith<IllegalStateException> { SetThemeUseCase(repository)(AppTheme.DARK) }
        assertEquals(AppTheme.LIGHT, ObserveThemeUseCase(repository)().first())
    }

    private class FakeThemeRepository : ThemeRepository {
        private val theme = MutableStateFlow(AppTheme.LIGHT)
        var fail = false
        override fun observeTheme(): Flow<AppTheme> = theme
        override suspend fun setTheme(theme: AppTheme) {
            check(!fail)
            this.theme.value = theme
        }
    }
}
