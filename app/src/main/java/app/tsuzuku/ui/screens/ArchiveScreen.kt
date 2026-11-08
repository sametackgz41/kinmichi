package app.tsuzuku.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.data.TrashedHabit
import app.tsuzuku.model.Habit

private sealed interface Confirm {
    data class Trash(val habit: Habit) : Confirm
    data class Forever(val habit: Habit) : Confirm
    data object Empty : Confirm
}

/**
 * Two tabs: the archive (habits put away, history kept) and the trash (deleted habits, kept
 * 30 days before they go for good).
 */
@Composable
internal fun ArchiveScreen(
    habits: List<Habit>,
    trashed: List<TrashedHabit>,
    onBack: () -> Unit,
    onRestore: (Long) -> Unit,
    onTrash: (Long) -> Unit,
    onDeleteForever: (Long) -> Unit,
    onEmptyTrash: () -> Unit,
    startInTrash: Boolean = false,
) {
    var inTrash by rememberSaveable { mutableStateOf(startInTrash) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                Text(stringResource(R.string.trash_page_title), style = MaterialTheme.typography.titleLarge)
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                SegmentedButton(
                    selected = !inTrash, onClick = { inTrash = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    label = { Text(stringResource(R.string.trash_tab_archive, habits.size), maxLines = 1) },
                )
                SegmentedButton(
                    selected = inTrash, onClick = { inTrash = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    label = { Text(stringResource(R.string.trash_tab_trash, trashed.size), maxLines = 1) },
                )
            }
            Text(
                stringResource(if (inTrash) R.string.trash_description else R.string.archive_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 4.dp),
            )
        }
        val empty = if (inTrash) trashed.isEmpty() else habits.isEmpty()
        if (empty) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (inTrash) "🗑️" else "🗃️", fontSize = 48.sp)
                    Text(
                        stringResource(if (inTrash) R.string.trash_empty else R.string.archive_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
        if (inTrash) {
            items(trashed, key = { "t" + it.habit.id }) { item ->
                val days = item.daysLeft()
                HabitRow(
                    habit = item.habit,
                    subtitle = pluralStringResource(R.plurals.trash_days_left, days, days),
                    urgent = days <= 7,
                    onDelete = { confirm = Confirm.Forever(item.habit) },
                    deleteIcon = true,
                    restoreIcon = { Icon(Icons.Rounded.Restore, null, modifier = Modifier.size(18.dp)) },
                    onRestore = { onRestore(item.habit.id) },
                )
            }
            if (trashed.isNotEmpty()) item {
                OutlinedButton(
                    onClick = { confirm = Confirm.Empty },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ) { Text(stringResource(R.string.trash_empty_action), color = MaterialTheme.colorScheme.error) }
            }
        } else {
            items(habits, key = { "a" + it.id }) { habit ->
                HabitRow(
                    habit = habit,
                    subtitle = stringResource(R.string.archive_since, habit.createdOn.toString()),
                    urgent = false,
                    onDelete = { confirm = Confirm.Trash(habit) },
                    deleteIcon = false,
                    restoreIcon = { Icon(Icons.Rounded.Unarchive, null, modifier = Modifier.size(18.dp)) },
                    onRestore = { onRestore(habit.id) },
                )
            }
        }
    }
    when (val c = confirm) {
        is Confirm.Trash -> ConfirmDialog(
            title = stringResource(R.string.delete_habit),
            text = stringResource(R.string.trash_move_confirmation, c.habit.name),
            action = stringResource(R.string.delete_habit),
            onConfirm = { onTrash(c.habit.id) },
            onDismiss = { confirm = null },
        )
        is Confirm.Forever -> ConfirmDialog(
            title = stringResource(R.string.trash_delete_forever),
            text = stringResource(R.string.delete_habit_confirmation, c.habit.name),
            action = stringResource(R.string.trash_delete_forever),
            onConfirm = { onDeleteForever(c.habit.id) },
            onDismiss = { confirm = null },
        )
        Confirm.Empty -> ConfirmDialog(
            title = stringResource(R.string.trash_empty_action),
            text = pluralStringResource(R.plurals.trash_empty_confirmation, trashed.size, trashed.size),
            action = stringResource(R.string.trash_empty_action),
            onConfirm = onEmptyTrash,
            onDismiss = { confirm = null },
        )
        null -> Unit
    }
}

@Composable
private fun HabitRow(
    habit: Habit,
    subtitle: String,
    urgent: Boolean,
    onDelete: () -> Unit,
    deleteIcon: Boolean,
    restoreIcon: @Composable () -> Unit,
    onRestore: () -> Unit,
) {
    val accent = Color(habit.colorArgb.toInt())
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Text(habit.emoji, fontSize = 22.sp) }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                if (deleteIcon) Icons.Rounded.DeleteForever else Icons.Rounded.DeleteOutline,
                stringResource(if (deleteIcon) R.string.trash_delete_forever else R.string.delete_habit),
                tint = MaterialTheme.colorScheme.error,
            )
        }
        FilledTonalButton(onClick = onRestore) {
            restoreIcon()
            Text(stringResource(R.string.archive_restore), modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, text: String, action: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) { Text(action, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
