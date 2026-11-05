package app.tsuzuku.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsuzuku.R
import app.tsuzuku.data.BackupSummary
import app.tsuzuku.data.CloudAccountState
import app.tsuzuku.data.CloudOperation
import app.tsuzuku.data.CloudSyncRepository
import app.tsuzuku.data.CloudSyncState
import app.tsuzuku.data.preferences.AppSettings
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun CloudAccountScreen(
    repository: CloudSyncRepository,
    settings: AppSettings,
    onBack: () -> Unit,
    onSetCloudSyncEnabled: (Boolean) -> Unit,
    onDataRestored: suspend () -> Unit,
    initialSetup: Boolean = false,
    onInitialSetupComplete: (String?) -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val account by repository.account.collectAsStateWithLifecycle()
    val syncState by repository.syncState.collectAsStateWithLifecycle()
    var initialAccountHandled by rememberSaveable { mutableStateOf(false) }
    var showSignOutChoice by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialSetup, account) {
        val signedIn = account as? CloudAccountState.SignedIn
        if (initialSetup && signedIn != null && !initialAccountHandled) {
            initialAccountHandled = true
            onInitialSetupComplete(preferredGoogleName(signedIn.displayName))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!initialSetup) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                    }
                }
                Column {
                    Text(stringResource(R.string.google_account_label), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.cloud_account_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        when (val current = account) {
            CloudAccountState.Unavailable -> item { FirebaseSetupCard() }
            CloudAccountState.SignedOut -> item {
                Column {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(Icons.Rounded.AccountCircle, null, modifier = Modifier.size(46.dp))
                        Text(stringResource(R.string.google_not_connected), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.google_sign_in_description),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = {
                                activity?.let {
                                    scope.launch {
                                        repository.signIn(it) ?: return@launch
                                        if (!initialSetup) repository.prepareChoice()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            enabled = syncState !is CloudSyncState.Working && activity != null,
                        ) { Text(stringResource(R.string.sign_in_with_google)) }
                    }
                }
            }
            is CloudAccountState.SignedIn -> {
                item { SignedInAccountCard(current) }
                item {
                    Column {
                        Column(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.automatic_cloud_sync), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        stringResource(R.string.automatic_cloud_sync_description),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Switch(
                                    checked = settings.cloudSyncEnabled,
                                    onCheckedChange = onSetCloudSyncEnabled,
                                )
                            }
                            if (settings.lastCloudSyncAt > 0L) {
                                Text(
                                    stringResource(
                                        R.string.last_cloud_sync,
                                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                            .format(Date(settings.lastCloudSyncAt)),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    stringResource(R.string.cloud_choice_required),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { scope.launch { repository.synchronize() } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = syncState !is CloudSyncState.Working,
                        ) {
                            Icon(Icons.Rounded.Sync, null)
                            Text(stringResource(R.string.sync_now), modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedButton(
                            onClick = { scope.launch { repository.prepareChoice() } },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = syncState !is CloudSyncState.Working,
                        ) { Text(stringResource(R.string.choose_sync_source)) }
                        TextButton(
                            onClick = { showSignOutChoice = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = syncState !is CloudSyncState.Working,
                        ) { Text(stringResource(R.string.sign_out_google)) }
                    }
                }
            }
        }

        if (initialSetup) {
            item {
                TextButton(
                    onClick = {
                        repository.clearStatus()
                        onInitialSetupComplete(null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = syncState !is CloudSyncState.Working,
                ) {
                    Text(stringResource(R.string.skip_google_for_now))
                }
            }
        }

        if (syncState is CloudSyncState.Working) {
            item {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    operationLabel((syncState as CloudSyncState.Working).operation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when (val state = syncState) {
            is CloudSyncState.Success -> item {
                StatusCard(
                    title = if (state.operation == CloudOperation.Download) {
                        stringResource(R.string.cloud_download_complete)
                    } else {
                        stringResource(R.string.cloud_upload_complete)
                    },
                    summary = state.summary,
                )
            }
            is CloudSyncState.NoChanges -> item {
                StatusCard(stringResource(R.string.cloud_already_current), null)
            }
            is CloudSyncState.Error -> item {
                Card(shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(stringResource(R.string.cloud_sync_error), style = MaterialTheme.typography.titleMedium)
                        Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = repository::clearStatus) { Text(stringResource(R.string.ok_label)) }
                    }
                }
            }
            else -> Unit
        }
    }

    val choice = syncState as? CloudSyncState.ChoiceRequired
    if (choice != null) {
        AlertDialog(
            onDismissRequest = repository::clearStatus,
            icon = { Icon(if (choice.conflict) Icons.Rounded.CloudOff else Icons.Rounded.Sync, null) },
            title = {
                Text(
                    stringResource(
                        if (choice.conflict) R.string.cloud_conflict_title else R.string.choose_sync_source,
                    ),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(
                            if (choice.conflict) R.string.cloud_conflict_description else R.string.cloud_choice_description,
                        ),
                    )
                    SummaryText(stringResource(R.string.this_device), choice.local)
                    choice.cloud?.let { SummaryText(stringResource(R.string.google_cloud), it) }
                        ?: Text(stringResource(R.string.no_cloud_backup), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { scope.launch { repository.uploadDevice() } }) {
                    Icon(Icons.Rounded.CloudUpload, null)
                    Text(stringResource(R.string.use_device_data), modifier = Modifier.padding(start = 6.dp))
                }
            },
            dismissButton = {
                Row {
                    if (choice.cloud != null) {
                        TextButton(onClick = {
                            scope.launch {
                                repository.downloadCloud()
                                if (repository.syncState.value is CloudSyncState.Success) onDataRestored()
                            }
                        }) {
                            Icon(Icons.Rounded.CloudDownload, null)
                            Text(stringResource(R.string.use_cloud_data), modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    TextButton(onClick = repository::clearStatus) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }

    if (showSignOutChoice) AlertDialog(
        onDismissRequest = { showSignOutChoice = false },
        title = { Text(stringResource(R.string.sign_out_google)) },
        text = { Text(stringResource(R.string.sign_out_data_choice)) },
        confirmButton = {
            TextButton(onClick = { showSignOutChoice = false; scope.launch { repository.signOut(removeLocalData = false) } }) {
                Text(stringResource(R.string.keep_on_device))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { showSignOutChoice = false }) { Text(stringResource(R.string.cancel)) }
                TextButton(onClick = {
                    showSignOutChoice = false
                    scope.launch { repository.signOut(removeLocalData = true); onDataRestored() }
                }) { Text(stringResource(R.string.remove_from_device), color = MaterialTheme.colorScheme.error) }
            }
        },
    )
}

internal fun preferredGoogleName(displayName: String): String = displayName
    .trim()
    .split(Regex("\\s+"))
    .firstOrNull()
    .orEmpty()

@Composable
private fun FirebaseSetupCard() {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.CloudOff, null, modifier = Modifier.size(44.dp))
            Text(stringResource(R.string.firebase_setup_required), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.firebase_setup_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.firebase_setup_step_one))
            Text(stringResource(R.string.firebase_setup_step_two))
            Text(stringResource(R.string.firebase_setup_step_three))
        }
}

@Composable
private fun SignedInAccountCard(account: CloudAccountState.SignedIn) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Rounded.AccountCircle, null, modifier = Modifier.padding(10.dp).size(36.dp))
            }
            Column {
                Text(account.displayName.ifBlank { stringResource(R.string.google_account_label) }, style = MaterialTheme.typography.titleLarge)
                Text(account.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
}

@Composable
private fun StatusCard(title: String, summary: BackupSummary?) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            summary?.let { SummaryText(stringResource(R.string.synced_data), it) }
        }
}

@Composable
private fun SummaryText(label: String, summary: BackupSummary) {
    Text(
        stringResource(
            R.string.cloud_summary,
            label,
            summary.habitCount,
            summary.historyCount,
            summary.contentCount,
            summary.goalCount,
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun operationLabel(operation: CloudOperation): String = stringResource(
    when (operation) {
        CloudOperation.SignIn -> R.string.google_signing_in
        CloudOperation.Upload -> R.string.cloud_uploading
        CloudOperation.Download -> R.string.cloud_downloading
        CloudOperation.Synchronize -> R.string.cloud_synchronizing
        CloudOperation.SignOut -> R.string.google_signing_out
    },
)
