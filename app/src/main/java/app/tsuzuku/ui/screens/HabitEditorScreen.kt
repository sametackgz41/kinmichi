package app.tsuzuku.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitDraft
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.GoalPeriod
import app.tsuzuku.model.HealthMetric
import app.tsuzuku.data.local.CategoryEntity

private val habitColors = listOf(
    0xFF5068A9,
    0xFF3F7D5A,
    0xFFAD762C,
    0xFFA7565D,
    0xFF7A669E,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorScreen(
    existing: Habit? = null,
    availableHabits: List<Habit> = emptyList(),
    customCategories: List<CategoryEntity> = emptyList(),
    onBack: () -> Unit,
    onSave: (HabitDraft) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var emoji by rememberSaveable(existing?.id) { mutableStateOf(existing?.emoji ?: "✓") }
    var colorIndex by rememberSaveable(existing?.id) { mutableIntStateOf(habitColors.indexOf(existing?.colorArgb).coerceAtLeast(0)) }
    var directionName by rememberSaveable(existing?.id) { mutableStateOf(existing?.direction?.name ?: HabitDirection.Build.name) }
    var typeName by rememberSaveable(existing?.id) { mutableStateOf(existing?.type?.name ?: HabitType.Check.name) }
    var targetText by rememberSaveable(existing?.id) { mutableStateOf(existing?.target?.let(::formatEditorAmount) ?: "1") }
    var unit by rememberSaveable(existing?.id) { mutableStateOf(existing?.unit.orEmpty()) }
    var stepText by rememberSaveable(existing?.id) { mutableStateOf(existing?.step?.let(::formatEditorAmount) ?: "1") }
    var minimumTargetText by rememberSaveable(existing?.id) { mutableStateOf(existing?.minimumTarget?.takeIf { it > 0 }?.let(::formatEditorAmount).orEmpty()) }
    var maximumTargetText by rememberSaveable(existing?.id) { mutableStateOf(existing?.maximumTarget?.let(::formatEditorAmount).orEmpty()) }
    var difficulty by rememberSaveable(existing?.id) { mutableIntStateOf(existing?.difficulty ?: 2) }
    var contextLabel by rememberSaveable(existing?.id) { mutableStateOf(existing?.contextLabel.orEmpty()) }
    var intention by rememberSaveable(existing?.id) { mutableStateOf(existing?.intention.orEmpty()) }
    var activeStartEpochDay by rememberSaveable(existing?.id) { mutableStateOf(existing?.activeStartEpochDay) }
    var activeEndEpochDay by rememberSaveable(existing?.id) { mutableStateOf(existing?.activeEndEpochDay) }
    var pausedUntilEpochDay by rememberSaveable(existing?.id) { mutableStateOf(existing?.pausedUntilEpochDay) }
    var repeatIntervalText by rememberSaveable(existing?.id) { mutableStateOf((existing?.repeatIntervalDays ?: 1).toString()) }
    var tags by rememberSaveable(existing?.id) { mutableStateOf(existing?.tags.orEmpty()) }
    var selectedDays by rememberSaveable(existing?.id) { mutableStateOf(existing?.scheduleDaysMask?.toDaySelection() ?: "1111111") }
    var goalPeriodName by rememberSaveable(existing?.id) { mutableStateOf(existing?.goalPeriod?.name ?: GoalPeriod.Daily.name) }
    var periodTargetText by rememberSaveable(existing?.id) { mutableStateOf(existing?.periodTarget?.toString() ?: "3") }
    var routineName by rememberSaveable(existing?.id) { mutableStateOf(existing?.routineName.orEmpty()) }
    var prerequisiteId by rememberSaveable(existing?.id) { mutableStateOf(existing?.prerequisiteHabitId ?: 0L) }
    var healthMetricName by rememberSaveable(existing?.id) { mutableStateOf(existing?.healthMetric?.name ?: HealthMetric.None.name) }
    var reminderEnabled by rememberSaveable(existing?.id) { mutableStateOf(existing?.reminderMinutes != null) }
    var reminderTime by rememberSaveable(existing?.id) { mutableStateOf(existing?.reminderMinutes?.toClockText() ?: "09:00") }
    var repeatEnabled by rememberSaveable(existing?.id) { mutableStateOf(existing?.reminderRepeatMinutes != null) }
    var repeatMinutes by rememberSaveable(existing?.id) { mutableStateOf(existing?.reminderRepeatMinutes?.toString() ?: "10") }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    val direction = HabitDirection.valueOf(directionName)
    val type = HabitType.valueOf(typeName)
    val target = targetText.toLocaleDoubleOrNull() ?: 0.0
    val increment = stepText.toLocaleDoubleOrNull() ?: 1.0
    val goalPeriod = GoalPeriod.valueOf(goalPeriodName)
    val periodTarget = periodTargetText.toIntOrNull() ?: 0
    val canContinue = when (step) {
        0 -> name.isNotBlank()
        1 -> type == HabitType.Check || target > 0.0
        2 -> selectedDays.contains('1') && (goalPeriod == GoalPeriod.Daily || periodTarget in 1..31)
        3 -> !reminderEnabled || (
            reminderTime.toReminderMinutesOrNull() != null &&
                (!repeatEnabled || (repeatMinutes.toIntOrNull() ?: 0) in 1..720)
            )
        else -> true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("習慣 · EDITOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(stringResource(if (existing == null) R.string.new_habit else R.string.edit_habit))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (step == 0) onBack() else step-- }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Text(
                        stringResource(R.string.step_fraction, step + 1, 5),
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (step > 0) {
                    OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.back))
                    }
                }
                Button(
                    enabled = canContinue,
                    onClick = {
                        if (step < 4) {
                            step++
                        } else {
                            onSave(
                                HabitDraft(
                                    name = name,
                                    emoji = emoji,
                                    colorArgb = habitColors[colorIndex],
                                    type = type,
                                    direction = direction,
                                    target = if (type == HabitType.Check) 1.0 else target,
                                    unit = if (type == HabitType.Check) "" else unit,
                                    step = if (type == HabitType.Check) 1.0 else increment.coerceAtLeast(0.1),
                                    scheduleDaysMask = selectedDays.toDaysMask(),
                                    goalPeriod = goalPeriod,
                                    periodTarget = if (goalPeriod == GoalPeriod.Daily) 1 else periodTarget,
                                    routineName = routineName,
                                    prerequisiteHabitId = prerequisiteId.takeIf { it > 0L },
                                    healthMetric = HealthMetric.valueOf(healthMetricName),
                                    reminderMinutes = if (reminderEnabled) reminderTime.toReminderMinutesOrNull() else null,
                                    reminderRepeatMinutes = if (reminderEnabled && repeatEnabled) repeatMinutes.toIntOrNull() else null,
                                    generalNote = existing?.generalNote.orEmpty(),
                                    minimumTarget = if (type == HabitType.Check) 1.0 else minimumTargetText.toLocaleDoubleOrNull() ?: 0.0,
                                    maximumTarget = maximumTargetText.toLocaleDoubleOrNull(),
                                    difficulty = difficulty,
                                    contextLabel = contextLabel,
                                    intention = intention,
                                    activeStartEpochDay = activeStartEpochDay,
                                    activeEndEpochDay = activeEndEpochDay,
                                    repeatIntervalDays = repeatIntervalText.toIntOrNull()?.coerceIn(1, 365) ?: 1,
                                    pausedUntilEpochDay = pausedUntilEpochDay,
                                    tags = tags,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(if (step == 4) R.string.save_habit else R.string.continue_label))
                    if (step < 4) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = 20.dp, vertical = 10.dp)),
        ) {
            LinearProgressIndicator(
                progress = { (step + 1) / 5f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(22.dp))
            Text(stringResource(stepTitle(step)), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(stepDescription(step)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp, bottom = 20.dp),
            )

            HabitLivePreview(
                name = name,
                emoji = emoji,
                color = androidx.compose.ui.graphics.Color(habitColors[colorIndex].toInt()),
                target = if (type == HabitType.Check) "✓" else "${targetText.ifBlank { "0" }} ${unit}",
            )
            Spacer(Modifier.height(22.dp))

            when (step) {
                0 -> Column {
                    BasicInfoStep(
                        name = name, onNameChange = { name = it }, emoji = emoji, onEmojiChange = { emoji = it },
                        colorIndex = colorIndex, onColorChange = { colorIndex = it }, direction = direction, onDirectionChange = { directionName = it.name },
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(tags, { tags = it.take(240) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tags_label)) }, supportingText = { Text(stringResource(R.string.tags_help)) }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    CategorySuggestions(customCategories, tags) { tags = it }
                }
                1 -> GoalStep(
                    type = type,
                    direction = direction,
                    onTypeChange = {
                        typeName = it.name
                        when (it) {
                            HabitType.Check -> {
                                targetText = "1"
                                stepText = "1"
                                unit = ""
                            }
                            HabitType.Count -> if (unit.isBlank()) unit = "adet"
                            HabitType.Duration -> {
                                if (targetText == "1") targetText = "30"
                                stepText = "5"
                                unit = "dk"
                            }
                        }
                    },
                    target = targetText,
                    onTargetChange = { targetText = it },
                    unit = unit,
                    onUnitChange = { unit = it },
                    increment = stepText,
                    onIncrementChange = { stepText = it },
                    minimumTarget = minimumTargetText,
                    onMinimumTargetChange = { minimumTargetText = it },
                    maximumTarget = maximumTargetText,
                    onMaximumTargetChange = { maximumTargetText = it },
                    difficulty = difficulty,
                    onDifficultyChange = { difficulty = it },
                    contextLabel = contextLabel,
                    onContextLabelChange = { contextLabel = it.take(40) },
                    intention = intention,
                    onIntentionChange = { intention = it.take(240) },
                    healthMetric = HealthMetric.valueOf(healthMetricName),
                    onHealthMetricChange = { healthMetricName = it.name },
                )
                2 -> Column {
                    ScheduleStep(
                        selectedDays = selectedDays,
                        onSelectedDaysChange = { selectedDays = it },
                        goalPeriod = goalPeriod,
                        onGoalPeriodChange = { goalPeriodName = it.name },
                        periodTarget = periodTargetText,
                        onPeriodTargetChange = { periodTargetText = it.filter(Char::isDigit).take(2) },
                        routineName = routineName,
                        onRoutineNameChange = { routineName = it.take(40) },
                        prerequisiteId = prerequisiteId,
                        onPrerequisiteChange = { prerequisiteId = it },
                        availableHabits = availableHabits.filterNot { it.id == existing?.id },
                    )
                    FlexibleScheduleFields(
                        repeatInterval = repeatIntervalText,
                        onRepeatIntervalChange = { repeatIntervalText = it.filter(Char::isDigit).take(3) },
                        activeStartEpochDay = activeStartEpochDay,
                        onActiveStartChange = { activeStartEpochDay = it },
                        activeEndEpochDay = activeEndEpochDay,
                        onActiveEndChange = { activeEndEpochDay = it },
                        pausedUntilEpochDay = pausedUntilEpochDay,
                        onPausedUntilChange = { pausedUntilEpochDay = it },
                    )
                }
                3 -> ReminderStep(
                    enabled = reminderEnabled,
                    onEnabledChange = { enabled ->
                        reminderEnabled = enabled
                        if (
                            enabled &&
                            Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    time = reminderTime,
                    onTimeChange = { reminderTime = it },
                    repeatEnabled = repeatEnabled,
                    onRepeatEnabledChange = { repeatEnabled = it },
                    repeatMinutes = repeatMinutes,
                    onRepeatMinutesChange = { repeatMinutes = it.filter(Char::isDigit).take(3) },
                )
                else -> SummaryStep(
                    name = name,
                    emoji = emoji,
                    direction = direction,
                    type = type,
                    target = target,
                    unit = unit,
                    selectedDays = selectedDays,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    reminderRepeatMinutes = if (reminderEnabled && repeatEnabled) repeatMinutes.toIntOrNull() else null,
                    goalPeriod = goalPeriod,
                    periodTarget = periodTarget,
                    routineName = routineName,
                )
            }
        }
    }
}

@Composable
private fun HabitLivePreview(name: String, emoji: String, color: androidx.compose.ui.graphics.Color, target: String) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(stringResource(R.string.live_preview), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(color = color.copy(alpha = .16f), shape = RoundedCornerShape(5.dp)) {
                    Text(emoji, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.titleLarge)
                }
                Column(Modifier.weight(1f)) {
                    Text(name.ifBlank { stringResource(R.string.habit_name) }, style = MaterialTheme.typography.titleMedium)
                    Text(target, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.size(22.dp).border(2.dp, color, CircleShape))
            }
        }
    }
}

@Composable
private fun BasicInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    emoji: String,
    onEmojiChange: (String) -> Unit,
    colorIndex: Int,
    onColorChange: (Int) -> Unit,
    direction: HabitDirection,
    onDirectionChange: (HabitDirection) -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.habit_name)) },
        singleLine = true,
    )
    EditorLabel(R.string.intent_label)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DirectionCard(
            title = stringResource(R.string.build_habit),
            selected = direction == HabitDirection.Build,
            onClick = { onDirectionChange(HabitDirection.Build) },
            modifier = Modifier.weight(1f),
        )
        DirectionCard(
            title = stringResource(R.string.quit_habit),
            selected = direction == HabitDirection.Quit,
            onClick = { onDirectionChange(HabitDirection.Quit) },
            modifier = Modifier.weight(1f),
        )
    }
    EditorLabel(R.string.icon_label)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("✓", "💧", "📖", "🏃", "🌿").forEach { option ->
            FilterChip(
                selected = emoji == option,
                onClick = { onEmojiChange(option) },
                label = { Text(option) },
            )
        }
    }
    EditorLabel(R.string.color_label)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        habitColors.forEachIndexed { index, color ->
            FilterChip(
                selected = colorIndex == index,
                onClick = { onColorChange(index) },
                label = { Text("●", color = androidx.compose.ui.graphics.Color(color.toInt())) },
                leadingIcon = if (colorIndex == index) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
            )
        }
    }
}

@Composable
private fun DirectionCard(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Text(title, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun GoalStep(
    type: HabitType,
    direction: HabitDirection = HabitDirection.Build,
    onTypeChange: (HabitType) -> Unit,
    target: String,
    onTargetChange: (String) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit,
    increment: String,
    onIncrementChange: (String) -> Unit,
    minimumTarget: String,
    onMinimumTargetChange: (String) -> Unit,
    maximumTarget: String,
    onMaximumTargetChange: (String) -> Unit,
    difficulty: Int,
    onDifficultyChange: (Int) -> Unit,
    contextLabel: String,
    onContextLabelChange: (String) -> Unit,
    intention: String,
    onIntentionChange: (String) -> Unit,
    healthMetric: HealthMetric,
    onHealthMetricChange: (HealthMetric) -> Unit,
) {
    if (direction == HabitDirection.Quit) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🛑 Kötü Alışkanlık / Limit Takibi", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(
                    "Bu alışkanlıkta amaç tüketimi sınırlandırmak veya sıfırlamaktır. Limit yaklaşımı (%80) ve aşımı sakin editoryal göstergelerle takip edilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                val maxVal = maximumTarget.toDoubleOrNull() ?: target.toDoubleOrNull() ?: 0.0
                if (maxVal > 0.0) {
                    val warnVal = maxVal * 0.8
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("🟢 İdeal: ≤ $target $unit", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text("⚡ Uyarı: ≥ ${"%.1f".format(warnVal)} $unit", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.errorContainer) {
                            Text("⚠️ Aşım: > $maxVal $unit", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }

    EditorLabel(R.string.tracking_type)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        HabitType.entries.forEach { option ->
            FilterChip(
                selected = type == option,
                onClick = { onTypeChange(option) },
                label = { Text(stringResource(typeLabel(option))) },
            )
        }
    }
    if (type != HabitType.Check) {
        OutlinedTextField(
            value = target,
            onValueChange = onTargetChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (direction == HabitDirection.Quit) "İdeal Hedef / Üst Sınır" else stringResource(R.string.target_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = unit,
            onValueChange = onUnitChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.unit_label)) },
            singleLine = true,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = increment,
            onValueChange = onIncrementChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.quick_increment)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = minimumTarget,
                onValueChange = onMinimumTargetChange,
                modifier = Modifier.weight(1f),
                label = { Text(if (direction == HabitDirection.Quit) "İdeal Alt Sınır" else stringResource(R.string.minimum_target)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            OutlinedTextField(
                value = maximumTarget,
                onValueChange = onMaximumTargetChange,
                modifier = Modifier.weight(1f),
                label = { Text(if (direction == HabitDirection.Quit) "Maksimum Limit" else stringResource(R.string.maximum_target)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
        }
    } else {
        InfoCard(if (direction == HabitDirection.Quit) "✓ Tamamlandı: Bugün bu kötü alışkanlıktan tamamen uzak duruldu (Temiz gün)." else stringResource(R.string.check_goal_explanation))
    }
    EditorLabel(R.string.difficulty)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..3).forEach { value ->
            FilterChip(selected = difficulty == value, onClick = { onDifficultyChange(value) }, label = { Text(stringResource(when (value) { 1 -> R.string.easy; 2 -> R.string.medium; else -> R.string.hard })) })
        }
    }
    OutlinedTextField(
        value = contextLabel,
        onValueChange = onContextLabelChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.habit_context)) },
        placeholder = { Text(stringResource(R.string.habit_context_example)) },
        singleLine = true,
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = intention,
        onValueChange = onIntentionChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.intention_sentence)) },
        placeholder = { Text(stringResource(R.string.intention_example)) },
        minLines = 2,
    )
    if (type == HabitType.Count) {
        EditorLabel(R.string.automatic_tracking)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = healthMetric == HealthMetric.None,
                onClick = { onHealthMetricChange(HealthMetric.None) },
                label = { Text(stringResource(R.string.manual_tracking)) },
            )
            FilterChip(
                selected = healthMetric == HealthMetric.Steps,
                onClick = { onHealthMetricChange(HealthMetric.Steps) },
                label = { Text(stringResource(R.string.health_connect_steps)) },
            )
        }
    }
}

@Composable
private fun ScheduleStep(
    selectedDays: String,
    onSelectedDaysChange: (String) -> Unit,
    goalPeriod: GoalPeriod,
    onGoalPeriodChange: (GoalPeriod) -> Unit,
    periodTarget: String,
    onPeriodTargetChange: (String) -> Unit,
    routineName: String,
    onRoutineNameChange: (String) -> Unit,
    prerequisiteId: Long,
    onPrerequisiteChange: (Long) -> Unit,
    availableHabits: List<Habit>,
) {
    val days = stringArrayResource(R.array.weekday_abbreviations)
    EditorLabel(R.string.days_label)
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        days.forEachIndexed { index, day ->
            FilterChip(
                selected = selectedDays[index] == '1',
                onClick = {
                    val replacement = if (selectedDays[index] == '1') '0' else '1'
                    onSelectedDaysChange(selectedDays.replaceRange(index, index + 1, replacement.toString()))
                },
                label = { Text(day) },
            )
        }
    }
    EditorLabel(R.string.goal_period)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        GoalPeriod.entries.forEach { option ->
            FilterChip(
                selected = goalPeriod == option,
                onClick = { onGoalPeriodChange(option) },
                label = { Text(stringResource(goalPeriodLabel(option))) },
            )
        }
    }
    if (goalPeriod != GoalPeriod.Daily) {
        OutlinedTextField(
            value = periodTarget,
            onValueChange = onPeriodTargetChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(if (goalPeriod == GoalPeriod.Weekly) R.string.times_per_week else R.string.times_per_month)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
    }
    EditorLabel(R.string.routine_optional)
    OutlinedTextField(
        value = routineName,
        onValueChange = onRoutineNameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.routine_name)) },
        singleLine = true,
    )
    if (availableHabits.isNotEmpty()) {
        EditorLabel(R.string.after_habit_optional)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FilterChip(selected = prerequisiteId == 0L, onClick = { onPrerequisiteChange(0L) }, label = { Text(stringResource(R.string.none_label)) })
            availableHabits.forEach { habit ->
                FilterChip(selected = prerequisiteId == habit.id, onClick = { onPrerequisiteChange(habit.id) }, label = { Text("${habit.emoji} ${habit.name}") })
            }
        }
    }
}

@Composable
private fun ReminderStep(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    repeatEnabled: Boolean,
    onRepeatEnabledChange: (Boolean) -> Unit,
    repeatMinutes: String,
    onRepeatMinutesChange: (String) -> Unit,
) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.reminder_label), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.reminder_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
    if (enabled) {
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = time,
            onValueChange = onTimeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.reminder_time)) },
            singleLine = true,
            isError = time.toReminderMinutesOrNull() == null,
            supportingText = {
                if (time.toReminderMinutesOrNull() == null) Text(stringResource(R.string.reminder_time_error))
            },
        )
        InfoCard(stringResource(R.string.reminder_future_note))
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(8.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.repeat_reminder), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.repeat_reminder_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = repeatEnabled, onCheckedChange = onRepeatEnabledChange)
            }
        }
        if (repeatEnabled) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = repeatMinutes,
                onValueChange = onRepeatMinutesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.repeat_minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = (repeatMinutes.toIntOrNull() ?: 0) !in 1..720,
            )
        }
    }
}

@Composable
private fun SummaryStep(
    name: String,
    emoji: String,
    direction: HabitDirection,
    type: HabitType,
    target: Double,
    unit: String,
    selectedDays: String,
    reminderEnabled: Boolean,
    reminderTime: String,
    reminderRepeatMinutes: Int?,
    goalPeriod: GoalPeriod,
    periodTarget: Int,
    routineName: String,
) {
    SummaryRow(stringResource(R.string.habit_name), "$emoji  $name")
    SummaryRow(stringResource(R.string.intent_label), stringResource(if (direction == HabitDirection.Build) R.string.build_habit else R.string.quit_habit))
    SummaryRow(stringResource(R.string.tracking_type), stringResource(typeLabel(type)))
    if (type != HabitType.Check) {
        SummaryRow(stringResource(R.string.target_label), "${formatEditorAmount(target)} $unit")
    }
    SummaryRow(
        stringResource(R.string.days_label),
        if (selectedDays == "1111111") stringResource(R.string.every_day) else stringResource(R.string.selected_days),
    )
    if (goalPeriod != GoalPeriod.Daily) {
        SummaryRow(stringResource(R.string.goal_period), stringResource(if (goalPeriod == GoalPeriod.Weekly) R.string.weekly_target_value else R.string.monthly_target_value, periodTarget))
    }
    if (routineName.isNotBlank()) SummaryRow(stringResource(R.string.routine_name), routineName)
    if (reminderEnabled && reminderRepeatMinutes != null) {
        SummaryRow(stringResource(R.string.repeat_reminder), stringResource(R.string.minutes_value, reminderRepeatMinutes))
    }
    SummaryRow(
        stringResource(R.string.reminder_label),
        if (reminderEnabled) reminderTime else stringResource(R.string.off_label),
    )
}

@androidx.annotation.StringRes
private fun goalPeriodLabel(period: GoalPeriod): Int = when (period) {
    GoalPeriod.Daily -> R.string.daily
    GoalPeriod.Weekly -> R.string.weekly
    GoalPeriod.Monthly -> R.string.monthly
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Card(modifier = Modifier.padding(bottom = 9.dp), shape = RoundedCornerShape(15.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier.padding(top = 14.dp),
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

private fun stepTitle(step: Int): Int = when (step) {
    0 -> R.string.basic_information
    1 -> R.string.goal_step
    2 -> R.string.schedule_step
    3 -> R.string.reminders_step
    else -> R.string.summary_step
}

private fun stepDescription(step: Int): Int = when (step) {
    0 -> R.string.basic_information_description
    1 -> R.string.goal_step_description
    2 -> R.string.schedule_step_description
    3 -> R.string.reminders_step_description
    else -> R.string.summary_step_description
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
