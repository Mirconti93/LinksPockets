package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.ChannelDraft
import it.linkspockets.domain.model.ChannelValidationError

class ValidateChannelUseCase {
    operator fun invoke(draft: ChannelDraft): ChannelValidationError? = when {
        draft.nome.isBlank() -> ChannelValidationError.EMPTY_NAME
        !HTTP_URL.matches(draft.url.trim()) -> ChannelValidationError.INVALID_URL
        else -> null
    }

    private companion object {
        val HTTP_URL = Regex("https?://[^\\s/?#]+(?:[/?#][^\\s]*)?", RegexOption.IGNORE_CASE)
    }
}
