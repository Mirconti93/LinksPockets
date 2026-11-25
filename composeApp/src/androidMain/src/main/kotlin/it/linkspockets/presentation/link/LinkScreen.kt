package it.linkspockets.presentation.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Stateless UI: all screen data comes from LinkState, all actions are intents. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkScreen(state: LinkState, onIntent: (LinkIntent) -> Unit, snackbar: SnackbarHostState) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("LinksPockets") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent(LinkIntent.ShowAddForm) }) {
                Text("+ Link", modifier = Modifier.padding(horizontal = 16.dp))
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.hasLoadError -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Impossibile leggere i link dal database.")
                TextButton(onClick = { onIntent(LinkIntent.RetryLoad) }) { Text("Riprova") }
            }
            state.links.isEmpty() -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Nessun link salvato", style = MaterialTheme.typography.titleLarge)
                Text("Tocca + Link per aggiungere il primo link.")
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.links, key = { it.id }) { link ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(link.nome, style = MaterialTheme.typography.titleMedium)
                            Text(link.url, style = MaterialTheme.typography.bodyMedium)
                            Text("Canale: ${link.canale}")
                            Text("Categoria: ${link.categoria}")
                            Row {
                                TextButton(onClick = { onIntent(LinkIntent.OpenLink(link)) }) { Text("Apri") }
                                TextButton(onClick = { onIntent(LinkIntent.RequestDeletion(link)) }) { Text("Elimina") }
                            }
                        }
                    }
                }
            }
        }
    }

    state.form?.let { AddLinkDialog(it, onIntent) }
    state.pendingDeletion?.let { link ->
        AlertDialog(
            onDismissRequest = { onIntent(LinkIntent.DismissDeletion) },
            title = { Text("Eliminare il link?") },
            text = { Text(link.nome) },
            confirmButton = {
                TextButton(enabled = !state.isDeleting, onClick = { onIntent(LinkIntent.ConfirmDeletion) }) {
                    Text(if (state.isDeleting) "Eliminazione…" else "Elimina")
                }
            },
            dismissButton = {
                TextButton(enabled = !state.isDeleting, onClick = { onIntent(LinkIntent.DismissDeletion) }) {
                    Text("Annulla")
                }
            },
        )
    }
}

@Composable
private fun AddLinkDialog(form: LinkFormState, onIntent: (LinkIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(LinkIntent.DismissAddForm) },
        title = { Text("Nuovo link") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    form.draft.nome, { onIntent(LinkIntent.NameChanged(it)) },
                    label = { Text("Nome") }, singleLine = true, enabled = !form.isSaving,
                )
                OutlinedTextField(
                    form.draft.url, { onIntent(LinkIntent.UrlChanged(it)) },
                    label = { Text("URL (https://…)") }, singleLine = true, enabled = !form.isSaving,
                )
                OutlinedTextField(
                    form.draft.canale, { onIntent(LinkIntent.ChannelChanged(it)) },
                    label = { Text("Canale") }, singleLine = true, enabled = !form.isSaving,
                )
                OutlinedTextField(
                    form.draft.categoria, { onIntent(LinkIntent.CategoryChanged(it)) },
                    label = { Text("Categoria") }, singleLine = true, enabled = !form.isSaving,
                )
                form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !form.isSaving, onClick = { onIntent(LinkIntent.SaveLink) }) {
                Text(if (form.isSaving) "Salvataggio…" else "Salva")
            }
        },
        dismissButton = {
            TextButton(enabled = !form.isSaving, onClick = { onIntent(LinkIntent.DismissAddForm) }) { Text("Annulla") }
        },
    )
}
