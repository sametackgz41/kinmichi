package app.tsuzuku.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.tsuzuku.HomeActivity
import app.tsuzuku.R
import app.tsuzuku.ui.formatClock
import java.time.LocalDate
import java.time.format.TextStyle as DayStyle
import kotlin.math.cos
import kotlin.math.sin

/** Looks for the wake-alarm widget, drawn side by side in previews. */
enum class AlarmStyle { Toggle, Dial, Sunrise, Minimal }

// ---- Bitmaps drawn on the device, so corners and shapes look the same on every launcher.

internal fun switchBitmap(wPx: Int, hPx: Int, on: Boolean, track: Int, knob: Int, offTrack: Int, offKnob: Int): Bitmap {
    val b = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = if (on) track else offTrack
    c.drawRoundRect(RectF(0f, 0f, wPx.toFloat(), hPx.toFloat()), hPx / 2f, hPx / 2f, p)
    val r = hPx / 2f - hPx * 0.12f
    p.color = if (on) knob else offKnob
    c.drawCircle(if (on) wPx - hPx / 2f else hPx / 2f, hPx / 2f, r, p)
    return b
}

internal fun dialBitmap(sizePx: Int, minutes: Int, face: Int, ink: Int, gold: Int, muted: Int): Bitmap {
    val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val cx = sizePx / 2f; val r = sizePx / 2f
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = face; c.drawCircle(cx, cx, r, p)
    p.color = muted; p.strokeCap = Paint.Cap.ROUND
    for (i in 0 until 12) {
        val a = Math.toRadians(i * 30.0 - 90)
        val inner = if (i % 3 == 0) r * 0.70f else r * 0.78f
        p.strokeWidth = if (i % 3 == 0) r * 0.07f else r * 0.04f
        c.drawLine(cx + inner * cos(a).toFloat(), cx + inner * sin(a).toFloat(), cx + r * 0.86f * cos(a).toFloat(), cx + r * 0.86f * sin(a).toFloat(), p)
    }
    val h = (minutes / 60) % 12 + (minutes % 60) / 60f
    val ha = Math.toRadians(h * 30.0 - 90); val ma = Math.toRadians((minutes % 60) * 6.0 - 90)
    p.color = ink
    p.strokeWidth = r * 0.10f; c.drawLine(cx, cx, cx + r * 0.45f * cos(ha).toFloat(), cx + r * 0.45f * sin(ha).toFloat(), p)
    p.strokeWidth = r * 0.06f; c.drawLine(cx, cx, cx + r * 0.66f * cos(ma).toFloat(), cx + r * 0.66f * sin(ma).toFloat(), p)
    p.color = gold; c.drawCircle(cx, cx, r * 0.09f, p)
    return b
}

internal fun skyBitmap(wPx: Int, hPx: Int, on: Boolean): Bitmap {
    val b = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val top = if (on) 0xFF22396A.toInt() else 0xFF15182A.toInt()
    val bottom = if (on) 0xFFE0A15A.toInt() else 0xFF2A2F4A.toInt()
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, 0f, hPx.toFloat(), top, bottom, Shader.TileMode.CLAMP) }
    c.drawRoundRect(RectF(0f, 0f, wPx.toFloat(), hPx.toFloat()), hPx * 0.3f, hPx * 0.3f, p)
    // Keep the sun and its glow inside the rounded card.
    c.clipPath(android.graphics.Path().apply { addRoundRect(RectF(0f, 0f, wPx.toFloat(), hPx.toFloat()), hPx * 0.3f, hPx * 0.3f, android.graphics.Path.Direction.CW) })
    // A sun rising at the right (or a moon when off).
    val sun = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (on) 0xFFF6D58A.toInt() else 0xFFCBD3EA.toInt() }
    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (on) 0x40F6D58A else 0x20CBD3EA }
    val sx = wPx * 0.84f; val sy = if (on) hPx * 0.98f else hPx * 0.5f; val sr = hPx * 0.24f
    c.drawCircle(sx, sy, sr * 1.7f, glow)
    c.drawCircle(sx, sy, sr, sun)
    if (!on) c.drawCircle(sx + sr * 0.45f, sy - sr * 0.2f, sr * 0.85f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1C2038.toInt() })
    return b
}

internal fun pillBitmap(wPx: Int, hPx: Int, color: Int): Bitmap {
    val b = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
    Canvas(b).drawRoundRect(RectF(0f, 0f, wPx.toFloat(), hPx.toFloat()), hPx / 2f, hPx / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    return b
}

@Composable
internal fun AlarmStyled(alarm: NextAlarm, style: AlarmStyle, today: LocalDate = LocalDate.now()) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val density = context.resources.displayMetrics.density
    val time = formatClock(alarm.at.hour * 60 + alarm.at.minute, locale)
    val day = when (alarm.at.toLocalDate()) {
        today -> context.getString(R.string.widget_alarm_today)
        today.plusDays(1) -> context.getString(R.string.widget_alarm_tomorrow)
        else -> alarm.at.dayOfWeek.getDisplayName(DayStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }
    }
    val toggle = actionRunCallback<ToggleAlarmAction>(actionParametersOf(HabitIdKey to alarm.habitId))
    val open = actionStartActivity<HomeActivity>()
    val primary = GlanceTheme.colors.primary.getColor(context).toArgb()
    val onPrimary = GlanceTheme.colors.onPrimary.getColor(context).toArgb()
    val variant = GlanceTheme.colors.surfaceVariant.getColor(context).toArgb()
    val outline = GlanceTheme.colors.outline.getColor(context).toArgb()
    val ink = GlanceTheme.colors.onSurface.getColor(context).toArgb()
    val gold = GlanceTheme.colors.tertiary.getColor(context).toArgb()
    val timeColor = if (alarm.on) GlanceTheme.colors.onSurface else GlanceTheme.colors.outline
    val status = if (alarm.on) "$day · ${alarm.emoji} ${alarm.name}".trim() else context.getString(R.string.widget_alarm_off)

    when (style) {
        AlarmStyle.Toggle -> Row(
            GlanceModifier.fillMaxSize().background(tsuzukuWidgetBackground()).cornerRadius(28.dp).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(GlanceModifier.defaultWeight().clickable(open)) {
                Text(time, style = TextStyle(color = timeColor, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif))
                Text(status, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 1)
            }
            Image(
                ImageProvider(switchBitmap((52 * density).toInt(), (30 * density).toInt(), alarm.on, primary, onPrimary, variant, outline)),
                context.getString(R.string.widget_alarm_name),
                GlanceModifier.width(52.dp).height(30.dp).clickable(toggle),
            )
        }
        AlarmStyle.Dial -> Row(
            GlanceModifier.fillMaxSize().background(tsuzukuWidgetBackground()).cornerRadius(28.dp).padding(horizontal = 14.dp, vertical = 10.dp).clickable(toggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                ImageProvider(dialBitmap((60 * density).toInt(), alarm.at.hour * 60 + alarm.at.minute, variant, if (alarm.on) ink else outline, gold, outline)),
                null,
                GlanceModifier.size(60.dp),
            )
            Spacer(GlanceModifier.width(14.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(time, style = TextStyle(color = timeColor, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif))
                Text(status, style = TextStyle(color = if (alarm.on) GlanceTheme.colors.primary else GlanceTheme.colors.outline, fontSize = 12.sp), maxLines = 1)
            }
        }
        AlarmStyle.Sunrise -> Box(GlanceModifier.fillMaxSize().clickable(toggle)) {
            Image(ImageProvider(skyBitmap((300 * density).toInt(), (96 * density).toInt(), alarm.on)), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Column(GlanceModifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(time, style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White), fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif))
                Text(status, style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xE6FFFFFF)), fontSize = 12.sp), maxLines = 1)
            }
        }
        AlarmStyle.Minimal -> Column(
            GlanceModifier.fillMaxSize().background(tsuzukuWidgetBackground()).cornerRadius(28.dp).padding(10.dp).clickable(toggle),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(time, style = TextStyle(color = timeColor, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, textAlign = TextAlign.Center))
            Box(contentAlignment = Alignment.Center) {
                val label = if (alarm.on) "$day · ${context.getString(R.string.widget_alarm_on)}" else context.getString(R.string.widget_alarm_off)
                Image(ImageProvider(pillBitmap((150 * density).toInt(), (24 * density).toInt(), if (alarm.on) primary else variant)), null,
                    GlanceModifier.width(150.dp).height(24.dp))
                Text(label, style = TextStyle(color = if (alarm.on) GlanceTheme.colors.onPrimary else GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            }
        }
    }
}
