package app.tsuzuku.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.tsuzuku.R
import app.tsuzuku.ui.screens.MonthReviewData
import java.io.File
import java.time.format.TextStyle

private const val W = 1080
private const val H = 1920

/** The month collage as a story-sized picture: big tiles in the palettes' colours. */
fun renderMonthCard(context: Context, data: MonthReviewData): Bitmap {
    val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val c = Canvas(bitmap)
    c.drawColor(0xFF121212.toInt())
    val locale = context.resources.configuration.locales[0]
    val serif = runCatching { ResourcesCompat.getFont(context, R.font.newsreader) }.getOrNull()?.let { Typeface.create(it, Typeface.BOLD) }
        ?: Typeface.create(Typeface.SERIF, Typeface.BOLD)
    fun p(size: Float, color: Int = 0xFFFFFFFF.toInt(), face: Typeface = Typeface.DEFAULT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; typeface = face }
    val month = data.month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
    c.drawText("KINMICHI · ${month.uppercase(locale)} ${data.month.year}", 80f, 170f, p(36f, 0xAAFFFFFF.toInt()))
    c.drawText(context.getString(R.string.month_title), 76f, 270f, p(92f, face = serif))

    fun tile(l: Float, t: Float, r: Float, b: Float, color: Long, label: String, big: String, bigSize: Float, sub: String? = null, labelColor: Int = 0xD9FFFFFF.toInt(), bigColor: Int = 0xFFFFFFFF.toInt()) {
        c.drawRoundRect(RectF(l, t, r, b), 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toInt() })
        c.drawText(label, l + 44f, t + 76f, p(34f, labelColor))
        // Shrink to fit rather than cut the text off.
        val bp = p(bigSize, bigColor, serif)
        while (bp.measureText(big) > r - l - 88f && bp.textSize > 40f) bp.textSize -= 4f
        var text = big
        while (bp.measureText(text) > r - l - 88f && text.length > 2) text = text.dropLast(2) + "…"
        c.drawText(text, l + 40f, b - (if (sub != null) 96f else 52f), bp)
        if (sub != null) c.drawText(sub, l + 44f, b - 44f, p(32f, 0xD9FFFFFF.toInt()))
    }
    val change = data.change
    val changeText = when {
        change == null -> context.getString(R.string.month_new)
        change > 0 -> context.getString(R.string.month_up, change)
        change < 0 -> context.getString(R.string.month_down, -change)
        else -> "="
    }
    tile(80f, 340f, 620f, 760f, 0xFFB2472A, context.getString(R.string.month_completions), data.completions.toString(), 190f)
    tile(650f, 340f, 1000f, 760f, 0xFF8A6A1F, context.getString(R.string.month_vs_prev), changeText, 96f)
    tile(80f, 790f, 470f, 1150f, 0xFF3B3F8F, context.getString(R.string.month_streak), "🔥 ${data.bestStreak}", 110f, data.bestStreakHabit.ifBlank { null })
    val star = data.top.firstOrNull()
    tile(500f, 790f, 1000f, 1150f, 0xFF4E6B2E, context.getString(R.string.month_star), star?.let { "${it.emoji} ${it.name}" } ?: "—", 76f,
        star?.let { context.getString(R.string.month_days_n, it.days) })
    tile(80f, 1180f, 520f, 1520f, 0xFF2E4A7D, context.getString(R.string.month_active), "${data.activeDays}/${data.days}", 110f)
    tile(550f, 1180f, 1000f, 1520f, 0xFF2A2A2A, context.getString(R.string.month_comebacks), "✨ ${data.comebacks}", 110f,
        labelColor = 0xFFE2C57C.toInt(), bigColor = 0xFFE2C57C.toInt())
    data.loved?.let { tile(80f, 1550f, 1000f, 1760f, 0xFFA2445E, context.getString(R.string.month_loved), "${it.emoji} ${it.name}", 72f) }
    c.drawText("Kinmichi", 80f, H - 70f, p(44f, 0xFFE2C57C.toInt(), serif))
    return bitmap
}

fun shareMonthCard(context: Context, data: MonthReviewData) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "kinmichi-${data.month}.png")
    val bitmap = renderMonthCard(context, data)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
    val locale = context.resources.configuration.locales[0]
    val month = data.month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.month_share_text, month, data.completions))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, uri)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.month_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
