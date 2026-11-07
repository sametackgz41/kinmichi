package app.tsuzuku.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.compose
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetComposeTest {
    /** Runs every widget's real provideGlance and turns it into RemoteViews, as a launcher would get. */
    @Test
    fun everyWidgetRenders() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        listOf(TodayWidget(), HabitWidget(), WeekWidget(), StreakWidget(), QuickGridWidget(), MoodWidget()).forEach { widget ->
            widget.compose(context, size = DpSize(250.dp, 180.dp))
        }
    }

    /**
     * Glance draws widgets inside a WorkManager worker. If R8 strips WorkManager's input merger,
     * no worker starts and every widget stays a blank box in release builds (fixed in 2.8.0).
     */
    @Test
    fun releaseBuildKeepsWorkManagerClasses() {
        val rules = File("proguard-rules.pro").takeIf { it.exists() } ?: File("app/proguard-rules.pro")
        val text = rules.readText()
        assertTrue(text.contains("androidx.work.InputMerger"))
        assertTrue(text.contains("androidx.work.ListenableWorker"))
    }
}
