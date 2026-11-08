package app.tsuzuku.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.PlanTaskDraft
import app.tsuzuku.model.PlanTask
import app.tsuzuku.data.local.CategoryEntity
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTaskEditorScreen(existing: PlanTask? = null, customCategories: List<CategoryEntity> = emptyList(), onBack: () -> Unit, onSave: (PlanTaskDraft) -> Unit, onSaveTemplate: (PlanTaskDraft) -> Unit = {}) {
    val context = LocalContext.current
    val draftStore = remember { context.getSharedPreferences("plan-editor-draft", android.content.Context.MODE_PRIVATE) }
    val today = LocalDate.now().toEpochDay()
    val initialDayChoice = when (existing?.scheduledEpochDay) { null -> if (existing == null) draftStore.getInt("day", 0) else 2; today -> 0; today + 1 -> 1; else -> 3 }
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title ?: draftStore.getString("title", "").orEmpty()) }
    var note by rememberSaveable(existing?.id) { mutableStateOf(existing?.note ?: draftStore.getString("note", "").orEmpty()) }
    var dayChoice by rememberSaveable(existing?.id) { mutableIntStateOf(initialDayChoice) }
    var customEpochDay by rememberSaveable(existing?.id) { mutableStateOf(existing?.scheduledEpochDay?.takeIf { it != today && it != today + 1 }) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customEpochDay?.let { LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() })
    var priority by rememberSaveable(existing?.id) { mutableIntStateOf(existing?.priority ?: draftStore.getInt("priority", draftStore.getInt("last_priority", 1))) }
    var estimate by rememberSaveable(existing?.id) { mutableStateOf(existing?.estimatedMinutes?.toString() ?: draftStore.getString("estimate", draftStore.getString("last_estimate", "")).orEmpty()) }
    var startTime by rememberSaveable(existing?.id) { mutableStateOf(existing?.startMinutes?.toClock() ?: draftStore.getString("start", draftStore.getString("last_start", "")).orEmpty()) }
    var endTime by rememberSaveable(existing?.id) { mutableStateOf(existing?.endMinutes?.toClock() ?: draftStore.getString("end", draftStore.getString("last_end", "")).orEmpty()) }
    var recurrence by rememberSaveable(existing?.id) { mutableStateOf(existing?.recurrence ?: draftStore.getString("recurrence", draftStore.getString("last_recurrence", "None")).orEmpty()) }
    var tags by rememberSaveable(existing?.id) { mutableStateOf(existing?.tags ?: draftStore.getString("tags", "").orEmpty()) }

    LaunchedEffect(title, note, dayChoice, priority, estimate, startTime, endTime, recurrence, tags) {
        if (existing == null) draftStore.edit()
            .putString("title", title).putString("note", note).putInt("day", dayChoice).putInt("priority", priority)
            .putString("estimate", estimate).putString("start", startTime).putString("end", endTime).putString("recurrence", recurrence)
            .putString("tags", tags)
            .apply()
    }

    if (showDatePicker) DatePickerDialog(
        onDismissRequest = { showDatePicker = false },
        confirmButton = {
            TextButton(onClick = {
                customEpochDay = datePickerState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                if (customEpochDay != null) dayChoice = 3
                showDatePicker = false
            }) { Text(stringResource(R.string.confirm_label)) }
        },
        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(datePickerState) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeader(stringResource(if (existing == null) R.string.new_task else R.string.edit_task), onBack) }
        item {
            Text(stringResource(R.string.task_identity), style = MaterialTheme.typography.headlineSmall)
        }
        item { OutlinedTextField(title, { title = it.take(120) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.task_title)) }, singleLine = true) }
        item { OutlinedTextField(note, { note = it.take(2_000) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.general_note)) }, minLines = 3) }
        item { OutlinedTextField(tags, { tags = it.take(240) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.tags_label)) }, supportingText = { Text(stringResource(R.string.tags_help)) }, singleLine = true) }
        item { CategorySuggestions(customCategories, tags) { tags = it } }
        item {
            Text(stringResource(R.string.schedule), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.today, R.string.tomorrow, R.string.inbox).forEachIndexed { index, label ->
                    FilterChip(selected = dayChoice == index, onClick = { dayChoice = index }, label = { Text(stringResource(label)) })
                }
                FilterChip(
                    selected = dayChoice == 3,
                    onClick = { showDatePicker = true },
                    label = { Text(customEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: stringResource(R.string.choose_date)) },
                )
            }
        }
        item {
            Text(stringResource(R.string.priority), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..3).forEach { value -> FilterChip(selected = priority == value, onClick = { priority = value }, label = { Text(value.toString()) }) }
            }
        }
        item { OutlinedTextField(estimate, { estimate = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.estimated_minutes)) }, singleLine = true) }
        item {
            Text(stringResource(R.string.time_block_optional), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(startTime, { startTime = it.filter { char -> char.isDigit() || char == ':' }.take(5) }, Modifier.weight(1f), label = { Text(stringResource(R.string.start_time)) }, placeholder = { Text("09:00") }, singleLine = true)
                OutlinedTextField(endTime, { endTime = it.filter { char -> char.isDigit() || char == ':' }.take(5) }, Modifier.weight(1f), label = { Text(stringResource(R.string.end_time)) }, placeholder = { Text("10:00") }, singleLine = true)
            }
        }
        item {
            Text(stringResource(R.string.repeat_task), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("None", "Daily", "Weekdays", "Weekly", "Monthly").forEach { value ->
                    FilterChip(selected = recurrence == value, onClick = { recurrence = value }, label = { Text(stringResource(when (value) {
                        "Daily" -> R.string.every_day
                        "Weekdays" -> R.string.weekdays_only
                        "Weekly" -> R.string.every_week
                        "Monthly" -> R.string.every_month
                        else -> R.string.none_label
                    })) })
                }
            }
        }
        item {
            val draft = PlanTaskDraft(
                title = title, note = note,
                scheduledEpochDay = when (dayChoice) { 0 -> LocalDate.now().toEpochDay(); 1 -> LocalDate.now().plusDays(1).toEpochDay(); 2 -> null; else -> customEpochDay },
                priority = priority, estimatedMinutes = estimate.toIntOrNull(),
                startMinutes = startTime.toMinutesOrNull(), endMinutes = endTime.toMinutesOrNull(), recurrence = recurrence,
                tags = tags,
            )
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    draftStore.edit().remove("title").remove("note").remove("day").remove("priority").remove("estimate").remove("start").remove("end").remove("recurrence").remove("tags")
                        .putInt("last_priority", priority).putString("last_estimate", estimate).putString("last_start", startTime).putString("last_end", endTime).putString("last_recurrence", recurrence).apply()
                    onSave(draft)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save_task)) }
            if (existing == null) androidx.compose.material3.OutlinedButton(
                enabled = title.isNotBlank(),
                onClick = {
                    draftStore.edit().remove("title").remove("note").remove("day").remove("priority").remove("estimate").remove("start").remove("end").remove("recurrence").remove("tags")
                        .putInt("last_priority", priority).putString("last_estimate", estimate).putString("last_start", startTime).putString("last_end", endTime).putString("last_recurrence", recurrence).apply()
                    onSaveTemplate(draft)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(stringResource(R.string.save_as_template)) }
        }
    }
}

private fun Int.toClock(): String = app.tsuzuku.ui.formatClock(this)

private fun String.toMinutesOrNull(): Int? {
    if (isBlank()) return null
    return app.tsuzuku.ui.parseClock(this)
}
