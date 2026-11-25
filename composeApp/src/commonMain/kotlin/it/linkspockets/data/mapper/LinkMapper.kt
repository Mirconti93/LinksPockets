package it.linkspockets.data.mapper

import it.linkspockets.database.Link
import it.linkspockets.domain.model.LinkEntry

internal fun Link.toDomain(): LinkEntry = LinkEntry(
    id = id,
    nome = nome,
    url = url,
    canale = canale,
    categoria = categoria,
)
