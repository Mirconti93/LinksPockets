package it.linkspockets.presentation.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddChannelDialog(form: ChannelFormState, onIntent: (ChannelIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(ChannelIntent.DismissForm) },
        title = { Text("Nuovo canale") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = form.draft.nome,
                    onValueChange = { onIntent(ChannelIntent.NameChanged(it)) },
                    label = { Text("Nome") }, singleLine = true, enabled = !form.isSaving,
                )
                OutlinedTextField(
                    value = form.draft.url,
                    onValueChange = { onIntent(ChannelIntent.UrlChanged(it)) },
                    label = { Text("URL (https://…)") }, singleLine = true, enabled = !form.isSaving,
                )
                OutlinedTextField(
                    value = form.draft.categoria,
                    onValueChange = { onIntent(ChannelIntent.CategoryChanged(it)) },
                    label = { Text("Categoria (facoltativa)") }, singleLine = true, enabled = !form.isSaving,
                )
                form.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = !form.isSaving, onClick = { onIntent(ChannelIntent.Save) }) {
                Text(if (form.isSaving) "Salvataggio…" else "Salva")
            }
        },
        dismissButton = {
            TextButton(enabled = !form.isSaving, onClick = { onIntent(ChannelIntent.DismissForm) }) {
                Text("Annulla")
            }
        },
    )
}
