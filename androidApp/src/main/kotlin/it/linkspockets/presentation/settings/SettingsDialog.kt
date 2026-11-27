package it.linkspockets.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import it.linkspockets.domain.model.AppTheme

@Composable
fun SettingsDialog(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(SettingsIntent.Close) },
        title = { Text("Impostazioni") },
        text = {
            Column {
                Text("Tema", style = MaterialTheme.typography.titleMedium)
                Column(Modifier.selectableGroup()) {
                    AppTheme.entries.forEach { theme ->
                        val selected = state.theme == theme
                        val enabled = !state.isLoading && !state.isSaving && !state.hasLoadError
                        Row(
                            modifier = Modifier.fillMaxWidth().selectable(
                                selected = selected,
                                enabled = enabled,
                                role = Role.RadioButton,
                                onClick = { onIntent(SettingsIntent.ThemeSelected(theme)) },
                            ).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, enabled = enabled, onClick = null)
                            Text(
                                text = if (theme == AppTheme.LIGHT) "Chiaro (Light mode)" else "Scuro (Dark mode)",
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
                if (state.isLoading) Text("Caricamento…")
                if (state.isSaving) Text("Salvataggio…")
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.hasLoadError) {
                    TextButton(onClick = { onIntent(SettingsIntent.RetryLoad) }) { Text("Riprova") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !state.isSaving, onClick = { onIntent(SettingsIntent.Close) }) { Text("Chiudi") }
        },
    )
}
