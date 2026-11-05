package app.tsuzuku.widget

import android.content.Context

/** Per-widget habit selection, keyed by the platform app widget id. */
class WidgetSettings(context: Context) {
    private val preferences = context.getSharedPreferences("tsuzuku_widgets", Context.MODE_PRIVATE)

    fun selectedHabitIds(widgetId: Int): Set<Long> = preferences
        .getStringSet("habits_$widgetId", emptySet()).orEmpty()
        .mapNotNull(String::toLongOrNull)
        .toSet()

    fun save(widgetId: Int, habitIds: Set<Long>) {
        preferences.edit()
            .putStringSet("habits_$widgetId", habitIds.map(Long::toString).toSet())
            .remove("palette_$widgetId")
            .apply()
    }

    fun delete(widgetId: Int) {
        preferences.edit().remove("habits_$widgetId").remove("palette_$widgetId").apply()
    }
}
