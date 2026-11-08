package app.tsuzuku.ui

import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.tsuzuku.model.feelingSummary
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.launch
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.tsuzuku.R
import app.tsuzuku.data.BackupRepository
import app.tsuzuku.data.CategoryRepository
import app.tsuzuku.data.CloudSyncRepository
import app.tsuzuku.data.ConnectionRepository
import app.tsuzuku.data.DiagnosticStore
import app.tsuzuku.data.FocusRepository
import app.tsuzuku.data.GoalRepository
import app.tsuzuku.data.HabitRepository
import app.tsuzuku.data.LibraryRepository
import app.tsuzuku.data.PlanRepository
import app.tsuzuku.data.ReviewRepository
import app.tsuzuku.data.preferences.AppSettings
import app.tsuzuku.data.preferences.LanguagePreference
import app.tsuzuku.data.preferences.ThemePreference
import app.tsuzuku.data.preferences.TimerSessionStore
import app.tsuzuku.data.preferences.WeekStartPreference
import app.tsuzuku.model.FocusMode
import app.tsuzuku.model.FocusTargetType
import app.tsuzuku.model.GoalStatus
import app.tsuzuku.model.ReviewPeriodType
import app.tsuzuku.notifications.HabitReminderScheduler
import app.tsuzuku.ui.screens.BackupScreen
import app.tsuzuku.ui.screens.CalendarScreen
import app.tsuzuku.ui.screens.CategoryManagerScreen
import app.tsuzuku.ui.screens.CloudAccountScreen
import app.tsuzuku.ui.screens.DailyReviewSheet
import app.tsuzuku.ui.screens.DiagnosticsScreen
import app.tsuzuku.ui.screens.FocusScreen
import app.tsuzuku.ui.screens.GoalDetailScreen
import app.tsuzuku.ui.screens.GoalEditorScreen
import app.tsuzuku.ui.screens.GoalsScreen
import app.tsuzuku.ui.screens.HabitDetailScreen
import app.tsuzuku.ui.screens.HabitEditorScreen
import app.tsuzuku.ui.screens.HabitOrderScreen
import app.tsuzuku.ui.screens.PlanTabScreen
import app.tsuzuku.ui.screens.PlanTaskDetailScreen
import app.tsuzuku.ui.screens.PlanTaskEditorScreen
import app.tsuzuku.ui.screens.PlanningScreen
import app.tsuzuku.ui.screens.ProfileScreen
import app.tsuzuku.ui.screens.RoutinePlayerScreen
import app.tsuzuku.ui.screens.SecurityScreen
import app.tsuzuku.ui.screens.StatisticsScreen
import app.tsuzuku.ui.screens.StatisticsTab
import app.tsuzuku.ui.screens.TodayScreen
import app.tsuzuku.ui.screens.UnifiedSearchScreen
import app.tsuzuku.widget.TsuzukuWidgetProvider

internal data class MainDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
)

/** Three tabs: what to do today, how it is going, and settings. */
internal val mainDestinations = listOf(
    MainDestination("today", R.string.today, Icons.Rounded.Home),
    MainDestination("progress", R.string.progress_tab, Icons.Rounded.BarChart),
    MainDestination("settings", R.string.settings_label, Icons.Rounded.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TsuzukuApp(
    repository: HabitRepository,
    libraryRepository: LibraryRepository,
    reviewRepository: ReviewRepository,
    goalRepository: GoalRepository,
    planRepository: PlanRepository,
    categoryRepository: CategoryRepository,
    backupRepository: BackupRepository,
    cloudSyncRepository: CloudSyncRepository,
    diagnosticStore: DiagnosticStore,
    reminderScheduler: HabitReminderScheduler,
    timerSessionStore: TimerSessionStore,
    settings: AppSettings,
    onSetProfileName: (String) -> Unit,
    onSetTheme: (ThemePreference) -> Unit,
    onSetPalette: (app.tsuzuku.data.preferences.PalettePreference) -> Unit = {},
    onSetPureBlack: (Boolean) -> Unit = {},
    onSetWidgetOpacity: (Int) -> Unit = {},
    onSetLanguage: (LanguagePreference) -> Unit,
    onSetWeekStart: (WeekStartPreference) -> Unit,
    onSetShowDailySummary: (Boolean) -> Unit,
    onSetProtectScreen: (Boolean) -> Unit,
    onSetCloudSyncEnabled: (Boolean) -> Unit,
    onSetVacation: (Long, Long) -> Unit,
    onClearVacation: () -> Unit,
    biometricAvailable: Boolean,
    onSetPin: (String) -> Boolean,
    onChangePin: (String, String) -> Boolean,
    onDisablePin: (String) -> Boolean,
    onSetBiometric: (Boolean) -> Unit,
    onSetLockDelayMinutes: (Int) -> Unit,
    focusRepository: FocusRepository,
    connectionRepository: ConnectionRepository,
    onSetTodaySectionOrder: (String) -> Unit = {},
    onSetTodayHiddenSections: (String) -> Unit = {},
    launchRoute: String? = null,
    onLaunchRouteHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    LaunchedEffect(launchRoute) {
        val route = launchRoute ?: return@LaunchedEffect
        // A new pre-filled habit replaces one that is already open, so its values show.
        if (route == "habit/new" && navController.currentDestination?.route == "habit/new") navController.popBackStack()
        navController.navigate(route) {
            if (route == "progress") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                restoreState = true
            }
            launchSingleTop = true
        }
        onLaunchRouteHandled()
    }
    val todayViewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.factory(repository, timerSessionStore) {
            TsuzukuWidgetProvider.requestUpdate(context.applicationContext)
        },
    )
    val goalViewModel: GoalViewModel = viewModel(factory = GoalViewModel.factory(goalRepository))
    val planViewModel: PlanViewModel = viewModel(factory = PlanViewModel.factory(planRepository))
    val categoryViewModel: CategoryViewModel = viewModel(factory = CategoryViewModel.factory(categoryRepository))
    val focusViewModel: FocusViewModel = viewModel(
        factory = FocusViewModel.factory(focusRepository, planRepository, repository, goalRepository),
    )
    val connectionViewModel: ConnectionViewModel = viewModel(
        factory = ConnectionViewModel.factory(connectionRepository, goalRepository, libraryRepository),
    )
    val reviewViewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.factory(reviewRepository))

    // Only state shared by several destinations lives here; screen-specific state is collected
    // inside its own destination so a check-off on Today does not recompose unrelated screens.
    val habits by todayViewModel.habits.collectAsStateWithLifecycle()
    val smartReminderContext = androidx.compose.ui.platform.LocalContext.current
    val smartReminderPrefs = remember { app.tsuzuku.data.SmartReminderPrefs(smartReminderContext) }
    val smartReminderScope = androidx.compose.runtime.rememberCoroutineScope()
    var reminderSuggestion by remember { androidx.compose.runtime.mutableStateOf<app.tsuzuku.model.ReminderSuggestion?>(null) }
    var availableUpdate by remember { androidx.compose.runtime.mutableStateOf<app.tsuzuku.update.AvailableUpdate?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        availableUpdate = app.tsuzuku.update.UpdateChecker.check(smartReminderContext)
    }
    var showWhatsNew by remember {
        androidx.compose.runtime.mutableStateOf(app.tsuzuku.update.WhatsNew.shouldShow(smartReminderContext, existingUser = settings.onboardingCompleted))
    }
    if (showWhatsNew) {
        app.tsuzuku.ui.screens.WhatsNewDialog(onDismiss = {
            app.tsuzuku.update.WhatsNew.markSeen(smartReminderContext)
            showWhatsNew = false
        })
    }
    androidx.compose.runtime.LaunchedEffect(habits.map { it.habit.id to it.habit.reminderMinutes }) {
        val today = java.time.LocalDate.now()
        reminderSuggestion = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val minutes = repository.completionMinutes(app.tsuzuku.model.SMART_REMINDER_WINDOW_DAYS)
            habits.asSequence()
                .filterNot { smartReminderPrefs.isSnoozed(it.habit.id, today) }
                .mapNotNull { app.tsuzuku.model.reminderSuggestion(it.habit, minutes[it.habit.id].orEmpty()) }
                .firstOrNull()
        }
    }
    val history by todayViewModel.history.collectAsStateWithLifecycle()
    val timer by todayViewModel.timer.collectAsStateWithLifecycle()
    val goals by goalViewModel.goals.collectAsStateWithLifecycle()
    val planTasks by planViewModel.tasks.collectAsStateWithLifecycle()
    val categories by categoryViewModel.categories.collectAsStateWithLifecycle()
    val habitGoalLinks by connectionViewModel.habitGoalLinks.collectAsStateWithLifecycle()
    val taskGoalLinks by connectionViewModel.taskGoalLinks.collectAsStateWithLifecycle()
    var showDailyReviewSheet by remember { mutableStateOf(false) }
    var statisticsTab by remember { mutableStateOf(StatisticsTab.General) }
    var progressPage by rememberSaveable { mutableIntStateOf(0) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "today"

    // "How did it go?" after completing a habit on Today; hides itself after a few seconds.
    var feelingPromptHabit by remember { mutableStateOf<app.tsuzuku.model.Habit?>(null) }
    LaunchedEffect(feelingPromptHabit) {
        if (feelingPromptHabit != null) {
            kotlinx.coroutines.delay(9_000)
            feelingPromptHabit = null
        }
    }
    val askFeeling: (app.tsuzuku.model.Habit) -> Unit = { habit ->
        if (app.tsuzuku.ui.screens.FeelingPromptSetting.isEnabled(context)) feelingPromptHabit = habit
    }

    Scaffold(
        snackbarHost = {
            val habit = feelingPromptHabit
            androidx.compose.animation.AnimatedVisibility(
                visible = habit != null && currentRoute == "today",
                enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut(),
            ) {
                if (habit != null) {
                    app.tsuzuku.ui.screens.FeelingPrompt(
                        habit = habit,
                        onPick = { feeling ->
                            todayViewModel.setFeeling(habit.id, feeling)
                            feelingPromptHabit = null
                        },
                        onDismiss = { feelingPromptHabit = null },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        },
        topBar = {
            mainDestinations.firstOrNull { it.route == currentRoute }?.let { destination ->
                MainTopBar(
                    title = stringResource(destination.labelRes),
                    onSearch = if (destination.route == "today") ({ navController.navigate("search") }) else null,
                    // Past days are edited in Progress > Calendar; one tap from Today.
                    onCalendar = if (destination.route == "today") ({
                        progressPage = 1
                        navController.navigate("progress") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) else null,
                )
            }
        },
        floatingActionButton = {
            if (currentRoute == "today") {
                FloatingActionButton(onClick = { navController.navigate("habit/new") }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_habit))
                }
            }
        },
        bottomBar = {
            if (currentRoute in mainRoutes) {
                MainNavigationBar(
                    destinations = mainDestinations,
                    currentRoute = currentRoute,
                    onSelect = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        WarmUpOnce()
        androidx.compose.runtime.CompositionLocalProvider(
            LocalScaffoldPadding provides innerPadding,
            LocalCurrentRoute provides currentRoute,
        ) {
        NavHost(
            navController = navController,
            startDestination = "today",
            // Tabs cross-fade with a slight zoom (M3 "fade through"); detail screens slide in
            // along the x-axis and back out the same way (M3 "shared axis").
            enterTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route)) fadeThroughIn()
                else sharedAxisIn(forward = true)
            },
            exitTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route)) fadeThroughOut()
                else sharedAxisOut(forward = true)
            },
            popEnterTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route)) fadeThroughIn()
                else sharedAxisIn(forward = false)
            },
            popExitTransition = {
                if (isTabSwitch(initialState.destination.route, targetState.destination.route)) fadeThroughOut()
                else sharedAxisOut(forward = false)
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            screen("today") {
                val dailyReviewToday by reviewViewModel.dailyReviewToday.collectAsStateWithLifecycle()
                val focusSessions by focusViewModel.sessions.collectAsStateWithLifecycle()
                val todayEpoch = remember { java.time.LocalDate.now().toEpochDay() }
                val todayStartMillis = remember {
                    java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
                val coaching = remember { app.tsuzuku.data.CoachingPrefs(context) }
                var adviceVersion by remember { mutableIntStateOf(0) }
                val targetAdvice = remember(history, adviceVersion) { coaching.pickAdvice(history, java.time.LocalDate.now()) }
                TodayScreen(
                    habits = habits,
                    targetAdvice = targetAdvice,
                    onAcceptAdvice = { advice ->
                        coaching.accepted(advice, java.time.LocalDate.now())
                        if (advice.changesFrequency) {
                            todayViewModel.setWeeklyGoal(advice.habit.id, advice.newTarget.toInt())
                        } else {
                            todayViewModel.setHabitTarget(advice.habit.id, advice.newTarget)
                        }
                        adviceVersion++
                    },
                    onDismissAdvice = { advice ->
                        coaching.dismissed(advice, java.time.LocalDate.now())
                        adviceVersion++
                    },
                    onQuickMood = { mood -> reviewViewModel.quickMood(mood, dailyReviewToday) },
                    comebackHabitIds = remember(history, habits) {
                        val today = java.time.LocalDate.now()
                        habits.filter { !it.isComplete && history.missedYesterday(it.habit, today) }.map { it.habit.id }.toSet()
                    },
                    onTinyVersion = todayViewModel::completeTinyVersion,
                    onReorderList = todayViewModel::reorderHabits,
                    reminderSuggestion = reminderSuggestion,
                    availableUpdate = availableUpdate,
                    onOpenUpdate = { update ->
                        runCatching {
                            smartReminderContext.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(update.pageUrl)))
                        }
                    },
                    onDismissUpdate = { update ->
                        app.tsuzuku.update.UpdateChecker.dismiss(smartReminderContext, update.version)
                        availableUpdate = null
                    },
                    onAcceptReminder = { suggestion ->
                        smartReminderPrefs.snooze(suggestion.habit.id, java.time.LocalDate.now())
                        reminderSuggestion = null
                        smartReminderScope.launch {
                            repository.setReminderMinutes(suggestion.habit.id, suggestion.toMinutes)
                            reminderScheduler.schedule(suggestion.habit.id, suggestion.habit.scheduleDaysMask, suggestion.toMinutes)
                        }
                    },
                    onDismissReminder = { suggestion ->
                        smartReminderPrefs.snooze(suggestion.habit.id, java.time.LocalDate.now())
                        reminderSuggestion = null
                    },
                    comebacksThisMonth = remember(history) {
                        val today = java.time.LocalDate.now()
                        history.comebackCount(today.withDayOfMonth(1), today)
                    },
                    weekProgress = remember(history, settings.weekStart) {
                        val today = java.time.LocalDate.now()
                        val first = if (settings.weekStart == WeekStartPreference.Sunday) java.time.DayOfWeek.SUNDAY else java.time.DayOfWeek.MONDAY
                        val start = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(first))
                        (0L until 7L).map { offset ->
                            val date = start.plusDays(offset)
                            val summary = history.daySummary(date)
                            app.tsuzuku.ui.screens.WeekDayProgress(
                                date,
                                if (date.isAfter(today) || summary.scheduledCount == 0) null else summary.progressFraction,
                            )
                        }
                    },
                    vacationActive = settings.vacationStartEpochDay?.let { start ->
                        settings.vacationEndEpochDay?.let { end -> java.time.LocalDate.now().toEpochDay() in start..end }
                    } == true,
                    profileName = settings.profileName,
                    showDailySummary = settings.showDailySummary,
                    timer = timer,
                    onAddHabit = { navController.navigate("habit/new") },
                    onAddTemplate = { draft ->
                        todayViewModel.addHabit(draft) { habitId ->
                            if (draft.reminderMinutes != null) {
                                reminderScheduler.schedule(habitId, draft.scheduleDaysMask, draft.reminderMinutes)
                            }
                        }
                    },
                    onToggleHabit = { habitId ->
                        val h = habits.firstOrNull { it.habit.id == habitId }
                        todayViewModel.toggleHabit(habitId)
                        if (h != null && !h.isComplete) {
                            connectionViewModel.onHabitProgressApplied(habitId, 1.0)
                            askFeeling(h.habit)
                        }
                    },
                    onIncrementHabit = { habitId ->
                        val h = habits.firstOrNull { it.habit.id == habitId }
                        todayViewModel.incrementHabit(habitId)
                        connectionViewModel.onHabitProgressApplied(habitId, 1.0)
                        if (h != null && !h.isComplete && h.progress + h.habit.step >= h.habit.target) askFeeling(h.habit)
                    },
                    onDecrementHabit = todayViewModel::decrementHabit,
                    onToggleTimer = todayViewModel::toggleTimer,
                    onOpenHabit = { id -> navController.navigate("habit/$id") },
                    onReorderHabits = { navController.navigate("habit/order") },
                    onOpenGoals = { navController.navigate("goals") },
                    onSearch = { navController.navigate("search") },
                    planTasks = planTasks.filter { it.scheduledEpochDay == null || it.scheduledEpochDay == todayEpoch },
                    goals = goals,
                    focusMinutesToday = focusSessions.filter { it.startedAt >= todayStartMillis }.sumOf { it.durationMinutes },
                    todaySectionOrder = settings.todaySectionOrder,
                    todayHiddenSections = settings.todayHiddenSections,
                    onUpdateSectionConfig = { order, hidden ->
                        onSetTodaySectionOrder(order)
                        onSetTodayHiddenSections(hidden)
                    },
                    onStartFocus = { navController.navigate("focus") },
                    onStartRoutine = { routineName -> navController.navigate("routine/$routineName") },
                    onTogglePlanTask = { task ->
                        planViewModel.toggle(task)
                        if (!task.completed) connectionViewModel.onTaskCompletedApplied(task.id)
                    },
                    onOpenPlanTask = { taskId -> navController.navigate("plan/$taskId") },
                    onOpenPlan = { navController.navigate("plan") },
                    dailyReview = dailyReviewToday,
                    onOpenDailyReview = { showDailyReviewSheet = true },
                    onSkipHabit = todayViewModel::skipHabit,
                    onMovePlanTask = { taskId, targetEpoch -> planViewModel.move(taskId, targetEpoch) },
                    onDeletePlanTask = planViewModel::delete,
                    onDividePlanTaskIntoSubtasks = { taskId, steps ->
                        steps.forEach { planViewModel.addChecklistItem(taskId, it) }
                    },
                )
            }
            screen("plan") {
                val planTemplates by planViewModel.templates.collectAsStateWithLifecycle()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.plan)) },
                            navigationIcon = {
                                IconButton(onClick = navController::popBackStack) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                                }
                            },
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { navController.navigate("plan/new") }) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_task))
                        }
                    },
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        PlanTabScreen(
                            tasks = planTasks,
                            templates = planTemplates,
                            onToggle = planViewModel::toggle,
                            onOpen = { navController.navigate("plan/$it") },
                            onMoveTomorrow = { planViewModel.move(it, java.time.LocalDate.now().plusDays(1).toEpochDay()) },
                            onDelete = planViewModel::delete,
                            onUseTemplate = planViewModel::createFromTemplate,
                            onReorder = planViewModel::reorder,
                            onBulkMove = planViewModel::bulkMove,
                            onBulkComplete = planViewModel::bulkComplete,
                            onBulkDelete = planViewModel::bulkDelete,
                            onBulkAddTags = planViewModel::bulkAddTags,
                        )
                    }
                }
            }
            screen("plan/new") {
                PlanTaskEditorScreen(
                    customCategories = categories,
                    onBack = navController::popBackStack,
                    onSave = {
                        planViewModel.create(it)
                        navController.popBackStack()
                    },
                    onSaveTemplate = {
                        planViewModel.saveTemplate(it)
                        navController.popBackStack()
                    },
                )
            }
            screen(
                route = "plan/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
            ) { entry ->
                val taskId = entry.arguments?.getLong("taskId") ?: return@screen
                planTasks.firstOrNull { it.id == taskId }?.let { task ->
                    val linkedLink = taskGoalLinks.firstOrNull { it.taskId == taskId }
                    val linkedGoal = linkedLink?.let { link ->
                        goals.firstOrNull { it.id == link.goalId }?.let { goal -> Pair(goal, link) }
                    }
                    PlanTaskDetailScreen(
                        task = task,
                        onBack = navController::popBackStack,
                        onToggle = {
                            planViewModel.toggle(task)
                            if (!task.completed) connectionViewModel.onTaskCompletedApplied(task.id)
                        },
                        onMove = { planViewModel.move(taskId, it) },
                        onAddChecklist = { planViewModel.addChecklistItem(taskId, it) },
                        onToggleChecklist = planViewModel::toggleChecklistItem,
                        onDeleteChecklist = planViewModel::deleteChecklistItem,
                        onSetActualMinutes = { planViewModel.setActualMinutes(taskId, it) },
                        onDuplicate = { planViewModel.duplicate(task) },
                        onEdit = { navController.navigate("plan/$taskId/edit") },
                        onStartFocus = {
                            focusViewModel.startSession(
                                mode = FocusMode.Pomodoro,
                                targetType = FocusTargetType.Task,
                                targetId = task.id,
                                targetTitle = task.title,
                            )
                            navController.navigate("focus")
                        },
                        linkedGoal = linkedGoal,
                        availableGoals = goals.filter { it.status == GoalStatus.Active },
                        onLinkGoal = { goalId, value -> connectionViewModel.linkTaskGoal(taskId, goalId, value) },
                        onUnlinkGoal = { goalId: Long -> connectionViewModel.unlinkTaskGoal(taskId, goalId) },
                    )
                }
            }
            screen(
                route = "plan/{taskId}/edit",
                arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
            ) { entry ->
                val taskId = entry.arguments?.getLong("taskId") ?: return@screen
                planTasks.firstOrNull { it.id == taskId }?.let { task ->
                    PlanTaskEditorScreen(
                        existing = task,
                        customCategories = categories,
                        onBack = navController::popBackStack,
                        onSave = { draft -> planViewModel.update(taskId, draft); navController.popBackStack() },
                    )
                }
            }
            screen("progress") {
                val weeklySummary by reviewViewModel.weeklySummary.collectAsStateWithLifecycle()
                val monthlySummary by reviewViewModel.monthlySummary.collectAsStateWithLifecycle()
                val yearlySummary by reviewViewModel.yearlySummary.collectAsStateWithLifecycle()
                val personalRecords by reviewViewModel.personalRecords.collectAsStateWithLifecycle()
                val heatmapDays by reviewViewModel.heatmapDays.collectAsStateWithLifecycle()
                val selectedHeatmapHabitId by reviewViewModel.selectedHeatmapHabitId.collectAsStateWithLifecycle()
                val skipReasonStats by reviewViewModel.skipReasonStats.collectAsStateWithLifecycle()
                val moodsByDay by reviewViewModel.moodsByDay.collectAsStateWithLifecycle()
                val moodInsights = remember(history, moodsByDay) {
                    app.tsuzuku.model.moodInsights(history, moodsByDay, java.time.LocalDate.now())
                }
                Column(Modifier.fillMaxSize()) {
                    PrimaryTabRow(selectedTabIndex = progressPage) {
                        listOf(R.string.statistics, R.string.calendar).forEachIndexed { index, label ->
                            Tab(
                                selected = progressPage == index,
                                onClick = { progressPage = index },
                                text = { Text(stringResource(label)) },
                            )
                        }
                    }
                    androidx.compose.animation.Crossfade(targetState = progressPage, label = "progress-page") { page ->
                        if (page == 0) {
                            StatisticsScreen(
                                history = history,
                                libraryItems = emptyList(),
                                moods = moodsByDay,
                                onOpenYearReview = { navController.navigate("year-review") },
                                weekStartsMonday = settings.weekStart == WeekStartPreference.Monday,
                                onShareWeek = { w ->
                                    app.tsuzuku.share.shareWeekCard(
                                        context,
                                        app.tsuzuku.share.WeekCardData(w.rate, w.completed, w.scheduled, w.perfectDays, w.dayFractions, w.bestDay),
                                    )
                                },
                                heatmapDays = heatmapDays,
                                selectedHeatmapHabitId = selectedHeatmapHabitId,
                                onSelectHeatmapHabitId = reviewViewModel::setHeatmapHabitId,
                                weeklySummary = weeklySummary,
                                onPreviousWeek = reviewViewModel::previousWeek,
                                onNextWeek = reviewViewModel::nextWeek,
                                onSaveWeeklyNote = reviewViewModel::saveWeeklyNote,
                                monthlySummary = monthlySummary,
                                onPreviousMonth = reviewViewModel::previousMonth,
                                onNextMonth = reviewViewModel::nextMonth,
                                onSaveMonthlyNote = { ym, note -> reviewViewModel.savePeriodNote(ReviewPeriodType.Monthly, ym.toString(), note) },
                                yearlySummary = yearlySummary,
                                onPreviousYear = reviewViewModel::previousYear,
                                onNextYear = reviewViewModel::nextYear,
                                onSaveYearlyNote = { y, note -> reviewViewModel.savePeriodNote(ReviewPeriodType.Yearly, y.toString(), note) },
                                personalRecords = personalRecords,
                                skipReasonStats = skipReasonStats,
                                moodInsights = moodInsights,
                                initialTab = statisticsTab,
                            )
                        } else {
                            CalendarScreen(
                                history = history,
                                weekStartsMonday = settings.weekStart == WeekStartPreference.Monday,
                                onToggle = todayViewModel::toggleHabitOnDate,
                                onIncrement = todayViewModel::incrementHabitOnDate,
                                onDecrement = todayViewModel::decrementHabitOnDate,
                            )
                        }
                    }
                }
            }
            screen("settings") {
                val cloudAccount by cloudSyncRepository.account.collectAsStateWithLifecycle()
                ProfileScreen(
                    settings = settings,
                    cloudAccount = cloudAccount,
                    onSetProfileName = onSetProfileName,
                    onSetTheme = onSetTheme,
                    onSetPalette = onSetPalette,
                    onSetPureBlack = onSetPureBlack,
                    onSetWidgetOpacity = onSetWidgetOpacity,
                    onSetLanguage = onSetLanguage,
                    onSetWeekStart = onSetWeekStart,
                    onSetShowDailySummary = onSetShowDailySummary,
                    onSetProtectScreen = onSetProtectScreen,
                    onOpenCloudAccount = { navController.navigate("cloud-account") },
                    onOpenAppearance = { navController.navigate("appearance") },
                    onOpenLanguage = { navController.navigate("language") },
                    onOpenWidgets = { navController.navigate("widgets") },
                    onOpenArchive = { navController.navigate("archive") },
                    onOpenPlanning = { navController.navigate("planning") },
                    onOpenSecurity = { navController.navigate("security") },
                    onOpenBackup = { navController.navigate("backup") },
                    onOpenDiagnostics = { navController.navigate("diagnostics") },
                    onOpenCategories = { navController.navigate("categories") },
                    onOpenRecords = {
                        statisticsTab = StatisticsTab.Records
                        progressPage = 0
                        navController.navigate("progress") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            screen("goals") {
                GoalsScreen(
                    goals = goals,
                    onBack = navController::popBackStack,
                    onAdd = { navController.navigate("goal/new") },
                    onOpen = { id -> navController.navigate("goal/$id") },
                    onIncrement = { goalViewModel.changeProgress(it, 1.0) },
                    onDecrement = { goalViewModel.changeProgress(it, -1.0) },
                )
            }
            screen("search") {
                UnifiedSearchScreen(
                    habits = history.habits,
                    tasks = planTasks,
                    contents = emptyList(),
                    onBack = navController::popBackStack,
                    onOpenHabit = { navController.navigate("habit/$it") },
                    onOpenTask = { navController.navigate("plan/$it") },
                    onOpenContent = {},
                )
            }
            screen("goal/new") {
                GoalEditorScreen(
                    onBack = navController::popBackStack,
                    onSave = { draft ->
                        goalViewModel.saveGoal(null, draft) { id ->
                            navController.navigate("goal/$id") { popUpTo("goals") }
                        }
                    },
                )
            }
            screen(
                route = "goal/{goalId}",
                arguments = listOf(navArgument("goalId") { type = NavType.LongType }),
            ) { entry ->
                val goalId = entry.arguments?.getLong("goalId") ?: return@screen
                val goal = goals.firstOrNull { it.id == goalId }
                if (goal == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.goal_details)) }
                } else {
                    val linkedH = habitGoalLinks.filter { it.goalId == goalId }.mapNotNull { link ->
                        history.habits.firstOrNull { it.id == link.habitId }?.let { habit -> Pair(habit, link) }
                    }
                    val linkedT = taskGoalLinks.filter { it.goalId == goalId }.mapNotNull { link ->
                        planTasks.firstOrNull { it.id == link.taskId }?.let { task -> Pair(task, link) }
                    }
                    GoalDetailScreen(
                        goal = goal,
                        onBack = navController::popBackStack,
                        onEdit = { navController.navigate("goal/$goalId/edit") },
                        onIncrement = { goalViewModel.changeProgress(goalId, 1.0) },
                        onDecrement = { goalViewModel.changeProgress(goalId, -1.0) },
                        onSetCompleted = { goalViewModel.setCompleted(goalId, it) },
                        onArchive = { goalViewModel.archiveGoal(goalId) { navController.popBackStack("goals", false) } },
                        onDelete = { goalViewModel.deleteGoal(goalId) { navController.popBackStack("goals", false) } },
                        linkedHabits = linkedH,
                        linkedTasks = linkedT,
                        availableHabits = history.habits,
                        availableTasks = planTasks,
                        onLinkHabit = { habitId, mode, value -> connectionViewModel.linkHabitGoal(habitId, goalId, mode, value) },
                        onUnlinkHabit = { habitId -> connectionViewModel.unlinkHabitGoal(habitId, goalId) },
                        onLinkTask = { taskId, value -> connectionViewModel.linkTaskGoal(taskId, goalId, value) },
                        onUnlinkTask = { taskId -> connectionViewModel.unlinkTaskGoal(taskId, goalId) },
                    )
                }
            }
            screen(
                route = "goal/{goalId}/edit",
                arguments = listOf(navArgument("goalId") { type = NavType.LongType }),
            ) { entry ->
                val goalId = entry.arguments?.getLong("goalId") ?: return@screen
                goals.firstOrNull { it.id == goalId }?.let { goal ->
                    GoalEditorScreen(
                        existing = goal,
                        onBack = navController::popBackStack,
                        onSave = { draft -> goalViewModel.saveGoal(goalId, draft) { navController.popBackStack() } },
                    )
                }
            }
            screen("security") {
                SecurityScreen(
                    settings = settings,
                    biometricAvailable = biometricAvailable,
                    onBack = navController::popBackStack,
                    onSetPin = onSetPin,
                    onChangePin = onChangePin,
                    onDisablePin = onDisablePin,
                    onSetBiometric = onSetBiometric,
                    onSetLockDelayMinutes = onSetLockDelayMinutes,
                )
            }
            screen("backup") {
                BackupScreen(
                    repository = backupRepository,
                    onBack = navController::popBackStack,
                    onRestored = {
                        reminderScheduler.rescheduleAll()
                        TsuzukuWidgetProvider.requestUpdate(context.applicationContext)
                    },
                )
            }
            screen("diagnostics") {
                DiagnosticsScreen(store = diagnosticStore, onBack = navController::popBackStack)
            }
            screen("categories") {
                CategoryManagerScreen(
                    categories = categories,
                    onBack = navController::popBackStack,
                    onCreate = categoryViewModel::create,
                    onDelete = categoryViewModel::delete,
                )
            }
            screen("year-review") {
                val moodsForYear by reviewViewModel.moodsByDay.collectAsStateWithLifecycle()
                val data = remember(history, moodsForYear) { app.tsuzuku.ui.screens.yearReview(history, moodsForYear, java.time.LocalDate.now()) }
                app.tsuzuku.ui.screens.YearReviewScreen(
                    data = data,
                    onClose = navController::popBackStack,
                    onShare = { app.tsuzuku.share.shareYearCard(context, data) },
                )
            }
            screen("archive") {
                val archived by repository.observeArchivedHabits().collectAsStateWithLifecycle(initialValue = emptyList())
                val archiveScope = androidx.compose.runtime.rememberCoroutineScope()
                app.tsuzuku.ui.screens.ArchiveScreen(
                    habits = archived,
                    onBack = navController::popBackStack,
                    onRestore = { id ->
                        archiveScope.launch {
                            repository.unarchiveHabit(id)
                            reminderScheduler.schedule(id)
                            TsuzukuWidgetProvider.requestUpdate(context.applicationContext)
                        }
                    },
                    onDelete = { id ->
                        reminderScheduler.cancel(id)
                        archiveScope.launch { repository.deleteHabit(id) }
                    },
                )
            }
            screen("widgets") {
                app.tsuzuku.ui.screens.WidgetsScreen(onBack = navController::popBackStack)
            }
            screen("language") {
                app.tsuzuku.ui.screens.LanguageScreen(
                    selected = settings.language,
                    onBack = navController::popBackStack,
                    onSelect = onSetLanguage,
                )
            }
            screen("appearance") {
                app.tsuzuku.ui.screens.AppearanceScreen(
                    theme = settings.theme,
                    palette = settings.palette,
                    pureBlack = settings.pureBlack,
                    systemDark = androidx.compose.foundation.isSystemInDarkTheme(),
                    onBack = navController::popBackStack,
                    onSetTheme = onSetTheme,
                    onSetPalette = onSetPalette,
                    onSetPureBlack = onSetPureBlack,
                )
            }
            screen("cloud-account") {
                CloudAccountScreen(
                    repository = cloudSyncRepository,
                    settings = settings,
                    onBack = navController::popBackStack,
                    onSetCloudSyncEnabled = onSetCloudSyncEnabled,
                    onDataRestored = {
                        reminderScheduler.rescheduleAll()
                        TsuzukuWidgetProvider.requestUpdate(context.applicationContext)
                    },
                    onSignedInName = onSetProfileName,
                )
            }
            screen("planning") {
                val weeklyReviews by repository.observeWeeklyReviews().collectAsStateWithLifecycle(initialValue = emptyList())
                PlanningScreen(
                    history = history,
                    reviews = weeklyReviews,
                    settings = settings,
                    onBack = navController::popBackStack,
                    onApplyVacation = { start, end ->
                        repository.applyVacation(start, end)
                        onSetVacation(start.toEpochDay(), end.toEpochDay())
                    },
                    onClearVacation = { start, end ->
                        repository.clearVacation(start, end)
                        onClearVacation()
                    },
                    onSaveReview = repository::saveWeeklyReview,
                )
            }
            screen("habit/new") {
                val template = remember { NewHabitTemplate.take() }
                HabitEditorScreen(
                    template = template,
                    onSaveMany = { drafts ->
                        // A whole pack: add each, schedule its reminder, then leave once.
                        drafts.forEachIndexed { index, draft ->
                            todayViewModel.addHabit(draft) { habitId ->
                                if (draft.reminderMinutes != null) {
                                    reminderScheduler.schedule(habitId, draft.scheduleDaysMask, draft.reminderMinutes)
                                }
                                if (index == drafts.lastIndex) navController.popBackStack()
                            }
                        }
                    },
                    availableHabits = history.habits,
                    customCategories = categories,
                    onBack = navController::popBackStack,
                    onSave = { draft ->
                        todayViewModel.addHabit(draft) { habitId ->
                            app.tsuzuku.notifications.HabitAlarms.set(context, habitId, draft.alarm, app.tsuzuku.notifications.AlarmMission.valueOf(draft.alarmMission), draft.alarmMarkDone)
                            applySpread(context, habitId, draft)
                            if (draft.reminderMinutes != null) {
                                reminderScheduler.schedule(habitId, draft.scheduleDaysMask, draft.reminderMinutes)
                            }
                            navController.popBackStack()
                        }
                    },
                )
            }
            screen("habit/order") {
                HabitOrderScreen(
                    habits = history.habits.sortedBy { it.sortOrder },
                    onBack = navController::popBackStack,
                    onMove = todayViewModel::moveHabit,
                )
            }
            screen(
                route = "habit/{habitId}",
                arguments = listOf(navArgument("habitId") { type = NavType.LongType }),
            ) { entry ->
                val habitId = entry.arguments?.getLong("habitId") ?: return@screen
                val habit = history.habits.firstOrNull { it.id == habitId }
                if (habit == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.loading_habit))
                    }
                } else {
                    val linkedG = habitGoalLinks.filter { it.habitId == habitId }.mapNotNull { link ->
                        goals.firstOrNull { it.id == link.goalId }?.let { goal -> Pair(goal, link) }
                    }
                    val habitLevels by todayViewModel.observeHabitLevels(habitId).collectAsStateWithLifecycle(initialValue = emptyList())
                    HabitDetailScreen(
                        habit = habit,
                        records = history.records.filter { it.habitId == habitId },
                        feelingSummary = history.feelingSummary(habitId, java.time.LocalDate.now().minusDays(89)),
                        onBack = navController::popBackStack,
                        onEdit = { navController.navigate("habit/$habitId/edit") },
                        onShare = {
                            val weekStart = if (settings.weekStart == WeekStartPreference.Sunday) java.time.DayOfWeek.SUNDAY else java.time.DayOfWeek.MONDAY
                            app.tsuzuku.share.shareStreakCard(
                                context,
                                app.tsuzuku.model.streakCardData(history, habit, java.time.LocalDate.now(), weekStart),
                            )
                        },
                        onSaveNote = { todayViewModel.saveHabitNote(habitId, it) },
                        onArchive = {
                            reminderScheduler.cancel(habitId)
                            todayViewModel.archiveHabit(habitId) { navController.popBackStack("today", inclusive = false) }
                        },
                        onDuplicate = {
                            todayViewModel.duplicateHabit(habitId) { newId ->
                                if (newId != null && habit.reminderMinutes != null) {
                                    reminderScheduler.schedule(newId, habit.scheduleDaysMask, habit.reminderMinutes)
                                }
                                navController.popBackStack("today", inclusive = false)
                            }
                        },
                        onDelete = {
                            reminderScheduler.cancel(habitId)
                            todayViewModel.deleteHabit(habitId) { navController.popBackStack("today", inclusive = false) }
                        },
                        linkedGoals = linkedG,
                        linkedContents = emptyList(),
                        availableGoals = goals.filter { it.status == GoalStatus.Active },
                        availableContents = emptyList(),
                        habitLevels = habitLevels,
                        onLinkGoal = { goalId, mode, value -> connectionViewModel.linkHabitGoal(habitId, goalId, mode, value) },
                        onUnlinkGoal = { goalId -> connectionViewModel.unlinkHabitGoal(habitId, goalId) },
                        onLinkContent = { _, _ -> },
                        onUnlinkContent = {},
                        onSaveLevels = { levels -> todayViewModel.saveHabitLevels(habitId, levels) },
                        onAdvanceLevel = { levelNum -> todayViewModel.advanceHabitLevel(habitId, levelNum) },
                    )
                }
            }
            screen(
                route = "habit/{habitId}/edit",
                arguments = listOf(navArgument("habitId") { type = NavType.LongType }),
            ) { entry ->
                val habitId = entry.arguments?.getLong("habitId") ?: return@screen
                val habit = history.habits.firstOrNull { it.id == habitId }
                if (habit == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.loading_habit))
                    }
                } else {
                    HabitEditorScreen(
                        existing = habit,
                        availableHabits = history.habits,
                        customCategories = categories,
                        onBack = navController::popBackStack,
                        onSave = { draft ->
                            todayViewModel.updateHabit(habitId, draft) {
                                app.tsuzuku.notifications.HabitAlarms.set(context, habitId, draft.alarm, app.tsuzuku.notifications.AlarmMission.valueOf(draft.alarmMission), draft.alarmMarkDone)
                                applySpread(context, habitId, draft)
                                if (draft.reminderMinutes == null) {
                                    reminderScheduler.cancel(habitId)
                                } else {
                                    reminderScheduler.schedule(habitId, draft.scheduleDaysMask, draft.reminderMinutes)
                                }
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }
            screen("focus") {
                val focusSessions by focusViewModel.sessions.collectAsStateWithLifecycle()
                val focusState by focusViewModel.activeState.collectAsStateWithLifecycle()
                FocusScreen(
                    activeState = focusState,
                    sessions = focusSessions,
                    habits = history.habits,
                    goals = goals,
                    tasks = planTasks,
                    onStart = focusViewModel::startSession,
                    onPause = focusViewModel::pauseSession,
                    onResume = focusViewModel::resumeSession,
                    onFinish = {
                        val s = focusState
                        if (s != null && s.targetType == FocusTargetType.Task && s.targetId != null) {
                            val minutes = (s.elapsedSeconds + 59) / 60
                            if (minutes > 0) planViewModel.addActualMinutes(s.targetId, minutes)
                        }
                        focusViewModel.finishSession()
                    },
                    onCancel = focusViewModel::cancelSession,
                    onBack = navController::popBackStack,
                )
            }
            screen(
                route = "routine/{routineName}",
                arguments = listOf(navArgument("routineName") { type = NavType.StringType }),
            ) { entry ->
                val routineName = entry.arguments?.getString("routineName") ?: return@screen
                RoutinePlayerScreen(
                    routineName = routineName,
                    routineHabits = habits.filter { it.habit.routineName.equals(routineName, ignoreCase = true) },
                    onToggleHabit = { habitId ->
                        todayViewModel.toggleHabit(habitId)
                        connectionViewModel.onHabitProgressApplied(habitId, 1.0)
                    },
                    onIncrementHabit = { habitId ->
                        todayViewModel.incrementHabit(habitId)
                        connectionViewModel.onHabitProgressApplied(habitId, 1.0)
                    },
                    onBack = navController::popBackStack,
                )
            }
        }
        }
    }

    if (showDailyReviewSheet) {
        val dailyReviewToday by reviewViewModel.dailyReviewToday.collectAsStateWithLifecycle()
        val todayEpoch = java.time.LocalDate.now().toEpochDay()
        val todaysTasks = planTasks.filter { it.scheduledEpochDay == null || it.scheduledEpochDay == todayEpoch }
        DailyReviewSheet(
            existingReview = dailyReviewToday,
            incompleteTodayTasks = todaysTasks.filter { !it.completed },
            onCarryTaskToTomorrow = { task -> planViewModel.move(task.id, todayEpoch + 1) },
            onSave = { mood, energy, productivity, stress, note ->
                reviewViewModel.saveDailyReview(
                    mood = mood,
                    energy = energy,
                    productivity = productivity,
                    stress = stress,
                    note = note,
                    epochDay = todayEpoch,
                )
                showDailyReviewSheet = false
            },
            onDismiss = { showDailyReviewSheet = false },
        )
    }
}

@Composable
internal fun MainNavigationBar(
    destinations: List<MainDestination>,
    currentRoute: String,
    onSelect: (MainDestination) -> Unit,
) {
    androidx.compose.material3.ShortNavigationBar {
        destinations.forEach { destination ->
            androidx.compose.material3.ShortNavigationBarItem(
                selected = destination.route == currentRoute,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainTopBar(
    title: String,
    onSearch: (() -> Unit)? = null,
    onCalendar: (() -> Unit)? = null,
) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            if (onCalendar != null) {
                IconButton(onClick = onCalendar) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = stringResource(R.string.edit_past_days))
                }
            }
            if (onSearch != null) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search_everything))
                }
            }
        },
    )
}

private val mainRoutes = mainDestinations.map { it.route }.toSet()

private fun isTabSwitch(from: String?, to: String?): Boolean = from in mainRoutes && to in mainRoutes

private fun fadeThroughIn(): androidx.compose.animation.EnterTransition =
    fadeIn(androidx.compose.animation.core.tween(160, delayMillis = 40)) +
        androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(160, delayMillis = 40), initialScale = 0.97f)

private fun fadeThroughOut(): androidx.compose.animation.ExitTransition =
    fadeOut(androidx.compose.animation.core.tween(60))

private fun sharedAxisIn(forward: Boolean): androidx.compose.animation.EnterTransition =
    androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(220)) { width -> if (forward) width / 10 else -width / 10 } +
        fadeIn(androidx.compose.animation.core.tween(160, delayMillis = 40))

private fun sharedAxisOut(forward: Boolean): androidx.compose.animation.ExitTransition =
    androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(220)) { width -> if (forward) -width / 10 else width / 10 } +
        fadeOut(androidx.compose.animation.core.tween(60))


/** The scaffold's live padding (top bar, bottom bar, insets) and the route on screen. */
private val LocalScaffoldPadding = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.foundation.layout.PaddingValues> { androidx.compose.foundation.layout.PaddingValues() }
private val LocalCurrentRoute = androidx.compose.runtime.compositionLocalOf<String?> { null }

/**
 * A destination that applies the scaffold padding itself. While it is leaving (another route is
 * current) it keeps the padding it had, so it doesn't jump when the top and bottom bars appear or
 * disappear for the incoming screen; that jump read as a flash of the old screen.
 */
private fun androidx.navigation.NavGraphBuilder.screen(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    content: @Composable androidx.compose.animation.AnimatedContentScope.(androidx.navigation.NavBackStackEntry) -> Unit,
) = composable(route = route, arguments = arguments) { entry ->
    val live = LocalScaffoldPadding.current
    val current = LocalCurrentRoute.current == entry.destination.route
    var kept by remember { mutableStateOf(live) }
    if (current) androidx.compose.runtime.SideEffect { kept = live }
    Box(Modifier.fillMaxSize().padding(if (current) live else kept)) { content(entry) }
}

/** Saves a habit's spread-reminder choice and sets its next reminder. */
private fun applySpread(context: android.content.Context, habitId: Long, draft: app.tsuzuku.model.HabitDraft) {
    app.tsuzuku.health.ScreenTime.set(context, habitId, draft.screenTime)
    val window = draft.spreadStartMinutes?.let { app.tsuzuku.notifications.SpreadWindow(it, draft.spreadEndMinutes) }
    app.tsuzuku.notifications.SpreadReminders.set(context, habitId, window)
    if (window != null) {
        val appContext = context.applicationContext
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { app.tsuzuku.notifications.SpreadReminders.schedule(appContext, habitId) }
    }
}
