package it.linkspockets.presentation.link

import it.linkspockets.domain.model.LinkEntry

sealed interface LinkIntent {
    data object RetryLoad : LinkIntent
    data object RetryChannels : LinkIntent
    data object ShowAddForm : LinkIntent
    data object DismissAddForm : LinkIntent
    data class NameChanged(val value: String) : LinkIntent
    data class UrlChanged(val value: String) : LinkIntent
    data class ChannelChanged(val value: String) : LinkIntent
    data class CategoryChanged(val value: String) : LinkIntent
    data object SaveLink : LinkIntent
    data class RequestDeletion(val link: LinkEntry) : LinkIntent
    data object DismissDeletion : LinkIntent
    data object ConfirmDeletion : LinkIntent
    data class OpenLink(val link: LinkEntry) : LinkIntent
    data object OpenLinkFailed : LinkIntent
}
