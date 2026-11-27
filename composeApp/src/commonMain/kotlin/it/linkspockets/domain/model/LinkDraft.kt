package it.linkspockets.domain.model

data class LinkDraft(
    val nome: String = "",
    val url: String = "",
    val canale: String = "",
    val categorie: List<String> = emptyList(),
)
