package it.linkspockets.domain.model

data class ChannelDraft(
    val nome: String = "",
    val url: String = "",
    val categoria: String = "",
)

enum class ChannelValidationError { EMPTY_NAME, INVALID_URL }

class InvalidChannelException(val error: ChannelValidationError) : IllegalArgumentException(error.name)

class ChannelAlreadyExistsException : IllegalArgumentException("Channel name already exists")
