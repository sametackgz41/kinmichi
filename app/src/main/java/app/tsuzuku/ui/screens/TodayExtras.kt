package app.tsuzuku.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.TodayHabit
import kotlin.random.Random

/** How the Today list is laid out; kept on the device. */
object TodayViewPrefs {
    private const val PREFS = "today_view"
    var groupByTime by mutableStateOf(false)
        private set
    var compact by mutableStateOf(false)
        private set
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        groupByTime = p.getBoolean("group_by_time", false)
        compact = p.getBoolean("compact", false)
    }

    fun setGroupByTime(context: Context, on: Boolean) {
        groupByTime = on
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("group_by_time", on).apply()
    }

    fun setCompact(context: Context, on: Boolean) {
        compact = on
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("compact", on).apply()
    }
}

/** Part of the day a habit belongs to, from its reminder time. */
internal enum class DayPart(val label: Int) {
    Morning(R.string.day_part_morning),
    Afternoon(R.string.day_part_afternoon),
    Evening(R.string.day_part_evening),
    Anytime(R.string.day_part_anytime),
}

internal fun dayPartOf(reminderMinutes: Int?): DayPart = when {
    reminderMinutes == null -> DayPart.Anytime
    reminderMinutes < 12 * 60 -> DayPart.Morning
    reminderMinutes < 17 * 60 -> DayPart.Afternoon
    else -> DayPart.Evening
}

/** Groups for the time-of-day view: the part of the day we're in comes first, "any time" last. */
internal fun groupByDayPart(habits: List<TodayHabit>, nowMinutes: Int): List<Pair<DayPart, List<TodayHabit>>> {
    val current = dayPartOf(nowMinutes)
    val order = listOf(DayPart.Morning, DayPart.Afternoon, DayPart.Evening)
    val rotated = order.drop(order.indexOf(current)) + order.take(order.indexOf(current)) + DayPart.Anytime
    val byPart = habits.groupBy { dayPartOf(it.habit.reminderMinutes) }
    return rotated.mapNotNull { part -> byPart[part]?.takeIf { it.isNotEmpty() }?.let { part to it } }
}

/**
 * Swipe right to finish (or +1 for counts), swipe left for the bad-day version (or to skip with a
 * reason). The row springs back; the list itself shows the result.
 */
@Composable
internal fun SwipeableHabit(
    todayHabit: TodayHabit,
    enabled: Boolean,
    onComplete: () -> Unit,
    onLeft: () -> Unit,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { haptic.performHapticFeedback(HapticFeedbackType.Confirm); onComplete() }
                SwipeToDismissBoxValue.EndToStart -> { haptic.performHapticFeedback(HapticFeedbackType.ContextClick); onLeft() }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false // never dismiss: the row stays and updates in place
        },
        positionalThreshold = { it * 0.35f },
    )
    val done = todayHabit.isComplete
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = enabled && !done,
        enableDismissFromEndToStart = enabled && !done,
        backgroundContent = {
            // Only while a swipe is under way: done rows are see-through and must not show it.
            if (state.dismissDirection == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val toRight = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val color = if (toRight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
            val onColor = if (toRight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
            Box(
                Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(color).padding(horizontal = 22.dp),
                contentAlignment = if (toRight) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (toRight) Icons.Rounded.Check else Icons.Rounded.Spa, null, tint = onColor)
                    Text(
                        stringResource(
                            when {
                                toRight && todayHabit.habit.type == HabitType.Count -> R.string.swipe_add_one
                                toRight -> R.string.swipe_done
                                todayHabit.habit.tinyVersion.isNotBlank() -> R.string.swipe_tiny
                                else -> R.string.swipe_skip
                            },
                        ),
                        color = onColor,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
    ) { content() }
}

/** A short burst of gold that falls once when the last habit of the day is done. */
@Composable
internal fun GoldConfetti(trigger: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val pieces = remember {
        val r = Random(42)
        List(70) { floatArrayOf(r.nextFloat(), r.nextFloat() * 0.35f, 0.6f + r.nextFloat() * 0.8f, r.nextFloat() * 360f, r.nextFloat()) }
    }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1_800, easing = LinearEasing))
        }
    }
    val gold = Color(0xFFD9B45A)
    val goldDeep = Color(0xFFB8902E)
    val indigo = MaterialTheme.colorScheme.primary
    if (progress.value in 0.001f..0.999f) {
        Canvas(modifier.fillMaxSize()) {
            val t = progress.value
            pieces.forEachIndexed { i, p ->
                val x = size.width * (p[0] + kotlin.math.sin((t * 6 + i) * p[2]) * 0.03f)
                val y = -40f + (size.height * 0.75f) * (p[1] + t * p[2])
                val alpha = (1f - t).coerceIn(0f, 1f)
                val color = when (i % 5) { 0 -> indigo; 1, 2 -> goldDeep; else -> gold }.copy(alpha = alpha)
                val w = 10f + p[4] * 10f
                drawRect(color, topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(w, w * 0.45f))
            }
        }
    }
}

/** The gold "ta-da" buzz for a finished day (a little longer than a tick). */
internal fun celebrateVibration(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
    } ?: return
    if (!vibrator.hasVibrator()) return
    runCatching {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 70, 30, 70, 90), intArrayOf(0, 140, 0, 180, 0, 255), -1))
    }
}

/**
 * Today's habits with drag handles: press and hold a row, drag it, let go. The new order is
 * handed back once, on drop.
 */
@Composable
internal fun ReorderableHabitList(habits: List<TodayHabit>, onReorder: (List<Long>) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val items: SnapshotStateList<TodayHabit> = remember(habits.map { it.habit.id }) { habits.toMutableStateList() }
    var dragging by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(1) }
    val gap = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            val id = item.habit.id
            val isDragging = dragging == id
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { rowHeight = it.height }
                    .graphicsLayer { translationY = if (isDragging) dragOffset else 0f; scaleX = if (isDragging) 1.02f else 1f; scaleY = scaleX }
                    .then(if (isDragging) Modifier.shadow(8.dp, MaterialTheme.shapes.medium) else Modifier)
                    .pointerInput(id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); dragging = id; dragOffset = 0f },
                            onDragEnd = { dragging = null; dragOffset = 0f; onReorder(items.map { it.habit.id }) },
                            onDragCancel = { dragging = null; dragOffset = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                val step = rowHeight + gap
                                val index = items.indexOfFirst { it.habit.id == id }
                                if (dragOffset > step * 0.55f && index < items.lastIndex) {
                                    items.add(index + 1, items.removeAt(index)); dragOffset -= step
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                } else if (dragOffset < -step * 0.55f && index > 0) {
                                    items.add(index - 1, items.removeAt(index)); dragOffset += step
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            },
                        )
                    },
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.habit.emoji, fontSize = 20.sp)
                    Text(
                        item.habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp).weight(1f),
                    )
                    Icon(Icons.Rounded.DragHandle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

/** A small rounded tag above a group, e.g. "Morning". */
@Composable
internal fun DayPartHeader(part: DayPart, current: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(part.label),
            style = MaterialTheme.typography.titleSmall,
            color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (current) {
            Box(
                Modifier.padding(start = 8.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) { Text(stringResource(R.string.day_part_now), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer) }
        }
    }
}
