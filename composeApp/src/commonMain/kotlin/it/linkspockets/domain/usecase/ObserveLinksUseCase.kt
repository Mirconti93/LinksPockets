package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.LinkEntry
import it.linkspockets.domain.repository.LinkRepository
import kotlinx.coroutines.flow.Flow

class ObserveLinksUseCase(private val repository: LinkRepository) {
    operator fun invoke(): Flow<List<LinkEntry>> = repository.observeLinks()
}
