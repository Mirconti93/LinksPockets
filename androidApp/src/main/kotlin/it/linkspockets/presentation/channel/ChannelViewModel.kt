package it.linkspockets.presentation.channel

import androidx.lifecycle.viewModelScope
import it.linkspockets.domain.model.ChannelAlreadyExistsException
import it.linkspockets.domain.model.ChannelDraft
import it.linkspockets.domain.model.ChannelValidationError
import it.linkspockets.domain.model.InvalidChannelException
import it.linkspockets.domain.usecase.CreateChannelDraftFromLinkUseCase
import it.linkspockets.domain.usecase.AddChannelUseCase
import it.linkspockets.domain.usecase.ValidateChannelUseCase
import it.linkspockets.presentation.BaseEffectViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ChannelViewModel(
    private val addChannel: AddChannelUseCase,
    private val validateChannel: ValidateChannelUseCase,
    private val createDraftFromLink: CreateChannelDraftFromLinkUseCase,
) : BaseEffectViewModel<ChannelIntent, ChannelState, ChannelEffect>(ChannelState()) {
    override suspend fun onAction(intent: ChannelIntent) {
        when (intent) {
            ChannelIntent.ShowForm -> updateState {
                if (it.form != null) it else it.copy(form = ChannelFormState())
            }
            is ChannelIntent.ShowFormForLink -> {
                val draft = createDraftFromLink(intent.linkUrl)
                updateState {
                    if (it.form != null) it else it.copy(form = ChannelFormState(draft = draft, forLink = true))
                }
            }
            ChannelIntent.DismissForm -> updateState {
                if (it.form?.isSaving == true) it else it.copy(form = null)
            }
            is ChannelIntent.NameChanged -> updateDraft { copy(nome = intent.value) }
            is ChannelIntent.UrlChanged -> updateDraft { copy(url = intent.value) }
            is ChannelIntent.CategoryChanged -> updateDraft { copy(categoria = intent.value) }
            ChannelIntent.Save -> save()
        }
    }

    private fun updateDraft(transform: ChannelDraft.() -> ChannelDraft) {
        updateState { current ->
            val form = current.form
            if (form == null || form.isSaving) current
            else current.copy(form = form.copy(draft = form.draft.transform(), error = null))
        }
    }

    private fun save() {
        val form = state.value.form ?: return
        if (form.isSaving) return
        val error = validateChannel(form.draft)
        if (error != null) {
            updateState { it.copy(form = form.copy(error = error.toMessage())) }
            return
        }
        updateState { it.copy(form = form.copy(isSaving = true, error = null)) }
        viewModelScope.launch {
            try {
                val saved = addChannel(form.draft)
                updateState { it.copy(form = null) }
                emitEffect(ChannelEffect.Saved(saved.nome, form.forLink))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                val message = when (error) {
                    is ChannelAlreadyExistsException -> "Esiste già un canale con questo nome."
                    is InvalidChannelException -> error.error.toMessage()
                    else -> "Impossibile salvare il canale. Riprova."
                }
                updateState { it.copy(form = form.copy(isSaving = false, error = message)) }
            }
        }
    }
}

private fun ChannelValidationError.toMessage(): String = when (this) {
    ChannelValidationError.EMPTY_NAME -> "Inserisci il nome."
    ChannelValidationError.INVALID_URL -> "Inserisci un URL valido con http:// o https://."
}
