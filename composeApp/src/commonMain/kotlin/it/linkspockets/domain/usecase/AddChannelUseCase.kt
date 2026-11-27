package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.ChannelAlreadyExistsException
import it.linkspockets.domain.model.ChannelDraft
import it.linkspockets.domain.model.InvalidChannelException
import it.linkspockets.domain.repository.ChannelRepository

class AddChannelUseCase(
    private val repository: ChannelRepository,
    private val validate: ValidateChannelUseCase,
) {
    suspend operator fun invoke(draft: ChannelDraft): ChannelDraft {
        validate(draft)?.let { throw InvalidChannelException(it) }
        val normalized = draft.copy(
            nome = draft.nome.trim(),
            url = draft.url.trim(),
            categoria = draft.categoria.trim(),
        )
        if (!repository.add(normalized)) throw ChannelAlreadyExistsException()
        return normalized
    }
}
