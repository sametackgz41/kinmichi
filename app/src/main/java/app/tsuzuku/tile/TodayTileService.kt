package app.tsuzuku.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.tsuzuku.HomeActivity
import app.tsuzuku.R
import app.tsuzuku.TsuzukuApplication
import app.tsuzuku.model.TodayHabit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings tile: shows how many of today's habits are left and opens the app on tap.
 * It is "on" once the day is done. With the app lock enabled it shows no counts.
 */
class TodayTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        val app = application as TsuzukuApplication
        scope.launch {
            val today = runCatching { app.habitRepository.observeToday().first() }.getOrNull() ?: return@launch
            val tile = qsTile ?: return@launch
            render(tile, today, locked = app.settingsRepository.settings.value.appLockEnabled)
        }
    }

    private fun render(tile: Tile, today: List<TodayHabit>, locked: Boolean) {
        val left = today.count { !it.isComplete }
        tile.label = getString(R.string.tile_today_label)
        tile.state = if (today.isNotEmpty() && left == 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        val subtitle = when {
            locked -> null
            today.isEmpty() -> getString(R.string.tile_no_habits)
            left == 0 -> getString(R.string.tile_all_done)
            else -> resources.getQuantityString(R.plurals.tile_habits_left, left, left)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = subtitle
        tile.contentDescription = listOfNotNull(tile.label, subtitle).joinToString(", ")
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Asks the system to refresh the tile, e.g. after a habit was checked off. */
        fun requestRefresh(context: Context) {
            runCatching {
                requestListeningState(context, ComponentName(context, TodayTileService::class.java))
            }
        }
    }
}
