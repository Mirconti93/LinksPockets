package it.linkspockets.data.mapper

import it.linkspockets.database.SelectAll
import it.linkspockets.domain.model.LinkEntry

internal fun SelectAll.toDomain(categories: List<String>): LinkEntry = LinkEntry(
    id = id,
    nome = nome,
    url = url,
    canale = canale,
    categorie = categories,
)
