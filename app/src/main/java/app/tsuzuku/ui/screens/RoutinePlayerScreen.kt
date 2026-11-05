package app.tsuzuku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.ui.theme.Success

@Composable
fun RoutinePlayerScreen(
    routineName: String,
    routineHabits: List<TodayHabit>,
    onToggleHabit: (Long) -> Unit,
    onIncrementHabit: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val totalCount = routineHabits.size
    val completedCount = routineHabits.count { it.isComplete }
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    // Find the first non-completed habit index as current step
    val currentStepIndex = routineHabits.indexOfFirst { !it.isComplete }.let { if (it == -1) totalCount - 1 else it }

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
                        text = "日課 · ROUTINE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        text = routineName.ifBlank { stringResource(R.string.routine_label) },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Routine Progress Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.routine_step, (completedCount + 1).coerceAtMost(totalCount), totalCount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                }
            }
        }

        // Steps List
        item {
            Text(
                text = stringResource(R.string.routine_steps),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        items(routineHabits.size) { index ->
            val todayHabit = routineHabits[index]
            val habit = todayHabit.habit
            val isDone = todayHabit.isComplete
            val isCurrent = index == currentStepIndex
            val isBlocked = todayHabit.isBlocked

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isDone -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        isCurrent -> MaterialTheme.colorScheme.surface
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    },
                ),
                border = if (isCurrent && !isDone) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDone) Success.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isDone) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Success, modifier = Modifier.size(20.dp))
                            } else if (isBlocked) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            } else {
                                Text(habit.emoji, fontSize = 20.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                            if (habit.target > 1) {
                                Text(
                                    text = "${todayHabit.progress.toInt()} / ${habit.target.toInt()} ${habit.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isBlocked) {
                                Text(
                                    text = todayHabit.blockedByName?.let { stringResource(R.string.must_complete_first, it) } ?: stringResource(R.string.locked_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    if (!isDone && !isBlocked) {
                        if (habit.target > 1) {
                            Button(
                                onClick = { onIncrementHabit(habit.id) },
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("+${habit.step.toInt()}")
                            }
                        } else {
                            Button(
                                onClick = { onToggleHabit(habit.id) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Success),
                            ) {
                                Text(stringResource(R.string.complete_habit))
                            }
                        }
                    }
                }
            }
        }

        if (completedCount == totalCount && totalCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.12f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Success, modifier = Modifier.size(36.dp))
                        Text(
                            text = stringResource(R.string.routine_completed_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Success,
                        )
                        Text(
                            text = stringResource(R.string.routine_completed_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = onBack, shape = RoundedCornerShape(10.dp)) {
                            Text(stringResource(R.string.back_to_today))
                        }
                    }
                }
            }
        }
    }
}
