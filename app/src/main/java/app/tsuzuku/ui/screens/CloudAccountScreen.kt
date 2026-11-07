package app.tsuzuku.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
    onSignedInName: (String) -> Unit = {},
) {
    val account by repository.account.collectAsStateWithLifecycle()
    val syncState by repository.syncState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showSignOutChoice by rememberSaveable { mutableStateOf(false) }
    var showDeleteAccount by rememberSaveable { mutableStateOf(false) }

    AccountContent(
        account = account,
        syncState = syncState,
        cloudSyncEnabled = settings.cloudSyncEnabled,
        lastCloudSyncAt = settings.lastCloudSyncAt,
        onBack = onBack,
        onSignIn = { activity, provider ->
            val result = if (provider == SignInProvider.GitHub) repository.signInWithGitHub(activity) else repository.signIn(activity)
            if (result != null) {
                preferredGoogleName(result.displayName).takeIf { it.isNotBlank() }?.let(onSignedInName)
                repository.prepareChoice()
            }
        },
        onSetCloudSyncEnabled = onSetCloudSyncEnabled,
        onSyncNow = { scope.launch { repository.synchronize() } },
        onChooseSource = { scope.launch { repository.prepareChoice() } },
        onSignOut = { showSignOutChoice = true },
        onDelete = { showDeleteAccount = true },
        onDismissStatus = repository::clearStatus,
    )

    val choice = syncState as? CloudSyncState.ChoiceRequired
    if (choice != null) {
        AlertDialog(
            onDismissRequest = repository::clearStatus,
            icon = { Icon(if (choice.conflict) Icons.Rounded.CloudOff else Icons.Rounded.Sync, null) },
            title = {
                Text(stringResource(if (choice.conflict) R.string.cloud_conflict_title else R.string.choose_sync_source))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(if (choice.conflict) R.string.cloud_conflict_description else R.string.cloud_choice_description))
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

    if (showDeleteAccount) AlertDialog(
        onDismissRequest = { showDeleteAccount = false },
        title = { Text(stringResource(R.string.delete_account_title)) },
        text = { Text(stringResource(R.string.delete_account_message)) },
        confirmButton = {
            TextButton(onClick = {
                showDeleteAccount = false
                scope.launch { if (repository.deleteAccount()) onDataRestored() }
            }) { Text(stringResource(R.string.delete_account_confirm), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteAccount = false }) { Text(stringResource(R.string.cancel)) }
        },
    )

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

/** The account page without the repository, so previews can draw each state. */
@Composable
internal fun AccountContent(
    account: CloudAccountState,
    syncState: CloudSyncState,
    cloudSyncEnabled: Boolean,
    lastCloudSyncAt: Long,
    onBack: () -> Unit,
    onSignIn: suspend (Activity, SignInProvider) -> Unit,
    onSetCloudSyncEnabled: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onChooseSource: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: () -> Unit,
    onDismissStatus: () -> Unit,
    layout: AccountLayout = AccountLayout.List,
) {
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    val working = syncState is CloudSyncState.Working
    var pending by rememberSaveable { mutableStateOf<SignInProvider?>(null) }
    if (!working) pending = null

    Surface(color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                    Text(stringResource(R.string.account_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
                }
            }

            when (account) {
                CloudAccountState.Unavailable -> item { FirebaseSetupCard() }
                CloudAccountState.SignedOut -> {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 12.dp)) {
                            GoldenPathArt(artSize = 84.dp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.account_signed_out_title),
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.account_signed_out_body),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    item { BenefitList() }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProviderButton(
                                label = stringResource(R.string.sign_in_with_google),
                                icon = R.drawable.ic_brand_google,
                                tintIcon = false,
                                loading = pending == SignInProvider.Google,
                                enabled = activity != null && !working,
                                onClick = {
                                    pending = SignInProvider.Google
                                    activity?.let { scope.launch { onSignIn(it, SignInProvider.Google) } }
                                },
                            )
                            ProviderButton(
                                label = stringResource(R.string.sign_in_with_github),
                                icon = R.drawable.ic_brand_github,
                                tintIcon = true,
                                loading = pending == SignInProvider.GitHub,
                                enabled = activity != null && !working,
                                onClick = {
                                    pending = SignInProvider.GitHub
                                    activity?.let { scope.launch { onSignIn(it, SignInProvider.GitHub) } }
                                },
                            )
                        }
                    }
                }
                is CloudAccountState.SignedIn -> when (layout) {
                    AccountLayout.List -> item {
                        SignedInList(account, cloudSyncEnabled, lastCloudSyncAt, working, onSetCloudSyncEnabled, onSyncNow, onChooseSource, onSignOut, onDelete)
                    }
                    AccountLayout.Hero -> item {
                        SignedInHero(account, cloudSyncEnabled, lastCloudSyncAt, working, onSetCloudSyncEnabled, onSyncNow, onChooseSource, onSignOut, onDelete)
                    }
                    AccountLayout.Cards -> {
                        item { ProfileCard(account) }
                        item {
                            SyncCard(
                                enabled = cloudSyncEnabled,
                                lastCloudSyncAt = lastCloudSyncAt,
                                working = working,
                                onToggle = onSetCloudSyncEnabled,
                                onSyncNow = onSyncNow,
                                onChooseSource = onChooseSource,
                            )
                        }
                        item {
                            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = onSignOut,
                                    enabled = !working,
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, null, modifier = Modifier.size(18.dp))
                                    Text(stringResource(R.string.sign_out_google), modifier = Modifier.padding(start = 8.dp))
                                }
                                TextButton(onClick = onDelete, enabled = !working, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.delete_account), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            if (syncState is CloudSyncState.Working) {
                item {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(
                            operationLabel(syncState.operation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            when (syncState) {
                is CloudSyncState.Success -> item {
                    StatusCard(
                        title = stringResource(
                            if (syncState.operation == CloudOperation.Download) R.string.cloud_download_complete else R.string.cloud_upload_complete,
                        ),
                        summary = syncState.summary,
                        onDismiss = onDismissStatus,
                    )
                }
                is CloudSyncState.NoChanges -> item { StatusCard(stringResource(R.string.cloud_already_current), null, onDismissStatus) }
                is CloudSyncState.Error -> item { StatusCard(stringResource(R.string.cloud_sync_error), null, onDismissStatus, message = syncState.message, error = true) }
                else -> Unit
            }
        }
    }
}

/** Avatar initial, name, e-mail and which account it is. */
@Composable
private fun ProfileCard(account: CloudAccountState.SignedIn) {
    val github = account.provider == CloudAccountState.PROVIDER_GITHUB
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val initial = account.displayName.trim().firstOrNull()?.uppercase() ?: account.email.firstOrNull()?.uppercase() ?: "?"
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(12.dp))
        Text(account.displayName.ifBlank { account.email }, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (account.email.isNotBlank() && account.displayName.isNotBlank()) {
            Text(account.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(if (github) R.drawable.ic_brand_github else R.drawable.ic_brand_google),
                null,
                tint = if (github) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.size(16.dp),
            )
            Text(
                stringResource(if (github) R.string.account_via_github else R.string.account_via_google),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun SyncCard(
    enabled: Boolean,
    lastCloudSyncAt: Long,
    working: Boolean,
    onToggle: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onChooseSource: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.CloudSync, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(R.string.automatic_cloud_sync), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (lastCloudSyncAt > 0L) {
                        stringResource(
                            R.string.last_cloud_sync,
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastCloudSyncAt)),
                        )
                    } else {
                        stringResource(R.string.cloud_choice_required)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onSyncNow,
                enabled = !working,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(Icons.Rounded.Sync, null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.sync_now), modifier = Modifier.padding(start = 6.dp), maxLines = 1)
            }
            androidx.compose.material3.FilledTonalButton(
                onClick = onChooseSource,
                enabled = !working,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text(stringResource(R.string.account_choose_source_short), maxLines = 1) }
        }
    }
}

internal fun preferredGoogleName(displayName: String): String = displayName
    .trim()
    .split(Regex("\\s+"))
    .firstOrNull()
    .orEmpty()

@Composable
private fun FirebaseSetupCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.CloudOff, null, modifier = Modifier.size(40.dp))
        Text(stringResource(R.string.firebase_setup_required), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.firebase_setup_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.firebase_setup_step_one))
        Text(stringResource(R.string.firebase_setup_step_two))
        Text(stringResource(R.string.firebase_setup_step_three))
    }
}

@Composable
private fun StatusCard(title: String, summary: BackupSummary?, onDismiss: () -> Unit, message: String? = null, error: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            )
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer) }
            summary?.let { SummaryText(stringResource(R.string.synced_data), it) }
        }
        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.close_label)) }
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
        CloudOperation.DeleteAccount -> R.string.deleting_account
    },
)

/** Three layouts for the signed-in account page, drawn side by side in previews to choose from. */
internal enum class AccountLayout { Cards, List, Hero }

@Composable
private fun SignedInList(
    account: CloudAccountState.SignedIn,
    cloudSyncEnabled: Boolean,
    lastCloudSyncAt: Long,
    working: Boolean,
    onSetCloudSyncEnabled: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onChooseSource: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: () -> Unit,
) {
    val github = account.provider == CloudAccountState.PROVIDER_GITHUB
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Profile row
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(account, 52.dp)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(account.displayName.ifBlank { account.email }, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                if (account.email.isNotBlank()) Text(account.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Icon(
                painterResource(if (github) R.drawable.ic_brand_github else R.drawable.ic_brand_google), null,
                tint = if (github) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.size(20.dp),
            )
        }
        GroupLabel(stringResource(R.string.automatic_cloud_sync))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            ListRow(Icons.Rounded.CloudSync, stringResource(R.string.account_auto_sync_short), syncSubtitle(lastCloudSyncAt), trailing = {
                Switch(checked = cloudSyncEnabled, onCheckedChange = onSetCloudSyncEnabled)
            })
            RowDivider()
            ListRow(Icons.Rounded.Sync, stringResource(R.string.sync_now), null, enabled = !working, onClick = onSyncNow)
            RowDivider()
            ListRow(Icons.Rounded.SwapVert, stringResource(R.string.choose_sync_source), null, enabled = !working, onClick = onChooseSource)
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            ListRow(Icons.AutoMirrored.Rounded.Logout, stringResource(R.string.sign_out_google), null, enabled = !working, onClick = onSignOut)
            RowDivider()
            ListRow(Icons.Rounded.DeleteOutline, stringResource(R.string.delete_account), null, enabled = !working, onClick = onDelete, danger = true)
        }
    }
}

@Composable
private fun SignedInHero(
    account: CloudAccountState.SignedIn,
    cloudSyncEnabled: Boolean,
    lastCloudSyncAt: Long,
    working: Boolean,
    onSetCloudSyncEnabled: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onChooseSource: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: () -> Unit,
) {
    val github = account.provider == CloudAccountState.PROVIDER_GITHUB
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.size(112.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(
                if (lastCloudSyncAt > 0L) Icons.Rounded.CloudDone else Icons.Rounded.CloudSync, null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(52.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(if (lastCloudSyncAt > 0L) R.string.account_safe_title else R.string.account_not_synced_title),
            style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(syncSubtitle(lastCloudSyncAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onSyncNow, enabled = !working, shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Icon(Icons.Rounded.Sync, null, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.sync_now), modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(account, 40.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(account.displayName.ifBlank { account.email }, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text(
                        stringResource(if (github) R.string.account_via_github else R.string.account_via_google),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    painterResource(if (github) R.drawable.ic_brand_github else R.drawable.ic_brand_google), null,
                    tint = if (github) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Unspecified,
                    modifier = Modifier.size(20.dp),
                )
            }
            RowDivider()
            ListRow(Icons.Rounded.CloudSync, stringResource(R.string.account_auto_sync_short), null, trailing = {
                Switch(checked = cloudSyncEnabled, onCheckedChange = onSetCloudSyncEnabled)
            })
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onChooseSource, enabled = !working) { Text(stringResource(R.string.account_choose_source_short)) }
            TextButton(onClick = onSignOut, enabled = !working) { Text(stringResource(R.string.sign_out_google)) }
            TextButton(onClick = onDelete, enabled = !working) { Text(stringResource(R.string.account_delete_short), color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun Avatar(account: CloudAccountState.SignedIn, size: androidx.compose.ui.unit.Dp) {
    val initial = account.displayName.trim().firstOrNull()?.uppercase() ?: account.email.firstOrNull()?.uppercase() ?: "?"
    Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Text(
            initial,
            style = if (size > 60.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp),
    )
}

@Composable
private fun RowDivider() {
    androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 64.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ListRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    danger: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    var modifier = Modifier.fillMaxWidth()
    if (onClick != null) modifier = modifier.clickable(enabled = enabled, onClick = onClick)
    Row(modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                .background(if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = color)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (trailing != null) trailing() else if (onClick != null) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun syncSubtitle(lastCloudSyncAt: Long): String =
    if (lastCloudSyncAt > 0L) {
        stringResource(R.string.last_cloud_sync, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastCloudSyncAt)))
    } else {
        stringResource(R.string.cloud_choice_required)
    }
