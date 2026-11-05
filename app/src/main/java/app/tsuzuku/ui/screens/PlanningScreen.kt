package app.tsuzuku.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsuzuku.R
import app.tsuzuku.data.HealthConnectRepository
import app.tsuzuku.data.HealthConnectState
import app.tsuzuku.data.preferences.AppSettings
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.WeeklyReview
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.tsuzuku.model.WeeklyAiCommentary
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    history: HabitHistorySnapshot,
    reviews: List<WeeklyReview>,
    settings: AppSettings,
    healthRepository: HealthConnectRepository,
    weeklyCommentary: WeeklyAiCommentary? = null,
    onRequestWeeklyCommentary: (() -> Unit)? = null,
    onBack: () -> Unit,
    onApplyVacation: suspend (LocalDate, LocalDate) -> Unit,
    onClearVacation: suspend (LocalDate, LocalDate) -> Unit,
    onSaveReview: suspend (LocalDate, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val weekStats = history.statistics(7, today)
    val currentReview = reviews.firstOrNull { it.weekStart == weekStart }
    var reviewNote by rememberSaveable(currentReview?.updatedAt) { mutableStateOf(currentReview?.note.orEmpty()) }
    var showVacationPicker by rememberSaveable { mutableStateOf(false) }
    val vacationPickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = settings.vacationStartEpochDay?.let { LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
        initialSelectedEndDateMillis = settings.vacationEndEpochDay?.let { LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
    )
    var message by rememberSaveable { mutableStateOf("") }
    val healthState by healthRepository.state.collectAsStateWithLifecycle()
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { scope.launch { healthRepository.refreshAndSync() } }

    if (showVacationPicker) {
        DatePickerDialog(
            onDismissRequest = { showVacationPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = vacationPickerState.selectedStartDateMillis?.toUtcDate()
                    val end = vacationPickerState.selectedEndDateMillis?.toUtcDate()
                    if (start == null || end == null || end.isBefore(start)) {
                        message = "invalid"
                    } else {
                        scope.launch { onApplyVacation(start, end); message = "vacation" }
                        showVacationPicker = false
                    }
                }) { Text(stringResource(R.string.confirm_label)) }
            },
            dismissButton = { TextButton(onClick = { showVacationPicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DateRangePicker(
                state = vacationPickerState,
                modifier = Modifier.fillMaxWidth(),
                title = { Text(stringResource(R.string.choose_vacation_dates), Modifier.padding(24.dp, 16.dp)) },
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader(stringResource(R.string.planning_and_review), onBack) }
        item {
            FeatureCard(Icons.Rounded.Hotel, stringResource(R.string.vacation_mode), stringResource(R.string.vacation_mode_description)) {
                val selectedStart = vacationPickerState.selectedStartDateMillis?.toUtcDate()
                val selectedEnd = vacationPickerState.selectedEndDateMillis?.toUtcDate()
                Text(
                    if (selectedStart != null && selectedEnd != null) "$selectedStart  —  $selectedEnd" else stringResource(R.string.no_vacation_dates_selected),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showVacationPicker = true }) { Text(stringResource(R.string.choose_dates)) }
                    if (settings.vacationStartEpochDay != null && settings.vacationEndEpochDay != null) {
                        TextButton(onClick = {
                            scope.launch {
                                onClearVacation(LocalDate.ofEpochDay(settings.vacationStartEpochDay), LocalDate.ofEpochDay(settings.vacationEndEpochDay))
                                vacationPickerState.setSelection(null, null)
                                message = "cleared"
                            }
                        }) { Text(stringResource(R.string.end_vacation)) }
                    }
                }
            }
        }
        item {
            FeatureCard(Icons.Rounded.RateReview, stringResource(R.string.weekly_review), stringResource(R.string.weekly_review_description)) {
                Text(stringResource(R.string.weekly_review_stats, weekStats.completedCount, weekStats.scheduledCount, (weekStats.completionRate * 100).toInt()))

                if (weeklyCommentary != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🌱", fontSize = 16.sp)
                                Text(weeklyCommentary.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(weeklyCommentary.summary, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(weeklyCommentary.suggestion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(weeklyCommentary.focusHighlight, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else if (onRequestWeeklyCommentary != null) {
                    androidx.compose.material3.FilledTonalButton(
                        onClick = onRequestWeeklyCommentary,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.get_ai_coach_review))
                    }
                }

                OutlinedTextField(
                    value = reviewNote,
                    onValueChange = { reviewNote = it.take(2_000) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text(stringResource(R.string.weekly_note)) },
                )
                Button(onClick = { scope.launch { onSaveReview(weekStart, reviewNote); message = "review" } }) {
                    Text(stringResource(R.string.save_weekly_review))
                }
            }
        }
        item {
            FeatureCard(Icons.Rounded.HealthAndSafety, stringResource(R.string.health_connect), stringResource(R.string.health_connect_description)) {
                when (val state = healthState) {
                    HealthConnectState.Unavailable -> Text(stringResource(R.string.health_connect_unavailable))
                    HealthConnectState.PermissionRequired -> Button(onClick = { healthPermissionLauncher.launch(healthRepository.permissions) }) { Text(stringResource(R.string.connect_health_connect)) }
                    is HealthConnectState.Connected -> {
                        Text(stringResource(R.string.today_steps, state.todaySteps))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { scope.launch { healthRepository.refreshAndSync() } }) { Text(stringResource(R.string.refresh_label)) }
                            TextButton(onClick = { scope.launch { healthRepository.disconnect() } }) { Text(stringResource(R.string.disconnect_label)) }
                        }
                    }
                    is HealthConnectState.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { scope.launch { healthRepository.refreshAndSync() } }) { Text(stringResource(R.string.retry_label)) }
                    }
                }
            }
        }
        if (message.isNotBlank()) item {
            Text(
                stringResource(
                    when (message) {
                        "invalid" -> R.string.invalid_vacation_dates
                        "vacation" -> R.string.vacation_activated
                        "cleared" -> R.string.vacation_ended
                        else -> R.string.weekly_review_saved
                    },
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
                Column { Text(title, style = MaterialTheme.typography.titleLarge); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            }
            content()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
