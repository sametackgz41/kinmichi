package app.tsuzuku.widget

import androidx.compose.runtime.remember
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
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.tsuzuku.HomeActivity
import app.tsuzuku.R
import app.tsuzuku.TsuzukuApplication
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.TodayHabit
import kotlinx.coroutines.flow.first
import java.time.LocalDate

// ---------------------------------------------------------------------------------------------
// Streak: the longest running streak, large, with this month's comebacks in gold.
// ---------------------------------------------------------------------------------------------

class StreakWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TsuzukuApplication
        val todayFlow = app.habitRepository.observeToday()
        val historyFlow = app.habitRepository.observeHistory()
        val initialToday = todayFlow.first()
        val initialHistory = historyFlow.first()
        provideContent {
            val habits by remember { todayFlow }.collectAsState(initial = initialToday)
            val history by remember { historyFlow }.collectAsState(initial = initialHistory)
            val top = habits.maxByOrNull { it.streakDays }
            val today = LocalDate.now()
            val comebacks = history?.comebackCount(today.withDayOfMonth(1), today) ?: 0
            TsuzukuGlanceTheme { StreakContent(top, comebacks, locked = app.settingsRepository.settings.value.appLockEnabled) }
        }
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

@Composable
internal fun StreakContent(top: TodayHabit?, comebacks: Int, locked: Boolean) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize().background(tsuzukuWidgetBackground()).cornerRadius(28.dp)
            .padding(16.dp).clickable(actionStartActivity<HomeActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        if (locked || top == null || top.streakDays == 0) {
            Text(
                context.getString(if (locked) R.string.widget_locked else R.string.widget_streak_empty),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
            return@Column
        }
        Text(
            "${top.habit.emoji} ${top.habit.name}".trim(),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            maxLines = 1,
        )
        Text(
            top.streakDays.toString(),
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 52.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif),
        )
        Text(
            context.resources.getQuantityString(R.plurals.widget_streak_days, top.streakDays, top.streakDays),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp),
        )
        if (comebacks > 0) {
            Text(
                context.resources.getQuantityString(R.plurals.comebacks_this_month, comebacks, comebacks),
                style = TextStyle(color = GlanceTheme.colors.tertiary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                maxLines = 2,
            )
        }
    }
}
