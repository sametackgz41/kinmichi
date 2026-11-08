package app.tsuzuku.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** The season by month (northern hemisphere). */
internal enum class Season { Spring, Summer, Autumn, Winter }

internal fun seasonOf(date: LocalDate): Season = when (date.monthValue) {
    3, 4, 5 -> Season.Spring
    6, 7, 8 -> Season.Summer
    9, 10, 11 -> Season.Autumn
    else -> Season.Winter
}

/** One full drift across the header at rest, in seconds. */
private const val DRIFT_SECONDS = 24f

/** How much faster at most a hard shake makes everything move. */
private const val MAX_BOOST = 6f

/**
 * Listens to the accelerometer while the screen is in front and turns shaking into "energy"
 * (0..1). Light, and off whenever the app is in the background.
 */
@Composable
private fun rememberShakeEnergy(onJolt: (Float) -> Unit) {
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) / SensorManager.GRAVITY_EARTH
                // Ignore holding still and ordinary handling; anything above ~1.4 g is a shake.
                if (g > 1.4f) onJolt(((g - 1.4f) / 1.6f).coerceIn(0f, 1f))
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onPauseOrDispose { sm?.unregisterListener(listener) }
    }
}

/**
 * A few slow, faint shapes drifting across the top of Today: sakura petals in spring, fireflies in
 * summer, maple leaves in autumn, snow in winter. Shake the phone and they whirl, then settle.
 */
@Composable
internal fun SeasonalDecor(modifier: Modifier = Modifier, season: Season = seasonOf(LocalDate.now())) {
    var t by remember { mutableFloatStateOf(0f) }
    var energy by remember { mutableFloatStateOf(0f) }
    rememberShakeEnergy { jolt -> energy = (energy + jolt * 0.35f).coerceAtMost(1f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.1f)
                last = now
                // Time runs faster with energy; energy fades back to calm in about two seconds.
                t = (t + dt / DRIFT_SECONDS * (1f + energy * MAX_BOOST)) % 1000f
                energy *= exp(-dt / 0.8f)
            }
        }
    }
    val bits = remember(season) {
        val r = Random(season.ordinal * 31 + 7)
        List(14) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 0.8f, r.nextFloat() * 360f, r.nextFloat()) }
    }
    Canvas(modifier) {
        val gust = energy
        bits.forEach { b ->
            val phase = (t * b[2] + b[1]) % 1f
            // A shake also pushes them sideways, more for the light ones.
            val sway = 0.06f + 0.10f * gust
            val x = size.width * (((b[0] + sway * sin((phase * 2 * PI * (1 + gust) + b[3]).toFloat())) % 1f + 1f) % 1f)
            when (season) {
                Season.Summer -> {
                    // Fireflies glow and fade in place; a shake makes them flicker and scatter.
                    val glow = (0.5f + 0.5f * sin((t * 2 * PI * 3 * b[2] + b[3]).toFloat())).coerceIn(0f, 1f)
                    val y = size.height * ((0.15f + 0.7f * b[1] + 0.08f * gust * sin((t * 40 + b[3]).toFloat())) % 1f)
                    drawCircle(Color(0xFFE6C463).copy(alpha = (0.10f + 0.1f * gust) * glow), radius = 14f, center = Offset(x, y))
                    drawCircle(Color(0xFFE6C463).copy(alpha = (0.55f + 0.3f * gust).coerceAtMost(1f) * glow), radius = 4f, center = Offset(x, y))
                }
                else -> {
                    val y = -20f + (size.height + 40f) * phase
                    val fade = (0.35f + 0.65f * (1f - phase)).coerceIn(0f, 1f) * 0.75f
                    val spin = 1f + 2f * gust
                    when (season) {
                        Season.Spring -> rotate(b[3] + phase * 220f * spin, Offset(x, y)) {
                            drawOval(Color(0xFFF2A7BC).copy(alpha = fade), Offset(x - 12f, y - 7f), Size(24f, 14f))
                        }
                        Season.Autumn -> rotate(b[3] + phase * 300f * spin, Offset(x, y)) {
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
