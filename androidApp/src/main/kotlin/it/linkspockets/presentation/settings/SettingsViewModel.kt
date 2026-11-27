package it.linkspockets.presentation.settings

import androidx.lifecycle.viewModelScope
import it.linkspockets.domain.model.AppTheme
import it.linkspockets.domain.usecase.ObserveThemeUseCase
import it.linkspockets.domain.usecase.SetThemeUseCase
import it.linkspockets.presentation.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val observeTheme: ObserveThemeUseCase,
    private val setTheme: SetThemeUseCase,
) : BaseViewModel<SettingsIntent, SettingsState>(SettingsState()) {
    private var observationJob: Job? = null

    init {
        loadTheme()
    }

    override suspend fun onAction(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Open -> updateState { it.copy(isOpen = true) }
            SettingsIntent.Close -> updateState { if (it.isSaving) it else it.copy(isOpen = false) }
            SettingsIntent.RetryLoad -> if (!state.value.isSaving) loadTheme()
            is SettingsIntent.ThemeSelected -> saveTheme(intent.theme)
        }
    }

    private fun loadTheme() {
        observationJob?.cancel()
        updateState { it.copy(isLoading = true, hasLoadError = false, error = null) }
        observationJob = viewModelScope.launch {
            try {
                observeTheme().collect { theme ->
                    updateState { it.copy(theme = theme, isLoading = false, hasLoadError = false) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                updateState {
                    it.copy(isLoading = false, hasLoadError = true, error = "Impossibile leggere il tema. Riprova.")
                }
            }
        }
    }

    private fun saveTheme(theme: AppTheme) {
        val current = state.value
        if (current.isLoading || current.isSaving || current.hasLoadError || current.theme == theme) return
        updateState { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                setTheme(theme)
                updateState { it.copy(isSaving = false) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                updateState { it.copy(isSaving = false, error = "Impossibile salvare il tema. Riprova.") }
            }
        }
    }
}
