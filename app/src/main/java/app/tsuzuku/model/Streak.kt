package app.tsuzuku.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Missed scheduled days per calendar week (Monday–Sunday) that don't break a streak. Forgetting
 * one day is the most common reason people abandon a habit tracker, so one slip a week is
 * forgiven. A forgiven day keeps the streak alive but doesn't add to it.
 */
const val REST_DAYS_PER_WEEK = 1

enum class StreakDay {
    /** Done (or within limits for a quit habit). */
    Kept,

    /** A past scheduled day that wasn't done (forgotten). The weekly rest day can cover it. */
    Missed,

    /** Marked failed, or a quit habit's limit was exceeded. Always ends the streak. */
    Broken,

    /** Today, not done yet: neither extends nor breaks the streak. */
    Pending,
}

data class StreakResult(
    val current: Int,
    val best: Int,
    /** The current streak survives only because this week's rest day was used. */
    val restUsedThisWeek: Boolean,
)

/**
 * Computes streaks over [days]: the habit's scheduled dates in ascending order, with skipped
 * days already removed (skips never count against a streak).
 */
fun streakOf(days: List<Pair<LocalDate, StreakDay>>, restPerWeek: Int = REST_DAYS_PER_WEEK): StreakResult {
    // Current: walk back from today so the most recent slip is the one that gets forgiven.
    var current = 0
    var restUsedThisWeek = false
    val thisWeek = days.lastOrNull()?.first?.weekStart()
    val usedBackward = mutableMapOf<LocalDate, Int>()
    for ((date, day) in days.asReversed()) {
        when (day) {
            StreakDay.Kept -> current++
            StreakDay.Pending -> Unit
            StreakDay.Broken -> break
            StreakDay.Missed -> {
                val week = date.weekStart()
                val used = usedBackward.getOrDefault(week, 0)
                if (used >= restPerWeek) break
                usedBackward[week] = used + 1
                if (week == thisWeek) restUsedThisWeek = true
            }
        }
    }

    var best = 0
    var run = 0
    val usedForward = mutableMapOf<LocalDate, Int>()
    for ((date, day) in days) {
        when (day) {
            StreakDay.Kept -> {
                run++
                best = maxOf(best, run)
            }
            StreakDay.Pending -> Unit
            StreakDay.Broken -> run = 0
            StreakDay.Missed -> {
                val week = date.weekStart()
                val used = usedForward.getOrDefault(week, 0)
                if (used < restPerWeek) usedForward[week] = used + 1 else run = 0
            }
        }
    }
    return StreakResult(
        current = current,
        best = maxOf(best, current),
        restUsedThisWeek = restUsedThisWeek && current > 0,
    )
}

private fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
