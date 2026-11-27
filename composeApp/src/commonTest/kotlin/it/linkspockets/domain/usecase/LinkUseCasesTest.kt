package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.InvalidLinkException
import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.model.LinkEntry
import it.linkspockets.domain.model.LinkValidationError
import it.linkspockets.domain.repository.LinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LinkUseCasesTest {
    private val repository = FakeLinkRepository()
    private val addLink = AddLinkUseCase(repository, ValidateLinkUseCase())

    @Test
    fun addNormalizesFieldsBeforeCallingRepository() = runTest {
        addLink(LinkDraft(" Kotlin ", " https://kotlinlang.org ", " Web ", listOf(" Dev ", "UI", "Dev", " ")))
        assertEquals(
            listOf(LinkDraft("Kotlin", "https://kotlinlang.org", "Web", listOf("Dev", "UI"))),
            repository.addedDrafts,
        )
    }

    @Test
    fun addAcceptsAndNormalizesBlankChannel() = runTest {
        addLink(LinkDraft("Kotlin", "https://kotlinlang.org", "   ", listOf("Dev")))
        assertEquals(
            listOf(LinkDraft("Kotlin", "https://kotlinlang.org", "", listOf("Dev"))),
            repository.addedDrafts,
        )
    }

    @Test
    fun invalidDraftDoesNotReachRepository() = runTest {
        val failure = assertFailsWith<InvalidLinkException> { addLink(LinkDraft()) }
        assertEquals(LinkValidationError.EMPTY_NAME, failure.error)
        assertTrue(repository.addedDrafts.isEmpty())
    }

    @Test
    fun observeReturnsRepositoryLinksAndUpdates() = runTest {
        val observeLinks = ObserveLinksUseCase(repository)
        val flow = observeLinks()
        assertTrue(flow.first().isEmpty())
        val links = listOf(LinkEntry(1, "Kotlin", "https://kotlinlang.org", "Web", listOf("Dev")))
        repository.links.value = links
        assertEquals(links, flow.first())
    }

    @Test
    fun deletePassesIdToRepository() = runTest {
        DeleteLinkUseCase(repository)(42)
        assertEquals(listOf(42L), repository.deletedIds)
    }

    private class FakeLinkRepository : LinkRepository {
        val links = MutableStateFlow<List<LinkEntry>>(emptyList())
        val addedDrafts = mutableListOf<LinkDraft>()
        val deletedIds = mutableListOf<Long>()

        override fun observeLinks(): Flow<List<LinkEntry>> = links
        override suspend fun add(draft: LinkDraft) {
            addedDrafts += draft
        }
        override suspend fun delete(id: Long) {
            deletedIds += id
        }
    }
}
