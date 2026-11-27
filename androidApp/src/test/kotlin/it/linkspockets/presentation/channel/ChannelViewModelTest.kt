package it.linkspockets.presentation.channel

import androidx.lifecycle.ViewModelStore
import it.linkspockets.domain.model.ChannelDraft
import it.linkspockets.domain.model.ChannelEntry
import it.linkspockets.domain.usecase.CreateChannelDraftFromLinkUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import it.linkspockets.domain.repository.ChannelRepository
import it.linkspockets.domain.usecase.AddChannelUseCase
import it.linkspockets.domain.usecase.ValidateChannelUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
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
class ChannelViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeChannelRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun invalidInputsShowErrorWithoutSavingAndEditingClearsError() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.act(ChannelIntent.ShowForm)
        viewModel.act(ChannelIntent.Save)
        assertEquals("Inserisci il nome.", viewModel.state.value.form?.error)
        viewModel.act(ChannelIntent.NameChanged("Kotlin"))
        assertNull(viewModel.state.value.form?.error)
        viewModel.act(ChannelIntent.UrlChanged("example.com"))
        viewModel.act(ChannelIntent.Save)
        assertNotNull(viewModel.state.value.form?.error)
        assertEquals(0, repository.calls)
    }

    @Test
    fun savesWithoutCategoryClosesFormAndEmitsConfirmation() = runTest(dispatcher) {
        val viewModel = createViewModel()
        fillForm(viewModel)
        viewModel.act(ChannelIntent.Save)
        assertNull(viewModel.state.value.form)
        assertEquals(listOf(ChannelDraft("Kotlin", "https://kotlinlang.org")), repository.added)
        assertEquals(ChannelEffect.Saved("Kotlin", forLink = false), viewModel.effect.first())
    }

    @Test
    fun busyFormRejectsDuplicateTapsEditsDismissalAndReset() = runTest(dispatcher) {
        repository.gate = CompletableDeferred()
        val viewModel = createViewModel()
        fillForm(viewModel)
        viewModel.act(ChannelIntent.CategoryChanged(" Dev "))
        viewModel.act(ChannelIntent.Save)
        assertTrue(viewModel.state.value.form?.isSaving == true)
        viewModel.act(ChannelIntent.Save)
        viewModel.act(ChannelIntent.NameChanged("Other"))
        viewModel.act(ChannelIntent.DismissForm)
        viewModel.act(ChannelIntent.ShowForm)
        assertEquals(1, repository.calls)
        assertEquals(" Kotlin ", viewModel.state.value.form?.draft?.nome)
        assertEquals(" Dev ", viewModel.state.value.form?.draft?.categoria)
        assertNotNull(viewModel.state.value.form)
        repository.gate?.complete(Unit)
        runCurrent()
        assertNull(viewModel.state.value.form)
        assertEquals("Dev", repository.added.single().categoria)
    }

    @Test
    fun duplicateNameKeepsInputsAndCanBeCorrected() = runTest(dispatcher) {
        repository.inserted = false
        val viewModel = createViewModel()
        fillForm(viewModel)
        val draft = viewModel.state.value.form?.draft
        viewModel.act(ChannelIntent.Save)
        val form = assertNotNull(viewModel.state.value.form)
        assertEquals(draft, form.draft)
        assertFalse(form.isSaving)
        assertEquals("Esiste già un canale con questo nome.", form.error)
        repository.inserted = true
        viewModel.act(ChannelIntent.NameChanged("Another channel"))
        assertNull(viewModel.state.value.form?.error)
        viewModel.act(ChannelIntent.Save)
        assertNull(viewModel.state.value.form)
    }

    @Test
    fun storageFailurePreservesDraftAndAllowsRetry() = runTest(dispatcher) {
        repository.fail = true
        val viewModel = createViewModel()
        fillForm(viewModel)
        val draft = viewModel.state.value.form?.draft
        viewModel.act(ChannelIntent.Save)
        val form = assertNotNull(viewModel.state.value.form)
        assertEquals(draft, form.draft)
        assertFalse(form.isSaving)
        assertEquals("Impossibile salvare il canale. Riprova.", form.error)
        repository.fail = false
        viewModel.act(ChannelIntent.Save)
        assertNull(viewModel.state.value.form)
        assertEquals(2, repository.calls)
    }

    @Test
    fun dismissAndReopenResetInputsWithoutDatabaseChanges() = runTest(dispatcher) {
        val viewModel = createViewModel()
        fillForm(viewModel)
        viewModel.act(ChannelIntent.DismissForm)
        assertNull(viewModel.state.value.form)
        viewModel.act(ChannelIntent.ShowForm)
        assertEquals(ChannelDraft(), viewModel.state.value.form?.draft)
        assertEquals(0, repository.calls)
    }

    @Test
    fun clearingViewModelCancelsSaveWithoutReportingAnErrorOrSuccess() = runTest(dispatcher) {
        repository.gate = CompletableDeferred()
        val viewModel = createViewModel()
        fillForm(viewModel)
        viewModel.act(ChannelIntent.Save)
        store.clear()
        runCurrent()
        repository.gate?.complete(Unit)
        runCurrent()
        assertTrue(repository.added.isEmpty())
        assertNull(viewModel.state.value.form?.error)
        assertTrue(viewModel.effect.toList().isEmpty())
    }

    @Test
    fun channelCreatedFromLinkIsPrefilledEditableAndReportsSelection() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.act(ChannelIntent.ShowFormForLink("https://www.esempio.it/article?q=1#section"))
        val form = assertNotNull(viewModel.state.value.form)
        assertTrue(form.forLink)
        assertEquals(ChannelDraft("www.esempio.it", "https://www.esempio.it"), form.draft)
        viewModel.act(ChannelIntent.NameChanged(" Example "))
        viewModel.act(ChannelIntent.UrlChanged("https://example.com"))
        // Reopening must not overwrite edits in the current form.
        viewModel.act(ChannelIntent.ShowFormForLink("https://other.example/path"))
        assertEquals(" Example ", viewModel.state.value.form?.draft?.nome)
        viewModel.act(ChannelIntent.Save)
        assertNull(viewModel.state.value.form)
        assertEquals(listOf(ChannelDraft("Example", "https://example.com")), repository.added)
        assertEquals(ChannelEffect.Saved("Example", forLink = true), viewModel.effect.first())
    }

    @Test
    fun missingLinkUrlOpensEmptyFormAndCancellationDoesNotSave() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.act(ChannelIntent.ShowFormForLink(""))
        assertEquals(ChannelDraft(), viewModel.state.value.form?.draft)
        viewModel.act(ChannelIntent.DismissForm)
        assertNull(viewModel.state.value.form)
        assertEquals(0, repository.calls)
        viewModel.act(ChannelIntent.ShowForm)
        assertFalse(assertNotNull(viewModel.state.value.form).forLink)
    }

    @Test
    fun duplicateSuggestedNameCanBeCorrectedWithoutLosingLinkOrigin() = runTest(dispatcher) {
        repository.inserted = false
        val viewModel = createViewModel()
        viewModel.act(ChannelIntent.ShowFormForLink("https://example.com/path"))
        viewModel.act(ChannelIntent.Save)
        val form = assertNotNull(viewModel.state.value.form)
        assertEquals(ChannelDraft("example.com", "https://example.com"), form.draft)
        assertNotNull(form.error)
        assertTrue(form.forLink)
        repository.inserted = true
        viewModel.act(ChannelIntent.NameChanged("Example"))
        viewModel.act(ChannelIntent.Save)
        assertEquals(ChannelEffect.Saved("Example", forLink = true), viewModel.effect.first())
    }

    private fun ChannelViewModel.act(intent: ChannelIntent) {
        dispatch(intent)
        dispatcher.scheduler.runCurrent()
    }

    private fun createViewModel(): ChannelViewModel {
        val validate = ValidateChannelUseCase()
        return ChannelViewModel(AddChannelUseCase(repository, validate), validate, CreateChannelDraftFromLinkUseCase()).also {
            store.put("channel", it)
        }
    }

    private fun fillForm(viewModel: ChannelViewModel) {
        viewModel.act(ChannelIntent.ShowForm)
        viewModel.act(ChannelIntent.NameChanged(" Kotlin "))
        viewModel.act(ChannelIntent.UrlChanged(" https://kotlinlang.org "))
    }

    private class FakeChannelRepository : ChannelRepository {
        override fun observeChannels(): Flow<List<ChannelEntry>> = flowOf(emptyList())
        val added = mutableListOf<ChannelDraft>()
        var calls = 0
        var inserted = true
        var fail = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun add(draft: ChannelDraft): Boolean {
            calls++
            gate?.await()
            if (fail) error("Storage failure")
            if (inserted) added += draft
            return inserted
        }
    }
}
