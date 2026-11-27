package it.linkspockets.presentation.link

import androidx.lifecycle.viewModelScope
import it.linkspockets.domain.model.InvalidLinkException
import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.usecase.AddLinkUseCase
import it.linkspockets.domain.usecase.DeleteLinkUseCase
import it.linkspockets.domain.usecase.ObserveChannelsUseCase
import it.linkspockets.domain.usecase.ObserveLinksUseCase
import it.linkspockets.domain.usecase.ValidateLinkUseCase
import it.linkspockets.presentation.BaseEffectViewModel
import it.linkspockets.presentation.toMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LinkViewModel(
    private val observeLinksUseCase: ObserveLinksUseCase,
    private val addLinkUseCase: AddLinkUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val validateLinkUseCase: ValidateLinkUseCase,
    private val observeChannelsUseCase: ObserveChannelsUseCase,
) : BaseEffectViewModel<LinkIntent, LinkState, LinkEffect>(LinkState()) {
    private var observationJob: Job? = null
    private var channelObservationJob: Job? = null

    init {
        observeLinks()
        observeChannels()
    }

    override suspend fun onAction(intent: LinkIntent) {
        when (intent) {
            LinkIntent.RetryLoad -> observeLinks()
            LinkIntent.RetryChannels -> observeChannels()
            LinkIntent.ShowAddForm -> updateState {
                if (it.form != null || it.pendingDeletion != null) it
                else it.copy(form = LinkFormState())
            }
            LinkIntent.DismissAddForm -> updateState {
                if (it.form?.isSaving == true) it else it.copy(form = null)
            }
            is LinkIntent.NameChanged -> updateDraft { copy(nome = intent.value) }
            is LinkIntent.UrlChanged -> updateDraft { copy(url = intent.value) }
            is LinkIntent.ChannelChanged -> updateDraft { copy(canale = intent.value) }
            is LinkIntent.CategoryChanged -> updateState { current ->
                val form = current.form
                if (form == null || form.isSaving) current
                else current.copy(form = form.copy(
                    categoriesInput = intent.value,
                    draft = form.draft.copy(categorie = intent.value.split(',')),
                    error = null,
                ))
            }
            LinkIntent.SaveLink -> saveLink()
            is LinkIntent.RequestDeletion -> updateState {
                if (it.form != null || it.pendingDeletion != null) it
                else it.copy(pendingDeletion = intent.link)
            }
            LinkIntent.DismissDeletion -> updateState {
                if (it.isDeleting) it else it.copy(pendingDeletion = null)
            }
            LinkIntent.ConfirmDeletion -> deleteLink()
            is LinkIntent.OpenLink -> emitEffect(LinkEffect.OpenUrl(intent.link.url))
            LinkIntent.OpenLinkFailed -> emitEffect(LinkEffect.ShowSnackbar("Impossibile aprire il link."))
        }
    }

    private fun observeLinks() {
        observationJob?.cancel()
        updateState { it.copy(isLoading = true, hasLoadError = false) }
        observationJob = viewModelScope.launch {
            try {
                observeLinksUseCase().collect { links ->
                    updateState { it.copy(links = links, isLoading = false, hasLoadError = false) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                updateState { it.copy(isLoading = false, hasLoadError = true) }
            }
        }
    }

    private fun observeChannels() {
        channelObservationJob?.cancel()
        updateState { it.copy(isLoadingChannels = true, hasChannelLoadError = false) }
        channelObservationJob = viewModelScope.launch {
            try {
                observeChannelsUseCase().collect { channels ->
                    updateState { it.copy(channels = channels, isLoadingChannels = false, hasChannelLoadError = false) }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                updateState { it.copy(isLoadingChannels = false, hasChannelLoadError = true) }
            }
        }
    }

    private fun updateDraft(transform: LinkDraft.() -> LinkDraft) {
        updateState { current ->
            val form = current.form
            if (form == null || form.isSaving) current
            else current.copy(form = form.copy(draft = form.draft.transform(), error = null))
        }
    }

    private fun saveLink() {
        val form = state.value.form ?: return
        if (form.isSaving) return
        val validationError = validateLinkUseCase(form.draft)
        if (validationError != null) {
            updateState { it.copy(form = form.copy(error = validationError.toMessage())) }
            return
        }
        // Mark busy before launching to reject repeated taps and form edits.
        updateState { it.copy(form = form.copy(isSaving = true, error = null)) }
        viewModelScope.launch {
            try {
                addLinkUseCase(form.draft)
                updateState { it.copy(form = null) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                val message = if (error is InvalidLinkException) error.error.toMessage()
                else "Impossibile salvare il link. Riprova."
                updateState { it.copy(form = form.copy(isSaving = false, error = message)) }
            }
        }
    }

    private fun deleteLink() {
        val current = state.value
        val link = current.pendingDeletion ?: return
        if (current.isDeleting) return
        updateState { it.copy(isDeleting = true) }
        viewModelScope.launch {
            try {
                deleteLinkUseCase(link.id)
                updateState { it.copy(pendingDeletion = null, isDeleting = false) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                updateState { it.copy(isDeleting = false) }
                emitEffect(LinkEffect.ShowSnackbar("Impossibile eliminare il link."))
            }
        }
    }
}
