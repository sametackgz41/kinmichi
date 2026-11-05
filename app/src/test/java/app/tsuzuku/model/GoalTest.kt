package app.tsuzuku.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalTest {
    @Test
    fun progressFractionIsBounded() {
        assertEquals(0.0f, goal(current = -2.0).progressFraction)
        assertEquals(0.5f, goal(current = 5.0).progressFraction)
        assertEquals(1.0f, goal(current = 20.0).progressFraction)
    }

    private fun goal(current: Double) = Goal(
        id = 1,
        title = "Read",
        description = "",
        currentValue = current,
        targetValue = 10.0,
        unit = "books",
        dueDateEpochDay = null,
        status = GoalStatus.Active,
        createdAt = 0,
        updatedAt = 0,
    )
}
