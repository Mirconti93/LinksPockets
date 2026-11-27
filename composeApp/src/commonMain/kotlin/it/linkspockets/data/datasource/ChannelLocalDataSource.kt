package it.linkspockets.data.datasource

import it.linkspockets.domain.model.ChannelDraft
import it.linkspockets.database.Canale
import kotlinx.coroutines.flow.Flow

interface ChannelLocalDataSource {
    fun observeChannels(): Flow<List<Canale>>
    suspend fun insert(draft: ChannelDraft): Boolean
}
