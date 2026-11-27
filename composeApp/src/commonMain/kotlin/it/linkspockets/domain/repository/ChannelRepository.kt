package it.linkspockets.domain.repository

import it.linkspockets.domain.model.ChannelDraft
import it.linkspockets.domain.model.ChannelEntry
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun observeChannels(): Flow<List<ChannelEntry>>

    /** Returns false if a channel with the same name already exists. */
    suspend fun add(draft: ChannelDraft): Boolean
}
