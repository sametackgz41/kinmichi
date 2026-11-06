package app.tsuzuku.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
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
        if (done) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainer,
        label = "habit-row-container",
    )
    val contentAlpha by animateFloatAsState(if (done) 0.62f else 1f, tween(300), label = "habit-row-alpha")

    val skipLabel = stringResource(R.string.skip_with_reason)
    val undoSkipLabel = stringResource(R.string.undo_skip)
    val decreaseLabel = stringResource(R.string.decrease_progress)

    Surface(
        color = container,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
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
                    .size(44.dp)
                    .alpha(contentAlpha)
                    .background(accent.copy(alpha = 0.16f), CircleShape),
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
                        StreakBadge(todayHabit.streakDays)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        habitContextLine(todayHabit, timerRunning, timerElapsedSeconds),
                        style = MaterialTheme.typography.bodySmall,
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
private fun StreakBadge(days: Int) {
    val bump = remember { Animatable(1f) }
    var last by remember { mutableStateOf(days) }
    LaunchedEffect(days) {
        if (days > last) {
            bump.animateTo(1.35f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
            bump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        last = days
    }
    val description = pluralStringResource(R.plurals.streak_days, days, days)
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
                .size(32.dp)
                .scale(squash.value)
                .background(fill, CircleShape)
                .border(2.dp, ring, CircleShape)
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
                    modifier = Modifier.size(20.dp),
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
 * Compact daily progress: an animated ring, a counter that rolls to the new value and a
 * short celebration line when everything is done.
 */
@Composable
internal fun TodayProgressHeader(completed: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction = if (total == 0) 0f else completed.toFloat() / total
    val animatedFraction by animateFloatAsState(
        fraction,
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "day-progress",
    )
    val percent by animateIntAsState((fraction * 100).toInt(), tween(600), label = "day-percent")
    val allDone = total > 0 && completed == total
    val celebrate = remember { Animatable(1f) }
    val haptic = LocalHapticFeedback.current
    // Starts as the current value so opening an already finished day doesn't buzz.
    var wasAllDone by remember { mutableStateOf(allDone) }
    LaunchedEffect(allDone) {
        if (allDone && !wasAllDone) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        wasAllDone = allDone
        if (allDone) {
            celebrate.animateTo(1.12f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            celebrate.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).scale(celebrate.value), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                    gapSize = 0.dp,
                )
                Text(
                    "$percent%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                AnimatedContent(
                    targetState = allDone,
                    transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith fadeOut() },
                    label = "day-title",
                ) { finished ->
                    Text(
                        stringResource(if (finished) R.string.today_all_done else R.string.daily_progress),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
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
                ) { count ->
                    Text(
                        stringResource(R.string.habits_done_of, count, total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}
