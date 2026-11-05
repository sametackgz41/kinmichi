package app.tsuzuku.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HabitEditorInputTest {
    @Test
    fun validReminderTimeConvertsToMinutes() {
        assertEquals(9 * 60 + 5, "09:05".toReminderMinutesOrNull())
        assertEquals(23 * 60 + 59, "23:59".toReminderMinutesOrNull())
    }

    @Test
    fun invalidReminderTimesAreRejected() {
        assertNull("24:00".toReminderMinutesOrNull())
        assertNull("09:60".toReminderMinutesOrNull())
        assertNull("morning".toReminderMinutesOrNull())
    }
}
