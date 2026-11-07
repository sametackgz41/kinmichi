package app.tsuzuku.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CoachingTest {
    private val today = LocalDate.of(2026, 11, 6)

    private fun habit(id: Long = 1, type: HabitType = HabitType.Count, target: Double = 8.0, step: Double = 1.0) = Habit(
        id = id, name = "Water$id", emoji = "💧", colorArgb = 0xFF3E8EC2, type = type,
        direction = HabitDirection.Build, target = target, unit = "glasses", step = step,
        scheduleDaysMask = 127, goalPeriod = GoalPeriod.Daily, sortOrder = id.toInt(),
        createdOn = today.minusDays(60),
    )

    /** amounts[0] is yesterday, amounts[1] the day before, ... */
    private fun history(habit: Habit, amounts: List<Double?>) = HabitHistorySnapshot(
        listOf(habit),
        amounts.mapIndexedNotNull { i, amount ->
            val date = today.minusDays(i + 1L)
            when (amount) {
                null -> HabitHistoryRecord(habit.id, date, 0.0, HabitDayStatus.Skipped)
                0.0 -> null
                else -> HabitHistoryRecord(habit.id, date, amount, if (amount >= habit.target) HabitDayStatus.Completed else HabitDayStatus.Pending)
            }
        },
    )

    // ---- target advice ---------------------------------------------------------------------

    @Test
    fun suggestsGrowingAfterThreeStrongWeeks() {
        val water = habit()
        val advice = targetAdvice(history(water, List(21) { 8.0 }), water, today)
        assertEquals(TargetAdvice.Kind.Grow, advice?.kind)
        assertEquals(10.0, advice!!.newTarget, 0.0)
    }

    /** Done on two days in each of the last three full weeks (Oct 12 – Nov 1). */
    private fun twicePerWeek(): List<Double?> = List(25) { i -> if (i in setOf(5, 7, 12, 14, 19, 21)) 1.0 else 0.0 }

    @Test
    fun suggestsAWeeklyGoalForARarelyDoneDailyHabit() {
        val stretch = habit(type = HabitType.Check, target = 1.0)
        val advice = frequencyAdvice(history(stretch, twicePerWeek()), stretch, today)
        assertEquals(TargetAdvice.Kind.Weekly, advice?.kind)
        assertEquals(2, advice!!.newTarget.toInt())
        assertTrue(advice.changesFrequency)
    }

    @Test
    fun adjustsAWeeklyGoalUpAndDown() {
        val met = habit(type = HabitType.Check, target = 1.0).copy(goalPeriod = GoalPeriod.Weekly, periodTarget = 2)
        val up = frequencyAdvice(history(met, twicePerWeek()), met, today)
        assertEquals(TargetAdvice.Kind.MorePerWeek, up?.kind)
        assertEquals(3, up!!.newTarget.toInt())

        val tooHigh = met.copy(periodTarget = 5)
        val down = frequencyAdvice(history(tooHigh, twicePerWeek()), tooHigh, today)
        assertEquals(TargetAdvice.Kind.FewerPerWeek, down?.kind)
        assertEquals(2, down!!.newTarget.toInt())
    }

    @Test
    fun noFrequencyAdviceForAHabitDoneMostDays() {
        val stretch = habit(type = HabitType.Check, target = 1.0)
        assertNull(frequencyAdvice(history(stretch, List(25) { 1.0 }), stretch, today))
    }


    @Test
    fun suggestsTheAmountActuallyReached() {
        val h = habit()
        val amounts = listOf(5.0, 4.0, 6.0, 5.0, 8.0, 5.0, 3.0, 5.0, 6.0, 4.0, 5.0, 0.0, 5.0, 4.0)
        val advice = targetAdvice(history(h, amounts), h, today)!!
        assertEquals(TargetAdvice.Kind.Lower, advice.kind)
        assertEquals(5.0, advice.newTarget, 0.0)
        assertEquals(1, advice.hits)
        assertEquals(14, advice.days)
    }

    @Test
    fun noAdviceWhenTheTargetIsUsuallyMet() {
        val h = habit()
        assertNull(targetAdvice(history(h, List(14) { 8.0 }), h, today))
    }

    @Test
    fun noAdviceWhenNothingIsLogged() {
        // Forgetting to log isn't a sign the target is too big.
        val h = habit()
        assertNull(targetAdvice(history(h, List(14) { 0.0 }), h, today))
    }

    @Test
    fun durationsRoundDownToTheStep() {
        val h = habit(type = HabitType.Duration, target = 30.0, step = 5.0)
        val amounts = listOf(12.0, 14.0, 13.0, 15.0, 11.0, 14.0, 12.0, 13.0, 16.0, 12.0, 14.0, 13.0)
        assertEquals(10.0, targetAdvice(history(h, amounts), h, today)!!.newTarget, 0.0)
    }

    @Test
    fun suggestsStepsBackUpTowardTheOriginal() {
        val h = habit(target = 5.0)
        val advice = targetAdvice(history(h, List(14) { 5.0 }), h, today, originalTarget = 8.0)!!
        assertEquals(TargetAdvice.Kind.Raise, advice.kind)
        assertEquals(7.0, advice.newTarget, 0.0) // 5 + max(1, ceil(1.25)) = 7, capped at 8
    }

    @Test
    fun skippedDaysAndCheckHabitsAreIgnored() {
        val h = habit()
        assertNull(targetAdvice(history(h, List(14) { null }), h, today))
        val check = habit(type = HabitType.Check, target = 1.0)
        assertNull(targetAdvice(history(check, List(14) { 0.0 }), check, today))
    }

    // ---- mood insights ---------------------------------------------------------------------

    @Test
    fun findsHabitsThatLineUpWithABetterMood() {
        val walk = habit(id = 1, type = HabitType.Check, target = 1.0)
        val water = habit(id = 2, type = HabitType.Check, target = 1.0)
        val records = mutableListOf<HabitHistoryRecord>()
        val moods = mutableMapOf<LocalDate, Int>()
        for (back in 1L..20L) {
            val date = today.minusDays(back)
            val walked = back % 2 == 0L
            if (walked) records += HabitHistoryRecord(walk.id, date, 1.0, HabitDayStatus.Completed)
            // Water is done every third day regardless of mood.
            if (back % 3 == 0L) records += HabitHistoryRecord(water.id, date, 1.0, HabitDayStatus.Completed)
            moods[date] = if (walked) 4 else 2
        }
        val result = moodInsights(HabitHistorySnapshot(listOf(walk, water), records), moods, today)
        assertEquals(20, result.loggedDays)
        assertEquals(1, result.insights.size)
        val insight = result.insights.single()
        assertEquals(walk.id, insight.habit.id)
        assertEquals(4.0, insight.withHabit, 0.0)
        assertEquals(2.0, insight.withoutHabit, 0.0)
    }

    @Test
    fun needsEnoughLoggedDays() {
        val walk = habit(type = HabitType.Check, target = 1.0)
        val moods = (1L..6L).associate { today.minusDays(it) to 4 }
        val result = moodInsights(HabitHistorySnapshot(listOf(walk), emptyList()), moods, today)
        assertTrue(result.insights.isEmpty())
        assertEquals(MOOD_MIN_DAYS - 6, result.daysUntilReady)
    }
}
