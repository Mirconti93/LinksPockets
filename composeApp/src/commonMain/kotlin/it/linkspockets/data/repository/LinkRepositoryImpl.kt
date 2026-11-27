package it.linkspockets.data.repository

import it.linkspockets.data.datasource.LinkLocalDataSource
import it.linkspockets.data.mapper.toDomain
import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.model.LinkEntry
import it.linkspockets.domain.repository.LinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LinkRepositoryImpl(private val localDataSource: LinkLocalDataSource) : LinkRepository {
    override fun observeLinks(): Flow<List<LinkEntry>> =
        localDataSource.observeLinks().map { rows ->
            rows.groupBy { it.id }.values.map { linkRows ->
                linkRows.first().toDomain(linkRows.mapNotNull { it.categoria })
            }
        }

    override suspend fun add(draft: LinkDraft) = localDataSource.insert(draft)

    override suspend fun delete(id: Long) = localDataSource.delete(id)
}
