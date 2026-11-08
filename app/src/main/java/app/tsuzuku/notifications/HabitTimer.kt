package app.tsuzuku.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import app.tsuzuku.HomeActivity
import app.tsuzuku.R
import app.tsuzuku.data.HabitRepository
import app.tsuzuku.data.RoomHabitRepository
import app.tsuzuku.data.local.TsuzukuDatabase
import app.tsuzuku.data.preferences.StoredTimerSession
import app.tsuzuku.data.preferences.TimerSessionStore
import app.tsuzuku.data.preferences.elapsedSeconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The timer of duration habits: an ongoing notification with a running clock and Pause /
 * Finish, and an alarm at the goal so it is saved even with the app closed.
 */
object HabitTimer {
    private const val CHANNEL_ID = "habit_timer"
    private const val NOTIFICATION_ID = 0x7100
    private const val DONE_NOTIFICATION_ID = 0x7101

    fun start(context: Context, session: StoredTimerSession) {
        TimerSessionStore(context).save(session)
        show(context, session)
        scheduleEnd(context, session)
    }

    fun pause(context: Context, now: Long = System.currentTimeMillis()) {
        val store = TimerSessionStore(context)
        val session = store.load()?.takeUnless { it.isPaused } ?: return
        val paused = session.copy(pausedAtMillis = now)
        store.save(paused)
        cancelEnd(context)
        show(context, paused)
    }

    fun resume(context: Context, now: Long = System.currentTimeMillis()) {
        val store = TimerSessionStore(context)
        val session = store.load()?.takeIf { it.isPaused } ?: return
        val running = session.copy(pausedMillis = session.pausedMillis + (now - session.pausedAtMillis), pausedAtMillis = 0L)
        store.save(running)
        show(context, running)
        scheduleEnd(context, running)
    }

    /**
     * Stops the timer and saves the time as progress. Clears the stored session first, so the
     * app and the notification can't both save it.
     */
    suspend fun finish(context: Context, repository: HabitRepository, reachedGoal: Boolean = false, now: Long = System.currentTimeMillis()) {
        val store = TimerSessionStore(context)
        val session = store.load() ?: return
        store.clear()
        cancelEnd(context)
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        val elapsed = session.elapsedSeconds(now)
        if (elapsed > 0) repository.addProgress(session.habitId, elapsed / 60.0)
        if (reachedGoal) showDone(context, session)
        app.tsuzuku.widget.TsuzukuWidgets.updateAll(context)
    }

    private fun channel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.timer_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            },
        )
    }

    private fun show(context: Context, session: StoredTimerSession, now: Long = System.currentTimeMillis()) {
        channel(context)
        val elapsed = session.elapsedSeconds(now)
        val title = listOf(session.habitEmoji, session.habitName).filter(String::isNotBlank).joinToString(" ")
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STOPWATCH)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(context))
            .setProgress(session.maxSeconds.toInt(), elapsed.toInt(), false)
        if (session.isPaused) {
            builder.setContentText(context.getString(R.string.timer_paused, clock(elapsed), clock(session.maxSeconds)))
                .setShowWhen(false)
                .addAction(action(context, R.string.timer_resume, ACTION_RESUME))
        } else {
            builder.setContentText(context.getString(R.string.timer_goal, clock(session.maxSeconds)))
                .setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(now - elapsed * 1_000L)
                .addAction(action(context, R.string.timer_pause, ACTION_PAUSE))
        }
        builder.addAction(action(context, R.string.timer_finish, ACTION_FINISH))
        runCatching { context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, builder.build()) }
    }

    private fun showDone(context: Context, session: StoredTimerSession) {
        HabitNotifications.createChannel(context)
        val title = listOf(session.habitEmoji, session.habitName).filter(String::isNotBlank).joinToString(" ")
        val notification = Notification.Builder(context, HabitNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.timer_done, clock(session.maxSeconds)))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()
        runCatching { context.getSystemService(NotificationManager::class.java).notify(DONE_NOTIFICATION_ID, notification) }
    }

    private fun scheduleEnd(context: Context, session: StoredTimerSession, now: Long = System.currentTimeMillis()) {
        val left = (session.maxSeconds - session.elapsedSeconds(now)).coerceAtLeast(1)
        val at = now + left * 1_000L
        val manager = context.getSystemService(AlarmManager::class.java)
        val intent = endIntent(context)
        runCatching { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent) }
            .onFailure { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent) }
    }

    private fun cancelEnd(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(endIntent(context))
    }

    private fun endIntent(context: Context) = broadcast(context, ACTION_DONE)

    private fun broadcast(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, HabitTimerReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(context: Context, label: Int, action: String): Notification.Action =
        Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_notification), context.getString(label), broadcast(context, action)).build()

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun elapsed(session: StoredTimerSession, now: Long): Long = session.elapsedSeconds(now)

    /** "20:00", "1:05:00". */
    fun clock(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    const val ACTION_PAUSE = "app.tsuzuku.action.TIMER_PAUSE"
    const val ACTION_RESUME = "app.tsuzuku.action.TIMER_RESUME"
    const val ACTION_FINISH = "app.tsuzuku.action.TIMER_FINISH"
    const val ACTION_DONE = "app.tsuzuku.action.TIMER_DONE"
}

class HabitTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = RoomHabitRepository(TsuzukuDatabase.getInstance(context).habitDao())
                when (intent.action) {
                    HabitTimer.ACTION_PAUSE -> HabitTimer.pause(context)
                    HabitTimer.ACTION_RESUME -> HabitTimer.resume(context)
                    HabitTimer.ACTION_FINISH -> HabitTimer.finish(context, repository)
                    HabitTimer.ACTION_DONE -> {
                        val session = TimerSessionStore(context).load()
                        if (session != null && !session.isPaused) HabitTimer.finish(context, repository, reachedGoal = true)
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}
