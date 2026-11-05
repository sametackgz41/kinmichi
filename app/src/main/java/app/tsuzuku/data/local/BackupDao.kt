package app.tsuzuku.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

data class BackupSnapshot(
    val habits: List<HabitEntity>,
    val habitEntries: List<HabitEntryEntity>,
    val contents: List<LibraryContentEntity>,
    val seasons: List<ContentSeasonEntity>,
    val contentNotes: List<ContentNoteEntity>,
    val goals: List<GoalEntity>,
    val weeklyReviews: List<WeeklyReviewEntity> = emptyList(),
    val planTasks: List<PlanTaskEntity> = emptyList(),
    val planChecklistItems: List<PlanChecklistItemEntity> = emptyList(),
    val planTemplates: List<PlanTemplateEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val focusSessions: List<FocusSessionEntity> = emptyList(),
    val inboxItems: List<InboxItemEntity> = emptyList(),
    val habitGoalLinks: List<HabitGoalLinkEntity> = emptyList(),
    val taskGoalLinks: List<TaskGoalLinkEntity> = emptyList(),
    val habitLibraryLinks: List<HabitLibraryLinkEntity> = emptyList(),
    val dailyReviews: List<DailyReviewEntity> = emptyList(),
    val periodReviews: List<PeriodReviewEntity> = emptyList(),
    val libraryCollections: List<LibraryCollectionEntity> = emptyList(),
    val libraryCollectionItems: List<LibraryCollectionItemEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val projectMilestones: List<ProjectMilestoneEntity> = emptyList(),
    val projectTaskLinks: List<ProjectTaskLinkEntity> = emptyList(),
    val projectGoalLinks: List<ProjectGoalLinkEntity> = emptyList(),
    val habitLevels: List<HabitLevelEntity> = emptyList(),
)

@Dao
interface BackupDao {
    @Query("SELECT * FROM habits ORDER BY id")
    suspend fun getHabits(): List<HabitEntity>

    @Query("SELECT * FROM habit_entries ORDER BY id")
    suspend fun getHabitEntries(): List<HabitEntryEntity>

    @Query("SELECT * FROM library_contents ORDER BY id")
    suspend fun getContents(): List<LibraryContentEntity>

    @Query("SELECT * FROM content_seasons ORDER BY id")
    suspend fun getSeasons(): List<ContentSeasonEntity>

    @Query("SELECT * FROM content_notes ORDER BY id")
    suspend fun getContentNotes(): List<ContentNoteEntity>

    @Query("SELECT * FROM goals ORDER BY id")
    suspend fun getGoals(): List<GoalEntity>

    @Query("SELECT * FROM weekly_reviews ORDER BY weekStartEpochDay")
    suspend fun getWeeklyReviews(): List<WeeklyReviewEntity>

    @Query("SELECT * FROM plan_tasks ORDER BY id") suspend fun getPlanTasks(): List<PlanTaskEntity>
    @Query("SELECT * FROM plan_checklist_items ORDER BY id") suspend fun getPlanChecklistItems(): List<PlanChecklistItemEntity>
    @Query("SELECT * FROM plan_templates ORDER BY id") suspend fun getPlanTemplates(): List<PlanTemplateEntity>
    @Query("SELECT * FROM custom_categories ORDER BY id") suspend fun getCategories(): List<CategoryEntity>
    @Query("SELECT * FROM focus_sessions ORDER BY id") suspend fun getFocusSessions(): List<FocusSessionEntity>
    @Query("SELECT * FROM inbox_items ORDER BY id") suspend fun getInboxItems(): List<InboxItemEntity>
    @Query("SELECT * FROM habit_goal_links ORDER BY id") suspend fun getHabitGoalLinks(): List<HabitGoalLinkEntity>
    @Query("SELECT * FROM task_goal_links ORDER BY id") suspend fun getTaskGoalLinks(): List<TaskGoalLinkEntity>
    @Query("SELECT * FROM habit_library_links ORDER BY id") suspend fun getHabitLibraryLinks(): List<HabitLibraryLinkEntity>
    @Query("SELECT * FROM daily_reviews ORDER BY epochDay") suspend fun getDailyReviews(): List<DailyReviewEntity>
    @Query("SELECT * FROM period_reviews ORDER BY periodKey") suspend fun getPeriodReviews(): List<PeriodReviewEntity>
    @Query("SELECT * FROM library_collections ORDER BY id") suspend fun getLibraryCollections(): List<LibraryCollectionEntity>
    @Query("SELECT * FROM library_collection_items ORDER BY id") suspend fun getLibraryCollectionItems(): List<LibraryCollectionItemEntity>
    @Query("SELECT * FROM projects ORDER BY id") suspend fun getProjects(): List<ProjectEntity>
    @Query("SELECT * FROM project_milestones ORDER BY id") suspend fun getProjectMilestones(): List<ProjectMilestoneEntity>
    @Query("SELECT * FROM project_task_links ORDER BY id") suspend fun getProjectTaskLinks(): List<ProjectTaskLinkEntity>
    @Query("SELECT * FROM project_goal_links ORDER BY id") suspend fun getProjectGoalLinks(): List<ProjectGoalLinkEntity>
    @Query("SELECT * FROM habit_levels ORDER BY id") suspend fun getHabitLevels(): List<HabitLevelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(items: List<HabitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitEntries(items: List<HabitEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContents(items: List<LibraryContentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasons(items: List<ContentSeasonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentNotes(items: List<ContentNoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(items: List<GoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyReviews(items: List<WeeklyReviewEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPlanTasks(items: List<PlanTaskEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPlanChecklistItems(items: List<PlanChecklistItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPlanTemplates(items: List<PlanTemplateEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCategories(items: List<CategoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFocusSessions(items: List<FocusSessionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertInboxItems(items: List<InboxItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHabitGoalLinks(items: List<HabitGoalLinkEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTaskGoalLinks(items: List<TaskGoalLinkEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHabitLibraryLinks(items: List<HabitLibraryLinkEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDailyReviews(items: List<DailyReviewEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPeriodReviews(items: List<PeriodReviewEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLibraryCollections(items: List<LibraryCollectionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLibraryCollectionItems(items: List<LibraryCollectionItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProjects(items: List<ProjectEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProjectMilestones(items: List<ProjectMilestoneEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProjectTaskLinks(items: List<ProjectTaskLinkEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProjectGoalLinks(items: List<ProjectGoalLinkEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHabitLevels(items: List<HabitLevelEntity>)

    @Query("DELETE FROM habit_entries")
    suspend fun clearHabitEntries()

    @Query("DELETE FROM habit_levels")
    suspend fun clearHabitLevels()

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Query("DELETE FROM content_notes")
    suspend fun clearContentNotes()

    @Query("DELETE FROM content_seasons")
    suspend fun clearSeasons()

    @Query("DELETE FROM library_contents")
    suspend fun clearContents()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Query("DELETE FROM weekly_reviews")
    suspend fun clearWeeklyReviews()

    @Query("DELETE FROM plan_checklist_items") suspend fun clearPlanChecklistItems()
    @Query("DELETE FROM plan_tasks") suspend fun clearPlanTasks()
    @Query("DELETE FROM plan_templates") suspend fun clearPlanTemplates()
    @Query("DELETE FROM custom_categories") suspend fun clearCategories()
    @Query("DELETE FROM focus_sessions") suspend fun clearFocusSessions()
    @Query("DELETE FROM inbox_items") suspend fun clearInboxItems()
    @Query("DELETE FROM habit_goal_links") suspend fun clearHabitGoalLinks()
    @Query("DELETE FROM task_goal_links") suspend fun clearTaskGoalLinks()
    @Query("DELETE FROM habit_library_links") suspend fun clearHabitLibraryLinks()
    @Query("DELETE FROM daily_reviews") suspend fun clearDailyReviews()
    @Query("DELETE FROM period_reviews") suspend fun clearPeriodReviews()
    @Query("DELETE FROM library_collection_items") suspend fun clearLibraryCollectionItems()
    @Query("DELETE FROM library_collections") suspend fun clearLibraryCollections()
    @Query("DELETE FROM project_task_links") suspend fun clearProjectTaskLinks()
    @Query("DELETE FROM project_goal_links") suspend fun clearProjectGoalLinks()
    @Query("DELETE FROM project_milestones") suspend fun clearProjectMilestones()
    @Query("DELETE FROM projects") suspend fun clearProjects()

    @Transaction
    suspend fun snapshot(): BackupSnapshot = BackupSnapshot(
        habits = getHabits(),
        habitEntries = getHabitEntries(),
        contents = getContents(),
        seasons = getSeasons(),
        contentNotes = getContentNotes(),
        goals = getGoals(),
        weeklyReviews = getWeeklyReviews(),
        planTasks = getPlanTasks(),
        planChecklistItems = getPlanChecklistItems(),
        planTemplates = getPlanTemplates(),
        categories = getCategories(),
        focusSessions = getFocusSessions(),
        inboxItems = getInboxItems(),
        habitGoalLinks = getHabitGoalLinks(),
        taskGoalLinks = getTaskGoalLinks(),
        habitLibraryLinks = getHabitLibraryLinks(),
        dailyReviews = getDailyReviews(),
        periodReviews = getPeriodReviews(),
        libraryCollections = getLibraryCollections(),
        libraryCollectionItems = getLibraryCollectionItems(),
        projects = getProjects(),
        projectMilestones = getProjectMilestones(),
        projectTaskLinks = getProjectTaskLinks(),
        projectGoalLinks = getProjectGoalLinks(),
        habitLevels = getHabitLevels(),
    )

    @Transaction
    suspend fun replaceAll(snapshot: BackupSnapshot) {
        clearHabitEntries()
        clearHabitLevels()
        clearHabits()
        clearContentNotes()
        clearSeasons()
        clearContents()
        clearGoals()
        clearWeeklyReviews()
        clearPlanChecklistItems()
        clearPlanTasks()
        clearPlanTemplates()
        clearCategories()
        clearFocusSessions()
        clearInboxItems()
        clearHabitGoalLinks()
        clearTaskGoalLinks()
        clearHabitLibraryLinks()
        clearDailyReviews()
        clearPeriodReviews()
        clearLibraryCollectionItems()
        clearLibraryCollections()
        clearProjectTaskLinks()
        clearProjectGoalLinks()
        clearProjectMilestones()
        clearProjects()
        insertHabits(snapshot.habits)
        insertHabitLevels(snapshot.habitLevels)
        insertHabitEntries(snapshot.habitEntries)
        insertContents(snapshot.contents)
        insertSeasons(snapshot.seasons)
        insertContentNotes(snapshot.contentNotes)
        insertGoals(snapshot.goals)
        insertWeeklyReviews(snapshot.weeklyReviews)
        insertPlanTasks(snapshot.planTasks)
        insertPlanChecklistItems(snapshot.planChecklistItems)
        insertPlanTemplates(snapshot.planTemplates)
        insertCategories(snapshot.categories)
        insertFocusSessions(snapshot.focusSessions)
        insertInboxItems(snapshot.inboxItems)
        insertHabitGoalLinks(snapshot.habitGoalLinks)
        insertTaskGoalLinks(snapshot.taskGoalLinks)
        insertHabitLibraryLinks(snapshot.habitLibraryLinks)
        insertDailyReviews(snapshot.dailyReviews)
        insertPeriodReviews(snapshot.periodReviews)
        insertLibraryCollections(snapshot.libraryCollections)
        insertLibraryCollectionItems(snapshot.libraryCollectionItems)
        insertProjects(snapshot.projects)
        insertProjectMilestones(snapshot.projectMilestones)
        insertProjectTaskLinks(snapshot.projectTaskLinks)
        insertProjectGoalLinks(snapshot.projectGoalLinks)
    }
}
