package app.tsuzuku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Reminder time as large hour and minute boxes right on the page, instead of a text field.
 * [minutes] is minutes after midnight; [onChange] gets the new value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderTimePicker(minutes: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val is24h = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val state = rememberTimePickerState(minutes / 60, minutes % 60, is24h)
    LaunchedEffect(state.hour, state.minute) {
        val chosen = state.hour * 60 + state.minute
        if (chosen != minutes) onChange(chosen)
    }
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) { TimeInput(state = state) }
}
