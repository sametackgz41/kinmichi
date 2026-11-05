package app.tsuzuku.data

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.content.Context
import app.tsuzuku.data.local.BackupDao
import app.tsuzuku.data.local.BackupSnapshot
import app.tsuzuku.data.local.ContentNoteEntity
import app.tsuzuku.data.local.ContentSeasonEntity
import app.tsuzuku.data.local.DailyReviewEntity
import app.tsuzuku.data.local.GoalEntity
import app.tsuzuku.data.local.HabitEntity
import app.tsuzuku.data.local.HabitEntryEntity
import app.tsuzuku.data.local.HabitLevelEntity
import app.tsuzuku.data.local.HabitGoalLinkEntity
import app.tsuzuku.data.local.HabitLibraryLinkEntity
import app.tsuzuku.data.local.InboxItemEntity
import app.tsuzuku.data.local.LibraryCollectionEntity
import app.tsuzuku.data.local.LibraryCollectionItemEntity
import app.tsuzuku.data.local.LibraryContentEntity
import app.tsuzuku.data.local.PeriodReviewEntity
import app.tsuzuku.data.local.WeeklyReviewEntity
import app.tsuzuku.data.local.PlanTaskEntity
import app.tsuzuku.data.local.PlanChecklistItemEntity
import app.tsuzuku.data.local.PlanTemplateEntity
import app.tsuzuku.data.local.CategoryEntity
import app.tsuzuku.data.local.FocusSessionEntity
import app.tsuzuku.data.local.ProjectEntity
import app.tsuzuku.data.local.ProjectMilestoneEntity
import app.tsuzuku.data.local.ProjectTaskLinkEntity
import app.tsuzuku.data.local.ProjectGoalLinkEntity
import app.tsuzuku.data.local.TaskGoalLinkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream

data class BackupSummary(
    val habitCount: Int,
    val historyCount: Int,
    val contentCount: Int,
    val goalCount: Int,
)

class BackupRepository(private val dao: BackupDao, private val context: Context? = null) {
    suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        dao.replaceAll(BackupSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
        context?.filesDir?.resolve("covers")?.takeIf { it.isDirectory }?.listFiles()?.forEach { it.delete() }
    }
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        BackupCodec.encode(dao.snapshot())
    }

    suspend fun preview(json: String): BackupSummary = withContext(Dispatchers.Default) {
        BackupCodec.decode(json).also(::validateBackup).summary()
    }

    suspend fun restore(json: String): BackupSummary = withContext(Dispatchers.IO) {
        val snapshot = BackupCodec.decode(json).also(::validateBackup)
        dao.replaceAll(snapshot)
        snapshot.summary()
    }

    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val snapshot = dao.snapshot()
        buildString {
            appendLine("type,id,date,name,value,target,unit,status,note,tags")
            snapshot.habits.forEach { h -> appendLine(listOf("habit", h.id, "", h.name, "", h.target, h.unit, h.goalPeriod, h.generalNote, h.tags).csvRow()) }
            snapshot.habitEntries.forEach { e -> appendLine(listOf("history", e.id, e.localDate, "habit:${e.habitId}", e.amount, "", "", e.status, e.note.orEmpty(), "").csvRow()) }
            snapshot.contents.forEach { c -> appendLine(listOf("content", c.id, "", c.title, c.currentUnits, c.totalUnits, c.unit, c.status, c.generalNote, c.tags).csvRow()) }
            snapshot.goals.forEach { g -> appendLine(listOf("goal", g.id, g.dueDateEpochDay?.toString().orEmpty(), g.title, g.currentValue, g.targetValue, g.unit, g.status, g.description, "").csvRow()) }
            snapshot.weeklyReviews.forEach { r -> appendLine(listOf("weekly_review", r.weekStartEpochDay, r.weekStartEpochDay, "", "", "", "", "", r.note, "").csvRow()) }
            snapshot.planTasks.forEach { t -> appendLine(listOf("task", t.id, t.scheduledEpochDay?.toString().orEmpty(), t.title, t.actualMinutes ?: "", t.estimatedMinutes ?: "", "minute", t.status, t.note, t.tags).csvRow()) }
            snapshot.categories.forEach { c -> appendLine(listOf("category", c.id, "", c.name, "", "", "", "", c.emoji, "").csvRow()) }
        }
    }

    suspend fun exportPdf(output: OutputStream) = withContext(Dispatchers.IO) {
        val snapshot = dao.snapshot()
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun newPage() {
            page?.let(document::finishPage)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            y = 52f
            page!!.canvas.drawText("Tsuzuku", 42f, y, titlePaint)
            y += 28f
        }
        fun line(text: String) {
            if (page == null || y > 800f) newPage()
            page!!.canvas.drawText(text.take(95), 42f, y, bodyPaint)
            y += 17f
        }
        newPage()
        line("Habits: ${snapshot.habits.size}   Tasks: ${snapshot.planTasks.size}   Library: ${snapshot.contents.size}   Goals: ${snapshot.goals.size}")
        line("")
        line("HABITS")
        snapshot.habits.forEach { line("${it.emoji} ${it.name} — ${it.goalPeriod} ${it.periodTarget} — target ${it.target} ${it.unit}") }
        line("")
        line("LIBRARY")
        snapshot.contents.forEach { line("${it.title} — ${it.currentUnits}/${it.totalUnits} ${it.unit} — ${it.status}") }
        line("")
        line("GOALS")
        snapshot.goals.forEach { line("${it.title} — ${it.currentValue}/${it.targetValue} ${it.unit} — ${it.status}") }
        line("")
        line("PLAN")
        snapshot.planTasks.forEach { line("${it.title} — ${it.status} — ${it.estimatedMinutes ?: 0} min") }
        line("")
        line("WEEKLY REVIEWS")
        snapshot.weeklyReviews.forEach { line("${java.time.LocalDate.ofEpochDay(it.weekStartEpochDay)} — ${it.note.replace('\n', ' ')}") }
        page?.let(document::finishPage)
        document.writeTo(output)
        document.close()
    }
}

private fun List<Any?>.csvRow(): String = joinToString(",") { value ->
    val text = value?.toString().orEmpty().replace("\"", "\"\"")
    "\"$text\""
}

internal object BackupCodec {
    private const val FORMAT = "tsuzuku-backup"
    private const val VERSION = 8

    fun encode(snapshot: BackupSnapshot): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("exportedAt", System.currentTimeMillis())
        put("habits", snapshot.habits.toJson { habitToJson(it) })
        put("habitEntries", snapshot.habitEntries.toJson { habitEntryToJson(it) })
        put("contents", snapshot.contents.toJson { contentToJson(it) })
        put("seasons", snapshot.seasons.toJson { seasonToJson(it) })
        put("contentNotes", snapshot.contentNotes.toJson { noteToJson(it) })
        put("goals", snapshot.goals.toJson { goalToJson(it) })
        put("weeklyReviews", snapshot.weeklyReviews.toJson { reviewToJson(it) })
        put("planTasks", snapshot.planTasks.toJson { planTaskToJson(it) })
        put("planChecklistItems", snapshot.planChecklistItems.toJson { planChecklistToJson(it) })
        put("planTemplates", snapshot.planTemplates.toJson { planTemplateToJson(it) })
        put("categories", snapshot.categories.toJson { categoryToJson(it) })
        put("focusSessions", snapshot.focusSessions.toJson { focusSessionToJson(it) })
        put("inboxItems", snapshot.inboxItems.toJson { inboxItemToJson(it) })
        put("habitGoalLinks", snapshot.habitGoalLinks.toJson { habitGoalLinkToJson(it) })
        put("taskGoalLinks", snapshot.taskGoalLinks.toJson { taskGoalLinkToJson(it) })
        put("habitLibraryLinks", snapshot.habitLibraryLinks.toJson { habitLibraryLinkToJson(it) })
        put("dailyReviews", snapshot.dailyReviews.toJson { dailyReviewToJson(it) })
        put("periodReviews", snapshot.periodReviews.toJson { periodReviewToJson(it) })
        put("libraryCollections", snapshot.libraryCollections.toJson { libraryCollectionToJson(it) })
        put("libraryCollectionItems", snapshot.libraryCollectionItems.toJson { libraryCollectionItemToJson(it) })
        put("projects", snapshot.projects.toJson { projectToJson(it) })
        put("projectMilestones", snapshot.projectMilestones.toJson { projectMilestoneToJson(it) })
        put("projectTaskLinks", snapshot.projectTaskLinks.toJson { projectTaskLinkToJson(it) })
        put("projectGoalLinks", snapshot.projectGoalLinks.toJson { projectGoalLinkToJson(it) })
        put("habitLevels", snapshot.habitLevels.toJson { habitLevelToJson(it) })
    }.toString(2)

    fun decode(json: String): BackupSnapshot {
        require(json.length <= 10_000_000) { "Backup is too large" }
        val root = JSONObject(json)
        require(root.getString("format") == FORMAT) { "Unknown backup format" }
        require(root.getInt("version") in 1..VERSION) { "Unsupported backup version" }
        return BackupSnapshot(
            habits = root.getJSONArray("habits").mapObjects(::jsonToHabit),
            habitEntries = root.getJSONArray("habitEntries").mapObjects(::jsonToHabitEntry),
            contents = root.getJSONArray("contents").mapObjects(::jsonToContent),
            seasons = root.getJSONArray("seasons").mapObjects(::jsonToSeason),
            contentNotes = root.getJSONArray("contentNotes").mapObjects(::jsonToNote),
            goals = root.getJSONArray("goals").mapObjects(::jsonToGoal),
            weeklyReviews = root.optJSONArray("weeklyReviews")?.mapObjects(::jsonToReview).orEmpty(),
            planTasks = root.optJSONArray("planTasks")?.mapObjects(::jsonToPlanTask).orEmpty(),
            planChecklistItems = root.optJSONArray("planChecklistItems")?.mapObjects(::jsonToPlanChecklist).orEmpty(),
            planTemplates = root.optJSONArray("planTemplates")?.mapObjects(::jsonToPlanTemplate).orEmpty(),
            categories = root.optJSONArray("categories")?.mapObjects(::jsonToCategory).orEmpty(),
            focusSessions = root.optJSONArray("focusSessions")?.mapObjects(::jsonToFocusSession).orEmpty(),
            inboxItems = root.optJSONArray("inboxItems")?.mapObjects(::jsonToInboxItem).orEmpty(),
            habitGoalLinks = root.optJSONArray("habitGoalLinks")?.mapObjects(::jsonToHabitGoalLink).orEmpty(),
            taskGoalLinks = root.optJSONArray("taskGoalLinks")?.mapObjects(::jsonToTaskGoalLink).orEmpty(),
            habitLibraryLinks = root.optJSONArray("habitLibraryLinks")?.mapObjects(::jsonToHabitLibraryLink).orEmpty(),
            dailyReviews = root.optJSONArray("dailyReviews")?.mapObjects(::jsonToDailyReview).orEmpty(),
            periodReviews = root.optJSONArray("periodReviews")?.mapObjects(::jsonToPeriodReview).orEmpty(),
            libraryCollections = root.optJSONArray("libraryCollections")?.mapObjects(::jsonToLibraryCollection).orEmpty(),
            libraryCollectionItems = root.optJSONArray("libraryCollectionItems")?.mapObjects(::jsonToLibraryCollectionItem).orEmpty(),
            projects = root.optJSONArray("projects")?.mapObjects(::jsonToProject).orEmpty(),
            projectMilestones = root.optJSONArray("projectMilestones")?.mapObjects(::jsonToProjectMilestone).orEmpty(),
            projectTaskLinks = root.optJSONArray("projectTaskLinks")?.mapObjects(::jsonToProjectTaskLink).orEmpty(),
            projectGoalLinks = root.optJSONArray("projectGoalLinks")?.mapObjects(::jsonToProjectGoalLink).orEmpty(),
            habitLevels = root.optJSONArray("habitLevels")?.mapObjects(::jsonToHabitLevel).orEmpty(),
        )
    }

    private fun habitToJson(item: HabitEntity) = JSONObject().apply {
        put("id", item.id); put("name", item.name); put("emoji", item.emoji); put("colorArgb", item.colorArgb)
        put("type", item.type); put("direction", item.direction); put("target", item.target); put("unit", item.unit)
        put("step", item.step); put("scheduleDaysMask", item.scheduleDaysMask); put("sortOrder", item.sortOrder)
        putNullable("reminderMinutes", item.reminderMinutes); putNullable("reminderRepeatMinutes", item.reminderRepeatMinutes)
        put("goalPeriod", item.goalPeriod); put("periodTarget", item.periodTarget); put("routineName", item.routineName)
        putNullable("prerequisiteHabitId", item.prerequisiteHabitId); put("healthMetric", item.healthMetric)
        put("minimumTarget", item.minimumTarget); putNullable("maximumTarget", item.maximumTarget); put("difficulty", item.difficulty)
        put("contextLabel", item.contextLabel); put("intention", item.intention)
        putNullable("activeStartEpochDay", item.activeStartEpochDay); putNullable("activeEndEpochDay", item.activeEndEpochDay)
        put("repeatIntervalDays", item.repeatIntervalDays); putNullable("pausedUntilEpochDay", item.pausedUntilEpochDay)
        put("tags", item.tags); put("currentLevel", item.currentLevel)
        put("generalNote", item.generalNote); put("isArchived", item.isArchived); put("createdAt", item.createdAt); put("updatedAt", item.updatedAt)
    }

    private fun jsonToHabit(o: JSONObject) = HabitEntity(
        id = o.getLong("id"), name = o.getString("name"), emoji = o.getString("emoji"), colorArgb = o.getLong("colorArgb"),
        type = o.getString("type"), direction = o.getString("direction"), target = o.getDouble("target"), unit = o.getString("unit"),
        step = o.getDouble("step"), scheduleDaysMask = o.getInt("scheduleDaysMask"), sortOrder = o.getInt("sortOrder"),
        reminderMinutes = o.optionalInt("reminderMinutes"), reminderRepeatMinutes = o.optionalInt("reminderRepeatMinutes"),
        goalPeriod = o.optString("goalPeriod", "Daily"), periodTarget = o.optInt("periodTarget", 1).coerceAtLeast(1),
        routineName = o.optString("routineName", ""), prerequisiteHabitId = o.optionalLong("prerequisiteHabitId"),
        healthMetric = o.optString("healthMetric", "None"),
        minimumTarget = o.optDouble("minimumTarget", 0.0), maximumTarget = o.optionalDouble("maximumTarget"),
        difficulty = o.optInt("difficulty", 2), contextLabel = o.optString("contextLabel", ""), intention = o.optString("intention", ""),
        activeStartEpochDay = o.optionalLong("activeStartEpochDay"), activeEndEpochDay = o.optionalLong("activeEndEpochDay"),
        repeatIntervalDays = o.optInt("repeatIntervalDays", 1), pausedUntilEpochDay = o.optionalLong("pausedUntilEpochDay"),
        tags = o.optString("tags", ""),
        currentLevel = o.optInt("currentLevel", 1),
        generalNote = o.getString("generalNote"), isArchived = o.getBoolean("isArchived"), createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
    )

    private fun habitEntryToJson(item: HabitEntryEntity) = JSONObject().apply {
        put("id", item.id); put("habitId", item.habitId); put("localDate", item.localDate); put("amount", item.amount)
        put("status", item.status); putNullable("note", item.note); put("createdAt", item.createdAt); put("updatedAt", item.updatedAt)
    }

    private fun jsonToHabitEntry(o: JSONObject) = HabitEntryEntity(
        id = o.getLong("id"), habitId = o.getLong("habitId"), localDate = o.getString("localDate"), amount = o.getDouble("amount"),
        status = o.getString("status"), note = o.optionalString("note"), createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
    )

    private fun contentToJson(item: LibraryContentEntity) = JSONObject().apply {
        put("id", item.id); put("title", item.title); put("category", item.category); put("status", item.status); put("creator", item.creator)
        putNullable("coverUri", item.coverUri); put("currentUnits", item.currentUnits); put("totalUnits", item.totalUnits); put("unit", item.unit)
        put("generalNote", item.generalNote); put("sortOrder", item.sortOrder); put("createdAt", item.createdAt); put("updatedAt", item.updatedAt)
        put("tags", item.tags)
        putNullable("rating", item.rating?.toDouble())
        put("isFavorite", item.isFavorite)
        putNullable("startDateEpochDay", item.startDateEpochDay)
        putNullable("finishDateEpochDay", item.finishDateEpochDay)
        put("reviewNote", item.reviewNote)
        put("rewatchDesired", item.rewatchDesired)
    }

    private fun jsonToContent(o: JSONObject) = LibraryContentEntity(
        id = o.getLong("id"), title = o.getString("title"), category = o.getString("category"), status = o.getString("status"),
        creator = o.getString("creator"), coverUri = o.optionalString("coverUri"), currentUnits = o.getDouble("currentUnits"),
        totalUnits = o.getDouble("totalUnits"), unit = o.getString("unit"), generalNote = o.getString("generalNote"),
        sortOrder = o.getInt("sortOrder"), createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
        tags = o.optString("tags", ""),
        rating = o.optDouble("rating").takeUnless { it.isNaN() || o.isNull("rating") }?.toFloat(),
        isFavorite = o.optBoolean("isFavorite", false),
        startDateEpochDay = o.optionalLong("startDateEpochDay"),
        finishDateEpochDay = o.optionalLong("finishDateEpochDay"),
        reviewNote = o.optString("reviewNote", ""),
        rewatchDesired = o.optBoolean("rewatchDesired", false),
    )

    private fun seasonToJson(item: ContentSeasonEntity) = JSONObject().apply {
        put("id", item.id); put("contentId", item.contentId); put("number", item.number); put("title", item.title)
        put("currentEpisodes", item.currentEpisodes); put("totalEpisodes", item.totalEpisodes); put("createdAt", item.createdAt); put("updatedAt", item.updatedAt)
    }

    private fun jsonToSeason(o: JSONObject) = ContentSeasonEntity(
        id = o.getLong("id"), contentId = o.getLong("contentId"), number = o.getInt("number"), title = o.getString("title"),
        currentEpisodes = o.getInt("currentEpisodes"), totalEpisodes = o.getInt("totalEpisodes"), createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
    )

    private fun noteToJson(item: ContentNoteEntity) = JSONObject().apply {
        put("id", item.id); put("contentId", item.contentId); put("text", item.text); put("createdAt", item.createdAt)
    }

    private fun jsonToNote(o: JSONObject) = ContentNoteEntity(
        id = o.getLong("id"), contentId = o.getLong("contentId"), text = o.getString("text"), createdAt = o.getLong("createdAt"),
    )

    private fun goalToJson(item: GoalEntity) = JSONObject().apply {
        put("id", item.id); put("title", item.title); put("description", item.description); put("currentValue", item.currentValue)
        put("targetValue", item.targetValue); put("unit", item.unit); putNullable("dueDateEpochDay", item.dueDateEpochDay)
        put("status", item.status); put("createdAt", item.createdAt); put("updatedAt", item.updatedAt)
    }

    private fun jsonToGoal(o: JSONObject) = GoalEntity(
        id = o.getLong("id"), title = o.getString("title"), description = o.getString("description"), currentValue = o.getDouble("currentValue"),
        targetValue = o.getDouble("targetValue"), unit = o.getString("unit"), dueDateEpochDay = o.optionalLong("dueDateEpochDay"),
        status = o.getString("status"), createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
    )

    private fun reviewToJson(item: WeeklyReviewEntity) = JSONObject().apply {
        put("weekStartEpochDay", item.weekStartEpochDay); put("note", item.note)
        put("createdAt", item.createdAt); put("updatedAt", item.updatedAt)
    }

    private fun jsonToReview(o: JSONObject) = WeeklyReviewEntity(
        weekStartEpochDay = o.getLong("weekStartEpochDay"), note = o.getString("note"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
    )

    private fun planTaskToJson(item: PlanTaskEntity) = JSONObject().apply {
        put("id", item.id); put("title", item.title); put("note", item.note); putNullable("scheduledEpochDay", item.scheduledEpochDay)
        putNullable("dueMinutes", item.dueMinutes); put("priority", item.priority); putNullable("estimatedMinutes", item.estimatedMinutes)
        putNullable("actualMinutes", item.actualMinutes); put("recurrence", item.recurrence); put("status", item.status)
        put("sortOrder", item.sortOrder); put("carryCount", item.carryCount); put("createdAt", item.createdAt); put("updatedAt", item.updatedAt)
        putNullable("startMinutes", item.startMinutes); putNullable("endMinutes", item.endMinutes)
        put("tags", item.tags)
    }

    private fun jsonToPlanTask(o: JSONObject) = PlanTaskEntity(
        id = o.getLong("id"), title = o.getString("title"), note = o.optString("note", ""),
        scheduledEpochDay = o.optionalLong("scheduledEpochDay"), dueMinutes = o.optionalInt("dueMinutes"),
        priority = o.optInt("priority", 1), estimatedMinutes = o.optionalInt("estimatedMinutes"), actualMinutes = o.optionalInt("actualMinutes"),
        recurrence = o.optString("recurrence", "None"), status = o.optString("status", "Active"), sortOrder = o.optInt("sortOrder", 0),
        carryCount = o.optInt("carryCount", 0), createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
        startMinutes = o.optionalInt("startMinutes"), endMinutes = o.optionalInt("endMinutes"),
        tags = o.optString("tags", ""),
    )

    private fun planChecklistToJson(item: PlanChecklistItemEntity) = JSONObject().apply {
        put("id", item.id); put("taskId", item.taskId); put("title", item.title); put("completed", item.completed); put("sortOrder", item.sortOrder)
    }

    private fun jsonToPlanChecklist(o: JSONObject) = PlanChecklistItemEntity(
        id = o.getLong("id"), taskId = o.getLong("taskId"), title = o.getString("title"), completed = o.optBoolean("completed"), sortOrder = o.optInt("sortOrder", 0),
    )

    private fun planTemplateToJson(item: PlanTemplateEntity) = JSONObject().apply {
        put("id", item.id); put("title", item.title); put("note", item.note); put("priority", item.priority)
        putNullable("estimatedMinutes", item.estimatedMinutes); putNullable("startMinutes", item.startMinutes); putNullable("endMinutes", item.endMinutes)
        put("recurrence", item.recurrence); put("tags", item.tags); put("createdAt", item.createdAt)
    }

    private fun jsonToPlanTemplate(o: JSONObject) = PlanTemplateEntity(
        id = o.getLong("id"), title = o.getString("title"), note = o.optString("note", ""), priority = o.optInt("priority", 1),
        estimatedMinutes = o.optionalInt("estimatedMinutes"), startMinutes = o.optionalInt("startMinutes"), endMinutes = o.optionalInt("endMinutes"),
        recurrence = o.optString("recurrence", "None"), tags = o.optString("tags", ""), createdAt = o.getLong("createdAt"),
    )

    private fun categoryToJson(item: CategoryEntity) = JSONObject().apply {
        put("id", item.id); put("name", item.name); put("emoji", item.emoji); put("colorArgb", item.colorArgb); put("createdAt", item.createdAt)
    }

    private fun jsonToCategory(o: JSONObject) = CategoryEntity(
        id = o.getLong("id"), name = o.getString("name"), emoji = o.optString("emoji", "◆"),
        colorArgb = o.optLong("colorArgb", 0xFF5068A9), createdAt = o.optLong("createdAt", System.currentTimeMillis()),
    )

    private fun focusSessionToJson(item: FocusSessionEntity) = JSONObject().apply {
        put("id", item.id); putNullable("taskId", item.taskId); putNullable("habitId", item.habitId)
        putNullable("goalId", item.goalId); put("mode", item.mode); put("startedAt", item.startedAt)
        putNullable("endedAt", item.endedAt); put("durationMinutes", item.durationMinutes)
        put("completed", item.completed); put("interrupted", item.interrupted); put("createdAt", item.createdAt)
    }

    private fun jsonToFocusSession(o: JSONObject) = FocusSessionEntity(
        id = o.getLong("id"), taskId = o.optionalLong("taskId"), habitId = o.optionalLong("habitId"),
        goalId = o.optionalLong("goalId"), mode = o.getString("mode"), startedAt = o.getLong("startedAt"),
        endedAt = o.optionalLong("endedAt"), durationMinutes = o.optInt("durationMinutes", 0),
        completed = o.optBoolean("completed"), interrupted = o.optBoolean("interrupted"),
        createdAt = o.getLong("createdAt"),
    )

    private fun inboxItemToJson(item: InboxItemEntity) = JSONObject().apply {
        put("id", item.id); put("text", item.text); put("status", item.status)
        put("createdAt", item.createdAt); put("updatedAt", item.updatedAt)
    }

    private fun jsonToInboxItem(o: JSONObject) = InboxItemEntity(
        id = o.getLong("id"), text = o.getString("text"), status = o.optString("status", "Open"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
    )

    private fun habitGoalLinkToJson(item: HabitGoalLinkEntity) = JSONObject().apply {
        put("id", item.id); put("habitId", item.habitId); put("goalId", item.goalId)
        put("contributionMode", item.contributionMode); put("contributionValue", item.contributionValue)
    }

    private fun jsonToHabitGoalLink(o: JSONObject) = HabitGoalLinkEntity(
        id = o.getLong("id"), habitId = o.getLong("habitId"), goalId = o.getLong("goalId"),
        contributionMode = o.optString("contributionMode", "HabitAmount"),
        contributionValue = o.optDouble("contributionValue", 1.0),
    )

    private fun taskGoalLinkToJson(item: TaskGoalLinkEntity) = JSONObject().apply {
        put("id", item.id); put("taskId", item.taskId); put("goalId", item.goalId)
        put("contributionValue", item.contributionValue)
    }

    private fun jsonToTaskGoalLink(o: JSONObject) = TaskGoalLinkEntity(
        id = o.getLong("id"), taskId = o.getLong("taskId"), goalId = o.getLong("goalId"),
        contributionValue = o.optDouble("contributionValue", 0.0),
    )

    private fun habitLibraryLinkToJson(item: HabitLibraryLinkEntity) = JSONObject().apply {
        put("id", item.id); put("habitId", item.habitId); put("contentId", item.contentId)
        put("contributionMultiplier", item.contributionMultiplier)
    }

    private fun jsonToHabitLibraryLink(o: JSONObject) = HabitLibraryLinkEntity(
        id = o.getLong("id"), habitId = o.getLong("habitId"), contentId = o.getLong("contentId"),
        contributionMultiplier = o.optDouble("contributionMultiplier", 1.0),
    )

    private fun dailyReviewToJson(item: DailyReviewEntity) = JSONObject().apply {
        put("epochDay", item.epochDay); put("mood", item.mood); put("energy", item.energy)
        put("productivity", item.productivity); put("stress", item.stress); put("note", item.note)
        put("createdAt", item.createdAt); put("updatedAt", item.updatedAt)
    }

    private fun jsonToDailyReview(o: JSONObject) = DailyReviewEntity(
        epochDay = o.getLong("epochDay"), mood = o.getInt("mood"), energy = o.getInt("energy"),
        productivity = o.getInt("productivity"), stress = o.getInt("stress"), note = o.getString("note"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
    )

    private fun periodReviewToJson(item: PeriodReviewEntity) = JSONObject().apply {
        put("periodType", item.periodType); put("periodKey", item.periodKey); put("note", item.note)
        put("createdAt", item.createdAt); put("updatedAt", item.updatedAt)
    }

    private fun jsonToPeriodReview(o: JSONObject) = PeriodReviewEntity(
        periodType = o.getString("periodType"), periodKey = o.getString("periodKey"), note = o.getString("note"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
    )

    private fun libraryCollectionToJson(item: LibraryCollectionEntity) = JSONObject().apply {
        put("id", item.id); put("name", item.name); put("emoji", item.emoji); put("createdAt", item.createdAt)
    }

    private fun jsonToLibraryCollection(o: JSONObject) = LibraryCollectionEntity(
        id = o.getLong("id"), name = o.getString("name"), emoji = o.getString("emoji"), createdAt = o.getLong("createdAt"),
    )

    private fun libraryCollectionItemToJson(item: LibraryCollectionItemEntity) = JSONObject().apply {
        put("id", item.id); put("collectionId", item.collectionId); put("contentId", item.contentId)
    }

    private fun jsonToLibraryCollectionItem(o: JSONObject) = LibraryCollectionItemEntity(
        id = o.getLong("id"), collectionId = o.getLong("collectionId"), contentId = o.getLong("contentId"),
    )

    private fun projectToJson(item: ProjectEntity) = JSONObject().apply {
        put("id", item.id); put("title", item.title); put("description", item.description)
        put("colorArgb", item.colorArgb); put("emoji", item.emoji); put("status", item.status)
        putNullable("targetDateEpochDay", item.targetDateEpochDay); put("sortOrder", item.sortOrder)
        put("createdAt", item.createdAt); put("updatedAt", item.updatedAt)
    }

    private fun jsonToProject(o: JSONObject) = ProjectEntity(
        id = o.getLong("id"), title = o.getString("title"), description = o.optString("description", ""),
        colorArgb = o.optLong("colorArgb", 0xFF5068A9), emoji = o.optString("emoji", "📁"),
        status = o.optString("status", "Active"), targetDateEpochDay = o.optionalLong("targetDateEpochDay"),
        sortOrder = o.optInt("sortOrder", 0), createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
    )

    private fun projectMilestoneToJson(item: ProjectMilestoneEntity) = JSONObject().apply {
        put("id", item.id); put("projectId", item.projectId); put("title", item.title)
        put("sortOrder", item.sortOrder); put("completed", item.completed)
        putNullable("dueDateEpochDay", item.dueDateEpochDay); put("createdAt", item.createdAt)
    }

    private fun jsonToProjectMilestone(o: JSONObject) = ProjectMilestoneEntity(
        id = o.getLong("id"), projectId = o.getLong("projectId"), title = o.getString("title"),
        sortOrder = o.optInt("sortOrder", 0), completed = o.optBoolean("completed", false),
        dueDateEpochDay = o.optionalLong("dueDateEpochDay"), createdAt = o.getLong("createdAt"),
    )

    private fun projectTaskLinkToJson(item: ProjectTaskLinkEntity) = JSONObject().apply {
        put("id", item.id); put("projectId", item.projectId); put("taskId", item.taskId)
    }

    private fun jsonToProjectTaskLink(o: JSONObject) = ProjectTaskLinkEntity(
        id = o.getLong("id"), projectId = o.getLong("projectId"), taskId = o.getLong("taskId"),
    )

    private fun projectGoalLinkToJson(item: ProjectGoalLinkEntity) = JSONObject().apply {
        put("id", item.id); put("projectId", item.projectId); put("goalId", item.goalId)
    }

    private fun jsonToProjectGoalLink(o: JSONObject) = ProjectGoalLinkEntity(
        id = o.getLong("id"), projectId = o.getLong("projectId"), goalId = o.getLong("goalId"),
    )

    private fun habitLevelToJson(item: HabitLevelEntity) = JSONObject().apply {
        put("id", item.id); put("habitId", item.habitId); put("levelNumber", item.levelNumber)
        put("name", item.name); put("target", item.target); put("minimumTarget", item.minimumTarget)
        putNullable("maximumTarget", item.maximumTarget); put("rewardNote", item.rewardNote); put("createdAt", item.createdAt)
    }

    private fun jsonToHabitLevel(o: JSONObject) = HabitLevelEntity(
        id = o.getLong("id"), habitId = o.getLong("habitId"), levelNumber = o.getInt("levelNumber"),
        name = o.getString("name"), target = o.getDouble("target"), minimumTarget = o.optDouble("minimumTarget", 0.0),
        maximumTarget = o.optionalDouble("maximumTarget"), rewardNote = o.optString("rewardNote", ""),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
    )
}

internal fun validateBackup(snapshot: BackupSnapshot) {
    require(snapshot.habits.map { it.id }.allUniquePositive()) { "Invalid habit identifiers" }
    require(snapshot.habitEntries.map { it.id }.allUniquePositive()) { "Invalid history identifiers" }
    require(snapshot.contents.map { it.id }.allUniquePositive()) { "Invalid content identifiers" }
    require(snapshot.seasons.map { it.id }.allUniquePositive()) { "Invalid season identifiers" }
    require(snapshot.contentNotes.map { it.id }.allUniquePositive()) { "Invalid note identifiers" }
    require(snapshot.goals.map { it.id }.allUniquePositive()) { "Invalid goal identifiers" }
    require(snapshot.weeklyReviews.map { it.weekStartEpochDay }.distinct().size == snapshot.weeklyReviews.size) { "Invalid weekly reviews" }
    require(snapshot.planTasks.map { it.id }.allUniquePositive()) { "Invalid task identifiers" }
    require(snapshot.planChecklistItems.map { it.id }.allUniquePositive()) { "Invalid checklist identifiers" }
    require(snapshot.planTemplates.map { it.id }.allUniquePositive()) { "Invalid template identifiers" }
    require(snapshot.categories.map { it.id }.allUniquePositive()) { "Invalid category identifiers" }
    require(snapshot.categories.all { it.name.isNotBlank() }) { "Invalid category data" }
    require(snapshot.focusSessions.map { it.id }.allUniquePositive()) { "Invalid focus session identifiers" }
    require(snapshot.inboxItems.map { it.id }.allUniquePositive()) { "Invalid inbox item identifiers" }
    require(snapshot.projects.map { it.id }.allUniquePositive()) { "Invalid project identifiers" }
    require(snapshot.projectMilestones.map { it.id }.allUniquePositive()) { "Invalid milestone identifiers" }
    require(snapshot.habitLevels.map { it.id }.allUniquePositive()) { "Invalid habit level identifiers" }
    val habitIds = snapshot.habits.mapTo(mutableSetOf()) { it.id }
    val contentIds = snapshot.contents.mapTo(mutableSetOf()) { it.id }
    val taskIds = snapshot.planTasks.mapTo(mutableSetOf()) { it.id }
    val projectIds = snapshot.projects.mapTo(mutableSetOf()) { it.id }
    require(snapshot.habitEntries.all { it.habitId in habitIds }) { "History references a missing habit" }
    require(snapshot.habitLevels.all { it.habitId in habitIds }) { "Habit level references a missing habit" }
    require(snapshot.habits.all { it.prerequisiteHabitId == null || (it.prerequisiteHabitId in habitIds && it.prerequisiteHabitId != it.id) }) { "Habit prerequisite is invalid" }
    require(snapshot.seasons.all { it.contentId in contentIds }) { "Season references missing content" }
    require(snapshot.contentNotes.all { it.contentId in contentIds }) { "Note references missing content" }
    require(snapshot.planChecklistItems.all { it.taskId in taskIds }) { "Checklist references a missing task" }
    require(snapshot.projectMilestones.all { it.projectId in projectIds }) { "Milestone references missing project" }
    require(snapshot.projectTaskLinks.all { it.projectId in projectIds && it.taskId in taskIds }) { "Project task link is invalid" }
    require(snapshot.planTasks.all { it.title.isNotBlank() && it.priority in 1..3 }) { "Invalid task data" }
    require(snapshot.habits.all { it.name.isNotBlank() && it.target > 0 && it.step > 0 && it.periodTarget in 1..31 }) { "Invalid habit data" }
    require(snapshot.contents.all { it.title.isNotBlank() && it.currentUnits >= 0 && it.totalUnits >= 0 }) { "Invalid content data" }
    require(snapshot.goals.all { it.title.isNotBlank() && it.currentValue >= 0 && it.targetValue > 0 }) { "Invalid goal data" }
    require(snapshot.projects.all { it.title.isNotBlank() }) { "Invalid project data" }
    require(snapshot.habitLevels.all { it.name.isNotBlank() && it.target > 0 }) { "Invalid habit level data" }
}

private fun BackupSnapshot.summary() = BackupSummary(habits.size, habitEntries.size, contents.size, goals.size)
private fun List<Long>.allUniquePositive(): Boolean = all { it > 0 } && distinct().size == size
private fun <T> List<T>.toJson(transform: (T) -> JSONObject) = JSONArray().also { array -> forEach { array.put(transform(it)) } }
private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = List(length()) { transform(getJSONObject(it)) }
private fun JSONObject.putNullable(key: String, value: Any?) { put(key, value ?: JSONObject.NULL) }
private fun JSONObject.optionalString(key: String): String? = if (isNull(key)) null else getString(key)
private fun JSONObject.optionalInt(key: String): Int? = if (isNull(key)) null else getInt(key)
private fun JSONObject.optionalLong(key: String): Long? = if (isNull(key)) null else getLong(key)
private fun JSONObject.optionalDouble(key: String): Double? = if (isNull(key)) null else getDouble(key)
