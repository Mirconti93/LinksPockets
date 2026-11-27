package it.linkspockets.data.datasource

import it.linkspockets.database.SelectAll
import it.linkspockets.domain.model.LinkDraft
import kotlinx.coroutines.flow.Flow

interface LinkLocalDataSource {
    fun observeLinks(): Flow<List<SelectAll>>
    suspend fun insert(draft: LinkDraft)
    suspend fun delete(id: Long)
}
