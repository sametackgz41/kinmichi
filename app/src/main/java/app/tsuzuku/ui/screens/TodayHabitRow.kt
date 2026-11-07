package app.tsuzuku.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.toPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.GoalPeriod
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.SkipReasonType
import app.tsuzuku.model.TodayHabit
import kotlinx.coroutines.launch

/**
 * A single, quiet row per habit: icon, name, one line of context and one primary action.
 * Tap opens details, long-press offers skip / undo-skip / decrease.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HabitRow(
    todayHabit: TodayHabit,
    timerRunning: Boolean,
    timerElapsedSeconds: Long,
    interactionDisabled: Boolean,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onToggleTimer: () -> Unit,
    onOpen: () -> Unit,
    onOpenSkipDialog: () -> Unit,
    onUndoSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habit = todayHabit.habit
    val accent = Color(habit.colorArgb.toInt())
    val done = todayHabit.isComplete
    val enabled = !todayHabit.isBlocked && !interactionDisabled
    var menuExpanded by remember { mutableStateOf(false) }

    val container by animateColorAsState(
        if (done) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLowest,
        label = "habit-row-container",
    )
    val outline by animateColorAsState(
        if (done) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
        label = "habit-row-outline",
    )
    val contentAlpha by animateFloatAsState(if (done) 0.62f else 1f, tween(300), label = "habit-row-alpha")

    val skipLabel = stringResource(R.string.skip_with_reason)
    val undoSkipLabel = stringResource(R.string.undo_skip)
    val decreaseLabel = stringResource(R.string.decrease_progress)

    Surface(
        color = container,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, outline),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onOpen,
                onLongClick = { menuExpanded = true },
                onLongClickLabel = stringResource(R.string.manage_habit),
            )
            .semantics {
                customActions = buildList {
                    if (todayHabit.skipReason == null) add(CustomAccessibilityAction(skipLabel) { onOpenSkipDialog(); true })
                    else add(CustomAccessibilityAction(undoSkipLabel) { onUndoSkip(); true })
                    if (habit.type == HabitType.Count && todayHabit.progress > 0) {
                        add(CustomAccessibilityAction(decreaseLabel) { onDecrement(); true })
                    }
                }
            },
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .alpha(contentAlpha)
                    .background(accent.copy(alpha = 0.16f), androidx.compose.foundation.shape.RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(habit.emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).alpha(contentAlpha)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (todayHabit.streakDays > 1) {
                        StreakBadge(todayHabit.streakDays, todayHabit.restUsedThisWeek)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        habitContextLine(todayHabit, timerRunning, timerElapsedSeconds),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = app.tsuzuku.ui.theme.SerifDisplay,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                        ),
                        color = when {
                            todayHabit.isBlocked || todayHabit.badHabitLimitExceeded -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box {
                when (habit.type) {
                    HabitType.Check -> CheckButton(done = done, enabled = enabled, onToggle = onToggle)
                    HabitType.Count -> RingButton(
                        fraction = todayHabit.progressFraction,
                        done = done,
                        enabled = enabled,
                        icon = { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.increase_progress)) },
                        onClick = onIncrement,
                    )
                    HabitType.Duration -> RingButton(
                        fraction = todayHabit.progressFraction,
                        done = done,
                        enabled = enabled,
                        icon = {
                            Icon(
                                if (timerRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(if (timerRunning) R.string.stop_timer else R.string.start_timer),
                            )
                        },
                        onClick = onToggleTimer,
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.open_details)) },
                        onClick = { menuExpanded = false; onOpen() },
                    )
                    if (habit.type == HabitType.Count && todayHabit.progress > 0) {
                        DropdownMenuItem(
                            text = { Text(decreaseLabel) },
                            onClick = { menuExpanded = false; onDecrement() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (todayHabit.skipReason == null) skipLabel else undoSkipLabel) },
                        onClick = {
                            menuExpanded = false
                            if (todayHabit.skipReason == null) onOpenSkipDialog() else onUndoSkip()
                        },
                    )
                }
            }
        }
    }
}

/** The single most useful line of context for a habit today. */
@Composable
private fun habitContextLine(todayHabit: TodayHabit, timerRunning: Boolean, timerElapsedSeconds: Long): String {
    val habit = todayHabit.habit
    return when {
        todayHabit.isBlocked -> stringResource(R.string.blocked_by_habit, todayHabit.blockedByName.orEmpty())
        todayHabit.skipReason != null -> SkipReasonType.parse(todayHabit.skipReason).let { (type, label) ->
            "${type.icon} ${label.ifBlank { stringResource(type.labelRes) }}"
        }
        timerRunning -> "%d:%02d".format(timerElapsedSeconds / 60, timerElapsedSeconds % 60)
        todayHabit.badHabitLimitExceeded -> stringResource(
            R.string.limit_exceeded,
            formatHabitAmount(todayHabit.progress),
            formatHabitAmount(habit.maximumTarget ?: habit.target),
            habit.unit,
        )
        habit.type != HabitType.Check -> stringResource(
            R.string.progress_template,
            formatHabitAmount(todayHabit.progress),
            formatHabitAmount(habit.target),
            habit.unit,
        )
        habit.goalPeriod != GoalPeriod.Daily -> stringResource(R.string.period_progress, todayHabit.periodProgress, habit.periodTarget)
        todayHabit.isComplete -> stringResource(R.string.habit_completed)
        else -> when (habit.scheduleDaysMask) {
            127 -> stringResource(R.string.every_day)
            31 -> stringResource(R.string.weekdays)
            96 -> stringResource(R.string.weekends)
            else -> stringResource(R.string.selected_days)
        }
    }
}

internal fun formatHabitAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)

/** Streak flame that bumps whenever the streak grows. */
@Composable
private fun StreakBadge(days: Int, restUsed: Boolean) {
    val bump = remember { Animatable(1f) }
    var last by remember { mutableStateOf(days) }
    LaunchedEffect(days) {
        if (days > last) {
            bump.animateTo(1.35f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
            bump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        last = days
    }
    val description = pluralStringResource(R.plurals.streak_days, days, days) +
        if (restUsed) ", " + stringResource(R.string.rest_day_used) else ""
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .scale(bump.value)
            .semantics { stateDescription = description },
    ) {
        Icon(
            Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(14.dp),
        )
        AnimatedContent(
            targetState = days,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "streak-count",
        ) { value ->
            Text(value.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        }
        if (restUsed) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Rounded.Bedtime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/**
 * Round check button: an outlined ring that fills with the accent and pops a check mark
 * when completed, with a springy squash on press and a confirming haptic.
 */
@Composable
private fun CheckButton(done: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val squash = remember { Animatable(1f) }
    val fill by animateColorAsState(
        if (done) MaterialTheme.colorScheme.primary else Color.Transparent,
        tween(220),
        label = "check-fill",
    )
    val ring by animateColorAsState(
        if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "check-ring",
    )
    val description = stringResource(R.string.complete_habit)
    // Expressive touch: the circle morphs into a scalloped "cookie" when the habit is done.
    val morph = remember { androidx.graphics.shapes.Morph(androidx.compose.material3.MaterialShapes.Circle, androidx.compose.material3.MaterialShapes.Cookie9Sided) }
    val morphProgress by animateFloatAsState(
        if (done) 1f else 0f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "check-morph",
    )
    val shape = remember(morphProgress) { MorphShape(morph, morphProgress) }
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .toggleable(
                value = done,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = {
                    haptic.performHapticFeedback(if (done) HapticFeedbackType.ContextClick else HapticFeedbackType.Confirm)
                    scope.launch {
                        squash.animateTo(0.82f, tween(70))
                        squash.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                    }
                    onToggle()
                },
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .scale(squash.value)
                .graphicsLayer { rotationZ = morphProgress * 40f }
                .background(fill, shape)
                .border(2.dp, ring, shape)
                .alpha(if (enabled) 1f else 0.38f),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = done,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = scaleOut(tween(120)) + fadeOut(tween(120)),
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    // Undo the background's rotation so the check stays upright.
                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = -morphProgress * 40f },
                )
            }
        }
    }
}

/** Progress ring around an action button for count and duration habits. */
@Composable
private fun RingButton(
    fraction: Float,
    done: Boolean,
    enabled: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    // A firmer confirm when this tap is the one that reaches the target.
    var wasDone by remember { mutableStateOf(done) }
    LaunchedEffect(done) {
        if (done && !wasDone) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        wasDone = done
    }
    val animated by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "ring-progress",
    )
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { animated },
            modifier = Modifier.size(40.dp),
            strokeWidth = 3.dp,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            gapSize = 0.dp,
        )
        Box(Modifier.alpha(if (enabled) 1f else 0.38f)) {
            AnimatedContent(targetState = done, label = "ring-icon") { complete ->
                if (complete) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                } else {
                    icon()
                }
            }
        }
    }
}

/**
 * Washi day card: the day's count set large in the serif, the longest running streak beside it
 * and a thin ink bar underneath. Rolls the count and gives a small lift when the day is done.
 */
@Composable
internal fun TodayProgressHeader(completed: Int, total: Int, modifier: Modifier = Modifier, streakDays: Int = 0) {
    val fraction = if (total == 0) 0f else completed.toFloat() / total
    val animatedFraction by animateFloatAsState(
        fraction,
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "day-progress",
    )
    val allDone = total > 0 && completed == total
    val celebrate = remember { Animatable(1f) }
    val haptic = LocalHapticFeedback.current
    // Starts as the current value so opening an already finished day doesn't buzz.
    var wasAllDone by remember { mutableStateOf(allDone) }
    LaunchedEffect(allDone) {
        if (allDone && !wasAllDone) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        wasAllDone = allDone
        if (allDone) {
            celebrate.animateTo(1.06f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            celebrate.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    val countDescription = stringResource(R.string.habits_done_of, completed, total)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = countDescription },
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedContent(
                    targetState = completed,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                        } else {
                            (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                        }
                    },
                    label = "day-count",
                    modifier = Modifier.scale(celebrate.value),
                ) { count ->
                    Text(
                        count.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = if (allDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    " / $total",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 6.dp)) {
                    AnimatedContent(
                        targetState = allDone,
                        transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith fadeOut() },
                        label = "day-title",
                    ) { finished ->
                        Text(
                            stringResource(if (finished) R.string.today_all_done else R.string.daily_progress),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (finished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (streakDays > 1) {
                        Text(
                            pluralStringResource(R.plurals.streak_days, streakDays, streakDays),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

/** One day of the week strip: null progress means the day is still ahead. */
data class WeekDayProgress(val date: java.time.LocalDate, val fraction: Float?)

/**
 * The week as a row of small enso circles: each one drawn as far as that day got,
 * today's label in bold. A finished day closes its circle.
 */
@Composable
internal fun WeekEnsoStrip(days: List<WeekDayProgress>, modifier: Modifier = Modifier) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val today = remember { java.time.LocalDate.now() }
    val track = MaterialTheme.colorScheme.outlineVariant
    val ink = MaterialTheme.colorScheme.primary
    val todayInk = MaterialTheme.colorScheme.onSurface
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            val isToday = day.date == today
            val label = day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale)
            val fraction by animateFloatAsState(day.fraction ?: 0f, tween(500), label = "enso-${day.date}")
            val description = if (day.fraction == null) label else "$label, ${((day.fraction) * 100).toInt()}%"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(40.dp).semantics(mergeDescendants = true) { contentDescription = description },
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) androidx.compose.ui.text.font.FontWeight.Bold else null,
                    color = if (isToday) todayInk else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.Canvas(Modifier.size(32.dp)) {
                    val stroke = 3.5.dp.toPx()
                    val inset = stroke / 2 + 1.dp.toPx()
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                    val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                    drawArc(track, 0f, 360f, false, topLeft, arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke * 0.6f))
                    if (fraction > 0f) {
                        // A brush enso leaves a small gap until the day is complete.
                        val sweep = if (fraction >= 1f) 360f else 330f * fraction
                        drawArc(
                            if (isToday) ink else ink.copy(alpha = 0.75f),
                            -100f, sweep, false, topLeft, arcSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        )
                    }
                }
            }
        }
    }
}

/** A [androidx.graphics.shapes.Morph] frozen at [progress], scaled to the component size. */
private class MorphShape(
    private val morph: androidx.graphics.shapes.Morph,
    private val progress: Float,
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline {
        val path = morph.toPath(progress)
        path.transform(androidx.compose.ui.graphics.Matrix().apply { scale(size.width, size.height) })
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
