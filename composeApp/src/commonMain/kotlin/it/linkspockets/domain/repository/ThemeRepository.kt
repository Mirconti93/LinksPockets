package it.linkspockets.domain.repository

import it.linkspockets.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun observeTheme(): Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
}
