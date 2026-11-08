package app.tsuzuku.ui.theme

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Look-and-feel choices kept on the device: seasonal touches, text size, contrast. */
object LookPrefs {
    private const val PREFS = "look"
    var seasonal by mutableStateOf(true)
        private set
    var textScale by mutableFloatStateOf(1f)
        private set
    var highContrast by mutableStateOf(false)
        private set
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        seasonal = p.getBoolean("seasonal", true)
        textScale = p.getFloat("text_scale", 1f)
        highContrast = p.getBoolean("high_contrast", false)
    }

    private fun edit(context: Context, block: android.content.SharedPreferences.Editor.() -> Unit) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply(block).apply()

    fun setSeasonal(context: Context, on: Boolean) { seasonal = on; edit(context) { putBoolean("seasonal", on) } }
    fun setTextScale(context: Context, scale: Float) { textScale = scale; edit(context) { putFloat("text_scale", scale) } }
    fun setHighContrast(context: Context, on: Boolean) { highContrast = on; edit(context) { putBoolean("high_contrast", on) } }

    val TextScales = listOf(1f, 1.15f, 1.3f)
}

/** Home-screen icons; each is a launcher alias of the same screen, and only the chosen one is on. */
enum class AppIcon(val alias: String) {
    Path("app.tsuzuku.MainActivity"),
    Enso("app.tsuzuku.LauncherEnso"),
    Night("app.tsuzuku.LauncherNight");

    companion object {
        fun current(context: Context): AppIcon = entries.firstOrNull { icon ->
            val state = context.packageManager.getComponentEnabledSetting(ComponentName(context.packageName, icon.alias))
            if (icon == Path) state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED else state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } ?: Path

        /** Switches the launcher icon. The launcher may move the app back to its app drawer. */
        fun set(context: Context, choice: AppIcon) {
            val pm = context.packageManager
            entries.forEach { icon ->
                pm.setComponentEnabledSetting(
                    ComponentName(context.packageName, icon.alias),
                    if (icon == choice) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
