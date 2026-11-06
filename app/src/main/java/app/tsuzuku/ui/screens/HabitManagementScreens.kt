package app.tsuzuku.ui.screens

import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import app.tsuzuku.R
import app.tsuzuku.model.Goal
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitLevel
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.HabitHistoryRecord
import app.tsuzuku.model.HabitDayStatus
import app.tsuzuku.model.HabitGoalContributionMode
import app.tsuzuku.model.HabitGoalLink
import app.tsuzuku.model.HabitLibraryLink
import app.tsuzuku.model.LibraryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habit: Habit,
    records: List<HabitHistoryRecord> = emptyList(),
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSaveNote: (String) -> Unit,
    onArchive: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    linkedGoals: List<Pair<Goal, HabitGoalLink>> = emptyList(),
    linkedContents: List<Pair<LibraryItem, HabitLibraryLink>> = emptyList(),
    availableGoals: List<Goal> = emptyList(),
    availableContents: List<LibraryItem> = emptyList(),
    habitLevels: List<HabitLevel> = emptyList(),
    onLinkGoal: (goalId: Long, mode: HabitGoalContributionMode, value: Double) -> Unit = { _, _, _ -> },
    onUnlinkGoal: (goalId: Long) -> Unit = {},
    onLinkContent: (contentId: Long, multiplier: Double) -> Unit = { _, _ -> },
    onUnlinkContent: (contentId: Long) -> Unit = {},
    onSaveLevels: (List<HabitLevel>) -> Unit = {},
    onAdvanceLevel: (Int) -> Unit = {},
) {
    var note by rememberSaveable(habit.id, habit.generalNote) { mutableStateOf(habit.generalNote) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showLinkGoalDialog by remember { mutableStateOf(false) }
    var showLinkContentDialog by remember { mutableStateOf(false) }
    var showLevelsDialog by remember { mutableStateOf(false) }
    var levelToAdvance by remember { mutableStateOf<HabitLevel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.habit_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_habit))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(habit.emoji, style = MaterialTheme.typography.displaySmall)
                        Column(Modifier.weight(1f)) {
                            Text(habit.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                habitGoalText(habit),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item { HabitYearCard(habit, records) }
            item {
                Column {
                    Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailLine(stringResource(R.string.days_label), scheduleSummaryForDetail(habit.scheduleDaysMask))
                        DetailLine(
                            stringResource(R.string.reminder_label),
                            habit.reminderMinutes?.let(::clockText) ?: stringResource(R.string.off_label),
                        )
                        habit.reminderRepeatMinutes?.let {
                            DetailLine(stringResource(R.string.repeat_reminder), stringResource(R.string.minutes_value, it))
                        }
                    }
                }
            }
            // Linked Goals Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.linked_goals), style = MaterialTheme.typography.titleMedium)
                    FilledTonalButton(onClick = { showLinkGoalDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.link_goal))
                    }
                }
            }
            if (linkedGoals.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_linked_goals),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(linkedGoals, key = { it.first.id }) { (goal, link) ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(goal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                val modeDesc = if (link.contributionMode == HabitGoalContributionMode.HabitAmount) {
                                    stringResource(R.string.contribution_by_habit_amount)
                                } else {
                                    stringResource(R.string.contribution_plus, link.contributionValue)
                                }
                                Text(modeDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onUnlinkGoal(goal.id) }) {
                                Icon(Icons.Rounded.LinkOff, contentDescription = stringResource(R.string.unlink), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Linked Library Content Section

            // Habit Levels Section
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.growth_levels), style = MaterialTheme.typography.titleMedium)
                    }
                    FilledTonalButton(onClick = { showLevelsDialog = true }) {
                        Icon(if (habitLevels.isEmpty()) Icons.Rounded.Add else Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (habitLevels.isEmpty()) stringResource(R.string.add_level) else stringResource(R.string.edit_action))
                    }
                }
            }
            if (habitLevels.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.growth_levels_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = { showLevelsDialog = true }) {
                                Text(stringResource(R.string.configure_stages))
                            }
                        }
                    }
                }
            } else {
                items(habitLevels, key = { it.id }) { level ->
                    val isCurrent = level.levelNumber == habit.currentLevel
                    val isPassed = level.levelNumber < habit.currentLevel
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = if (isCurrent) CardDefaults.outlinedCardBorder() else null,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else if (isPassed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${level.levelNumber}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(level.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    if (isCurrent) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                        ) {
                                            Text(
                                                stringResource(R.string.active_level),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                    } else if (isPassed) {
                                        Text(stringResource(R.string.completed_check), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                Text(
                                    stringResource(R.string.level_target_value, formatLibraryAmount(level.target), habit.unit) + if (level.rewardNote.isNotBlank()) " · ${level.rewardNote}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (!isCurrent && !isPassed) {
                                FilledTonalButton(onClick = { levelToAdvance = level }) {
                                    Text(stringResource(R.string.switch_to_level))
                                }
                            }
                        }
                    }
                }

                val nextLevel = habitLevels.firstOrNull { it.levelNumber == habit.currentLevel + 1 }
                if (nextLevel != null) {
                    item {
                        Button(
                            onClick = { levelToAdvance = nextLevel },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.level_up_to, nextLevel.name, formatLibraryAmount(nextLevel.target), habit.unit))
                        }
                    }
                } else if (habitLevels.isNotEmpty() && habit.currentLevel >= habitLevels.maxOf { it.levelNumber }) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("👑", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(R.string.top_level_reached),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            item { Text(stringResource(R.string.general_note), style = MaterialTheme.typography.titleLarge) }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    label = { Text(stringResource(R.string.habit_note_hint)) },
                )
            }
            item {
                Button(
                    onClick = { onSaveNote(note) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.save_note)) }
            }
            if (records.isNotEmpty()) {
                item { Text(stringResource(R.string.recent_timeline), style = MaterialTheme.typography.titleLarge) }
                items(records.sortedByDescending { it.date }.take(30).size) { index ->
                    val record = records.sortedByDescending { it.date }.take(30)[index]
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(record.date.toString(), style = MaterialTheme.typography.titleSmall)
                            Text(habitStatusLabel(record.status), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${formatLibraryAmount(record.progress)} ${habit.unit}", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            item {
                OutlinedButton(onClick = onDuplicate, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.duplicate_habit))
                }
            }
            item {
                OutlinedButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.archive_habit))
                }
            }
            item {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.delete_habit), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showLinkGoalDialog) {
        val unlinked = availableGoals.filter { goal -> linkedGoals.none { it.first.id == goal.id } }
        var selectedGoalId by remember { mutableStateOf(unlinked.firstOrNull()?.id) }
        var mode by remember { mutableStateOf(HabitGoalContributionMode.HabitAmount) }
        var valueText by remember { mutableStateOf("1.0") }

        AlertDialog(
            onDismissRequest = { showLinkGoalDialog = false },
            title = { Text(stringResource(R.string.link_goal)) },
            text = {
                if (unlinked.isEmpty()) {
                    Text(stringResource(R.string.no_more_active_goals_to_link))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.select_goal), style = MaterialTheme.typography.labelLarge)
                        unlinked.forEach { goal ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selectedGoalId = goal.id }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selectedGoalId == goal.id, onClick = { selectedGoalId = goal.id })
                                Text(goal.title, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        Text(stringResource(R.string.contribution_mode), style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mode == HabitGoalContributionMode.HabitAmount,
                                onClick = { mode = HabitGoalContributionMode.HabitAmount },
                            )
                            Text(stringResource(R.string.habit_amount), modifier = Modifier.padding(start = 6.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mode == HabitGoalContributionMode.FixedValue,
                                onClick = { mode = HabitGoalContributionMode.FixedValue },
                            )
                            Text(stringResource(R.string.fixed_value), modifier = Modifier.padding(start = 6.dp))
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
                            val id = selectedGoalId ?: return@TextButton
                            val v = valueText.toDoubleOrNull() ?: 1.0
                            onLinkGoal(id, mode, v)
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

    if (showLinkContentDialog) {
        val unlinked = availableContents.filter { content -> linkedContents.none { it.first.content.id == content.content.id } }
        var selectedContentId by remember { mutableStateOf(unlinked.firstOrNull()?.content?.id) }
        var multiplierText by remember { mutableStateOf("1.0") }

        AlertDialog(
            onDismissRequest = { showLinkContentDialog = false },
            title = { Text(stringResource(R.string.link_library_content)) },
            text = {
                if (unlinked.isEmpty()) {
                    Text(stringResource(R.string.no_more_content_to_link))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.select_content), style = MaterialTheme.typography.labelLarge)
                        unlinked.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selectedContentId = item.content.id }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selectedContentId == item.content.id, onClick = { selectedContentId = item.content.id })
                                Text(item.content.title, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        OutlinedTextField(
                            value = multiplierText,
                            onValueChange = { multiplierText = it },
                            label = { Text(stringResource(R.string.progress_multiplier_label)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {
                if (unlinked.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val id = selectedContentId ?: return@TextButton
                            val m = multiplierText.toDoubleOrNull() ?: 1.0
                            onLinkContent(id, m)
                            showLinkContentDialog = false
                        },
                    ) { Text(stringResource(R.string.link_action)) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkContentDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_habit)) },
            text = { Text(stringResource(R.string.delete_habit_confirmation, habit.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.delete_label), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    levelToAdvance?.let { targetLevel ->
        AlertDialog(
            onDismissRequest = { levelToAdvance = null },
            title = { Text(stringResource(R.string.level_up_title)) },
            text = {
                Text(
                    stringResource(R.string.level_up_message, targetLevel.name, formatLibraryAmount(targetLevel.target), habit.unit)
                )
            },
            confirmButton = {
                Button(onClick = {
                    onAdvanceLevel(targetLevel.levelNumber)
                    levelToAdvance = null
                }) {
                    Text(stringResource(R.string.level_up_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { levelToAdvance = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showLevelsDialog) {
        HabitLevelsDialog(
            initialLevels = habitLevels,
            unit = habit.unit,
            onDismiss = { showLevelsDialog = false },
            onSave = { levels ->
                onSaveLevels(levels)
                showLevelsDialog = false
            },
        )
    }
}

@Composable
fun HabitLevelsDialog(
    initialLevels: List<HabitLevel>,
    unit: String,
    onDismiss: () -> Unit,
    onSave: (List<HabitLevel>) -> Unit,
) {
    data class LevelDraft(val name: String, val target: String, val rewardNote: String)
    val context = LocalContext.current
    val defaultLevelNames = listOf(
        stringResource(R.string.level_beginner),
        stringResource(R.string.level_intermediate),
        stringResource(R.string.level_advanced),
    )
    var items by remember {
        mutableStateOf(
            if (initialLevels.isEmpty()) {
                listOf(
                    LevelDraft(defaultLevelNames[0], "1.0", ""),
                    LevelDraft(defaultLevelNames[1], "3.0", ""),
                    LevelDraft(defaultLevelNames[2], "5.0", ""),
                )
            } else {
                initialLevels.map { LevelDraft(it.name, formatLibraryAmount(it.target), it.rewardNote) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_growth_levels)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(items) { index, item ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(stringResource(R.string.level_number, index + 1), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                if (items.size > 1) {
                                    IconButton(
                                        onClick = {
                                            items = items.toMutableList().apply { removeAt(index) }
                                        },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { newName ->
                                    items = items.toMutableList().apply { set(index, item.copy(name = newName)) }
                                },
                                label = { Text(stringResource(R.string.level_title)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = item.target,
                                    onValueChange = { newTarget ->
                                        items = items.toMutableList().apply { set(index, item.copy(target = newTarget)) }
                                    },
                                    label = { Text(stringResource(R.string.target_with_unit, unit)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = item.rewardNote,
                                    onValueChange = { newNote ->
                                        items = items.toMutableList().apply { set(index, item.copy(rewardNote = newNote)) }
                                    },
                                    label = { Text(stringResource(R.string.reward_note)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = {
                            val nextNum = items.size + 1
                            items = items + LevelDraft(context.getString(R.string.level_number_next, nextNum), "${nextNum * 2}.0", "")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_new_level))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val converted = items.mapIndexedNotNull { index, item ->
                        val target = item.target.toDoubleOrNull() ?: return@mapIndexedNotNull null
                        HabitLevel(
                            id = 0L,
                            habitId = 0L,
                            levelNumber = index + 1,
                            name = item.name.ifBlank { context.getString(R.string.level_number, index + 1) },
                            target = target,
                            rewardNote = item.rewardNote,
                        )
                    }
                    onSave(converted)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.save_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitOrderScreen(
    habits: List<Habit>,
    onBack: () -> Unit,
    onMove: (Long, Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reorder_habits)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text(stringResource(R.string.reorder_habits_description), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            itemsIndexed(habits, key = { _, habit -> habit.id }) { index, habit ->
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(habit.emoji, style = MaterialTheme.typography.headlineSmall)
                        Text(habit.name, modifier = Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { onMove(habit.id, -1) }, enabled = index > 0) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.move_up))
                        }
                        IconButton(onClick = { onMove(habit.id, 1) }, enabled = index < habits.lastIndex) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = stringResource(R.string.move_down))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun habitGoalText(habit: Habit): String = when (habit.type) {
    HabitType.Check -> stringResource(R.string.type_check)
    HabitType.Count, HabitType.Duration -> "${formatLibraryAmount(habit.target)} ${habit.unit}"
}

@Composable
private fun scheduleSummaryForDetail(daysMask: Int): String = when (daysMask) {
    127 -> stringResource(R.string.every_day)
    31 -> stringResource(R.string.weekdays)
    96 -> stringResource(R.string.weekends)
    else -> stringResource(R.string.selected_days)
}

private fun clockText(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

@Composable
private fun habitStatusLabel(status: HabitDayStatus): String = stringResource(when (status) {
    HabitDayStatus.Pending -> R.string.pending_label
    HabitDayStatus.Completed -> R.string.status_completed
    HabitDayStatus.Failed -> R.string.failed_label
    HabitDayStatus.Skipped -> R.string.skipped_label
})

internal fun formatLibraryAmount(value: Double): String = formatHabitAmount(value)
