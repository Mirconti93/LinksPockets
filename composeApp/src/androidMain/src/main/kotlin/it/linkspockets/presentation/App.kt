package it.linkspockets.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import it.linkspockets.presentation.link.LinkRoute
import it.linkspockets.presentation.link.LinkViewModel

@Composable
fun App() {
    MaterialTheme {
        val viewModel: LinkViewModel = koinViewModel()
        LinkRoute(viewModel)
    }
}
