package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.ChannelEntry
import it.linkspockets.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow

class ObserveChannelsUseCase(private val repository: ChannelRepository) {
    operator fun invoke(): Flow<List<ChannelEntry>> = repository.observeChannels()
}
