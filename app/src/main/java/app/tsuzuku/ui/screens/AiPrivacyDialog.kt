package app.tsuzuku.ui.screens

import app.tsuzuku.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.data.preferences.AppSettings

@Composable
fun AiPrivacyDialog(
    settings: AppSettings,
    onSetCloudAiEnabled: (Boolean) -> Unit,
    onSetGeminiApiKey: (String) -> Unit,
    onSetCloudAiModel: (String) -> Unit,
    onSetAiShareHabitTitles: (Boolean) -> Unit,
    onSetAiShareTaskTitles: (Boolean) -> Unit,
    onSetAiShareNotes: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var apiKeyInput by remember { mutableStateOf(settings.geminiApiKey) }

    val models = listOf(
        "gemini-1.5-flash" to "Gemini 1.5 Flash (Dengeli & Hızlı)",
        "gemini-2.0-flash" to "Gemini 2.0 Flash (Yeni Nesil)",
        "gemini-1.5-pro" to "Gemini 1.5 Pro (Derin Akıl Yürütme)",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.ai_privacy_settings_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Status banner
                val isCloudActive = settings.cloudAiEnabled && settings.geminiApiKey.isNotBlank()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCloudActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(if (isCloudActive) "🟢" else "🌱", fontSize = 16.sp)
                        Column {
                            Text(
                                if (isCloudActive) "Google Gemini (${settings.cloudAiModel})" else stringResource(R.string.ai_offline_engine_active),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                if (isCloudActive) stringResource(R.string.ai_cloud_mode_desc)
                                else stringResource(R.string.ai_offline_mode_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cloud AI Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.ai_cloud_title), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.ai_cloud_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.cloudAiEnabled,
                        onCheckedChange = onSetCloudAiEnabled,
                    )
                }

                if (settings.cloudAiEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ai_api_key_label)) },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.ai_model_selection), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    models.forEach { (modelKey, label) ->
                        FilterChip(
                            selected = settings.cloudAiModel == modelKey,
                            onClick = { onSetCloudAiModel(modelKey) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text(stringResource(R.string.ai_shared_data_permissions), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.ai_shared_data_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = settings.aiShareHabitTitles,
                        onCheckedChange = onSetAiShareHabitTitles,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.ai_share_habit_names), style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = settings.aiShareTaskTitles,
                        onCheckedChange = onSetAiShareTaskTitles,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.ai_share_task_titles), style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = settings.aiShareNotes,
                        onCheckedChange = onSetAiShareNotes,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.ai_share_daily_notes), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (apiKeyInput != settings.geminiApiKey) {
                        onSetGeminiApiKey(apiKeyInput.trim())
                    }
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.ai_save_and_close))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
