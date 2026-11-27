package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.AppTheme
import it.linkspockets.domain.repository.ThemeRepository

class SetThemeUseCase(private val repository: ThemeRepository) {
    suspend operator fun invoke(theme: AppTheme) = repository.setTheme(theme)
}
