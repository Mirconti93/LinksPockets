package it.linkspockets.domain.usecase

import it.linkspockets.domain.model.ChannelDraft

/** Suggest the host and origin, excluding credentials, path, query and fragment. */
class CreateChannelDraftFromLinkUseCase {
    operator fun invoke(linkUrl: String): ChannelDraft {
        val match = HTTP_URL.matchEntire(linkUrl.trim()) ?: return ChannelDraft()
        val scheme = match.groupValues[1].lowercase()
        val host = match.groupValues[2].lowercase()
        val port = match.groupValues[3]
        if (port.isNotEmpty() && port.toIntOrNull() !in 1..65535) return ChannelDraft()
        val origin = "$scheme://$host" + if (port.isEmpty()) "" else ":$port"
        return ChannelDraft(nome = host, url = origin)
    }

    private companion object {
        val HTTP_URL = Regex(
            "^(https?)://(?:[^\\s/@]+@)?(\\[[0-9a-fA-F:.]+\\]|[^\\s/:?#@]+)(?::([0-9]{1,5}))?(?:[/?#][^\\s]*)?$",
            RegexOption.IGNORE_CASE,
        )
    }
}
