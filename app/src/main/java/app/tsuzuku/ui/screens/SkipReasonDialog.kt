package app.tsuzuku.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.SkipReasonType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkipReasonDialog(
    habitName: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var selectedReason by remember { mutableStateOf<SkipReasonType?>(null) }
    var customText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.skip_mark), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.skip_habit_title), style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.skip_habit_message, habitName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkipReasonType.entries.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = {
                                selectedReason = if (selectedReason == reason) null else reason
                            },
                            label = { Text("${reason.icon} ${reason.defaultLabel}") },
                        )
                    }
                }
                if (selectedReason == SkipReasonType.Other) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it.take(80) },
                        label = { Text(stringResource(R.string.custom_reason)) },
                        placeholder = { Text(stringResource(R.string.custom_reason_example)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = selectedReason?.let { SkipReasonType.format(it, customText) }
                    onConfirm(formatted)
                },
            ) {
                Text(stringResource(R.string.save_and_skip))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = { onConfirm(null) }) {
                    Text(stringResource(R.string.skip_without_reason))
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
    )
}
