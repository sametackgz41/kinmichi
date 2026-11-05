package app.tsuzuku.ui.screens

import app.tsuzuku.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.model.PlanTask
import java.time.LocalDate

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCarrySheet(
    task: PlanTask,
    onDismiss: () -> Unit,
    onDoToday: () -> Unit,
    onMoveTomorrow: () -> Unit,
    onMoveNextWeek: () -> Unit,
    onDivideIntoSubtasks: (List<String>) -> Unit,
    onDeleteOrCancel: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSplittingMode by remember { mutableStateOf(false) }
    var step1 by remember { mutableStateOf("") }
    var step2 by remember { mutableStateOf("") }
    var step3 by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                Text(
                    stringResource(R.string.carry_count_title, task.carryCount),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "\"${task.title}\"",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isSplittingMode) {
                        stringResource(R.string.carry_split_hint)
                    } else {
                        stringResource(R.string.carry_intro)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isSplittingMode) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = step1,
                        onValueChange = { step1 = it },
                        label = { Text(stringResource(R.string.carry_step_1)) },
                        placeholder = { Text(stringResource(R.string.carry_step_1_example)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = step2,
                        onValueChange = { step2 = it },
                        label = { Text(stringResource(R.string.carry_step_2)) },
                        placeholder = { Text(stringResource(R.string.carry_step_2_example)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = step3,
                        onValueChange = { step3 = it },
                        label = { Text(stringResource(R.string.carry_step_3)) },
                        placeholder = { Text(stringResource(R.string.carry_step_3_example)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { isSplittingMode = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(stringResource(R.string.back))
                        }
                        Button(
                            onClick = {
                                val steps = listOf(step1, step2, step3).map(String::trim).filter(String::isNotBlank)
                                if (steps.isNotEmpty()) {
                                    onDivideIntoSubtasks(steps)
                                    onDismiss()
                                }
                            },
                            enabled = step1.isNotBlank() || step2.isNotBlank() || step3.isNotBlank(),
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(stringResource(R.string.add_steps_and_start))
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CarryOptionItem(
                        icon = Icons.Rounded.PlayArrow,
                        title = stringResource(R.string.do_today),
                        subtitle = stringResource(R.string.do_today_desc),
                        onClick = { onDoToday(); onDismiss() },
                    )
                    CarryOptionItem(
                        icon = Icons.Rounded.CalendarMonth,
                        title = stringResource(R.string.move_to_tomorrow),
                        subtitle = stringResource(R.string.move_tomorrow_desc),
                        onClick = { onMoveTomorrow(); onDismiss() },
                    )
                    CarryOptionItem(
                        icon = Icons.Rounded.CalendarMonth,
                        title = stringResource(R.string.postpone_next_week),
                        subtitle = stringResource(R.string.postpone_next_week_desc),
                        onClick = { onMoveNextWeek(); onDismiss() },
                    )
                    CarryOptionItem(
                        icon = Icons.Rounded.FormatListBulleted,
                        title = stringResource(R.string.split_smaller),
                        subtitle = stringResource(R.string.split_smaller_desc),
                        onClick = { isSplittingMode = true },
                    )
                    CarryOptionItem(
                        icon = Icons.Rounded.Close,
                        title = stringResource(R.string.cancel_or_delete_task),
                        subtitle = stringResource(R.string.cancel_or_delete_task_desc),
                        onClick = { onDeleteOrCancel(); onDismiss() },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CarryOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
