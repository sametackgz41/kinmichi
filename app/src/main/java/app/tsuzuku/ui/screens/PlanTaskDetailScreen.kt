package app.tsuzuku.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.Goal
import app.tsuzuku.model.PlanTask
import app.tsuzuku.model.TaskGoalLink
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PlanTaskDetailScreen(
    task: PlanTask,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onMove: (Long?) -> Unit,
    onAddChecklist: (String) -> Unit,
    onToggleChecklist: (Long) -> Unit,
    onDeleteChecklist: (Long) -> Unit,
    onSetActualMinutes: (Int?) -> Unit,
    onDuplicate: () -> Unit,
    onEdit: () -> Unit,
    onStartFocus: () -> Unit = {},
    linkedGoal: Pair<Goal, TaskGoalLink>? = null,
    availableGoals: List<Goal> = emptyList(),
    onLinkGoal: (goalId: Long, value: Double) -> Unit = { _, _ -> },
    onUnlinkGoal: (goalId: Long) -> Unit = {},
) {
    var newStep by rememberSaveable { mutableStateOf("") }
    var actualMinutes by rememberSaveable(task.id, task.actualMinutes) { mutableStateOf(task.actualMinutes?.toString().orEmpty()) }
    var showLinkGoalDialog by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeader(stringResource(R.string.task_details), onBack) { IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit_task)) } } }
        item {
            Text("計画 · TASK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
                Text(task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            if (task.note.isNotBlank()) Text(task.note, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 48.dp))
        }
        item {
            FilledTonalButton(
                onClick = onStartFocus,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.start_focus))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.linked_goals), style = MaterialTheme.typography.titleMedium)
                if (linkedGoal == null) {
                    FilledTonalButton(onClick = { showLinkGoalDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.link_goal))
                    }
                }
            }
            if (linkedGoal != null) {
                val (goal, link) = linkedGoal
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(goal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("+${link.contributionValue} puan katkı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onUnlinkGoal(goal.id) }) {
                            Icon(Icons.Rounded.LinkOff, contentDescription = stringResource(R.string.unlink), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.schedule), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = task.scheduledEpochDay == LocalDate.now().toEpochDay(), onClick = { onMove(LocalDate.now().toEpochDay()) }, label = { Text(stringResource(R.string.today)) })
                FilterChip(selected = task.scheduledEpochDay == LocalDate.now().plusDays(1).toEpochDay(), onClick = { onMove(LocalDate.now().plusDays(1).toEpochDay()) }, label = { Text(stringResource(R.string.tomorrow)) })
                FilterChip(selected = task.scheduledEpochDay == null, onClick = { onMove(null) }, label = { Text(stringResource(R.string.inbox)) })
            }
            task.scheduledEpochDay?.let {
                Text(LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d MMMM yyyy")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.checklist), style = MaterialTheme.typography.titleMedium)
                Text("${task.checklist.count { it.completed }} / ${task.checklist.size}", color = MaterialTheme.colorScheme.secondary)
            }
            task.checklist.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().clickable { onToggleChecklist(item.id) }.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = item.completed, onCheckedChange = { onToggleChecklist(item.id) })
                    Text(item.title, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onDeleteChecklist(item.id) }) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete_label)) }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newStep,
                    onValueChange = { newStep = it.take(160) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.new_checklist_item)) },
                    singleLine = true,
                )
                IconButton(
                    enabled = newStep.isNotBlank(),
                    onClick = { onAddChecklist(newStep); newStep = "" },
                ) { Icon(Icons.Rounded.Add, stringResource(R.string.add)) }
            }
        }
        if (task.carryCount >= 3) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("⏳", fontSize = 18.sp)
                        Column {
                            Text(
                                "Erteleme Farkındalığı (${task.carryCount} kez ertelendi)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                "Bu görev gizli bir direnç yaratıyor olabilir. Yukarıdaki checklist alanından görevi 5 dakikalık alt adımlara bölmeyi deneyebilirsin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = actualMinutes,
                onValueChange = { actualMinutes = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.actual_minutes)) },
                supportingText = { Text(stringResource(R.string.actual_minutes_help)) },
                singleLine = true,
            )
            OutlinedButton(onClick = { onSetActualMinutes(actualMinutes.toIntOrNull()) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_label)) }
        }
        item {
            val details = buildList {
                add(stringResource(R.string.priority_value, task.priority))
                task.estimatedMinutes?.let { add(stringResource(R.string.minute_count, it)) }
                task.actualMinutes?.let { add(stringResource(R.string.actual_minute_count, it)) }
                if (task.carryCount > 0) add(stringResource(R.string.carried_count, task.carryCount))
            }
            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedButton(onClick = onDuplicate, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.duplicate_task))
            }
        }
    }

    if (showLinkGoalDialog) {
        var selectedGoalId by remember { mutableStateOf(availableGoals.firstOrNull()?.id) }
        var valueText by remember { mutableStateOf("1.0") }

        AlertDialog(
            onDismissRequest = { showLinkGoalDialog = false },
            title = { Text(stringResource(R.string.link_goal)) },
            text = {
                if (availableGoals.isEmpty()) {
                    Text("Bağlanabilecek aktif hedef yok.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Hedef seçin:", style = MaterialTheme.typography.labelLarge)
                        availableGoals.forEach { goal ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selectedGoalId = goal.id }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selectedGoalId == goal.id, onClick = { selectedGoalId = goal.id })
                                Text(goal.title, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        OutlinedTextField(
                            value = valueText,
                            onValueChange = { valueText = it },
                            label = { Text(stringResource(R.string.contribution_value)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {
                if (availableGoals.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val id = selectedGoalId ?: return@TextButton
                            val v = valueText.toDoubleOrNull() ?: 1.0
                            onLinkGoal(id, v)
                            showLinkGoalDialog = false
                        },
                    ) { Text(stringResource(R.string.link_goal)) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkGoalDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
