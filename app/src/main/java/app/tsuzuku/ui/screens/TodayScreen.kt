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
import app.tsuzuku.model.ContentStatus
import app.tsuzuku.model.Goal
import app.tsuzuku.model.GoalStatus
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.GoalPeriod
import app.tsuzuku.model.Insight
import app.tsuzuku.model.LibraryItem
import app.tsuzuku.model.PlanTask
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.ui.theme.Success
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
    libraryItems: List<LibraryItem> = emptyList(),
    focusMinutesToday: Int = 0,
    todaySectionOrder: String = "summary,intelligence,habits,plan,goals,library,focus,health,routines",
    todayHiddenSections: String = "",
    healthSteps: Long = 0L,
    onUpdateSectionConfig: (newOrder: String, newHidden: String) -> Unit = { _, _ -> },
    onStartFocus: () -> Unit = {},
    onOpenInbox: () -> Unit = {},
    onStartRoutine: (String) -> Unit = {},
    onTogglePlanTask: (PlanTask) -> Unit = {},
    onOpenPlanTask: (Long) -> Unit = {},
    onOpenContent: (Long) -> Unit = {},
    onIncrementContent: (Long, Double) -> Unit = { _, _ -> },
    dailyReview: DailyReview? = null,
    onOpenDailyReview: () -> Unit = {},
    onSkipHabit: (Long, String?) -> Unit = { _, _ -> },
    onMovePlanTask: (Long, Long?) -> Unit = { _, _ -> },
    onDeletePlanTask: (Long) -> Unit = {},
    onDividePlanTaskIntoSubtasks: (Long, List<String>) -> Unit = { _, _ -> },
    insights: List<Insight> = emptyList(),
    onOpenIntelligence: () -> Unit = {},
    onOpenProjects: () -> Unit = {},
) {
    val active = habits.filterNot(TodayHabit::isComplete)
    val completed = habits.filter(TodayHabit::isComplete)
    var completedExpanded by rememberSaveable { mutableStateOf(false) }
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
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { BrandLockup() }
        item { GreetingBlock(profileName) }
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
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("今日 · TODAY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.6.sp)
                    Text(stringResource(R.string.today), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenIntelligence) {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = "Tsuzuku Intelligence")
                    }
                    IconButton(onClick = onOpenInbox) {
                        Icon(Icons.Rounded.Inbox, contentDescription = stringResource(R.string.inbox_label))
                    }
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search_everything))
                    }
                    IconButton(onClick = onOpenGoals) {
                        Icon(Icons.Rounded.Flag, contentDescription = stringResource(R.string.open_goals))
                    }
                    IconButton(onClick = { showConfigSheet = true }) {
                        Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.customize_sections))
                    }
                }
            }
        }

        orderList.forEach { sectionId ->
            if (sectionId !in hiddenSet) {
                when (sectionId) {
                    "summary" -> {
                        if (showDailySummary) {
                            item(key = "section-summary") { DailySummary(completed = completed.size, total = habits.size) }
                        }
                    }
                    "intelligence" -> {
                        if (insights.isNotEmpty()) {
                            item(key = "section-intelligence") {
                                TodayIntelligenceCard(
                                    insights = insights,
                                    onOpenIntelligence = onOpenIntelligence,
                                )
                            }
                        }
                    }
                    "habits" -> {
                        item(key = "section-habits-header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text("習慣", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.8.sp)
                                    Text(stringResource(R.string.todays_habits), style = MaterialTheme.typography.titleLarge)
                                }
                                IconButton(onClick = onReorderHabits) {
                                    Icon(Icons.Rounded.SwapVert, contentDescription = stringResource(R.string.reorder_habits))
                                }
                            }
                        }
                        if (habits.isEmpty()) {
                            item(key = "empty-habits") { EmptyTodayCard(onAddHabit) }
                        }
                        active.groupBy { it.habit.routineName }.forEach { (routine, routineHabits) ->
                            if (routine.isNotBlank()) {
                                item(key = "routine-$routine") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                                            Text(
                                                routine.uppercase(LocalConfiguration.current.locales[0]),
                                                modifier = Modifier.padding(start = 8.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                letterSpacing = 1.1.sp,
                                            )
                                        }
                                        TextButton(onClick = { onStartRoutine(routine) }) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.start_routine), style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                            items(routineHabits, key = { it.habit.id }) { todayHabit ->
                                TodayHabitCard(
                                    todayHabit = todayHabit,
                                    onToggle = { onToggleHabit(todayHabit.habit.id) },
                                    onIncrement = { onIncrementHabit(todayHabit.habit.id) },
                                    onDecrement = { onDecrementHabit(todayHabit.habit.id) },
                                    timerRunning = timer.habitId == todayHabit.habit.id,
                                    timerElapsedSeconds = if (timer.habitId == todayHabit.habit.id) timer.elapsedSeconds else 0,
                                    onToggleTimer = { onToggleTimer(todayHabit.habit.id) },
                                    onOpen = { onOpenHabit(todayHabit.habit.id) },
                                    onSkip = { reason -> onSkipHabit(todayHabit.habit.id, reason) },
                                    onOpenSkipDialog = { skipHabitTarget = todayHabit },
                                    interactionDisabled = vacationActive,
                                )
                            }
                        }
                        if (completed.isNotEmpty()) {
                            item(key = "completed-header") {
                                Surface(
                                    onClick = { completedExpanded = !completedExpanded },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 13.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            pluralStringResource(R.plurals.completed_section, completed.size, completed.size),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Icon(
                                            if (completedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = null,
                                        )
                                    }
                                }
                            }
                            items(completed, key = { "completed-${it.habit.id}" }) { todayHabit ->
                                AnimatedVisibility(visible = completedExpanded) {
                                    TodayHabitCard(
                                        todayHabit = todayHabit,
                                        onToggle = { onToggleHabit(todayHabit.habit.id) },
                                        onIncrement = { onIncrementHabit(todayHabit.habit.id) },
                                        onDecrement = { onDecrementHabit(todayHabit.habit.id) },
                                        timerRunning = timer.habitId == todayHabit.habit.id,
                                        timerElapsedSeconds = if (timer.habitId == todayHabit.habit.id) timer.elapsedSeconds else 0,
                                        onToggleTimer = { onToggleTimer(todayHabit.habit.id) },
                                        onOpen = { onOpenHabit(todayHabit.habit.id) },
                                        onSkip = { reason -> onSkipHabit(todayHabit.habit.id, reason) },
                                        onOpenSkipDialog = { skipHabitTarget = todayHabit },
                                        interactionDisabled = vacationActive,
                                    )
                                }
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
                                TodaySectionHeader(icon = Icons.Rounded.Checklist, title = stringResource(R.string.section_plan))
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
                    "library" -> {
                        val inProgressItems = libraryItems.filter { it.content.status == ContentStatus.InProgress }
                        if (inProgressItems.isNotEmpty()) {
                            item(key = "section-library-header") {
                                TodaySectionHeader(icon = Icons.Rounded.MenuBook, title = stringResource(R.string.section_library))
                            }
                            items(inProgressItems.take(3), key = { "today-lib-${it.content.id}" }) { item ->
                                TodayLibraryCard(item = item, onOpen = { onOpenContent(item.content.id) }, onIncrement = { onIncrementContent(item.content.id, 1.0) })
                            }
                        }
                    }
                    "focus" -> {
                        item(key = "section-focus") {
                            TodayFocusCard(minutesToday = focusMinutesToday, onStartFocus = onStartFocus)
                        }
                    }
                    "health" -> {
                        item(key = "section-health") {
                            TodayHealthCard(steps = healthSteps)
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
            TodayDailyReviewCard(
                review = dailyReview,
                onClick = onOpenDailyReview,
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
private fun BrandLockup() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.brand_japanese),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        Box(
            Modifier
                .width(1.dp)
                .height(22.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Text(
            stringResource(R.string.app_name).uppercase(LocalConfiguration.current.locales[0]),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        Text(
            if (profileName.isBlank()) stringResource(greeting) else stringResource(R.string.personal_greeting, stringResource(greeting), profileName),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(5.dp))
        Text(formattedDate, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DailySummary(completed: Int, total: Int) {
    val progress = if (total == 0) 0f else completed.toFloat() / total
    val remaining = (total - completed).coerceAtLeast(0)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
    ) {
        Row {
            Box(Modifier.width(4.dp).height(118.dp).background(MaterialTheme.colorScheme.secondary))
            Column(Modifier.padding(14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        stringResource(R.string.daily_progress).uppercase(LocalConfiguration.current.locales[0]),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("%${(progress * 100).toInt()}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        stringResource(R.string.goal_fraction, completed, total),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(stringResource(R.string.completed_short, completed), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.remaining_short, remaining), style = MaterialTheme.typography.labelSmall)
            }
            }
        }
    }
}

@Composable
private fun EmptyTodayCard(onAddHabit: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.first_step_mark), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.4.sp)
            Text(stringResource(R.string.empty_today), color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalIconButton(onClick = onAddHabit) {
                Icon(Icons.Rounded.Add, contentDescription = null)
            }
        }
    }
}

@Composable
private fun TodayHabitCard(
    todayHabit: TodayHabit,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    timerRunning: Boolean,
    timerElapsedSeconds: Long,
    onToggleTimer: () -> Unit,
    onOpen: () -> Unit,
    onSkip: (String?) -> Unit = {},
    onOpenSkipDialog: () -> Unit = {},
    interactionDisabled: Boolean = false,
) {
    val habit = todayHabit.habit
    val haptic = LocalHapticFeedback.current
    val containerColor by animateColorAsState(
        if (todayHabit.isComplete) Success.copy(alpha = 0.07f) else Color.Transparent,
        label = "habit-card",
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.width(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        habit.reminderMinutes?.let(::formatReminderTime) ?: "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (habit.reminderMinutes != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary,
                    )
                    Box(Modifier.size(7.dp).background(Color(habit.colorArgb.toInt()), CircleShape))
                }
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(habit.colorArgb.toInt()).copy(alpha = 0.14f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(habit.emoji, fontSize = 21.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(habit.name, style = MaterialTheme.typography.titleMedium)
                        if (habit.currentLevel > 1) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    "Lv. ${habit.currentLevel}",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                "${todayHabit.masteryStage.icon} ${todayHabit.masteryStage.title}",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        scheduleSummary(habit.scheduleDaysMask),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (todayHabit.skipReason != null) {
                        val (parsedType, parsedLabel) = SkipReasonType.parse(todayHabit.skipReason)
                        Spacer(Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.clickable { onOpenSkipDialog() },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    "${parsedType.icon} $parsedLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text("✎", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                    if (todayHabit.badHabitLimitExceeded) {
                        Spacer(Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                stringResource(R.string.limit_exceeded, formatAmount(todayHabit.progress), formatAmount(habit.maximumTarget ?: habit.target), habit.unit),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else if (todayHabit.badHabitLimitWarning) {
                        Spacer(Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        ) {
                            Text(
                                stringResource(R.string.limit_approaching, formatAmount(todayHabit.progress), formatAmount(habit.maximumTarget ?: habit.target), habit.unit),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                    if (habit.type != HabitType.Check) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(
                                R.string.progress_template,
                                formatAmount(todayHabit.progress),
                                formatAmount(habit.target),
                                habit.unit,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (habit.goalPeriod != GoalPeriod.Daily) {
                        Text(
                            stringResource(R.string.period_progress, todayHabit.periodProgress, habit.periodTarget),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (todayHabit.isMinimumComplete && !todayHabit.isComplete) {
                        Text(
                            stringResource(R.string.minimum_goal_reached),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    if (todayHabit.isBlocked) {
                        Text(
                            stringResource(R.string.blocked_by_habit, todayHabit.blockedByName.orEmpty()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.manage_habit))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.open_details)) },
                            onClick = {
                                menuExpanded = false
                                onOpen()
                            },
                        )
                        if (todayHabit.skipReason == null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.skip_with_reason)) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenSkipDialog()
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.undo_skip)) },
                                onClick = {
                                    menuExpanded = false
                                    onSkip(null)
                                },
                            )
                        }
                    }
                }
                if (habit.type == HabitType.Check) {
                    FilledTonalIconButton(
                        enabled = !todayHabit.isBlocked && !interactionDisabled,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onToggle()
                        },
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.complete_habit))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDecrement, enabled = todayHabit.progress > 0 && !todayHabit.isBlocked && !interactionDisabled) {
                            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.decrease_progress))
                        }
                        FilledIconButton(
                            enabled = !todayHabit.isBlocked && !interactionDisabled,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onIncrement()
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.increase_progress))
                        }
                    }
                }
            }
            if (habit.type != HabitType.Check) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { todayHabit.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = if (habit.direction == HabitDirection.Quit) {
                        if (todayHabit.badHabitLimitExceeded) MaterialTheme.colorScheme.error
                        else if (todayHabit.badHabitLimitWarning) MaterialTheme.colorScheme.tertiary
                        else Success
                    } else {
                        if (todayHabit.isComplete) Success else Color(habit.colorArgb.toInt())
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                if (habit.type == HabitType.Duration && (!todayHabit.isComplete || habit.direction == HabitDirection.Quit)) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        onClick = onToggleTimer,
                        enabled = !todayHabit.isBlocked && !interactionDisabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (timerRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                if (timerRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                            )
                            Text(
                                if (timerRunning) stringResource(R.string.stop_timer) else stringResource(R.string.start_timer),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                formatTimer(timerElapsedSeconds),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.getDefault(), "%.1f", value)

private fun formatTimer(totalSeconds: Long): String =
    "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)

private fun formatReminderTime(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

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
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                    text = stringResource(R.string.evening_review_mark),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(2.dp))
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
private fun TodayLibraryCard(
    item: LibraryItem,
    onOpen: () -> Unit,
    onIncrement: () -> Unit,
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.content.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${item.content.currentUnits.toInt()} / ${item.content.totalUnits.toInt()} ${item.content.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onIncrement,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("+1")
            }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            Button(onClick = onStartFocus, shape = RoundedCornerShape(10.dp)) {
                Text(stringResource(R.string.start_focus))
            }
        }
    }
}

@Composable
private fun TodayHealthCard(steps: Long) {
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
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.section_health), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (steps > 0) stringResource(R.string.steps_completed, steps) else stringResource(R.string.no_step_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

@Composable
private fun TodayIntelligenceCard(
    insights: List<Insight>,
    onOpenIntelligence: () -> Unit,
) {
    if (insights.isEmpty()) return
    val topInsight = insights.first()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenIntelligence),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        "TSUZUKU INTELLIGENCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                ) {
                    Text(
                        text = topInsight.confidence.label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${topInsight.type.icon} ${topInsight.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = topInsight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (insights.size > 1) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.more_insights_see_all, insights.size - 1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

