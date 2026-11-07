package app.tsuzuku.model

import java.time.LocalDate

/** How doing a habit felt, asked right after it is completed. */
enum class Feeling(val value: Int, val emoji: String) {
    Hard(1, "😣"),
    Okay(2, "😐"),
    Good(3, "😊");

    companion object {
        fun of(value: Int): Feeling? = entries.firstOrNull { it.value == value }
    }
}

data class HabitFeeling(val habitId: Long, val date: LocalDate, val feeling: Feeling)

/** Counts of each answer for one habit over a period. */
data class FeelingSummary(val hard: Int = 0, val okay: Int = 0, val good: Int = 0) {
    val total: Int get() = hard + okay + good

    /** Average on a 1–3 scale, or null with no answers. */
    val average: Double? get() = if (total == 0) null else (hard * 1.0 + okay * 2.0 + good * 3.0) / total

    /** The most common answer; ties lean towards the kinder one. */
    val usual: Feeling? get() = when {
        total == 0 -> null
        good >= okay && good >= hard -> Feeling.Good
        okay >= hard -> Feeling.Okay
        else -> Feeling.Hard
    }
}

fun HabitHistorySnapshot.feelingSummary(habitId: Long, since: LocalDate? = null): FeelingSummary =
    feelings.filter { it.habitId == habitId && (since == null || !it.date.isBefore(since)) }
        .fold(FeelingSummary()) { acc, item ->
            when (item.feeling) {
                Feeling.Hard -> acc.copy(hard = acc.hard + 1)
                Feeling.Okay -> acc.copy(okay = acc.okay + 1)
                Feeling.Good -> acc.copy(good = acc.good + 1)
            }
        }

/** Habits that felt best on average, among those answered at least [minAnswers] times. */
fun HabitHistorySnapshot.mostEnjoyed(since: LocalDate? = null, minAnswers: Int = 3, limit: Int = 3): List<Pair<Habit, FeelingSummary>> =
    habits.map { it to feelingSummary(it.id, since) }
        .filter { (_, summary) -> summary.total >= minAnswers }
        .sortedWith(compareByDescending<Pair<Habit, FeelingSummary>> { it.second.average }.thenByDescending { it.second.total })
        .take(limit)
