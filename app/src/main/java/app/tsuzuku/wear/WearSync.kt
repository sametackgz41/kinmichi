package app.tsuzuku.wear

import android.content.Context
import android.util.Log
import app.tsuzuku.TsuzukuApplication
import app.tsuzuku.model.HabitDayStatus
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.widget.TsuzukuWidgets
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Phone ⇄ watch protocol. The phone owns the data: it publishes today's habits as one data
 * item, and the watch sends small messages back ("toggle habit 12", "send me a fresh copy").
 * The same constants live in the :wear module.
 */
object WearProtocol {
    const val PATH_TODAY = "/tsuzuku/today"
    const val PATH_TOGGLE = "/tsuzuku/toggle"
    const val PATH_REFRESH = "/tsuzuku/refresh"
    const val KEY_JSON = "json"
}

private fun amount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.ROOT, "%.1f", value)

/** The compact JSON the watch renders. Kept free of Android types so it is easy to test. */
internal fun todayJson(habits: List<TodayHabit>, date: LocalDate): String {
    val list = JSONArray()
    habits.filter { it.status != HabitDayStatus.Skipped }.forEach { item ->
        val habit = item.habit
        val detail = when (habit.type) {
            HabitType.Check -> ""
            else -> "${amount(item.progress)}/${amount(habit.target)} ${habit.unit}".trim()
        }
        list.put(
            JSONObject()
                .put("id", habit.id)
                .put("name", habit.name)
                .put("emoji", habit.emoji)
                .put("color", habit.colorArgb.toInt())
                .put("done", item.isComplete)
                .put("detail", detail)
                .put("fraction", if (habit.type == HabitType.Check || habit.target <= 0) (if (item.isComplete) 1.0 else 0.0) else (item.progress / habit.target).coerceIn(0.0, 1.0)),
        )
    }
    return JSONObject().put("date", date.toString()).put("habits", list).toString()
}

object WearSync {
    private const val TAG = "WearSync"
    private var lastSent: String? = null

    /** Publishes today's list to any paired watch. A no-op when Wear OS isn't available. */
    fun push(context: Context, habits: List<TodayHabit>, force: Boolean = false) {
        val json = todayJson(habits, LocalDate.now())
        if (!force && json == lastSent) return
        lastSent = json
        val request = PutDataMapRequest.create(WearProtocol.PATH_TODAY).apply {
            dataMap.putString(WearProtocol.KEY_JSON, json)
            // A changing stamp makes a forced refresh reach the watch even if nothing changed.
            dataMap.putLong("sentAt", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        runCatching {
            Wearable.getDataClient(context).putDataItem(request)
                .addOnFailureListener { Log.d(TAG, "No watch available: ${it.message}") }
        }
    }
}

/** Handles taps that arrive from the watch tile or app. */
class PhoneWearService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        val app = applicationContext as TsuzukuApplication
        val repository = app.habitRepository
        runBlocking {
            when (event.path) {
                WearProtocol.PATH_TOGGLE -> {
                    val id = String(event.data).toLongOrNull() ?: return@runBlocking
                    val item = repository.observeToday().first().firstOrNull { it.habit.id == id } ?: return@runBlocking
                    when {
                        item.habit.type == HabitType.Check -> repository.toggleHabit(id)
                        item.isComplete -> Unit
                        item.habit.type == HabitType.Count -> repository.incrementHabit(id)
                        else -> repository.addProgress(id, (item.habit.target - item.progress).coerceAtLeast(0.0))
                    }
                    TsuzukuWidgets.updateAll(this@PhoneWearService)
                }
                WearProtocol.PATH_REFRESH -> Unit
                else -> return@runBlocking
            }
            WearSync.push(this@PhoneWearService, repository.observeToday().first(), force = true)
        }
    }
}
