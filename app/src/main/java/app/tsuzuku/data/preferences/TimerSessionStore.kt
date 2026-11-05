package app.tsuzuku.data.preferences

import android.content.Context
import java.time.LocalDate

data class StoredTimerSession(
    val habitId: Long,
    val startedAtMillis: Long,
    val maxSeconds: Long,
    val localDate: LocalDate,
)

class TimerSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("tsuzuku_timer", Context.MODE_PRIVATE)

    fun load(): StoredTimerSession? {
        val habitId = preferences.getLong(KEY_HABIT_ID, -1L)
        val startedAt = preferences.getLong(KEY_STARTED_AT, -1L)
        val maxSeconds = preferences.getLong(KEY_MAX_SECONDS, -1L)
        val localDate = runCatching { LocalDate.parse(preferences.getString(KEY_DATE, "")) }.getOrNull()
        if (habitId < 0 || startedAt < 0 || maxSeconds <= 0 || localDate == null) return null
        return StoredTimerSession(habitId, startedAt, maxSeconds, localDate)
    }

    fun save(session: StoredTimerSession) {
        preferences.edit()
            .putLong(KEY_HABIT_ID, session.habitId)
            .putLong(KEY_STARTED_AT, session.startedAtMillis)
            .putLong(KEY_MAX_SECONDS, session.maxSeconds)
            .putString(KEY_DATE, session.localDate.toString())
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_HABIT_ID = "habit_id"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_MAX_SECONDS = "max_seconds"
        private const val KEY_DATE = "local_date"
    }
}
