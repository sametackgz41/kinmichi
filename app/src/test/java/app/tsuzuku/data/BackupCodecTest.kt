package app.tsuzuku.data

import app.tsuzuku.data.local.BackupSnapshot
import app.tsuzuku.data.local.ContentNoteEntity
import app.tsuzuku.data.local.ContentSeasonEntity
import app.tsuzuku.data.local.GoalEntity
import app.tsuzuku.data.local.HabitEntity
import app.tsuzuku.data.local.HabitEntryEntity
import app.tsuzuku.data.local.LibraryContentEntity
import app.tsuzuku.data.local.WeeklyReviewEntity
import app.tsuzuku.data.local.CategoryEntity
import app.tsuzuku.data.local.PlanTaskEntity
import app.tsuzuku.data.local.PlanChecklistItemEntity
import app.tsuzuku.data.local.PlanTemplateEntity
import app.tsuzuku.data.local.FocusSessionEntity
import app.tsuzuku.data.local.InboxItemEntity
import app.tsuzuku.data.local.HabitGoalLinkEntity
import app.tsuzuku.data.local.TaskGoalLinkEntity
import app.tsuzuku.data.local.HabitLibraryLinkEntity
import app.tsuzuku.data.local.ProjectEntity
import app.tsuzuku.data.local.ProjectMilestoneEntity
import app.tsuzuku.data.local.ProjectTaskLinkEntity
import app.tsuzuku.data.local.ProjectGoalLinkEntity
import app.tsuzuku.data.local.HabitLevelEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCodecTest {
    @Test
    fun completeSnapshotRoundTripsWithoutSecuritySettings() {
        val snapshot = sampleSnapshot()
        val json = BackupCodec.encode(snapshot)
        val decoded = BackupCodec.decode(json)

        assertEquals(snapshot, decoded)
        assertFalse(json.contains("pin_hash"))
        assertFalse(json.contains("pin_salt"))
        validateBackup(decoded)
    }

    @Test
    fun unknownFormatIsRejectedBeforeRestore() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode("""{"format":"other","version":1,"habits":[],"habitEntries":[],"contents":[],"seasons":[],"contentNotes":[],"goals":[]}""")
        }
    }

    private fun sampleSnapshot() = BackupSnapshot(
        habits = listOf(
            HabitEntity(1, "Kitap oku", "📖", 0xFF5068A9, "Duration", "Build", 30.0, "dk", 5.0, 127, 0, 540, 10, "Not", false, 1, 2, "Weekly", 3, 10, "Akşam", null, "None"),
        ),
        habitEntries = listOf(HabitEntryEntity(1, 1, "2026-08-18", 5.0, "PROGRESS", "", 2, 3)),
        contents = listOf(LibraryContentEntity(1, "Frieren", "Series", "InProgress", "", null, 4.0, 40.0, "bölüm", "Harika", 0, 1, 2)),
        seasons = listOf(ContentSeasonEntity(1, 1, 1, "", 4, 28, 1, 2)),
        contentNotes = listOf(ContentNoteEntity(1, 1, "Kısa not", 2)),
        goals = listOf(GoalEntity(1, "On kitap", "", 1.0, 10.0, "kitap", 21_000, "Active", 1, 2)),
        weeklyReviews = listOf(WeeklyReviewEntity(20_000, "İyi bir hafta", 1, 2)),
        categories = listOf(CategoryEntity(1, "Odak", "🎯", 0xFF5068A9, 3)),
        planTasks = listOf(PlanTaskEntity(id = 1, title = "İçeriği gözden geçir", note = "Ç, Ğ, İ, Ö, Ş, Ü", tags = "Odak,Önemli", createdAt = 1, updatedAt = 2)),
        planChecklistItems = listOf(PlanChecklistItemEntity(id = 1, taskId = 1, title = "İlk adım")),
        planTemplates = listOf(PlanTemplateEntity(id = 1, title = "Haftalık plan", tags = "Odak", createdAt = 1)),
        focusSessions = listOf(FocusSessionEntity(id = 1, taskId = 1, habitId = null, goalId = null, mode = "Pomodoro", startedAt = 1000, endedAt = 2500, durationMinutes = 25, completed = true, interrupted = false, createdAt = 1000)),
        inboxItems = listOf(InboxItemEntity(id = 1, text = "Fikir notu", status = "Open", createdAt = 1000, updatedAt = 1000)),
        habitGoalLinks = listOf(HabitGoalLinkEntity(id = 1, habitId = 1, goalId = 1, contributionMode = "HabitAmount", contributionValue = 1.0)),
        taskGoalLinks = listOf(TaskGoalLinkEntity(id = 1, taskId = 1, goalId = 1, contributionValue = 2.0)),
        habitLibraryLinks = listOf(HabitLibraryLinkEntity(id = 1, habitId = 1, contentId = 1, contributionMultiplier = 1.0)),
        dailyReviews = listOf(app.tsuzuku.data.local.DailyReviewEntity(epochDay = 20_000, mood = 5, energy = 4, productivity = 5, stress = 2, note = "Mükemmel gün", createdAt = 1, updatedAt = 2)),
        periodReviews = listOf(app.tsuzuku.data.local.PeriodReviewEntity(periodType = "Monthly", periodKey = "2026-08", note = "Verimli ay", createdAt = 1, updatedAt = 2)),
        libraryCollections = listOf(app.tsuzuku.data.local.LibraryCollectionEntity(id = 1, name = "Favoriler", emoji = "⭐", createdAt = 1)),
        libraryCollectionItems = listOf(app.tsuzuku.data.local.LibraryCollectionItemEntity(id = 1, collectionId = 1, contentId = 1)),
        projects = listOf(
            ProjectEntity(id = 1, title = "Yeni Web Sitesi", description = "Lansman hazırlığı", colorArgb = 0xFF5068A9, emoji = "🚀", status = "Active", targetDateEpochDay = 21_000, sortOrder = 0, createdAt = 1, updatedAt = 2),
        ),
        projectMilestones = listOf(
            ProjectMilestoneEntity(id = 1, projectId = 1, title = "Tasarım Onayı", sortOrder = 0, completed = true, dueDateEpochDay = 21_000, createdAt = 1),
        ),
        projectTaskLinks = listOf(
            ProjectTaskLinkEntity(id = 1, projectId = 1, taskId = 1),
        ),
        projectGoalLinks = listOf(
            ProjectGoalLinkEntity(id = 1, projectId = 1, goalId = 1),
        ),
        habitFeelings = listOf(app.tsuzuku.data.local.HabitFeelingEntity(habitId = 1, localDate = "2026-08-18", feeling = 3, updatedAt = 4)),
        habitLevels = listOf(
            HabitLevelEntity(
                id = 1,
                habitId = 1,
                levelNumber = 1,
                name = "Başlangıç",
                target = 15.0,
                minimumTarget = 10.0,
                maximumTarget = 30.0,
                rewardNote = "Tebrikler!",
                createdAt = 1,
            ),
        ),
    )
}
