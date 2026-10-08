package com.simpass.smssyncer.ui

import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simpass.smssyncer.R
import com.simpass.smssyncer.data.*
import com.simpass.smssyncer.sms.SmsAccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSyncerApp(store: ForwardingStore, permissionsGranted: Boolean, refreshPermissions: () -> Unit, openSettings: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var config by remember { mutableStateOf(store.config()) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingEnable by rememberSaveable { mutableStateOf(false) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    DisposableEffect(store) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == ForwardingStore.CONFIG) config = store.config()
        }
        store.preferences.registerOnSharedPreferenceChangeListener(listener)
        config = store.config()
        onDispose { store.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    LaunchedEffect(notice) { notice?.let { snackbar.showSnackbar(it); notice = null } }
    fun update(action: () -> Unit) {
        try { action() } catch (_: Exception) { notice = resources.getString(R.string.save_changes_failed) }
    }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshPermissions()
        val granted = SmsAccess.hasPermissions(context)
        permissionDenied = !granted
        if (pendingEnable && granted) update { store.setEnabled(true) }
        pendingEnable = false
        if (!granted) notice = resources.getString(R.string.sms_access_denied)
    }

    if (editorOpen) {
        ProviderEditor(provider = config.providers.find { it.id == editingId }, config = config,
            onClose = { editorOpen = false }, onSave = { provider ->
                try {
                    store.saveProvider(provider)?.let { resources.getString(it.messageRes) }
                        .also { if (it == null) editorOpen = false }
                } catch (_: Exception) { resources.getString(R.string.save_changes_failed) }
            }, onDelete = {
                try {
                    editingId?.let(store::removeProvider)
                    editorOpen = false
                    null
                } catch (_: Exception) { resources.getString(R.string.save_changes_failed) }
            })
    } else {
        Scaffold(topBar = {
            TopAppBar(title = {
                Column {
                    Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            })
        }, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
            Surface {
                Button(onClick = { editingId = null; editorOpen = true },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_provider))
                }
            }
        }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    ForwardingCard(config, permissionsGranted, permissionDenied,
                        onToggle = { enable ->
                            if (!enable) update { store.setEnabled(false) }
                            else if (permissionsGranted) update { store.setEnabled(true) }
                            else { pendingEnable = true; permissionRequest.launch(SmsAccess.permissions) }
                        }, onPermission = {
                            if (permissionDenied) openSettings()
                            else { pendingEnable = false; permissionRequest.launch(SmsAccess.permissions) }
                        })
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.providers_heading), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.providers_description), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (config.providers.isEmpty()) item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.providers_empty_title), fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.providers_empty_description), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                items(config.providers, key = { it.id }) { provider ->
                    ProviderCard(provider) { editingId = provider.id; editorOpen = true }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(provider: Provider, onEdit: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(provider.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(provider.displayNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.edit))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(stringResource(R.string.receivers_count, provider.receivers.size), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            if (provider.receivers.isEmpty()) Text(stringResource(R.string.receivers_empty_short), style = MaterialTheme.typography.bodySmall)
            provider.receivers.forEach { receiver ->
                Text(if (receiver.label.isBlank()) receiver.displayNumber else "${receiver.label} · ${receiver.displayNumber}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ForwardingCard(
    config: ForwardingConfig,
    permissionsGranted: Boolean,
    permissionDenied: Boolean,
    onToggle: (Boolean) -> Unit,
    onPermission: () -> Unit,
) {
    val active = config.enabled && permissionsGranted && config.isReady
    val switchLabel = stringResource(R.string.forwarding_switch)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(when {
                        active -> R.string.forwarding_on
                        config.enabled && !permissionsGranted -> R.string.forwarding_paused
                        else -> R.string.forwarding_off
                    }), fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(when {
                        !config.isReady -> stringResource(R.string.setup_required)
                        !permissionsGranted -> stringResource(R.string.permission_required)
                        active -> stringResource(R.string.forwarding_summary, config.providers.count { it.receivers.isNotEmpty() })
                        else -> stringResource(R.string.ready_to_forward)
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = active, onCheckedChange = onToggle, enabled = config.isReady,
                    modifier = Modifier.semantics { contentDescription = switchLabel })
            }
            if (!permissionsGranted) {
                Text(stringResource(R.string.permission_explanation),
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onPermission, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(if (permissionDenied) R.string.open_settings else R.string.allow_sms_access))
                }
            }
        }
    }
}
