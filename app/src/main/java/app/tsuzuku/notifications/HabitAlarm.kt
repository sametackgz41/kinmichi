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
import android.os.Build
import app.tsuzuku.R
import app.tsuzuku.data.RoomHabitRepository
import app.tsuzuku.data.local.TsuzukuDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** What has to be done before an alarm habit goes quiet. */
enum class AlarmMission { None, Math, Memory, Puzzle, Mixed }

/** The ten wake-up tasks; an alarm uses up to three of them. */
enum class AlarmTask(val emoji: String, val label: Int) {
    Math("➕", R.string.alarm_mission_math),
    Memory("🧠", R.string.alarm_mission_memory),
    Hunt("🔢", R.string.alarm_mission_puzzle),
    Shake("📳", R.string.alarm_task_shake),
    Steps("🚶", R.string.alarm_task_steps),
    Type("⌨️", R.string.alarm_task_type),
    Stroop("🎨", R.string.alarm_task_stroop),
    Anagram("🔤", R.string.alarm_task_anagram),
    Pattern("✍️", R.string.alarm_task_pattern),
    Catch("✨", R.string.alarm_task_catch);

    companion object {
        val Default = listOf(Math, Memory, Hunt)
        fun parse(csv: String?): List<AlarmTask> =
            csv.orEmpty().split(',').mapNotNull { name -> entries.firstOrNull { it.name == name } }.distinct().take(3)
    }
}

data class AlarmOptions(
    val on: Boolean = false,
    val mission: AlarmMission = AlarmMission.None,
    /** Turning the alarm off counts the habit as done (the wake-up habit). */
    val markDone: Boolean = false,
    /** Made with the "Alarm" habit type (not just a reminder that rings). */
    val isAlarmType: Boolean = false,
    val tasks: List<AlarmTask> = AlarmTask.Default,
    /** 0 easy, 1 normal, 2 hard. */
    val level: Int = 1,
    /** The sound starts low and rises over half a minute. */
    val rampUp: Boolean = true,
    val snoozes: Int = 2,
)

/**
 * Habits whose reminder rings like an alarm clock: on time to the minute, with the phone's
 * alarm sound (played by [AlarmRingService], so volume and power keys can't silence it),
 * a full-screen page, an optional task to turn it off and a limited snooze.
 *
 * The choices are kept on the device: they describe this phone, not the habit itself.
 */
object HabitAlarms {
    private const val PREFS = "habit_alarms"
    const val CHANNEL_ID = "habit_alarm_ring_v2"
    private const val OLD_CHANNEL_ID = "habit_alarms_v1"
    const val SNOOZE_MINUTES = 5
    const val MAX_SNOOZES = 2

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isOn(context: Context, habitId: Long): Boolean = prefs(context).getBoolean(habitId.toString(), false)

    fun options(context: Context, habitId: Long): AlarmOptions {
        val p = prefs(context)
        return AlarmOptions(
            on = p.getBoolean(habitId.toString(), false),
            mission = runCatching { AlarmMission.valueOf(p.getString("$habitId.mission", null) ?: "None") }.getOrDefault(AlarmMission.None),
            markDone = p.getBoolean("$habitId.done", false),
            isAlarmType = p.getBoolean("$habitId.kind", false),
            tasks = AlarmTask.parse(p.getString("$habitId.tasks", null)).ifEmpty { AlarmTask.Default },
            level = p.getInt("$habitId.level", 1).coerceIn(0, 2),
            rampUp = p.getBoolean("$habitId.ramp", true),
            snoozes = p.getInt("$habitId.snoozes", MAX_SNOOZES).coerceIn(0, 3),
        )
    }

    /** The alarm type's own choices: which tasks, how hard, rising sound, how many snoozes. */
    fun setDetails(context: Context, habitId: Long, isAlarmType: Boolean, tasks: List<AlarmTask>, level: Int, rampUp: Boolean, snoozes: Int) {
        prefs(context).edit()
            .putBoolean("$habitId.kind", isAlarmType)
            .putString("$habitId.tasks", tasks.take(3).joinToString(",") { it.name })
            .putInt("$habitId.level", level.coerceIn(0, 2))
            .putBoolean("$habitId.ramp", rampUp)
            .putInt("$habitId.snoozes", snoozes.coerceIn(0, 3))
            .apply()
    }

    fun set(context: Context, habitId: Long, on: Boolean, mission: AlarmMission = AlarmMission.None, markDone: Boolean = false) {
        prefs(context).edit().apply {
            if (on) {
                putBoolean(habitId.toString(), true)
                putString("$habitId.mission", mission.name)
                putBoolean("$habitId.done", markDone)
            } else {
                remove(habitId.toString()); remove("$habitId.mission"); remove("$habitId.done"); remove("$habitId.paused")
            }
        }.apply()
    }

    /** Was an alarm and was switched off from the widget (kept so it can be switched back on). */
    fun wasAlarm(context: Context, habitId: Long): Boolean = prefs(context).getBoolean("$habitId.paused", false)

    /** Switches an alarm habit's ringing off or back on, keeping its task and settings. */
    fun toggle(context: Context, habitId: Long) {
        val p = prefs(context)
        if (isOn(context, habitId)) {
            p.edit().remove(habitId.toString()).putBoolean("$habitId.paused", true).apply()
            stop(context, habitId)
        } else if (wasAlarm(context, habitId)) {
            p.edit().putBoolean(habitId.toString(), true).remove("$habitId.paused").apply()
        }
    }

    private fun snoozeKey(habitId: Long) = "$habitId.snoozed.${LocalDate.now().toEpochDay()}"

    fun snoozesLeft(context: Context, habitId: Long): Int =
        (options(context, habitId).snoozes - prefs(context).getInt(snoozeKey(habitId), 0)).coerceAtLeast(0)

    /** A silent, urgent channel: the service plays the sound itself. */
    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(OLD_CHANNEL_ID)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.alarm_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.alarm_channel_description)
                setSound(null, null)
                enableVibration(false)
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
            Intent(context, app.tsuzuku.HomeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, show), operation)
    }

    internal fun notificationId(habitId: Long) = habitId.hashCode() xor 0x10000000

    internal fun pageIntent(context: Context, habitId: Long, name: String, emoji: String, plan: String): Intent =
        Intent(context, HabitAlarmActivity::class.java).apply {
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(HabitAlarmActivity.EXTRA_NAME, name)
            putExtra(HabitAlarmActivity.EXTRA_EMOJI, emoji)
            putExtra(HabitAlarmActivity.EXTRA_PLAN, plan)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }

    /** Rings now. */
    fun ring(context: Context, habitId: Long, name: String, emoji: String, plan: String) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        createChannel(context)
        val start = Intent(context, AlarmRingService::class.java).apply {
            putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(HabitAlarmActivity.EXTRA_NAME, name)
            putExtra(HabitAlarmActivity.EXTRA_EMOJI, emoji)
            putExtra(HabitAlarmActivity.EXTRA_PLAN, plan)
        }
        runCatching { context.startForegroundService(start) }
            .onFailure { HabitNotifications.show(context, habitId, name, emoji, null, plan) }
    }

    /** Ends the ringing, whatever the reason. */
    fun stop(context: Context, habitId: Long) {
        // stopService never starts anything, so it is safe from the background too.
        if (AlarmRingService.ringingHabitId.let { it < 0 || it == habitId }) context.stopService(Intent(context, AlarmRingService::class.java))
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(habitId))
    }

    /** Quiet for a moment while the task is being solved; it rings again if left alone. */
    fun hush(context: Context, seconds: Int = 40) {
        if (AlarmRingService.ringingHabitId < 0) return
        runCatching { context.startService(Intent(context, AlarmRingService::class.java).setAction(AlarmRingService.ACTION_HUSH).putExtra(AlarmRingService.EXTRA_SECONDS, seconds)) }
    }

    fun snooze(context: Context, habitId: Long): Boolean {
        if (snoozesLeft(context, habitId) <= 0) return false
        val p = prefs(context)
        p.edit().putInt(snoozeKey(habitId), p.getInt(snoozeKey(habitId), 0) + 1).apply()
        stop(context, habitId)
        HabitReminderScheduler(context, TsuzukuDatabase.getInstance(context).habitDao()).snooze(habitId, SNOOZE_MINUTES)
        return true
    }

    /** The alarm was answered for good: stop it and, for the wake-up habit, tick it off. */
    suspend fun finish(context: Context, habitId: Long) {
        stop(context, habitId)
        if (!options(context, habitId).markDone) return
        val repository = RoomHabitRepository(TsuzukuDatabase.getInstance(context).habitDao())
        val today = repository.observeToday().first().firstOrNull { it.habit.id == habitId }
        if (today != null && !today.isComplete) {
            if (today.habit.type == app.tsuzuku.model.HabitType.Check) {
                repository.toggleHabit(habitId)
            } else {
                repository.addProgress(habitId, (today.habit.target - today.progress).coerceAtLeast(0.0))
            }
            app.tsuzuku.widget.TsuzukuWidgets.updateAll(context)
        }
    }

    const val ACTION_SNOOZE = "app.tsuzuku.action.ALARM_SNOOZE"
    const val ACTION_DISMISS = "app.tsuzuku.action.ALARM_DISMISS"
}

/** Snooze / not-today from the heads-up notification when the phone is in use. */
class HabitAlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(HabitReminderReceiver.EXTRA_HABIT_ID, -1L)
        if (habitId < 0) return
        when (intent.action) {
            HabitAlarms.ACTION_SNOOZE -> HabitAlarms.snooze(context, habitId)
            HabitAlarms.ACTION_DISMISS -> if (HabitAlarms.options(context, habitId).mission == AlarmMission.None) HabitAlarms.stop(context, habitId)
        }
        context.sendBroadcast(Intent(HabitAlarmActivity.ACTION_CLOSE).setPackage(context.packageName))
    }
}
