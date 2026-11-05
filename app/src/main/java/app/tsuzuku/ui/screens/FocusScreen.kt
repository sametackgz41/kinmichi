package app.tsuzuku.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.FocusMode
import app.tsuzuku.model.FocusSession
import app.tsuzuku.model.FocusTargetType
import app.tsuzuku.model.Goal
import app.tsuzuku.model.Habit
import app.tsuzuku.model.PlanTask
import app.tsuzuku.ui.ActiveFocusState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FocusScreen(
    activeState: ActiveFocusState?,
    sessions: List<FocusSession>,
    tasks: List<PlanTask>,
    habits: List<Habit>,
    goals: List<Goal>,
    onStart: (mode: FocusMode, targetType: FocusTargetType, targetId: Long?, targetTitle: String, customMinutes: Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
) {
    var selectedMode by remember { mutableStateOf(FocusMode.Pomodoro) }
    var selectedTargetType by remember { mutableStateOf(FocusTargetType.Free) }
    var selectedTargetId by remember { mutableStateOf<Long?>(null) }
    var customMinutes by remember { mutableIntStateOf(25) }

    val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val todaySessions = sessions.filter { it.startedAt >= todayStart && it.completed }
    val totalMinutesToday = todaySessions.sumOf { it.durationMinutes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(R.string.focus),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Active Focus Card or New Session Launcher
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (activeState != null) {
                        // Timer Display
                        val displaySeconds = if (activeState.mode == FocusMode.Stopwatch) activeState.elapsedSeconds else activeState.remainingSeconds
                        val minutes = displaySeconds / 60
                        val seconds = displaySeconds % 60
                        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                        Text(
                            text = if (activeState.targetTitle.isNotBlank()) activeState.targetTitle else stringResource(R.string.free_focus),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Text(
                            text = when (activeState.mode) {
                                FocusMode.Pomodoro -> stringResource(R.string.pomodoro)
                                FocusMode.Stopwatch -> stringResource(R.string.stopwatch)
                                FocusMode.Custom -> stringResource(R.string.custom_timer)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )

                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (activeState.isPaused) {
                                Button(
                                    onClick = onResume,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.resume_action))
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onPause,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Rounded.Pause, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.pause_action))
                                }
                            }

                            Button(
                                onClick = onFinish,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.finish_and_save))
                            }

                            IconButton(onClick = onCancel) {
                                Icon(Icons.Rounded.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        // Mode Selection
                        Text(
                            text = stringResource(R.string.focus_mode),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedMode == FocusMode.Pomodoro,
                                onClick = { selectedMode = FocusMode.Pomodoro },
                                label = { Text(stringResource(R.string.pomodoro)) },
                            )
                            FilterChip(
                                selected = selectedMode == FocusMode.Stopwatch,
                                onClick = { selectedMode = FocusMode.Stopwatch },
                                label = { Text(stringResource(R.string.stopwatch)) },
                            )
                            FilterChip(
                                selected = selectedMode == FocusMode.Custom,
                                onClick = { selectedMode = FocusMode.Custom },
                                label = { Text(stringResource(R.string.custom_timer)) },
                            )
                        }

                        if (selectedMode == FocusMode.Custom) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(15, 25, 45, 60, 90).forEach { mins ->
                                    FilterChip(
                                        selected = customMinutes == mins,
                                        onClick = { customMinutes = mins },
                                        label = { Text("$mins dk") },
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Target Selection
                        Text(
                            text = stringResource(R.string.focus_target),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedTargetType == FocusTargetType.Free,
                                onClick = {
                                    selectedTargetType = FocusTargetType.Free
                                    selectedTargetId = null
                                },
                                label = { Text(stringResource(R.string.free_focus)) },
                            )
                            FilterChip(
                                selected = selectedTargetType == FocusTargetType.Task,
                                onClick = {
                                    selectedTargetType = FocusTargetType.Task
                                    selectedTargetId = tasks.firstOrNull { !it.completed }?.id
                                },
                                label = { Text(stringResource(R.string.plan)) },
                            )
                            FilterChip(
                                selected = selectedTargetType == FocusTargetType.Habit,
                                onClick = {
                                    selectedTargetType = FocusTargetType.Habit
                                    selectedTargetId = habits.firstOrNull()?.id
                                },
                                label = { Text(stringResource(R.string.habits)) },
                            )
                            FilterChip(
                                selected = selectedTargetType == FocusTargetType.Goal,
                                onClick = {
                                    selectedTargetType = FocusTargetType.Goal
                                    selectedTargetId = goals.firstOrNull()?.id
                                },
                                label = { Text(stringResource(R.string.linked_goals)) },
                            )
                        }

                        // Target Item Picker
                        when (selectedTargetType) {
                            FocusTargetType.Task -> {
                                val activeTasks = tasks.filter { !it.completed }
                                if (activeTasks.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                        activeTasks.take(4).forEach { task ->
                                            val isSelected = selectedTargetId == task.id
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { selectedTargetId = task.id },
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            ) {
                                                Text(
                                                    text = task.title,
                                                    modifier = Modifier.padding(10.dp),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            FocusTargetType.Habit -> {
                                if (habits.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                        habits.take(4).forEach { habit ->
                                            val isSelected = selectedTargetId == habit.id
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { selectedTargetId = habit.id },
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            ) {
                                                Text(
                                                    text = "${habit.emoji} ${habit.name}",
                                                    modifier = Modifier.padding(10.dp),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            FocusTargetType.Goal -> {
                                if (goals.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                        goals.take(4).forEach { goal ->
                                            val isSelected = selectedTargetId == goal.id
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { selectedTargetId = goal.id },
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            ) {
                                                Text(
                                                    text = goal.title,
                                                    modifier = Modifier.padding(10.dp),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            FocusTargetType.Free -> {}
                        }

                        Button(
                            onClick = {
                                val targetTitle = when (selectedTargetType) {
                                    FocusTargetType.Task -> tasks.firstOrNull { it.id == selectedTargetId }?.title.orEmpty()
                                    FocusTargetType.Habit -> habits.firstOrNull { it.id == selectedTargetId }?.name.orEmpty()
                                    FocusTargetType.Goal -> goals.firstOrNull { it.id == selectedTargetId }?.title.orEmpty()
                                    FocusTargetType.Free -> ""
                                }
                                onStart(selectedMode, selectedTargetType, selectedTargetId, targetTitle, customMinutes)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.start_focus))
                        }
                    }
                }
            }
        }

        // Today's summary card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.focus_today),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = stringResource(R.string.focus_minutes_today, totalMinutesToday),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = stringResource(R.string.focus_sessions_count, todaySessions.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Focus Session History
        if (todaySessions.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.sessions_today),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(todaySessions.reversed()) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = when {
                                    session.taskId != null -> tasks.firstOrNull { it.id == session.taskId }?.let { stringResource(R.string.focus_target_task, it.title) } ?: stringResource(R.string.task_label)
                                    session.habitId != null -> habits.firstOrNull { it.id == session.habitId }?.let { stringResource(R.string.focus_target_habit, it.emoji, it.name) } ?: stringResource(R.string.habit_label)
                                    session.goalId != null -> goals.firstOrNull { it.id == session.goalId }?.let { stringResource(R.string.focus_target_goal, it.title) } ?: stringResource(R.string.goal_step)
                                    else -> stringResource(R.string.free_focus)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = session.mode.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${session.durationMinutes} dk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
