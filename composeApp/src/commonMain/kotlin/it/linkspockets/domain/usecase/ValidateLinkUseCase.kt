package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.model.LinkValidationError

class ValidateLinkUseCase {
    operator fun invoke(draft: LinkDraft): LinkValidationError? = when {
        draft.nome.isBlank() -> LinkValidationError.EMPTY_NAME
        !HTTP_URL.matches(draft.url.trim()) -> LinkValidationError.INVALID_URL
        draft.categorie.none { it.isNotBlank() } -> LinkValidationError.EMPTY_CATEGORY
        else -> null
    }

    private companion object {
        val HTTP_URL = Regex("https?://[^\\s/?#]+(?:[/?#][^\\s]*)?", RegexOption.IGNORE_CASE)
    }
}
