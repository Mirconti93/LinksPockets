package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.ChannelAlreadyExistsException
import it.linkspockets.domain.model.ChannelDraft
import it.linkspockets.domain.model.ChannelEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import it.linkspockets.domain.model.ChannelValidationError
import it.linkspockets.domain.model.InvalidChannelException
import it.linkspockets.domain.repository.ChannelRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChannelUseCasesTest {
    private val validate = ValidateChannelUseCase()
    private val valid = ChannelDraft("Kotlin", "https://kotlinlang.org")
    private val repository = FakeChannelRepository()
    private val add = AddChannelUseCase(repository, validate)

    @Test
    fun acceptsOptionalCategoryAndHttpUrls() {
        assertNull(validate(valid))
        assertNull(validate(valid.copy(url = " http://example.com/path?q=kotlin#docs ", categoria = "   ")))
    }

    @Test
    fun rejectsMissingNamesAndInvalidUrls() {
        assertEquals(ChannelValidationError.EMPTY_NAME, validate(valid.copy(nome = "   ")))
        listOf("", "example.com", "https://", "https:///path", "https://exa mple.com", "javascript:alert(1)").forEach {
            assertEquals(ChannelValidationError.INVALID_URL, validate(valid.copy(url = it)), it)
        }
    }

    @Test
    fun normalizesFieldsBeforeSaving() = runTest {
        val saved = add(ChannelDraft(" Kotlin ", " https://kotlinlang.org ", " Dev "))
        assertEquals(ChannelDraft("Kotlin", "https://kotlinlang.org", "Dev"), saved)
        assertEquals(listOf(saved), repository.added)
    }

    @Test
    fun invalidInputsNeverReachRepository() = runTest {
        val error = assertFailsWith<InvalidChannelException> { add(ChannelDraft()) }
        assertEquals(ChannelValidationError.EMPTY_NAME, error.error)
        assertTrue(repository.added.isEmpty())
    }

    @Test
    fun duplicateNamesProduceDomainError() = runTest {
        repository.inserted = false
        assertFailsWith<ChannelAlreadyExistsException> { add(valid) }
    }

    @Test
    fun observesChannelsAndUpdates() = runTest {
        val observe = ObserveChannelsUseCase(repository)
        val flow = observe()
        assertTrue(flow.first().isEmpty())
        val channels = listOf(ChannelEntry(1, "Kotlin", "https://kotlinlang.org", null))
        repository.channels.value = channels
        assertEquals(channels, flow.first())
    }

    private class FakeChannelRepository : ChannelRepository {
        val channels = MutableStateFlow<List<ChannelEntry>>(emptyList())
        override fun observeChannels(): Flow<List<ChannelEntry>> = channels
        val added = mutableListOf<ChannelDraft>()
        var inserted = true
        override suspend fun add(draft: ChannelDraft): Boolean {
            added += draft
            return inserted
        }
    }
}
