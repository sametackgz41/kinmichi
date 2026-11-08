package app.tsuzuku.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** The season by month (northern hemisphere). */
internal enum class Season { Spring, Summer, Autumn, Winter }

internal fun seasonOf(date: LocalDate): Season = when (date.monthValue) {
    3, 4, 5 -> Season.Spring
    6, 7, 8 -> Season.Summer
    9, 10, 11 -> Season.Autumn
    else -> Season.Winter
}

/**
 * A few slow, faint shapes drifting across the top of Today: sakura petals in spring, fireflies in
 * summer, maple leaves in autumn, snow in winter. Quiet enough to stay out of the way.
 */
@Composable
internal fun SeasonalDecor(modifier: Modifier = Modifier, season: Season = seasonOf(LocalDate.now())) {
    val t by rememberInfiniteTransition(label = "season").animateFloat(
        0f, 1f, infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart), label = "drift",
    )
    val bits = remember(season) {
        val r = Random(season.ordinal * 31 + 7)
        List(14) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 0.8f, r.nextFloat() * 360f, r.nextFloat()) }
    }
    Canvas(modifier) {
        bits.forEach { b ->
            val phase = (t * b[2] + b[1]) % 1f
            val x = size.width * ((b[0] + 0.06f * sin((phase * 2 * PI + b[3]).toFloat())) % 1f)
            when (season) {
                Season.Summer -> {
                    // Fireflies glow and fade in place.
                    val glow = (0.5f + 0.5f * sin((t * 2 * PI * 3 * b[2] + b[3]).toFloat())).coerceIn(0f, 1f)
                    val y = size.height * (0.15f + 0.7f * b[1])
                    drawCircle(Color(0xFFE6C463).copy(alpha = 0.10f * glow), radius = 14f, center = Offset(x, y))
                    drawCircle(Color(0xFFE6C463).copy(alpha = 0.55f * glow), radius = 4f, center = Offset(x, y))
                }
                else -> {
                    val y = -20f + (size.height + 40f) * phase
                    val fade = (0.35f + 0.65f * (1f - phase)).coerceIn(0f, 1f) * 0.75f
                    when (season) {
                        Season.Spring -> rotate(b[3] + phase * 220f, Offset(x, y)) {
                            drawOval(Color(0xFFF2A7BC).copy(alpha = fade), Offset(x - 12f, y - 7f), Size(24f, 14f))
                        }
                        Season.Autumn -> rotate(b[3] + phase * 300f, Offset(x, y)) {
                            val leaf = if (b[4] > 0.5f) Color(0xFFC8553A) else Color(0xFFD9913B)
                            drawOval(leaf.copy(alpha = fade), Offset(x - 13f, y - 8f), Size(26f, 16f))
                            drawOval(leaf.copy(alpha = fade), Offset(x - 8f, y - 13f), Size(16f, 26f))
                        }
                        else -> drawCircle(Color(0xFF9FB3CC).copy(alpha = fade), radius = 5f + b[4] * 5f, center = Offset(x, y))
                    }
                }
            }
        }
    }
}
