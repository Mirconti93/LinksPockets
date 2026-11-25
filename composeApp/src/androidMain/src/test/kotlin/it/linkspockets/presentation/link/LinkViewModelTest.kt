package it.linkspockets.presentation.link

import androidx.lifecycle.ViewModelStore
import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.model.LinkEntry
import it.linkspockets.domain.repository.LinkRepository
import it.linkspockets.domain.usecase.AddLinkUseCase
import it.linkspockets.domain.usecase.DeleteLinkUseCase
import it.linkspockets.domain.usecase.ObserveLinksUseCase
import it.linkspockets.domain.usecase.ValidateLinkUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
class LinkViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeLinkRepository()
    private val link = LinkEntry(1, "Kotlin", "https://kotlinlang.org", "Web", "Dev")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun loadsAndObservesLinks() = runTest(dispatcher) {
        val viewModel = createViewModel()
        assertTrue(viewModel.state.value.isLoading)
        runCurrent()
        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.links.isEmpty())
        repository.links.value = listOf(link)
        runCurrent()
        assertEquals(listOf(link), viewModel.state.value.links)
    }

    @Test
    fun loadFailureCanBeRetriedWithoutDuplicateObservers() = runTest(dispatcher) {
        repository.failLoad = true
        val viewModel = createViewModel()
        runCurrent()
        assertTrue(viewModel.state.value.hasLoadError)
        assertFalse(viewModel.state.value.isLoading)
        repository.failLoad = false
        repository.links.value = listOf(link)
        viewModel.onIntent(LinkIntent.RetryLoad)
        viewModel.onIntent(LinkIntent.RetryLoad)
        runCurrent()
        assertFalse(viewModel.state.value.hasLoadError)
        assertEquals(listOf(link), viewModel.state.value.links)
        assertEquals(1, repository.links.subscriptionCount.value)
    }

    @Test
    fun invalidFormShowsErrorWithoutSavingAndEditingClearsError() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(LinkIntent.ShowAddForm)
        viewModel.onIntent(LinkIntent.SaveLink)
        assertNotNull(viewModel.state.value.form?.error)
        assertEquals(0, repository.addCalls)
        viewModel.onIntent(LinkIntent.NameChanged("Kotlin"))
        assertNull(viewModel.state.value.form?.error)
        assertEquals("Kotlin", viewModel.state.value.form?.draft?.nome)
    }

    @Test
    fun saveRejectsDuplicateTapsEditsAndDismissalWhileBusy() = runTest(dispatcher) {
        repository.addGate = CompletableDeferred()
        val viewModel = createViewModel()
        fillForm(viewModel)
        viewModel.onIntent(LinkIntent.SaveLink)
        assertTrue(viewModel.state.value.form?.isSaving == true)
        viewModel.onIntent(LinkIntent.SaveLink)
        viewModel.onIntent(LinkIntent.NameChanged("Changed"))
        viewModel.onIntent(LinkIntent.DismissAddForm)
        runCurrent()
        assertEquals(1, repository.addCalls)
        assertEquals(" Kotlin ", viewModel.state.value.form?.draft?.nome)
        repository.addGate!!.complete(Unit)
        runCurrent()
        assertNull(viewModel.state.value.form)
        assertEquals(listOf(LinkDraft("Kotlin", "https://kotlinlang.org", "Web", "Dev")), repository.added)
        assertEquals("Kotlin", viewModel.state.value.links.single().nome)
    }

    @Test
    fun saveFailureKeepsDraftAndAllowsRetry() = runTest(dispatcher) {
        repository.failAdd = true
        val viewModel = createViewModel()
        fillForm(viewModel)
        val draft = viewModel.state.value.form!!.draft
        viewModel.onIntent(LinkIntent.SaveLink)
        runCurrent()
        val form = assertNotNull(viewModel.state.value.form)
        assertEquals(draft, form.draft)
        assertFalse(form.isSaving)
        assertNotNull(form.error)
        repository.failAdd = false
        viewModel.onIntent(LinkIntent.SaveLink)
        runCurrent()
        assertNull(viewModel.state.value.form)
    }

    @Test
    fun deleteRequiresConfirmationAndRejectsDuplicateTapsWhileBusy() = runTest(dispatcher) {
        repository.links.value = listOf(link)
        repository.deleteGate = CompletableDeferred()
        val viewModel = createViewModel()
        viewModel.onIntent(LinkIntent.RequestDeletion(link))
        assertEquals(0, repository.deleteCalls)
        viewModel.onIntent(LinkIntent.ConfirmDeletion)
        viewModel.onIntent(LinkIntent.ConfirmDeletion)
        viewModel.onIntent(LinkIntent.DismissDeletion)
        runCurrent()
        assertTrue(viewModel.state.value.isDeleting)
        assertEquals(link, viewModel.state.value.pendingDeletion)
        assertEquals(1, repository.deleteCalls)
        repository.deleteGate!!.complete(Unit)
        runCurrent()
        assertFalse(viewModel.state.value.isDeleting)
        assertNull(viewModel.state.value.pendingDeletion)
        assertTrue(viewModel.state.value.links.isEmpty())
        assertEquals(listOf(link.id), repository.deleted)
    }

    @Test
    fun deleteFailureKeepsConfirmationAndEmitsSnackbar() = runTest(dispatcher) {
        repository.failDelete = true
        val viewModel = createViewModel()
        viewModel.onIntent(LinkIntent.RequestDeletion(link))
        viewModel.onIntent(LinkIntent.ConfirmDeletion)
        runCurrent()
        assertFalse(viewModel.state.value.isDeleting)
        assertEquals(link, viewModel.state.value.pendingDeletion)
        assertEquals(
            LinkEffect.ShowSnackbar("Impossibile eliminare il link."),
            viewModel.effect.first(),
        )
    }

    @Test
    fun openUrlIsOneShotAndFailureEmitsSnackbar() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onIntent(LinkIntent.OpenLink(link))
        runCurrent()
        assertEquals(LinkEffect.OpenUrl(link.url), viewModel.effect.first())
        val nextEffect = backgroundScope.async { viewModel.effect.first() }
        runCurrent()
        assertFalse(nextEffect.isCompleted)
        viewModel.onIntent(LinkIntent.OpenLinkFailed)
        runCurrent()
        assertEquals(LinkEffect.ShowSnackbar("Impossibile aprire il link."), nextEffect.await())
    }

    @Test
    fun dismissResetsFormAndCancelsDeletionWithoutDatabaseChanges() = runTest(dispatcher) {
        val viewModel = createViewModel()
        fillForm(viewModel)
        viewModel.onIntent(LinkIntent.DismissAddForm)
        assertNull(viewModel.state.value.form)
        viewModel.onIntent(LinkIntent.ShowAddForm)
        assertEquals(LinkDraft(), viewModel.state.value.form?.draft)
        viewModel.onIntent(LinkIntent.DismissAddForm)
        viewModel.onIntent(LinkIntent.RequestDeletion(link))
        viewModel.onIntent(LinkIntent.DismissDeletion)
        assertNull(viewModel.state.value.pendingDeletion)
        assertEquals(0, repository.addCalls)
        assertEquals(0, repository.deleteCalls)
    }

    @Test
    fun clearingViewModelStopsDatabaseObservation() = runTest(dispatcher) {
        createViewModel()
        runCurrent()
        assertEquals(1, repository.links.subscriptionCount.value)
        store.clear()
        runCurrent()
        assertEquals(0, repository.links.subscriptionCount.value)
    }

    @Test
    fun dispatchProcessesIntentsOnMainAndEffectsCloseWhenCleared() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.dispatch(LinkIntent.ShowAddForm)
        // StandardTestDispatcher queues actions until the virtual UI thread runs.
        assertNull(viewModel.state.value.form)
        runCurrent()
        assertNotNull(viewModel.state.value.form)
        store.clear()
        runCurrent()
        assertTrue(viewModel.effect.toList().isEmpty())
    }

    // Keep test scenarios readable while exercising the inherited dispatch API.
    private fun LinkViewModel.onIntent(intent: LinkIntent) {
        dispatch(intent)
        dispatcher.scheduler.runCurrent()
    }

    private fun createViewModel(): LinkViewModel {
        val validate = ValidateLinkUseCase()
        val viewModel = LinkViewModel(
            ObserveLinksUseCase(repository), AddLinkUseCase(repository, validate),
            DeleteLinkUseCase(repository), validate,
        )
        store.put("links", viewModel)
        return viewModel
    }

    private fun fillForm(viewModel: LinkViewModel) {
        viewModel.onIntent(LinkIntent.ShowAddForm)
        viewModel.onIntent(LinkIntent.NameChanged(" Kotlin "))
        viewModel.onIntent(LinkIntent.UrlChanged(" https://kotlinlang.org "))
        viewModel.onIntent(LinkIntent.ChannelChanged(" Web "))
        viewModel.onIntent(LinkIntent.CategoryChanged(" Dev "))
    }

    private class FakeLinkRepository : LinkRepository {
        val links = MutableStateFlow<List<LinkEntry>>(emptyList())
        var failLoad = false
        var failAdd = false
        var failDelete = false
        var addCalls = 0
        var deleteCalls = 0
        var addGate: CompletableDeferred<Unit>? = null
        var deleteGate: CompletableDeferred<Unit>? = null
        val added = mutableListOf<LinkDraft>()
        val deleted = mutableListOf<Long>()

        override fun observeLinks(): Flow<List<LinkEntry>> = flow {
            if (failLoad) error("Read failed")
            emitAll(links)
        }

        override suspend fun add(draft: LinkDraft) {
            addCalls++
            addGate?.await()
            if (failAdd) error("Insert failed")
            added += draft
            links.value = listOf(LinkEntry(addCalls.toLong(), draft.nome, draft.url, draft.canale, draft.categoria)) + links.value
        }

        override suspend fun delete(id: Long) {
            deleteCalls++
            deleteGate?.await()
            if (failDelete) error("Delete failed")
            deleted += id
            links.value = links.value.filterNot { it.id == id }
        }
    }
}
