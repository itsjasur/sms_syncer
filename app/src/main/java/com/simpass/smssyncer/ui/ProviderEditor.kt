package com.simpass.smssyncer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simpass.smssyncer.R
import com.simpass.smssyncer.data.*
import java.util.UUID

private val ReceiversSaver = listSaver<List<PhoneEntry>, String>(
    save = { entries -> entries.flatMap { listOf(it.id, it.number, it.canonical, it.label) } },
    restore = { values -> values.chunked(4).map { PhoneEntry(it[0], it[1], it[2], it[3]) } },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditor(provider: Provider?, config: ForwardingConfig, onClose: () -> Unit,
    onSave: (Provider) -> String?, onDelete: () -> String?) {
    val resources = LocalResources.current
    val initialNumber = provider?.let { NumberRules.domesticNumber(it.canonical) } ?: ""
    val providerId = rememberSaveable(provider?.id) { provider?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable(providerId) { mutableStateOf(provider?.name ?: "") }
    var number by rememberSaveable(providerId) { mutableStateOf(initialNumber) }
    var receivers by rememberSaveable(providerId, stateSaver = ReceiversSaver) { mutableStateOf(provider?.receivers ?: emptyList()) }
    var error by rememberSaveable(providerId) { mutableStateOf<String?>(null) }
    var receiverEditorOpen by rememberSaveable(providerId) { mutableStateOf(false) }
    var receiverEditingId by rememberSaveable(providerId) { mutableStateOf<String?>(null) }
    var discardDialog by rememberSaveable(providerId) { mutableStateOf(false) }
    var deleteDialog by rememberSaveable(providerId) { mutableStateOf(false) }
    val canonical = NumberRules.canonicalize(number, true)
    val dirty = name != (provider?.name ?: "") || number != initialNumber || receivers != (provider?.receivers ?: emptyList<PhoneEntry>())
    fun close() { if (dirty) discardDialog = true else onClose() }
    BackHandler { close() }

    Scaffold(modifier = Modifier.imePadding(), topBar = {
        TopAppBar(title = { Text(stringResource(if (provider == null) R.string.add_provider else R.string.edit_provider)) },
            navigationIcon = { IconButton(onClick = { close() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }, bottomBar = {
        Surface {
            Button(onClick = {
                error = when {
                    name.isBlank() -> resources.getString(R.string.provider_name_required)
                    canonical == null -> resources.getString(R.string.invalid_watched_number)
                    else -> onSave(Provider(providerId, name.trim(), NumberRules.domesticNumber(canonical), canonical, receivers.toList()))
                }
            }, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(stringResource(R.string.save_provider))
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.provider_info), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(name, onValueChange = { name = it.take(60); error = null }, label = { Text(stringResource(R.string.provider_name)) },
                        placeholder = { Text(stringResource(R.string.watched_label_example)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(number, onValueChange = { number = it; error = null }, label = { Text(stringResource(R.string.provider_phone)) },
                        placeholder = { Text(stringResource(R.string.phone_number_example)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true, modifier = Modifier.fillMaxWidth(), supportingText = {
                            Text(canonical?.let { stringResource(R.string.normalized_number, NumberRules.displayNumber(it)) } ?: stringResource(R.string.watched_hint))
                        })
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.receivers_count, receivers.size), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.provider_receivers_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (receivers.isEmpty()) item {
                Text(stringResource(R.string.receivers_empty_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(receivers, key = { it.id }) { receiver ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(receiver.label.ifBlank { receiver.displayNumber }, fontWeight = FontWeight.Medium)
                            if (receiver.label.isNotBlank()) Text(receiver.displayNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { receiverEditingId = receiver.id; receiverEditorOpen = true }) {
                            Icon(Icons.Default.Edit, stringResource(R.string.edit_number_accessibility, receiver.displayNumber), Modifier.size(20.dp))
                        }
                        IconButton(onClick = { receivers = receivers.filterNot { it.id == receiver.id }; error = null }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.remove_number_accessibility, receiver.displayNumber), Modifier.size(20.dp))
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { receiverEditingId = null; receiverEditorOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_receiver))
                }
            }
            item { Text(stringResource(R.string.editor_save_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (provider != null) item { TextButton(onClick = { deleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.delete_provider), color = MaterialTheme.colorScheme.error)
            } }
        }
    }

    if (receiverEditorOpen) ReceiverEditor(receivers.find { it.id == receiverEditingId }, onDismiss = { receiverEditorOpen = false }, onSave = { receiver ->
        val providerNumbers = config.providers.filterNot { it.id == providerId }.map { it.canonical }.toSet() + listOfNotNull(canonical)
        when {
            receivers.any { it.id != receiver.id && it.canonical == receiver.canonical } -> resources.getString(R.string.duplicate_number)
            receiver.canonical in providerNumbers -> resources.getString(R.string.overlapping_number)
            receiverEditingId == null && receivers.size >= 20 -> resources.getString(R.string.recipient_limit)
            else -> {
                receivers = if (receivers.any { it.id == receiver.id }) receivers.map { if (it.id == receiver.id) receiver else it } else receivers + receiver
                error = null
                receiverEditorOpen = false
                null
            }
        }
    })
    if (discardDialog) AlertDialog(onDismissRequest = { discardDialog = false }, title = { Text(stringResource(R.string.discard_title)) },
        text = { Text(stringResource(R.string.discard_message)) }, confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton(onClick = { discardDialog = false }) { Text(stringResource(R.string.cancel)) } })
    if (deleteDialog) AlertDialog(onDismissRequest = { deleteDialog = false }, title = { Text(stringResource(R.string.delete_provider_title)) },
        text = { Text(stringResource(R.string.delete_provider_message)) },
        confirmButton = { TextButton(onClick = { error = onDelete(); deleteDialog = false }) { Text(stringResource(R.string.remove)) } },
        dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun ReceiverEditor(entry: PhoneEntry?, onDismiss: () -> Unit, onSave: (PhoneEntry) -> String?) {
    val resources = LocalResources.current
    var number by rememberSaveable(entry?.id) { mutableStateOf(entry?.let { NumberRules.domesticNumber(it.canonical) } ?: "") }
    var label by rememberSaveable(entry?.id) { mutableStateOf(entry?.label ?: "") }
    var error by rememberSaveable(entry?.id) { mutableStateOf<String?>(null) }
    val canonical = NumberRules.canonicalize(number, false)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(if (entry == null) R.string.add_receiver else R.string.edit_receiver)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(number, onValueChange = { number = it; error = null }, label = { Text(stringResource(R.string.receiver_phone)) },
                    placeholder = { Text(stringResource(R.string.phone_number_example)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true, isError = error != null, modifier = Modifier.fillMaxWidth(), supportingText = {
                        Text(error ?: canonical?.let { stringResource(R.string.normalized_number, NumberRules.displayNumber(it)) } ?: stringResource(R.string.local_number_hint))
                    })
                OutlinedTextField(label, onValueChange = { label = it.take(60) }, label = { Text(stringResource(R.string.label_optional)) },
                    placeholder = { Text(stringResource(R.string.destination_label_example)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }, confirmButton = { Button(onClick = {
            error = if (canonical == null) resources.getString(R.string.invalid_destination_number)
                else onSave(PhoneEntry(entry?.id ?: UUID.randomUUID().toString(), NumberRules.domesticNumber(canonical), canonical, label.trim()))
        }, enabled = number.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
