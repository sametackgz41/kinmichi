package app.tsuzuku.notifications

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.TodayHabit
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Shared backdrop: a soft wash of the theme's colours, dark enough for a bedroom at night. */
@Composable
private fun AlarmBackdrop(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(scheme.primaryContainer.copy(alpha = 0.55f), scheme.surface, scheme.surface)),
        ),
    ) { content() }
}

/** The ringing page. */
@Composable
internal fun AlarmRingingPage(
    time: String,
    date: String,
    emoji: String,
    name: String,
    plan: String,
    mission: AlarmMission,
    snoozesLeft: Int,
    onStart: () -> Unit,
    onSnooze: () -> Unit,
    onNotToday: () -> Unit,
    modifier: Modifier = Modifier,
) = AlarmBackdrop {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring by pulse.animateFloat(1f, 1.18f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ring")
    val glow by pulse.animateFloat(0.35f, 0.08f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    Column(modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(36.dp))
        Text(date, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(time, style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp, fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            Box(Modifier.size(170.dp).scale(ring).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = glow)))
            Box(
                Modifier.size(150.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(emoji.ifBlank { "⏰" }, fontSize = 68.sp) }
        }
        Spacer(Modifier.height(18.dp))
        Text(name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
        Text(
            plan.ifBlank { stringResource(R.string.alarm_time_to_start) },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.weight(1.2f))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(68.dp), shape = RoundedCornerShape(24.dp)) {
            Text(
                stringResource(if (mission == AlarmMission.None) R.string.alarm_start else R.string.alarm_start_mission),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(Modifier.height(10.dp))
        if (snoozesLeft > 0) {
            FilledTonalButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(20.dp)) {
                Text(stringResource(R.string.alarm_snooze_left, HabitAlarms.SNOOZE_MINUTES, snoozesLeft))
            }
        } else {
            Text(
                stringResource(R.string.alarm_no_snooze),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        if (mission == AlarmMission.None) {
            TextButton(onClick = onNotToday, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.alarm_not_today)) }
        }
    }
}

/** Header shared by the tasks: what to do and how far along. */
@Composable
private fun MissionHeader(title: String, done: Int, total: Int) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.alarm_step_of, done + 1, total), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { done / total.toFloat() },
            modifier = Modifier.fillMaxWidth(0.6f).height(6.dp).clip(RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ---- Math -------------------------------------------------------------------------------------

internal data class MathProblem(val text: String, val answer: Int)

internal fun mathProblem(random: Random): MathProblem = when (random.nextInt(3)) {
    0 -> { val a = random.nextInt(12, 90); val b = random.nextInt(12, 90); MathProblem("$a + $b", a + b) }
    1 -> { val a = random.nextInt(40, 99); val b = random.nextInt(11, a); MathProblem("$a − $b", a - b) }
    else -> { val a = random.nextInt(3, 10); val b = random.nextInt(3, 10); val c = random.nextInt(2, 20); MathProblem("$a × $b + $c", a * b + c) }
}

@Composable
internal fun MathMission(onProgress: () -> Unit, onSolved: () -> Unit, seed: Long = System.nanoTime(), count: Int = 3, step: Pair<Int, Int>? = null) = AlarmBackdrop {
    val random = remember { Random(seed) }
    var solved by remember { mutableIntStateOf(0) }
    var problem by remember { mutableStateOf(mathProblem(random)) }
    var input by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val answerColor by animateColorAsState(if (wrong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, label = "answer")
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        MissionHeader(stringResource(R.string.alarm_math_title), step?.first ?: solved, step?.second ?: count)
        Spacer(Modifier.weight(1f))
        Text(problem.text, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.width(180.dp).height(72.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) { Text(input.ifEmpty { "?" }, style = MaterialTheme.typography.displaySmall, color = if (input.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else answerColor) }
        Spacer(Modifier.weight(1f))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "✓")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { key ->
                        val accent = key == "✓"
                        Surface(
                            onClick = {
                                onProgress()
                                wrong = false
                                when (key) {
                                    "⌫" -> input = input.dropLast(1)
                                    "✓" -> if (input.toIntOrNull() == problem.answer) {
                                        solved++
                                        input = ""
                                        if (solved >= count) onSolved() else problem = mathProblem(random)
                                    } else {
                                        wrong = true
                                        input = ""
                                    }
                                    else -> if (input.length < 4) input += key
                                }
                            },
                            shape = RoundedCornerShape(22.dp),
                            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.weight(1f).height(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(key, style = MaterialTheme.typography.headlineSmall, color = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ---- Memory -----------------------------------------------------------------------------------

@Composable
internal fun MemoryMission(
    onProgress: () -> Unit,
    onSolved: () -> Unit,
    seed: Long = System.nanoTime(),
    preview: List<Int>? = null,
    lengths: List<Int> = listOf(4, 5),
    step: Pair<Int, Int>? = null,
) = AlarmBackdrop {
    val random = remember { Random(seed) }
    var round by remember { mutableIntStateOf(0) }
    var attempt by remember { mutableIntStateOf(0) }
    val sequence = remember(round, attempt) { List(lengths[round.coerceAtMost(lengths.lastIndex)]) { random.nextInt(9) } }
    var lit by remember { mutableIntStateOf(-1) }
    var watching by remember { mutableStateOf(preview == null) }
    var missed by remember { mutableStateOf(false) }
    val tapped = remember(round, attempt) { mutableStateListOf<Int>() }
    LaunchedEffect(round, attempt) {
        if (preview != null) return@LaunchedEffect
        watching = true
        delay(700)
        sequence.forEach { cell ->
            lit = cell; delay(560); lit = -1; delay(240)
        }
        watching = false
    }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        MissionHeader(
            stringResource(
                when {
                    missed -> R.string.alarm_memory_wrong
                    watching -> R.string.alarm_memory_watch
                    else -> R.string.alarm_memory_repeat
                },
            ),
            step?.first ?: round,
            step?.second ?: lengths.size,
        )
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth(0.86f)) {
            (0 until 3).forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    (0 until 3).forEach { c ->
                        val cell = r * 3 + c
                        val on = lit == cell || preview?.contains(cell) == true
                        val color by animateColorAsState(
                            if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            tween(140),
                            label = "cell",
                        )
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(22.dp)).background(color)
                                .clickable(enabled = !watching) {
                                    onProgress()
                                    missed = false
                                    if (sequence[tapped.size] == cell) {
                                        tapped += cell
                                        if (tapped.size == sequence.size) {
                                            if (round + 1 >= lengths.size) onSolved() else round++
                                        }
                                    } else {
                                        missed = true
                                        attempt++
                                    }
                                },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sequence.indices.forEach { i ->
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (i < tapped.size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

// ---- Number hunt ------------------------------------------------------------------------------

@Composable
internal fun PuzzleMission(
    onProgress: () -> Unit,
    onSolved: () -> Unit,
    seed: Long = System.nanoTime(),
    startAt: Int = 1,
    size: Int = 4,
    step: Pair<Int, Int>? = null,
) = AlarmBackdrop {
    val last = size * size
    val numbers = remember { (1..last).shuffled(Random(seed)) }
    var next by remember { mutableIntStateOf(startAt) }
    var miss by remember { mutableIntStateOf(-1) }
    LaunchedEffect(miss) { if (miss >= 0) { delay(350); miss = -1 } }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        MissionHeader(
            if (last == 16) stringResource(R.string.alarm_puzzle_title) else stringResource(R.string.alarm_puzzle_title9),
            step?.first ?: (next - 1),
            step?.second ?: last,
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.alarm_puzzle_next, next), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            numbers.chunked(size).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { n ->
                        val done = n < next
                        val color by animateColorAsState(
                            when {
                                n == miss -> MaterialTheme.colorScheme.errorContainer
                                done -> Color.Transparent
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            label = "num",
                        )
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(18.dp)).background(color)
                                .clickable(enabled = !done) {
                                    onProgress()
                                    if (n == next) {
                                        next++
                                        if (next > last) onSolved()
                                    } else {
                                        miss = n
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                n.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (done) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

// ---- Good morning -----------------------------------------------------------------------------

@Composable
internal fun MorningPage(
    greeting: String,
    date: String,
    streak: Int?,
    habits: List<TodayHabit>,
    onStartDay: () -> Unit,
) = AlarmBackdrop {
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp)) {
        Spacer(Modifier.height(56.dp))
        Text("☀️", fontSize = 56.sp)
        Spacer(Modifier.height(10.dp))
        Text(greeting, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
        Text(date, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (streak != null && streak > 0) {
            Spacer(Modifier.height(20.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Text(
                    stringResource(R.string.alarm_wake_streak, streak),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.alarm_today_list), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        val pending = habits.filterNot { it.isComplete }
        if (pending.isEmpty()) {
            Text(stringResource(R.string.alarm_all_clear), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(vertical = 6.dp),
            ) {
                pending.take(6).forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.habit.emoji, fontSize = 22.sp)
                        Text(
                            item.habit.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp).weight(1f),
                            maxLines = 1,
                        )
                        item.habit.reminderMinutes?.let { m ->
                            Text("%02d:%02d".format(m / 60, m % 60), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onStartDay, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(24.dp)) {
            Text(stringResource(R.string.alarm_start_day), style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ---- One verification, several kinds of task --------------------------------------------------

internal enum class VerificationTask { Math, Memory, Hunt }

/** Two of each task (sum, memory round, 1-9 number hunt), shuffled so the same kind never comes twice in a row. */
@Composable
internal fun VerificationFlow(
    onProgress: () -> Unit,
    onSolved: () -> Unit,
    seed: Long = System.nanoTime(),
    order: List<VerificationTask> = remember { verificationOrder(Random(seed)) },
    startAt: Int = 0,
) {
    var index by remember { mutableIntStateOf(startAt) }
    val advance: () -> Unit = {
        onProgress()
        if (index + 1 >= order.size) onSolved() else index++
    }
    val step = index to order.size
    androidx.compose.animation.AnimatedContent(
        targetState = index,
        transitionSpec = {
            (androidx.compose.animation.slideInHorizontally { it / 3 } + androidx.compose.animation.fadeIn()) togetherWith
                (androidx.compose.animation.slideOutHorizontally { -it / 3 } + androidx.compose.animation.fadeOut())
        },
        label = "verification",
    ) { i ->
        val taskSeed = seed + i * 7919
        when (order[i]) {
            VerificationTask.Math -> MathMission(onProgress, advance, seed = taskSeed, count = 1, step = step)
            VerificationTask.Memory -> MemoryMission(onProgress, advance, seed = taskSeed, lengths = listOf(4), step = step)
            VerificationTask.Hunt -> PuzzleMission(onProgress, advance, seed = taskSeed, size = 3, step = step)
        }
    }
}

internal fun verificationOrder(random: Random, each: Int = 2): List<VerificationTask> {
    val pool = VerificationTask.entries.flatMap { task -> List(each) { task } }
    repeat(50) {
        val order = pool.shuffled(random)
        if (order.zipWithNext().none { (a, b) -> a == b }) return order
    }
    return List(each) { VerificationTask.entries }.flatten()
}
