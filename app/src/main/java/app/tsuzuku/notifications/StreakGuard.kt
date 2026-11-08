package app.tsuzuku.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import app.tsuzuku.HomeActivity
import app.tsuzuku.R
import app.tsuzuku.TsuzukuApplication
import app.tsuzuku.model.GoalPeriod
import app.tsuzuku.model.HabitDayStatus
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.widget.TsuzukuWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** Next 20:00 local time. */
internal fun nextStreakGuardTime(now: ZonedDateTime): ZonedDateTime {
    val candidate = now.withHour(20).withMinute(0).withSecond(0).withNano(0)
    return if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
}

/** The streak most worth protecting tonight: a daily habit, not done yet, at least 3 days long. */
internal fun streakAtRisk(habits: List<TodayHabit>): TodayHabit? =
    habits.asSequence()
        .filter { !it.isComplete && !it.isBlocked && it.status != HabitDayStatus.Skipped }
        .filter { it.habit.direction == HabitDirection.Build && it.habit.goalPeriod == GoalPeriod.Daily }
        .filter { it.streakDays >= 3 }
        .maxByOrNull { it.streakDays }

/**
 * One quiet evening nudge, only when a real streak is about to break. It names the streak and
 * offers the bad-day version as a one-tap way to keep it.
 */
object StreakGuard {
    private const val REQUEST_CODE = 0x57A6
    private const val NOTIFICATION_ID = 0x57A6
    private const val PREFS = "tsuzuku_streak_guard"
    private const val KEY_ENABLED = "enabled"
    internal const val ACTION_CHECK = "app.tsuzuku.action.STREAK_CHECK"
    internal const val ACTION_TINY = "app.tsuzuku.action.STREAK_TINY"
    internal const val EXTRA_HABIT_ID = "habit_id"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) schedule(context) else context.getSystemService(AlarmManager::class.java).cancel(checkIntent(context))
    }

    fun schedule(context: Context) {
        if (!isEnabled(context)) return
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextStreakGuardTime(ZonedDateTime.now()).toInstant().toEpochMilli(),
            checkIntent(context),
        )
    }

    private fun checkIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, StreakGuardReceiver::class.java).setAction(ACTION_CHECK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun show(context: Context, item: TodayHabit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        HabitNotifications.createChannel(context)
        val habit = item.habit
        val open = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = if (habit.tinyVersion.isNotBlank()) {
            context.getString(R.string.streak_guard_body_tiny, habit.tinyVersion)
        } else {
            context.getString(R.string.streak_guard_body)
        }
        val tiny = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE + 1,
            Intent(context, StreakGuardReceiver::class.java).setAction(ACTION_TINY).putExtra(EXTRA_HABIT_ID, habit.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, HabitNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.streak_guard_title, "${habit.emoji} ${habit.name}".trim(), item.streakDays))
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .addAction(Notification.Action.Builder(null as Icon?, context.getString(R.string.tiny_version_done), tiny).build())
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    internal fun dismiss(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }
}

class StreakGuardReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TsuzukuApplication
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    StreakGuard.ACTION_CHECK -> {
                        StreakGuard.schedule(context)
                        streakAtRisk(app.habitRepository.observeToday().first())?.let { StreakGuard.show(context, it) }
                    }
                    StreakGuard.ACTION_TINY -> {
                        val id = intent.getLongExtra(StreakGuard.EXTRA_HABIT_ID, -1L)
                        if (id > 0) app.habitRepository.completeTinyVersion(id)
                        StreakGuard.dismiss(context)
                        TsuzukuWidgets.updateAll(context)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
