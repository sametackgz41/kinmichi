package app.tsuzuku.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun FlexibleScheduleFields(
    repeatInterval: String,
    onRepeatIntervalChange: (String) -> Unit,
    activeStartEpochDay: Long?,
    onActiveStartChange: (Long?) -> Unit,
    activeEndEpochDay: Long?,
    onActiveEndChange: (Long?) -> Unit,
    pausedUntilEpochDay: Long?,
    onPausedUntilChange: (Long?) -> Unit,
) {
    Text(stringResource(R.string.flexible_schedule))
    OutlinedTextField(
        value = repeatInterval,
        onValueChange = onRepeatIntervalChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.repeat_every_days)) },
        supportingText = { Text(stringResource(R.string.repeat_interval_help)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
    Spacer(Modifier.height(10.dp))
    HabitDateField(stringResource(R.string.start_date_optional), activeStartEpochDay, onActiveStartChange)
    Spacer(Modifier.height(10.dp))
    HabitDateField(stringResource(R.string.end_date_optional), activeEndEpochDay, onActiveEndChange)
    Spacer(Modifier.height(10.dp))
    HabitDateField(stringResource(R.string.pause_until_optional), pausedUntilEpochDay, onPausedUntilChange)
}

@Composable
private fun HabitDateField(label: String, epochDay: Long?, onChange: (Long?) -> Unit) {
    val context = LocalContext.current
    val initial = epochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
    OutlinedButton(onClick = {
        android.app.DatePickerDialog(
            context,
            { _, year, month, day -> onChange(LocalDate.of(year, month + 1, day).toEpochDay()) },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth,
        ).show()
    }, modifier = Modifier.fillMaxWidth()) {
        Text(epochDay?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d MMMM yyyy")) } ?: label, modifier = Modifier.weight(1f))
    }
    if (epochDay != null) TextButton(onClick = { onChange(null) }) { Text(stringResource(R.string.clear_label)) }
}
