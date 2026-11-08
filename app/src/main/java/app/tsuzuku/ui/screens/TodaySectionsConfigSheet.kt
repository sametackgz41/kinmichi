package app.tsuzuku.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.ui.theme.LookPrefs

/** "Edit the Today screen": only things that are really on it, and each change shows at once. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodaySectionsConfigSheet(
    summaryVisible: Boolean,
    onSummaryVisible: (Boolean) -> Unit,
    onReorder: () -> Unit,
    onClose: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        TodayEditContent(summaryVisible, onSummaryVisible, onReorder = { onClose(); onReorder() })
    }
}

@Composable
internal fun TodayEditContent(summaryVisible: Boolean, onSummaryVisible: (Boolean) -> Unit, onReorder: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.customize_sections), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.today_edit_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Group(stringResource(R.string.today_edit_shown)) {
            ToggleRow(Icons.Rounded.TrackChanges, stringResource(R.string.today_edit_week), stringResource(R.string.today_edit_week_desc), TodayViewPrefs.showWeek) {
                TodayViewPrefs.setShowWeek(context, it)
            }
            ToggleRow(Icons.Rounded.DonutLarge, stringResource(R.string.today_edit_summary), stringResource(R.string.today_edit_summary_desc), summaryVisible, onSummaryVisible)
            ToggleRow(Icons.Rounded.AutoAwesome, stringResource(R.string.today_edit_seasonal), stringResource(R.string.today_edit_seasonal_desc), LookPrefs.seasonal) {
                LookPrefs.setSeasonal(context, it)
            }
        }
        Group(stringResource(R.string.today_edit_list)) {
            ToggleRow(Icons.Rounded.Schedule, stringResource(R.string.today_group_time), stringResource(R.string.today_edit_group_desc), TodayViewPrefs.groupByTime) {
                TodayViewPrefs.setGroupByTime(context, it)
            }
            ToggleRow(Icons.Rounded.ViewAgenda, stringResource(R.string.today_compact), stringResource(R.string.today_edit_compact_desc), TodayViewPrefs.compact) {
                TodayViewPrefs.setCompact(context, it)
            }
        }
        FilledTonalButton(onClick = onReorder, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Icon(Icons.Rounded.SwapVert, null, Modifier.size(18.dp))
            Text(stringResource(R.string.today_reorder), Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
