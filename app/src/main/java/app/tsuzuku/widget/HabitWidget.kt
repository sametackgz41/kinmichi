package app.tsuzuku.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
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
private fun HabitContent(item: TodayHabit?, locked: Boolean) {
    val context = LocalContext.current
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
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (item == null || locked) {
            Text(
                context.getString(if (locked) R.string.widget_locked else R.string.widget_no_habits),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center),
            )
            return@Box
        }
        val ringSize = 64.dp
        val density = context.resources.displayMetrics.density
        val ring = ringBitmap(
            sizePx = (ringSize.value * density).toInt(),
            fraction = if (item.isComplete) 1f else item.progressFraction,
            trackColor = GlanceTheme.colors.secondaryContainer.getColor(context).toArgb(),
            progressColor = GlanceTheme.colors.primary.getColor(context).toArgb(),
            strokePx = 6f * density,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = GlanceModifier.size(ringSize), contentAlignment = Alignment.Center) {
                Image(
                    provider = ImageProvider(ring),
                    contentDescription = null,
                    modifier = GlanceModifier.size(ringSize),
                )
                if (item.isComplete) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_check),
                        contentDescription = context.getString(R.string.habit_completed),
                        modifier = GlanceModifier.size(28.dp),
                        colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.primary),
                    )
                } else {
                    Text(item.habit.emoji, style = TextStyle(fontSize = 24.sp))
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            Text(
                item.habit.name,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
            )
            if (item.streakDays > 0) {
                Text(
                    "🔥 ${item.streakDays}",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center),
                )
            }
        }
    }
}
