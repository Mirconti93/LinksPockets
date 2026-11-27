package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.ChannelDraft
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateChannelDraftFromLinkUseCaseTest {
    private val create = CreateChannelDraftFromLinkUseCase()

    @Test
    fun extractsDomainAndOriginWithoutPathQueryOrFragment() {
        assertEquals(
            ChannelDraft("www.esempio.it", "https://www.esempio.it"),
            create(" https://www.esempio.it/articolo?q=abc#sezione "),
        )
        assertEquals(ChannelDraft("blog.example.com", "http://blog.example.com"), create("HTTP://Blog.Example.COM"))
        assertEquals(ChannelDraft("example.com", "https://example.com"), create("https://example.com?q=x"))
        assertEquals(ChannelDraft("example.com", "https://example.com"), create("https://example.com#fragment"))
    }

    @Test
    fun excludesCredentialsAndKeepsExplicitPortOnlyInUrl() {
        assertEquals(
            ChannelDraft("example.com", "https://example.com:8443"),
            create("https://user:password@example.com:8443/path"),
        )
    }

    @Test
    fun supportsLocalHostsAndIpv6() {
        assertEquals(ChannelDraft("localhost", "http://localhost:8080"), create("http://localhost:8080/path"))
        assertEquals(ChannelDraft("[::1]", "http://[::1]:8080"), create("http://[::1]:8080/path"))
    }

    @Test
    fun leavesFieldsEmptyForMissingOrInvalidUrls() {
        listOf("", "   ", "example.com", "https://", "https:///path", "https://exa mple.com", "ftp://example.com",
            "https://example.com:bad/path", "https://example.com:99999", "https://example.com:0").forEach {
            assertEquals(ChannelDraft(), create(it), it)
        }
    }
}
