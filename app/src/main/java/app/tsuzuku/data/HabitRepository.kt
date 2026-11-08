package app.tsuzuku.data

import app.tsuzuku.data.local.HabitDao
import app.tsuzuku.data.local.HabitEntity
import app.tsuzuku.data.local.HabitEntryEntity
import app.tsuzuku.data.local.HabitLevelEntity
import app.tsuzuku.data.local.WeeklyReviewEntity
import app.tsuzuku.model.GoalPeriod
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitHistoryRecord
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.HabitDayStatus
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.HabitDraft
import app.tsuzuku.model.HabitLevel
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.HealthMetric
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.model.slotStates
import app.tsuzuku.model.WeeklyReview
import app.tsuzuku.model.isScheduledOn
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface HabitRepository {
    fun observeToday(): Flow<List<TodayHabit>>
    fun observeHistory(): Flow<HabitHistorySnapshot>
    suspend fun toggleHabit(id: Long)
    /** Saves how doing a habit felt on [date]; null clears it. */
    suspend fun setFeeling(habitId: Long, date: LocalDate, feeling: app.tsuzuku.model.Feeling?)
    suspend fun incrementHabit(id: Long)
    suspend fun decrementHabit(id: Long)
    suspend fun toggleHabitOnDate(id: Long, date: LocalDate)
    suspend fun incrementHabitOnDate(id: Long, date: LocalDate)
    suspend fun decrementHabitOnDate(id: Long, date: LocalDate)
    suspend fun addProgress(id: Long, amount: Double)
    /** Ticks or unticks one time of the day of a habit done several times a day. */
    suspend fun toggleSlot(id: Long, index: Int) {}
    suspend fun addHabit(draft: HabitDraft): Long
    suspend fun updateHabit(id: Long, draft: HabitDraft)
    suspend fun updateHabitNote(id: Long, note: String)
    suspend fun setHabitTarget(id: Long, target: Double)
    /** Switches to (or adjusts) a weekly goal of [timesPerWeek], on any day of the week. */
    suspend fun setWeeklyGoal(id: Long, timesPerWeek: Int) {}
    suspend fun archiveHabit(id: Long)
    /** Habits put away with their history kept, newest first. */
    fun observeArchivedHabits(): Flow<List<Habit>> = kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun unarchiveHabit(id: Long) {}
    suspend fun deleteHabit(id: Long)
    /** Moves a habit to the trash; it stays there, restorable, for [TRASH_DAYS] days. */
    suspend fun trashHabit(id: Long) {}
    fun observeTrash(): Flow<List<TrashedHabit>> = kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun emptyTrash() {}
    /** Deletes for good what has been in the trash longer than [TRASH_DAYS] days. */
    suspend fun purgeOldTrash() {}
    suspend fun duplicateHabit(id: Long): Long?
    suspend fun moveHabit(id: Long, direction: Int)
    /** Puts [orderedIds] in this order; habits not listed (not on today's list) keep their slots. */
    suspend fun reorderHabits(orderedIds: List<Long>) {}
    fun observeWeeklyReviews(): Flow<List<WeeklyReview>>
    suspend fun saveWeeklyReview(weekStart: LocalDate, note: String)
    suspend fun applyVacation(startDate: LocalDate, endDate: LocalDate)
    suspend fun clearVacation(startDate: LocalDate, endDate: LocalDate)
    suspend fun syncHealthSteps(steps: Long)
    /** Health metrics some active habit is linked to. */
    suspend fun healthMetricsInUse(): Set<HealthMetric> = emptySet()
    /** Replaces today's health reading for every habit linked to [metric]; [valueFor] converts to the habit's unit. */
    suspend fun syncHealth(metric: HealthMetric, valueFor: (unit: String) -> Double) {}
    /** Replaces today's automatic reading for one habit (e.g. screen time); hand entries stay. */
    suspend fun setAutoValue(habitId: Long, value: Double) {}
    /** Marks today done with the habit's bad-day version (its minimum, or a plain completion). */
    suspend fun completeTinyVersion(id: Long)
    /** Minute of the day each habit was usually done on, over the last [days] days. */
    suspend fun completionMinutes(days: Int): Map<Long, List<Int>> = emptyMap()
    suspend fun setReminderMinutes(id: Long, minutes: Int) {}
    suspend fun skipHabit(id: Long, reason: String?)
    suspend fun skipHabitOnDate(id: Long, date: LocalDate, reason: String?)
    fun observeHabitLevels(habitId: Long): Flow<List<HabitLevel>>
    suspend fun getHabitLevels(habitId: Long): List<HabitLevel>
    suspend fun saveHabitLevels(habitId: Long, levels: List<HabitLevel>)
    suspend fun advanceHabitLevel(habitId: Long, targetLevelNumber: Int)
}

class RoomHabitRepository(
    private val dao: HabitDao,
    private val today: () -> LocalDate = LocalDate::now,
    private val now: () -> Long = System::currentTimeMillis,
) : HabitRepository {
    override fun observeToday(): Flow<List<TodayHabit>> {
        val localDate = today().toString()
        return combine(
            dao.observeActiveHabits(),
            dao.observeAllEntries(),
        ) { habits, entries ->
            val date = today()
            val entriesByHabit = entries.groupBy(HabitEntryEntity::habitId)
            val mapped = habits.filter { it.scheduleDaysMask and (1 shl (date.dayOfWeek.value - 1)) != 0 }.map { entity ->
                val allHabitEntries = entriesByHabit[entity.id].orEmpty()
                val habitEntries = allHabitEntries.filter { it.localDate == localDate }
                val skippedEntry = habitEntries.firstOrNull { it.status == HabitEntryEntity.STATUS_SKIPPED }
                val status = when {
                    skippedEntry != null -> HabitDayStatus.Skipped
                    habitEntries.any { it.status == HabitEntryEntity.STATUS_FAILED } -> HabitDayStatus.Failed
                    habitEntries.any { it.status == HabitEntryEntity.STATUS_COMPLETED } -> HabitDayStatus.Completed
                    else -> HabitDayStatus.Pending
                }
                TodayHabit(
                    habit = entity.toDomain(),
                    doneSlots = habitEntries.mapNotNull { app.tsuzuku.model.HabitSlot.indexOf(it.note) }.toSet(),
                    progress = habitEntries
                        .filter(HabitEntryEntity::isProgress)
                        .sumOf(HabitEntryEntity::amount),
                    status = status,
                    periodProgress = periodCompletionCount(entity, allHabitEntries, date),
                    skipReason = skippedEntry?.note,
                    hasLevels = false,
                    currentLevelNumber = entity.currentLevel,
                )
            }
            val entitiesById = habits.associateBy(HabitEntity::id)
            val namesById = habits.associate { it.id to it.name }
            val history = HabitHistorySnapshot(habits.map(HabitEntity::toDomain), entries.toHistoryRecords())
            mapped.map { item ->
                val streak = history.streak(item.habit, date)
                val prerequisiteId = item.habit.prerequisiteHabitId
                val blocked = prerequisiteId?.let { id ->
                    entitiesById[id]?.let { prerequisite ->
                        val prerequisiteEntries = entriesByHabit[id].orEmpty()
                        val todayEntries = prerequisiteEntries.filter { it.localDate == localDate }
                        val dayComplete = todayEntries.any { it.status == HabitEntryEntity.STATUS_COMPLETED } ||
                            todayEntries.filter(HabitEntryEntity::isProgress).sumOf(HabitEntryEntity::amount) >= prerequisite.target
                        val periodComplete = periodCompletionCount(prerequisite, prerequisiteEntries, date) >= prerequisite.periodTarget
                        !(dayComplete || (prerequisite.goalPeriod != GoalPeriod.Daily.name && periodComplete))
                    } ?: false
                } ?: false
                item.copy(
                    isBlocked = blocked,
                    blockedByName = if (blocked) namesById[prerequisiteId] else null,
                    streakDays = streak.current,
                    restUsedThisWeek = streak.restUsedThisWeek,
                )
            }
        }
    }

    override fun observeHistory(): Flow<HabitHistorySnapshot> = combine(
        dao.observeActiveHabits(),
        dao.observeAllEntries(),
        dao.observeFeelings(),
    ) { habits, entries, feelings ->
        HabitHistorySnapshot(
            habits.map(HabitEntity::toDomain),
            entries.toHistoryRecords(),
            feelings.mapNotNull { item ->
                val feeling = app.tsuzuku.model.Feeling.of(item.feeling) ?: return@mapNotNull null
                val date = runCatching { LocalDate.parse(item.localDate) }.getOrNull() ?: return@mapNotNull null
                app.tsuzuku.model.HabitFeeling(item.habitId, date, feeling)
            },
        )
    }

    override suspend fun setFeeling(habitId: Long, date: LocalDate, feeling: app.tsuzuku.model.Feeling?) {
        if (feeling == null) {
            dao.deleteFeeling(habitId, date.toString())
        } else {
            dao.upsertFeeling(app.tsuzuku.data.local.HabitFeelingEntity(habitId, date.toString(), feeling.value, now()))
        }
    }

    override suspend fun toggleHabit(id: Long) = toggleHabitOnDate(id, today())

    override suspend fun toggleHabitOnDate(id: Long, date: LocalDate) {
        val habit = dao.getHabit(id) ?: return
        if (!prerequisiteComplete(habit, date)) return
        val localDate = date.toString()
        val current = dao.getEntries(id, localDate)
        val isComplete = current.any { it.status == HabitEntryEntity.STATUS_COMPLETED } ||
            current.sumOf { if (it.isProgress()) it.amount else 0.0 } >= habit.target

        if (!isComplete && periodGoalReached(habit, date)) return

        if (isComplete) {
            dao.clearEntries(id, localDate)
        } else {
            val timestamp = now()
            dao.replaceDayWithEntry(
                habitId = id,
                localDate = localDate,
                entry = HabitEntryEntity(
                    habitId = id,
                    localDate = localDate,
                    amount = habit.target,
                    status = HabitEntryEntity.STATUS_COMPLETED,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                ),
            )
        }
    }

    override suspend fun incrementHabit(id: Long) = incrementHabitOnDate(id, today())

    override suspend fun incrementHabitOnDate(id: Long, date: LocalDate) {
        val habit = dao.getHabit(id) ?: return
        if (!prerequisiteComplete(habit, date) || periodGoalReached(habit, date)) return
        val current = dao.getEntries(id, date.toString()).filter(HabitEntryEntity::isProgress).sumOf(HabitEntryEntity::amount)
        val amount = habit.maximumTarget?.let { (it - current).coerceAtMost(habit.step) } ?: habit.step
        if (amount <= 0.0) return
        val timestamp = now()
        dao.insertEntry(
            HabitEntryEntity(
                habitId = id,
                localDate = date.toString(),
                amount = amount,
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    override suspend fun decrementHabit(id: Long) = decrementHabitOnDate(id, today())

    override suspend fun decrementHabitOnDate(id: Long, date: LocalDate) {
        dao.deleteLatestProgress(id, date.toString())
    }

    override suspend fun completionMinutes(days: Int): Map<Long, List<Int>> {
        val zone = ZoneId.systemDefault()
        return dao.completionTimes(today().minusDays(days.toLong()).toString())
            .groupBy({ it.habitId }) { Instant.ofEpochMilli(it.at).atZone(zone).toLocalTime().let { time -> time.hour * 60 + time.minute } }
    }

    override suspend fun setReminderMinutes(id: Long, minutes: Int) {
        dao.updateReminderMinutes(id, minutes.coerceIn(0, 24 * 60 - 1), now())
    }

    override suspend fun completeTinyVersion(id: Long) {
        val habit = dao.getHabit(id) ?: return
        val date = today()
        if (!prerequisiteComplete(habit, date)) return
        val localDate = date.toString()
        val current = dao.getEntries(id, localDate)
        if (current.any { it.status == HabitEntryEntity.STATUS_COMPLETED }) return
        val progress = current.filter(HabitEntryEntity::isProgress).sumOf(HabitEntryEntity::amount)
        val timestamp = now()
        if (habit.type != HabitType.Check.name && habit.minimumTarget > 0.0) {
            if (progress >= habit.minimumTarget) return
            dao.insertEntry(
                HabitEntryEntity(habitId = id, localDate = localDate, amount = habit.minimumTarget - progress, note = HabitEntryEntity.NOTE_TINY, createdAt = timestamp, updatedAt = timestamp),
            )
        } else {
            dao.replaceDayWithEntry(
                habitId = id,
                localDate = localDate,
                entry = HabitEntryEntity(habitId = id, localDate = localDate, amount = habit.target, status = HabitEntryEntity.STATUS_COMPLETED, note = HabitEntryEntity.NOTE_TINY, createdAt = timestamp, updatedAt = timestamp),
            )
        }
    }

    override suspend fun addProgress(id: Long, amount: Double) {
        if (amount <= 0.0) return
        val habit = dao.getHabit(id) ?: return
        if (!prerequisiteComplete(habit, today()) || periodGoalReached(habit, today())) return
        val current = dao.getEntries(id, today().toString()).filter(HabitEntryEntity::isProgress).sumOf(HabitEntryEntity::amount)
        val acceptedAmount = habit.maximumTarget?.let { (it - current).coerceAtMost(amount) } ?: amount
        if (acceptedAmount <= 0.0) return
        val timestamp = now()
        dao.insertEntry(
            HabitEntryEntity(
                habitId = id,
                localDate = today().toString(),
                amount = acceptedAmount,
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    override suspend fun toggleSlot(id: Long, index: Int) {
        val habit = dao.getHabit(id) ?: return
        val date = today().toString()
        val entries = dao.getEntries(id, date)
        val today = TodayHabit(
            habit = habit.toDomain(),
            progress = entries.filter(HabitEntryEntity::isProgress).sumOf(HabitEntryEntity::amount),
            doneSlots = entries.mapNotNull { app.tsuzuku.model.HabitSlot.indexOf(it.note) }.toSet(),
        )
        val states = today.slotStates()
        if (index !in states.indices) return
        val note = app.tsuzuku.model.HabitSlot.note(index)
        when {
            index in today.doneSlots -> dao.deleteEntriesWithNote(id, date, note)
            states[index] -> dao.deleteLatestUntaggedProgress(id, date)
            else -> {
                val timestamp = now()
                dao.insertEntry(HabitEntryEntity(habitId = id, localDate = date, amount = 1.0, note = note, createdAt = timestamp, updatedAt = timestamp))
            }
        }
    }

    override suspend fun addHabit(draft: HabitDraft): Long {
        val timestamp = now()
        val sortOrder = dao.countHabits()
        return dao.insertHabit(
            HabitEntity(
                name = draft.name.trim(),
                emoji = draft.emoji,
                colorArgb = draft.colorArgb,
                type = draft.type.name,
                direction = draft.direction.name,
                target = draft.target,
                unit = draft.unit.trim(),
                step = draft.step,
                scheduleDaysMask = draft.scheduleDaysMask,
                goalPeriod = draft.goalPeriod.name,
                periodTarget = draft.periodTarget.coerceAtLeast(1),
                routineName = draft.routineName.trim().take(40),
                prerequisiteHabitId = draft.prerequisiteHabitId,
                healthMetric = draft.healthMetric.name,
                minimumTarget = if (draft.direction == HabitDirection.Quit) draft.minimumTarget.coerceAtLeast(0.0) else draft.minimumTarget.coerceIn(0.0, draft.target),
                maximumTarget = if (draft.direction == HabitDirection.Quit) draft.maximumTarget else draft.maximumTarget?.takeIf { it >= draft.target },
                difficulty = draft.difficulty.coerceIn(1, 3),
                contextLabel = draft.contextLabel.trim().take(40),
                intention = draft.intention.trim().take(240),
                tinyVersion = draft.tinyVersion.trim().take(60),
                slots = draft.slots,
                activeStartEpochDay = draft.activeStartEpochDay,
                activeEndEpochDay = draft.activeEndEpochDay,
                repeatIntervalDays = draft.repeatIntervalDays.coerceIn(1, 365),
                pausedUntilEpochDay = draft.pausedUntilEpochDay,
                tags = normalizeHabitTags(draft.tags),
                currentLevel = draft.currentLevel,
                sortOrder = sortOrder,
                reminderMinutes = draft.reminderMinutes,
                reminderRepeatMinutes = draft.reminderRepeatMinutes,
                generalNote = draft.generalNote.trim(),
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    override suspend fun updateHabit(id: Long, draft: HabitDraft) {
        val current = dao.getHabit(id) ?: return
        dao.insertHabit(
            current.copy(
                name = draft.name.trim(),
                emoji = draft.emoji,
                colorArgb = draft.colorArgb,
                type = draft.type.name,
                direction = draft.direction.name,
                target = draft.target,
                unit = draft.unit.trim(),
                step = draft.step,
                scheduleDaysMask = draft.scheduleDaysMask,
                goalPeriod = draft.goalPeriod.name,
                periodTarget = draft.periodTarget.coerceAtLeast(1),
                routineName = draft.routineName.trim().take(40),
                prerequisiteHabitId = draft.prerequisiteHabitId?.takeUnless { it == id },
                healthMetric = draft.healthMetric.name,
                minimumTarget = if (draft.direction == HabitDirection.Quit) draft.minimumTarget.coerceAtLeast(0.0) else draft.minimumTarget.coerceIn(0.0, draft.target),
                maximumTarget = if (draft.direction == HabitDirection.Quit) draft.maximumTarget else draft.maximumTarget?.takeIf { it >= draft.target },
                difficulty = draft.difficulty.coerceIn(1, 3),
                contextLabel = draft.contextLabel.trim().take(40),
                intention = draft.intention.trim().take(240),
                tinyVersion = draft.tinyVersion.trim().take(60),
                slots = draft.slots,
                activeStartEpochDay = draft.activeStartEpochDay,
                activeEndEpochDay = draft.activeEndEpochDay,
                repeatIntervalDays = draft.repeatIntervalDays.coerceIn(1, 365),
                pausedUntilEpochDay = draft.pausedUntilEpochDay,
                tags = normalizeHabitTags(draft.tags),
                currentLevel = draft.currentLevel,
                reminderMinutes = draft.reminderMinutes,
                reminderRepeatMinutes = draft.reminderRepeatMinutes,
                generalNote = draft.generalNote.trim(),
                updatedAt = now(),
            ),
        )
    }

    override suspend fun setHabitTarget(id: Long, target: Double) {
        val current = dao.getHabit(id) ?: return
        dao.insertHabit(
            current.copy(
                target = target,
                minimumTarget = current.minimumTarget.coerceAtMost(target),
                updatedAt = now(),
            ),
        )
    }

    override suspend fun setWeeklyGoal(id: Long, timesPerWeek: Int) {
        val current = dao.getHabit(id) ?: return
        dao.insertHabit(
            current.copy(
                goalPeriod = GoalPeriod.Weekly.name,
                periodTarget = timesPerWeek.coerceIn(1, 7),
                scheduleDaysMask = 127,
                repeatIntervalDays = 1,
                updatedAt = now(),
            ),
        )
    }

    override suspend fun updateHabitNote(id: Long, note: String) {
        val current = dao.getHabit(id) ?: return
        dao.insertHabit(current.copy(generalNote = note.trim(), updatedAt = now()))
    }

    override suspend fun archiveHabit(id: Long) = dao.archiveHabit(id, now())

    override fun observeArchivedHabits(): Flow<List<Habit>> = dao.observeArchivedHabits().map { list -> list.map { it.toDomain() } }

    override suspend fun unarchiveHabit(id: Long) = dao.unarchiveHabit(id, now())

    override suspend fun deleteHabit(id: Long) = dao.deleteHabit(id)

    override suspend fun trashHabit(id: Long) = dao.trashHabit(id, now())

    override fun observeTrash(): Flow<List<TrashedHabit>> =
        dao.observeTrashedHabits().map { list -> list.map { TrashedHabit(it.toDomain(), it.deletedAt ?: it.updatedAt) } }

    override suspend fun emptyTrash() = dao.emptyTrash()

    override suspend fun purgeOldTrash() = dao.purgeTrash(now() - TRASH_DAYS * 86_400_000L)

    override suspend fun duplicateHabit(id: Long): Long? {
        val source = dao.getHabit(id) ?: return null
        val timestamp = now()
        val newId = dao.insertHabit(source.copy(
            id = 0,
            name = "${source.name} · 2",
            sortOrder = dao.countHabits(),
            createdAt = timestamp,
            updatedAt = timestamp,
        ))
        val levels = dao.getHabitLevels(id)
        if (levels.isNotEmpty()) {
            dao.insertHabitLevels(levels.map { it.copy(id = 0, habitId = newId, createdAt = timestamp) })
        }
        return newId
    }

    override suspend fun moveHabit(id: Long, direction: Int) {
        val habits = dao.getActiveHabits()
        val index = habits.indexOfFirst { it.id == id }
        if (index < 0) return
        val target = (index + direction).coerceIn(habits.indices)
        if (target == index) return
        val reordered = habits.toMutableList().apply { add(target, removeAt(index)) }
        val timestamp = now()
        reordered.forEachIndexed { order, habit -> dao.updateSortOrder(habit.id, order, timestamp) }
    }

    override suspend fun reorderHabits(orderedIds: List<Long>) {
        val habits = dao.getActiveHabits()
        val wanted = orderedIds.toSet()
        val queue = ArrayDeque(orderedIds.filter { id -> habits.any { it.id == id } })
        // Fill the listed habits' slots with the new order; the rest stay where they are.
        val result = habits.map { habit -> if (habit.id in wanted && queue.isNotEmpty()) queue.removeFirst() else habit.id }
        val timestamp = now()
        result.forEachIndexed { order, id -> dao.updateSortOrder(id, order, timestamp) }
    }

    override fun observeWeeklyReviews(): Flow<List<WeeklyReview>> = dao.observeWeeklyReviews().map { reviews ->
        reviews.map { WeeklyReview(LocalDate.ofEpochDay(it.weekStartEpochDay), it.note, it.updatedAt) }
    }

    override suspend fun saveWeeklyReview(weekStart: LocalDate, note: String) {
        val timestamp = now()
        dao.upsertWeeklyReview(WeeklyReviewEntity(weekStart.toEpochDay(), note.trim().take(2_000), timestamp, timestamp))
    }

    override suspend fun applyVacation(startDate: LocalDate, endDate: LocalDate) {
        if (endDate.isBefore(startDate)) return
        val habits = dao.getActiveHabits()
        val timestamp = now()
        val entries = buildList {
            var date = startDate
            while (!date.isAfter(endDate)) {
                habits.filter { it.toDomain().isScheduledOn(date) }.forEach { habit ->
                    if (dao.getEntries(habit.id, date.toString()).isEmpty()) {
                        add(HabitEntryEntity(habitId = habit.id, localDate = date.toString(), amount = 0.0, status = HabitEntryEntity.STATUS_SKIPPED, note = "VACATION", createdAt = timestamp, updatedAt = timestamp))
                    }
                }
                date = date.plusDays(1)
            }
        }
        if (entries.isNotEmpty()) dao.insertEntries(entries)
    }

    override suspend fun clearVacation(startDate: LocalDate, endDate: LocalDate) {
        if (!endDate.isBefore(startDate)) dao.clearSkippedEntries(startDate.toString(), endDate.toString())
    }

    override suspend fun syncHealthSteps(steps: Long) = syncHealth(HealthMetric.Steps) { steps.toDouble() }

    override suspend fun healthMetricsInUse(): Set<HealthMetric> =
        dao.getActiveHabits().mapNotNull { habit -> HealthMetric.entries.firstOrNull { it.name == habit.healthMetric } }
            .filter { it != HealthMetric.None }
            .toSet()

    override suspend fun syncHealth(metric: HealthMetric, valueFor: (unit: String) -> Double) {
        val date = today().toString()
        dao.getActiveHabits().filter { it.healthMetric == metric.name }.forEach { habit ->
            // Only the health reading is replaced; anything logged by hand stays.
            dao.clearHealthEntries(habit.id, date)
            val value = valueFor(habit.unit)
            if (value > 0) {
                val timestamp = now()
                dao.insertEntry(HabitEntryEntity(habitId = habit.id, localDate = date, amount = value, status = HabitEntryEntity.STATUS_HEALTH, createdAt = timestamp, updatedAt = timestamp))
            }
        }
    }

    override suspend fun setAutoValue(habitId: Long, value: Double) {
        val date = today().toString()
        dao.clearHealthEntries(habitId, date)
        if (value > 0) {
            val timestamp = now()
            dao.insertEntry(HabitEntryEntity(habitId = habitId, localDate = date, amount = value, status = HabitEntryEntity.STATUS_HEALTH, createdAt = timestamp, updatedAt = timestamp))
        }
    }

    override suspend fun skipHabit(id: Long, reason: String?) = skipHabitOnDate(id, today(), reason)

    override suspend fun skipHabitOnDate(id: Long, date: LocalDate, reason: String?) {
        val localDate = date.toString()
        val current = dao.getEntries(id, localDate)
        val isAlreadySkipped = current.any { it.status == HabitEntryEntity.STATUS_SKIPPED }
        if (isAlreadySkipped && reason == null) {
            dao.clearEntries(id, localDate)
        } else {
            val timestamp = now()
            dao.replaceDayWithEntry(
                habitId = id,
                localDate = localDate,
                entry = HabitEntryEntity(
                    habitId = id,
                    localDate = localDate,
                    amount = 0.0,
                    status = HabitEntryEntity.STATUS_SKIPPED,
                    note = reason,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                ),
            )
        }
    }

    private suspend fun prerequisiteComplete(habit: HabitEntity, date: LocalDate): Boolean {
        val prerequisiteId = habit.prerequisiteHabitId ?: return true
        val prerequisite = dao.getHabit(prerequisiteId) ?: return true
        val entries = dao.getEntries(prerequisiteId, date.toString())
        return entries.any { it.status == HabitEntryEntity.STATUS_COMPLETED } || entries.filter(HabitEntryEntity::isProgress).sumOf(HabitEntryEntity::amount) >= prerequisite.target
    }

    private suspend fun periodGoalReached(habit: HabitEntity, date: LocalDate): Boolean {
        val period = GoalPeriod.entries.firstOrNull { it.name == habit.goalPeriod } ?: GoalPeriod.Daily
        if (period == GoalPeriod.Daily) return false
        val (start, end) = period.bounds(date)
        return periodCompletionCount(habit, dao.getEntriesBetween(habit.id, start.toString(), end.toString()), date) >= habit.periodTarget
    }

    override fun observeHabitLevels(habitId: Long): Flow<List<HabitLevel>> =
        dao.observeHabitLevels(habitId).map { list -> list.map(HabitLevelEntity::toDomain) }

    override suspend fun getHabitLevels(habitId: Long): List<HabitLevel> =
        dao.getHabitLevels(habitId).map(HabitLevelEntity::toDomain)

    override suspend fun saveHabitLevels(habitId: Long, levels: List<HabitLevel>) {
        dao.clearHabitLevels(habitId)
        val timestamp = now()
        val entities = levels.mapIndexed { index, level ->
            HabitLevelEntity(
                id = 0,
                habitId = habitId,
                levelNumber = index + 1,
                name = level.name.trim(),
                target = level.target,
                minimumTarget = level.minimumTarget,
                maximumTarget = level.maximumTarget,
                rewardNote = level.rewardNote.trim(),
                createdAt = timestamp,
            )
        }
        if (entities.isNotEmpty()) {
            dao.insertHabitLevels(entities)
        }
    }

    override suspend fun advanceHabitLevel(habitId: Long, targetLevelNumber: Int) {
        val levels = dao.getHabitLevels(habitId)
        val targetLevel = levels.firstOrNull { it.levelNumber == targetLevelNumber } ?: return
        val timestamp = now()
        dao.advanceHabitLevel(
            habitId = habitId,
            levelNumber = targetLevel.levelNumber,
            newTarget = targetLevel.target,
            newMinTarget = targetLevel.minimumTarget,
            newMaxTarget = targetLevel.maximumTarget,
            updatedAt = timestamp,
        )
    }
}

/** A habit in the trash and when it was put there. */
data class TrashedHabit(val habit: Habit, val deletedAt: Long) {
    fun daysLeft(now: Long = System.currentTimeMillis()): Int =
        (TRASH_DAYS - ((now - deletedAt) / 86_400_000L).toInt()).coerceIn(0, TRASH_DAYS)
}

const val TRASH_DAYS = 30

private fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    emoji = emoji,
    colorArgb = colorArgb,
    type = HabitType.valueOf(type),
    direction = HabitDirection.valueOf(direction),
    target = target,
    unit = unit,
    step = step,
    scheduleDaysMask = scheduleDaysMask,
    goalPeriod = GoalPeriod.entries.firstOrNull { it.name == goalPeriod } ?: GoalPeriod.Daily,
    periodTarget = periodTarget.coerceAtLeast(1),
    routineName = routineName,
    prerequisiteHabitId = prerequisiteHabitId,
    healthMetric = HealthMetric.entries.firstOrNull { it.name == healthMetric } ?: HealthMetric.None,
    sortOrder = sortOrder,
    reminderMinutes = reminderMinutes,
    reminderRepeatMinutes = reminderRepeatMinutes,
    generalNote = generalNote,
    createdOn = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate(),
    minimumTarget = minimumTarget,
    maximumTarget = maximumTarget,
    difficulty = difficulty.coerceIn(1, 3),
    contextLabel = contextLabel,
    intention = intention,
    activeStartEpochDay = activeStartEpochDay,
    activeEndEpochDay = activeEndEpochDay,
    repeatIntervalDays = repeatIntervalDays.coerceAtLeast(1),
    pausedUntilEpochDay = pausedUntilEpochDay,
    tags = tags,
    currentLevel = currentLevel,
    tinyVersion = tinyVersion,
    slots = app.tsuzuku.model.HabitSlot.parse(slots),
)

private fun HabitLevelEntity.toDomain() = HabitLevel(
    id = id,
    habitId = habitId,
    levelNumber = levelNumber,
    name = name,
    target = target,
    minimumTarget = minimumTarget,
    maximumTarget = maximumTarget,
    rewardNote = rewardNote,
    createdAt = createdAt,
)

private fun HabitLevel.toEntity() = HabitLevelEntity(
    id = id,
    habitId = habitId,
    levelNumber = levelNumber,
    name = name,
    target = target,
    minimumTarget = minimumTarget,
    maximumTarget = maximumTarget,
    rewardNote = rewardNote,
    createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt,
)

private fun normalizeHabitTags(value: String): String = value.split(',', ';', '#').map(String::trim).filter(String::isNotBlank).distinct().joinToString(",")

private fun HabitEntryEntity.isProgress(): Boolean = status == HabitEntryEntity.STATUS_PROGRESS || status == HabitEntryEntity.STATUS_HEALTH

private fun GoalPeriod.bounds(date: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
    GoalPeriod.Daily -> date to date
    GoalPeriod.Weekly -> date.minusDays((date.dayOfWeek.value - 1).toLong()) to date.plusDays((7 - date.dayOfWeek.value).toLong())
    GoalPeriod.Monthly -> date.withDayOfMonth(1) to date.with(TemporalAdjusters.lastDayOfMonth())
}

private fun periodCompletionCount(habit: HabitEntity, entries: List<HabitEntryEntity>, date: LocalDate): Int {
    val period = GoalPeriod.entries.firstOrNull { it.name == habit.goalPeriod } ?: GoalPeriod.Daily
    if (period == GoalPeriod.Daily) return 0
    val (start, end) = period.bounds(date)
    return entries.filter { runCatching { LocalDate.parse(it.localDate) }.getOrNull()?.let { day -> !day.isBefore(start) && !day.isAfter(end) } == true }
        .groupBy(HabitEntryEntity::localDate)
        .count { (_, dayEntries) -> dayEntries.any { it.status == HabitEntryEntity.STATUS_COMPLETED } || dayEntries.filter(HabitEntryEntity::isProgress).sumOf(HabitEntryEntity::amount) >= habit.target }
}

private fun List<HabitEntryEntity>.toHistoryRecords(): List<HabitHistoryRecord> =
    groupBy { it.habitId to it.localDate }
        .mapNotNull { (key, dayEntries) ->
            val date = runCatching { LocalDate.parse(key.second) }.getOrNull() ?: return@mapNotNull null
            HabitHistoryRecord(
                habitId = key.first,
                date = date,
                progress = dayEntries.filter(HabitEntryEntity::isProgress).sumOf(HabitEntryEntity::amount),
                status = dayEntries.toDayStatus(),
                note = dayEntries.firstOrNull { it.status == HabitEntryEntity.STATUS_SKIPPED }?.note,
            )
        }

private fun List<HabitEntryEntity>.toDayStatus(): HabitDayStatus = when {
    any { it.status == HabitEntryEntity.STATUS_SKIPPED } -> HabitDayStatus.Skipped
    any { it.status == HabitEntryEntity.STATUS_FAILED } -> HabitDayStatus.Failed
    any { it.status == HabitEntryEntity.STATUS_COMPLETED } -> HabitDayStatus.Completed
    else -> HabitDayStatus.Pending
}
