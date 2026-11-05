package app.tsuzuku.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProjectTest {

    @Test
    fun projectProgressCalculatesCorrectlyFromMilestones() {
        val project = Project(
            id = 1L,
            title = "Tsuzuku 2.0 Release",
            description = "Offline Intelligence & Project Tracking",
            colorArgb = 0xFF5068A9,
            emoji = "🚀",
            status = ProjectStatus.Active,
            targetDate = LocalDate.of(2026, 8, 20),
            sortOrder = 0,
            createdAt = 1L,
            updatedAt = 2L,
            milestonesTotal = 4,
            milestonesCompleted = 2,
            tasksTotal = 10,
            tasksCompleted = 5,
        )

        assertEquals(0.5f, project.progressFraction, 0.001f)
    }

    @Test
    fun projectProgressFallsBackToTasksWhenNoMilestones() {
        val project = Project(
            id = 2L,
            title = "Task-Only Project",
            description = "",
            colorArgb = 0xFF5068A9,
            emoji = "📁",
            status = ProjectStatus.Active,
            targetDate = null,
            sortOrder = 1,
            createdAt = 1L,
            updatedAt = 2L,
            milestonesTotal = 0,
            milestonesCompleted = 0,
            tasksTotal = 8,
            tasksCompleted = 6,
        )

        assertEquals(0.75f, project.progressFraction, 0.001f)
    }

    @Test
    fun projectDetailAggregatesMilestonesAndLinks() {
        val project = Project(
            id = 1L,
            title = "Tsuzuku 2.0",
            description = "",
            colorArgb = 0xFF5068A9,
            emoji = "✨",
            status = ProjectStatus.Active,
            targetDate = null,
            sortOrder = 0,
            createdAt = 1L,
            updatedAt = 2L,
            milestonesTotal = 2,
            milestonesCompleted = 1,
            tasksTotal = 1,
            tasksCompleted = 0,
        )

        val milestones = listOf(
            ProjectMilestone(id = 1L, projectId = 1L, title = "Phase 1: Database", sortOrder = 0, completed = true, dueDate = null),
            ProjectMilestone(id = 2L, projectId = 1L, title = "Phase 2: UI", sortOrder = 1, completed = false, dueDate = null),
        )

        val task = PlanTask(
            id = 10L,
            title = "Write Unit Tests",
            note = "",
            scheduledEpochDay = 20_000L,
            dueMinutes = null,
            priority = 1,
            estimatedMinutes = 30,
            actualMinutes = null,
            recurrence = "",
            completed = false,
            sortOrder = 0,
            carryCount = 0,
            checklist = emptyList(),
            startMinutes = null,
            endMinutes = null,
            tags = "",
        )

        val goal = Goal(
            id = 20L,
            title = "Ship Tsuzuku 2.0",
            description = "",
            currentValue = 1.0,
            targetValue = 1.0,
            unit = "release",
            dueDateEpochDay = null,
            status = GoalStatus.Active,
            createdAt = 1L,
            updatedAt = 2L,
        )

        val detail = ProjectDetail(
            project = project,
            milestones = milestones,
            linkedTasks = listOf(task),
            linkedGoals = listOf(goal),
        )

        assertEquals(2, detail.milestones.size)
        assertEquals(1, detail.linkedTasks.size)
        assertEquals(1, detail.linkedGoals.size)
        assertTrue(detail.milestones.first().completed)
    }
}
