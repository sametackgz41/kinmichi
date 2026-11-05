package app.tsuzuku.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import app.tsuzuku.MainActivity
import app.tsuzuku.R
import app.tsuzuku.TsuzukuApplication
import app.tsuzuku.model.TodayHabit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TsuzukuWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            appWidgetIds.forEach { updateWidget(context, manager, it) }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        onUpdate(context, manager, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val settings = WidgetSettings(context)
        appWidgetIds.forEach(settings::delete)
    }

    companion object {
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TsuzukuWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                context.sendBroadcast(Intent(context, TsuzukuWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                })
            }
        }

        suspend fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val application = context.applicationContext as TsuzukuApplication
            val allToday = application.habitRepository.observeToday().first()
            val settings = WidgetSettings(context)
            val selected = settings.selectedHabitIds(widgetId)
            val items = if (selected.isEmpty()) allToday else allToday.filter { it.habit.id in selected }
            val appLocked = application.settingsRepository.settings.value.appLockEnabled
            val options = manager.getAppWidgetOptions(widgetId)
            val maxRows = when {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 180) < 140 -> 1
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 180) < 210 -> 2
                else -> 4
            }
            val palette = settings.palette(widgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_tsuzuku)
            applyPalette(context, views, palette)
            views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent(context))

            if (appLocked) {
                views.setTextViewText(R.id.widget_progress, context.getString(R.string.widget_locked))
                views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                setRows(context, views, emptyList(), 0)
            } else {
                val completed = items.count(TodayHabit::isComplete)
                val total = items.size
                val percent = if (total == 0) 0 else completed * 100 / total
                views.setTextViewText(R.id.widget_progress, context.getString(R.string.widget_progress_value, completed, total, percent))
                views.setProgressBar(R.id.widget_progress_bar, 100, percent, false)
                setRows(context, views, items.take(maxRows), maxRows)
            }
            manager.updateAppWidget(widgetId, views)
        }

        private fun setRows(context: Context, views: RemoteViews, items: List<TodayHabit>, maxRows: Int) {
            val rowIds = intArrayOf(R.id.widget_habit_1, R.id.widget_habit_2, R.id.widget_habit_3, R.id.widget_habit_4)
            rowIds.forEachIndexed { index, rowId ->
                val item = items.getOrNull(index)
                views.setViewVisibility(rowId, if (item != null && index < maxRows) View.VISIBLE else View.GONE)
                if (item != null) {
                    val mark = if (item.isComplete) "✓" else "○"
                    views.setTextViewText(rowId, context.getString(R.string.widget_habit_line, mark, item.habit.emoji, item.habit.name))
                }
            }
        }

        private fun applyPalette(context: Context, views: RemoteViews, palette: WidgetPalette) {
            val background = when (palette) {
                WidgetPalette.Indigo -> R.drawable.widget_background_indigo
                WidgetPalette.Light -> R.drawable.widget_background_light
                WidgetPalette.Sage -> R.drawable.widget_background_sage
            }
            val textColor = context.getColor(if (palette == WidgetPalette.Light) R.color.widget_text_dark else R.color.widget_text_light)
            views.setInt(R.id.widget_root, "setBackgroundResource", background)
            intArrayOf(
                R.id.widget_brand,
                R.id.widget_progress,
                R.id.widget_habit_1,
                R.id.widget_habit_2,
                R.id.widget_habit_3,
                R.id.widget_habit_4,
            ).forEach { views.setTextColor(it, textColor) }
        }

        private fun appPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            7001,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

