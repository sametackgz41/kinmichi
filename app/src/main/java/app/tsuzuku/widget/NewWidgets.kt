package app.tsuzuku.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.tsuzuku.HomeActivity
import app.tsuzuku.R
import app.tsuzuku.TsuzukuApplication
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.notifications.HabitAlarms
import app.tsuzuku.notifications.nextReminderTime
import app.tsuzuku.ui.formatClock
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.TextStyle as DayStyle

private fun Context.app() = applicationContext as TsuzukuApplication

// ---------------------------------------------------------------------------------------------
// Next habit: the one habit to do now, with one big tap; when it's done, the next one comes up.
// ---------------------------------------------------------------------------------------------

/** What's due next: the nearest reminder still ahead today, then habits without a time. */
internal fun nextHabit(habits: List<TodayHabit>, now: LocalTime = LocalTime.now()): TodayHabit? {
    val pending = habits.filter { !it.isComplete && !it.isBlocked && it.habit.type != HabitType.Duration }
    val nowMin = now.hour * 60 + now.minute
    return pending.sortedWith(compareBy<TodayHabit> { item ->
        val m = item.habit.reminderMinutes
        when {
            m == null -> 2_000                      // any time: after timed ones still ahead
            m >= nowMin - 60 -> m                   // due soon (or just passed)
            else -> 1_000 + m                       // earlier today and missed: after those ahead
        }
    }.thenBy { it.habit.sortOrder }).firstOrNull()
}

class NextHabitWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.app()
        val flow = app.habitRepository.observeToday()
        val initial = flow.first()
        provideContent {
            val habits by remember { flow }.collectAsState(initial = initial)
            TsuzukuGlanceTheme { NextHabitContent(habits, locked = app.settingsRepository.settings.value.appLockEnabled) }
        }
    }
}

class NextHabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextHabitWidget()
}

@Composable
internal fun NextHabitContent(habits: List<TodayHabit>, locked: Boolean) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val next = if (locked) null else nextHabit(habits)
    Row(
        modifier = GlanceModifier.fillMaxSize().background(tsuzukuWidgetBackground()).cornerRadius(28.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (next == null) {
            Column(GlanceModifier.defaultWeight().clickable(actionStartActivity<HomeActivity>())) {
                Text(context.getString(R.string.widget_next_title), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
                Text(
                    context.getString(when { locked -> R.string.widget_locked; habits.isEmpty() -> R.string.widget_no_habits; else -> R.string.tile_all_done }),
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    maxLines = 2,
                )
            }
            return@Row
        }
        val habit = next.habit
        val accent = habit.colorArgb.toInt()
        Box(GlanceModifier.size(44.dp), contentAlignment = Alignment.Center) {
            Image(ImageProvider(dotBitmap((44 * density).toInt(), accent.withAlpha(0.22f))), null, GlanceModifier.size(44.dp))
            Text(habit.emoji, style = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center))
        }
        Spacer(GlanceModifier.width(12.dp))
        Column(GlanceModifier.defaultWeight().clickable(actionStartActivity<HomeActivity>())) {
            Text(
                habit.reminderMinutes?.let { context.getString(R.string.widget_next_at, formatClock(it, context.resources.configuration.locales[0])) } ?: context.getString(R.string.widget_next_title),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            )
            Text(habit.name, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            val detail = if (habit.type == HabitType.Count) "${formatAmount(next.progress)} / ${formatAmount(habit.target)} ${habit.unit}".trim() else habit.tinyVersion
            if (detail.isNotBlank()) Text(detail, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 1)
        }
        val params = actionParametersOf(HabitIdKey to habit.id)
        val action = if (habit.type == HabitType.Count) actionRunCallback<IncrementHabitAction>(params) else actionRunCallback<ToggleHabitAction>(params)
        Box(GlanceModifier.size(52.dp).clickable(action), contentAlignment = Alignment.Center) {
            val ring = ringBitmap(
                (52 * density).toInt(),
                if (habit.type == HabitType.Count) next.progressFraction else 0f,
                GlanceTheme.colors.surfaceVariant.getColor(context).toArgb(),
                accent,
                3.5f * density,
            )
            Image(ImageProvider(ring), context.getString(R.string.complete_habit), GlanceModifier.size(52.dp))
            Text(if (habit.type == HabitType.Count) "+" else "✓", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold))
        }
    }
}

private fun formatAmount(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

// ---------------------------------------------------------------------------------------------
// Progress ring: today's share done, nothing else. Small (1x1).
// ---------------------------------------------------------------------------------------------

class RingWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.app()
        val flow = app.habitRepository.observeToday()
        val initial = flow.first()
        provideContent {
            val habits by remember { flow }.collectAsState(initial = initial)
            TsuzukuGlanceTheme { RingContent(habits, locked = app.settingsRepository.settings.value.appLockEnabled) }
        }
    }
}

class RingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RingWidget()
}

@Composable
internal fun RingContent(habits: List<TodayHabit>, locked: Boolean) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val total = habits.size
    val done = habits.count(TodayHabit::isComplete)
    val fraction = if (locked || total == 0) 0f else done.toFloat() / total
    val primary = GlanceTheme.colors.primary.getColor(context).toArgb()
    val gold = GlanceTheme.colors.tertiary.getColor(context).toArgb()
    Box(
        GlanceModifier.fillMaxSize().background(tsuzukuWidgetBackground()).cornerRadius(28.dp).padding(8.dp).clickable(actionStartActivity<HomeActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        val px = (84 * density).toInt()
        Image(
            ImageProvider(ringBitmap(px, fraction, GlanceTheme.colors.surfaceVariant.getColor(context).toArgb(), if (fraction >= 1f) gold else primary, 7f * density)),
            null,
            GlanceModifier.size(84.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (locked) "·" else "%${(fraction * 100).toInt()}",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
            )
            if (!locked && total > 0) Text("$done/$total", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Mini heatmap: the last 13 weeks as small squares; comeback days are ringed in gold.
// ---------------------------------------------------------------------------------------------

class HeatmapWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.app()
        val flow = app.habitRepository.observeHistory()
        val initial = flow.first()
        provideContent {
            val history by remember { flow }.collectAsState(initial = initial)
            TsuzukuGlanceTheme { HeatmapContent(history, locked = app.settingsRepository.settings.value.appLockEnabled) }
        }
    }
}

class HeatmapWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HeatmapWidget()
}

internal const val HEATMAP_WEEKS = 13

/** Weeks as columns (Monday on top), today in the last column. */
internal fun heatmapBitmap(
    history: HabitHistorySnapshot,
    today: LocalDate,
    cellPx: Int,
    gapPx: Int,
    base: Int,
    empty: Int,
    gold: Int,
): Bitmap {
    val w = HEATMAP_WEEKS * (cellPx + gapPx) - gapPx
    val h = 7 * (cellPx + gapPx) - gapPx
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = cellPx * 0.16f; color = gold }
    val lastMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val firstMonday = lastMonday.minusWeeks((HEATMAP_WEEKS - 1).toLong())
    val r = cellPx * 0.28f
    for (week in 0 until HEATMAP_WEEKS) for (day in 0 until 7) {
        val date = firstMonday.plusDays((week * 7 + day).toLong())
        if (date.isAfter(today)) continue
        val s = history.daySummary(date)
        val x = (week * (cellPx + gapPx)).toFloat(); val y = (day * (cellPx + gapPx)).toFloat()
        val rect = RectF(x, y, x + cellPx, y + cellPx)
        fill.color = if (s.scheduledCount == 0 || s.progressFraction <= 0f) empty else base.withAlpha(0.25f + 0.75f * s.progressFraction)
        c.drawRoundRect(rect, r, r, fill)
        // A comeback day: something done after a day where nothing scheduled was done.
        val before = history.daySummary(date.minusDays(1))
        if (s.completedCount > 0 && before.scheduledCount > 0 && before.completedCount == 0) {
            val inset = ring.strokeWidth / 2
            c.drawRoundRect(RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset), r, r, ring)
        }
    }
    return bitmap
}

@Composable
internal fun HeatmapContent(history: HabitHistorySnapshot?, locked: Boolean, today: LocalDate = LocalDate.now()) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    Column(
        GlanceModifier.fillMaxSize().background(tsuzukuWidgetBackground()).cornerRadius(28.dp).padding(14.dp).clickable(actionStartActivity<HomeActivity>()),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(context.getString(R.string.widget_heatmap_title), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight())
            if (history != null && !locked) {
                val from = today.minusWeeks(HEATMAP_WEEKS.toLong())
                val days = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.count { history.daySummary(it).completedCount > 0 }
                Text(context.getString(R.string.widget_heatmap_days, days), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        if (history == null || locked) {
            Text(context.getString(if (locked) R.string.widget_locked else R.string.widget_no_habits), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
            return@Column
        }
        val bmp = heatmapBitmap(
            history, today,
            cellPx = (14 * density).toInt(), gapPx = (3 * density).toInt(),
            base = GlanceTheme.colors.primary.getColor(context).toArgb(),
            empty = GlanceTheme.colors.surfaceVariant.getColor(context).toArgb(),
            gold = GlanceTheme.colors.tertiary.getColor(context).toArgb(),
        )
        Image(ImageProvider(bmp), context.getString(R.string.widget_heatmap_title), GlanceModifier.fillMaxWidth().defaultWeight())
    }
}

// ---------------------------------------------------------------------------------------------
// Wake alarm: the next alarm habit's time; a tap turns that alarm on or off.
// ---------------------------------------------------------------------------------------------

internal data class NextAlarm(val habitId: Long, val name: String, val emoji: String, val at: ZonedDateTime, val on: Boolean)

/** The alarm habit ringing soonest (or, if all are off, the first one so it can be switched on). */
internal suspend fun nextAlarm(context: Context, now: ZonedDateTime = ZonedDateTime.now()): NextAlarm? {
    val dao = app.tsuzuku.data.local.TsuzukuDatabase.getInstance(context).habitDao()
    val candidates = dao.getHabitsWithReminders().filter { HabitAlarms.isOn(context, it.id) || HabitAlarms.wasAlarm(context, it.id) }
    return candidates.map { h ->
        NextAlarm(h.id, h.name, h.emoji, nextReminderTime(h.scheduleDaysMask, h.reminderMinutes ?: 0, now), HabitAlarms.isOn(context, h.id))
    }.sortedWith(compareByDescending<NextAlarm> { it.on }.thenBy { it.at }).firstOrNull()
}

class AlarmWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val alarm = nextAlarm(context.applicationContext)
        provideContent { TsuzukuGlanceTheme { AlarmContent(alarm) } }
    }
}

class AlarmWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AlarmWidget()
}

/** Turns the shown alarm on or off and sets (or clears) its next ring. */
class ToggleAlarmAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HabitIdKey] ?: return
        HabitAlarms.toggle(context, id)
        val dao = app.tsuzuku.data.local.TsuzukuDatabase.getInstance(context).habitDao()
        dao.getHabit(id)?.let { app.tsuzuku.notifications.HabitReminderScheduler(context, dao).schedule(it) }
        AlarmWidget().updateAll(context)
    }
}

@Composable
internal fun AlarmContent(alarm: NextAlarm?, today: LocalDate = LocalDate.now()) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    if (alarm == null) {
        Column(
            GlanceModifier.fillMaxSize().background(tsuzukuWidgetBackground()).cornerRadius(28.dp).padding(16.dp)
                .clickable(androidx.glance.appwidget.action.actionStartActivity(addHabitIntent(context))),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text("⏰", style = TextStyle(fontSize = 22.sp))
            Text(context.getString(R.string.widget_alarm_none), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp), maxLines = 2)
        }
        return
    }
    val day = when (alarm.at.toLocalDate()) {
        today -> context.getString(R.string.widget_alarm_today)
        today.plusDays(1) -> context.getString(R.string.widget_alarm_tomorrow)
        else -> alarm.at.dayOfWeek.getDisplayName(DayStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }
    }
    Row(
        GlanceModifier.fillMaxSize().background(tsuzukuWidgetBackground()).cornerRadius(28.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(GlanceModifier.defaultWeight().clickable(actionStartActivity<HomeActivity>())) {
            Text("${alarm.emoji} ${alarm.name}".trim(), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 1)
            Text(
                formatClock(alarm.at.hour * 60 + alarm.at.minute, locale),
                style = TextStyle(
                    color = if (alarm.on) GlanceTheme.colors.onSurface else GlanceTheme.colors.outline,
                    fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif,
                ),
            )
            Text(if (alarm.on) day else context.getString(R.string.widget_alarm_off), style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp))
        }
        val params = actionParametersOf(HabitIdKey to alarm.habitId)
        Box(
            GlanceModifier.size(48.dp).cornerRadius(24.dp)
                .background(if (alarm.on) GlanceTheme.colors.primary else GlanceTheme.colors.surfaceVariant)
                .clickable(actionRunCallback<ToggleAlarmAction>(params)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (alarm.on) "⏰" else "💤", style = TextStyle(fontSize = 20.sp))
        }
    }
}
