package app.tsuzuku.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalQuickAddSheet(
    onNewHabit: (prefillName: String) -> Unit,
    onNewTask: (prefillTitle: String, scheduledEpochDay: Long?, dueMinutes: Int?) -> Unit,
    onNewGoal: (prefillTitle: String) -> Unit,
    onNewContent: (prefillTitle: String) -> Unit,
    onStartFocus: () -> Unit,
    onSaveToInbox: (String) -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }

    // Simple natural language parsing
    val isTomorrow = text.contains("yarın", ignoreCase = true) || text.contains("tomorrow", ignoreCase = true)
    val timeMatch = Regex("""\b([01]?[0-9]|2[0-3]):([0-5][0-9])\b""").find(text)
    val parsedDueMinutes = timeMatch?.let {
        val (h, m) = it.destructured
        h.toInt() * 60 + m.toInt()
    }
    val cleanedTitle = text
        .replace(Regex("""(?i)\byarın\b"""), "")
        .replace(Regex("""(?i)\btomorrow\b"""), "")
        .replace(Regex("""\b([01]?[0-9]|2[0-3]):([0-5][0-9])\b"""), "")
        .trim()

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.quick_add_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.quick_add_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )

            if (isTomorrow || parsedDueMinutes != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isTomorrow) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(stringResource(R.string.tomorrow_chip), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (parsedDueMinutes != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            val h = parsedDueMinutes / 60
                            val m = parsedDueMinutes % 60
                            Text("⏰ %02d:%02d".format(h, m), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(stringResource(R.string.what_to_create), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickActionCard(
                    icon = Icons.Rounded.Checklist,
                    title = stringResource(R.string.task_label),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val epochDay = if (isTomorrow) LocalDate.now().plusDays(1).toEpochDay() else LocalDate.now().toEpochDay()
                        onNewTask(cleanedTitle.ifBlank { text }, epochDay, parsedDueMinutes)
                    },
                )
                QuickActionCard(
                    icon = Icons.Rounded.Repeat,
                    title = stringResource(R.string.habit_label),
                    modifier = Modifier.weight(1f),
                    onClick = { onNewHabit(cleanedTitle.ifBlank { text }) },
                )
                QuickActionCard(
                    icon = Icons.Rounded.Flag,
                    title = stringResource(R.string.goal_step),
                    modifier = Modifier.weight(1f),
                    onClick = { onNewGoal(cleanedTitle.ifBlank { text }) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickActionCard(
                    icon = Icons.Rounded.MenuBook,
                    title = stringResource(R.string.library),
                    modifier = Modifier.weight(1f),
                    onClick = { onNewContent(cleanedTitle.ifBlank { text }) },
                )
                QuickActionCard(
                    icon = Icons.Rounded.Timer,
                    title = stringResource(R.string.focus_action),
                    modifier = Modifier.weight(1f),
                    onClick = onStartFocus,
                )
                QuickActionCard(
                    icon = Icons.Rounded.Inbox,
                    title = stringResource(R.string.inbox_label),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (text.isNotBlank()) {
                            onSaveToInbox(text.trim())
                            onClose()
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
