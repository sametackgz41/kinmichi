package app.tsuzuku.notifications

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

// ---- Shake: count firm shakes from the accelerometer -------------------------------------------

/** Calls [onShake] for each firm shake (acceleration well above gravity, spaced out). */
@Composable
internal fun rememberShakeCounter(onShake: () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var last = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) / SensorManager.GRAVITY_EARTH
                val now = System.currentTimeMillis()
                if (g > 2.3f && now - last > 260) { last = now; onShake() }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm?.unregisterListener(listener) }
    }
}

/** Steps from the accelerometer: a gentle peak detector, so no activity permission is needed. */
@Composable
internal fun rememberStepCounter(onStep: () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var smooth = SensorManager.GRAVITY_EARTH
        var above = false
        var last = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val mag = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2])
                smooth = smooth * 0.8f + mag * 0.2f
                val now = System.currentTimeMillis()
                if (!above && smooth > SensorManager.GRAVITY_EARTH * 1.12f) {
                    above = true
                    if (now - last > 300) { last = now; onStep() }
                } else if (above && smooth < SensorManager.GRAVITY_EARTH * 1.02f) above = false
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm?.unregisterListener(listener) }
    }
}

@Composable
private fun CountMission(title: String, emoji: String, count: Int, target: Int, hint: String, step: Pair<Int, Int>?) = AlarmBackdrop {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        MissionHeader(title, step?.first ?: 0, step?.second ?: 1)
        Spacer(Modifier.weight(1f))
        Text(emoji, fontSize = 72.sp)
        Spacer(Modifier.height(16.dp))
        Text("$count / $target", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = { count / target.toFloat() }, modifier = Modifier.fillMaxWidth(0.7f).height(10.dp).clip(RoundedCornerShape(5.dp)))
        Spacer(Modifier.height(16.dp))
        Text(hint, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1.3f))
    }
}

@Composable
internal fun ShakeMission(onProgress: () -> Unit, onSolved: () -> Unit, target: Int = 30, step: Pair<Int, Int>? = null, preview: Int? = null) {
    var count by remember { mutableIntStateOf(preview ?: 0) }
    if (preview == null) rememberShakeCounter {
        if (count < target) { count++; onProgress(); if (count >= target) onSolved() }
    }
    CountMission(stringResource(R.string.alarm_shake_title), "📳", count, target, stringResource(R.string.alarm_shake_hint), step)
}

@Composable
internal fun StepMission(onProgress: () -> Unit, onSolved: () -> Unit, target: Int = 20, step: Pair<Int, Int>? = null, preview: Int? = null) {
    var count by remember { mutableIntStateOf(preview ?: 0) }
    if (preview == null) rememberStepCounter {
        if (count < target) { count++; onProgress(); if (count >= target) onSolved() }
    }
    CountMission(stringResource(R.string.alarm_steps_title), "🚶", count, target, stringResource(R.string.alarm_steps_hint), step)
}

// ---- Type a sentence exactly -------------------------------------------------------------------

@Composable
internal fun TypeMission(onProgress: () -> Unit, onSolved: () -> Unit, seed: Long = System.nanoTime(), step: Pair<Int, Int>? = null, preview: String? = null) = AlarmBackdrop {
    val phrases = stringArrayResource(R.array.alarm_phrases)
    val phrase = remember { phrases[Random(seed).nextInt(phrases.size)] }
    var typed by remember { mutableStateOf(preview ?: "") }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        MissionHeader(stringResource(R.string.alarm_type_title), step?.first ?: 0, step?.second ?: 1)
        Spacer(Modifier.weight(0.6f))
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(
                phrase,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(20.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = typed,
            onValueChange = {
                typed = it; onProgress()
                if (it.trim().equals(phrase, ignoreCase = true)) onSolved()
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, autoCorrectEnabled = false),
            isError = typed.isNotEmpty() && !phrase.startsWith(typed.trimEnd(), ignoreCase = true),
            shape = RoundedCornerShape(16.dp),
        )
        Text(stringResource(R.string.alarm_type_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.weight(1f))
    }
}

// ---- Colour trap (Stroop): tap the ink, not the word -------------------------------------------

private val stroopColors = listOf(Color(0xFFD9534F), Color(0xFF3B6FD8), Color(0xFF4E9A6B), Color(0xFFE0A93B))

@Composable
internal fun StroopMission(onProgress: () -> Unit, onSolved: () -> Unit, rounds: Int = 5, seed: Long = System.nanoTime(), step: Pair<Int, Int>? = null) = AlarmBackdrop {
    val names = stringArrayResource(R.array.alarm_colors)
    val random = remember { Random(seed) }
    var solved by remember { mutableIntStateOf(0) }
    fun next(): Pair<Int, Int> { val w = random.nextInt(4); var ink = random.nextInt(4); if (ink == w) ink = (ink + 1 + random.nextInt(3)) % 4; return w to ink }
    var current by remember { mutableStateOf(next()) }
    var miss by remember { mutableStateOf(false) }
    LaunchedEffect(miss) { if (miss) { delay(400); miss = false } }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        MissionHeader(stringResource(R.string.alarm_stroop_title), step?.first ?: solved, step?.second ?: rounds)
        Spacer(Modifier.weight(1f))
        Text(
            names[current.first].uppercase(),
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            color = stroopColors[current.second],
        )
        Text(
            if (miss) stringResource(R.string.alarm_stroop_wrong) else stringResource(R.string.alarm_stroop_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = if (miss) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            (0 until 4).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { i ->
                        Surface(
                            onClick = {
                                onProgress()
                                if (i == current.second) { solved++; if (solved >= rounds) onSolved() else current = next() } else miss = true
                            },
                            shape = RoundedCornerShape(22.dp),
                            color = stroopColors[i],
                            modifier = Modifier.weight(1f).height(72.dp),
                        ) {}
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ---- Word unscramble ---------------------------------------------------------------------------

internal fun scramble(word: String, random: Random): String {
    if (word.length < 2) return word
    repeat(20) {
        val s = word.toList().shuffled(random).joinToString("")
        if (s != word) return s
    }
    return word.reversed()
}

@Composable
internal fun AnagramMission(onProgress: () -> Unit, onSolved: () -> Unit, seed: Long = System.nanoTime(), step: Pair<Int, Int>? = null) = AlarmBackdrop {
    val words = stringArrayResource(R.array.alarm_words)
    val random = remember { Random(seed) }
    val word = remember { words[random.nextInt(words.size)].uppercase() }
    val letters = remember { scramble(word, random).toList() }
    val picked = remember { mutableStateListOf<Int>() }
    var miss by remember { mutableStateOf(false) }
    LaunchedEffect(miss) { if (miss) { delay(500); picked.clear(); miss = false } }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        MissionHeader(stringResource(R.string.alarm_anagram_title), step?.first ?: 0, step?.second ?: 1)
        Spacer(Modifier.weight(1f))
        // The answer so far, one box per letter.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            word.indices.forEach { i ->
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (miss) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) { Text(picked.getOrNull(i)?.let { letters[it].toString() } ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            letters.forEachIndexed { i, ch ->
                val used = i in picked
                Surface(
                    onClick = {
                        if (used || miss) return@Surface
                        onProgress(); picked += i
                        if (picked.size == word.length) {
                            if (picked.joinToString("") { letters[it].toString() } == word) onSolved() else miss = true
                        }
                    },
                    shape = CircleShape,
                    color = if (used) Color.Transparent else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(ch.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (used) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.alarm_anagram_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
    }
}

// ---- Trace a pattern over 3x3 dots -------------------------------------------------------------

internal fun patternPath(random: Random, length: Int): List<Int> {
    // A walk over neighbouring dots (no repeats), so the pattern can be traced in one stroke.
    repeat(100) {
        val path = mutableListOf(random.nextInt(9))
        while (path.size < length) {
            val (r, c) = path.last() / 3 to path.last() % 3
            val next = (-1..1).flatMap { dr -> (-1..1).map { dc -> (r + dr) to (c + dc) } }
                .filter { (nr, nc) -> nr in 0..2 && nc in 0..2 }.map { (nr, nc) -> nr * 3 + nc }
                .filter { it !in path }
            if (next.isEmpty()) break
            path += next[random.nextInt(next.size)]
        }
        if (path.size == length) return path
    }
    return listOf(0, 1, 2, 5, 8).take(length)
}

@Composable
internal fun PatternMission(onProgress: () -> Unit, onSolved: () -> Unit, length: Int = 5, seed: Long = System.nanoTime(), step: Pair<Int, Int>? = null, preview: Int = 0) = AlarmBackdrop {
    val target = remember { patternPath(Random(seed), length) }
    val drawn = remember { mutableStateListOf<Int>().apply { addAll(target.take(preview)) } }
    var miss by remember { mutableStateOf(false) }
    LaunchedEffect(miss) { if (miss) { delay(450); drawn.clear(); miss = false } }
    val primary = MaterialTheme.colorScheme.primary
    val faint = MaterialTheme.colorScheme.outlineVariant
    val dot = MaterialTheme.colorScheme.onSurface
    val error = MaterialTheme.colorScheme.error
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        MissionHeader(stringResource(R.string.alarm_pattern_title), step?.first ?: 0, step?.second ?: 1)
        Spacer(Modifier.weight(1f))
        BoxWithConstraints(Modifier.fillMaxWidth(0.82f).aspectRatio(1f)) {
            val sizePx = with(LocalDensity.current) { maxWidth.toPx() }
            val cell = sizePx / 3
            fun center(i: Int) = Offset((i % 3 + 0.5f) * cell, (i / 3 + 0.5f) * cell)
            fun hit(o: Offset): Int? = (0 until 9).firstOrNull { (center(it) - o).getDistance() < cell * 0.32f }
            Canvas(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { o -> drawn.clear(); hit(o)?.let { drawn += it } },
                        onDrag = { change, _ ->
                            val i = hit(change.position)
                            if (i != null && i !in drawn) { drawn += i; onProgress() }
                        },
                        onDragEnd = {
                            if (drawn.toList() == target) onSolved() else miss = true
                        },
                    )
                },
            ) {
                // The pattern to copy, faint; what you've drawn on top.
                for (k in 0 until target.size - 1) drawLine(faint, center(target[k]), center(target[k + 1]), strokeWidth = cell * 0.16f, cap = StrokeCap.Round)
                for (k in 0 until drawn.size - 1) drawLine(if (miss) error else primary, center(drawn[k]), center(drawn[k + 1]), strokeWidth = cell * 0.12f, cap = StrokeCap.Round)
                (0 until 9).forEach { i ->
                    val on = i in drawn
                    drawCircle(if (on) (if (miss) error else primary) else dot.copy(alpha = 0.55f), radius = cell * (if (on) 0.12f else 0.08f), center = center(i))
                }
                drawCircle(primary.copy(alpha = 0.35f), radius = cell * 0.2f, center = center(target.first()))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(if (miss) R.string.alarm_pattern_wrong else R.string.alarm_pattern_hint), style = MaterialTheme.typography.bodyLarge,
            color = if (miss) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
    }
}

// ---- Catch: tap a target that jumps around -----------------------------------------------------

@Composable
internal fun CatchMission(onProgress: () -> Unit, onSolved: () -> Unit, target: Int = 12, seed: Long = System.nanoTime(), step: Pair<Int, Int>? = null) = AlarmBackdrop {
    val random = remember { Random(seed) }
    var hits by remember { mutableIntStateOf(0) }
    var pos by remember { mutableStateOf(random.nextFloat() to random.nextFloat()) }
    // It moves on its own too, so you have to watch it.
    LaunchedEffect(hits) { while (true) { delay(1_100); pos = random.nextFloat() to random.nextFloat() } }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        MissionHeader(stringResource(R.string.alarm_catch_title), step?.first ?: hits, step?.second ?: target)
        Text("$hits / $target", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp)) {
            val size = 72.dp
            val x = ((maxWidth - size).value * pos.first).dp
            val y = ((maxHeight - size).value * pos.second).dp
            Box(
                Modifier.offset(x, y).size(size).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary)
                    .clickable {
                        onProgress(); hits++
                        if (hits >= target) onSolved() else pos = random.nextFloat() to random.nextFloat()
                    },
                contentAlignment = Alignment.Center,
            ) { Text("✨", fontSize = 30.sp) }
        }
        Text(stringResource(R.string.alarm_catch_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
    }
}
