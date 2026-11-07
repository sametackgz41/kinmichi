package app.tsuzuku.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.BuildConfig
import app.tsuzuku.R

/** Once after an update: the few things worth knowing about this version. */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.whats_new_title, BuildConfig.VERSION_NAME)) },
        text = { WhatsNewList() },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.whats_new_ok)) } },
    )
}

@Composable
internal fun WhatsNewList(modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stringArrayResource(R.array.whats_new_items).forEach { item ->
            val title = item.substringBefore('|')
            val body = item.substringAfter('|', "")
            Row {
                Text("•", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    if (body.isNotBlank()) {
                        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
