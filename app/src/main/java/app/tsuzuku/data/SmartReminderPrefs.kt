package app.tsuzuku.data

import android.content.Context
import java.time.LocalDate

/** Remembers which habits' reminder suggestions were answered, so each is offered at most monthly. */
class SmartReminderPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tsuzuku_smart_reminder", Context.MODE_PRIVATE)

    fun isSnoozed(habitId: Long, today: LocalDate): Boolean =
        today.toEpochDay() <= prefs.getLong("snooze_$habitId", Long.MIN_VALUE)

    fun snooze(habitId: Long, today: LocalDate) {
        prefs.edit().putLong("snooze_$habitId", today.toEpochDay() + 30).apply()
    }
}
