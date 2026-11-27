package it.linkspockets.presentation

import it.linkspockets.domain.model.LinkValidationError

internal fun LinkValidationError.toMessage(): String = when (this) {
    LinkValidationError.EMPTY_NAME -> "Inserisci il nome."
    LinkValidationError.INVALID_URL -> "Inserisci un URL valido con http:// o https://."
    LinkValidationError.EMPTY_CATEGORY -> "Inserisci almeno una categoria."
}
