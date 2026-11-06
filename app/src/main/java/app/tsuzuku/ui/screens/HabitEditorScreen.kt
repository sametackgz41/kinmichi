package app.tsuzuku.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.data.local.CategoryEntity
import app.tsuzuku.model.GoalPeriod
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.HabitDraft
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.HealthMetric

private val habitColors = listOf(
    0xFF5068A9,
    0xFF3F7D5A,
    0xFFAD762C,
    0xFFA7565D,
    0xFF7A669E,
)

private val habitEmojis = listOf("✓", "💧", "📖", "🏃", "🧘", "🌿", "💪", "🍎", "😴", "✍️")

/**
 * All editable fields in one place. Sections read and write this object instead of receiving a
 * value and a callback per field, which keeps the composables small and the compiler happy.
 */
@Stable
internal class HabitEditorState(existing: Habit?) {
    var name by mutableStateOf(existing?.name.orEmpty())
    var emoji by mutableStateOf(existing?.emoji ?: "✓")
    var colorIndex by mutableStateOf(habitColors.indexOf(existing?.colorArgb).coerceAtLeast(0))
    var direction by mutableStateOf(existing?.direction ?: HabitDirection.Build)
    var type by mutableStateOf(existing?.type ?: HabitType.Check)
    var targetText by mutableStateOf(existing?.target?.let(::formatEditorAmount) ?: "1")
    var unit by mutableStateOf(existing?.unit.orEmpty())
    var stepText by mutableStateOf(existing?.step?.let(::formatEditorAmount) ?: "1")
    var minimumTargetText by mutableStateOf(existing?.minimumTarget?.takeIf { it > 0 }?.let(::formatEditorAmount).orEmpty())
    var maximumTargetText by mutableStateOf(existing?.maximumTarget?.let(::formatEditorAmount).orEmpty())
    var difficulty by mutableStateOf(existing?.difficulty ?: 2)
    var contextLabel by mutableStateOf(existing?.contextLabel.orEmpty())
    var intention by mutableStateOf(existing?.intention.orEmpty())
    var activeStartEpochDay by mutableStateOf(existing?.activeStartEpochDay)
    var activeEndEpochDay by mutableStateOf(existing?.activeEndEpochDay)
    var pausedUntilEpochDay by mutableStateOf(existing?.pausedUntilEpochDay)
    var repeatIntervalText by mutableStateOf((existing?.repeatIntervalDays ?: 1).toString())
    var tags by mutableStateOf(existing?.tags.orEmpty())
    var selectedDays by mutableStateOf(existing?.scheduleDaysMask?.toDaySelection() ?: "1111111")
    var goalPeriod by mutableStateOf(existing?.goalPeriod ?: GoalPeriod.Daily)
    var periodTargetText by mutableStateOf(existing?.periodTarget?.toString() ?: "3")
    var routineName by mutableStateOf(existing?.routineName.orEmpty())
    var prerequisiteId by mutableStateOf(existing?.prerequisiteHabitId ?: 0L)
    var reminderEnabled by mutableStateOf(existing?.reminderMinutes != null)
    var reminderTime by mutableStateOf(existing?.reminderMinutes?.toClockText() ?: "09:00")
    var repeatEnabled by mutableStateOf(existing?.reminderRepeatMinutes != null)
    var repeatMinutes by mutableStateOf(existing?.reminderRepeatMinutes?.toString() ?: "10")
    private val generalNote = existing?.generalNote.orEmpty()

    private val target get() = targetText.toLocaleDoubleOrNull() ?: 0.0
    private val periodTarget get() = periodTargetText.toIntOrNull() ?: 0

    val canSave: Boolean
        get() = name.isNotBlank() &&
            (type == HabitType.Check || target > 0.0) &&
            selectedDays.contains('1') &&
            (goalPeriod == GoalPeriod.Daily || periodTarget in 1..31) &&
            (!reminderEnabled || (reminderTime.toReminderMinutesOrNull() != null && (!repeatEnabled || (repeatMinutes.toIntOrNull() ?: 0) in 1..720)))

    fun changeType(newType: HabitType, timesUnit: String, minutesUnit: String) {
        type = newType
        when (newType) {
            HabitType.Check -> {
                targetText = "1"
                stepText = "1"
                unit = ""
            }
            HabitType.Count -> if (unit.isBlank()) unit = timesUnit
            HabitType.Duration -> {
                if (targetText == "1") targetText = "30"
                stepText = "5"
                unit = minutesUnit
            }
        }
    }

    fun toggleDay(index: Int) {
        val replacement = if (selectedDays[index] == '1') '0' else '1'
        selectedDays = selectedDays.replaceRange(index, index + 1, replacement.toString())
    }

    fun toDraft(): HabitDraft {
        val check = type == HabitType.Check
        return HabitDraft(
            name = name.trim(),
            emoji = emoji,
            colorArgb = habitColors[colorIndex],
            type = type,
            direction = direction,
            target = if (check) 1.0 else target,
            unit = if (check) "" else unit,
            step = if (check) 1.0 else (stepText.toLocaleDoubleOrNull() ?: 1.0).coerceAtLeast(0.1),
            scheduleDaysMask = selectedDays.toDaysMask(),
            goalPeriod = goalPeriod,
            periodTarget = if (goalPeriod == GoalPeriod.Daily) 1 else periodTarget,
            routineName = routineName,
            prerequisiteHabitId = prerequisiteId.takeIf { it > 0L },
            healthMetric = HealthMetric.None,
            reminderMinutes = if (reminderEnabled) reminderTime.toReminderMinutesOrNull() else null,
            reminderRepeatMinutes = if (reminderEnabled && repeatEnabled) repeatMinutes.toIntOrNull() else null,
            generalNote = generalNote,
            minimumTarget = if (check) 1.0 else minimumTargetText.toLocaleDoubleOrNull() ?: 0.0,
            maximumTarget = maximumTargetText.toLocaleDoubleOrNull(),
            difficulty = difficulty,
            contextLabel = contextLabel,
            intention = intention,
            activeStartEpochDay = activeStartEpochDay,
            activeEndEpochDay = activeEndEpochDay,
            repeatIntervalDays = repeatIntervalText.toIntOrNull()?.coerceIn(1, 365) ?: 1,
            pausedUntilEpochDay = pausedUntilEpochDay,
            tags = tags,
        )
    }
}

/**
 * One-screen habit editor: name, icon, how to track, which days and a reminder. Everything else
 * (quit habits, limits, periods, routines, dates, tags) sits under "More options".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorScreen(
    existing: Habit? = null,
    availableHabits: List<Habit> = emptyList(),
    customCategories: List<CategoryEntity> = emptyList(),
    onBack: () -> Unit,
    onSave: (HabitDraft) -> Unit,
) {
    val state = remember(existing?.id) { HabitEditorState(existing) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing == null) R.string.new_habit else R.string.edit_habit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            Button(
                enabled = state.canSave,
                onClick = { onSave(state.toDraft()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(stringResource(R.string.save_habit))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = 20.dp, vertical = 8.dp)),
        ) {
            HabitLivePreview(state)
            Spacer(Modifier.height(12.dp))
            NameAndLookSection(state)
            TrackingSection(state)
            DaysSection(state)
            Spacer(Modifier.height(16.dp))
            ReminderSection(state)
            TextButton(onClick = { showAdvanced = !showAdvanced }, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(if (showAdvanced) R.string.hide_advanced else R.string.show_advanced))
                Icon(
                    if (showAdvanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            AnimatedVisibility(visible = showAdvanced) {
                AdvancedSection(state, availableHabits.filterNot { it.id == existing?.id }, customCategories)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HabitLivePreview(state: HabitEditorState) {
    val color = Color(habitColors[state.colorIndex].toInt())
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).border(0.dp, Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Surface(color = color.copy(alpha = .16f), shape = CircleShape, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(state.emoji, style = MaterialTheme.typography.titleLarge) }
                }
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(state.name.ifBlank { stringResource(R.string.habit_name) }, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.type == HabitType.Check) stringResource(R.string.type_check) else "${state.targetText.ifBlank { "0" }} ${state.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.size(28.dp).border(2.dp, color, CircleShape))
        }
    }
}

@Composable
private fun NameAndLookSection(state: HabitEditorState) {
    OutlinedTextField(
        value = state.name,
        onValueChange = { state.name = it.take(80) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.habit_name)) },
        singleLine = true,
    )
    EditorLabel(R.string.icon_label)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        habitEmojis.forEach { option ->
            FilterChip(selected = state.emoji == option, onClick = { state.emoji = option }, label = { Text(option) })
        }
    }
    EditorLabel(R.string.color_label)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        habitColors.forEachIndexed { index, color ->
            FilterChip(
                selected = state.colorIndex == index,
                onClick = { state.colorIndex = index },
                label = { Text("●", color = Color(color.toInt())) },
                leadingIcon = if (state.colorIndex == index) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun TrackingSection(state: HabitEditorState) {
    val context = LocalContext.current
    EditorLabel(R.string.tracking_type)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        HabitType.entries.forEach { option ->
            FilterChip(
                selected = state.type == option,
                onClick = { state.changeType(option, context.getString(R.string.unit_times), context.getString(R.string.unit_minutes)) },
                label = { Text(stringResource(typeLabel(option))) },
            )
        }
    }
    if (state.type != HabitType.Check) {
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.targetText,
                onValueChange = { state.targetText = it.take(8) },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(if (state.direction == HabitDirection.Quit) R.string.ideal_target_upper else R.string.target_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.unit,
                onValueChange = { state.unit = it.take(20) },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.unit_label)) },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun DaysSection(state: HabitEditorState) {
    val days = stringArrayResource(R.array.weekday_abbreviations)
    EditorLabel(R.string.days_label)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        days.forEachIndexed { index, day ->
            FilterChip(selected = state.selectedDays[index] == '1', onClick = { state.toggleDay(index) }, label = { Text(day) })
        }
    }
}

@Composable
private fun ReminderSection(state: HabitEditorState) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    Card(shape = MaterialTheme.shapes.large) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.reminder_label), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.reminder_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = state.reminderEnabled,
                onCheckedChange = { enabled ->
                    state.reminderEnabled = enabled
                    if (enabled && Build.VERSION.SDK_INT >= 33 &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }
    }
    if (state.reminderEnabled) {
        Spacer(Modifier.height(10.dp))
        val invalid = state.reminderTime.toReminderMinutesOrNull() == null
        OutlinedTextField(
            value = state.reminderTime,
            onValueChange = { state.reminderTime = it.take(5) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.reminder_time)) },
            singleLine = true,
            isError = invalid,
            supportingText = { if (invalid) Text(stringResource(R.string.reminder_time_error)) },
        )
    }
}

@Composable
private fun AdvancedSection(state: HabitEditorState, otherHabits: List<Habit>, categories: List<CategoryEntity>) {
    Column {
        EditorLabel(R.string.intent_label)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.direction == HabitDirection.Build,
                onClick = { state.direction = HabitDirection.Build },
                label = { Text(stringResource(R.string.build_habit)) },
            )
            FilterChip(
                selected = state.direction == HabitDirection.Quit,
                onClick = { state.direction = HabitDirection.Quit },
                label = { Text(stringResource(R.string.quit_habit)) },
            )
        }
        if (state.direction == HabitDirection.Quit) {
            InfoCard(stringResource(if (state.type == HabitType.Check) R.string.bad_habit_clean_day else R.string.bad_habit_limit_desc))
        }
        if (state.type != HabitType.Check) {
            Spacer(Modifier.height(10.dp))
            NumberField(state.stepText, { state.stepText = it }, R.string.quick_increment, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(
                    state.minimumTargetText,
                    { state.minimumTargetText = it },
                    if (state.direction == HabitDirection.Quit) R.string.ideal_lower_limit else R.string.minimum_target,
                    Modifier.weight(1f),
                )
                NumberField(
                    state.maximumTargetText,
                    { state.maximumTargetText = it },
                    if (state.direction == HabitDirection.Quit) R.string.maximum_limit else R.string.maximum_target,
                    Modifier.weight(1f),
                )
            }
        }
        EditorLabel(R.string.goal_period)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            GoalPeriod.entries.forEach { option ->
                FilterChip(selected = state.goalPeriod == option, onClick = { state.goalPeriod = option }, label = { Text(stringResource(goalPeriodLabel(option))) })
            }
        }
        if (state.goalPeriod != GoalPeriod.Daily) {
            NumberField(
                state.periodTargetText,
                { state.periodTargetText = it.filter(Char::isDigit).take(2) },
                if (state.goalPeriod == GoalPeriod.Weekly) R.string.times_per_week else R.string.times_per_month,
                Modifier.fillMaxWidth(),
            )
        }
        EditorLabel(R.string.difficulty)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { value ->
                FilterChip(
                    selected = state.difficulty == value,
                    onClick = { state.difficulty = value },
                    label = { Text(stringResource(when (value) { 1 -> R.string.easy; 2 -> R.string.medium; else -> R.string.hard })) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.routineName,
            onValueChange = { state.routineName = it.take(40) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.routine_name)) },
            singleLine = true,
        )
        if (otherHabits.isNotEmpty()) {
            EditorLabel(R.string.after_habit_optional)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(selected = state.prerequisiteId == 0L, onClick = { state.prerequisiteId = 0L }, label = { Text(stringResource(R.string.none_label)) })
                otherHabits.forEach { habit ->
                    FilterChip(selected = state.prerequisiteId == habit.id, onClick = { state.prerequisiteId = habit.id }, label = { Text("${habit.emoji} ${habit.name}") })
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.intention,
            onValueChange = { state.intention = it.take(240) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.intention_sentence)) },
            placeholder = { Text(stringResource(R.string.intention_example)) },
            minLines = 2,
        )
        if (state.reminderEnabled) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.repeat_reminder), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.repeat_reminder_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.repeatEnabled, onCheckedChange = { state.repeatEnabled = it })
            }
            if (state.repeatEnabled) {
                NumberField(state.repeatMinutes, { state.repeatMinutes = it.filter(Char::isDigit).take(3) }, R.string.repeat_minutes, Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(12.dp))
        FlexibleScheduleFields(
            repeatInterval = state.repeatIntervalText,
            onRepeatIntervalChange = { state.repeatIntervalText = it.filter(Char::isDigit).take(3) },
            activeStartEpochDay = state.activeStartEpochDay,
            onActiveStartChange = { state.activeStartEpochDay = it },
            activeEndEpochDay = state.activeEndEpochDay,
            onActiveEndChange = { state.activeEndEpochDay = it },
            pausedUntilEpochDay = state.pausedUntilEpochDay,
            onPausedUntilChange = { state.pausedUntilEpochDay = it },
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.tags,
            onValueChange = { state.tags = it.take(240) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.tags_label)) },
            supportingText = { Text(stringResource(R.string.tags_help)) },
            singleLine = true,
        )
        CategorySuggestions(categories, state.tags) { state.tags = it }
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, labelRes: Int, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(8)) },
        modifier = modifier,
        label = { Text(stringResource(labelRes)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

@Composable
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier.padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Text(text, modifier = Modifier.padding(15.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun EditorLabel(textRes: Int) {
    Text(
        stringResource(textRes),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

@androidx.annotation.StringRes
private fun goalPeriodLabel(period: GoalPeriod): Int = when (period) {
    GoalPeriod.Daily -> R.string.daily
    GoalPeriod.Weekly -> R.string.weekly
    GoalPeriod.Monthly -> R.string.monthly
}

private fun typeLabel(type: HabitType): Int = when (type) {
    HabitType.Check -> R.string.type_check
    HabitType.Count -> R.string.type_count
    HabitType.Duration -> R.string.type_duration
}

private fun String.toLocaleDoubleOrNull(): Double? = replace(',', '.').toDoubleOrNull()

private fun String.toDaysMask(): Int = foldIndexed(0) { index, mask, char ->
    if (char == '1') mask or (1 shl index) else mask
}

private fun Int.toDaySelection(): String = (0..6).joinToString("") { index ->
    if (this and (1 shl index) != 0) "1" else "0"
}

private fun Int.toClockText(): String = "%02d:%02d".format(this / 60, this % 60)

internal fun String.toReminderMinutesOrNull(): Int? {
    val parts = trim().split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun formatEditorAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
