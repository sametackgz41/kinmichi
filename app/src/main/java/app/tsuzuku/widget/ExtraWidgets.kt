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
import app.tsuzuku.MainActivity
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
            .padding(16.dp).clickable(actionStartActivity<MainActivity>()),
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

// ---------------------------------------------------------------------------------------------
// Quick grid: four habits as big tap targets; a tap checks or adds one.
// ---------------------------------------------------------------------------------------------

class QuickGridWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TsuzukuApplication
        val todayFlow = app.habitRepository.observeToday()
        val initialToday = todayFlow.first()
        provideContent {
            val habits by remember { todayFlow }.collectAsState(initial = initialToday)
            val tiles = habits.filter { it.habit.type != HabitType.Duration }.sortedBy { it.isComplete }.take(4)
            TsuzukuGlanceTheme { QuickGridContent(tiles, locked = app.settingsRepository.settings.value.appLockEnabled) }
        }
    }
}

class QuickGridWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickGridWidget()
}

@Composable
internal fun QuickGridContent(tiles: List<TodayHabit>, locked: Boolean) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize().background(tsuzukuWidgetBackground()).cornerRadius(28.dp).padding(8.dp),
    ) {
        if (locked || tiles.isEmpty()) {
            Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()), contentAlignment = Alignment.Center) {
                Text(
                    context.getString(if (locked) R.string.widget_locked else R.string.widget_no_habits),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center),
                )
            }
            return@Column
        }
        tiles.chunked(2).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(GlanceModifier.height(8.dp))
            Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                row.forEachIndexed { index, item ->
                    if (index > 0) Spacer(GlanceModifier.width(8.dp))
                    QuickTile(item, GlanceModifier.defaultWeight())
                }
                if (row.size == 1) {
                    Spacer(GlanceModifier.width(8.dp))
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

@Composable
private fun QuickTile(item: TodayHabit, modifier: GlanceModifier) {
    val done = item.isComplete
    val params = actionParametersOf(HabitIdKey to item.habit.id)
    val action = if (item.habit.type == HabitType.Count) actionRunCallback<IncrementHabitAction>(params) else actionRunCallback<ToggleHabitAction>(params)
    val detail = if (item.habit.type == HabitType.Count) {
        "${app.tsuzuku.ui.screens.formatHabitAmount(item.progress)}/${app.tsuzuku.ui.screens.formatHabitAmount(item.habit.target)}"
    } else {
        ""
    }
    Column(
        modifier = modifier.fillMaxHeight().cornerRadius(20.dp)
            .background(if (done) GlanceTheme.colors.primary else GlanceTheme.colors.surfaceVariant)
            .clickable(action),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(item.habit.emoji, style = TextStyle(fontSize = 20.sp, textAlign = TextAlign.Center))
        Text(
            item.habit.name,
            style = TextStyle(
                color = if (done) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
        if (detail.isNotEmpty()) {
            Text(
                detail,
                style = TextStyle(color = if (done) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Mood: five faces; one tap logs today's mood.
// ---------------------------------------------------------------------------------------------

private val MoodKey = ActionParameters.Key<Int>("mood")
internal val MoodFaces = listOf("😣", "😕", "😐", "🙂", "😄")

class MoodWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TsuzukuApplication
        val reviewFlow = app.reviewRepository.observeDailyReview(LocalDate.now().toEpochDay())
        val initialReview = reviewFlow.first()
        provideContent {
            val review by remember { reviewFlow }.collectAsState(initial = initialReview)
            TsuzukuGlanceTheme { MoodContent(selected = review?.mood) }
        }
    }
}

class MoodWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoodWidget()
}

class LogMoodAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mood = parameters[MoodKey] ?: return
        val app = context.applicationContext as TsuzukuApplication
        val day = LocalDate.now().toEpochDay()
        val existing = app.reviewRepository.observeDailyReview(day).first()
        app.reviewRepository.saveDailyReview(day, mood, existing?.energy ?: 3, existing?.productivity ?: 3, existing?.stress ?: 3, existing?.note.orEmpty())
        MoodWidget().update(context, glanceId)
    }
}

@Composable
internal fun MoodContent(selected: Int?) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxSize().background(tsuzukuWidgetBackground()).cornerRadius(28.dp).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            context.getString(R.string.mood_question),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp, fontFamily = FontFamily.Serif),
            maxLines = 2,
            modifier = GlanceModifier.defaultWeight(),
        )
        MoodFaces.forEachIndexed { index, face ->
            val mood = index + 1
            Box(
                modifier = GlanceModifier.width(40.dp).height(40.dp).cornerRadius(20.dp)
                    .background(if (selected == mood) GlanceTheme.colors.primaryContainer else ColorProvider(androidx.compose.ui.graphics.Color.Transparent))
                    .clickable(actionRunCallback<LogMoodAction>(actionParametersOf(MoodKey to mood))),
                contentAlignment = Alignment.Center,
            ) {
                Text(face, style = TextStyle(fontSize = 20.sp, textAlign = TextAlign.Center))
            }
        }
    }
}
