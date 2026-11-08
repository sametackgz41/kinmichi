package app.tsuzuku.model

import java.time.LocalDate

enum class HabitType {
    Check,
    Count,
    Duration,
}

enum class HabitDirection {
    Build,
    Quit,
}

enum class GoalPeriod {
    Daily,
    Weekly,
    Monthly,
    /** X times in every block of [Habit.periodDays] days, counted from the habit's start. */
    Custom,
}

enum class HealthMetric {
    None,
    Steps,
    Distance,
    Calories,
    ExerciseDuration,
    SleepDuration,
}

enum class HabitDayStatus {
    Pending,
    Completed,
    Failed,
    Skipped,
}

data class Habit(
    val id: Long,
    val name: String,
    val emoji: String,
    val colorArgb: Long,
    val type: HabitType,
    val direction: HabitDirection,
    val target: Double,
    val unit: String,
    val step: Double,
    val scheduleDaysMask: Int,
    val goalPeriod: GoalPeriod = GoalPeriod.Daily,
    val periodTarget: Int = 1,
    val periodDays: Int = 10,
    val routineName: String = "",
    val prerequisiteHabitId: Long? = null,
    val healthMetric: HealthMetric = HealthMetric.None,
    val sortOrder: Int,
    val reminderMinutes: Int? = null,
    val reminderRepeatMinutes: Int? = null,
    val generalNote: String = "",
    val createdOn: LocalDate = LocalDate.MIN,
    val minimumTarget: Double = 0.0,
    val maximumTarget: Double? = null,
    val difficulty: Int = 2,
    val contextLabel: String = "",
    val intention: String = "",
    val activeStartEpochDay: Long? = null,
    val activeEndEpochDay: Long? = null,
    val repeatIntervalDays: Int = 1,
    val pausedUntilEpochDay: Long? = null,
    val tags: String = "",
    val currentLevel: Int = 1,
    /** What still counts on a bad day, e.g. "1 page"; keeps the streak alive. */
    val tinyVersion: String = "",
    val slots: List<HabitSlot> = emptyList(),
    val journeyTotal: Double? = null,
    val journeyStart: Double = 0.0,
    /** Earlier targets, oldest last: each says what the target was before [TargetChange.changedOn]. */
    val targetHistory: List<TargetChange> = emptyList(),
)

data class HabitLevel(
    val id: Long = 0,
    val habitId: Long,
    val levelNumber: Int,
    val name: String,
    val target: Double,
    val minimumTarget: Double = 0.0,
    val maximumTarget: Double? = null,
    val rewardNote: String = "",
    val createdAt: Long = 0L,
)

data class TodayHabit(
    val habit: Habit,
    val progress: Double,
    /** Which of the habit's times of day were ticked by name today. */
    val doneSlots: Set<Int> = emptySet(),
    /** For a habit with a whole to get through: where it stands now (page 184). */
    val journeyPosition: Double = 0.0,
    /** Average gain per day lately, for the finish-date guess. */
    val journeyPace: Double = 0.0,
    val status: HabitDayStatus = HabitDayStatus.Pending,
    val streakDays: Int = 0,
    val restUsedThisWeek: Boolean = false,
    val periodProgress: Int = 0,
    val isBlocked: Boolean = false,
    val blockedByName: String? = null,
    val skipReason: String? = null,
    val hasLevels: Boolean = false,
    val currentLevelNumber: Int = 1,
    val currentLevelName: String? = null,
) {
    val isMinimumComplete: Boolean
        get() = habit.minimumTarget > 0.0 && progress >= habit.minimumTarget
    val isComplete: Boolean
        get() = if (habit.direction == HabitDirection.Quit) {
            status == HabitDayStatus.Completed || (habit.maximumTarget != null && progress <= habit.maximumTarget && status != HabitDayStatus.Failed)
        } else {
            status == HabitDayStatus.Completed || progress >= habit.target ||
                (habit.goalPeriod != GoalPeriod.Daily && periodProgress >= habit.periodTarget)
        }

    val progressFraction: Float
        get() = if (habit.target <= 0.0) 0f else (progress / habit.target).coerceIn(0.0, 1.0).toFloat()

    val masteryStage: HabitMasteryStage
        get() = HabitMasteryStage.fromDays(streakDays)

    val badHabitLimitExceeded: Boolean
        get() = habit.direction == HabitDirection.Quit && habit.maximumTarget != null && progress > habit.maximumTarget

    val badHabitLimitWarning: Boolean
        get() = habit.direction == HabitDirection.Quit && habit.maximumTarget != null && progress >= (habit.maximumTarget * 0.8) && !badHabitLimitExceeded

    val isIdealRange: Boolean
        get() = if (habit.direction == HabitDirection.Quit) {
            val max = habit.maximumTarget ?: habit.target
            progress <= max && (habit.minimumTarget <= 0.0 || progress >= habit.minimumTarget)
        } else {
            progress >= habit.target
        }
}


enum class HabitMasteryStage(val icon: String, val minDays: Int, val maxDays: Int) {
    Seed("🌱", 1, 7),
    Sprout("🌿", 8, 30),
    Habit("🌳", 31, 90),
    Mastery("🗻", 91, Int.MAX_VALUE);

    companion object {
        fun fromDays(days: Int): HabitMasteryStage = when {
            days <= 7 -> Seed
            days <= 30 -> Sprout
            days <= 90 -> Habit
            else -> Mastery
        }
    }
}

data class HabitDraft(
    val name: String,
    val emoji: String,
    val colorArgb: Long,
    val type: HabitType,
    val direction: HabitDirection,
    val target: Double,
    val unit: String,
    val step: Double,
    val scheduleDaysMask: Int,
    val goalPeriod: GoalPeriod = GoalPeriod.Daily,
    val periodTarget: Int = 1,
    val periodDays: Int = 10,
    val routineName: String = "",
    val prerequisiteHabitId: Long? = null,
    val healthMetric: HealthMetric = HealthMetric.None,
    val reminderMinutes: Int? = null,
    val reminderRepeatMinutes: Int? = null,
    val generalNote: String = "",
    val minimumTarget: Double = 0.0,
    val maximumTarget: Double? = null,
    val difficulty: Int = 2,
    val contextLabel: String = "",
    val intention: String = "",
    val activeStartEpochDay: Long? = null,
    val activeEndEpochDay: Long? = null,
    val repeatIntervalDays: Int = 1,
    val pausedUntilEpochDay: Long? = null,
    val tags: String = "",
    val currentLevel: Int = 1,
    /** What still counts on a bad day, e.g. "1 page"; keeps the streak alive. */
    val tinyVersion: String = "",
    /** Times of the day in the stored form, see [HabitSlot.format]. */
    val slots: String = "",
    val journeyTotal: Double? = null,
    val journeyStart: Double = 0.0,
    /** Ring like an alarm clock instead of a quiet notification (kept per device). */
    val alarm: Boolean = false,
    /** Name of an app.tsuzuku.notifications.AlarmMission. */
    val alarmMission: String = "None",
    val alarmMarkDone: Boolean = false,
    /** Spread-out reminders through the day (count habits); null start = off. */
    val spreadStartMinutes: Int? = null,
    val spreadEndMinutes: Int = 21 * 60,
    /** Filled automatically from today's screen time. */
    val screenTime: Boolean = false,
    /** Made as the "Alarm" type: these tasks (names of AlarmTask), level 0-2, rising sound, snoozes. */
    val alarmType: Boolean = false,
    val alarmTasks: String = "",
    val alarmLevel: Int = 1,
    val alarmRamp: Boolean = true,
    val alarmSnoozes: Int = 2,
)

data class WeeklyReview(
    val weekStart: LocalDate,
    val note: String,
    val updatedAt: Long,
)


/** One time of the day for a habit done several times a day ("Morning, 08.00"). */
data class HabitSlot(val name: String, val minutes: Int) {
    companion object {
        const val MAX = 6
        fun parse(stored: String?): List<HabitSlot> = stored.orEmpty().split('|').mapNotNull { part ->
            val at = part.lastIndexOf('@')
            if (at <= 0) return@mapNotNull null
            val minutes = part.substring(at + 1).toIntOrNull() ?: return@mapNotNull null
            HabitSlot(part.substring(0, at).replace('|', ' ').replace('@', ' ').trim(), minutes.coerceIn(0, 24 * 60 - 1))
        }.take(MAX)

        fun format(slots: List<HabitSlot>): String =
            slots.take(MAX).joinToString("|") { "${it.name.replace('|', ' ').replace('@', ' ').trim()}@${it.minutes}" }

        /** The note on an entry that ticks the slot at [index]. */
        fun note(index: Int) = "SLOT:$index"
        fun indexOf(note: String?): Int? = note?.takeIf { it.startsWith("SLOT:") }?.removePrefix("SLOT:")?.toIntOrNull()
    }
}

/**
 * Which slots count as done: the ones ticked by name, then any other progress (from a
 * notification or a widget) fills the earliest open ones.
 */
fun TodayHabit.slotStates(): List<Boolean> {
    val slots = habit.slots
    val named = doneSlots.filter { it in slots.indices }.toSet()
    var extra = (progress.toInt() - named.size).coerceAtLeast(0)
    return slots.indices.map { i ->
        when {
            i in named -> true
            extra > 0 -> { extra--; true }
            else -> false
        }
    }
}


/** First and last day of the goal period that [date] falls in. */
fun Habit.periodBounds(date: java.time.LocalDate): Pair<java.time.LocalDate, java.time.LocalDate> = when (goalPeriod) {
    GoalPeriod.Daily -> date to date
    GoalPeriod.Weekly -> date.minusDays((date.dayOfWeek.value - 1).toLong()) to date.plusDays((7 - date.dayOfWeek.value).toLong())
    GoalPeriod.Monthly -> date.withDayOfMonth(1) to date.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
    GoalPeriod.Custom -> {
        val anchor = activeStartEpochDay?.let(java.time.LocalDate::ofEpochDay) ?: createdOn.takeIf { it != java.time.LocalDate.MIN } ?: date
        val length = periodDays.coerceIn(2, 90).toLong()
        val offset = Math.floorMod(java.time.temporal.ChronoUnit.DAYS.between(anchor, date), length)
        val start = date.minusDays(offset)
        start to start.plusDays(length - 1)
    }
}


/** The target was [oldTarget] until the day before [changedOn]. */
data class TargetChange(val changedOn: java.time.LocalDate, val oldTarget: Double) {
    companion object {
        fun parse(stored: String?): List<TargetChange> = stored.orEmpty().split('|').mapNotNull { part ->
            val (day, value) = part.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            val date = runCatching { java.time.LocalDate.parse(day) }.getOrNull() ?: return@mapNotNull null
            TargetChange(date, value.toDoubleOrNull() ?: return@mapNotNull null)
        }.sortedBy { it.changedOn }

        fun format(list: List<TargetChange>): String = list.sortedBy { it.changedOn }.joinToString("|") { "${it.changedOn}:${it.oldTarget}" }

        /** Records a change made today; a second change on the same day keeps the first old value. */
        fun record(stored: String, oldTarget: Double, today: java.time.LocalDate): String {
            val list = parse(stored)
            if (list.any { it.changedOn == today }) return stored
            return format(list + TargetChange(today, oldTarget))
        }
    }
}

/** The target that applied on [date]: past days keep the target they had then. */
fun Habit.targetOn(date: java.time.LocalDate): Double =
    targetHistory.firstOrNull { it.changedOn.isAfter(date) }?.oldTarget ?: target
