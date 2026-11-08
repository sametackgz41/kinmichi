package app.tsuzuku.ui

import android.content.Intent
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitDirection

/**
 * A pre-filled "new habit" handed over by an ADD_HABIT intent with extras. The editor opens
 * with these values and nothing is saved until the person taps Save.
 *
 * Extras (all optional): name, emoji, type (check|count|duration), target, unit, step,
 * tiny (bad-day version), intention (if-then plan), days (7 chars, Monday first, e.g. 1111111),
 * reminder (HH:mm).
 */
object NewHabitTemplate {
    @Volatile private var pending: Habit? = null

    fun offer(intent: Intent?) {
        val extras = intent?.extras ?: return
        if (extras.getString("name").isNullOrBlank()) return
        val type = when (extras.getString("type")?.lowercase()) {
            "count" -> HabitType.Count
            "duration" -> HabitType.Duration
            else -> HabitType.Check
        }
        val days = extras.getString("days")?.takeIf { it.length == 7 && it.all { c -> c == '0' || c == '1' } } ?: "1111111"
        val reminder = extras.getString("reminder")?.let { text ->
            val parts = text.split(':')
            val h = parts.getOrNull(0)?.toIntOrNull()
            val m = parts.getOrNull(1)?.toIntOrNull()
            if (h != null && m != null && h in 0..23 && m in 0..59) h * 60 + m else null
        }
        pending = Habit(
            id = 0,
            name = extras.getString("name").orEmpty().take(80),
            emoji = extras.getString("emoji")?.takeIf { it.isNotBlank() } ?: "✓",
            colorArgb = 0,
            type = type,
            direction = HabitDirection.Build,
            target = extras.getString("target")?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0,
            unit = extras.getString("unit").orEmpty().take(30),
            step = extras.getString("step")?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0,
            scheduleDaysMask = days.foldIndexed(0) { i, mask, c -> if (c == '1') mask or (1 shl i) else mask },
            sortOrder = 0,
            reminderMinutes = reminder,
            intention = extras.getString("intention").orEmpty().take(240),
            tinyVersion = extras.getString("tiny").orEmpty().take(60),
        )
    }

    /** Offers a ready habit (from inside the app) to the next new-habit screen. */
    fun offer(habit: Habit) { pending = habit }

    /** The waiting template, handed out once. */
    fun take(): Habit? = pending.also { pending = null }
}
