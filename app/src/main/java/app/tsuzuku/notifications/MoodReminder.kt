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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

/** Next 21:00 local time. */
internal fun nextMoodReminderTime(now: ZonedDateTime): ZonedDateTime {
    val candidate = now.withHour(21).withMinute(0).withSecond(0).withNano(0)
    return if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
}

/**
 * Evening "How was today?" notification with three one-tap answers. Skipped when a mood is
 * already logged. Answers feed the mood insights on the Progress tab.
 */
object MoodReminder {
    private const val REQUEST_CODE = 0x40D0
    internal const val NOTIFICATION_ID = 0x40D0
    private const val PREFS = "tsuzuku_mood_reminder"
    private const val KEY_ENABLED = "enabled"
    internal const val ACTION_SHOW = "app.tsuzuku.action.MOOD_SHOW"
    internal const val ACTION_LOG = "app.tsuzuku.action.MOOD_LOG"
    internal const val EXTRA_MOOD = "mood"

    /** The three quick answers: rough, okay, good — on the same 1–5 scale as the app. */
    private val quickAnswers = listOf(2 to R.string.mood_quick_rough, 3 to R.string.mood_quick_okay, 4 to R.string.mood_quick_good)

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
            nextMoodReminderTime(ZonedDateTime.now()).toInstant().toEpochMilli(),
            showIntent(context),
        )
    }

    private fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(showIntent(context))
    }

    private fun showIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, MoodReminderReceiver::class.java).setAction(ACTION_SHOW),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun show(context: Context) {
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
        val builder = Notification.Builder(context, HabitNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.mood_question))
            .setContentText(context.getString(R.string.mood_notification_body))
            .setContentIntent(open)
            .setAutoCancel(true)
        quickAnswers.forEach { (mood, label) ->
            val log = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE + mood,
                Intent(context, MoodReminderReceiver::class.java).setAction(ACTION_LOG).putExtra(EXTRA_MOOD, mood),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(Notification.Action.Builder(null as Icon?, context.getString(label), log).build())
        }
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, builder.build())
    }
}

class MoodReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TsuzukuApplication
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val today = LocalDate.now().toEpochDay()
                when (intent.action) {
                    MoodReminder.ACTION_LOG -> {
                        val mood = intent.getIntExtra(MoodReminder.EXTRA_MOOD, 3)
                        val existing = app.reviewRepository.observeDailyReview(today).first()
                        app.reviewRepository.saveDailyReview(
                            today,
                            mood,
                            existing?.energy ?: 3,
                            existing?.productivity ?: 3,
                            existing?.stress ?: 3,
                            existing?.note.orEmpty(),
                        )
                        context.getSystemService(NotificationManager::class.java).cancel(MoodReminder.NOTIFICATION_ID)
                    }
                    else -> {
                        if (app.reviewRepository.observeDailyReview(today).first() == null) MoodReminder.show(context)
                        MoodReminder.schedule(context)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
