package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.InvalidLinkException
import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.repository.LinkRepository

class AddLinkUseCase(
    private val repository: LinkRepository,
    private val validateLink: ValidateLinkUseCase,
) {
    suspend operator fun invoke(draft: LinkDraft) {
        validateLink(draft)?.let { throw InvalidLinkException(it) }
        repository.add(
            draft.copy(
                nome = draft.nome.trim(),
                url = draft.url.trim(),
                canale = draft.canale.trim(),
                categoria = draft.categoria.trim(),
            ),
        )
    }
}
