package app.tsuzuku.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import app.tsuzuku.R
import app.tsuzuku.data.local.TsuzukuDatabase

/**
 * Habits whose reminder rings like an alarm clock: on time to the minute, with the phone's
 * alarm sound repeating and a full-screen page, instead of a quiet notification.
 *
 * Which habits ring is kept on the device (it describes this phone, not the habit itself).
 */
object HabitAlarms {
    private const val PREFS = "habit_alarms"
    const val CHANNEL_ID = "habit_alarms_v1"
    const val SNOOZE_MINUTES = 10

    fun isOn(context: Context, habitId: Long): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(habitId.toString(), false)

    fun set(context: Context, habitId: Long, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (on) putBoolean(habitId.toString(), true) else remove(habitId.toString())
        }.apply()
    }

    fun createChannel(context: Context) {
        val sound = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.alarm_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.alarm_channel_description)
                setSound(
                    sound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600, 400)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    /** Exact, alarm-clock style trigger; falls back to the normal window if exact alarms aren't allowed. */
    fun scheduleExact(context: Context, triggerAtMillis: Long, operation: PendingIntent) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            return
        }
        val show = PendingIntent.getActivity(
            context,
            0x51,
            Intent(context, app.tsuzuku.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, show), operation)
    }

    private fun notificationId(habitId: Long) = habitId.hashCode() xor 0x10000000

    /** Rings now: a looping alarm-sound notification that opens the full-screen page. */
    fun ring(context: Context, habitId: Long, name: String, emoji: String, plan: String) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        createChannel(context)
        val page = PendingIntent.getActivity(
            context,
            notificationId(habitId),
            Intent(context, HabitAlarmActivity::class.java).apply {
                putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
                putExtra(HabitAlarmActivity.EXTRA_NAME, name)
                putExtra(HabitAlarmActivity.EXTRA_EMOJI, emoji)
                putExtra(HabitAlarmActivity.EXTRA_PLAN, plan)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        fun action(kind: String, label: String) = Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_notification),
            label,
            // Start opens a page, which Android 12+ only allows straight from the notification.
            if (kind == ACTION_START) PendingIntent.getActivity(
                context,
                notificationId(habitId) xor kind.hashCode(),
                Intent(context, HabitAlarmActivity::class.java).apply {
                    putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
                    putExtra(HabitAlarmActivity.EXTRA_ANSWER, kind)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ) else PendingIntent.getBroadcast(
                context,
                notificationId(habitId) xor kind.hashCode(),
                Intent(context, HabitAlarmActionReceiver::class.java).apply {
                    action = kind
                    putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$emoji $name")
            .setContentText(plan.ifBlank { context.getString(R.string.alarm_time_to_start) })
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(page, true)
            .setContentIntent(page)
            .addAction(action(ACTION_START, context.getString(R.string.alarm_start)))
            .addAction(action(ACTION_SNOOZE, context.getString(R.string.alarm_snooze, SNOOZE_MINUTES)))
            .addAction(action(ACTION_DISMISS, context.getString(R.string.alarm_not_today)))
            .build()
        // Insistent: the sound keeps repeating until someone answers it.
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        context.getSystemService(NotificationManager::class.java).notify(notificationId(habitId), notification)
    }

    fun stop(context: Context, habitId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(habitId))
    }

    /** What the three answers do, shared by the full-screen page and the notification buttons. */
    fun answer(context: Context, habitId: Long, kind: String) {
        stop(context, habitId)
        when (kind) {
            ACTION_SNOOZE -> HabitReminderScheduler(context, TsuzukuDatabase.getInstance(context).habitDao()).snooze(habitId, SNOOZE_MINUTES)
            ACTION_START -> context.startActivity(
                Intent(context, app.tsuzuku.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
    }

    const val ACTION_START = "app.tsuzuku.action.ALARM_START"
    const val ACTION_SNOOZE = "app.tsuzuku.action.ALARM_SNOOZE"
    const val ACTION_DISMISS = "app.tsuzuku.action.ALARM_DISMISS"
}

class HabitAlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(HabitReminderReceiver.EXTRA_HABIT_ID, -1L)
        val kind = intent.action ?: return
        if (habitId < 0) return
        HabitAlarms.answer(context, habitId, kind)
        context.sendBroadcast(Intent(HabitAlarmActivity.ACTION_CLOSE).setPackage(context.packageName))
    }
}
