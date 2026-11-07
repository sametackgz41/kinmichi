package app.tsuzuku.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import app.tsuzuku.model.SkipReasonType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.DailyReview
import app.tsuzuku.model.Goal
import app.tsuzuku.model.GoalStatus
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.GoalPeriod
import app.tsuzuku.model.PlanTask
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.ui.HabitTimerState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    habits: List<TodayHabit>,
    vacationActive: Boolean = false,
    profileName: String,
    showDailySummary: Boolean,
    timer: HabitTimerState,
    onAddHabit: () -> Unit,
    onAddTemplate: (app.tsuzuku.model.HabitDraft) -> Unit = {},
    onOpenPlan: () -> Unit = {},
    onToggleHabit: (Long) -> Unit,
    onIncrementHabit: (Long) -> Unit,
    onDecrementHabit: (Long) -> Unit,
    onToggleTimer: (Long) -> Unit,
    onOpenHabit: (Long) -> Unit,
    onReorderHabits: () -> Unit,
    onOpenGoals: () -> Unit,
    onSearch: () -> Unit,
    planTasks: List<PlanTask> = emptyList(),
    goals: List<Goal> = emptyList(),
    focusMinutesToday: Int = 0,
    todaySectionOrder: String = "summary,intelligence,habits,plan,goals,library,focus,health,routines",
    todayHiddenSections: String = "",
    onUpdateSectionConfig: (newOrder: String, newHidden: String) -> Unit = { _, _ -> },
    onStartFocus: () -> Unit = {},
    onStartRoutine: (String) -> Unit = {},
    onTogglePlanTask: (PlanTask) -> Unit = {},
    onOpenPlanTask: (Long) -> Unit = {},
    dailyReview: DailyReview? = null,
    onOpenDailyReview: () -> Unit = {},
    onSkipHabit: (Long, String?) -> Unit = { _, _ -> },
    onMovePlanTask: (Long, Long?) -> Unit = { _, _ -> },
    onDeletePlanTask: (Long) -> Unit = {},
    onDividePlanTaskIntoSubtasks: (Long, List<String>) -> Unit = { _, _ -> },
    targetAdvice: app.tsuzuku.model.TargetAdvice? = null,
    onAcceptAdvice: (app.tsuzuku.model.TargetAdvice) -> Unit = {},
    onDismissAdvice: (app.tsuzuku.model.TargetAdvice) -> Unit = {},
    onQuickMood: (Int) -> Unit = {},
    weekProgress: List<WeekDayProgress> = emptyList(),
) {
    val active = habits.filterNot(TodayHabit::isComplete)
    val completed = habits.filter(TodayHabit::isComplete)
    var showConfigSheet by remember { mutableStateOf(false) }
    var skipHabitTarget by remember { mutableStateOf<TodayHabit?>(null) }
    var selectedCarryTask by remember { mutableStateOf<PlanTask?>(null) }

    val orderList = remember(todaySectionOrder) {
        todaySectionOrder.split(',').filter(String::isNotBlank)
    }
    val hiddenSet = remember(todayHiddenSections) {
        todayHiddenSections.split(',').filter(String::isNotBlank).toSet()
    }
    val todayEpochDay = remember { LocalDate.now().toEpochDay() }
    val carryTasks = remember(planTasks) {
        planTasks.filter { !it.completed && it.carryCount >= 3 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { GreetingBlock(profileName) }
                IconButton(onClick = { showConfigSheet = true }) {
                    Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.customize_sections))
                }
                Box {
                    var moreExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { moreExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_actions))
                    }
                    DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.open_goals)) },
                            leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null) },
                            onClick = { moreExpanded = false; onOpenGoals() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reorder_habits)) },
                            leadingIcon = { Icon(Icons.Rounded.SwapVert, contentDescription = null) },
                            onClick = { moreExpanded = false; onReorderHabits() },
                        )
                    }
                }
            }
        }
        if (weekProgress.isNotEmpty()) {
            item(key = "week-enso") {
                WeekEnsoStrip(weekProgress, Modifier.padding(top = 4.dp, bottom = 8.dp))
            }
        }
        if (vacationActive) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(R.string.vacation_today_message), Modifier.fillMaxWidth().padding(12.dp))
                }
            }
        }

        orderList.forEach { sectionId ->
            if (sectionId !in hiddenSet) {
                when (sectionId) {
                    "summary" -> {
                        if (showDailySummary && habits.isNotEmpty()) {
                            item(key = "section-summary") {
                                TodayProgressHeader(
                                    completed = completed.size,
                                    total = habits.size,
                                    streakDays = habits.maxOfOrNull { it.streakDays } ?: 0,
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                    "habits" -> {
                        if (targetAdvice != null) {
                            item(key = "target-advice-${targetAdvice.habit.id}") {
                                TargetAdviceCard(
                                    advice = targetAdvice,
                                    onAccept = { onAcceptAdvice(targetAdvice) },
                                    onDismiss = { onDismissAdvice(targetAdvice) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                        if (habits.isEmpty()) {
                            item(key = "empty-habits") { StarterTemplatesCard(onAddTemplate = onAddTemplate, onCreateOwn = onAddHabit, modifier = Modifier.animateItem()) }
                        }
                        habits.groupBy { it.habit.routineName }.forEach { (routine, routineHabits) ->
                            if (routine.isNotBlank()) {
                                item(key = "routine-$routine") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).animateItem(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            routine,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                        )
                                        TextButton(onClick = { onStartRoutine(routine) }) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.start_routine))
                                        }
                                    }
                                }
                            }
                            // Completed habits glide to the bottom of their group instead of disappearing.
                            items(routineHabits.sortedBy { it.isComplete }, key = { it.habit.id }) { todayHabit ->
                                HabitRow(
                                    todayHabit = todayHabit,
                                    timerRunning = timer.habitId == todayHabit.habit.id,
                                    timerElapsedSeconds = if (timer.habitId == todayHabit.habit.id) timer.elapsedSeconds else 0,
                                    interactionDisabled = vacationActive,
                                    onToggle = { onToggleHabit(todayHabit.habit.id) },
                                    onIncrement = { onIncrementHabit(todayHabit.habit.id) },
                                    onDecrement = { onDecrementHabit(todayHabit.habit.id) },
                                    onToggleTimer = { onToggleTimer(todayHabit.habit.id) },
                                    onOpen = { onOpenHabit(todayHabit.habit.id) },
                                    onOpenSkipDialog = { skipHabitTarget = todayHabit },
                                    onUndoSkip = { onSkipHabit(todayHabit.habit.id, null) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                    "plan" -> {
                        val todaysTasks = planTasks.filter {
                            !it.completed && (it.scheduledEpochDay == null || it.scheduledEpochDay <= todayEpochDay)
                        }
                        if (carryTasks.isNotEmpty()) {
                            item(key = "section-carry-banner") {
                                SmartCarryBanner(
                                    carryTasksCount = carryTasks.size,
                                    onClick = { selectedCarryTask = carryTasks.first() },
                                )
                            }
                        }
                        if (todaysTasks.isNotEmpty()) {
                            item(key = "section-plan-header") {
                                TodaySectionHeader(icon = Icons.Rounded.Checklist, title = stringResource(R.string.section_plan), actionText = stringResource(R.string.all_categories), onAction = onOpenPlan)
                            }
                            items(todaysTasks.take(5), key = { "today-task-${it.id}" }) { task ->
                                TodayPlanTaskCard(
                                    task = task,
                                    onToggle = { onTogglePlanTask(task) },
                                    onOpen = { onOpenPlanTask(task.id) },
                                    onSmartCarry = if (task.carryCount >= 3) {
                                        { selectedCarryTask = task }
                                    } else null,
                                )
                            }
                        }
                    }
                    "goals" -> {
                        val activeGoals = goals.filter { it.status == GoalStatus.Active }
                        if (activeGoals.isNotEmpty()) {
                            item(key = "section-goals-header") {
                                TodaySectionHeader(icon = Icons.Rounded.Flag, title = stringResource(R.string.section_goals), actionText = stringResource(R.string.all_categories), onAction = onOpenGoals)
                            }
                            items(activeGoals.take(3), key = { "today-goal-${it.id}" }) { goal ->
                                TodayGoalCard(goal = goal, onOpen = onOpenGoals)
                            }
                        }
                    }
                    "focus" -> {
                        item(key = "section-focus") {
                            TodayFocusCard(minutesToday = focusMinutesToday, onStartFocus = onStartFocus)
                        }
                    }
                    "routines" -> {
                        val routines = habits.map { it.habit.routineName }.filter(String::isNotBlank).distinct()
                        if (routines.isNotEmpty()) {
                            item(key = "section-routines-header") {
                                TodaySectionHeader(icon = Icons.Rounded.Repeat, title = stringResource(R.string.section_routines))
                            }
                            items(routines, key = { "routine-card-$it" }) { routineName ->
                                val routineHabits = habits.filter { it.habit.routineName == routineName }
                                val doneCount = routineHabits.count { it.isComplete }
                                TodayRoutineCard(
                                    name = routineName,
                                    total = routineHabits.size,
                                    completed = doneCount,
                                    onStart = { onStartRoutine(routineName) },
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "daily-review-card") {
            Spacer(Modifier.height(6.dp))
            MoodCheckInCard(
                review = dailyReview,
                onPick = onQuickMood,
                onOpenDetails = onOpenDailyReview,
            )
        }
    }

    if (showConfigSheet) {
        TodaySectionsConfigSheet(
            currentOrder = todaySectionOrder,
            currentHidden = todayHiddenSections,
            onSave = onUpdateSectionConfig,
            onClose = { showConfigSheet = false },
        )
    }

    if (skipHabitTarget != null) {
        val target = skipHabitTarget!!
        SkipReasonDialog(
            habitName = target.habit.name,
            onDismiss = { skipHabitTarget = null },
            onConfirm = { reason ->
                onSkipHabit(target.habit.id, reason)
                skipHabitTarget = null
            },
        )
    }

    if (selectedCarryTask != null) {
        val task = selectedCarryTask!!
        SmartCarrySheet(
            task = task,
            onDismiss = { selectedCarryTask = null },
            onDoToday = {
                onTogglePlanTask(task)
                selectedCarryTask = null
            },
            onMoveTomorrow = {
                onMovePlanTask(task.id, todayEpochDay + 1)
                selectedCarryTask = null
            },
            onMoveNextWeek = {
                onMovePlanTask(task.id, todayEpochDay + 7)
                selectedCarryTask = null
            },
            onDivideIntoSubtasks = { steps ->
                onDividePlanTaskIntoSubtasks(task.id, steps)
                selectedCarryTask = null
            },
            onDeleteOrCancel = {
                onDeletePlanTask(task.id)
                selectedCarryTask = null
            },
        )
    }
}

@Composable
private fun GreetingBlock(profileName: String) {
    val locale = LocalConfiguration.current.locales[0]
    val formattedDate = remember(locale) {
        LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM, EEEE", locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> R.string.good_morning
        in 12..17 -> R.string.good_afternoon
        else -> R.string.good_evening
    }
    Column(modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)) {
        Text(formattedDate, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            if (profileName.isBlank()) stringResource(greeting) else stringResource(R.string.personal_greeting, stringResource(greeting), profileName),
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}

@Composable
private fun scheduleSummary(daysMask: Int): String = when (daysMask) {
    127 -> stringResource(R.string.every_day)
    31 -> stringResource(R.string.weekdays)
    96 -> stringResource(R.string.weekends)
    else -> stringResource(R.string.selected_days)
}

@Composable
private fun TodaySectionHeader(
    icon: ImageVector,
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionText, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TodayPlanTaskCard(
    task: PlanTask,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onSmartCarry: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (task.dueMinutes != null) {
                        val h = task.dueMinutes / 60
                        val m = task.dueMinutes % 60
                        Text("⏰ %02d:%02d".format(h, m), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (task.carryCount > 0) {
                        Text(
                            stringResource(R.string.carried_times, task.carryCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (task.carryCount >= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (task.carryCount >= 3 && onSmartCarry != null) {
                TextButton(onClick = onSmartCarry) {
                    Text(stringResource(R.string.resolve_action), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SmartCarryBanner(
    carryTasksCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("💡", fontSize = 18.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.smart_carry_assistant),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.carry_banner_message, carryTasksCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TodayDailyReviewCard(
    review: DailyReview?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val emoji = when (review?.mood) {
                1 -> "🌧️"
                2 -> "⛅"
                3 -> "🌤️"
                4 -> "☀️"
                5 -> "✨"
                else -> "🌙"
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 22.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (review != null) stringResource(R.string.daily_review_completed) else stringResource(R.string.do_evening_review),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (review != null && review.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = review.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TodayGoalCard(
    goal: Goal,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(goal.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${goal.currentValue.toInt()} / ${goal.targetValue.toInt()} ${goal.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { goal.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun TodayFocusCard(
    minutesToday: Int,
    onStartFocus: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.focus), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.focus_minutes_today, minutesToday), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onStartFocus) {
                Text(stringResource(R.string.start_focus), maxLines = 1)
            }
        }
    }
}

@Composable
private fun TodayRoutineCard(
    name: String,
    total: Int,
    completed: Int,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.completed_of_total, completed, total), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onStart, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.start_routine))
            }
        }
    }
}

