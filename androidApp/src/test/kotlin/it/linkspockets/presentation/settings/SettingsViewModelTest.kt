package it.linkspockets.presentation.settings

import androidx.lifecycle.ViewModelStore
import it.linkspockets.domain.model.AppTheme
import it.linkspockets.domain.repository.ThemeRepository
import it.linkspockets.domain.usecase.ObserveThemeUseCase
import it.linkspockets.domain.usecase.SetThemeUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeThemeRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun restoresThemeAndClosesSettingsWithoutResettingIt() = runTest(dispatcher) {
        repository.theme.value = AppTheme.DARK
        val viewModel = createViewModel()
        assertTrue(viewModel.state.value.isLoading)
        runCurrent()
        assertEquals(AppTheme.DARK, viewModel.state.value.theme)
        assertFalse(viewModel.state.value.isLoading)
        viewModel.act(SettingsIntent.Open)
        assertTrue(viewModel.state.value.isOpen)
        viewModel.act(SettingsIntent.Close)
        assertFalse(viewModel.state.value.isOpen)
        assertEquals(AppTheme.DARK, viewModel.state.value.theme)
        assertTrue(repository.writes.isEmpty())
    }

    @Test
    fun switchesBetweenLightAndDarkAndIgnoresCurrentTheme() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.act(SettingsIntent.Open)
        viewModel.act(SettingsIntent.ThemeSelected(AppTheme.DARK))
        assertEquals(AppTheme.DARK, viewModel.state.value.theme)
        viewModel.act(SettingsIntent.ThemeSelected(AppTheme.DARK))
        viewModel.act(SettingsIntent.ThemeSelected(AppTheme.LIGHT))
        assertEquals(AppTheme.LIGHT, viewModel.state.value.theme)
        assertEquals(listOf(AppTheme.DARK, AppTheme.LIGHT), repository.writes)
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun busySettingsRejectDuplicateTapsChangesAndDismissal() = runTest(dispatcher) {
        repository.gate = CompletableDeferred()
        val viewModel = createViewModel()
        viewModel.act(SettingsIntent.Open)
        viewModel.act(SettingsIntent.ThemeSelected(AppTheme.DARK))
        assertTrue(viewModel.state.value.isSaving)
        viewModel.act(SettingsIntent.ThemeSelected(AppTheme.DARK))
        viewModel.act(SettingsIntent.ThemeSelected(AppTheme.LIGHT))
        viewModel.act(SettingsIntent.Close)
        assertTrue(viewModel.state.value.isOpen)
        assertEquals(1, repository.calls)
        repository.gate?.complete(Unit)
        runCurrent()
        assertEquals(AppTheme.DARK, viewModel.state.value.theme)
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun failedSaveKeepsThemeAndAllowsRetry() = runTest(dispatcher) {
        repository.failSave = true
        val viewModel = createViewModel()
        viewModel.act(SettingsIntent.Open)
        viewModel.act(SettingsIntent.ThemeSelected(AppTheme.DARK))
        assertEquals(AppTheme.LIGHT, viewModel.state.value.theme)
        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSaving)
        repository.failSave = false
        viewModel.act(SettingsIntent.ThemeSelected(AppTheme.DARK))
        assertEquals(AppTheme.DARK, viewModel.state.value.theme)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun failedLoadCanBeRetriedWithoutDuplicateObservers() = runTest(dispatcher) {
        repository.failLoad = true
        val viewModel = createViewModel()
        viewModel.act(SettingsIntent.Open)
        assertTrue(viewModel.state.value.hasLoadError)
        viewModel.act(SettingsIntent.ThemeSelected(AppTheme.DARK))
        assertEquals(0, repository.calls)
        repository.failLoad = false
        viewModel.act(SettingsIntent.RetryLoad)
        viewModel.act(SettingsIntent.RetryLoad)
        assertFalse(viewModel.state.value.hasLoadError)
        assertNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.isOpen)
        assertEquals(1, repository.theme.subscriptionCount.value)
    }

    @Test
    fun clearingViewModelCancelsSaveAndThemeObservation() = runTest(dispatcher) {
        repository.gate = CompletableDeferred()
        val viewModel = createViewModel()
        viewModel.act(SettingsIntent.Open)
        viewModel.act(SettingsIntent.ThemeSelected(AppTheme.DARK))
        assertEquals(1, repository.theme.subscriptionCount.value)
        store.clear()
        runCurrent()
        repository.gate?.complete(Unit)
        runCurrent()
        assertEquals(0, repository.theme.subscriptionCount.value)
        assertTrue(repository.writes.isEmpty())
        assertNull(viewModel.state.value.error)
    }

    private fun SettingsViewModel.act(intent: SettingsIntent) {
        dispatch(intent)
        dispatcher.scheduler.runCurrent()
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(ObserveThemeUseCase(repository), SetThemeUseCase(repository)).also {
            store.put("settings", it)
        }

    private class FakeThemeRepository : ThemeRepository {
        val theme = MutableStateFlow(AppTheme.LIGHT)
        val writes = mutableListOf<AppTheme>()
        var gate: CompletableDeferred<Unit>? = null
        var failSave = false
        var failLoad = false
        var calls = 0
        override fun observeTheme(): Flow<AppTheme> = flow {
            if (failLoad) error("Read failure")
            emitAll(theme)
        }
        override suspend fun setTheme(theme: AppTheme) {
            calls++
            gate?.await()
            check(!failSave)
            writes += theme
            this.theme.value = theme
        }
    }
}
