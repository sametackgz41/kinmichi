package app.tsuzuku.data

import android.content.Context
import app.tsuzuku.model.ADVICE_WINDOW_DAYS
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.TargetAdvice
import app.tsuzuku.model.targetAdvice
import java.time.LocalDate

/**
 * Small device-local memory for target coaching: the target a habit had before Tsuzuku
 * suggested lowering it, and until when a habit's suggestions are snoozed.
 */
class CoachingPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tsuzuku_coaching", Context.MODE_PRIVATE)

    fun originalTarget(habitId: Long): Double? =
        prefs.getString("original_$habitId", null)?.toDoubleOrNull()

    private fun snoozedUntil(habitId: Long): Long = prefs.getLong("snooze_$habitId", Long.MIN_VALUE)

    /** The first habit with a suggestion today, skipping snoozed ones. */
    fun pickAdvice(history: HabitHistorySnapshot, today: LocalDate): TargetAdvice? =
        history.habits.asSequence()
            .filter { today.toEpochDay() > snoozedUntil(it.id) }
            .mapNotNull { targetAdvice(history, it, today, originalTarget(it.id)) }
            .firstOrNull()

    /**
     * Records an accepted suggestion. The next one waits a full window so it's judged on
     * days lived with the new target.
     */
    fun accepted(advice: TargetAdvice, today: LocalDate) {
        val id = advice.habit.id
        prefs.edit().apply {
            if (advice.kind == TargetAdvice.Kind.Lower && originalTarget(id) == null) {
                putString("original_$id", advice.habit.target.toString())
            }
            if (advice.kind == TargetAdvice.Kind.Raise && advice.newTarget >= (originalTarget(id) ?: Double.MAX_VALUE)) {
                remove("original_$id")
            }
            putLong("snooze_$id", today.toEpochDay() + ADVICE_WINDOW_DAYS)
        }.apply()
    }

    fun dismissed(advice: TargetAdvice, today: LocalDate) {
        prefs.edit().putLong("snooze_${advice.habit.id}", today.toEpochDay() + ADVICE_WINDOW_DAYS).apply()
    }
}
