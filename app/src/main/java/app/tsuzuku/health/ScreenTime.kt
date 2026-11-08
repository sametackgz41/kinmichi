package app.tsuzuku.health

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import java.time.LocalDate
import java.time.ZoneId

/**
 * Screen time as a habit: today's minutes on the phone (from Android's usage stats, which stay on
 * the device) fill in the habits linked to it, typically a "stay under 2 hours" quit habit.
 */
object ScreenTime {
    private const val PREFS = "screen_time_habits"

    fun isLinked(context: Context, habitId: Long): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(habitId.toString(), false)

    fun set(context: Context, habitId: Long, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (on) putBoolean(habitId.toString(), true) else remove(habitId.toString())
        }.apply()
    }

    fun linkedHabits(context: Context): List<Long> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all.keys.mapNotNull { it.toLongOrNull() }

    /** Whether the person has allowed usage access in system settings. */
    fun hasAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun accessSettingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Minutes apps were in the foreground since midnight, not counting the home screen. */
    fun todayMinutes(context: Context): Int {
        val usage = context.getSystemService(UsageStatsManager::class.java) ?: return 0
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val home = context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
        val events = usage.queryEvents(start, now)
        val event = UsageEvents.Event()
        val openedAt = HashMap<String, Long>()
        var total = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg == home) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> openedAt[pkg] = event.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> openedAt.remove(pkg)?.let { total += event.timeStamp - it }
            }
        }
        // Still open right now.
        openedAt.values.forEach { total += now - it }
        return (total / 60_000L).toInt()
    }

    /** Fills today's value of every linked habit. */
    suspend fun syncToday(context: Context, repository: app.tsuzuku.data.HabitRepository) {
        val ids = linkedHabits(context)
        if (ids.isEmpty() || !hasAccess(context)) return
        val minutes = runCatching { todayMinutes(context) }.getOrNull() ?: return
        ids.forEach { repository.setAutoValue(it, minutes.toDouble()) }
    }
}
