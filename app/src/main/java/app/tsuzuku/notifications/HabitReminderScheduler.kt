package app.tsuzuku.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.tsuzuku.data.local.HabitDao
import app.tsuzuku.data.local.HabitEntity
import java.time.ZonedDateTime

class HabitReminderScheduler(
    private val context: Context,
    private val habitDao: HabitDao,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun schedule(habitId: Long) {
        habitDao.getHabit(habitId)?.let(::schedule)
    }

    fun schedule(habit: HabitEntity) {
        val reminderMinutes = habit.reminderMinutes ?: return cancel(habit.id)
        val slots = app.tsuzuku.model.HabitSlot.parse(habit.slots)
        cancelSlots(habit.id)
        if (slots.size > 1) {
            slots.forEachIndexed { i, slot ->
                val at = nextReminderTime(habit.scheduleDaysMask, slot.minutes, ZonedDateTime.now()).toInstant().toEpochMilli()
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, slotPendingIntent(habit.id, i))
            }
            return
        }
        schedule(habit.id, habit.scheduleDaysMask, reminderMinutes)
    }

    private fun cancelSlots(habitId: Long) {
        for (i in 0 until app.tsuzuku.model.HabitSlot.MAX) alarmManager.cancel(slotPendingIntent(habitId, i))
    }

    private fun slotPendingIntent(habitId: Long, index: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        habitId.hashCode() xor ((index + 1) shl 24),
        Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_REMIND
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun schedule(habitId: Long, daysMask: Int, reminderMinutes: Int) {
        val triggerAt = nextReminderTime(
            daysMask = daysMask,
            reminderMinutes = reminderMinutes,
            now = ZonedDateTime.now(),
        )
        val at = triggerAt.toInstant().toEpochMilli()
        if (HabitAlarms.isOn(context, habitId)) {
            HabitAlarms.scheduleExact(context, at, reminderPendingIntent(habitId))
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, reminderPendingIntent(habitId))
        }
    }

    suspend fun rescheduleAll() {
        habitDao.getHabitsWithReminders().forEach(::schedule)
    }

    fun cancel(habitId: Long) {
        alarmManager.cancel(reminderPendingIntent(habitId))
        alarmManager.cancel(snoozePendingIntent(habitId))
        cancelSlots(habitId)
        HabitAlarms.stop(context, habitId)
    }

    fun snooze(habitId: Long, minutes: Int) {
        val at = System.currentTimeMillis() + minutes.coerceIn(1, 720) * 60_000L
        if (HabitAlarms.isOn(context, habitId)) {
            HabitAlarms.scheduleExact(context, at, snoozePendingIntent(habitId))
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, snoozePendingIntent(habitId))
        }
    }

    private fun reminderPendingIntent(habitId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        habitId.hashCode(),
        Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_REMIND
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun snoozePendingIntent(habitId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        habitId.hashCode() xor SNOOZE_REQUEST_MASK,
        Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderReceiver.ACTION_REMIND
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val SNOOZE_REQUEST_MASK = 0x40000000
    }
}

internal fun nextReminderTime(
    daysMask: Int,
    reminderMinutes: Int,
    now: ZonedDateTime,
): ZonedDateTime {
    val hour = (reminderMinutes / 60).coerceIn(0, 23)
    val minute = (reminderMinutes % 60).coerceIn(0, 59)
    for (dayOffset in 0..7) {
        val candidateDate = now.toLocalDate().plusDays(dayOffset.toLong())
        val dayBit = 1 shl (candidateDate.dayOfWeek.value - 1)
        if (daysMask and dayBit == 0) continue
        val candidate = candidateDate.atTime(hour, minute).atZone(now.zone)
        if (candidate.isAfter(now)) return candidate
    }
    return now.plusDays(1).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
}
