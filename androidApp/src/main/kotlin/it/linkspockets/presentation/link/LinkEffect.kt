package it.linkspockets.presentation.link

/** One-shot actions consumed by the route, never stored in the screen state. */
sealed interface LinkEffect {
    data class ShowSnackbar(val message: String) : LinkEffect
    data class OpenUrl(val url: String) : LinkEffect
}
