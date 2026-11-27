package it.linkspockets.presentation.link

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSelector(state: LinkState, onIntent: (LinkIntent) -> Unit, onCreate: () -> Unit) {
    val form = state.form ?: return
    var expanded by remember { mutableStateOf(false) }
    val enabled = !form.isSaving && !state.isLoadingChannels && !state.hasChannelLoadError
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { expanded = it && enabled },
    ) {
        OutlinedTextField(
            value = form.draft.canale.ifEmpty { "Nessun canale" },
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Canale (facoltativo)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Nessun canale") },
                onClick = {
                    onIntent(LinkIntent.ChannelChanged(""))
                    expanded = false
                },
            )
            state.channels.forEach { channel ->
                DropdownMenuItem(
                    text = { Text(channel.nome) },
                    onClick = {
                        onIntent(LinkIntent.ChannelChanged(channel.nome))
                        expanded = false
                    },
                )
            }
        }
    }
    when {
        state.isLoadingChannels -> Text("Caricamento canali…")
        state.hasChannelLoadError -> {
            Text("Impossibile caricare i canali.", color = MaterialTheme.colorScheme.error)
            TextButton(enabled = !form.isSaving, onClick = { onIntent(LinkIntent.RetryChannels) }) {
                Text("Riprova a caricare i canali")
            }
        }
        state.channels.isEmpty() -> Text("Nessun canale salvato.")
    }
    TextButton(enabled = !form.isSaving, onClick = onCreate) { Text("+ Nuovo canale") }
}
