package it.linkspockets.presentation.settings

import it.linkspockets.domain.model.AppTheme

data class SettingsState(
    val theme: AppTheme = AppTheme.LIGHT,
    val isOpen: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasLoadError: Boolean = false,
    val error: String? = null,
)

sealed interface SettingsIntent {
    data object Open : SettingsIntent
    data object Close : SettingsIntent
    data object RetryLoad : SettingsIntent
    data class ThemeSelected(val theme: AppTheme) : SettingsIntent
}
