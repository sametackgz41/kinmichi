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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import app.tsuzuku.R
import app.tsuzuku.model.HeatDay
import app.tsuzuku.model.SHARE_CARD_WEEKS
import app.tsuzuku.model.StreakCardData
import java.io.File
import java.text.NumberFormat

private const val CARD_W = 1080
private const val CARD_H = 1350

private fun blend(color: Int, other: Int, amount: Float): Int {
    fun ch(shift: Int) = (((color shr shift) and 0xFF) * (1 - amount) + ((other shr shift) and 0xFF) * amount).toInt()
    return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

private fun white(alpha: Float) = ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or 0xFFFFFF

/**
 * Draws the shareable streak card: the habit in its own colour, the streak in big numbers
 * and the last 20 weeks as a grid. Plain Canvas so it renders the same everywhere.
 */
fun renderStreakCard(context: Context, data: StreakCardData): Bitmap {
    val res = context.resources
    val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val accent = data.habit.colorArgb.toInt()
    val ink = 0xFF101114.toInt()

    // Background: the habit colour fading into near-black.
    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, CARD_W.toFloat(), CARD_H.toFloat(),
            blend(accent, ink, 0.25f), blend(accent, ink, 0.72f), Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, CARD_W.toFloat(), CARD_H.toFloat(), bg)
    // A soft glow behind the number.
    canvas.drawCircle(CARD_W * 0.82f, CARD_H * 0.36f, 420f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white(0.06f) })

    val pad = 88f
    val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    fun text(size: Float, alpha: Float = 1f, face: Typeface = Typeface.DEFAULT) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; color = white(alpha); typeface = face }

    // Brand chip.
    val brand = text(30f, 0.9f, bold).apply { letterSpacing = 0.2f }
    val brandText = "TSUZUKU"
    val chip = RectF(pad, 88f, pad + brand.measureText(brandText) + 56f, 148f)
    canvas.drawRoundRect(chip, 30f, 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = white(0.14f) })
    canvas.drawText(brandText, chip.left + 28f, chip.centerY() + 11f, brand)

    // Emoji and name.
    canvas.drawText(data.habit.emoji, pad, 300f, text(120f))
    val nameLayout = StaticLayout.Builder
        .obtain(data.habit.name, 0, data.habit.name.length, text(68f, 1f, bold), (CARD_W - 2 * pad).toInt())
        .setMaxLines(2)
        .setEllipsize(TextUtils.TruncateAt.END)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .build()
    canvas.save()
    canvas.translate(pad, 335f)
    nameLayout.draw(canvas)
    canvas.restore()
    val afterName = 335f + nameLayout.height

    // Hero number: the running streak, or completed days when there is no streak yet.
    val showStreak = data.current > 0
    val hero = (if (showStreak) data.current else data.doneDays).toString()
    val heroLabel = res.getString(if (showStreak) R.string.share_card_streak else R.string.share_card_done)
    val heroSize = if (nameLayout.lineCount > 1) 220f else 270f
    val heroPaint = text(heroSize, 1f, bold).apply { letterSpacing = -0.03f }
    val heroBaseline = afterName + heroSize * 0.9f
    canvas.drawText(hero, pad - 10f, heroBaseline, heroPaint)
    canvas.drawText("🔥", pad + heroPaint.measureText(hero) + 16f, heroBaseline - heroSize * 0.1f, text(heroSize * 0.4f))
    canvas.drawText(heroLabel, pad, heroBaseline + 62f, text(44f, 0.85f))

    // Heatmap: weeks as columns, days as rows.
    val cols = data.weeks.size.coerceAtLeast(1)
    val gap = 10f
    val cell = ((CARD_W - 2 * pad) - gap * (cols - 1)) / cols
    val statTop = CARD_H - 170f
    // Sit above the stats, but never closer than one line below the hero label.
    val gridTop = maxOf(statTop - 80f - (7 * cell + 6 * gap), heroBaseline + 62f + 100f)
    val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = white(0.35f) }
    data.weeks.forEachIndexed { x, week ->
        week.forEachIndexed { y, day ->
            val left = pad + x * (cell + gap)
            val top = gridTop + y * (cell + gap)
            val rect = RectF(left, top, left + cell, top + cell)
            val radius = cell * 0.3f
            when (day) {
                null -> Unit
                HeatDay.Done -> canvas.drawRoundRect(rect, radius, radius, cellPaint.apply { color = white(0.95f) })
                is HeatDay.Partial -> canvas.drawRoundRect(rect, radius, radius, cellPaint.apply { color = white(0.12f + 0.45f * day.fraction) })
                HeatDay.Skipped -> {
                    rect.inset(1.5f, 1.5f)
                    canvas.drawRoundRect(rect, radius, radius, outline)
                }
                HeatDay.Off -> canvas.drawRoundRect(rect, radius, radius, cellPaint.apply { color = white(0.06f) })
            }
        }
    }
    canvas.drawText(
        res.getString(R.string.share_card_weeks, SHARE_CARD_WEEKS),
        pad, gridTop - 28f, text(32f, 0.7f),
    )

    // Footer stats.
    val percent = NumberFormat.getPercentInstance(res.configuration.locales[0]).format(data.rate30.toDouble())
    fun stat(x: Float, value: String, label: String) {
        canvas.drawText(value, x, statTop + 40f, text(56f, 1f, bold))
        canvas.drawText(label, x, statTop + 92f, text(32f, 0.7f))
    }
    stat(pad, data.best.toString(), res.getString(R.string.best_streak))
    stat(CARD_W / 2f, percent, res.getString(R.string.share_card_rate))
    return bitmap
}

/** Saves the card to the app cache and opens the system share sheet. */
fun shareStreakCard(context: Context, data: StreakCardData) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "tsuzuku-streak.png")
    val bitmap = renderStreakCard(context, data)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
    val text = if (data.current > 0) {
        context.getString(R.string.share_card_text, data.habit.name, data.current)
    } else {
        context.getString(R.string.share_card_text_done, data.habit.name, data.doneDays)
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, text)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, uri)
    context.startActivity(
        Intent.createChooser(send, context.getString(R.string.share_streak)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
