package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.LinkDraft
import it.linkspockets.domain.model.LinkValidationError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidateLinkUseCaseTest {
    private val validateLink = ValidateLinkUseCase()
    private val valid = LinkDraft("Kotlin", "https://kotlinlang.org", "Web", "Sviluppo")

    @Test
    fun acceptsHttpAndHttps() {
        assertNull(validateLink(valid))
        assertNull(validateLink(valid.copy(url = "http://example.com/path?q=kmp#docs")))
        assertNull(validateLink(valid.copy(url = " https://example.com ")))
    }

    @Test
    fun rejectsMissingRequiredFields() {
        assertEquals(LinkValidationError.EMPTY_NAME, validateLink(valid.copy(nome = " ")))
        assertEquals(LinkValidationError.EMPTY_CATEGORY, validateLink(valid.copy(categoria = " ")))
    }

    @Test
    fun acceptsMissingChannel() {
        assertNull(validateLink(valid.copy(canale = "")))
        assertNull(validateLink(valid.copy(canale = "   ")))
    }

    @Test
    fun rejectsInvalidUrls() {
        listOf("", "example.com", "https://", "https:///path", "https://exa mple.com", "javascript:alert(1)").forEach {
            assertEquals(LinkValidationError.INVALID_URL, validateLink(valid.copy(url = it)), it)
        }
    }
}
