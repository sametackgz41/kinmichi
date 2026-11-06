package app.tsuzuku.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
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
import androidx.glance.color.ColorProvider
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

/** Share of scheduled habits completed on one day; null when nothing was scheduled. */
internal data class DayScore(val date: LocalDate, val fraction: Float?)

internal fun lastSevenDays(history: HabitHistorySnapshot, today: LocalDate = LocalDate.now()): List<DayScore> =
    (6 downTo 0).map { back ->
        val date = today.minusDays(back.toLong())
        val day = history.habitsForDay(date)
        DayScore(date, if (day.isEmpty()) null else day.count { it.isComplete }.toFloat() / day.size)
    }

/** Week-at-a-glance widget: seven day tiles shaded by completion plus the best active streak. */
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
                    days = history?.let(::lastSevenDays).orEmpty(),
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
internal fun WeekContent(days: List<DayScore>, bestStreak: Int, locked: Boolean) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val primary = GlanceTheme.colors.primary.getColor(context)
    val empty = GlanceTheme.colors.secondaryContainer
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
            Text(
                context.getString(R.string.widget_week_title),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (!locked && bestStreak > 0) {
                Text(
                    "🔥 " + context.resources.getQuantityString(R.plurals.streak_days, bestStreak, bestStreak),
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 13.sp),
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
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val fill = day.fraction?.let { f ->
                        val tinted = primary.copy(alpha = 0.18f + 0.82f * f)
                        ColorProvider(day = tinted, night = tinted)
                    } ?: empty
                    Box(
                        modifier = GlanceModifier.size(28.dp).cornerRadius(8.dp).background(fill),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day.fraction == 1f) {
                            Text("✓", style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        day.date.dayOfWeek.getDisplayName(DateTextStyle.NARROW, locale),
                        style = TextStyle(
                            color = if (day.date == LocalDate.now()) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
    }
}
