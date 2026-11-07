package app.tsuzuku.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
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
    var tinyVersion by mutableStateOf(existing?.tinyVersion.orEmpty())
    var healthMetric by mutableStateOf(existing?.healthMetric ?: HealthMetric.None)
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
            healthMetric = if (check) HealthMetric.None else healthMetric,
            reminderMinutes = if (reminderEnabled) reminderTime.toReminderMinutesOrNull() else null,
            reminderRepeatMinutes = if (reminderEnabled && repeatEnabled) repeatMinutes.toIntOrNull() else null,
            generalNote = generalNote,
            minimumTarget = if (check) 1.0 else minimumTargetText.toLocaleDoubleOrNull() ?: 0.0,
            maximumTarget = maximumTargetText.toLocaleDoubleOrNull(),
            difficulty = difficulty,
            contextLabel = contextLabel,
            intention = intention,
            tinyVersion = tinyVersion,
            activeStartEpochDay = activeStartEpochDay,
            activeEndEpochDay = activeEndEpochDay,
            repeatIntervalDays = repeatIntervalText.toIntOrNull()?.coerceIn(1, 365) ?: 1,
            pausedUntilEpochDay = pausedUntilEpochDay,
            tags = tags,
        )
    }

    private fun snapshot(): List<Any?> = listOf(
        name, emoji, colorIndex, direction.name, type.name, targetText, unit, stepText,
        minimumTargetText, maximumTargetText, difficulty, contextLabel, intention,
        activeStartEpochDay, activeEndEpochDay, pausedUntilEpochDay, repeatIntervalText, tags,
        selectedDays, goalPeriod.name, periodTargetText, routineName, prerequisiteId,
        reminderEnabled, reminderTime, repeatEnabled, repeatMinutes, tinyVersion, healthMetric.name,
    )

    private fun restore(values: List<Any?>) {
        var i = 0
        fun <T> next(): T {
            @Suppress("UNCHECKED_CAST")
            return values[i++] as T
        }
        name = next(); emoji = next(); colorIndex = next()
        direction = HabitDirection.valueOf(next()); type = HabitType.valueOf(next())
        targetText = next(); unit = next(); stepText = next()
        minimumTargetText = next(); maximumTargetText = next(); difficulty = next()
        contextLabel = next(); intention = next()
        activeStartEpochDay = next(); activeEndEpochDay = next(); pausedUntilEpochDay = next()
        repeatIntervalText = next(); tags = next(); selectedDays = next()
        goalPeriod = GoalPeriod.valueOf(next()); periodTargetText = next(); routineName = next()
        prerequisiteId = next(); reminderEnabled = next(); reminderTime = next()
        repeatEnabled = next(); repeatMinutes = next()
        if (i < values.size) tinyVersion = next()
        if (i < values.size) healthMetric = HealthMetric.valueOf(next())
    }

    companion object {
        /** Keeps unsaved edits across rotation and process death. */
        fun saver(existing: Habit?): Saver<HabitEditorState, Any> = listSaver(
            save = { it.snapshot() },
            restore = { values -> HabitEditorState(existing).also { it.restore(values) } },
        )
    }
}

/**
 * One-screen habit editor: name, icon, how to track, which days and a reminder. Everything else
 * (quit habits, limits, periods, routines, dates, tags) sits under "More options".
 */
@OptIn(ExperimentalMaterial3Api::class)
/** Step by step for a new habit; one page of cards when editing an existing one. */
internal enum class EditorLayout { Cards, Steps }

@Composable
fun HabitEditorScreen(
    existing: Habit? = null,
    availableHabits: List<Habit> = emptyList(),
    customCategories: List<CategoryEntity> = emptyList(),
    onBack: () -> Unit,
    onSave: (HabitDraft) -> Unit,
) = HabitEditorContent(existing, availableHabits, customCategories, onBack, onSave)

@Composable
internal fun HabitEditorContent(
    existing: Habit? = null,
    availableHabits: List<Habit> = emptyList(),
    customCategories: List<CategoryEntity> = emptyList(),
    onBack: () -> Unit,
    onSave: (HabitDraft) -> Unit,
    layout: EditorLayout = EditorLayout.Steps,
    initialStep: Int = 0,
    openGroup: AdvancedGroup? = null,
) {
    val state = rememberSaveable(existing?.id, saver = HabitEditorState.saver(existing)) { HabitEditorState(existing) }
    val others = availableHabits.filterNot { it.id == existing?.id }
    // Steps only for a new habit; editing shows everything on one page.
    val effective = if (layout == EditorLayout.Steps && existing != null) EditorLayout.Cards else layout
    var step by rememberSaveable { mutableIntStateOf(initialStep) }
    val lastStep = 3

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing == null) R.string.new_habit else R.string.edit_habit)) },
                navigationIcon = {
                    IconButton(onClick = { if (effective == EditorLayout.Steps && step > 0) step-- else onBack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (effective == EditorLayout.Steps && step < lastStep) {
                    if (step >= 2) {
                        OutlinedButton(
                            onClick = { onSave(state.toDraft()) },
                            enabled = state.canSave,
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) { Text(stringResource(R.string.save_habit), maxLines = 1) }
                    }
                    Button(
                        onClick = { step++ },
                        enabled = step != 0 || state.name.isNotBlank(),
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) { Text(stringResource(if (step >= 2) R.string.editor_fine_tune else R.string.continue_label), maxLines = 1) }
                } else {
                    Button(
                        enabled = state.canSave,
                        onClick = { onSave(state.toDraft()) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(stringResource(R.string.save_habit)) }
                }
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            when (effective) {
                EditorLayout.Cards -> EditorScroll {
                    HabitLivePreview(state)
                    SectionCard(Icons.Rounded.Edit, R.string.editor_section_what) { NameAndLookSection(state) }
                    SectionCard(Icons.Rounded.Tune, R.string.editor_section_how) { TrackingSection(state) }
                    SectionCard(Icons.Rounded.Schedule, R.string.editor_section_when) {
                        DaysSection(state)
                        Spacer(Modifier.height(14.dp))
                        ReminderSection(state)
                    }
                    AdvancedGroups(state, others, customCategories, openGroup)
                }
                EditorLayout.Steps -> {
                    StepIndicator(step, lastStep + 1)
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            val forward = targetState > initialState
                            (slideInHorizontally(tween(260)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(tween(200)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(160)))
                        },
                        label = "editor-step",
                        modifier = Modifier.weight(1f),
                    ) { current ->
                        EditorScroll {
                            StepTitle(current)
                            HabitLivePreview(state)
                            when (current) {
                                0 -> SectionCard(Icons.Rounded.Edit, R.string.editor_section_what) { NameAndLookSection(state) }
                                1 -> SectionCard(Icons.Rounded.Tune, R.string.editor_section_how) { TrackingSection(state) }
                                2 -> SectionCard(Icons.Rounded.Schedule, R.string.editor_section_when) {
                                    DaysSection(state)
                                    Spacer(Modifier.height(14.dp))
                                    ReminderSection(state)
                                }
                                else -> AdvancedGroups(state, others, customCategories, openGroup)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorScroll(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun StepIndicator(step: Int, count: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            Box(
                Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@Composable
private fun StepTitle(step: Int) {
    val (title, body) = when (step) {
        0 -> R.string.editor_step_what_title to R.string.editor_step_what_body
        1 -> R.string.editor_step_how_title to R.string.editor_step_how_body
        2 -> R.string.editor_step_when_title to R.string.editor_step_when_body
        else -> R.string.editor_step_more_title to R.string.editor_step_more_body
    }
    Column(Modifier.padding(top = 8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A rounded card with a small icon and title, used for every editor section. */
@Composable
private fun SectionCard(icon: ImageVector, @androidx.annotation.StringRes title: Int, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp)) }
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
        }
        content()
    }
}

/** The habit as it will appear on Today, updating as you edit. */
@Composable
private fun HabitLivePreview(state: HabitEditorState) {
    val color = Color(habitColors[state.colorIndex].toInt())
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(color.copy(alpha = .12f)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = .22f)), contentAlignment = Alignment.Center) {
            Text(state.emoji, style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                state.name.ifBlank { stringResource(R.string.habit_name) },
                style = MaterialTheme.typography.titleMedium,
                color = if (state.name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                if (state.type == HabitType.Check) stringResource(R.string.type_check) else "${state.targetText.ifBlank { "0" }} ${state.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(Modifier.size(30.dp).border(2.5.dp, color, CircleShape))
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun NameAndLookSection(state: HabitEditorState) {
    val color = Color(habitColors[state.colorIndex].toInt())
    OutlinedTextField(
        value = state.name,
        onValueChange = { state.name = it.take(80) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.habit_name)) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
    )
    EditorLabel(R.string.icon_label)
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        habitEmojis.forEach { option ->
            val selected = state.emoji == option
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (selected) color.copy(alpha = .22f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(if (selected) 2.dp else 0.dp, if (selected) color else Color.Transparent, CircleShape)
                    .clickable { state.emoji = option },
                contentAlignment = Alignment.Center,
            ) { Text(option, style = MaterialTheme.typography.titleMedium) }
        }
    }
    EditorLabel(R.string.color_label)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        habitColors.forEachIndexed { index, value ->
            val selected = state.colorIndex == index
            Box(
                Modifier
                    .size(36.dp)
                    .border(if (selected) 2.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color(value.toInt()))
                    .clickable { state.colorIndex = index },
                contentAlignment = Alignment.Center,
            ) { if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun TrackingSection(state: HabitEditorState) {
    val context = LocalContext.current
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        HabitType.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = state.type == option,
                onClick = { state.changeType(option, context.getString(R.string.unit_times), context.getString(R.string.unit_minutes)) },
                shape = SegmentedButtonDefaults.itemShape(index, HabitType.entries.size),
                label = { Text(stringResource(typeLabel(option)), maxLines = 1) },
            )
        }
    }
    AnimatedVisibility(
        visible = state.type != HabitType.Check,
        enter = fadeIn(tween(200)) + expandVertically(tween(240), expandFrom = Alignment.Top),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(200), shrinkTowards = Alignment.Top),
    ) {
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.targetText,
                onValueChange = { state.targetText = it.take(8) },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(if (state.direction == HabitDirection.Quit) R.string.ideal_target_upper else R.string.target_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            OutlinedTextField(
                value = state.unit,
                onValueChange = { state.unit = it.take(20) },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.unit_label)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

/** Seven round day buttons; filled when the habit is scheduled that day. */
@Composable
private fun DaysSection(state: HabitEditorState) {
    val days = stringArrayResource(R.array.weekday_abbreviations)
    EditorLabel(R.string.days_label, top = 0)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { index, day ->
            val selected = state.selectedDays[index] == '1'
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { state.toggleDay(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    day.take(2),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReminderSection(state: HabitEditorState) {
    val context = LocalContext.current
    // Re-checked on every resume so the hint disappears after the user allows notifications in
    // system settings and comes back.
    var notificationsAllowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.reminder_label), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.reminder_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    AnimatedVisibility(visible = state.reminderEnabled && !notificationsAllowed) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        ) {
            Column(Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp)) {
                Text(stringResource(R.string.notifications_blocked), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    },
                    modifier = Modifier.align(Alignment.End),
                ) { Text(stringResource(R.string.open_notification_settings)) }
            }
        }
    }
    AnimatedVisibility(
        visible = state.reminderEnabled,
        enter = fadeIn(tween(200)) + expandVertically(tween(240), expandFrom = Alignment.Top),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(200), shrinkTowards = Alignment.Top),
    ) {
        val invalid = state.reminderTime.toReminderMinutesOrNull() == null
        OutlinedTextField(
            value = state.reminderTime,
            onValueChange = { state.reminderTime = it.take(5) },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            label = { Text(stringResource(R.string.reminder_time)) },
            singleLine = true,
            isError = invalid,
            shape = RoundedCornerShape(16.dp),
            supportingText = { if (invalid) Text(stringResource(R.string.reminder_time_error)) },
        )
    }
}

/** The optional settings, split into four small groups that open one at a time. */
internal enum class AdvancedGroup { Goal, Plan, Schedule, Tags }

@Composable
private fun AdvancedGroups(state: HabitEditorState, otherHabits: List<Habit>, categories: List<CategoryEntity>, initiallyOpen: AdvancedGroup?) {
    var open by rememberSaveable { mutableStateOf(initiallyOpen) }
    Text(
        stringResource(R.string.editor_more_title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, top = 6.dp),
    )
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        AdvancedGroup.entries.forEachIndexed { index, group ->
            if (index > 0) HorizontalDivider(Modifier.padding(start = 60.dp), color = MaterialTheme.colorScheme.outlineVariant)
            val expanded = open == group
            val (icon, title, summary) = when (group) {
                AdvancedGroup.Goal -> Triple(Icons.Rounded.Flag, R.string.editor_group_goal, R.string.editor_group_goal_desc)
                AdvancedGroup.Plan -> Triple(Icons.Rounded.Lightbulb, R.string.editor_group_plan, R.string.editor_group_plan_desc)
                AdvancedGroup.Schedule -> Triple(Icons.Rounded.Event, R.string.editor_group_schedule, R.string.editor_group_schedule_desc)
                AdvancedGroup.Tags -> Triple(Icons.Rounded.Sell, R.string.editor_group_tags, R.string.editor_group_tags_desc)
            }
            Row(
                Modifier.fillMaxWidth().clickable { open = if (expanded) null else group }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp)) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                val rotation by androidx.compose.animation.core.animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "chevron")
                Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.graphicsLayer { rotationZ = rotation })
            }
            // Opens downwards from its own header with a short, even animation.
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200, delayMillis = 60)) + expandVertically(tween(260), expandFrom = Alignment.Top),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(220), shrinkTowards = Alignment.Top),
            ) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    when (group) {
                        AdvancedGroup.Goal -> GoalGroup(state)
                        AdvancedGroup.Plan -> PlanGroup(state, otherHabits)
                        AdvancedGroup.Schedule -> ScheduleGroup(state)
                        AdvancedGroup.Tags -> {
                            OutlinedTextField(
                                value = state.tags,
                                onValueChange = { state.tags = it.take(240) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.tags_label)) },
                                supportingText = { Text(stringResource(R.string.tags_help)) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                            )
                            CategorySuggestions(categories, state.tags) { state.tags = it }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalGroup(state: HabitEditorState) {
    EditorLabel(R.string.intent_label, top = 0)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        listOf(HabitDirection.Build to R.string.build_habit, HabitDirection.Quit to R.string.quit_habit).forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = state.direction == value,
                onClick = { state.direction = value },
                shape = SegmentedButtonDefaults.itemShape(index, 2),
                label = { Text(stringResource(label), maxLines = 1) },
            )
        }
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
    if (state.type != HabitType.Check && state.direction == HabitDirection.Build) {
        HealthMetricPicker(state.healthMetric) { state.healthMetric = it }
    }
    EditorLabel(R.string.goal_period)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        GoalPeriod.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = state.goalPeriod == option,
                onClick = { state.goalPeriod = option },
                shape = SegmentedButtonDefaults.itemShape(index, GoalPeriod.entries.size),
                label = { Text(stringResource(goalPeriodLabel(option)), maxLines = 1) },
            )
        }
    }
    if (state.goalPeriod != GoalPeriod.Daily) {
        Spacer(Modifier.height(10.dp))
        NumberField(
            state.periodTargetText,
            { state.periodTargetText = it.filter(Char::isDigit).take(2) },
            if (state.goalPeriod == GoalPeriod.Weekly) R.string.times_per_week else R.string.times_per_month,
            Modifier.fillMaxWidth(),
        )
    }
    EditorLabel(R.string.difficulty)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        (1..3).forEach { value ->
            SegmentedButton(
                selected = state.difficulty == value,
                onClick = { state.difficulty = value },
                shape = SegmentedButtonDefaults.itemShape(value - 1, 3),
                label = { Text(stringResource(when (value) { 1 -> R.string.easy; 2 -> R.string.medium; else -> R.string.hard }), maxLines = 1) },
            )
        }
    }
}

@Composable
private fun PlanGroup(state: HabitEditorState, otherHabits: List<Habit>) {
    OutlinedTextField(
        value = state.intention,
        onValueChange = { state.intention = it.take(240) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.intention_sentence)) },
        placeholder = { Text(stringResource(R.string.intention_example)) },
        supportingText = { Text(stringResource(R.string.intention_help)) },
        minLines = 2,
        shape = RoundedCornerShape(16.dp),
    )
    if (state.direction == HabitDirection.Build) {
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = state.tinyVersion,
            onValueChange = { state.tinyVersion = it.take(60) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.tiny_version_label)) },
            placeholder = { Text(stringResource(R.string.tiny_version_example)) },
            supportingText = { Text(stringResource(R.string.tiny_version_help)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
    }
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = state.routineName,
        onValueChange = { state.routineName = it.take(40) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.routine_name)) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
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
}

@Composable
private fun ScheduleGroup(state: HabitEditorState) {
    if (state.reminderEnabled) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.repeat_reminder), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.repeat_reminder_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state.repeatEnabled, onCheckedChange = { state.repeatEnabled = it })
        }
        if (state.repeatEnabled) {
            NumberField(state.repeatMinutes, { state.repeatMinutes = it.filter(Char::isDigit).take(3) }, R.string.repeat_minutes, Modifier.fillMaxWidth())
        }
    }
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
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier.padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(text, modifier = Modifier.padding(15.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun EditorLabel(textRes: Int, top: Int = 18) {
    Text(
        stringResource(textRes),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = top.dp, bottom = 8.dp),
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

/**
 * "Fill from health data": links a count or duration habit to a Health Connect metric and asks
 * for the read permission when one is picked.
 */
@Composable
private fun HealthMetricPicker(selected: HealthMetric, onSelect: (HealthMetric) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val available = remember { app.tsuzuku.health.HealthSync.isAvailable(context) }
    var pending by remember { mutableStateOf<HealthMetric?>(null) }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        val metric = pending ?: return@rememberLauncherForActivityResult
        pending = null
        if (app.tsuzuku.health.HealthSync.permissionFor(metric) in granted) onSelect(metric)
    }
    EditorLabel(R.string.health_fill_label)
    if (!available) {
        Text(
            stringResource(R.string.health_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        HealthMetric.entries.forEach { metric ->
            FilterChip(
                selected = selected == metric,
                onClick = {
                    val permission = app.tsuzuku.health.HealthSync.permissionFor(metric)
                    if (permission == null) {
                        onSelect(metric)
                    } else {
                        pending = metric
                        permissionLauncher.launch(setOf(permission))
                    }
                },
                label = { Text(stringResource(healthMetricLabel(metric))) },
            )
        }
    }
    if (selected != HealthMetric.None) {
        Text(
            stringResource(R.string.health_fill_help, stringResource(healthMetricUnitHint(selected))),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun healthMetricLabel(metric: HealthMetric): Int = when (metric) {
    HealthMetric.None -> R.string.health_none
    HealthMetric.Steps -> R.string.health_steps
    HealthMetric.Distance -> R.string.health_distance
    HealthMetric.Calories -> R.string.health_calories
    HealthMetric.ExerciseDuration -> R.string.health_exercise
    HealthMetric.SleepDuration -> R.string.health_sleep
}

private fun healthMetricUnitHint(metric: HealthMetric): Int = when (metric) {
    HealthMetric.None, HealthMetric.Steps -> R.string.health_unit_steps
    HealthMetric.Distance -> R.string.health_unit_distance
    HealthMetric.Calories -> R.string.health_unit_calories
    HealthMetric.ExerciseDuration, HealthMetric.SleepDuration -> R.string.health_unit_time
}
