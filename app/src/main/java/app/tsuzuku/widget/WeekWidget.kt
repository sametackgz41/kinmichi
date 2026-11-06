package app.tsuzuku.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import java.time.DayOfWeek
import app.tsuzuku.data.preferences.WeekStartPreference
import java.time.temporal.TemporalAdjusters
import app.tsuzuku.model.HabitDayStatus
import androidx.compose.ui.graphics.toArgb
import androidx.glance.ColorFilter
import androidx.glance.ImageProvider
import androidx.glance.Image
import androidx.glance.LocalSize
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import app.tsuzuku.MainActivity
import app.tsuzuku.R
import app.tsuzuku.TsuzukuApplication
import app.tsuzuku.model.HabitHistorySnapshot
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

/** Share of scheduled habits completed on one day; null when nothing was scheduled or it's ahead. */
internal data class DayScore(val date: LocalDate, val fraction: Float?)

/** The current calendar week, starting on the day chosen in settings. */
internal fun thisWeek(history: HabitHistorySnapshot, today: LocalDate = LocalDate.now(), firstDay: DayOfWeek = DayOfWeek.MONDAY): List<DayScore> {
    val start = today.with(TemporalAdjusters.previousOrSame(firstDay))
    return (0..6).map { offset ->
        val date = start.plusDays(offset.toLong())
        val day = if (date.isAfter(today)) emptyList() else history.habitsForDay(date).filter { it.status != HabitDayStatus.Skipped }
        DayScore(date, if (day.isEmpty()) null else day.count { it.isComplete }.toFloat() / day.size)
    }
}

/** This-week widget: a progress ring per day, the week's completion rate and the best active streak. */
class WeekWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TsuzukuApplication
        provideContent {
            val history by app.habitRepository.observeHistory().collectAsState(initial = null)
            val today by app.habitRepository.observeToday().collectAsState(initial = null)
            val locked = app.settingsRepository.settings.value.appLockEnabled
            TsuzukuGlanceTheme {
                WeekContent(
                    days = history?.let { thisWeek(it, firstDay = if (app.settingsRepository.settings.value.weekStart == WeekStartPreference.Sunday) DayOfWeek.SUNDAY else DayOfWeek.MONDAY) }.orEmpty(),
                    bestStreak = today.orEmpty().maxOfOrNull { it.streakDays } ?: 0,
                    locked = locked,
                )
            }
        }
    }
}

class WeekWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekWidget()
}

@Composable
internal fun WeekContent(days: List<DayScore>, bestStreak: Int, locked: Boolean, today: LocalDate = LocalDate.now()) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val primary = GlanceTheme.colors.primary.getColor(context).toArgb()
    val track = GlanceTheme.colors.secondaryContainer.getColor(context).toArgb()
    val scored = days.mapNotNull(DayScore::fraction)
    val weekPercent = if (scored.isEmpty()) null else (scored.average() * 100).toInt()
    val size = LocalSize.current
    val ringSize = ((size.width.value - 28f) / 7f - 8f).coerceIn(24f, 44f).dp
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    context.getString(R.string.widget_week_title),
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Medium),
                )
                if (!locked && bestStreak > 0) {
                    Text(
                        "🔥 " + context.resources.getQuantityString(R.plurals.streak_days, bestStreak, bestStreak),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                    )
                }
            }
            if (!locked && weekPercent != null) {
                Text(
                    "%$weekPercent".takeIf { locale.language == "tr" } ?: "$weekPercent%",
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
        Spacer(GlanceModifier.height(10.dp))
        if (locked) {
            Text(context.getString(R.string.widget_locked), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
            return@Column
        }
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            days.forEach { day ->
                val isToday = day.date == today
                val future = day.date.isAfter(today)
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val full = day.fraction == 1f
                    val ring = ringBitmap(
                        sizePx = (ringSize.value * density).toInt(),
                        fraction = day.fraction ?: 0f,
                        trackColor = if (future) track.withAlpha(0.45f) else track,
                        progressColor = primary,
                        strokePx = (ringSize.value / 8f) * density,
                        fillColor = if (full) primary else null,
                    )
                    Box(modifier = GlanceModifier.size(ringSize), contentAlignment = Alignment.Center) {
                        Image(ImageProvider(ring), contentDescription = null, modifier = GlanceModifier.size(ringSize))
                        if (full) {
                            Image(
                                ImageProvider(R.drawable.ic_widget_check),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                                modifier = GlanceModifier.size(ringSize * 0.55f),
                            )
                        }
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        day.date.dayOfWeek.getDisplayName(DateTextStyle.SHORT, locale).take(3),
                        style = TextStyle(
                            color = when {
                                isToday -> GlanceTheme.colors.primary
                                future -> GlanceTheme.colors.outline
                                else -> GlanceTheme.colors.onSurfaceVariant
                            },
                            fontSize = 11.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
