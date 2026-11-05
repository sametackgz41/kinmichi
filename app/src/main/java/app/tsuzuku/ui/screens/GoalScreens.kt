package app.tsuzuku.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitGoalContributionMode
import app.tsuzuku.model.HabitGoalLink
import app.tsuzuku.model.PlanTask
import app.tsuzuku.model.TaskGoalLink
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.RadioButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.Goal
import app.tsuzuku.model.GoalDraft
import app.tsuzuku.model.GoalStatus
import app.tsuzuku.ui.theme.Success
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun GoalsScreen(
    goals: List<Goal>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onIncrement: (Long) -> Unit,
    onDecrement: (Long) -> Unit,
) {
    val visible = goals.filter { it.status != GoalStatus.Archived }
    val active = visible.filter { it.status == GoalStatus.Active }
    val completed = visible.filter { it.status == GoalStatus.Completed }
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(stringResource(R.string.goals), onBack) {
                FilledIconButton(onClick = onAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_goal))
                }
            }
            Text(stringResource(R.string.goals_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)) {
                        Icon(
                            Icons.Rounded.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Column {
                        Text(
                            stringResource(R.string.active_goal_count, active.size),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            stringResource(R.string.completed_goal_count, completed.size),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                        )
                    }
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(10.dp)) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.Flag, contentDescription = null)
                        Text(stringResource(R.string.no_goals), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.no_goals_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = onAdd) { Text(stringResource(R.string.add_goal)) }
                    }
                }
            }
        }
        if (active.isNotEmpty()) item { Text(stringResource(R.string.active_goals), style = MaterialTheme.typography.titleMedium) }
        items(active, key = Goal::id) { goal ->
            GoalCard(goal, { onOpen(goal.id) }, { onDecrement(goal.id) }, { onIncrement(goal.id) })
        }
        if (completed.isNotEmpty()) item { Text(stringResource(R.string.completed_goals), style = MaterialTheme.typography.titleMedium) }
        items(completed, key = Goal::id) { goal ->
            GoalCard(goal, { onOpen(goal.id) }, { onDecrement(goal.id) }, { onIncrement(goal.id) })
        }
    }
}

@Composable
private fun GoalCard(goal: Goal, onOpen: () -> Unit, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(goal.title, style = MaterialTheme.typography.titleMedium)
                    goal.dueDateEpochDay?.let {
                        Text(
                            stringResource(R.string.due_date_value, formatGoalDate(it)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(
                    stringResource(R.string.goal_progress_value, formatGoalAmount(goal.currentValue), formatGoalAmount(goal.targetValue), goal.unit),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            LinearProgressIndicator(
                progress = { goal.progressFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = if (goal.status == GoalStatus.Completed) Success else MaterialTheme.colorScheme.primary,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDecrement, enabled = goal.currentValue > 0) {
                    Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.decrease_progress))
                }
                FilledTonalIconButton(onClick = onIncrement, enabled = goal.currentValue < goal.targetValue) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.increase_progress))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun GoalDetailScreen(
    goal: Goal,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetCompleted: (Boolean) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    linkedHabits: List<Pair<Habit, HabitGoalLink>> = emptyList(),
    linkedTasks: List<Pair<PlanTask, TaskGoalLink>> = emptyList(),
    availableHabits: List<Habit> = emptyList(),
    availableTasks: List<PlanTask> = emptyList(),
    onLinkHabit: (habitId: Long, mode: HabitGoalContributionMode, value: Double) -> Unit = { _, _, _ -> },
    onUnlinkHabit: (habitId: Long) -> Unit = {},
    onLinkTask: (taskId: Long, value: Double) -> Unit = { _, _ -> },
    onUnlinkTask: (taskId: Long) -> Unit = {},
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showLinkHabitDialog by remember { mutableStateOf(false) }
    var showLinkTaskDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(stringResource(R.string.goal_details), onBack) {
                IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_goal)) }
            }
        }
        item {
            Card(shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(goal.title, style = MaterialTheme.typography.headlineSmall)
                    if (goal.description.isNotBlank()) Text(goal.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.goal_progress_value, formatGoalAmount(goal.currentValue), formatGoalAmount(goal.targetValue), goal.unit),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    LinearProgressIndicator(
                        progress = { goal.progressFraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = if (goal.status == GoalStatus.Completed) Success else MaterialTheme.colorScheme.primary,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                    goal.dueDateEpochDay?.let { Text(stringResource(R.string.due_date_value, formatGoalDate(it))) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDecrement, enabled = goal.currentValue > 0, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Remove, contentDescription = null)
                        }
                        Button(onClick = onIncrement, enabled = goal.currentValue < goal.targetValue, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        }
                    }
                    FilledTonalButton(
                        onClick = { onSetCompleted(goal.status != GoalStatus.Completed) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                        Text(
                            if (goal.status == GoalStatus.Completed) stringResource(R.string.mark_goal_active)
                            else stringResource(R.string.complete_goal),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }

        // Linked Habits Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.linked_habits), style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(onClick = { showLinkHabitDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.link_habit))
                }
            }
        }
        if (linkedHabits.isEmpty()) {
            item {
                Text(
                    text = "Henüz bağlı alışkanlık yok.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(linkedHabits, key = { it.first.id }) { (habit, link) ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(habit.emoji, style = MaterialTheme.typography.titleLarge)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(habit.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            val modeDesc = if (link.contributionMode == HabitGoalContributionMode.HabitAmount) {
                                "Alışkanlık miktarı kadar katkı"
                            } else {
                                "+${link.contributionValue} katkı"
                            }
                            Text(modeDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onUnlinkHabit(habit.id) }) {
                            Icon(Icons.Rounded.LinkOff, contentDescription = stringResource(R.string.unlink), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Linked Tasks Section
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.linked_tasks), style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(onClick = { showLinkTaskDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.link_task))
                }
            }
        }
        if (linkedTasks.isEmpty()) {
            item {
                Text(
                    text = "Henüz bağlı görev yok.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(linkedTasks, key = { it.first.id }) { (task, link) ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            if (task.completed) Icons.Rounded.CheckCircle else Icons.Rounded.Flag,
                            contentDescription = null,
                            tint = if (task.completed) Success else MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("+${link.contributionValue} puan katkı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onUnlinkTask(task.id) }) {
                            Icon(Icons.Rounded.LinkOff, contentDescription = stringResource(R.string.unlink), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Archive, contentDescription = null)
                Text(stringResource(R.string.archive_goal), modifier = Modifier.padding(start = 8.dp))
            }
        }
        item {
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.delete_goal), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (showLinkHabitDialog) {
        val unlinked = availableHabits.filter { habit -> linkedHabits.none { it.first.id == habit.id } }
        var selectedHabitId by remember { mutableStateOf(unlinked.firstOrNull()?.id) }
        var mode by remember { mutableStateOf(HabitGoalContributionMode.HabitAmount) }
        var valueText by remember { mutableStateOf("1.0") }

        AlertDialog(
            onDismissRequest = { showLinkHabitDialog = false },
            title = { Text(stringResource(R.string.link_habit)) },
            text = {
                if (unlinked.isEmpty()) {
                    Text("Bağlanabilecek başka alışkanlık yok.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Alışkanlık seçin:", style = MaterialTheme.typography.labelLarge)
                        unlinked.forEach { habit ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selectedHabitId = habit.id }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selectedHabitId == habit.id, onClick = { selectedHabitId = habit.id })
                                Text("${habit.emoji} ${habit.name}", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        Text(stringResource(R.string.contribution_mode), style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mode == HabitGoalContributionMode.HabitAmount,
                                onClick = { mode = HabitGoalContributionMode.HabitAmount },
                            )
                            Text("Alışkanlık Miktarı", modifier = Modifier.padding(start = 6.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mode == HabitGoalContributionMode.FixedValue,
                                onClick = { mode = HabitGoalContributionMode.FixedValue },
                            )
                            Text("Sabit Değer", modifier = Modifier.padding(start = 6.dp))
                        }
                        if (mode == HabitGoalContributionMode.FixedValue) {
                            OutlinedTextField(
                                value = valueText,
                                onValueChange = { valueText = it },
                                label = { Text(stringResource(R.string.contribution_value)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (unlinked.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val id = selectedHabitId ?: return@TextButton
                            val v = valueText.toDoubleOrNull() ?: 1.0
                            onLinkHabit(id, mode, v)
                            showLinkHabitDialog = false
                        },
                    ) { Text(stringResource(R.string.link_habit)) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkHabitDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showLinkTaskDialog) {
        val unlinked = availableTasks.filter { task -> linkedTasks.none { it.first.id == task.id } }
        var selectedTaskId by remember { mutableStateOf(unlinked.firstOrNull()?.id) }
        var valueText by remember { mutableStateOf("1.0") }

        AlertDialog(
            onDismissRequest = { showLinkTaskDialog = false },
            title = { Text(stringResource(R.string.link_task)) },
            text = {
                if (unlinked.isEmpty()) {
                    Text("Bağlanabilecek başka görev yok.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Görev seçin:", style = MaterialTheme.typography.labelLarge)
                        unlinked.forEach { task ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selectedTaskId = task.id }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selectedTaskId == task.id, onClick = { selectedTaskId = task.id })
                                Text(task.title, modifier = Modifier.padding(start = 8.dp))
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
                if (unlinked.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val id = selectedTaskId ?: return@TextButton
                            val v = valueText.toDoubleOrNull() ?: 1.0
                            onLinkTask(id, v)
                            showLinkTaskDialog = false
                        },
                    ) { Text(stringResource(R.string.link_task)) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkTaskDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_goal)) },
            text = { Text(stringResource(R.string.delete_goal_confirmation, goal.title)) },
            confirmButton = { TextButton(onClick = onDelete) { Text(stringResource(R.string.delete_label), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorScreen(existing: Goal? = null, onBack: () -> Unit, onSave: (GoalDraft) -> Unit) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var description by rememberSaveable(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var current by rememberSaveable(existing?.id) { mutableStateOf(existing?.currentValue?.let(::formatGoalAmount).orEmpty()) }
    var target by rememberSaveable(existing?.id) { mutableStateOf(existing?.targetValue?.let(::formatGoalAmount).orEmpty()) }
    var unit by rememberSaveable(existing?.id) { mutableStateOf(existing?.unit.orEmpty()) }
    var dueDate by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.dueDateEpochDay?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty())
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = existing?.dueDateEpochDay?.let { LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
    )
    val parsedCurrent = current.replace(',', '.').toDoubleOrNull() ?: 0.0
    val parsedTarget = target.replace(',', '.').toDoubleOrNull()
    val parsedDate = dueDate.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() }
    val dateValid = dueDate.isBlank() || parsedDate != null
    val canSave = title.isNotBlank() && unit.isNotBlank() && parsedTarget != null && parsedTarget > 0 && parsedCurrent >= 0 && dateValid

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                        .orEmpty()
                    showDatePicker = false
                }) { Text(stringResource(R.string.confirm_label)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(datePickerState) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader(stringResource(if (existing == null) R.string.new_goal else R.string.edit_goal), onBack) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(stringResource(R.string.live_preview), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(title.ifBlank { stringResource(R.string.goal_title) }, style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(
                    progress = { if ((parsedTarget ?: 0.0) <= 0.0) 0f else (parsedCurrent / (parsedTarget ?: 1.0)).coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                )
                Text("${formatGoalAmount(parsedCurrent)} / ${target.ifBlank { "0" }} $unit", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.goal_title)) },
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(300) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.goal_description)) },
                minLines = 3,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = goalNumberInput(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.current_progress)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = goalNumberInput(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.target_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        }
        item {
            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it.take(24) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.unit_label)) },
                singleLine = true,
            )
        }
        item {
            Surface(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(stringResource(R.string.due_date_optional), style = MaterialTheme.typography.labelSmall)
                    Text(dueDate.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Button(
                enabled = canSave,
                onClick = {
                    onSave(
                        GoalDraft(
                            title = title,
                            description = description,
                            currentValue = parsedCurrent,
                            targetValue = parsedTarget ?: return@Button,
                            unit = unit,
                            dueDateEpochDay = parsedDate,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(8.dp),
            ) { Text(stringResource(R.string.save_goal)) }
        }
    }
}

@Composable
internal fun ScreenHeader(title: String, onBack: () -> Unit, action: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back)) }
        Column(modifier = Modifier.weight(1f)) {
            Text("続く · TSUZUKU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(title, style = MaterialTheme.typography.headlineMedium)
        }
        action()
    }
}

private fun goalNumberInput(value: String): String =
    value.filter { it.isDigit() || it == '.' || it == ',' }.take(10)

private fun formatGoalAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

@Composable
private fun formatGoalDate(epochDay: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(epochDay, locale) {
        LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
}
