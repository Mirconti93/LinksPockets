package it.linkspockets.presentation.link

import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.model.LinkEntry

data class LinkState(
    val links: List<LinkEntry> = emptyList(),
    val isLoading: Boolean = true,
    val hasLoadError: Boolean = false,
    val form: LinkFormState? = null,
    val pendingDeletion: LinkEntry? = null,
    val isDeleting: Boolean = false,
)

data class LinkFormState(
    val draft: LinkDraft = LinkDraft(),
    val isSaving: Boolean = false,
    val error: String? = null,
)
