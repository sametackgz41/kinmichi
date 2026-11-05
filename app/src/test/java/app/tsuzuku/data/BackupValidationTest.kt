package app.tsuzuku.data

import app.tsuzuku.data.local.BackupSnapshot
import app.tsuzuku.data.local.HabitEntity
import app.tsuzuku.data.local.HabitEntryEntity
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupValidationTest {
    @Test
    fun validSnapshotPassesRelationshipChecks() {
        validateBackup(validSnapshot())
    }

    @Test
    fun validSnapshotWithHabitLevelsPassesValidation() {
        val level = app.tsuzuku.data.local.HabitLevelEntity(id = 1, habitId = 1, levelNumber = 1, name = "Level 1", target = 10.0)
        val snapshot = validSnapshot().copy(habitLevels = listOf(level))
        validateBackup(snapshot)
    }

    @Test
    fun historyCannotReferenceMissingHabit() {
        val snapshot = validSnapshot().copy(habits = emptyList())
        assertThrows(IllegalArgumentException::class.java) { validateBackup(snapshot) }
    }

    @Test
    fun duplicateIdentifiersAreRejected() {
        val habit = validSnapshot().habits.single()
        val snapshot = validSnapshot().copy(habits = listOf(habit, habit.copy(name = "Duplicate")))
        assertThrows(IllegalArgumentException::class.java) { validateBackup(snapshot) }
    }

    @Test
    fun habitLevelCannotReferenceMissingHabit() {
        val level = app.tsuzuku.data.local.HabitLevelEntity(id = 1, habitId = 999, levelNumber = 1, name = "Level 1", target = 10.0)
        val snapshot = validSnapshot().copy(habitLevels = listOf(level))
        assertThrows(IllegalArgumentException::class.java) { validateBackup(snapshot) }
    }

    @Test
    fun invalidHabitLevelDataIsRejected() {
        val level = app.tsuzuku.data.local.HabitLevelEntity(id = 1, habitId = 1, levelNumber = 1, name = "", target = 0.0)
        val snapshot = validSnapshot().copy(habitLevels = listOf(level))
        assertThrows(IllegalArgumentException::class.java) { validateBackup(snapshot) }
    }

    @Test
    fun duplicateHabitLevelIdentifiersAreRejected() {
        val level1 = app.tsuzuku.data.local.HabitLevelEntity(id = 1, habitId = 1, levelNumber = 1, name = "Level 1", target = 10.0)
        val level2 = app.tsuzuku.data.local.HabitLevelEntity(id = 1, habitId = 1, levelNumber = 2, name = "Level 2", target = 20.0)
        val snapshot = validSnapshot().copy(habitLevels = listOf(level1, level2))
        assertThrows(IllegalArgumentException::class.java) { validateBackup(snapshot) }
    }

    private fun validSnapshot() = BackupSnapshot(
        habits = listOf(
            HabitEntity(
                id = 1,
                name = "Read",
                emoji = "📖",
                colorArgb = 0xFF5068A9,
                type = "Count",
                direction = "Build",
                target = 10.0,
                unit = "pages",
                step = 1.0,
                scheduleDaysMask = 127,
                sortOrder = 0,
                createdAt = 1,
                updatedAt = 1,
            ),
        ),
        habitEntries = listOf(
            HabitEntryEntity(
                id = 1,
                habitId = 1,
                localDate = "2026-08-18",
                amount = 1.0,
                createdAt = 1,
                updatedAt = 1,
            ),
        ),
        contents = emptyList(),
        seasons = emptyList(),
        contentNotes = emptyList(),
        goals = emptyList(),
    )
}
