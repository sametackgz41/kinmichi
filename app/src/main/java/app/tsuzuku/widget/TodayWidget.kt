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
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.tsuzuku.MainActivity
import app.tsuzuku.R
import app.tsuzuku.TsuzukuApplication
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.ui.screens.formatHabitAmount

/**
 * "Today" widget: the day's habits with live check boxes and +1 buttons, so habits can be
 * completed without opening the app. Check boxes and the progress bar animate natively on
 * Android 12+ when their state changes.
 */
class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TsuzukuApplication
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val selected = WidgetSettings(context).selectedHabitIds(widgetId)
        provideContent {
            val habits by app.habitRepository.observeToday().collectAsState(initial = null)
            val locked = app.settingsRepository.settings.value.appLockEnabled
            val visible = habits.orEmpty()
                .let { all -> if (selected.isEmpty()) all else all.filter { it.habit.id in selected } }
                .sortedBy { it.isComplete }
            TsuzukuGlanceTheme { TodayContent(visible, loaded = habits != null, locked = locked) }
        }
    }
}

/** Keeps the original receiver name so widgets users already placed keep working. */
class TsuzukuWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val settings = WidgetSettings(context)
        appWidgetIds.forEach(settings::delete)
    }

    companion object {
        fun requestUpdate(context: Context) = TsuzukuWidgets.requestUpdate(context)
    }
}

@Composable
private fun TodayContent(habits: List<TodayHabit>, loaded: Boolean, locked: Boolean) {
    val context = LocalContext.current
    val completed = habits.count(TodayHabit::isComplete)
    val total = habits.size
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                context.getString(R.string.today),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (!locked && total > 0) {
                Text(
                    "$completed/$total",
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                )
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        LinearProgressIndicator(
            progress = if (total == 0 || locked) 0f else completed.toFloat() / total,
            modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
            color = GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.secondaryContainer,
        )
        Spacer(GlanceModifier.height(6.dp))
        when {
            locked -> Message(context.getString(R.string.widget_locked))
            !loaded -> Unit
            habits.isEmpty() -> Message(context.getString(R.string.widget_no_habits))
            else -> LazyColumn {
                items(habits, itemId = { it.habit.id }) { item -> HabitLine(item) }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
        modifier = GlanceModifier.padding(top = 6.dp).clickable(actionStartActivity<MainActivity>()),
    )
}

@Composable
private fun HabitLine(item: TodayHabit) {
    val context = LocalContext.current
    val habit = item.habit
    val params = actionParametersOf(HabitIdKey to habit.id)
    val textColor = if (item.isComplete) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(habit.emoji, style = TextStyle(fontSize = 17.sp))
        Spacer(GlanceModifier.width(10.dp))
        Text(
            habit.name,
            style = TextStyle(color = textColor, fontSize = 14.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()),
        )
        when (habit.type) {
            HabitType.Check -> CheckBox(
                checked = item.isComplete,
                onCheckedChange = if (item.isBlocked) null else actionRunCallback<ToggleHabitAction>(params),
                colors = CheckboxDefaults.colors(
                    checkedColor = GlanceTheme.colors.primary,
                    uncheckedColor = GlanceTheme.colors.outline,
                ),
            )
            HabitType.Count -> {
                Text(
                    "${formatHabitAmount(item.progress)}/${formatHabitAmount(habit.target)}",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                )
                Spacer(GlanceModifier.width(6.dp))
                CircleIconButton(
                    imageProvider = ImageProvider(if (item.isComplete) R.drawable.ic_widget_check else R.drawable.ic_widget_add),
                    contentDescription = context.getString(R.string.increase_progress),
                    onClick = actionRunCallback<IncrementHabitAction>(params),
                    enabled = !item.isBlocked,
                    backgroundColor = if (item.isComplete) GlanceTheme.colors.primary else GlanceTheme.colors.secondaryContainer,
                    contentColor = if (item.isComplete) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSecondaryContainer,
                    modifier = GlanceModifier.size(36.dp),
                )
            }
            HabitType.Duration -> Text(
                "${formatHabitAmount(item.progress)}/${formatHabitAmount(habit.target)} ${habit.unit}",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
            )
        }
    }
}
