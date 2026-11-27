package it.linkspockets.presentation.channel

import it.linkspockets.domain.model.ChannelDraft

data class ChannelState(val form: ChannelFormState? = null)

data class ChannelFormState(
    val draft: ChannelDraft = ChannelDraft(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val forLink: Boolean = false,
)

sealed interface ChannelIntent {
    data object ShowForm : ChannelIntent
    data class ShowFormForLink(val linkUrl: String) : ChannelIntent
    data object DismissForm : ChannelIntent
    data class NameChanged(val value: String) : ChannelIntent
    data class UrlChanged(val value: String) : ChannelIntent
    data class CategoryChanged(val value: String) : ChannelIntent
    data object Save : ChannelIntent
}

sealed interface ChannelEffect {
    data class Saved(val name: String, val forLink: Boolean) : ChannelEffect
}
