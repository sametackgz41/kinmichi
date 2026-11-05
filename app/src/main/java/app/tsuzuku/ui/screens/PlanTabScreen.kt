package app.tsuzuku.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.PlanTask
import app.tsuzuku.model.PlanTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class PlanFilter { Today, Upcoming, Inbox, Completed }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlanTabScreen(
    tasks: List<PlanTask>,
    templates: List<PlanTemplate>,
    onToggle: (PlanTask) -> Unit,
    onOpen: (Long) -> Unit,
    onMoveTomorrow: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onUseTemplate: (PlanTemplate) -> Unit,
    onReorder: (Long, Int) -> Unit,
    onBulkMove: (Set<Long>, Long?) -> Unit,
    onBulkComplete: (List<PlanTask>) -> Unit,
    onBulkDelete: (Set<Long>) -> Unit,
    onBulkAddTags: (List<PlanTask>, String) -> Unit,
    onOpenProjects: () -> Unit = {},
    onOpenPlanAssistant: () -> Unit = {},
) {
    var filter by remember { mutableStateOf(PlanFilter.Today) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showCleanup by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var bulkTags by remember { mutableStateOf("") }
    val today = LocalDate.now().toEpochDay()
    val visible = tasks.filter { task ->
        when (filter) {
            PlanFilter.Today -> !task.completed && task.scheduledEpochDay != null && task.scheduledEpochDay <= today
            PlanFilter.Upcoming -> !task.completed && (task.scheduledEpochDay ?: Long.MIN_VALUE) > today
            PlanFilter.Inbox -> !task.completed && task.scheduledEpochDay == null
            PlanFilter.Completed -> task.completed
        }
    }.sortedWith(compareBy<PlanTask> { it.startMinutes == null }.thenBy { it.startMinutes ?: Int.MAX_VALUE }.thenByDescending { it.priority }.thenBy { it.sortOrder })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.plan_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenPlanAssistant) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = stringResource(R.string.ai_plan_assistant))
                    }
                    IconButton(onClick = onOpenProjects) {
                        Icon(Icons.Rounded.AccountTree, contentDescription = stringResource(R.string.projects_title))
                    }
                }
            }
        }
        if (selectedIds.isNotEmpty()) item {
            Column {
                Text(stringResource(R.string.selected_count, selectedIds.size), style = MaterialTheme.typography.titleMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = { onBulkMove(selectedIds, LocalDate.now().plusDays(1).toEpochDay()); selectedIds = emptySet() }, label = { Text(stringResource(R.string.move_all_tomorrow)) })
                    AssistChip(onClick = { onBulkMove(selectedIds, null); selectedIds = emptySet() }, label = { Text(stringResource(R.string.move_to_inbox)) })
                    AssistChip(onClick = { onBulkComplete(tasks.filter { it.id in selectedIds }); selectedIds = emptySet() }, label = { Text(stringResource(R.string.complete_selected)) })
                    AssistChip(onClick = { showTagDialog = true }, label = { Text(stringResource(R.string.add_tags_selected)) })
                    AssistChip(onClick = { onBulkDelete(selectedIds); selectedIds = emptySet() }, label = { Text(stringResource(R.string.delete_selected)) })
                    TextButton(onClick = { selectedIds = emptySet() }) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
        if (filter == PlanFilter.Today && templates.isNotEmpty()) item {
            Column {
                Text(stringResource(R.string.task_templates), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { template ->
                        AssistChip(onClick = { onUseTemplate(template) }, label = { Text("+ ${template.title}") })
                    }
                }
            }
        }
        val overdueCount = visible.count { (it.scheduledEpochDay ?: today) < today }
        if (filter == PlanFilter.Today && overdueCount > 0) item {
            TextButton(onClick = { showCleanup = true }) {
                Text(stringResource(R.string.overdue_cleanup, overdueCount), color = MaterialTheme.colorScheme.error)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlanFilter.entries.forEach { value ->
                    FilterChip(
                        selected = filter == value,
                        onClick = { filter = value },
                        label = { Text(stringResource(value.labelRes())) },
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.plan_empty), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.plan_empty_detail), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(visible.size, key = { visible[it].id }) { index ->
                val task = visible[index]
                var dragDistance by remember(task.id) { mutableStateOf(0f) }
                val dismissState = rememberSwipeToDismissBoxState()
                LaunchedEffect(dismissState.currentValue) {
                    when (dismissState.currentValue) {
                        SwipeToDismissBoxValue.StartToEnd -> onToggle(task)
                        SwipeToDismissBoxValue.EndToStart -> onMoveTomorrow(task.id)
                        SwipeToDismissBoxValue.Settled -> Unit
                    }
                }
                SwipeToDismissBox(
                    modifier = Modifier.animateItem(),
                    state = dismissState,
                    enableDismissFromStartToEnd = selectedIds.isEmpty(),
                    enableDismissFromEndToStart = selectedIds.isEmpty() && !task.completed,
                    backgroundContent = {
                        val complete = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                        Box(
                            Modifier.fillMaxSize().background(if (complete) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 22.dp),
                            contentAlignment = if (complete) Alignment.CenterStart else Alignment.CenterEnd,
                        ) {
                            Text(stringResource(if (complete) R.string.swipe_complete else R.string.swipe_tomorrow), fontWeight = FontWeight.Bold)
                        }
                    },
                ) { Column {
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(
                            onClick = {
                                if (selectedIds.isEmpty()) onOpen(task.id)
                                else selectedIds = if (task.id in selectedIds) selectedIds - task.id else selectedIds + task.id
                            },
                            onLongClick = { selectedIds = if (task.id in selectedIds) selectedIds - task.id else selectedIds + task.id },
                        ).padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = if (selectedIds.isEmpty()) task.completed else task.id in selectedIds,
                            onCheckedChange = {
                                if (selectedIds.isEmpty()) onToggle(task)
                                else selectedIds = if (task.id in selectedIds) selectedIds - task.id else selectedIds + task.id
                            },
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(task.title, style = MaterialTheme.typography.titleMedium)
                            val meta = buildList {
                                task.startMinutes?.let { start -> add("%02d:%02d".format(start / 60, start % 60) + task.endMinutes?.let { end -> "–%02d:%02d".format(end / 60, end % 60) }.orEmpty()) }
                                task.estimatedMinutes?.let { add(stringResource(R.string.minute_count, it)) }
                                task.scheduledEpochDay?.let { add(LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d MMM"))) }
                                if ((task.scheduledEpochDay ?: today) < today) add(stringResource(R.string.overdue))
                                if (task.priority > 1) add(stringResource(R.string.priority_value, task.priority))
                            }.joinToString(" · ")
                            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!task.completed) IconButton(onClick = { onMoveTomorrow(task.id) }) { Icon(Icons.Rounded.Schedule, stringResource(R.string.move_tomorrow)) }
                        IconButton(onClick = { onDelete(task.id) }) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete_label)) }
                        Icon(
                            Icons.Rounded.DragHandle,
                            contentDescription = stringResource(R.string.drag_to_reorder),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    dragDistance += delta
                                    if (dragDistance > 36f) { onReorder(task.id, 1); dragDistance = 0f }
                                    if (dragDistance < -36f) { onReorder(task.id, -1); dragDistance = 0f }
                                },
                                onDragStopped = { dragDistance = 0f },
                            ),
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                } }
            }
        }
    }

    if (showCleanup) {
        val overdue = tasks.filter { !it.completed && (it.scheduledEpochDay ?: today) < today }
        AlertDialog(
            onDismissRequest = { showCleanup = false },
            title = { Text(stringResource(R.string.day_end_cleanup)) },
            text = { Text(stringResource(R.string.day_end_cleanup_detail, overdue.size)) },
            confirmButton = {
                TextButton(onClick = { onBulkMove(overdue.mapTo(mutableSetOf()) { it.id }, LocalDate.now().plusDays(1).toEpochDay()); showCleanup = false }) { Text(stringResource(R.string.move_all_tomorrow)) }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = { onBulkMove(overdue.mapTo(mutableSetOf()) { it.id }, null); showCleanup = false }) { Text(stringResource(R.string.move_to_inbox)) }
                    TextButton(onClick = { onBulkComplete(overdue); showCleanup = false }) { Text(stringResource(R.string.complete_selected)) }
                }
            },
        )
    }
    if (showTagDialog) AlertDialog(
        onDismissRequest = { showTagDialog = false },
        title = { Text(stringResource(R.string.add_tags_selected)) },
        text = { OutlinedTextField(bulkTags, { bulkTags = it.take(240) }, label = { Text(stringResource(R.string.tags_label)) }, supportingText = { Text(stringResource(R.string.tags_help)) }) },
        confirmButton = {
            TextButton(enabled = bulkTags.isNotBlank(), onClick = {
                onBulkAddTags(tasks.filter { it.id in selectedIds }, bulkTags)
                bulkTags = ""
                selectedIds = emptySet()
                showTagDialog = false
            }) { Text(stringResource(R.string.add_label)) }
        },
        dismissButton = { TextButton(onClick = { showTagDialog = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun PlanFilter.labelRes() = when (this) {
    PlanFilter.Today -> R.string.today
    PlanFilter.Upcoming -> R.string.upcoming
    PlanFilter.Inbox -> R.string.inbox
    PlanFilter.Completed -> R.string.status_completed
}
