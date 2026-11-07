package app.tsuzuku.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch

/** Today's full list on the watch; each row toggles the habit on the phone. */
class WatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { WatchToday() } }
    }
}

@Composable
private fun WatchToday() {
    val context = LocalContext.current
    val today by WatchStore.flow(context).collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { PhoneLink.refresh(context.applicationContext) }
    WatchTodayContent(today) { id -> scope.launch { PhoneLink.toggle(context.applicationContext, id) } }
}

/** The list itself, without the Data Layer, so previews can render it. */
@Composable
internal fun WatchTodayContent(today: WatchToday, onToggle: (Long) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    AppScaffold {
        ScreenScaffold(scrollState = listState) { padding ->
            TransformingLazyColumn(state = listState, contentPadding = padding) {
                item {
                    ListHeader {
                        Text(
                            if (today.isCurrent() && today.habits.isNotEmpty()) {
                                stringResource(R.string.tile_title_progress, today.doneCount, today.habits.size)
                            } else {
                                stringResource(R.string.tile_title)
                            },
                        )
                    }
                }
                if (!today.isCurrent() || today.habits.isEmpty()) {
                    item {
                        Text(
                            stringResource(if (!today.isCurrent()) R.string.tile_waiting else R.string.tile_nothing_today),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    items(today.habits, key = { it.id }) { habit ->
                        CheckboxButton(
                            checked = habit.done,
                            onCheckedChange = { onToggle(habit.id) },
                            modifier = Modifier.fillMaxWidth(),
                            icon = { Text(habit.emoji) },
                            secondaryLabel = if (habit.detail.isNotEmpty()) {
                                { Text(habit.detail, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            } else {
                                null
                            },
                            label = { Text(habit.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
        }
    }
}
