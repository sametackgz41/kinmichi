package app.tsuzuku.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.tsuzuku.R
import app.tsuzuku.data.RoomHabitRepository
import app.tsuzuku.data.local.TsuzukuDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** A daily window for spread-out reminders, in minutes of the day. */
data class SpreadWindow(val startMinutes: Int, val endMinutes: Int) {
    companion object {
        val Presets = listOf(SpreadWindow(8 * 60, 20 * 60), SpreadWindow(9 * 60, 21 * 60), SpreadWindow(10 * 60, 22 * 60))
    }
}

/**
 * "Remind me through the day" for count habits (water, say): the amount still to go is spread
 * over the time left in the window, so reminders come closer together when you fall behind and
 * stop once the day's target is reached. Kept on the device, like alarms.
 */
object SpreadReminders {
    private const val PREFS = "spread_reminders"
    const val ACTION_FIRE = "app.tsuzuku.action.SPREAD_FIRE"
    const val ACTION_ADD_ONE = "app.tsuzuku.action.SPREAD_ADD_ONE"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun window(context: Context, habitId: Long): SpreadWindow? {
        val p = prefs(context)
        if (!p.getBoolean(habitId.toString(), false)) return null
        return SpreadWindow(p.getInt("$habitId.start", 9 * 60), p.getInt("$habitId.end", 21 * 60))
    }

    fun set(context: Context, habitId: Long, window: SpreadWindow?) {
        prefs(context).edit().apply {
            if (window == null) {
                remove(habitId.toString()); remove("$habitId.start"); remove("$habitId.end")
            } else {
                putBoolean(habitId.toString(), true)
                putInt("$habitId.start", window.startMinutes)
                putInt("$habitId.end", window.endMinutes)
            }
        }.apply()
        if (window == null) cancel(context, habitId)
    }

    private fun habitIds(context: Context): List<Long> =
        prefs(context).all.keys.mapNotNull { it.toLongOrNull() }

    private fun pending(context: Context, habitId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        habitId.hashCode() xor 0x04000000,
        Intent(context, SpreadReminderReceiver::class.java).setAction(ACTION_FIRE).putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun notificationId(habitId: Long) = habitId.hashCode() xor 0x04000000

    fun cancel(context: Context, habitId: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context, habitId))
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(habitId))
    }

    /** Works out and sets the next reminder for one habit. */
    suspend fun schedule(context: Context, habitId: Long) {
        val window = window(context, habitId) ?: return
        val dao = TsuzukuDatabase.getInstance(context).habitDao()
        val habit = dao.getHabit(habitId) ?: return cancel(context, habitId)
        val today = RoomHabitRepository(dao).observeToday().first().firstOrNull { it.habit.id == habitId }
        val remaining = if (today == null) 0.0 else (today.habit.target - today.progress).coerceAtLeast(0.0)
        val next = nextSpreadTime(LocalDateTime.now(), window, kotlin.math.ceil(remaining).toInt(), habit.scheduleDaysMask)
        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context, habitId))
    }

    suspend fun rescheduleAll(context: Context) = habitIds(context).forEach { schedule(context, it) }

    fun show(context: Context, habitId: Long, title: String, progressText: String) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val open = PendingIntent.getActivity(
            context, notificationId(habitId),
            Intent(context, app.tsuzuku.HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val addOne = PendingIntent.getBroadcast(
            context, notificationId(habitId) xor 1,
            Intent(context, SpreadReminderReceiver::class.java).setAction(ACTION_ADD_ONE).putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val icon = android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_notification)
        val notification = Notification.Builder(context, HabitNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(progressText)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .addAction(Notification.Action.Builder(icon, context.getString(R.string.spread_add_one), addOne).build())
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notificationId(habitId), notification)
    }
}

/**
 * When the next spread reminder is due. [remaining] whole units are spread evenly over what is
 * left of today's window (at least 30 minutes apart); outside the window, or when nothing is
 * left, it is the start of the next scheduled day's window.
 */
internal fun nextSpreadTime(now: LocalDateTime, window: SpreadWindow, remaining: Int, daysMask: Int): LocalDateTime {
    fun scheduled(date: LocalDate) = daysMask and (1 shl (date.dayOfWeek.value - 1)) != 0
    fun windowStart(date: LocalDate) = date.atStartOfDay().plusMinutes(window.startMinutes.toLong())
    fun nextDayStart(): LocalDateTime {
        var d = now.toLocalDate().plusDays(1)
        repeat(7) { if (scheduled(d)) return windowStart(d); d = d.plusDays(1) }
        return windowStart(now.toLocalDate().plusDays(1))
    }
    val date = now.toLocalDate()
    val start = windowStart(date)
    val end = date.atStartOfDay().plusMinutes(window.endMinutes.toLong())
    if (!scheduled(date) || remaining <= 0 || !now.isBefore(end)) return nextDayStart()
    if (now.isBefore(start)) return start
    val minutesLeft = java.time.Duration.between(now, end).toMinutes()
    val gap = (minutesLeft / remaining).coerceAtLeast(30)
    val next = now.plusMinutes(gap)
    return if (next.isAfter(end)) end else next
}

class SpreadReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(HabitReminderReceiver.EXTRA_HABIT_ID, -1L)
        if (habitId < 0) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = TsuzukuDatabase.getInstance(context).habitDao()
                val repository = RoomHabitRepository(dao)
                when (intent.action) {
                    SpreadReminders.ACTION_ADD_ONE -> {
                        repository.incrementHabit(habitId)
                        context.getSystemService(NotificationManager::class.java).cancel(SpreadReminders.notificationId(habitId))
                        app.tsuzuku.widget.TsuzukuWidgets.updateAll(context)
                    }
                    SpreadReminders.ACTION_FIRE -> {
                        val item = repository.observeToday().first().firstOrNull { it.habit.id == habitId }
                        if (item != null && !item.isComplete) {
                            val target = item.habit.target
                            fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
                            SpreadReminders.show(
                                context,
                                habitId,
                                "${item.habit.emoji} ${item.habit.name}",
                                "${fmt(item.progress)} / ${fmt(target)} ${item.habit.unit}".trim(),
                            )
                        }
                    }
                }
                SpreadReminders.schedule(context, habitId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
