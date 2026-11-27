package it.linkspockets.domain.model

data class ChannelEntry(
    val id: Long,
    val nome: String,
    val url: String,
    val categoria: String?,
)
