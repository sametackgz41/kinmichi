package app.tsuzuku.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.tsuzuku.HomeActivity
import app.tsuzuku.R
import app.tsuzuku.data.RoomHabitRepository
import app.tsuzuku.data.local.TsuzukuDatabase
import app.tsuzuku.model.HabitHistorySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/** Result of the last seven days, used for the Sunday recap notification. */
internal data class WeekRecap(val completed: Int, val scheduled: Int, val bestHabit: String?) {
    val percent: Int get() = if (scheduled == 0) 0 else completed * 100 / scheduled
}

internal fun weekRecap(history: HabitHistorySnapshot, today: LocalDate = LocalDate.now()): WeekRecap {
    val days = (0L..6L).map { today.minusDays(it) }
    val perDay = days.flatMap { history.habitsForDay(it) }
    val best = perDay.filter { it.isComplete }.groupingBy { it.habit.name }.eachCount().maxByOrNull { it.value }?.key
    return WeekRecap(completed = perDay.count { it.isComplete }, scheduled = perDay.size, bestHabit = best)
}

/** Next Sunday 19:00 local time (today if it is Sunday before 19:00). */
internal fun nextWeeklySummaryTime(now: ZonedDateTime): ZonedDateTime {
    val candidate = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        .withHour(19).withMinute(0).withSecond(0).withNano(0)
    return if (candidate.isAfter(now)) candidate else candidate.plusWeeks(1)
}

object WeeklySummary {
    private const val REQUEST_CODE = 0x5EC0
    private const val NOTIFICATION_ID = 0x5EC0
    private const val PREFS = "tsuzuku_weekly_summary"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) schedule(context) else cancel(context)
    }

    fun schedule(context: Context) {
        if (!isEnabled(context)) return
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextWeeklySummaryTime(ZonedDateTime.now()).toInstant().toEpochMilli(),
            pendingIntent(context),
        )
    }

    private fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, WeeklySummaryReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun show(context: Context, recap: WeekRecap) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        HabitNotifications.createChannel(context)
        val open = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = if (recap.scheduled == 0 || recap.bestHabit == null) {
            context.getString(R.string.weekly_summary_body_empty)
        } else {
            context.getString(R.string.weekly_summary_body, recap.completed, recap.scheduled, recap.bestHabit)
        }
        val notification = Notification.Builder(context, HabitNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.weekly_summary_title, recap.percent))
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }
}

class WeeklySummaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = TsuzukuDatabase.getInstance(context).habitDao()
                val history = RoomHabitRepository(dao).observeHistory().first()
                WeeklySummary.show(context, weekRecap(history))
            } finally {
                WeeklySummary.schedule(context)
                pending.finish()
            }
        }
    }
}
