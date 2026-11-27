package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.AppTheme
import it.linkspockets.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow

class ObserveThemeUseCase(private val repository: ThemeRepository) {
    operator fun invoke(): Flow<AppTheme> = repository.observeTheme()
}
