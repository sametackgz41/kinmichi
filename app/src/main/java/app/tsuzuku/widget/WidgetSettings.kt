package app.tsuzuku.widget

import android.content.Context

enum class WidgetPalette { Indigo, Light, Sage }

class WidgetSettings(context: Context) {
    private val preferences = context.getSharedPreferences("tsuzuku_widgets", Context.MODE_PRIVATE)

    fun selectedHabitIds(widgetId: Int): Set<Long> = preferences
        .getStringSet("habits_$widgetId", emptySet()).orEmpty()
        .mapNotNull(String::toLongOrNull)
        .toSet()

    fun palette(widgetId: Int): WidgetPalette = runCatching {
        WidgetPalette.valueOf(preferences.getString("palette_$widgetId", null).orEmpty())
    }.getOrDefault(WidgetPalette.Indigo)

    fun save(widgetId: Int, habitIds: Set<Long>, palette: WidgetPalette) {
        preferences.edit()
            .putStringSet("habits_$widgetId", habitIds.map(Long::toString).toSet())
            .putString("palette_$widgetId", palette.name)
            .apply()
    }

    fun delete(widgetId: Int) {
        preferences.edit().remove("habits_$widgetId").remove("palette_$widgetId").apply()
    }
}

