package app.tsuzuku.previews

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.core.app.ApplicationProvider
import app.tsuzuku.model.GoalPeriod
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitDayStatus
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.HabitHistoryRecord
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.ui.HabitTimerState
import app.tsuzuku.ui.MainNavigationBar
import app.tsuzuku.ui.MainTopBar
import app.tsuzuku.ui.mainDestinations
import app.tsuzuku.ui.screens.HabitRow
import app.tsuzuku.ui.screens.StatisticsScreen
import app.tsuzuku.ui.screens.TodayProgressHeader
import app.tsuzuku.ui.screens.TodayScreen
import app.tsuzuku.ui.theme.TsuzukuTheme
import app.tsuzuku.widget.HabitContent
import app.tsuzuku.widget.TodayContent
import app.tsuzuku.widget.TsuzukuGlanceTheme
import app.tsuzuku.widget.WeekContent
import app.tsuzuku.widget.thisWeek
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * Renders the real Compose screens and Glance widgets to PNG files on the JVM, so the UI can be
 * reviewed in a browser without an emulator. Output goes to `previews/images` at the repo root.
 *
 * Run: ./gradlew :app:testDebugUnitTest -Ppreviews --tests "app.tsuzuku.previews.*"
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class PreviewGalleryTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File(System.getProperty("previews.dir") ?: "build/previews").apply { mkdirs() }

    private fun setVariant(lang: String, dark: Boolean) {
        RuntimeEnvironment.setQualifiers("+$lang-r${if (lang == "tr") "TR" else "US"}")
        RuntimeEnvironment.setQualifiers(if (dark) "+night" else "+notnight")
    }

    private fun save(name: String, bitmap: Bitmap) {
        File(outDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Software-draws the activity's content view; works in Robolectric without PixelCopy. */
    private fun snapshot(): Bitmap {
        compose.waitForIdle()
        val root = compose.activity.window.decorView.findViewById<View>(android.R.id.content)
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        return bitmap
    }

    private fun captureScreen(name: String) = save(name, snapshot())

    // ---- sample data -------------------------------------------------------------------------

    private fun habit(id: Long, tr: String, en: String, lang: String, emoji: String, color: Long, type: HabitType,
                      target: Double = 1.0, unitTr: String = "", unitEn: String = "", direction: HabitDirection = HabitDirection.Build) =
        Habit(
            id = id, name = if (lang == "tr") tr else en, emoji = emoji, colorArgb = color, type = type,
            direction = direction, target = target, unit = if (lang == "tr") unitTr else unitEn, step = 1.0,
            scheduleDaysMask = 127, goalPeriod = GoalPeriod.Daily, sortOrder = id.toInt(),
        )

    private fun sampleToday(lang: String, allDone: Boolean = false): List<TodayHabit> {
        val read = habit(1, "Kitap oku", "Read a book", lang, "📖", 0xFF5B7DB1, HabitType.Check)
        val water = habit(2, "Su iç", "Drink water", lang, "💧", 0xFF3E8EC2, HabitType.Count, 8.0, "bardak", "glasses")
        val meditate = habit(3, "Meditasyon", "Meditate", lang, "🧘", 0xFF8C6BB1, HabitType.Duration, 15.0, "dk", "min")
        val run = habit(4, "Koşu", "Run", lang, "🏃", 0xFFC2733E, HabitType.Check)
        val walk = habit(5, "Akşam yürüyüşü", "Evening walk", lang, "🌿", 0xFF4E9A6B, HabitType.Check)
        return listOf(
            TodayHabit(read, progress = 1.0, status = HabitDayStatus.Completed, streakDays = 12),
            TodayHabit(water, progress = if (allDone) 8.0 else 5.0, status = if (allDone) HabitDayStatus.Completed else HabitDayStatus.Pending, streakDays = 4),
            TodayHabit(meditate, progress = if (allDone) 15.0 else 10.0, status = if (allDone) HabitDayStatus.Completed else HabitDayStatus.Pending, streakDays = 7, restUsedThisWeek = true),
            TodayHabit(run, progress = if (allDone) 1.0 else 0.0, status = if (allDone) HabitDayStatus.Completed else HabitDayStatus.Pending, streakDays = 3),
            TodayHabit(walk, progress = if (allDone) 1.0 else 0.0, status = if (allDone) HabitDayStatus.Completed else HabitDayStatus.Pending, streakDays = 0),
        )
    }

    private fun sampleHistory(lang: String): HabitHistorySnapshot {
        val habits = sampleToday(lang).map { it.habit.copy(createdOn = LocalDate.now().minusDays(60)) }
        val today = LocalDate.now()
        val records = (0L..45L).flatMap { back ->
            val date = today.minusDays(back)
            habits.mapNotNull { h ->
                val done = ((back * 7 + h.id * 3) % 10) < 7 || (h.id == 1L && back < 12)
                if (!done) null else HabitHistoryRecord(h.id, date, h.target, HabitDayStatus.Completed)
            }
        }
        return HabitHistorySnapshot(habits, records)
    }

    // ---- app screens -------------------------------------------------------------------------

    @Composable
    private fun Shell(title: String, route: String, profile: String, content: @Composable (Modifier) -> Unit) {
        Scaffold(
            topBar = { MainTopBar(title = title, onSearch = if (route == "today") ({}) else null) },
            bottomBar = { MainNavigationBar(destinations = mainDestinations, currentRoute = route, onSelect = {}) },
        ) { padding -> content(Modifier.padding(padding)) }
    }

    @Test fun todayTrLight() = renderToday("tr", false)
    @Test fun todayTrDark() = renderToday("tr", true)
    @Test fun todayEnLight() = renderToday("en", false)
    @Test fun todayEnDark() = renderToday("en", true)

    private fun sampleWeek(): List<app.tsuzuku.ui.screens.WeekDayProgress> {
        val today = java.time.LocalDate.now()
        val start = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val fractions = listOf(1f, 0.75f, 0.5f, 1f, 0.25f, 0.66f, 0.8f)
        return (0 until 7).map { i ->
            val date = start.plusDays(i.toLong())
            app.tsuzuku.ui.screens.WeekDayProgress(date, if (date.isAfter(today)) null else fractions[i])
        }
    }

    private fun renderToday(lang: String, dark: Boolean) {
        setVariant(lang, dark)
        val state = mutableStateOf(sampleToday(lang))
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                Shell(title = if (lang == "tr") "Bugün" else "Today", route = "today", profile = "Samet") { modifier ->
                    Box(modifier) {
                        TodayScreen(
                            habits = state.value, profileName = "Samet", showDailySummary = true,
                            timer = HabitTimerState(), onAddHabit = {}, onToggleHabit = {}, onIncrementHabit = {},
                            onDecrementHabit = {}, onToggleTimer = {}, onOpenHabit = {}, onReorderHabits = {},
                            onOpenGoals = {}, onSearch = {},
                            todayHiddenSections = app.tsuzuku.data.preferences.DEFAULT_TODAY_HIDDEN_SECTIONS,
                            weekProgress = sampleWeek(),
                        )
                    }
                }
            }
        }
        val suffix = "${lang}_${if (dark) "dark" else "light"}"
        captureScreen("today_$suffix")
        state.value = sampleToday(lang, allDone = true)
        compose.mainClock.advanceTimeBy(2_000)
        captureScreen("today_done_$suffix")
        state.value = emptyList()
        compose.mainClock.advanceTimeBy(1_000)
        captureScreen("today_empty_$suffix")
    }

    @Test fun progressTrLight() = renderProgress("tr", false)
    @Test fun progressTrDark() = renderProgress("tr", true)
    @Test fun progressEnLight() = renderProgress("en", false)
    @Test fun progressEnDark() = renderProgress("en", true)

    private fun renderProgress(lang: String, dark: Boolean) {
        setVariant(lang, dark)
        val history = sampleHistory(lang)
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                Shell(title = if (lang == "tr") "İlerleme" else "Progress", route = "progress", profile = "Samet") { modifier ->
                    Box(modifier) { StatisticsScreen(history = history, libraryItems = emptyList()) }
                }
            }
        }
        captureScreen("progress_${lang}_${if (dark) "dark" else "light"}")
    }

    @Test fun editorTrLight() = renderEditorAndSettings("tr", false)
    @Test fun editorTrDark() = renderEditorAndSettings("tr", true)
    @Test fun editorEnLight() = renderEditorAndSettings("en", false)
    @Test fun editorEnDark() = renderEditorAndSettings("en", true)

    private fun renderEditorAndSettings(lang: String, dark: Boolean) {
        setVariant(lang, dark)
        val suffix = "${lang}_${if (dark) "dark" else "light"}"
        val screen = mutableStateOf("editor")
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                if (screen.value == "editor") {
                    app.tsuzuku.ui.screens.HabitEditorScreen(onBack = {}, onSave = {})
                } else {
                    Shell(title = if (lang == "tr") "Ayarlar" else "Settings", route = "settings", profile = "Samet") { modifier ->
                        Box(modifier) {
                            app.tsuzuku.ui.screens.ProfileScreen(
                                settings = app.tsuzuku.data.preferences.AppSettings(profileName = "Samet"),
                                cloudAccount = app.tsuzuku.data.CloudAccountState.SignedOut,
                                onSetProfileName = {}, onSetTheme = {}, onSetLanguage = {}, onSetWeekStart = {},
                                onSetShowDailySummary = {}, onSetProtectScreen = {}, onOpenCloudAccount = {},
                                onOpenPlanning = {}, onOpenSecurity = {}, onOpenBackup = {}, onOpenDiagnostics = {},
                                onOpenCategories = {},
                            )
                        }
                    }
                }
            }
        }
        captureScreen("editor_$suffix")
        screen.value = "settings"
        compose.mainClock.advanceTimeBy(500)
        captureScreen("settings_$suffix")
    }

    // ---- coaching: target advice, mood check-in, mood insights ------------------------------

    @Test fun coachingTrLight() = renderCoaching("tr", false)
    @Test fun coachingTrDark() = renderCoaching("tr", true)
    @Test fun coachingEnLight() = renderCoaching("en", false)
    @Test fun coachingEnDark() = renderCoaching("en", true)

    private fun renderCoaching(lang: String, dark: Boolean) {
        setVariant(lang, dark)
        val habits = sampleToday(lang).map { it.habit }
        val advice = app.tsuzuku.model.TargetAdvice(habits[1], app.tsuzuku.model.TargetAdvice.Kind.Lower, 5.0, hits = 3, days = 14)
        val review = app.tsuzuku.model.DailyReview(LocalDate.now().toEpochDay(), 4, 3, 3, 3, "", 0L)
        val insights = app.tsuzuku.model.MoodInsights(
            loggedDays = 34,
            insights = listOf(
                app.tsuzuku.model.MoodInsight(habits[4], 4.1, 3.0, 15, 12),
                app.tsuzuku.model.MoodInsight(habits[2], 3.9, 3.2, 18, 9),
                app.tsuzuku.model.MoodInsight(habits[3], 2.9, 3.6, 8, 20),
            ),
        )
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    androidx.compose.foundation.layout.Column(
                        Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
                    ) {
                        app.tsuzuku.ui.screens.TargetAdviceCard(advice, onAccept = {}, onDismiss = {})
                        app.tsuzuku.ui.screens.MoodCheckInCard(review, onPick = {}, onOpenDetails = {})
                        app.tsuzuku.ui.screens.MoodInsightsCard(insights)
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(800)
        captureScreen("coaching_${lang}_${if (dark) "dark" else "light"}")
    }

    // ---- habit detail: year heatmap --------------------------------------------------------

    @Test fun detailTrLight() = renderDetail("tr", false)
    @Test fun detailTrDark() = renderDetail("tr", true)
    @Test fun detailEnLight() = renderDetail("en", false)
    @Test fun detailEnDark() = renderDetail("en", true)

    private fun renderDetail(lang: String, dark: Boolean) {
        setVariant(lang, dark)
        val today = LocalDate.now()
        val water = sampleToday(lang)[1].habit.copy(createdOn = today.minusDays(400))
        // A believable year: a slow start, a strong summer, a few skips and partial days.
        val records = (0L..364L).mapNotNull { back ->
            val date = today.minusDays(back)
            val seed = ((back * 37 + 11) % 100).toInt()
            val chance = if (back > 240) 45 else 82
            when {
                back == 0L -> HabitHistoryRecord(water.id, date, 5.0, HabitDayStatus.Pending)
                seed % 29 == 0 -> HabitHistoryRecord(water.id, date, 0.0, HabitDayStatus.Skipped)
                seed < chance -> HabitHistoryRecord(water.id, date, 8.0, HabitDayStatus.Completed)
                seed < chance + 10 -> HabitHistoryRecord(water.id, date, (2 + seed % 5).toDouble(), HabitDayStatus.Pending)
                else -> null
            }
        }
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                app.tsuzuku.ui.screens.HabitDetailScreen(
                    habit = water, records = records,
                    onBack = {}, onEdit = {}, onSaveNote = {}, onArchive = {}, onDuplicate = {}, onDelete = {},
                    onShare = {},
                )
            }
        }
        compose.mainClock.advanceTimeBy(1500)
        captureScreen("detail_${lang}_${if (dark) "dark" else "light"}")
    }

    // ---- shareable streak card -------------------------------------------------------------

    @Test fun shareCardTr() = renderShareCard("tr")
    @Test fun shareCardEn() = renderShareCard("en")

    private fun renderShareCard(lang: String) {
        setVariant(lang, false)
        val today = LocalDate.now()
        val read = sampleToday(lang)[0].habit.copy(createdOn = today.minusDays(200))
        val walk = sampleToday(lang)[4].habit.copy(createdOn = today.minusDays(200))
        val records = (0L..150L).flatMap { back ->
            val date = today.minusDays(back)
            listOfNotNull(
                if (back < 34 || (back * 13) % 10 < 7) HabitHistoryRecord(read.id, date, 1.0, HabitDayStatus.Completed) else null,
                if (back in 1L..120L && (back * 7) % 10 < 6) HabitHistoryRecord(walk.id, date, 1.0, HabitDayStatus.Completed) else null,
            )
        }
        val history = HabitHistorySnapshot(listOf(read, walk), records)
        val context = RuntimeEnvironment.getApplication()
        save(
            "share_${lang}",
            app.tsuzuku.share.renderStreakCard(context, app.tsuzuku.model.streakCardData(history, read, today, java.time.DayOfWeek.MONDAY)),
        )
        save(
            "share_${lang}_nostreak",
            app.tsuzuku.share.renderStreakCard(context, app.tsuzuku.model.streakCardData(history, walk, today, java.time.DayOfWeek.MONDAY)),
        )
    }

    // ---- animations: frame strips, stitched into GIFs by the gallery script ------------------

    @Test fun animationsTrLight() = renderAnimations("tr", false)
    @Test fun animationsTrDark() = renderAnimations("tr", true)

    private fun renderAnimations(lang: String, dark: Boolean) {
        setVariant(lang, dark)
        val suffix = "${lang}_${if (dark) "dark" else "light"}"
        val base = sampleToday(lang)
        val run = mutableStateOf(base[3])
        val water = mutableStateOf(base[1])
        val summary = mutableStateOf(3 to 5)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                Column(
                    Modifier.width(411.dp).background(MaterialTheme.colorScheme.background).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TodayProgressHeader(completed = summary.value.first, total = summary.value.second)
                    HabitRow(
                        todayHabit = run.value, timerRunning = false, timerElapsedSeconds = 0, interactionDisabled = false,
                        onToggle = {}, onIncrement = {}, onDecrement = {}, onToggleTimer = {}, onOpen = {},
                        onOpenSkipDialog = {}, onUndoSkip = {},
                    )
                    HabitRow(
                        todayHabit = water.value, timerRunning = false, timerElapsedSeconds = 0, interactionDisabled = false,
                        onToggle = {}, onIncrement = {}, onDecrement = {}, onToggleTimer = {}, onOpen = {},
                        onOpenSkipDialog = {}, onUndoSkip = {},
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        val frames = mutableListOf<Bitmap>()
        fun grab(count: Int, stepMs: Long) = repeat(count) {
            compose.mainClock.advanceTimeBy(stepMs)
            frames += snapshot()
        }
        grab(4, 120)
        // Complete "run", add a glass of water, and finish the day: check pop, ring fill, counter roll, celebration.
        run.value = run.value.copy(progress = 1.0, status = HabitDayStatus.Completed, streakDays = 4)
        water.value = water.value.copy(progress = 8.0, status = HabitDayStatus.Completed)
        summary.value = 5 to 5
        grab(28, 40)
        grab(6, 150)
        frames.forEachIndexed { index, bitmap -> save("anim_${suffix}_%02d".format(index), bitmap) }
    }

    // ---- widgets -----------------------------------------------------------------------------

    private class PreviewWidget(private val body: @Composable () -> Unit) : GlanceAppWidget() {
        override suspend fun provideGlance(context: android.content.Context, id: androidx.glance.GlanceId) {
            provideContent { body() }
        }
    }

    @OptIn(ExperimentalGlanceApi::class)
    private fun renderWidget(name: String, widthDp: Int, heightDp: Int, body: @Composable () -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val views = runBlocking {
            PreviewWidget(body).compose(context, size = DpSize(widthDp.dp, heightDp.dp))
        }
        val density = context.resources.displayMetrics.density
        val w = (widthDp * density).toInt()
        val h = (heightDp * density).toInt()
        val host = FrameLayout(context)
        val view = views.apply(context, host)
        host.addView(view, FrameLayout.LayoutParams(w, h))
        host.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        host.layout(0, 0, w, h)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        host.draw(Canvas(bitmap))
        save(name, bitmap)
    }

    @Test fun widgetsTrLight() = renderWidgets("tr", false)
    @Test fun widgetsTrDark() = renderWidgets("tr", true)
    @Test fun widgetsEnLight() = renderWidgets("en", false)
    @Test fun widgetsEnDark() = renderWidgets("en", true)

    private fun renderWidgets(lang: String, dark: Boolean) {
        setVariant(lang, dark)
        val suffix = "${lang}_${if (dark) "dark" else "light"}"
        val today = sampleToday(lang)
        val todaySorted = today.sortedBy { it.isComplete } // same order as TodayWidget
        renderWidget("widget_today_$suffix", 300, 260) { TsuzukuGlanceTheme { TodayContent(todaySorted, loaded = true, locked = false, scrollable = false) } }
        renderWidget("widget_today_compact_$suffix", 250, 110) { TsuzukuGlanceTheme { TodayContent(todaySorted, loaded = true, locked = false, scrollable = false) } }
        val allDone = sampleToday(lang, allDone = true).take(4)
        renderWidget("widget_today_done_$suffix", 300, 260) { TsuzukuGlanceTheme { TodayContent(allDone, loaded = true, locked = false, scrollable = false) } }
        renderWidget("widget_habit_$suffix", 150, 150) { TsuzukuGlanceTheme { HabitContent(today[0], locked = false) } }
        renderWidget("widget_habit_progress_$suffix", 150, 150) { TsuzukuGlanceTheme { HabitContent(today[1], locked = false) } }
        renderWidget("widget_habit_wide_$suffix", 300, 120) { TsuzukuGlanceTheme { HabitContent(today[1], locked = false) } }
        val days = thisWeek(sampleHistory(lang), firstDay = if (lang == "tr") java.time.DayOfWeek.MONDAY else java.time.DayOfWeek.SUNDAY)
        renderWidget("widget_week_$suffix", 320, 150) { TsuzukuGlanceTheme { WeekContent(days, bestStreak = 12, locked = false) } }
        renderWidget("widget_today_locked_$suffix", 300, 140) { TsuzukuGlanceTheme { TodayContent(emptyList(), loaded = true, locked = true, scrollable = false) } }
    }
}
