package app.tsuzuku.wear

import android.content.Context
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.time.LocalDate

/** Same paths as the phone's WearProtocol. */
internal object WearProtocol {
    const val PATH_TODAY = "/tsuzuku/today"
    const val PATH_TOGGLE = "/tsuzuku/toggle"
    const val PATH_REFRESH = "/tsuzuku/refresh"
    const val KEY_JSON = "json"
}

data class WatchHabit(
    val id: Long,
    val name: String,
    val emoji: String,
    val color: Int,
    val done: Boolean,
    val detail: String,
    val fraction: Float,
)

data class WatchToday(val date: LocalDate?, val habits: List<WatchHabit>) {
    val doneCount: Int get() = habits.count { it.done }
    /** False when the phone hasn't sent anything for today yet. */
    fun isCurrent(today: LocalDate = LocalDate.now()): Boolean = date == today

    companion object {
        val Empty = WatchToday(null, emptyList())

        fun parse(json: String?): WatchToday {
            if (json.isNullOrBlank()) return Empty
            return runCatching {
                val root = JSONObject(json)
                val list = root.getJSONArray("habits")
                WatchToday(
                    date = LocalDate.parse(root.getString("date")),
                    habits = (0 until list.length()).map { i ->
                        val o = list.getJSONObject(i)
                        WatchHabit(
                            id = o.getLong("id"),
                            name = o.getString("name"),
                            emoji = o.optString("emoji"),
                            color = o.optInt("color"),
                            done = o.getBoolean("done"),
                            detail = o.optString("detail"),
                            fraction = o.optDouble("fraction", 0.0).toFloat(),
                        )
                    },
                )
            }.getOrDefault(Empty)
        }
    }
}

/** The last list the phone sent, kept on the watch so the tile works offline. */
object WatchStore {
    private const val PREFS = "tsuzuku_watch"
    private const val KEY = "today"
    private val state = MutableStateFlow(WatchToday.Empty)
    private var loaded = false

    fun flow(context: Context): StateFlow<WatchToday> {
        load(context)
        return state.asStateFlow()
    }

    fun current(context: Context): WatchToday {
        load(context)
        return state.value
    }

    private fun load(context: Context) {
        if (loaded) return
        loaded = true
        state.value = WatchToday.parse(context.prefs().getString(KEY, null))
    }

    fun save(context: Context, json: String) {
        context.prefs().edit().putString(KEY, json).apply()
        loaded = true
        state.value = WatchToday.parse(json)
        TileService.getUpdater(context).requestUpdate(TodayTileService::class.java)
    }

    /** Shows the tap right away; the phone's answer replaces it a moment later. */
    fun optimisticToggle(context: Context, id: Long) {
        load(context)
        val today = state.value
        state.value = today.copy(
            // Only yes/no habits can be predicted; counted ones wait for the phone's numbers.
            habits = today.habits.map {
                if (it.id == id && it.detail.isEmpty()) it.copy(done = !it.done, fraction = if (it.done) 0f else 1f) else it
            },
        )
    }

    private fun Context.prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** Talks to the phone app. */
object PhoneLink {
    suspend fun send(context: Context, path: String, payload: ByteArray = ByteArray(0)): Boolean {
        val nodes = runCatching { Wearable.getNodeClient(context).connectedNodes.await() }.getOrDefault(emptyList())
        if (nodes.isEmpty()) return false
        val client = Wearable.getMessageClient(context)
        nodes.forEach { node -> runCatching { client.sendMessage(node.id, path, payload).await() } }
        return true
    }

    suspend fun toggle(context: Context, id: Long): Boolean {
        WatchStore.optimisticToggle(context, id)
        return send(context, WearProtocol.PATH_TOGGLE, id.toString().toByteArray())
    }

    suspend fun refresh(context: Context): Boolean = send(context, WearProtocol.PATH_REFRESH)
}

/** Receives the phone's list, even when the watch app isn't open. */
class DataLayerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WearProtocol.PATH_TODAY) {
                val json = DataMapItem.fromDataItem(event.dataItem).dataMap.getString(WearProtocol.KEY_JSON)
                if (json != null) WatchStore.save(this, json)
            }
        }
    }
}
