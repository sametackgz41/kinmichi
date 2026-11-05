package app.tsuzuku.ui.screens

import app.tsuzuku.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.model.AskTsuzukuMessage
import app.tsuzuku.model.MessageSender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskTsuzukuSheet(
    messages: List<AskTsuzukuMessage>,
    isGenerating: Boolean,
    onAsk: (String) -> Unit,
    onClearChat: () -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        stringResource(R.string.ask_suggestion_productive_hours),
        stringResource(R.string.ask_suggestion_struggling_habit),
        stringResource(R.string.ask_suggestion_postponed_tasks),
        stringResource(R.string.ask_suggestion_30_day_progress),
    )

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .imePadding(),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🌱", fontSize = 18.sp)
                        }
                    }
                    Column {
                        Text(stringResource(R.string.ask_tsuzuku_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.ask_tsuzuku_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = onClearChat) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.clear_label))
                        }
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close_action))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Quick prompts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickPrompts.forEach { prompt ->
                    SuggestionChip(
                        onClick = { onAsk(prompt) },
                        label = { Text(prompt, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(380.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.ask_empty_title), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    stringResource(R.string.ask_empty_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    if (msg.sender == MessageSender.User) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Surface(
                                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 48.dp),
                            ) {
                                Text(
                                    msg.content,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 24.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Markdown-style bold rendering
                                    FormattedAssistantText(text = msg.content)

                                    if (msg.supportingMetrics.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            stringResource(R.string.ask_data_used),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            msg.supportingMetrics.forEach { (k, v) ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                ) {
                                                    Text(
                                                        "$k: $v",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Dynamic Follow-up Questions
                                    if (msg.followUpQuestions.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            stringResource(R.string.ask_follow_ups),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            msg.followUpQuestions.forEach { followUp ->
                                                SuggestionChip(
                                                    onClick = { onAsk(followUp) },
                                                    label = { Text(followUp, style = MaterialTheme.typography.labelSmall) },
                                                )
                                            }
                                        }
                                    }

                                    // Actions row
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                    ) {
                                        IconButton(
                                            onClick = { clipboardManager.setText(AnnotatedString(msg.content)) },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(
                                                Icons.Rounded.ContentCopy,
                                                contentDescription = stringResource(R.string.copy_action),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isGenerating) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(8.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.ask_thinking), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    placeholder = { Text(stringResource(R.string.ask_input_placeholder), style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                )
                IconButton(
                    enabled = inputQuery.isNotBlank() && !isGenerating,
                    onClick = {
                        onAsk(inputQuery)
                        inputQuery = ""
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (inputQuery.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.send_action),
                        tint = if (inputQuery.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormattedAssistantText(text: String) {
    val annotatedString = buildAnnotatedString {
        val parts = text.split("**")
        for (i in parts.indices) {
            if (i % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(parts[i])
                }
            } else {
                append(parts[i])
            }
        }
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 20.sp,
    )
}
