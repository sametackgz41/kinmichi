package app.tsuzuku.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.glance.material3.ColorProviders
import app.tsuzuku.TsuzukuApplication
import app.tsuzuku.data.preferences.PalettePreference
import app.tsuzuku.ui.theme.paletteScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Habit id passed from a widget tap to its action callback. */
val HabitIdKey = ActionParameters.Key<Long>("habit_id")

/** The app's palette: Washi by default, the wallpaper colours when the user picked those. */
@Composable
fun TsuzukuGlanceTheme(content: @Composable () -> Unit) {
    val context = androidx.glance.LocalContext.current
    val settings = (context.applicationContext as? TsuzukuApplication)?.settingsRepository?.settings?.value
    val palette = settings?.palette ?: PalettePreference.Washi
    if (palette == PalettePreference.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceTheme(content = content)
    } else {
        val pureBlack = settings?.pureBlack == true
        GlanceTheme(
            colors = ColorProviders(light = paletteScheme(palette, false), dark = paletteScheme(palette, true, pureBlack)),
            content = content,
        )
    }
}

/**
 * Widget card colour: warm paper (or indigo ink at night) for Washi, the launcher's own widget
 * background for the wallpaper palette.
 */
@Composable
fun tsuzukuWidgetBackground(): androidx.glance.unit.ColorProvider {
    val context = androidx.glance.LocalContext.current
    val settings = (context.applicationContext as? TsuzukuApplication)?.settingsRepository?.settings?.value
    val palette = settings?.palette ?: PalettePreference.Washi
    return if (palette == PalettePreference.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceTheme.colors.widgetBackground
    } else {
        androidx.glance.color.ColorProvider(
            day = paletteScheme(palette, false).surfaceContainerLow,
            night = paletteScheme(palette, true, settings?.pureBlack == true).surfaceContainerLow,
        )
    }
}

/** Refreshes every Tsuzuku widget kind after data changed anywhere in the app. */
object TsuzukuWidgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        scope.launch { updateAll(appContext) }
    }

    suspend fun updateAll(context: Context) {
        app.tsuzuku.tile.TodayTileService.requestRefresh(context)
        runCatching { TodayWidget().updateAll(context) }
        runCatching { HabitWidget().updateAll(context) }
        runCatching { WeekWidget().updateAll(context) }
    }
}

private fun Context.tsuzuku(): TsuzukuApplication = applicationContext as TsuzukuApplication

/** Completes / un-completes a check habit straight from a widget. */
class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HabitIdKey] ?: return
        context.tsuzuku().habitRepository.toggleHabit(id)
        TsuzukuWidgets.updateAll(context)
    }
}

/** Adds one unit to a count habit straight from a widget. */
class IncrementHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HabitIdKey] ?: return
        context.tsuzuku().habitRepository.incrementHabit(id)
        TsuzukuWidgets.updateAll(context)
    }
}

/** Opens the app straight on the new-habit screen (same route as the launcher shortcut). */
fun addHabitIntent(context: Context): android.content.Intent =
    android.content.Intent(context, app.tsuzuku.MainActivity::class.java)
        .setAction(app.tsuzuku.MainActivity.ACTION_ADD_HABIT)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)

/** A filled circle, used where Glance can't round corners (Android 11 and older). */
fun dotBitmap(sizePx: Int, color: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    return bitmap
}

/** [color] with its alpha replaced, for soft tints of a habit's colour. */
fun Int.withAlpha(alpha: Float): Int = (this and 0x00FFFFFF) or ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24)

/**
 * Glance cannot draw arcs, so the progress ring is rendered into a small bitmap.
 * [sizePx] should match the on-screen size to stay crisp.
 */
fun ringBitmap(
    sizePx: Int,
    fraction: Float,
    trackColor: Int,
    progressColor: Int,
    strokePx: Float,
    fillColor: Int? = null,
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    if (fillColor != null) {
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor })
    }
    val inset = strokePx / 2f
    val bounds = RectF(inset, inset, sizePx - inset, sizePx - inset)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
    }
    paint.color = trackColor
    canvas.drawArc(bounds, 0f, 360f, false, paint)
    val sweep = 360f * fraction.coerceIn(0f, 1f)
    if (sweep > 0f) {
        paint.color = progressColor
        canvas.drawArc(bounds, -90f, sweep, false, paint)
    }
    return bitmap
}
