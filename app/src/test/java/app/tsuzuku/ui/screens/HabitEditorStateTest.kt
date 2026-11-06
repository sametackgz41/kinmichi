package app.tsuzuku.ui.screens

import androidx.compose.runtime.saveable.SaverScope
import app.tsuzuku.model.GoalPeriod
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitEditorStateTest {
    @Test
    fun unsavedEditsSurviveRecreation() {
        val state = HabitEditorState(existing = null).apply {
            name = "Read"
            emoji = "📚"
            changeType(HabitType.Duration, "times", "min")
            direction = HabitDirection.Quit
            toggleDay(6)
            goalPeriod = GoalPeriod.Weekly
            periodTargetText = "4"
            reminderEnabled = true
            reminderTime = "21:30"
            activeEndEpochDay = 20_500L
        }
        val saver = HabitEditorState.saver(existing = null)
        val saved = with(saver) { SaverScope { true }.save(state) }!!
        val restored = saver.restore(saved)!!

        assertEquals(state.toDraft(), restored.toDraft())
        assertEquals("Read", restored.name)
        assertEquals("1111110", restored.selectedDays)
    }
}
