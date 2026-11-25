package it.linkspockets.domain.usecase

import it.linkspockets.domain.repository.LinkRepository

class DeleteLinkUseCase(private val repository: LinkRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
