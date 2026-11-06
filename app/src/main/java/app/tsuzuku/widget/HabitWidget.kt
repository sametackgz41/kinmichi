package app.tsuzuku.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import app.tsuzuku.ui.screens.formatHabitAmount
import androidx.compose.ui.unit.Dp
import androidx.glance.layout.width
import androidx.glance.layout.Row
import androidx.glance.LocalSize
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
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
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.TodayHabit

/**
 * Single-habit widget: progress ring, emoji and streak. Tapping completes a check habit or
 * adds one to a count habit; duration habits open the app.
 */
class HabitWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TsuzukuApplication
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val chosen = WidgetSettings(context).selectedHabitIds(widgetId).firstOrNull()
        provideContent {
            val habits by app.habitRepository.observeToday().collectAsState(initial = null)
            val locked = app.settingsRepository.settings.value.appLockEnabled
            val item = habits?.let { list -> list.firstOrNull { it.habit.id == chosen } ?: list.firstOrNull() }
            TsuzukuGlanceTheme { HabitContent(item, locked) }
        }
    }
}

class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val settings = WidgetSettings(context)
        appWidgetIds.forEach(settings::delete)
    }
}

@Composable
internal fun HabitContent(item: TodayHabit?, locked: Boolean) {
    val context = LocalContext.current
    val size = LocalSize.current
    val openApp: Action = actionStartActivity<MainActivity>()
    val tapAction: Action = when {
        item == null || locked || item.isBlocked -> openApp
        item.habit.type == HabitType.Check -> actionRunCallback<ToggleHabitAction>(actionParametersOf(HabitIdKey to item.habit.id))
        item.habit.type == HabitType.Count -> actionRunCallback<IncrementHabitAction>(actionParametersOf(HabitIdKey to item.habit.id))
        else -> openApp
    }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .clickable(tapAction)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (item == null || locked) {
            Text(
                context.getString(if (locked) R.string.widget_locked else R.string.widget_no_habits),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center),
            )
            return@Box
        }
        // Wide widgets put the text beside the ring; square ones stack it underneath.
        val wide = size.width > size.height * 1.6f
        val ringSize = if (wide) (size.height - 24.dp).coerceIn(48.dp, 96.dp) else (minOf(size.width, size.height - 52.dp) - 16.dp).coerceIn(48.dp, 110.dp)
        if (wide) {
            Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                HabitRing(item, ringSize)
                Spacer(GlanceModifier.width(14.dp))
                Column(modifier = GlanceModifier.defaultWeight()) { HabitText(item, TextAlign.Start) }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HabitRing(item, ringSize)
                Spacer(GlanceModifier.height(8.dp))
                HabitText(item, TextAlign.Center)
            }
        }
    }
}

@Composable
private fun HabitRing(item: TodayHabit, ringSize: Dp) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val accent = item.habit.colorArgb.toInt()
    val done = item.isComplete
    val ring = ringBitmap(
        sizePx = (ringSize.value * density).toInt(),
        fraction = if (done) 1f else item.progressFraction,
        trackColor = accent.withAlpha(0.18f),
        progressColor = accent,
        strokePx = (ringSize.value / 11f) * density,
        fillColor = if (done) accent.withAlpha(0.16f) else null,
    )
    Box(modifier = GlanceModifier.size(ringSize), contentAlignment = Alignment.Center) {
        Image(ImageProvider(ring), contentDescription = null, modifier = GlanceModifier.size(ringSize))
        Text(item.habit.emoji, style = TextStyle(fontSize = (ringSize.value * 0.36f).sp, textAlign = TextAlign.Center))
    }
}

@Composable
private fun HabitText(item: TodayHabit, align: TextAlign) {
    val context = LocalContext.current
    val habit = item.habit
    val status = when {
        item.isComplete -> context.getString(R.string.habit_completed)
        habit.type == HabitType.Check -> context.getString(R.string.widget_tap_to_complete)
        else -> "${formatHabitAmount(item.progress)}/${formatHabitAmount(habit.target)} ${habit.unit}".trim()
    }
    Text(
        habit.name,
        maxLines = 1,
        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = align),
    )
    Text(
        status + if (item.streakDays > 1) "  ·  🔥 ${item.streakDays}" else "",
        maxLines = 1,
        style = TextStyle(
            color = if (item.isComplete) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = align,
        ),
    )
}
