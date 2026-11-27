package it.linkspockets.domain.model

data class LinkEntry(
    val id: Long,
    val nome: String,
    val url: String,
    val canale: String,
    val categorie: List<String>,
)
