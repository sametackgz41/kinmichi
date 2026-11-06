package app.tsuzuku.wear

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.ButtonDefaults.filledButtonColors
import androidx.wear.protolayout.material3.ButtonDefaults.filledTonalButtonColors
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.button
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.loadAction
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TOGGLE_PREFIX = "toggle:"
private const val REFRESH_ID = "refresh"
private const val TILE_SLOTS = 2

/**
 * "Today" tile: progress in the title, the next habits as big tap targets and an edge button
 * that opens the full list. A tap marks the habit on the phone through the Data Layer.
 */
class TodayTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val clicked = requestParams.currentState.lastClickableId
        when {
            clicked.startsWith(TOGGLE_PREFIX) -> clicked.removePrefix(TOGGLE_PREFIX).toLongOrNull()?.let { id ->
                WatchStore.optimisticToggle(this, id)
                scope.launch { PhoneLink.toggle(applicationContext, id) }
            }
            clicked == REFRESH_ID -> scope.launch { PhoneLink.refresh(applicationContext) }
        }
        val today = WatchStore.current(this)
        if (!today.isCurrent()) scope.launch { PhoneLink.refresh(applicationContext) }
        val layout = todayTileLayout(this, requestParams.deviceConfiguration, today)
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(30 * 60 * 1000L)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
        return CallbackToFutureAdapter.getFuture { completer -> completer.set(tile); "tile" }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}

private fun openApp(context: Context): Clickable = clickable(
    action = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setPackageName(context.packageName)
                .setClassName(WatchActivity::class.java.name)
                .build(),
        )
        .build(),
    id = "open",
)

internal fun todayTileLayout(context: Context, device: DeviceParameters, today: WatchToday): LayoutElement =
    materialScope(context, device) {
        val current = today.isCurrent() && today.habits.isNotEmpty()
        val pending = today.habits.filterNot { it.done }
        primaryLayout(
            titleSlot = {
                text(
                    if (current) {
                        context.getString(R.string.tile_title_progress, today.doneCount, today.habits.size).layoutString
                    } else {
                        context.getString(R.string.tile_title).layoutString
                    },
                )
            },
            mainSlot = {
                when {
                    !today.isCurrent() -> message(context.getString(R.string.tile_waiting))
                    today.habits.isEmpty() -> message(context.getString(R.string.tile_nothing_today))
                    pending.isEmpty() -> message(context.getString(R.string.tile_all_done))
                    else -> {
                        val column = LayoutElementBuilders.Column.Builder().setWidth(expand()).setHeight(expand())
                        pending.take(TILE_SLOTS).forEachIndexed { index, habit ->
                            if (index > 0) column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())
                            column.addContent(habitButton(habit))
                        }
                        column.build()
                    }
                }
            },
            bottomSlot = {
                if (today.isCurrent()) {
                    textEdgeButton(onClick = openApp(context), colors = filledTonalButtonColors()) {
                        text(context.getString(R.string.tile_open_all).layoutString)
                    }
                } else {
                    textEdgeButton(onClick = clickable(loadAction(), REFRESH_ID)) {
                        text(context.getString(R.string.tile_refresh).layoutString)
                    }
                }
            },
        )
    }

private fun MaterialScope.message(value: String): LayoutElement =
    text(value.layoutString, typography = Typography.BODY_MEDIUM, maxLines = 3)

private fun MaterialScope.habitButton(habit: WatchHabit): LayoutElement = button(
    onClick = clickable(loadAction(), TOGGLE_PREFIX + habit.id),
    labelContent = { text("${habit.emoji} ${habit.name}".trim().layoutString, maxLines = 1) },
    secondaryLabelContent = if (habit.detail.isNotEmpty()) {
        { text(habit.detail.layoutString, maxLines = 1) }
    } else {
        null
    },
    width = expand(),
    colors = if (habit.done) filledButtonColors() else filledTonalButtonColors(),
)
