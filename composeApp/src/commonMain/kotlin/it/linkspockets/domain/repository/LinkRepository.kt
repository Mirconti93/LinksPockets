package it.linkspockets.domain.repository

import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.model.LinkEntry
import kotlinx.coroutines.flow.Flow

interface LinkRepository {
    fun observeLinks(): Flow<List<LinkEntry>>
    suspend fun add(draft: LinkDraft)
    suspend fun delete(id: Long)
}
