package app.tsuzuku.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * The first text field, slider or picture a screen shows costs tens of milliseconds the very
 * first time (class loading, first layout, image decoding). Paid on tap, that shows as the old
 * screen lingering. This pays it once, invisibly, while the app sits idle after opening.
 */
@Composable
fun WarmUpOnce() {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (warmed) return@LaunchedEffect
        warmed = true
        delay(1_500)
        withContext(Dispatchers.IO) { WidgetPreviewCache.preload(context) }
        show = true
        delay(1_200)
        show = false
    }
    if (show) {
        Box(Modifier.size(1.dp).alpha(0f).clearAndSetSemantics { }) {
            Column {
                OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("·") }, singleLine = true)
                Slider(value = 0.5f, onValueChange = {}, steps = 15)
                Switch(checked = true, onCheckedChange = {})
                FilterChip(selected = true, onClick = {}, label = { Text("·") })
            }
        }
    }
}

private var warmed = false

/** Widget previews decoded off the main thread, keyed by drawable and current configuration. */
object WidgetPreviewCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    private fun key(context: Context, id: Int): String {
        val c = context.resources.configuration
        return "$id/${c.uiMode}/${c.locales[0]}"
    }

    fun get(context: Context, id: Int): ImageBitmap? = cache[key(context, id)]

    fun load(context: Context, id: Int): ImageBitmap? = get(context, id) ?: runCatching {
        BitmapFactory.decodeResource(context.resources, id)?.asImageBitmap()
    }.getOrNull()?.also { cache[key(context, id)] = it }

    fun preload(context: Context) {
        listOf(
            app.tsuzuku.R.drawable.widget_preview_today, app.tsuzuku.R.drawable.widget_preview_habit,
            app.tsuzuku.R.drawable.widget_preview_streak, app.tsuzuku.R.drawable.widget_preview_week,
            app.tsuzuku.R.drawable.widget_preview_next, app.tsuzuku.R.drawable.widget_preview_ring,
            app.tsuzuku.R.drawable.widget_preview_heatmap, app.tsuzuku.R.drawable.widget_preview_alarm,
        ).forEach { load(context, it) }
    }
}
