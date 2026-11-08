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
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        val region = when (lang) { "tr" -> "TR"; "es" -> "ES"; "de" -> "DE"; "pt" -> "BR"; "ja" -> "JP"; "fr" -> "FR"; "it" -> "IT"; "ru" -> "RU"; "ko" -> "KR"; else -> "US" }
        RuntimeEnvironment.setQualifiers("+$lang-r$region")
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

    private val sampleNames = mapOf(
        "Read a book" to listOf("Leer un libro", "Ein Buch lesen", "Ler um livro", "読書", "Lire un livre", "Leggere un libro", "Читать книгу", "독서"),
        "Drink water" to listOf("Beber agua", "Wasser trinken", "Beber água", "水を飲む", "Boire de l'eau", "Bere acqua", "Пить воду", "물 마시기"),
        "Meditate" to listOf("Meditar", "Meditieren", "Meditar", "瞑想", "Méditer", "Meditare", "Медитация", "명상"),
        "Run" to listOf("Correr", "Laufen", "Correr", "ランニング", "Courir", "Correre", "Бег", "달리기"),
        "Evening walk" to listOf("Paseo nocturno", "Abendspaziergang", "Caminhada à noite", "夜の散歩", "Promenade du soir", "Passeggiata serale", "Вечерняя прогулка", "저녁 산책"),
        "glasses" to listOf("vasos", "Gläser", "copos", "杯", "verres", "bicchieri", "стаканов", "잔"),
        "min" to listOf("min", "Min.", "min", "分", "min", "min", "мин", "분"),
    )

    private fun loc(lang: String, tr: String, en: String): String = when (lang) {
        "tr" -> tr
        "es" -> sampleNames[en]?.get(0) ?: en
        "de" -> sampleNames[en]?.get(1) ?: en
        "pt" -> sampleNames[en]?.get(2) ?: en
        "ja" -> sampleNames[en]?.get(3) ?: en
        "fr" -> sampleNames[en]?.get(4) ?: en
        "it" -> sampleNames[en]?.get(5) ?: en
        "ru" -> sampleNames[en]?.get(6) ?: en
        "ko" -> sampleNames[en]?.get(7) ?: en
        else -> en
    }

    private fun habit(id: Long, tr: String, en: String, lang: String, emoji: String, color: Long, type: HabitType,
                      target: Double = 1.0, unitTr: String = "", unitEn: String = "", direction: HabitDirection = HabitDirection.Build) =
        Habit(
            id = id, name = loc(lang, tr, en), emoji = emoji, colorArgb = color, type = type,
            direction = direction, target = target, unit = loc(lang, unitTr, unitEn), step = 1.0,
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
    @Test fun welcomeFrLight() = renderWelcome("fr", false)
    @Test fun welcomeRuLight() = renderWelcome("ru", false)
    @Test fun todayTrDark() = renderToday("tr", true)
    @Test fun todayEnLight() = renderToday("en", false)
    @Test fun todayEnDark() = renderToday("en", true)
    @Test fun siteEsLightToday() = renderToday("es", false)
    @Test fun siteEsDarkToday() = renderToday("es", true)
    @Test fun siteEsDetail() = renderDetail("es", false)
    @Test fun siteEsProgress() = renderProgress("es", false)
    @Test fun siteEsEditor() = renderEditorAndSettings("es", false)
    @Test fun palettesEs() = renderPalettes("es")
    @Test fun siteDeLightToday() = renderToday("de", false)
    @Test fun siteDeDarkToday() = renderToday("de", true)
    @Test fun siteDeDetail() = renderDetail("de", false)
    @Test fun siteDeProgress() = renderProgress("de", false)
    @Test fun siteDeEditor() = renderEditorAndSettings("de", false)
    @Test fun palettesDe() = renderPalettes("de")
    @Test fun sitePtLightToday() = renderToday("pt", false)
    @Test fun sitePtDarkToday() = renderToday("pt", true)
    @Test fun sitePtDetail() = renderDetail("pt", false)
    @Test fun sitePtProgress() = renderProgress("pt", false)
    @Test fun sitePtEditor() = renderEditorAndSettings("pt", false)
    @Test fun palettesPt() = renderPalettes("pt")
    @Test fun siteJaLightToday() = renderToday("ja", false)
    @Test fun siteJaDarkToday() = renderToday("ja", true)
    @Test fun siteJaDetail() = renderDetail("ja", false)
    @Test fun siteJaProgress() = renderProgress("ja", false)
    @Test fun siteJaEditor() = renderEditorAndSettings("ja", false)
    @Test fun palettesJa() = renderPalettes("ja")
    @Test fun siteFrLightToday() = renderToday("fr", false)
    @Test fun siteFrDarkToday() = renderToday("fr", true)
    @Test fun siteFrDetail() = renderDetail("fr", false)
    @Test fun siteFrProgress() = renderProgress("fr", false)
    @Test fun siteFrEditor() = renderEditorAndSettings("fr", false)
    @Test fun palettesFr() = renderPalettes("fr")
    @Test fun siteItLightToday() = renderToday("it", false)
    @Test fun siteItDarkToday() = renderToday("it", true)
    @Test fun siteItDetail() = renderDetail("it", false)
    @Test fun siteItProgress() = renderProgress("it", false)
    @Test fun siteItEditor() = renderEditorAndSettings("it", false)
    @Test fun palettesIt() = renderPalettes("it")
    @Test fun siteRuLightToday() = renderToday("ru", false)
    @Test fun siteRuDarkToday() = renderToday("ru", true)
    @Test fun siteRuDetail() = renderDetail("ru", false)
    @Test fun siteRuProgress() = renderProgress("ru", false)
    @Test fun siteRuEditor() = renderEditorAndSettings("ru", false)
    @Test fun palettesRu() = renderPalettes("ru")
    @Test fun siteKoLightToday() = renderToday("ko", false)
    @Test fun siteKoDarkToday() = renderToday("ko", true)
    @Test fun siteKoDetail() = renderDetail("ko", false)
    @Test fun siteKoProgress() = renderProgress("ko", false)
    @Test fun siteKoEditor() = renderEditorAndSettings("ko", false)
    @Test fun palettesKo() = renderPalettes("ko")
    @Test fun siteWidgetPageTr() = renderSiteWidgetPage("tr")
    @Test fun siteWidgetPageEn() = renderSiteWidgetPage("en")
    @Test fun siteWidgetPageEs() = renderSiteWidgetPage("es")
    @Test fun siteWidgetPageDe() = renderSiteWidgetPage("de")
    @Test fun siteWidgetPagePt() = renderSiteWidgetPage("pt")
    @Test fun siteWidgetPageJa() = renderSiteWidgetPage("ja")
    @Test fun siteWidgetPageFr() = renderSiteWidgetPage("fr")
    @Test fun siteWidgetPageIt() = renderSiteWidgetPage("it")
    @Test fun siteWidgetPageRu() = renderSiteWidgetPage("ru")
    @Test fun siteWidgetPageKo() = renderSiteWidgetPage("ko")

    private fun renderSiteWidgetPage(lang: String) {
        setVariant(lang, false)
        compose.setContent {
            TsuzukuTheme(darkTheme = false) {
                app.tsuzuku.ui.screens.WidgetsScreen(onBack = {}, layout = app.tsuzuku.ui.screens.WidgetsLayout.Carousel, canPinOverride = true)
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        captureScreen("site_widgets_$lang")
    }

    @Test fun palettesTr() = renderPalettes("tr")
    @Test fun palettesEn() = renderPalettes("en")

    private fun renderPalettes(lang: String) {
        setVariant(lang, false)
        val palette = mutableStateOf(app.tsuzuku.data.preferences.PalettePreference.Washi)
        val dark = mutableStateOf(false)
        val habits = sampleToday(lang)
        compose.setContent {
            TsuzukuTheme(darkTheme = dark.value, palette = palette.value) {
                Shell(title = androidx.compose.ui.res.stringResource(app.tsuzuku.R.string.today), route = "today", profile = "Samet") { modifier ->
                    Box(modifier) {
                        TodayScreen(
                            habits = habits, profileName = "Samet", showDailySummary = true,
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
        listOf(
            app.tsuzuku.data.preferences.PalettePreference.Washi,
            app.tsuzuku.data.preferences.PalettePreference.Sumi,
            app.tsuzuku.data.preferences.PalettePreference.Matcha,
            app.tsuzuku.data.preferences.PalettePreference.Sakura,
            app.tsuzuku.data.preferences.PalettePreference.Umi,
        ).forEach { choice ->
            listOf(false, true).forEach { night ->
                palette.value = choice
                dark.value = night
                compose.mainClock.advanceTimeBy(1_000)
                captureScreen("palette_${choice.name.lowercase()}_${if (night) "dark" else "light"}" + if (lang == "tr") "" else "_$lang")
            }
        }
    }

    @Test fun whatsNewTr() = renderWhatsNew("tr")
    @Test fun whatsNewEn() = renderWhatsNew("en")

    private fun renderWhatsNew(lang: String) {
        setVariant(lang, false)
        compose.setContent {
            TsuzukuTheme(darkTheme = false) {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh) {
                    app.tsuzuku.ui.screens.WhatsNewList(Modifier.padding(24.dp))
                }
            }
        }
        captureScreen("whats_new_${lang}_light")
    }

    // First run, as a new user sees it: the five-page tour, then the welcome screen.
    @Test fun tourTr() = renderTour("tr")
    @Test fun tourEn() = renderTour("en")
    @Test fun tourEs() = renderTour("es")
    @Test fun tourDe() = renderTour("de")
    @Test fun tourPt() = renderTour("pt")
    @Test fun tourJa() = renderTour("ja")
    @Test fun tourEnDark() = renderTour("en", dark = true)
    @Test fun welcomeTrLight() = renderWelcome("tr", false)
    @Test fun welcomeTrDark() = renderWelcome("tr", true)
    @Test fun welcomeEnLight() = renderWelcome("en", false)
    @Test fun welcomeEnDark() = renderWelcome("en", true)
    @Test fun welcomeEsLight() = renderWelcome("es", false)
    @Test fun welcomeDeLight() = renderWelcome("de", false)
    @Test fun welcomePtLight() = renderWelcome("pt", false)
    @Test fun welcomeJaLight() = renderWelcome("ja", false)
    @Test fun welcomeSignedInEn() = renderWelcome("en", false, signedIn = true)
    @Test fun welcomeGuestNameTr() = renderWelcome("tr", false, nameStep = true)
    @Test fun welcomeGuestNameTrDark() = renderWelcome("tr", true, nameStep = true)
    @Test fun welcomeGuestNameFilledTr() = renderWelcome("tr", false, nameStep = true, name = "Samet")
    @Test fun welcomeGuestNameFilledTrDark() = renderWelcome("tr", true, nameStep = true, name = "Samet")
    @Test fun welcomeSignedInTr() = renderWelcome("tr", false, signedIn = true)
    @Test fun welcomeAlreadySignedInTr() = renderWelcome("tr", false, signedIn = true, chooseStep = true)

    // Reminder time: hour and minute boxes on the page.
    @Test fun timePickerTr() {
        setVariant("tr", false)
        compose.setContent {
            TsuzukuTheme(darkTheme = false) {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize().padding(20.dp)) {
                        app.tsuzuku.ui.screens.ReminderTimePicker(minutes = 7 * 60 + 30, onChange = {})
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(800)
        captureScreen("timepicker_tr_light")
    }

    // Settings: three looks.
    @Test fun settingsMinimalTr() = renderSettingsLayout(app.tsuzuku.ui.screens.SettingsLayout.Minimal, false)
    @Test fun settingsCardsTr() = renderSettingsLayout(app.tsuzuku.ui.screens.SettingsLayout.Cards, false)
    @Test fun settingsTilesTr() = renderSettingsLayout(app.tsuzuku.ui.screens.SettingsLayout.Tiles, false)
    @Test fun settingsCardsTrDark() = renderSettingsLayout(app.tsuzuku.ui.screens.SettingsLayout.Cards, true)
    @Test fun settingsTilesTrDark() = renderSettingsLayout(app.tsuzuku.ui.screens.SettingsLayout.Tiles, true)

    @Test fun settingsHomeGridTr() = renderSettingsLayout(app.tsuzuku.ui.screens.SettingsLayout.Tiles, false, app.tsuzuku.ui.screens.SettingsHome.Grid)
    @Test fun settingsHomeBentoTr() = renderSettingsLayout(app.tsuzuku.ui.screens.SettingsLayout.Tiles, false, app.tsuzuku.ui.screens.SettingsHome.Bento)
    @Test fun settingsHomeGroupsTr() = renderSettingsLayout(app.tsuzuku.ui.screens.SettingsLayout.Tiles, false, app.tsuzuku.ui.screens.SettingsHome.Groups)
    @Test fun settingsHomeBentoTrDark() = renderSettingsLayout(app.tsuzuku.ui.screens.SettingsLayout.Tiles, true, app.tsuzuku.ui.screens.SettingsHome.Bento)
    @Test fun settingsPageNotificationsTr() = renderSettingsLayout(app.tsuzuku.ui.screens.SettingsLayout.Tiles, false, app.tsuzuku.ui.screens.SettingsHome.Grid, openTile = "Bildirimler")

    private fun renderSettingsLayout(
        layout: app.tsuzuku.ui.screens.SettingsLayout,
        dark: Boolean,
        home: app.tsuzuku.ui.screens.SettingsHome = app.tsuzuku.ui.screens.SettingsHome.Grid,
        openTile: String? = null,
    ) {
        setVariant("tr", dark)
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                Shell(title = androidx.compose.ui.res.stringResource(app.tsuzuku.R.string.settings_label), route = "settings", profile = "Samet") { modifier ->
                    Box(modifier) {
                        app.tsuzuku.ui.screens.ProfileScreen(
                            settings = app.tsuzuku.data.preferences.AppSettings(profileName = "Samet"),
                            cloudAccount = app.tsuzuku.data.CloudAccountState.SignedOut,
                            onSetProfileName = {}, onSetTheme = {}, onSetLanguage = {}, onSetWeekStart = {},
                            onSetShowDailySummary = {}, onSetProtectScreen = {}, onOpenCloudAccount = {},
                            onOpenPlanning = {}, onOpenSecurity = {}, onOpenBackup = {}, onOpenDiagnostics = {},
                            onOpenCategories = {}, layout = layout, home = home,
                        )
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(800)
        if (openTile != null) {
            compose.onNodeWithText(openTile).performClick()
            compose.mainClock.advanceTimeBy(800)
            captureScreen("settingspage_${openTile.lowercase()}_tr")
            return
        }
        val name = if (layout == app.tsuzuku.ui.screens.SettingsLayout.Tiles) "settingshome_${home.name.lowercase()}" else "settingslayout_${layout.name.lowercase()}"
        captureScreen("${name}_tr_${if (dark) "dark" else "light"}")
    }

    // Alarm: ringing page, the three tasks, and the good-morning page (dark, like at night).
    @Test fun alarmRingingTr() = renderAlarm("alarm_ringing_tr") {
        app.tsuzuku.notifications.AlarmRingingPage(
            time = "07:00", date = "9 Kasım Pazartesi", emoji = "⏰", name = "Uyan", plan = "",
            mission = app.tsuzuku.notifications.AlarmMission.Math, snoozesLeft = 2, onStart = {}, onSnooze = {}, onNotToday = {},
        )
    }
    @Test fun alarmMathTr() = renderAlarm("alarm_math_tr") { app.tsuzuku.notifications.MathMission(onProgress = {}, onSolved = {}, seed = 7) }
    @Test fun alarmMemoryTr() = renderAlarm("alarm_memory_tr") { app.tsuzuku.notifications.MemoryMission(onProgress = {}, onSolved = {}, seed = 3, preview = listOf(2)) }
    @Test fun alarmPuzzleTr() = renderAlarm("alarm_puzzle_tr") { app.tsuzuku.notifications.PuzzleMission(onProgress = {}, onSolved = {}, seed = 5, startAt = 6) }
    @Test fun alarmVerify1Tr() = renderVerify(0)
    @Test fun alarmVerify2Tr() = renderVerify(1)
    @Test fun alarmVerify3Tr() = renderVerify(2)
    private fun renderVerify(at: Int) = renderAlarm("alarm_verify${at + 1}_tr") {
        app.tsuzuku.notifications.VerificationFlow(
            onProgress = {}, onSolved = {}, seed = 11,
            order = listOf(app.tsuzuku.notifications.AlarmTask.Math, app.tsuzuku.notifications.AlarmTask.Hunt, app.tsuzuku.notifications.AlarmTask.Memory),
            startAt = at,
        )
    }
    @Test fun alarmMorningTr() = renderAlarm("alarm_morning_tr") {
        app.tsuzuku.notifications.MorningPage(
            greeting = "Günaydın", date = "9 Kasım Pazartesi", streak = 5, habits = sampleToday("tr"), onStartDay = {},
        )
    }
    @Test fun editorAlarmTr() {
        setVariant("tr", false)
        compose.setContent {
            TsuzukuTheme(darkTheme = false) {
                app.tsuzuku.ui.screens.HabitEditorContent(existing = null, onBack = {}, onSave = {}, layout = app.tsuzuku.ui.screens.EditorLayout.Steps)
            }
        }
        compose.mainClock.advanceTimeBy(800)
        captureScreen("editor_wake_card_tr")
        compose.onNodeWithText("Uyanma alarmı kur").performClick()
        compose.mainClock.advanceTimeBy(800)
        repeat(2) {
            compose.onNodeWithText("Devam et").performClick()
            compose.mainClock.advanceTimeBy(800)
        }
        compose.onNodeWithText("Alarm olarak çal").performScrollTo()
        compose.mainClock.advanceTimeBy(800)
        captureScreen("editor_alarm_options_tr")
    }

    @Test fun templateLibraryTr() {
        setVariant("tr", false)
        compose.setContent {
            TsuzukuTheme(darkTheme = false) {
                app.tsuzuku.ui.screens.HabitEditorContent(existing = null, onBack = {}, onSave = {}, startInLibrary = true)
            }
        }
        compose.mainClock.advanceTimeBy(800)
        captureScreen("template_library_tr")
        compose.onNodeWithText("Esneme").performClick()
        compose.mainClock.advanceTimeBy(800)
        captureScreen("template_picked_tr")
    }

    @Test fun spreadRemindersTr() {
        setVariant("tr", false)
        compose.setContent {
            TsuzukuTheme(darkTheme = false) {
                app.tsuzuku.ui.screens.HabitEditorContent(existing = null, onBack = {}, onSave = {}, startInLibrary = true)
            }
        }
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithText("Su iç").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(800)
        repeat(2) { compose.onNodeWithText("Devam et").performClick(); compose.mainClock.advanceTimeBy(800) }
        compose.onNodeWithText("Gün boyu hatırlat").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(800)
        captureScreen("spread_reminders_tr")
    }

    // Package A on Today: toolbar, time-of-day groups, compact rows, reorder mode, swipe, confetti.
    @Test fun todayPackageATr() {
        setVariant("tr", false)
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        app.tsuzuku.ui.screens.TodayViewPrefs.load(ctx)
        app.tsuzuku.ui.screens.TodayViewPrefs.setGroupByTime(ctx, false)
        app.tsuzuku.ui.screens.TodayViewPrefs.setCompact(ctx, false)
        val times = listOf(7 * 60 + 30, 13 * 60, 21 * 60, null)
        val base = sampleToday("tr").mapIndexed { i, t -> t.copy(habit = t.habit.copy(reminderMinutes = times[i % times.size])) }
        val state = mutableStateOf(base)
        compose.setContent {
            TsuzukuTheme(darkTheme = false) {
                Shell(title = androidx.compose.ui.res.stringResource(app.tsuzuku.R.string.today), route = "today", profile = "Samet") { modifier ->
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
        compose.mainClock.advanceTimeBy(800)
        val list = compose.onNode(androidx.compose.ui.test.hasScrollAction())
        list.performScrollToNode(androidx.compose.ui.test.hasText("Alışkanlıklar"))
        compose.mainClock.advanceTimeBy(600)
        captureScreen("todayA_normal_tr")
        // Swipe right part-way on the first habit.
        val first = base.first().habit.name
        compose.onNodeWithText(first).performTouchInput {
            down(centerLeft + androidx.compose.ui.geometry.Offset(40f, 0f))
            moveBy(androidx.compose.ui.geometry.Offset(260f, 0f))
        }
        compose.mainClock.advanceTimeBy(300)
        captureScreen("todayA_swipe_tr")
        compose.onNodeWithText(first).performTouchInput { up() }
        compose.mainClock.advanceTimeBy(800)
        app.tsuzuku.ui.screens.TodayViewPrefs.setGroupByTime(ctx, true)
        compose.mainClock.advanceTimeBy(800)
        list.performScrollToNode(androidx.compose.ui.test.hasText("Alışkanlıklar"))
        compose.mainClock.advanceTimeBy(400)
        captureScreen("todayA_grouped_tr")
        app.tsuzuku.ui.screens.TodayViewPrefs.setCompact(ctx, true)
        compose.mainClock.advanceTimeBy(800)
        captureScreen("todayA_compact_tr")
        app.tsuzuku.ui.screens.TodayViewPrefs.setGroupByTime(ctx, false)
        app.tsuzuku.ui.screens.TodayViewPrefs.setCompact(ctx, false)
        compose.onNodeWithContentDescription("Sırala").performClick()
        compose.mainClock.advanceTimeBy(800)
        list.performScrollToNode(androidx.compose.ui.test.hasText("Bitti"))
        compose.mainClock.advanceTimeBy(400)
        captureScreen("todayA_reorder_tr")
        compose.onNodeWithText("Bitti").performClick()
        state.value = sampleToday("tr", allDone = true)
        compose.mainClock.advanceTimeBy(250)
        captureScreen("todayA_confetti0_tr")
        compose.mainClock.advanceTimeBy(450)
        captureScreen("todayA_confetti_tr")
    }

    @Test fun emojiPickerTr() {
        setVariant("tr", false)
        compose.setContent { TsuzukuTheme(darkTheme = false) { app.tsuzuku.ui.screens.EmojiPickerContent(selected = "💧", onPick = {}, onDismiss = {}) } }
        compose.mainClock.advanceTimeBy(800)
        captureScreen("emoji_picker_tr")
    }

    @Test fun archiveTr() {
        setVariant("tr", false)
        val habits = sampleToday("tr").take(3).map { it.habit.copy(createdOn = LocalDate.of(2026, 3, 14)) }
        compose.setContent { TsuzukuTheme(darkTheme = false) { app.tsuzuku.ui.screens.ArchiveScreen(habits, onBack = {}, onRestore = {}, onDelete = {}) } }
        compose.mainClock.advanceTimeBy(800)
        captureScreen("archive_tr")
    }

    @Test fun task01() = renderTask("task01_math") { app.tsuzuku.notifications.MathMission(onProgress = {}, onSolved = {}, seed = 7, count = 1) }
    @Test fun task02() = renderTask("task02_memory") { app.tsuzuku.notifications.MemoryMission(onProgress = {}, onSolved = {}, seed = 3, preview = listOf(4), lengths = listOf(4)) }
    @Test fun task03() = renderTask("task03_hunt") { app.tsuzuku.notifications.PuzzleMission(onProgress = {}, onSolved = {}, seed = 5, startAt = 4, size = 3) }
    @Test fun task04() = renderTask("task04_shake") { app.tsuzuku.notifications.ShakeMission(onProgress = {}, onSolved = {}, preview = 18) }
    @Test fun task05() = renderTask("task05_steps") { app.tsuzuku.notifications.StepMission(onProgress = {}, onSolved = {}, preview = 12) }
    @Test fun task06() = renderTask("task06_type") { app.tsuzuku.notifications.TypeMission(onProgress = {}, onSolved = {}, seed = 1, preview = "Kalktım ve") }
    @Test fun task07() = renderTask("task07_stroop") { app.tsuzuku.notifications.StroopMission(onProgress = {}, onSolved = {}, seed = 4) }
    @Test fun task08() = renderTask("task08_anagram") { app.tsuzuku.notifications.AnagramMission(onProgress = {}, onSolved = {}, seed = 2) }
    @Test fun task09() = renderTask("task09_pattern") { app.tsuzuku.notifications.PatternMission(onProgress = {}, onSolved = {}, seed = 9, preview = 3) }
    @Test fun task10() = renderTask("task10_catch") { app.tsuzuku.notifications.CatchMission(onProgress = {}, onSolved = {}, seed = 6) }

    @Test fun trashA() = renderMock("trash_a") { TrashOptionA() }
    @Test fun trashB() = renderMock("trash_b") { TrashOptionB() }
    @Test fun trashC() = renderMock("trash_c") { TrashOptionC() }

    private fun renderMock(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        setVariant("tr", dark)
        compose.setContent { TsuzukuTheme(darkTheme = dark) { content() } }
        compose.mainClock.advanceTimeBy(900)
        captureScreen(name)
    }

    @Test fun alarmTypeSettings() =renderAlarmType("alarm_type_settings", dark = false)
    @Test fun alarmTypeSettingsDark() = renderAlarmType("alarm_type_settings_dark", dark = true)

    private fun renderAlarmType(name: String, dark: Boolean) {
        setVariant("tr", dark)
        val state = app.tsuzuku.ui.screens.HabitEditorState(null).apply {
            this.name = "Uyan"; emoji = "⏰"
            chooseAlarmType()
            alarmTasks = "Math,Shake,Pattern"
            alarmLevel = 2
        }
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                androidx.compose.material3.Surface {
                    androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(20.dp)) {
                        app.tsuzuku.ui.screens.AlarmTypeSection(state)
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(900)
        captureScreen(name)
    }

    private fun renderTask(name: String, content: @Composable () -> Unit) {
        setVariant("tr", true)
        compose.mainClock.autoAdvance = false
        compose.setContent { TsuzukuTheme(darkTheme = true) { content() } }
        compose.mainClock.advanceTimeBy(600)
        captureScreen(name)
    }

    private fun renderAlarm(name: String, content: @Composable () -> Unit) {
        setVariant("tr", true)
        compose.setContent { TsuzukuTheme(darkTheme = true) { content() } }
        compose.mainClock.advanceTimeBy(900)
        captureScreen(name)
    }

    // Habit editor: three layouts (new habit), plus an open "more" group.
    @Test fun editorCardsTr() = renderEditorLayout(app.tsuzuku.ui.screens.EditorLayout.Cards, false)
    @Test fun editorStepsTr() = renderEditorLayout(app.tsuzuku.ui.screens.EditorLayout.Steps, false)
    @Test fun editorCardsTrDark() = renderEditorLayout(app.tsuzuku.ui.screens.EditorLayout.Cards, true)
    @Test fun editorStepsTr2() = renderEditorLayout(app.tsuzuku.ui.screens.EditorLayout.Steps, false, step = 2)
    @Test fun editorCardsMoreTr() = renderEditorLayout(app.tsuzuku.ui.screens.EditorLayout.Cards, false, open = app.tsuzuku.ui.screens.AdvancedGroup.Goal, scrolled = true)

    private fun renderEditorLayout(
        layout: app.tsuzuku.ui.screens.EditorLayout,
        dark: Boolean,
        step: Int = 0,
        open: app.tsuzuku.ui.screens.AdvancedGroup? = null,
        scrolled: Boolean = false,
    ) {
        setVariant("tr", dark)
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                app.tsuzuku.ui.screens.HabitEditorContent(
                    existing = null, onBack = {}, onSave = {}, layout = layout, initialStep = step, openGroup = open,
                )
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        if (scrolled) {
            compose.onNodeWithText(compose.activity.getString(app.tsuzuku.R.string.editor_group_goal)).performScrollTo()
            compose.mainClock.advanceTimeBy(1_000)
        }
        captureScreen("editorlayout_${layout.name.lowercase()}_tr_${if (dark) "dark" else "light"}" + (if (step > 0) "_s$step" else "") + (if (open != null) "_open" else ""))
    }

    // Widget page: three layouts.
    @Test fun widgetPageListTr() = renderWidgetPage(app.tsuzuku.ui.screens.WidgetsLayout.List, false)
    @Test fun widgetPageCarouselTr() = renderWidgetPage(app.tsuzuku.ui.screens.WidgetsLayout.Carousel, false)
    @Test fun widgetPageHomeTr() = renderWidgetPage(app.tsuzuku.ui.screens.WidgetsLayout.HomeScreen, false)
    @Test fun widgetPageListTrDark() = renderWidgetPage(app.tsuzuku.ui.screens.WidgetsLayout.List, true)
    @Test fun widgetPageCarouselTrDark() = renderWidgetPage(app.tsuzuku.ui.screens.WidgetsLayout.Carousel, true)
    @Test fun widgetPageCarouselStreakTr() = renderWidgetPage(app.tsuzuku.ui.screens.WidgetsLayout.Carousel, false, page = 2)
    @Test fun widgetPageCarouselWeekTrDark() = renderWidgetPage(app.tsuzuku.ui.screens.WidgetsLayout.Carousel, true, page = 3)
    @Test fun widgetPageCarouselAllTr1() = renderWidgetPage(app.tsuzuku.ui.screens.WidgetsLayout.Carousel, false, page = 1)
    @Test fun widgetPageCarouselAllTr3() = renderWidgetPage(app.tsuzuku.ui.screens.WidgetsLayout.Carousel, false, page = 3)
    @Test fun widgetPageCarouselAllTr4() = renderWidgetPage(app.tsuzuku.ui.screens.WidgetsLayout.Carousel, false, page = 4)
    @Test fun widgetPageCarouselAllTr5() = renderWidgetPage(app.tsuzuku.ui.screens.WidgetsLayout.Carousel, false, page = 5)
    @Test fun widgetPageHomeTrDark() = renderWidgetPage(app.tsuzuku.ui.screens.WidgetsLayout.HomeScreen, true)

    private fun renderWidgetPage(layout: app.tsuzuku.ui.screens.WidgetsLayout, dark: Boolean, page: Int = 0) {
        setVariant("tr", dark)
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                app.tsuzuku.ui.screens.WidgetsScreen(onBack = {}, layout = layout, canPinOverride = true, initialPage = page)
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        captureScreen("widgetpage_${layout.name.lowercase()}_tr_${if (dark) "dark" else "light"}" + if (page > 0) "_p$page" else "")
    }

    // Language page: three layouts with flags.
    @Test fun languageListTr() = renderLanguage(app.tsuzuku.ui.screens.LanguageLayout.List, false)
    @Test fun languageGridTr() = renderLanguage(app.tsuzuku.ui.screens.LanguageLayout.Grid, false)
    @Test fun languageSearchTr() = renderLanguage(app.tsuzuku.ui.screens.LanguageLayout.Search, false)
    @Test fun languageListTrDark() = renderLanguage(app.tsuzuku.ui.screens.LanguageLayout.List, true)
    @Test fun languageGridTrDark() = renderLanguage(app.tsuzuku.ui.screens.LanguageLayout.Grid, true)
    @Test fun languageSearchTrDark() = renderLanguage(app.tsuzuku.ui.screens.LanguageLayout.Search, true)

    private fun renderLanguage(layout: app.tsuzuku.ui.screens.LanguageLayout, dark: Boolean) {
        setVariant("tr", dark)
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                app.tsuzuku.ui.screens.LanguageScreen(
                    selected = app.tsuzuku.data.preferences.LanguagePreference.Turkish,
                    onBack = {}, onSelect = {}, layout = layout,
                )
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        captureScreen("language_${layout.name.lowercase()}_tr_${if (dark) "dark" else "light"}")
    }

    // Appearance: three layouts for theme + palette together.
    @Test fun appearancePreviewTr() = renderAppearance(app.tsuzuku.ui.screens.AppearanceLayout.Preview, false)
    @Test fun appearanceCardsTr() = renderAppearance(app.tsuzuku.ui.screens.AppearanceLayout.Cards, false)
    @Test fun appearanceGridTr() = renderAppearance(app.tsuzuku.ui.screens.AppearanceLayout.Grid, false)
    @Test fun appearancePreviewTrDark() = renderAppearance(app.tsuzuku.ui.screens.AppearanceLayout.Preview, true)
    @Test fun appearanceCardsTrDark() = renderAppearance(app.tsuzuku.ui.screens.AppearanceLayout.Cards, true)
    @Test fun appearanceGridTrDark() = renderAppearance(app.tsuzuku.ui.screens.AppearanceLayout.Grid, true)

    // Package D: new palettes, the look section, and Today in Momiji / Yoru with autumn touches.
    @Test fun lookPackageDTr() {
        setVariant("tr", false)
        compose.setContent {
            TsuzukuTheme(darkTheme = false) {
                app.tsuzuku.ui.screens.AppearanceScreen(
                    theme = app.tsuzuku.data.preferences.ThemePreference.Light,
                    palette = app.tsuzuku.data.preferences.PalettePreference.Momiji,
                    pureBlack = false, systemDark = false,
                    onBack = {}, onSetTheme = {}, onSetPalette = {}, onSetPureBlack = {},
                )
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(androidx.compose.ui.test.hasText("Momiji"))
        compose.mainClock.advanceTimeBy(500)
        captureScreen("lookD_palettes_tr")
        compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(androidx.compose.ui.test.hasText("Uygulama simgesi"))
        compose.mainClock.advanceTimeBy(500)
        captureScreen("lookD_section_tr")
        compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(androidx.compose.ui.test.hasText("Gece"))
        compose.mainClock.advanceTimeBy(500)
        captureScreen("lookD_icons_tr")
    }

    @Test fun todayMomijiTr() = renderTodayPalette(app.tsuzuku.data.preferences.PalettePreference.Momiji, false, "lookD_today_momiji_tr")
    @Test fun todayYoruTr() = renderTodayPalette(app.tsuzuku.data.preferences.PalettePreference.Yoru, true, "lookD_today_yoru_tr")

    private fun renderTodayPalette(palette: app.tsuzuku.data.preferences.PalettePreference, dark: Boolean, name: String) {
        setVariant("tr", dark)
        compose.setContent {
            TsuzukuTheme(darkTheme = dark, palette = palette) {
                Shell(title = androidx.compose.ui.res.stringResource(app.tsuzuku.R.string.today), route = "today", profile = "Samet") { modifier ->
                    Box(modifier) {
                        TodayScreen(
                            habits = sampleToday("tr"), profileName = "Samet", showDailySummary = true,
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
        compose.mainClock.advanceTimeBy(6_000)
        captureScreen(name)
    }

    // Package E: year in review, card by card, plus its share card; screen-time template in the editor.
    @Test fun yearReviewTr() {
        setVariant("tr", false)
        val data = app.tsuzuku.ui.screens.YearReviewData(
            year = 2026, completions = 1248, activeDays = 286, perfectDays = 74, comebacks = 41,
            bestStreak = 63, bestStreakHabit = "Kitap oku", topHabit = "Su iç", topHabitEmoji = "💧", topHabitCount = 301,
            bestMonth = java.time.YearMonth.of(2026, 7), bestMonthRate = 0.91f, loved = "Meditasyon", lovedEmoji = "🧘", averageMood = 3.8f,
        )
        val page = mutableStateOf(0)
        compose.setContent {
            TsuzukuTheme(darkTheme = true) {
                androidx.compose.runtime.key(page.value) {
                    app.tsuzuku.ui.screens.YearReviewScreen(data, onClose = {}, onShare = {}, startPage = page.value)
                }
            }
        }
        (0..7).forEach { i ->
            page.value = i
            compose.mainClock.advanceTimeBy(1500)
            captureScreen("year_$i")
        }
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        save("year_share", app.tsuzuku.share.renderYearCard(ctx, data))
    }

    @Test fun screenTimeTemplateTr() {
        setVariant("tr", false)
        compose.setContent {
            TsuzukuTheme(darkTheme = false) {
                app.tsuzuku.ui.screens.HabitEditorContent(existing = null, onBack = {}, onSave = {}, layout = app.tsuzuku.ui.screens.EditorLayout.Steps)
            }
        }
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithText("Ekran süresi sınırı koy").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithText("Devam et").performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithText("Ekran süresinden doldur").performScrollTo()
        compose.mainClock.advanceTimeBy(500)
        captureScreen("screentime_editor_tr")
    }

    private fun renderAppearance(layout: app.tsuzuku.ui.screens.AppearanceLayout, dark: Boolean) {
        setVariant("tr", dark)
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                app.tsuzuku.ui.screens.AppearanceScreen(
                    theme = if (dark) app.tsuzuku.data.preferences.ThemePreference.Dark else app.tsuzuku.data.preferences.ThemePreference.Light,
                    palette = app.tsuzuku.data.preferences.PalettePreference.Washi,
                    pureBlack = false,
                    systemDark = dark,
                    onBack = {}, onSetTheme = {}, onSetPalette = {}, onSetPureBlack = {},
                    layout = layout,
                )
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        captureScreen("appearance_${layout.name.lowercase()}_tr_${if (dark) "dark" else "light"}")
    }

    // Account page: three signed-in layouts to choose from, plus signed out.
    @Test fun accountCardsTr() = renderAccount(app.tsuzuku.ui.screens.AccountLayout.Cards, false)
    @Test fun accountListTr() = renderAccount(app.tsuzuku.ui.screens.AccountLayout.List, false)
    @Test fun accountHeroTr() = renderAccount(app.tsuzuku.ui.screens.AccountLayout.Hero, false)
    @Test fun accountCardsTrDark() = renderAccount(app.tsuzuku.ui.screens.AccountLayout.Cards, true)
    @Test fun accountListTrDark() = renderAccount(app.tsuzuku.ui.screens.AccountLayout.List, true)
    @Test fun accountHeroTrDark() = renderAccount(app.tsuzuku.ui.screens.AccountLayout.Hero, true)
    @Test fun accountSignedOutTr() = renderAccount(app.tsuzuku.ui.screens.AccountLayout.Cards, false, signedIn = false)

    private fun renderAccount(layout: app.tsuzuku.ui.screens.AccountLayout, dark: Boolean, signedIn: Boolean = true) {
        setVariant("tr", dark)
        val account = if (signedIn) {
            app.tsuzuku.data.CloudAccountState.SignedIn("Abdulsamet Açıkgöz", "samet@example.com", null)
        } else {
            app.tsuzuku.data.CloudAccountState.SignedOut
        }
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                app.tsuzuku.ui.screens.AccountContent(
                    account = account,
                    syncState = app.tsuzuku.data.CloudSyncState.Idle,
                    cloudSyncEnabled = true,
                    lastCloudSyncAt = java.time.LocalDate.now().atTime(18, 21).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    onBack = {}, onSignIn = { _, _ -> }, onSetCloudSyncEnabled = {}, onSyncNow = {}, onChooseSource = {},
                    onSignOut = {}, onDelete = {}, onDismissStatus = {},
                    layout = layout,
                )
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        captureScreen("account_${if (signedIn) layout.name.lowercase() else "signedout"}_tr_${if (dark) "dark" else "light"}")
    }

    private fun renderWelcome(lang: String, dark: Boolean, signedIn: Boolean = false, nameStep: Boolean = false, name: String = "", chooseStep: Boolean = false) {
        setVariant(lang, dark)
        val account = if (signedIn) {
            app.tsuzuku.data.CloudAccountState.SignedIn("Samet Example", "samet@example.com", null)
        } else {
            app.tsuzuku.data.CloudAccountState.SignedOut
        }
        compose.setContent {
            TsuzukuTheme(darkTheme = dark) {
                app.tsuzuku.ui.screens.WelcomeScreen(
                    account = account,
                    syncState = app.tsuzuku.data.CloudSyncState.Idle,
                    initialName = name,
                    onSignIn = { _, _ -> },
                    onFinish = { _, _ -> },
                    startOnName = nameStep || (signedIn && !chooseStep),
                )
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        captureScreen("welcome_${lang}_${if (dark) "dark" else "light"}" + (if (signedIn) "_signedin" else "") + (if (nameStep) "_name" else "") + (if (name.isNotEmpty()) "_filled" else "") + (if (chooseStep) "_choose" else ""))
    }

    private fun renderTour(lang: String, dark: Boolean = false) {
        setVariant(lang, dark)
        compose.setContent { TsuzukuTheme(darkTheme = dark) { app.tsuzuku.ui.screens.OnboardingScreen(onComplete = {}) } }
        val next = compose.activity.getString(app.tsuzuku.R.string.continue_label)
        repeat(5) { page ->
            compose.mainClock.advanceTimeBy(1_000)
            captureScreen("tour_${page + 1}_${lang}_${if (dark) "dark" else "light"}")
            if (page < 4) compose.onNodeWithText(next).performClick()
        }
    }

    @Test fun palettePickerTr() {
        setVariant("tr", false)
        compose.setContent {
            TsuzukuTheme(darkTheme = false, palette = app.tsuzuku.data.preferences.PalettePreference.Sumi) {
                androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh) {
                    app.tsuzuku.ui.screens.PalettePickerGrid(
                        selected = app.tsuzuku.data.preferences.PalettePreference.Sumi,
                        onSelect = {},
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        captureScreen("palette_picker_tr_light")
    }

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
                Shell(title = androidx.compose.ui.res.stringResource(app.tsuzuku.R.string.today), route = "today", profile = "Samet") { modifier ->
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
                Shell(title = androidx.compose.ui.res.stringResource(app.tsuzuku.R.string.progress_tab), route = "progress", profile = "Samet") { modifier ->
                    Box(modifier) { StatisticsScreen(history = history, libraryItems = emptyList()) }
                }
            }
        }
        captureScreen("progress_${lang}_${if (dark) "dark" else "light"}")
    }

    // Package C: weekly card, month calendar (done / mood), badge showcase, clean days, share card.
    @Test fun progressPackageCTr() {
        setVariant("tr", false)
        val history = sampleHistory("tr")
        val today = LocalDate.now()
        val moods = (0L..40L).associate { today.minusDays(it) to (1 + ((it * 7 + 3) % 5)).toInt() }
        val milestones = listOf(
            app.tsuzuku.model.PersonalMilestone("streak_7", "", "", "🔥", 7, 7, true),
            app.tsuzuku.model.PersonalMilestone("streak_30", "", "", "🏆", 16, 30, false),
            app.tsuzuku.model.PersonalMilestone("completions_100", "", "", "💯", 100, 100, true),
            app.tsuzuku.model.PersonalMilestone("comeback_1", "", "", "✨", 1, 1, true),
            app.tsuzuku.model.PersonalMilestone("focus_10", "", "", "⏱️", 4, 10, false),
            app.tsuzuku.model.PersonalMilestone("goals_1", "", "", "🎯", 0, 1, false),
        )
        val records = app.tsuzuku.model.PersonalRecords(16, "Kitap oku", "Salı", 41, 12, 230, 2, 3, milestones)
        val tab = mutableStateOf(app.tsuzuku.ui.screens.StatisticsTab.General)
        compose.setContent {
            TsuzukuTheme(darkTheme = false) {
                Shell(title = androidx.compose.ui.res.stringResource(app.tsuzuku.R.string.progress_tab), route = "progress", profile = "Samet") { modifier ->
                    Box(modifier) {
                        androidx.compose.runtime.key(tab.value) {
                            StatisticsScreen(history = history, libraryItems = emptyList(), moods = moods, personalRecords = records, initialTab = tab.value)
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1500)
        captureScreen("progressC_week_tr")
        tab.value = app.tsuzuku.ui.screens.StatisticsTab.Heatmap
        compose.mainClock.advanceTimeBy(1000)
        captureScreen("progressC_month_tr")
        compose.onNodeWithText("Ruh hali").performClick()
        compose.mainClock.advanceTimeBy(800)
        captureScreen("progressC_mood_tr")
        tab.value = app.tsuzuku.ui.screens.StatisticsTab.Records
        compose.mainClock.advanceTimeBy(1000)
        compose.onNode(androidx.compose.ui.test.hasScrollToNodeAction()).performScrollToNode(androidx.compose.ui.test.hasText("Rozet", substring = true))
        compose.mainClock.advanceTimeBy(600)
        captureScreen("progressC_badges_tr")
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val w = app.tsuzuku.ui.screens.weekSummary(history, today)
        val bmp = app.tsuzuku.share.renderWeekCard(ctx, app.tsuzuku.share.WeekCardData(w.rate, w.completed, w.scheduled, w.perfectDays, w.dayFractions, w.bestDay))
        save("progressC_sharecard_tr", bmp)
    }

    @Test fun cleanDaysTr() {
        setVariant("tr", false)
        val today = LocalDate.now()
        val quit = sampleToday("tr")[0].habit.copy(name = "Sigara yok", emoji = "🚭", direction = app.tsuzuku.model.HabitDirection.Quit,
            maximumTarget = 0.0, createdOn = today.minusDays(60), type = HabitType.Count, unit = "adet")
        val records = listOf(40L, 25L).map { HabitHistoryRecord(quit.id, today.minusDays(it), 3.0, HabitDayStatus.Pending) }
        compose.setContent {
            TsuzukuTheme(darkTheme = false) {
                app.tsuzuku.ui.screens.HabitDetailScreen(habit = quit, records = records, onBack = {}, onEdit = {}, onSaveNote = {}, onArchive = {}, onDuplicate = {}, onDelete = {})
            }
        }
        compose.mainClock.advanceTimeBy(1500)
        captureScreen("progressC_clean_tr")
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
                    Shell(title = androidx.compose.ui.res.stringResource(app.tsuzuku.R.string.settings_label), route = "settings", profile = "Samet") { modifier ->
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

    @Test fun detailLayoutTabsTr() = renderDetail("tr", false, app.tsuzuku.ui.screens.HabitDetailLayout.Tabs, "detaillayout_tabs_tr")
    @Test fun detailLayoutTabsNotesTr() = renderDetail("tr", false, app.tsuzuku.ui.screens.HabitDetailLayout.Tabs, "detaillayout_tabs_notes_tr", tapText = "Not ve geçmiş")
    @Test fun detailLayoutCardsTr() = renderDetail("tr", false, app.tsuzuku.ui.screens.HabitDetailLayout.Cards, "detaillayout_cards_tr", scrollTo = "Hedef ve seviyeler")
    @Test fun detailLayoutCardsOpenTr() = renderDetail("tr", false, app.tsuzuku.ui.screens.HabitDetailLayout.Cards, "detaillayout_cards_open_tr", scrollTo = "Not ve geçmiş", tapText = "Not ve geçmiş")
    @Test fun detailLayoutClassicTr() = renderDetail("tr", false, app.tsuzuku.ui.screens.HabitDetailLayout.Classic, "detaillayout_classic_tr")

    private fun renderDetail(
        lang: String,
        dark: Boolean,
        layout: app.tsuzuku.ui.screens.HabitDetailLayout = app.tsuzuku.ui.screens.HabitDetailLayout.Tabs,
        name: String? = null,
        tapText: String? = null,
        scrollTo: String? = null,
    ) {
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
                    onShare = {}, layout = layout,
                )
            }
        }
        compose.mainClock.advanceTimeBy(1500)
        if (scrollTo != null) {
            compose.onNode(androidx.compose.ui.test.hasScrollToNodeAction()).performScrollToNode(androidx.compose.ui.test.hasText(scrollTo))
            compose.mainClock.advanceTimeBy(500)
        }
        if (tapText != null) {
            compose.onNodeWithText(tapText).performClick()
            compose.mainClock.advanceTimeBy(800)
        }
        captureScreen(name ?: "detail_${lang}_${if (dark) "dark" else "light"}")
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
    @Test fun widgetsEsLight() = renderWidgets("es", false)
    @Test fun widgetsEsDark() = renderWidgets("es", true)
    @Test fun widgetsDeLight() = renderWidgets("de", false)
    @Test fun widgetsDeDark() = renderWidgets("de", true)
    @Test fun widgetsPtLight() = renderWidgets("pt", false)
    @Test fun widgetsPtDark() = renderWidgets("pt", true)
    @Test fun widgetsJaLight() = renderWidgets("ja", false)
    @Test fun widgetsJaDark() = renderWidgets("ja", true)
    @Test fun widgetsFrLight() = renderWidgets("fr", false)
    @Test fun widgetsFrDark() = renderWidgets("fr", true)
    @Test fun widgetsItLight() = renderWidgets("it", false)
    @Test fun widgetsItDark() = renderWidgets("it", true)
    @Test fun widgetsRuLight() = renderWidgets("ru", false)
    @Test fun widgetsRuDark() = renderWidgets("ru", true)
    @Test fun widgetsKoLight() = renderWidgets("ko", false)
    @Test fun widgetsKoDark() = renderWidgets("ko", true)

    @Test fun alarmStylesLight() = renderAlarmStyles(false)
    @Test fun alarmStylesDark() = renderAlarmStyles(true)

    @Test fun ringStylesLight() = renderRingStyles(false)
    @Test fun ringStylesDark() = renderRingStyles(true)

    @Test fun ensoLabelsLight() = renderEnsoLabels(false)
    @Test fun ensoLabelsDark() = renderEnsoLabels(true)

    private fun renderEnsoLabels(dark: Boolean) {
        setVariant("tr", dark)
        val mode = if (dark) "dark" else "light"
        val base = sampleToday("tr")
        listOf(1, 3, 5).forEach { doneCount ->
            val habits = base.mapIndexed { i, t -> if (i < doneCount) t.copy(status = HabitDayStatus.Completed, progress = t.habit.target) else t.copy(status = HabitDayStatus.Pending, progress = 0.0) }
            app.tsuzuku.widget.EnsoLabel.entries.forEach { label ->
                renderWidget("ensolabel_${label.name.lowercase()}_${doneCount}_$mode", 110, 110) {
                    TsuzukuGlanceTheme { app.tsuzuku.widget.RingStyled(habits, locked = false, style = app.tsuzuku.widget.RingStyle.Enso, ensoLabel = label) }
                }
            }
        }
    }

    private fun renderRingStyles(dark: Boolean) {
        setVariant("tr", dark)
        val mode = if (dark) "dark" else "light"
        val base = sampleToday("tr")
        listOf(1, 3, 5).forEach { doneCount ->
            val habits = base.mapIndexed { i, t -> if (i < doneCount) t.copy(status = HabitDayStatus.Completed, progress = t.habit.target) else t.copy(status = HabitDayStatus.Pending, progress = 0.0) }
            app.tsuzuku.widget.RingStyle.entries.forEach { style ->
                renderWidget("ringstyle_${style.name.lowercase()}_${doneCount}_$mode", 110, 110) {
                    TsuzukuGlanceTheme { app.tsuzuku.widget.RingStyled(habits, locked = false, style = style) }
                }
            }
        }
    }

    private fun renderAlarmStyles(dark: Boolean) {
        setVariant("tr", dark)
        val mode = if (dark) "dark" else "light"
        listOf(true, false).forEach { on ->
            val wake = app.tsuzuku.widget.NextAlarm(1, "Uyan", "⏰", java.time.ZonedDateTime.now().plusDays(1).withHour(7).withMinute(0), on = on)
            app.tsuzuku.widget.AlarmStyle.entries.forEach { style ->
                renderWidget("alarmstyle_${style.name.lowercase()}_${if (on) "on" else "off"}_$mode", 300, 96) {
                    TsuzukuGlanceTheme { app.tsuzuku.widget.AlarmStyled(wake, style) }
                }
            }
        }
    }

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
        val longest = today.maxBy { it.streakDays }
        renderWidget("widget_streak_$suffix", 160, 170) { TsuzukuGlanceTheme { app.tsuzuku.widget.StreakContent(longest, comebacks = 2, locked = false) } }
        renderWidget("widget_next_$suffix", 300, 96) { TsuzukuGlanceTheme { app.tsuzuku.widget.NextHabitContent(today, locked = false) } }
        renderWidget("widget_ring_$suffix", 110, 110) { TsuzukuGlanceTheme { app.tsuzuku.widget.RingContent(today, locked = false) } }
        renderWidget("widget_heatmap_$suffix", 320, 170) { TsuzukuGlanceTheme { app.tsuzuku.widget.HeatmapContent(sampleHistory(lang), locked = false) } }
        val wake = app.tsuzuku.widget.NextAlarm(1, if (lang == "tr") "Uyan" else "Wake up", "⏰",
            java.time.ZonedDateTime.now().plusDays(1).withHour(7).withMinute(0), on = true)
        renderWidget("widget_alarm_$suffix", 300, 96) { TsuzukuGlanceTheme { app.tsuzuku.widget.AlarmContent(wake) } }
    }
}
