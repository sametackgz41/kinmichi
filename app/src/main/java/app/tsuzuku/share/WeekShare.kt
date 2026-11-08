package app.tsuzuku.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import app.tsuzuku.R
import java.io.File
import java.time.format.TextStyle
import kotlin.math.roundToInt

/** What the weekly card shows. */
data class WeekCardData(
    val rate: Float,
    val completed: Int,
    val scheduled: Int,
    val perfectDays: Int,
    val days: List<Pair<java.time.LocalDate, Float>>,
    val bestDay: java.time.LocalDate?,
)

private const val W = 1080
private const val H = 1350

/** A portrait card in the app's colours: the week's rate, seven bars and the totals. */
fun renderWeekCard(context: Context, data: WeekCardData): Bitmap {
    val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val c = Canvas(bitmap)
    val indigo = 0xFF2E4A7D.toInt()
    val deep = 0xFF1C2E52.toInt()
    val gold = 0xFFD9B45A.toInt()
    c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply {
        shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), indigo, deep, Shader.TileMode.CLAMP)
    })
    c.drawCircle(W * 0.85f, H * 0.18f, 360f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x10FFFFFF })
    val bold = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    fun text(size: Float, color: Int = 0xFFFFFFFF.toInt(), face: Typeface = Typeface.DEFAULT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; typeface = face }
    val locale = context.resources.configuration.locales[0]

    c.drawText(context.getString(R.string.week_summary_title), 90f, 190f, text(56f, 0xCCFFFFFF.toInt()))
    c.drawText("%${(data.rate * 100).roundToInt()}", 82f, 420f, text(230f, face = bold))
    c.drawText(context.getString(R.string.week_summary_line, data.completed, data.scheduled, data.perfectDays), 90f, 510f, text(40f, 0xCCFFFFFF.toInt()))

    // Seven bars.
    val left = 90f; val right = W - 90f; val bottom = 1060f; val maxH = 380f
    val gap = 26f
    val barW = (right - left - gap * 6) / 7
    data.days.forEachIndexed { i, (date, fraction) ->
        val x = left + i * (barW + gap)
        c.drawRoundRect(RectF(x, bottom - maxH, x + barW, bottom), 26f, 26f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF })
        val h = (maxH * fraction).coerceAtLeast(14f)
        c.drawRoundRect(
            RectF(x, bottom - h, x + barW, bottom), 26f, 26f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (date == data.bestDay) gold else 0xE6FFFFFF.toInt() },
        )
        val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        val p = text(34f, 0xB3FFFFFF.toInt())
        c.drawText(label, x + barW / 2 - p.measureText(label) / 2, bottom + 60f, p)
    }
    val brand = text(44f, gold, bold)
    c.drawText("Kinmichi", 90f, H - 110f, brand)
    val tag = context.getString(R.string.share_week_tagline)
    c.drawText(tag, 90f + brand.measureText("Kinmichi") + 26f, H - 110f, text(34f, 0x99FFFFFF.toInt()))
    return bitmap
}

fun shareWeekCard(context: Context, data: WeekCardData) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "kinmichi-week.png")
    val bitmap = renderWeekCard(context, data)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_week_text, (data.rate * 100).roundToInt()))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, uri)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_progress)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
