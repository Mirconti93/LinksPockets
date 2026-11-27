package it.linkspockets.data.repository

import it.linkspockets.data.datasource.ChannelLocalDataSource
import it.linkspockets.domain.model.ChannelDraft
import it.linkspockets.domain.model.ChannelEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import it.linkspockets.domain.repository.ChannelRepository

class ChannelRepositoryImpl(private val dataSource: ChannelLocalDataSource) : ChannelRepository {
    override fun observeChannels(): Flow<List<ChannelEntry>> = dataSource.observeChannels().map { channels ->
        channels.map { ChannelEntry(it.id, it.nome, it.url, it.categoria) }
    }

    override suspend fun add(draft: ChannelDraft): Boolean = dataSource.insert(draft)
}
