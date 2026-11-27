package it.linkspockets.domain.model

enum class LinkValidationError {
    EMPTY_NAME,
    INVALID_URL,
    EMPTY_CATEGORY,
}

class InvalidLinkException(val error: LinkValidationError) : IllegalArgumentException(error.name)
