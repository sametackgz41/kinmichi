package app.tsuzuku.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.data.CloudAccountState
import app.tsuzuku.data.CloudSyncState
import kotlinx.coroutines.launch

/**
 * Last step of the first run, after the tour: the name Today greets you with, and an optional
 * Google sign-in for backup and sync. Nothing here is required.
 */
@Composable
fun WelcomeScreen(
    account: CloudAccountState,
    syncState: CloudSyncState,
    initialName: String,
    onSignIn: suspend (Activity) -> Unit,
    onFinish: (name: String, signedIn: Boolean) -> Unit,
) {
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var name by rememberSaveable { mutableStateOf(initialName) }
    var nameEdited by rememberSaveable { mutableStateOf(initialName.isNotBlank()) }
    val signedIn = account as? CloudAccountState.SignedIn
    val working = syncState is CloudSyncState.Working

    // Fill in the first name from Google, unless the user already typed one.
    LaunchedEffect(signedIn) {
        if (signedIn != null && !nameEdited) name = preferredGoogleName(signedIn.displayName)
    }

    androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) { Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterStart) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(40.dp))
        Text(
            stringResource(R.string.welcome_eyebrow),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40); nameEdited = true },
            label = { Text(stringResource(R.string.welcome_name_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
        )

        if (account !is CloudAccountState.Unavailable) {
            Spacer(Modifier.height(28.dp))
            SyncCard(
                signedIn = signedIn,
                working = working,
                error = (syncState as? CloudSyncState.Error)?.message,
                onSignIn = { activity?.let { scope.launch { onSignIn(it) } } },
                canSignIn = activity != null && !working,
            )
        }

        Spacer(Modifier.weight(1f).height(32.dp))
        Button(
            onClick = { onFinish(name.trim(), signedIn != null) },
            enabled = !working,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.start_using_tsuzuku)) }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.welcome_private_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    } }
}

@Composable
private fun SyncCard(
    signedIn: CloudAccountState.SignedIn?,
    working: Boolean,
    error: String?,
    onSignIn: () -> Unit,
    canSignIn: Boolean,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.CloudSync, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
            Column(Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.welcome_sync_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.welcome_optional), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            stringResource(R.string.welcome_sync_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            signedIn != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.welcome_signed_in, signedIn.email),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            working -> LinearProgressIndicator(Modifier.fillMaxWidth())
            else -> OutlinedButton(onClick = onSignIn, enabled = canSignIn, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sign_in_with_google))
            }
        }
        if (error != null && signedIn == null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
