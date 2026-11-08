package app.tsuzuku.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
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
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.tsuzuku.HomeActivity
import app.tsuzuku.model.TodayHabit
import kotlin.math.cos
import kotlin.math.sin

/** Looks for the 1x1 progress widget, drawn side by side in previews. */
enum class RingStyle { Badge, Enso, Path, Sun }

private fun disc(size: Int, color: Int): Pair<Bitmap, Canvas> {
    val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    c.drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    return b to c
}

/** A round disc with the ring inside: the widget reads as a round badge, not a box. */
internal fun badgeBitmap(size: Int, fraction: Float, paper: Int, track: Int, ink: Int, gold: Int): Bitmap {
    val (b, c) = disc(size, paper)
    val stroke = size * 0.085f
    val inset = size * 0.13f
    val rect = RectF(inset, inset, size - inset, size - inset)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND }
    p.color = track; c.drawArc(rect, 0f, 360f, false, p)
    if (fraction > 0f) { p.color = if (fraction >= 1f) gold else ink; c.drawArc(rect, -90f, 360f * fraction.coerceAtMost(1f), false, p) }
    return b
}

/** A brush ensō that closes as the day fills; the empty part is a faint trace. */
internal fun ensoRingBitmap(size: Int, fraction: Float, paper: Int, faint: Int, ink: Int, gold: Int): Bitmap {
    val (b, c) = disc(size, paper)
    val cx = size / 2f; val r = size * 0.34f; val w = size * 0.11f
    val start = -100.0
    fun brush(from: Double, sweep: Double, color: Int, thick: Float) {
        if (sweep <= 0) return
        val n = 90
        val outer = ArrayList<Pair<Float, Float>>(); val inner = ArrayList<Pair<Float, Float>>()
        for (i in 0..n) {
            val t = i / n.toDouble()
            val a = Math.toRadians(from + sweep * t)
            val press = (0.55 + 0.45 * kotlin.math.min(1.0, t * 6)) * (1 - 0.35 * t)
            val half = thick * press / 2
            outer += (cx + (r + half) * cos(a)).toFloat() to (cx + (r + half) * sin(a)).toFloat()
            inner += (cx + (r - half) * cos(a)).toFloat() to (cx + (r - half) * sin(a)).toFloat()
        }
        val path = Path()
        path.moveTo(outer[0].first, outer[0].second)
        outer.forEach { path.lineTo(it.first, it.second) }
        inner.reversed().forEach { path.lineTo(it.first, it.second) }
        path.close()
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        val a0 = Math.toRadians(from)
        c.drawCircle((cx + r * cos(a0)).toFloat(), (cx + r * sin(a0)).toFloat(), thick * 0.55f / 2 * 1.1f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }
    brush(start, 330.0, faint, w * 0.55f)
    brush(start, 330.0 * fraction.coerceIn(0f, 1f), if (fraction >= 1f) gold else ink, w)
    return b
}

/** The app's golden path: steps light up one by one; the last one is gold when the day is done. */
internal fun pathBitmap(size: Int, fraction: Float, paper: Int, faint: Int, ink: Int, gold: Int): Bitmap {
    val (b, c) = disc(size, paper)
    val s = size / 108f
    val steps = listOf(30.9f to 74.8f, 34.6f to 65.3f, 43.7f to 59.6f, 54.8f to 54.6f, 63.9f to 47.4f, 66.7f to 34.8f)
    val radii = listOf(3.9f, 4.2f, 4.6f, 4.9f, 5.3f, 6.5f)
    val lit = (fraction.coerceIn(0f, 1f) * steps.size).toInt()
    steps.forEachIndexed { i, (x, y) ->
        val color = when {
            i == steps.lastIndex && fraction >= 1f -> gold
            i < lit -> ink
            else -> faint
        }
        // Centre the mark a little toward the middle of the disc.
        c.drawCircle((x + 4f) * s, (y + 1f) * s, radii[i] * s * 1.15f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }
    return b
}

/** A sky disc whose sun rises with the day's progress. */
internal fun sunDiscBitmap(size: Int, fraction: Float): Bitmap {
    val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val f = fraction.coerceIn(0f, 1f)
    val top = if (f >= 1f) 0xFF4A6FA8.toInt() else 0xFF22396A.toInt()
    val bottom = 0xFFE0A15A.toInt()
    c.drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 0f, size.toFloat(), top, bottom, Shader.TileMode.CLAMP)
    })
    c.save()
    val clip = Path().apply { addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW) }
    c.clipPath(clip)
    val sr = size * 0.16f
    val sy = size * (0.95f - 0.62f * f)
    c.drawCircle(size / 2f, sy, sr * 1.8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40F6D58A })
    c.drawCircle(size / 2f, sy, sr, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF6D58A.toInt() })
    c.restore()
    return b
}

@Composable
internal fun RingStyled(habits: List<TodayHabit>, locked: Boolean, style: RingStyle) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val total = habits.size
    val done = habits.count(TodayHabit::isComplete)
    val fraction = if (locked || total == 0) 0f else done.toFloat() / total
    val px = (110 * density).toInt()
    val paper = GlanceTheme.colors.surface.getColor(context).toArgb()
    val track = GlanceTheme.colors.surfaceVariant.getColor(context).toArgb()
    val ink = GlanceTheme.colors.primary.getColor(context).toArgb()
    val gold = GlanceTheme.colors.tertiary.getColor(context).toArgb()
    val bmp = when (style) {
        RingStyle.Badge -> badgeBitmap(px, fraction, paper, track, ink, gold)
        RingStyle.Enso -> ensoRingBitmap(px, fraction, paper, track, ink, gold)
        RingStyle.Path -> pathBitmap(px, fraction, paper, track, ink, gold)
        RingStyle.Sun -> sunDiscBitmap(px, fraction)
    }
    val percent = if (locked) "·" else "%${(fraction * 100).toInt()}"
    Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity<HomeActivity>()), contentAlignment = Alignment.Center) {
        Image(ImageProvider(bmp), percent, GlanceModifier.fillMaxSize(), contentScale = ContentScale.Fit)
        when (style) {
            RingStyle.Badge, RingStyle.Enso -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(percent, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif))
                if (!locked && total > 0) Text("$done/$total", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp))
            }
            RingStyle.Path -> Box(GlanceModifier.fillMaxSize().padding(bottom = 14.dp, end = 14.dp), contentAlignment = Alignment.BottomEnd) {
                Text("$done/$total", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium))
            }
            RingStyle.Sun -> Box(GlanceModifier.fillMaxSize().padding(top = 16.dp), contentAlignment = Alignment.TopCenter) {
                Text(percent, style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif))
            }
        }
    }
}
