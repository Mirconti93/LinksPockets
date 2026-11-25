package it.linkspockets.presentation.link

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun LinkRoute(viewModel: LinkViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner, uriHandler) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is LinkEffect.ShowSnackbar -> snackbar.showSnackbar(effect.message)
                    is LinkEffect.OpenUrl -> {
                        try {
                            uriHandler.openUri(effect.url)
                        } catch (error: Exception) {
                            viewModel.dispatch(LinkIntent.OpenLinkFailed)
                        }
                    }
                }
            }
        }
    }

    LinkScreen(state, { viewModel.dispatch(it) }, snackbar)
}
