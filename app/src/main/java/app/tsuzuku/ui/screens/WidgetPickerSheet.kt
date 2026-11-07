package app.tsuzuku.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.widget.HabitWidgetReceiver
import app.tsuzuku.widget.MoodWidgetReceiver
import app.tsuzuku.widget.QuickGridWidgetReceiver
import app.tsuzuku.widget.StreakWidgetReceiver
import app.tsuzuku.widget.TsuzukuWidgetProvider
import app.tsuzuku.widget.WeekWidgetReceiver

/** One home-screen widget kind the user can add straight from Settings. */
private data class WidgetKind(
    val receiver: Class<*>,
    @StringRes val name: Int,
    @StringRes val description: Int,
    val icon: ImageVector,
    @DrawableRes val preview: Int? = null,
)

private val widgetKinds = listOf(
    WidgetKind(TsuzukuWidgetProvider::class.java, R.string.widget_today_name, R.string.widget_description, Icons.Rounded.Today, R.drawable.widget_preview_today),
    WidgetKind(HabitWidgetReceiver::class.java, R.string.widget_habit_name, R.string.widget_habit_description, Icons.Rounded.TaskAlt, R.drawable.widget_preview_habit),
    WidgetKind(WeekWidgetReceiver::class.java, R.string.widget_week_name, R.string.widget_week_description, Icons.Rounded.CalendarViewWeek, R.drawable.widget_preview_week),
    WidgetKind(StreakWidgetReceiver::class.java, R.string.widget_streak_name, R.string.widget_streak_description, Icons.Rounded.LocalFireDepartment),
    WidgetKind(QuickGridWidgetReceiver::class.java, R.string.widget_quick_name, R.string.widget_quick_description, Icons.Rounded.GridView),
    WidgetKind(MoodWidgetReceiver::class.java, R.string.widget_mood_name, R.string.widget_mood_description, Icons.Rounded.Mood),
)

/**
 * Lists every widget with an "Add to home screen" button. Uses the launcher's pin dialog when it
 * supports one (most launchers since Android 8); otherwise explains where the widgets live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val canPin = remember(context) { canPinWidgets(context) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            item {
                Column(Modifier.padding(bottom = 4.dp)) {
                    Text(stringResource(R.string.widgets_sheet_title), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(if (canPin) R.string.widgets_sheet_subtitle else R.string.widgets_sheet_manual),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(widgetKinds) { kind ->
                WidgetKindCard(kind, canPin) {
                    if (!pinWidget(context, kind.receiver)) {
                        Toast.makeText(context, R.string.widgets_sheet_manual, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetKindCard(kind: WidgetKind, canPin: Boolean, onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (kind.preview != null) {
            Image(
                painterResource(kind.preview),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(14.dp)),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(kind.icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(kind.name), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(kind.description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canPin) {
                FilledTonalButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Text(stringResource(R.string.add_to_home), Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

private fun canPinWidgets(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

private fun pinWidget(context: Context, receiver: Class<*>): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
}
