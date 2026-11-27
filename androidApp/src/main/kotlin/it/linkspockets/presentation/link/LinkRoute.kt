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
import it.linkspockets.presentation.channel.AddChannelDialog
import it.linkspockets.presentation.channel.ChannelEffect
import it.linkspockets.presentation.channel.ChannelIntent
import it.linkspockets.presentation.channel.ChannelViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LinkRoute(viewModel: LinkViewModel, channelViewModel: ChannelViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val channelState by channelViewModel.state.collectAsStateWithLifecycle()
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

    LaunchedEffect(channelViewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            channelViewModel.effect.collectLatest { effect ->
                when (effect) {
                    is ChannelEffect.Saved -> {
                        if (effect.forLink) viewModel.dispatch(LinkIntent.ChannelChanged(effect.name))
                        snackbar.showSnackbar("Canale salvato.")
                    }
                }
            }
        }
    }

    LinkScreen(
        state = state,
        onIntent = { viewModel.dispatch(it) },
        snackbar = snackbar,
        onAddChannel = { channelViewModel.dispatch(ChannelIntent.ShowForm) },
        onAddChannelFromLink = {
            state.form?.takeUnless { it.isSaving }?.let {
                channelViewModel.dispatch(ChannelIntent.ShowFormForLink(it.draft.url))
            }
        },
        isChannelFormOpen = channelState.form != null,
        onOpenSettings = onOpenSettings,
    )
    channelState.form?.let { form ->
        AddChannelDialog(form) { channelViewModel.dispatch(it) }
    }
}
