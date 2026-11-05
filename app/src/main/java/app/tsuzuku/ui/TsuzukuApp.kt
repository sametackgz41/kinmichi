package app.tsuzuku.ui

import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import app.tsuzuku.R
import app.tsuzuku.data.HabitRepository
import app.tsuzuku.data.LibraryRepository
import app.tsuzuku.data.ReviewRepository
import app.tsuzuku.data.GoalRepository
import app.tsuzuku.data.PlanRepository
import app.tsuzuku.data.CategoryRepository
import app.tsuzuku.data.BackupRepository
import app.tsuzuku.data.CloudAccountState
import app.tsuzuku.data.CloudSyncRepository
import app.tsuzuku.data.DiagnosticStore
import app.tsuzuku.data.HealthConnectRepository
import app.tsuzuku.data.preferences.AppSettings
import app.tsuzuku.data.preferences.LanguagePreference
import app.tsuzuku.data.preferences.ThemePreference
import app.tsuzuku.data.preferences.WeekStartPreference
import app.tsuzuku.data.preferences.TimerSessionStore
import app.tsuzuku.model.DailyReview
import app.tsuzuku.model.ReviewPeriodType
import app.tsuzuku.notifications.HabitReminderScheduler
import app.tsuzuku.ui.screens.CalendarScreen
import app.tsuzuku.ui.screens.HabitEditorScreen
import app.tsuzuku.ui.screens.HabitDetailScreen
import app.tsuzuku.ui.screens.HabitOrderScreen
import app.tsuzuku.ui.screens.LibraryScreen
import app.tsuzuku.ui.screens.LibraryOrderScreen
import app.tsuzuku.ui.screens.ContentDetailScreen
import app.tsuzuku.ui.screens.ContentEditorScreen
import app.tsuzuku.ui.screens.ProfileScreen
import app.tsuzuku.ui.screens.StatisticsScreen
import app.tsuzuku.ui.screens.TodayScreen
import app.tsuzuku.ui.screens.DailyReviewSheet
import app.tsuzuku.ui.screens.GoalsScreen
import app.tsuzuku.ui.screens.GoalDetailScreen
import app.tsuzuku.ui.screens.GoalEditorScreen
import app.tsuzuku.ui.screens.SecurityScreen
import app.tsuzuku.ui.screens.BackupScreen
import app.tsuzuku.ui.screens.DiagnosticsScreen
import app.tsuzuku.ui.screens.CloudAccountScreen
import app.tsuzuku.ui.screens.PlanningScreen
import app.tsuzuku.ui.screens.PlanTabScreen
import app.tsuzuku.ui.screens.PlanTaskDetailScreen
import app.tsuzuku.ui.screens.PlanTaskEditorScreen
import app.tsuzuku.data.ConnectionRepository
import app.tsuzuku.data.FocusRepository
import app.tsuzuku.data.InboxRepository
import app.tsuzuku.data.HealthConnectState
import app.tsuzuku.model.PlanTaskDraft
import app.tsuzuku.model.FocusMode
import app.tsuzuku.model.FocusTargetType
import app.tsuzuku.model.GoalDraft
import app.tsuzuku.model.GoalStatus
import app.tsuzuku.model.HabitDraft
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.HabitGoalContributionMode
import app.tsuzuku.model.ContentDraft
import app.tsuzuku.model.ContentCategory
import app.tsuzuku.model.ContentStatus
import app.tsuzuku.ui.FocusViewModel
import app.tsuzuku.ui.InboxViewModel
import app.tsuzuku.ui.ConnectionViewModel
import app.tsuzuku.ui.ReviewViewModel
import app.tsuzuku.ui.screens.FocusScreen
import app.tsuzuku.ui.screens.RoutinePlayerScreen
import app.tsuzuku.ui.screens.InboxSheet
import app.tsuzuku.ui.screens.UniversalQuickAddSheet
import app.tsuzuku.ui.screens.UnifiedSearchScreen
import app.tsuzuku.ui.screens.CategoryManagerScreen
import app.tsuzuku.data.ProjectRepository
import app.tsuzuku.data.intelligence.IntelligenceRepository
import app.tsuzuku.model.ProjectDraft
import app.tsuzuku.model.ProjectStatus
import app.tsuzuku.ui.ProjectViewModel
import app.tsuzuku.ui.IntelligenceViewModel
import app.tsuzuku.ui.screens.ProjectsScreen
import app.tsuzuku.ui.screens.ProjectDetailScreen
import app.tsuzuku.ui.screens.ProjectEditorSheet
import app.tsuzuku.ui.screens.IntelligenceScreen
import app.tsuzuku.ui.screens.AskTsuzukuSheet
import app.tsuzuku.ui.screens.PlanAssistantSheet
import app.tsuzuku.ui.screens.AiPrivacyDialog
import app.tsuzuku.widget.TsuzukuWidgetProvider

private data class MainDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val mainDestinations = listOf(
    MainDestination("today", R.string.today, Icons.Rounded.Home),
    MainDestination("plan", R.string.plan, Icons.Rounded.Checklist),
    MainDestination("library", R.string.library, Icons.AutoMirrored.Rounded.MenuBook),
    MainDestination("calendar", R.string.calendar, Icons.Rounded.CalendarMonth),
    MainDestination("statistics", R.string.statistics, Icons.Rounded.BarChart),
    MainDestination("profile", R.string.profile, Icons.Rounded.Person),
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
    healthConnectRepository: HealthConnectRepository,
    diagnosticStore: DiagnosticStore,
    reminderScheduler: HabitReminderScheduler,
    timerSessionStore: TimerSessionStore,
    settings: AppSettings,
    onSetProfileName: (String) -> Unit,
    onSetTheme: (ThemePreference) -> Unit,
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
    inboxRepository: InboxRepository,
    connectionRepository: ConnectionRepository,
    projectRepository: ProjectRepository,
    intelligenceRepository: IntelligenceRepository,
    onSetTodaySectionOrder: (String) -> Unit = {},
    onSetTodayHiddenSections: (String) -> Unit = {},
    onSetHealthSyncEnabled: (Boolean) -> Unit = {},
    onSetOfflineIntelligenceEnabled: (Boolean) -> Unit = {},
    onSetCloudAiEnabled: (Boolean) -> Unit = {},
    onSetGeminiApiKey: (String) -> Unit = {},
    onSetAiShareHabitTitles: (Boolean) -> Unit = {},
    onSetAiShareTaskTitles: (Boolean) -> Unit = {},
    onSetAiShareNotes: (Boolean) -> Unit = {},
    onSetCloudAiModel: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val todayViewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.factory(repository, timerSessionStore) {
            TsuzukuWidgetProvider.requestUpdate(context.applicationContext)
        },
    )
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(libraryRepository))
    val goalViewModel: GoalViewModel = viewModel(factory = GoalViewModel.factory(goalRepository))
    val planViewModel: PlanViewModel = viewModel(factory = PlanViewModel.factory(planRepository))
    val categoryViewModel: CategoryViewModel = viewModel(factory = CategoryViewModel.factory(categoryRepository))
    val projectViewModel: ProjectViewModel = viewModel(
        factory = ProjectViewModel.factory(projectRepository),
    )
    val intelligenceViewModel: IntelligenceViewModel = viewModel(
        factory = IntelligenceViewModel.factory(intelligenceRepository),
    )
    val focusViewModel: FocusViewModel = viewModel(
        factory = FocusViewModel.factory(focusRepository, planRepository, repository, goalRepository),
    )
    val inboxViewModel: InboxViewModel = viewModel(
        factory = InboxViewModel.factory(inboxRepository),
    )
    val connectionViewModel: ConnectionViewModel = viewModel(
        factory = ConnectionViewModel.factory(connectionRepository, goalRepository, libraryRepository),
    )
    val reviewViewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.factory(reviewRepository))
    val projects by projectViewModel.projects.collectAsStateWithLifecycle()
    val todayInsights by intelligenceViewModel.todayInsights.collectAsStateWithLifecycle()
    val allInsights by intelligenceViewModel.insights.collectAsStateWithLifecycle()
    val recommendations by intelligenceViewModel.recommendations.collectAsStateWithLifecycle()
    val dailyReviewToday by reviewViewModel.dailyReviewToday.collectAsStateWithLifecycle()
    val weeklySummary by reviewViewModel.weeklySummary.collectAsStateWithLifecycle()
    val monthlySummary by reviewViewModel.monthlySummary.collectAsStateWithLifecycle()
    val yearlySummary by reviewViewModel.yearlySummary.collectAsStateWithLifecycle()
    val personalRecords by reviewViewModel.personalRecords.collectAsStateWithLifecycle()
    val heatmapDays by reviewViewModel.heatmapDays.collectAsStateWithLifecycle()
    val selectedHeatmapHabitId by reviewViewModel.selectedHeatmapHabitId.collectAsStateWithLifecycle()
    val skipReasonStats by reviewViewModel.skipReasonStats.collectAsStateWithLifecycle()
    var showDailyReviewSheet by remember { mutableStateOf(false) }
    val habits by todayViewModel.habits.collectAsStateWithLifecycle()
    val history by todayViewModel.history.collectAsStateWithLifecycle()
    val timer by todayViewModel.timer.collectAsStateWithLifecycle()
    val libraryItems by libraryViewModel.items.collectAsStateWithLifecycle()
    val goals by goalViewModel.goals.collectAsStateWithLifecycle()
    val planTasks by planViewModel.tasks.collectAsStateWithLifecycle()
    val planTemplates by planViewModel.templates.collectAsStateWithLifecycle()
    val categories by categoryViewModel.categories.collectAsStateWithLifecycle()
    val cloudAccount by cloudSyncRepository.account.collectAsStateWithLifecycle()
    val weeklyReviews by repository.observeWeeklyReviews().collectAsStateWithLifecycle(initialValue = emptyList())
    val habitGoalLinks by connectionViewModel.habitGoalLinks.collectAsStateWithLifecycle()
    val taskGoalLinks by connectionViewModel.taskGoalLinks.collectAsStateWithLifecycle()
    val habitLibraryLinks by connectionViewModel.habitLibraryLinks.collectAsStateWithLifecycle()
    val focusSessions by focusViewModel.sessions.collectAsStateWithLifecycle()
    val focusState by focusViewModel.activeState.collectAsStateWithLifecycle()
    var showQuickAddSheet by remember { mutableStateOf(false) }
    var showInboxSheet by remember { mutableStateOf(false) }
    var showAskTsuzukuSheet by remember { mutableStateOf(false) }
    var showPlanAssistantSheet by remember { mutableStateOf(false) }
    var showAiPrivacyDialog by remember { mutableStateOf(false) }
    val chatMessages by intelligenceViewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiGenerating by intelligenceViewModel.isGenerating.collectAsStateWithLifecycle()
    val activePlanProposal by intelligenceViewModel.activePlanProposal.collectAsStateWithLifecycle()
    val weeklyCommentary by intelligenceViewModel.weeklyCommentary.collectAsStateWithLifecycle()
    var statisticsTab by remember { mutableStateOf(app.tsuzuku.ui.screens.StatisticsTab.General) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "today"

    Scaffold(
        floatingActionButton = {
            when (currentRoute) {
                "today" -> FloatingActionButton(
                    onClick = { showQuickAddSheet = true },
                    shape = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.secondary,
                ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.quick_add)) }
                "plan" -> FloatingActionButton(
                    onClick = { navController.navigate("plan/new") },
                    shape = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.secondary,
                ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_task)) }
                "library" -> FloatingActionButton(
                    onClick = { navController.navigate("library/new") },
                    shape = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.secondary,
                ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_content)) }
                else -> {}
            }
        },
        bottomBar = {
            if (currentRoute in mainDestinations.map { it.route }) {
                EditorialNavigationBar(
                    destinations = mainDestinations,
                    currentRoute = currentRoute,
                    onSelect = { destination ->
                        if (destination.route == "statistics") {
                            statisticsTab = app.tsuzuku.ui.screens.StatisticsTab.General
                        }
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
        NavHost(
            navController = navController,
            startDestination = "today",
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable("today") {
                val todayEpoch = remember { java.time.LocalDate.now().toEpochDay() }
                val todayStartMillis = remember {
                    java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
                val todayFocusMinutes = remember(focusSessions) {
                    focusSessions.filter { it.startedAt >= todayStartMillis }.sumOf { it.durationMinutes }
                }

                TodayScreen(
                    habits = habits,
                    vacationActive = settings.vacationStartEpochDay?.let { start ->
                        settings.vacationEndEpochDay?.let { end -> java.time.LocalDate.now().toEpochDay() in start..end }
                    } == true,
                    profileName = settings.profileName,
                    showDailySummary = settings.showDailySummary,
                    timer = timer,
                    onAddHabit = { showQuickAddSheet = true },
                    onToggleHabit = { habitId ->
                        val h = habits.firstOrNull { it.habit.id == habitId }
                        todayViewModel.toggleHabit(habitId)
                        if (h != null && !h.isComplete) {
                            connectionViewModel.onHabitProgressApplied(habitId, 1.0)
                        }
                    },
                    onIncrementHabit = { habitId ->
                        todayViewModel.incrementHabit(habitId)
                        connectionViewModel.onHabitProgressApplied(habitId, 1.0)
                    },
                    onDecrementHabit = todayViewModel::decrementHabit,
                    onToggleTimer = todayViewModel::toggleTimer,
                    onOpenHabit = { id -> navController.navigate("habit/$id") },
                    onReorderHabits = { navController.navigate("habit/order") },
                    onOpenGoals = { navController.navigate("goals") },
                    onSearch = { navController.navigate("search") },
                    planTasks = planTasks.filter { it.scheduledEpochDay == null || it.scheduledEpochDay == todayEpoch },
                    goals = goals,
                    libraryItems = libraryItems,
                    focusMinutesToday = todayFocusMinutes,
                    todaySectionOrder = settings.todaySectionOrder,
                    todayHiddenSections = settings.todayHiddenSections,
                    healthSteps = 0L,
                    onUpdateSectionConfig = { order, hidden ->
                        onSetTodaySectionOrder(order)
                        onSetTodayHiddenSections(hidden)
                    },
                    onStartFocus = { navController.navigate("focus") },
                    onOpenInbox = { showInboxSheet = true },
                    onStartRoutine = { routineName -> navController.navigate("routine/$routineName") },
                    onTogglePlanTask = { task ->
                        planViewModel.toggle(task)
                        if (!task.completed) {
                            connectionViewModel.onTaskCompletedApplied(task.id)
                        }
                    },
                    onOpenPlanTask = { taskId -> navController.navigate("plan/$taskId") },
                    onOpenContent = { contentId -> navController.navigate("library/$contentId") },
                    onIncrementContent = { contentId, delta -> libraryViewModel.changeProgress(contentId, delta) },
                    dailyReview = dailyReviewToday,
                    onOpenDailyReview = { showDailyReviewSheet = true },
                    onSkipHabit = todayViewModel::skipHabit,
                    onMovePlanTask = { taskId, targetEpoch -> planViewModel.move(taskId, targetEpoch) },
                    onDeletePlanTask = planViewModel::delete,
                    onDividePlanTaskIntoSubtasks = { taskId, steps ->
                        steps.forEach { planViewModel.addChecklistItem(taskId, it) }
                    },
                    insights = if (settings.offlineIntelligenceEnabled) todayInsights else emptyList(),
                    onOpenIntelligence = { navController.navigate("intelligence") },
                    onOpenProjects = { navController.navigate("projects") },
                )
            }
            composable("library") {
                val collections by libraryViewModel.collections.collectAsStateWithLifecycle()
                val selectedCollectionId by libraryViewModel.selectedCollectionId.collectAsStateWithLifecycle()
                val selectedCollectionContentIds by libraryViewModel.selectedCollectionContentIds.collectAsStateWithLifecycle()

                LibraryScreen(
                    items = libraryItems,
                    collections = collections,
                    selectedCollectionId = selectedCollectionId,
                    selectedCollectionContentIds = selectedCollectionContentIds,
                    onSelectCollection = libraryViewModel::selectCollection,
                    onCreateCollection = libraryViewModel::createCollection,
                    onDeleteCollection = libraryViewModel::deleteCollection,
                    onAddContent = { navController.navigate("library/new") },
                    onOpenContent = { id -> navController.navigate("library/$id") },
                    onReorderContent = { navController.navigate("library/order") },
                )
            }
            composable("plan") {
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
                    onOpenProjects = { navController.navigate("projects") },
                    onOpenPlanAssistant = { showPlanAssistantSheet = true },
                )
            }
            composable("plan/new") {
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
            composable(
                route = "plan/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
            ) { entry ->
                val taskId = entry.arguments?.getLong("taskId") ?: return@composable
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
                            if (!task.completed) {
                                connectionViewModel.onTaskCompletedApplied(task.id)
                            }
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
            composable(
                route = "plan/{taskId}/edit",
                arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
            ) { entry ->
                val taskId = entry.arguments?.getLong("taskId") ?: return@composable
                planTasks.firstOrNull { it.id == taskId }?.let { task ->
                    PlanTaskEditorScreen(
                        existing = task,
                        customCategories = categories,
                        onBack = navController::popBackStack,
                        onSave = { draft -> planViewModel.update(taskId, draft); navController.popBackStack() },
                    )
                }
            }
            composable("calendar") {
                CalendarScreen(
                    history = history,
                    weekStartsMonday = settings.weekStart == WeekStartPreference.Monday,
                    onToggle = todayViewModel::toggleHabitOnDate,
                    onIncrement = todayViewModel::incrementHabitOnDate,
                    onDecrement = todayViewModel::decrementHabitOnDate,
                )
            }
            composable("statistics") {
                StatisticsScreen(
                    history = history,
                    libraryItems = libraryItems,
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
                    initialTab = statisticsTab,
                )
            }
            composable("profile") {
                ProfileScreen(
                    settings = settings,
                    cloudAccount = cloudAccount,
                    onSetProfileName = onSetProfileName,
                    onSetTheme = onSetTheme,
                    onSetLanguage = onSetLanguage,
                    onSetWeekStart = onSetWeekStart,
                    onSetShowDailySummary = onSetShowDailySummary,
                    onSetProtectScreen = onSetProtectScreen,
                    onOpenCloudAccount = { navController.navigate("cloud-account") },
                    onOpenPlanning = { navController.navigate("planning") },
                    onOpenSecurity = { navController.navigate("security") },
                    onOpenBackup = { navController.navigate("backup") },
                    onOpenDiagnostics = { navController.navigate("diagnostics") },
                    onOpenCategories = { navController.navigate("categories") },
                    onSetOfflineIntelligenceEnabled = onSetOfflineIntelligenceEnabled,
                    onOpenProjects = { navController.navigate("projects") },
                    onOpenRecords = {
                        statisticsTab = app.tsuzuku.ui.screens.StatisticsTab.Records
                        navController.navigate("statistics")
                    },
                    onOpenAiPrivacy = { showAiPrivacyDialog = true },
                )
            }
            composable("goals") {
                GoalsScreen(
                    goals = goals,
                    onBack = navController::popBackStack,
                    onAdd = { navController.navigate("goal/new") },
                    onOpen = { id -> navController.navigate("goal/$id") },
                    onIncrement = { goalViewModel.changeProgress(it, 1.0) },
                    onDecrement = { goalViewModel.changeProgress(it, -1.0) },
                )
            }
            composable("search") {
                UnifiedSearchScreen(
                    habits = history.habits,
                    tasks = planTasks,
                    contents = libraryItems,
                    onBack = navController::popBackStack,
                    onOpenHabit = { navController.navigate("habit/$it") },
                    onOpenTask = { navController.navigate("plan/$it") },
                    onOpenContent = { navController.navigate("library/$it") },
                )
            }
            composable("goal/new") {
                GoalEditorScreen(
                    onBack = navController::popBackStack,
                    onSave = { draft ->
                        goalViewModel.saveGoal(null, draft) { id ->
                            navController.navigate("goal/$id") { popUpTo("goals") }
                        }
                    },
                )
            }
            composable(
                route = "goal/{goalId}",
                arguments = listOf(navArgument("goalId") { type = NavType.LongType }),
            ) { entry ->
                val goalId = entry.arguments?.getLong("goalId") ?: return@composable
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
            composable(
                route = "goal/{goalId}/edit",
                arguments = listOf(navArgument("goalId") { type = NavType.LongType }),
            ) { entry ->
                val goalId = entry.arguments?.getLong("goalId") ?: return@composable
                goals.firstOrNull { it.id == goalId }?.let { goal ->
                    GoalEditorScreen(
                        existing = goal,
                        onBack = navController::popBackStack,
                        onSave = { draft -> goalViewModel.saveGoal(goalId, draft) { navController.popBackStack() } },
                    )
                }
            }
            composable("security") {
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
            composable("backup") {
                BackupScreen(
                    repository = backupRepository,
                    onBack = navController::popBackStack,
                    onRestored = {
                        reminderScheduler.rescheduleAll()
                        TsuzukuWidgetProvider.requestUpdate(context.applicationContext)
                    },
                )
            }
            composable("diagnostics") {
                DiagnosticsScreen(store = diagnosticStore, onBack = navController::popBackStack)
            }
            composable("categories") {
                CategoryManagerScreen(
                    categories = categories,
                    onBack = navController::popBackStack,
                    onCreate = categoryViewModel::create,
                    onDelete = categoryViewModel::delete,
                )
            }
            composable("cloud-account") {
                CloudAccountScreen(
                    repository = cloudSyncRepository,
                    settings = settings,
                    onBack = navController::popBackStack,
                    onSetCloudSyncEnabled = onSetCloudSyncEnabled,
                    onDataRestored = {
                        reminderScheduler.rescheduleAll()
                        TsuzukuWidgetProvider.requestUpdate(context.applicationContext)
                    },
                )
            }
            composable("planning") {
                PlanningScreen(
                    history = history,
                    reviews = weeklyReviews,
                    settings = settings,
                    healthRepository = healthConnectRepository,
                    weeklyCommentary = weeklyCommentary,
                    onRequestWeeklyCommentary = {
                        intelligenceViewModel.fetchWeeklyCommentary(weeklySummary, settings)
                    },
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
            composable("habit/new") {
                HabitEditorScreen(
                    availableHabits = history.habits,
                    customCategories = categories,
                    onBack = navController::popBackStack,
                    onSave = { draft ->
                        todayViewModel.addHabit(draft) { habitId ->
                            if (draft.reminderMinutes != null) {
                                reminderScheduler.schedule(habitId, draft.scheduleDaysMask, draft.reminderMinutes)
                            }
                            navController.popBackStack()
                        }
                    },
                )
            }
            composable("habit/order") {
                HabitOrderScreen(
                    habits = history.habits.sortedBy { it.sortOrder },
                    onBack = navController::popBackStack,
                    onMove = todayViewModel::moveHabit,
                )
            }
            composable(
                route = "habit/{habitId}",
                arguments = listOf(navArgument("habitId") { type = NavType.LongType }),
            ) { entry ->
                val habitId = entry.arguments?.getLong("habitId") ?: return@composable
                val habit = history.habits.firstOrNull { it.id == habitId }
                if (habit == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.loading_habit))
                    }
                } else {
                    val linkedG = habitGoalLinks.filter { it.habitId == habitId }.mapNotNull { link ->
                        goals.firstOrNull { it.id == link.goalId }?.let { goal -> Pair(goal, link) }
                    }
                    val linkedC = habitLibraryLinks.filter { it.habitId == habitId }.mapNotNull { link ->
                        libraryItems.firstOrNull { it.content.id == link.contentId }?.let { content -> Pair(content, link) }
                    }
                    val habitLevels by todayViewModel.observeHabitLevels(habitId).collectAsStateWithLifecycle(initialValue = emptyList())
                    HabitDetailScreen(
                        habit = habit,
                        records = history.records.filter { it.habitId == habitId },
                        onBack = navController::popBackStack,
                        onEdit = { navController.navigate("habit/$habitId/edit") },
                        onSaveNote = { todayViewModel.saveHabitNote(habitId, it) },
                        onArchive = {
                            reminderScheduler.cancel(habitId)
                            todayViewModel.archiveHabit(habitId) {
                                navController.popBackStack("today", inclusive = false)
                            }
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
                            todayViewModel.deleteHabit(habitId) {
                                navController.popBackStack("today", inclusive = false)
                            }
                        },
                        linkedGoals = linkedG,
                        linkedContents = linkedC,
                        availableGoals = goals.filter { it.status == GoalStatus.Active },
                        availableContents = libraryItems,
                        habitLevels = habitLevels,
                        onLinkGoal = { goalId, mode, value -> connectionViewModel.linkHabitGoal(habitId, goalId, mode, value) },
                        onUnlinkGoal = { goalId -> connectionViewModel.unlinkHabitGoal(habitId, goalId) },
                        onLinkContent = { contentId, mult -> connectionViewModel.linkHabitLibrary(habitId, contentId, mult) },
                        onUnlinkContent = { contentId -> connectionViewModel.unlinkHabitLibrary(habitId, contentId) },
                        onSaveLevels = { levels -> todayViewModel.saveHabitLevels(habitId, levels) },
                        onAdvanceLevel = { levelNum -> todayViewModel.advanceHabitLevel(habitId, levelNum) },
                    )
                }
            }
            composable(
                route = "habit/{habitId}/edit",
                arguments = listOf(navArgument("habitId") { type = NavType.LongType }),
            ) { entry ->
                val habitId = entry.arguments?.getLong("habitId") ?: return@composable
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
            composable("library/new") {
                ContentEditorScreen(
                    customCategories = categories,
                    existingTitles = libraryItems.map { it.content.title },
                    onBack = navController::popBackStack,
                    onSave = { draft ->
                        libraryViewModel.saveContent(null, draft) { id ->
                            navController.navigate("library/$id") {
                                popUpTo("library")
                            }
                        }
                    },
                )
            }
            composable("library/order") {
                LibraryOrderScreen(
                    items = libraryItems,
                    onBack = navController::popBackStack,
                    onMove = libraryViewModel::moveContent,
                )
            }
            composable(
                route = "library/{contentId}",
                arguments = listOf(navArgument("contentId") { type = NavType.LongType }),
            ) { entry ->
                val contentId = entry.arguments?.getLong("contentId") ?: return@composable
                val detailFlow = remember(contentId) { libraryViewModel.detail(contentId) }
                val detail by detailFlow.collectAsStateWithLifecycle(initialValue = null)
                val collections by libraryViewModel.collections.collectAsStateWithLifecycle()
                val contentCollectionIdsFlow = remember(contentId) { libraryRepository.observeCollectionIdsForContent(contentId) }
                val contentCollectionIds by contentCollectionIdsFlow.collectAsStateWithLifecycle(initialValue = emptyList<Long>())

                ContentDetailScreen(
                    detail = detail,
                    availableCollections = collections,
                    contentCollectionIds = contentCollectionIds,
                    onToggleCollection = { colId, isIn ->
                        if (isIn) {
                            libraryViewModel.removeContentFromCollection(colId, contentId)
                        } else {
                            libraryViewModel.addContentToCollection(colId, contentId)
                        }
                    },
                    onBack = navController::popBackStack,
                    onEdit = { id -> navController.navigate("library/$id/edit") },
                    onProgress = libraryViewModel::changeProgress,
                    onStatus = libraryViewModel::updateStatus,
                    onSaveGeneralNote = libraryViewModel::saveGeneralNote,
                    onAddShortNote = libraryViewModel::addShortNote,
                    onDeleteShortNote = libraryViewModel::deleteShortNote,
                    onAddSeason = libraryViewModel::addSeason,
                    onSeasonProgress = libraryViewModel::changeSeasonProgress,
                    onDeleteContent = { id ->
                        libraryViewModel.deleteContent(id) {
                            navController.popBackStack("library", inclusive = false)
                        }
                    },
                    onUpdateReview = libraryViewModel::updateReview,
                )
            }
            composable(
                route = "library/{contentId}/edit",
                arguments = listOf(navArgument("contentId") { type = NavType.LongType }),
            ) { entry ->
                val contentId = entry.arguments?.getLong("contentId") ?: return@composable
                val existing = libraryItems.firstOrNull { it.content.id == contentId }?.content
                if (existing == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.loading_content))
                    }
                } else {
                    ContentEditorScreen(
                        existing = existing,
                        customCategories = categories,
                        existingTitles = libraryItems.map { it.content.title },
                        onBack = navController::popBackStack,
                        onSave = { draft ->
                            libraryViewModel.saveContent(contentId, draft) {
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }
            composable("focus") {
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
                            if (minutes > 0) {
                                planViewModel.addActualMinutes(s.targetId, minutes)
                            }
                        }
                        focusViewModel.finishSession()
                    },
                    onCancel = focusViewModel::cancelSession,
                    onBack = navController::popBackStack,
                )
            }
            composable(
                route = "routine/{routineName}",
                arguments = listOf(navArgument("routineName") { type = NavType.StringType }),
            ) { entry ->
                val routineName = entry.arguments?.getString("routineName") ?: return@composable
                val routineHabits = habits.filter {
                    it.habit.routineName.equals(routineName, ignoreCase = true)
                }
                RoutinePlayerScreen(
                    routineName = routineName,
                    routineHabits = routineHabits,
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
            composable("intelligence") {
                IntelligenceScreen(
                    insights = allInsights,
                    recommendations = recommendations,
                    onBack = navController::popBackStack,
                    onApplyRecommendation = intelligenceViewModel::applyRecommendation,
                    onOpenAskTsuzuku = { showAskTsuzukuSheet = true },
                    onOpenPlanAssistant = { showPlanAssistantSheet = true },
                    onOpenAiPrivacy = { showAiPrivacyDialog = true },
                )
            }
            composable("projects") {
                ProjectsScreen(
                    projects = projects,
                    onBack = navController::popBackStack,
                    onOpenProject = { id -> navController.navigate("project/$id") },
                    onCreateProject = { draft -> projectViewModel.createProject(draft) },
                )
            }
            composable(
                route = "project/{projectId}",
                arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
            ) { entry ->
                val projectId = entry.arguments?.getLong("projectId") ?: return@composable
                val detailFlow = remember(projectId) { projectViewModel.detail(projectId) }
                val detail by detailFlow.collectAsStateWithLifecycle(initialValue = null)
                var showEditSheet by remember { mutableStateOf(false) }

                ProjectDetailScreen(
                    detail = detail,
                    availableTasks = planTasks,
                    availableGoals = goals,
                    onBack = navController::popBackStack,
                    onEdit = { showEditSheet = true },
                    onDelete = {
                        projectViewModel.deleteProject(projectId)
                        navController.popBackStack("projects", false)
                    },
                    onToggleStatus = { newStatus -> projectViewModel.setStatus(projectId, newStatus) },
                    onAddMilestone = { title -> projectViewModel.addMilestone(projectId, title) },
                    onToggleMilestone = { milestoneId, done -> projectViewModel.toggleMilestone(milestoneId, done) },
                    onDeleteMilestone = { milestoneId -> projectViewModel.deleteMilestone(milestoneId) },
                    onLinkTask = { taskId -> projectViewModel.linkTask(projectId, taskId) },
                    onUnlinkTask = { taskId -> projectViewModel.unlinkTask(projectId, taskId) },
                    onLinkGoal = { goalId -> projectViewModel.linkGoal(projectId, goalId) },
                    onUnlinkGoal = { goalId -> projectViewModel.unlinkGoal(projectId, goalId) },
                    onOpenTask = { taskId -> navController.navigate("plan/$taskId") },
                    onOpenGoal = { goalId -> navController.navigate("goal/$goalId") },
                )

                val currentDetail = detail
                if (showEditSheet && currentDetail != null) {
                    ProjectEditorSheet(
                        existing = currentDetail.project,
                        onDismiss = { showEditSheet = false },
                        onSave = { draft ->
                            projectViewModel.updateProject(projectId, draft)
                            showEditSheet = false
                        },
                    )
                }
            }
        }
    }

    if (showQuickAddSheet) {
        UniversalQuickAddSheet(
            onNewHabit = { prefillName ->
                showQuickAddSheet = false
                if (prefillName.isNotBlank()) {
                    todayViewModel.addHabit(
                        HabitDraft(
                            name = prefillName,
                            emoji = "✨",
                            colorArgb = 0xFF4A6B5D,
                            type = HabitType.Check,
                            direction = HabitDirection.Build,
                            target = 1.0,
                            unit = "",
                            step = 1.0,
                            scheduleDaysMask = 127,
                        )
                    ) { }
                } else {
                    navController.navigate("habit/new")
                }
            },
            onNewTask = { prefillTitle, scheduledEpochDay, dueMinutes ->
                showQuickAddSheet = false
                if (prefillTitle.isNotBlank()) {
                    planViewModel.create(
                        PlanTaskDraft(
                            title = prefillTitle,
                            scheduledEpochDay = scheduledEpochDay ?: java.time.LocalDate.now().toEpochDay(),
                            dueMinutes = dueMinutes,
                        )
                    )
                } else {
                    navController.navigate("plan/new")
                }
            },
            onNewGoal = { prefillTitle ->
                showQuickAddSheet = false
                if (prefillTitle.isNotBlank()) {
                    goalViewModel.saveGoal(
                        null,
                        GoalDraft(
                            title = prefillTitle,
                            targetValue = 1.0,
                            unit = "",
                        )
                    ) { }
                } else {
                    navController.navigate("goal/new")
                }
            },
            onNewContent = { prefillTitle ->
                showQuickAddSheet = false
                if (prefillTitle.isNotBlank()) {
                    libraryViewModel.saveContent(
                        null,
                        ContentDraft(
                            title = prefillTitle,
                            category = ContentCategory.Book,
                            status = ContentStatus.Planned,
                            creator = "",
                            currentUnits = 0.0,
                            totalUnits = 0.0,
                            unit = "sayfa",
                            generalNote = "",
                        )
                    ) { }
                } else {
                    navController.navigate("library/new")
                }
            },
            onStartFocus = {
                showQuickAddSheet = false
                navController.navigate("focus")
            },
            onSaveToInbox = { text ->
                inboxViewModel.capture(text)
            },
            onClose = { showQuickAddSheet = false },
        )
    }

    if (showInboxSheet) {
        val inboxItems by inboxViewModel.items.collectAsStateWithLifecycle()
        InboxSheet(
            items = inboxItems,
            onCapture = inboxViewModel::capture,
            onDismissItem = inboxViewModel::dismiss,
            onConvertToTask = { text, id ->
                planViewModel.create(
                    PlanTaskDraft(
                        title = text,
                        scheduledEpochDay = java.time.LocalDate.now().toEpochDay(),
                    )
                )
                inboxViewModel.markConverted(id)
            },
            onConvertToHabit = { text, id ->
                todayViewModel.addHabit(
                    HabitDraft(
                        name = text,
                        emoji = "✨",
                        colorArgb = 0xFF4A6B5D,
                        type = HabitType.Check,
                        direction = HabitDirection.Build,
                        target = 1.0,
                        unit = "",
                        step = 1.0,
                        scheduleDaysMask = 127,
                    )
                ) { }
                inboxViewModel.markConverted(id)
            },
            onConvertToGoal = { text, id ->
                goalViewModel.saveGoal(
                    null,
                    GoalDraft(
                        title = text,
                        targetValue = 1.0,
                        unit = "",
                    )
                ) { }
                inboxViewModel.markConverted(id)
            },
            onConvertToContent = { text, id ->
                libraryViewModel.saveContent(
                    null,
                    ContentDraft(
                        title = text,
                        category = ContentCategory.Book,
                        status = ContentStatus.Planned,
                        creator = "",
                        currentUnits = 0.0,
                        totalUnits = 0.0,
                        unit = "sayfa",
                        generalNote = "",
                    )
                ) { }
                inboxViewModel.markConverted(id)
            },
            onConvertToNote = { text, id ->
                val epoch = java.time.LocalDate.now().toEpochDay()
                val existingNote = dailyReviewToday?.note.orEmpty()
                val updated = if (existingNote.isBlank()) text else "$existingNote\n• $text"
                reviewViewModel.saveDailyReview(
                    mood = dailyReviewToday?.mood ?: 3,
                    energy = dailyReviewToday?.energy ?: 3,
                    productivity = dailyReviewToday?.productivity ?: 3,
                    stress = dailyReviewToday?.stress ?: 3,
                    note = updated,
                    epochDay = epoch,
                )
                inboxViewModel.markConverted(id)
            },
            onClose = { showInboxSheet = false },
        )
    }

    if (showDailyReviewSheet) {
        val todayEpoch = java.time.LocalDate.now().toEpochDay()
        val todayStartMillis = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todayFocusMinutes = focusSessions.filter { it.startedAt >= todayStartMillis }.sumOf { it.durationMinutes }
        val completedHabits = habits.filter { it.isComplete }
        val todaysTasks = planTasks.filter { it.scheduledEpochDay == null || it.scheduledEpochDay == todayEpoch }

        DailyReviewSheet(
            existingReview = dailyReviewToday,
            incompleteTodayTasks = todaysTasks.filter { !it.completed },
            onCarryTaskToTomorrow = { task ->
                planViewModel.move(task.id, todayEpoch + 1)
            },
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

    if (showAskTsuzukuSheet) {
        AskTsuzukuSheet(
            messages = chatMessages,
            isGenerating = isAiGenerating,
            onAsk = { intelligenceViewModel.askQuestion(it, settings) },
            onClearChat = intelligenceViewModel::clearChat,
            onClose = { showAskTsuzukuSheet = false },
        )
    }

    if (showPlanAssistantSheet) {
        PlanAssistantSheet(
            proposal = activePlanProposal,
            isGenerating = isAiGenerating,
            onGeneratePlan = { prompt, mode -> intelligenceViewModel.generatePlanDraft(prompt, mode, settings) },
            onApplyPlan = { selectedTasks ->
                selectedTasks.forEach { task ->
                    planViewModel.create(
                        PlanTaskDraft(
                            title = task.title,
                            scheduledEpochDay = task.scheduledEpochDay ?: java.time.LocalDate.now().toEpochDay(),
                            startMinutes = task.startMinutes,
                            dueMinutes = task.dueMinutes,
                            estimatedMinutes = task.estimatedMinutes,
                        )
                    ) { createdTaskId ->
                        task.subtasks.forEach { subtaskTitle ->
                            planViewModel.addChecklistItem(createdTaskId, subtaskTitle)
                        }
                    }
                }
                intelligenceViewModel.dismissPlanProposal()
                showPlanAssistantSheet = false
            },
            onToggleTaskSelection = intelligenceViewModel::toggleDraftTaskSelection,
            onUpdateTaskTitle = intelligenceViewModel::updateDraftTaskTitle,
            onUpdateTaskDuration = intelligenceViewModel::updateDraftTaskDuration,
            onAddSubtask = intelligenceViewModel::addDraftSubtask,
            onRemoveSubtask = intelligenceViewModel::removeDraftSubtask,
            onDismissProposal = intelligenceViewModel::dismissPlanProposal,
            onClose = {
                showPlanAssistantSheet = false
                intelligenceViewModel.dismissPlanProposal()
            },
        )
    }

    if (showAiPrivacyDialog) {
        AiPrivacyDialog(
            settings = settings,
            onSetCloudAiEnabled = onSetCloudAiEnabled,
            onSetGeminiApiKey = onSetGeminiApiKey,
            onSetCloudAiModel = onSetCloudAiModel,
            onSetAiShareHabitTitles = onSetAiShareHabitTitles,
            onSetAiShareTaskTitles = onSetAiShareTaskTitles,
            onSetAiShareNotes = onSetAiShareNotes,
            onDismiss = { showAiPrivacyDialog = false },
        )
    }
}

@Composable
private fun EditorialNavigationBar(
    destinations: List<MainDestination>,
    currentRoute: String,
    onSelect: (MainDestination) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        // Do not react to transient system-bar visibility. Otherwise the app's
        // navigation row jumps whenever the immersive navigation bar appears.
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().height(62.dp)) {
                destinations.forEach { destination ->
                    val selected = destination.route == currentRoute
                    val label = stringResource(destination.labelRes)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(destination) }
                            .padding(top = 7.dp, bottom = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .size(width = 26.dp, height = 2.dp)
                                .background(if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent),
                        )
                        Icon(
                            destination.icon,
                            contentDescription = label,
                            modifier = Modifier.padding(top = 5.dp).size(20.dp),
                            tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
