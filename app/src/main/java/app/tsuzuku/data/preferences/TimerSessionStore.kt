package app.tsuzuku.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import java.time.LocalDate

data class StoredTimerSession(
    val habitId: Long,
    val startedAtMillis: Long,
    val maxSeconds: Long,
    val localDate: LocalDate,
    /** When it was paused, or 0 while it runs. */
    val pausedAtMillis: Long = 0L,
    /** Time spent paused so far, not counted. */
    val pausedMillis: Long = 0L,
    val habitName: String = "",
    val habitEmoji: String = "",
) {
    val isPaused: Boolean get() = pausedAtMillis > 0L
}

/** The one running timer, kept on disk so it survives the app being closed. */
class TimerSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("tsuzuku_timer", Context.MODE_PRIVATE)

    fun load(): StoredTimerSession? {
        val habitId = preferences.getLong(KEY_HABIT_ID, -1L)
        val startedAt = preferences.getLong(KEY_STARTED_AT, -1L)
        val maxSeconds = preferences.getLong(KEY_MAX_SECONDS, -1L)
        val localDate = runCatching { LocalDate.parse(preferences.getString(KEY_DATE, "")) }.getOrNull()
        if (habitId < 0 || startedAt < 0 || maxSeconds <= 0 || localDate == null) return null
        return StoredTimerSession(
            habitId, startedAt, maxSeconds, localDate,
            pausedAtMillis = preferences.getLong(KEY_PAUSED_AT, 0L),
            pausedMillis = preferences.getLong(KEY_PAUSED_MILLIS, 0L),
            habitName = preferences.getString(KEY_NAME, "").orEmpty(),
            habitEmoji = preferences.getString(KEY_EMOJI, "").orEmpty(),
        )
    }

    fun save(session: StoredTimerSession) {
        preferences.edit()
            .putLong(KEY_HABIT_ID, session.habitId)
            .putLong(KEY_STARTED_AT, session.startedAtMillis)
            .putLong(KEY_MAX_SECONDS, session.maxSeconds)
            .putString(KEY_DATE, session.localDate.toString())
            .putLong(KEY_PAUSED_AT, session.pausedAtMillis)
            .putLong(KEY_PAUSED_MILLIS, session.pausedMillis)
            .putString(KEY_NAME, session.habitName)
            .putString(KEY_EMOJI, session.habitEmoji)
            .commit()
    }

    fun clear() {
        preferences.edit().clear().commit()
    }

    /** The session now and after every change, also changes made from a notification. */
    fun observe(): Flow<StoredTimerSession?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(load()) }
        trySend(load())
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    companion object {
        private const val KEY_HABIT_ID = "habit_id"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_MAX_SECONDS = "max_seconds"
        private const val KEY_DATE = "local_date"
        private const val KEY_PAUSED_AT = "paused_at"
        private const val KEY_PAUSED_MILLIS = "paused_millis"
        private const val KEY_NAME = "habit_name"
        private const val KEY_EMOJI = "habit_emoji"
    }
}

/** Seconds counted so far: wall time minus pauses, never more than the goal. */
fun StoredTimerSession.elapsedSeconds(nowMillis: Long): Long {
    val end = if (isPaused) pausedAtMillis else nowMillis
    return ((end - startedAtMillis - pausedMillis).coerceAtLeast(0L) / 1_000L).coerceAtMost(maxSeconds)
}
