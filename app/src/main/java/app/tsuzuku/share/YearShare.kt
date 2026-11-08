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
import app.tsuzuku.ui.screens.YearReviewData
import java.io.File

private const val W = 1080
private const val H = 1920

/** A tall story-sized card with the year's headline numbers. */
fun renderYearCard(context: Context, data: YearReviewData): Bitmap {
    val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val c = Canvas(bitmap)
    c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xFF1C2E52.toInt(), 0xFF6E5418.toInt(), Shader.TileMode.CLAMP)
    })
    val gold = 0xFFE2C57C.toInt()
    val serif = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    fun p(size: Float, color: Int = 0xFFFFFFFF.toInt(), face: Typeface = Typeface.DEFAULT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; typeface = face }
    c.drawText(context.getString(R.string.year_card_title), 90f, 230f, p(56f, 0xCCFFFFFF.toInt()))
    c.drawText(data.year.toString(), 82f, 470f, p(260f, gold, serif))
    val rows = listOf(
        data.completions.toString() to context.getString(R.string.year_card_completions),
        data.activeDays.toString() to context.getString(R.string.year_card_active),
        data.bestStreak.toString() to context.getString(R.string.year_card_streak),
        data.comebacks.toString() to context.getString(R.string.year_card_comebacks),
    )
    var y = 700f
    rows.forEach { (value, label) ->
        c.drawRoundRect(RectF(80f, y - 120f, W - 80f, y + 60f), 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF })
        c.drawText(value, 130f, y + 10f, p(110f, face = serif))
        c.drawText(label, 130f + p(110f, face = serif).measureText(value) + 34f, y - 6f, p(42f, 0xD9FFFFFF.toInt()))
        y += 230f
    }
    if (data.topHabit.isNotBlank()) {
        c.drawText("${data.topHabitEmoji} ${data.topHabit}", 90f, y + 40f, p(56f))
        c.drawText(context.getString(R.string.year_top_habit, data.topHabitCount), 90f, y + 110f, p(38f, 0xB3FFFFFF.toInt()))
    }
    c.drawText("Kinmichi", 90f, H - 120f, p(48f, gold, serif))
    return bitmap
}

fun shareYearCard(context: Context, data: YearReviewData) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "kinmichi-${data.year}.png")
    val bitmap = renderYearCard(context, data)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.year_share_text, data.year, data.completions))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, uri)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.year_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
