package app.tsuzuku.wear

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.renderer.TileRenderer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Renders the watch app and tile to previews/images (see wear/build.gradle.kts). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "tr-w227dp-h227dp-round-watch-night-xhdpi")
class WatchPreviewTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File(System.getProperty("previews.dir") ?: "build/previews").apply { mkdirs() }

    private val today = WatchToday(
        LocalDate.now(),
        listOf(
            WatchHabit(1, "Kitap oku", "📖", 0, true, "", 1f),
            WatchHabit(2, "Su iç", "💧", 0, false, "3/8 bardak", 0.375f),
            WatchHabit(3, "Meditasyon", "🧘", 0, false, "10 dk", 0f),
            WatchHabit(4, "Yürüyüş", "🚶", 0, false, "4.200 / 8.000 adım", 0.5f),
            WatchHabit(5, "Erken yat", "🌙", 0, false, "", 0f),
        ),
    )

    private fun save(name: String, bitmap: Bitmap) {
        File(outDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun watchApp() {
        // The scaffold's clock ticks forever, so step time by hand instead of waiting for idle.
        compose.mainClock.autoAdvance = false
        compose.setContent { MaterialTheme { WatchTodayContent(today) {} } }
        compose.mainClock.advanceTimeBy(2_000)
        shadowOf(compose.activity.mainLooper).idle()
        val root = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        save("watch_app_tr", bitmap)
    }

    @Test
    fun watchTile() = renderTile("watch_tile_tr", today)

    @Test
    fun watchTileWaiting() = renderTile("watch_tile_waiting_tr", WatchToday.Empty)

    private fun renderTile(name: String, data: WatchToday) {
        val context = compose.activity
        val metrics = context.resources.displayMetrics
        val device = DeviceParametersBuilders.DeviceParameters.Builder()
            .setScreenWidthDp(227)
            .setScreenHeightDp(227)
            .setScreenDensity(metrics.density)
            .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_ROUND)
            .setDevicePlatform(DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
            .build()
        val layout = LayoutElementBuilders.Layout.fromLayoutElement(todayTileLayout(context, device, data))
        val resources = ResourceBuilders.Resources.Builder().setVersion("1").build()
        val size = (227 * metrics.density).toInt()
        val parent = FrameLayout(context)
        context.setContentView(parent, FrameLayout.LayoutParams(size, size))
        val renderer = TileRenderer(context, { it.run() }, { })
        val future = renderer.inflateAsync(layout, resources, parent)
        repeat(20) { shadowOf(context.mainLooper).idle(); if (!future.isDone) Thread.sleep(50) }
        future.get(5, TimeUnit.SECONDS)
        parent.measure(View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY))
        parent.layout(0, 0, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.BLACK)
        parent.draw(canvas)
        save(name, bitmap)
    }
}
