package app.tsuzuku.widget

import androidx.glance.semantics.semantics
import androidx.glance.semantics.contentDescription
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.first
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as FixedColor
import app.tsuzuku.HomeActivity
import app.tsuzuku.R
import app.tsuzuku.TsuzukuApplication
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.ui.screens.formatHabitAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * "Today" widget: a day-progress ring with what's left, then every habit with a one-tap
 * action in its own colour (check, +1 ring, or open the timer). Scrolls when the list is long;
 * shrinks to just the summary at small sizes.
 */
class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TsuzukuApplication
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val selected = WidgetSettings(context).selectedHabitIds(widgetId)
        // Load before drawing so a fresh session never shows an empty first frame (that flickered).
        val todayFlow = app.habitRepository.observeToday()
        val initial = todayFlow.first()
        provideContent {
            val habits by remember { todayFlow }.collectAsState(initial = initial)
            val locked = app.settingsRepository.settings.value.appLockEnabled
            val visible = habits.orEmpty()
                .let { all -> if (selected.isEmpty()) all else all.filter { it.habit.id in selected } }
                .sortedBy { it.isComplete }
            TsuzukuGlanceTheme { TodayContent(visible, loaded = habits != null, locked = locked) }
        }
    }
}

/** Keeps the original receiver name so widgets users already placed keep working. */
class TsuzukuWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val settings = WidgetSettings(context)
        appWidgetIds.forEach(settings::delete)
    }

    companion object {
        fun requestUpdate(context: Context) = TsuzukuWidgets.requestUpdate(context)
    }
}

/**
 * @param scrollable a LazyColumn on the home screen; previews pass false because the static
 *   renderer can't populate collection views.
 */
@Composable
internal fun TodayContent(
    habits: List<TodayHabit>,
    loaded: Boolean,
    locked: Boolean,
    today: LocalDate = LocalDate.now(),
    scrollable: Boolean = true,
) {
    val size = LocalSize.current
    val compact = size.height < 150.dp
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(tsuzukuWidgetBackground())
            .cornerRadius(24.dp)
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 6.dp),
    ) {
        Header(habits, locked, today)
        if (locked || !loaded) return@Column
        if (habits.isEmpty()) {
            EmptyState()
            return@Column
        }
        if (compact) return@Column
        Spacer(GlanceModifier.height(8.dp))
        val rows = ((size.height.value - 76f) / 50f).toInt().coerceAtLeast(1)
        // Taps inside a scrolling list go through a hidden trampoline activity, which is slower and
        // briefly pauses the launcher. Only scroll when the habits don't fit; otherwise use a plain
        // column so a tap reaches the app directly.
        if (scrollable && habits.size > rows) {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(habits, itemId = { it.habit.id }) { item -> HabitLine(item) }
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                habits.take(rows).forEach { item -> HabitLine(item) }
            }
        }
    }
}

@Composable
private fun Header(habits: List<TodayHabit>, locked: Boolean, today: LocalDate) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val completed = habits.count(TodayHabit::isComplete)
    val total = habits.size
    val left = total - completed
    val ringSize = 46.dp
    val primary = GlanceTheme.colors.primary.getColor(context).toArgb()
    val ring = ringBitmap(
        sizePx = (ringSize.value * density).toInt(),
        fraction = if (locked || total == 0) 0f else completed.toFloat() / total,
        trackColor = GlanceTheme.colors.surfaceVariant.getColor(context).toArgb(),
        progressColor = primary,
        strokePx = 5f * density,
        fillColor = if (!locked && total > 0 && left == 0) primary else null,
    )
    val subtitle = when {
        locked -> context.getString(R.string.widget_locked)
        total == 0 -> today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", context.resources.configuration.locales[0]))
        left == 0 -> context.getString(R.string.tile_all_done)
        else -> context.resources.getQuantityString(R.plurals.tile_habits_left, left, left)
    }
    Row(
        modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<HomeActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = GlanceModifier.size(ringSize), contentAlignment = Alignment.Center) {
            Image(ImageProvider(ring), contentDescription = null, modifier = GlanceModifier.size(ringSize))
            if (!locked && total > 0) {
                if (left == 0) {
                    Image(
                        ImageProvider(R.drawable.ic_widget_check),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                        modifier = GlanceModifier.size(24.dp),
                    )
                } else {
                    Text(
                        "$completed/$total",
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.glance.text.FontFamily.Serif),
                    )
                }
            }
        }
        Spacer(GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                context.getString(R.string.today),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Medium, fontFamily = androidx.glance.text.FontFamily.Serif),
                maxLines = 1,
            )
            Text(
                subtitle,
                style = TextStyle(
                    color = if (!locked && total > 0 && left == 0) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp,
                ),
                maxLines = 1,
            )
        }
        if (!locked) {
            CircleIconButton(
                imageProvider = ImageProvider(R.drawable.ic_widget_add),
                contentDescription = context.getString(R.string.shortcut_add_habit),
                onClick = actionStartActivity(addHabitIntent(context)),
                backgroundColor = GlanceTheme.colors.primaryContainer,
                contentColor = GlanceTheme.colors.onPrimaryContainer,
                modifier = GlanceModifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    val context = LocalContext.current
    Spacer(GlanceModifier.height(10.dp))
    Text(
        context.getString(R.string.widget_no_habits),
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
        modifier = GlanceModifier.clickable(actionStartActivity(addHabitIntent(context))),
    )
}

@Composable
private fun HabitLine(item: TodayHabit) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val habit = item.habit
    val accent = habit.colorArgb.toInt()
    val params = actionParametersOf(HabitIdKey to habit.id)
    val openApp: Action = actionStartActivity<HomeActivity>()
    val done = item.isComplete

    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Emoji on a soft disc in the habit's colour.
        Box(modifier = GlanceModifier.size(36.dp), contentAlignment = Alignment.Center) {
            Image(
                ImageProvider(dotBitmap((36 * density).toInt(), accent.withAlpha(if (done) 0.10f else 0.22f))),
                contentDescription = null,
                modifier = GlanceModifier.size(36.dp),
            )
            Text(habit.emoji, style = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Center))
        }
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight().clickable(openApp)) {
            Text(
                habit.name,
                style = TextStyle(
                    color = if (done) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = if (done) FontWeight.Normal else FontWeight.Medium,
                ),
                maxLines = 1,
            )
            val detail = habitDetail(context, item)
            if (detail.isNotEmpty()) {
                Text(detail, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 1)
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        ActionButton(item, accent, params, openApp)
    }
}

/** "5/8 glasses · 🔥 4" style second line; empty when there's nothing worth saying. */
private fun habitDetail(context: Context, item: TodayHabit): String {
    val habit = item.habit
    val parts = buildList {
        if (habit.type != HabitType.Check) {
            add("${formatHabitAmount(item.progress)}/${formatHabitAmount(habit.target)} ${habit.unit}".trim())
        }
        if (item.streakDays > 1) add("🔥 ${item.streakDays}")
        if (item.isBlocked && item.blockedByName != null) add("🔒 ${item.blockedByName}")
    }
    return parts.joinToString(" · ")
}

/**
 * Check: a round button that fills with the habit colour. Count: a progress ring around +1.
 * Duration: a progress ring around play, opening the app's timer.
 */
@Composable
private fun ActionButton(item: TodayHabit, accent: Int, params: androidx.glance.action.ActionParameters, openApp: Action) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val habit = item.habit
    val done = item.isComplete
    val sizeDp = 40
    val px = (sizeDp * density).toInt()
    val track = GlanceTheme.colors.surfaceVariant.getColor(context).toArgb()
    val action: Action? = when {
        item.isBlocked -> null
        habit.type == HabitType.Check -> actionRunCallback<ToggleHabitAction>(params)
        habit.type == HabitType.Count -> actionRunCallback<IncrementHabitAction>(params)
        else -> openApp
    }
    val ring = when (habit.type) {
        HabitType.Check -> ringBitmap(px, if (done) 1f else 0f, track, accent, 2.5f * density, fillColor = if (done) accent else null)
        else -> ringBitmap(px, if (done) 1f else item.progressFraction, track, accent, 3.5f * density, fillColor = if (done) accent else null)
    }
    val icon = when {
        done || habit.type == HabitType.Check -> R.drawable.ic_widget_check
        habit.type == HabitType.Count -> R.drawable.ic_widget_add
        else -> R.drawable.ic_widget_play
    }
    val description = when (habit.type) {
        HabitType.Check -> context.getString(if (done) R.string.widget_mark_not_done else R.string.widget_mark_done, habit.name)
        HabitType.Count -> context.getString(R.string.increase_progress)
        HabitType.Duration -> habit.name
    }
    if (habit.type == HabitType.Check && !item.isBlocked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        // A real checkbox: the launcher ticks it the moment it's tapped, before the app answers.
        NativeHabitCheck(done, accent, params, description, sizeDp)
        return
    }
    var modifier = GlanceModifier.size(sizeDp.dp)
    if (action != null) modifier = modifier.clickable(action)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(ImageProvider(ring), contentDescription = description, modifier = GlanceModifier.size(sizeDp.dp))
        if (done || habit.type != HabitType.Check) {
            Image(
                ImageProvider(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (done) FixedColor(androidx.compose.ui.graphics.Color.White) else GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.size(if (done) 22.dp else 20.dp),
            )
        }
    }
}

/**
 * Android 12+ checkbox drawn by the launcher itself, so the tick appears instantly on tap; the
 * app then saves it and refreshes the other widgets. Tinted with the habit's colour.
 */
@Composable
internal fun NativeHabitCheck(done: Boolean, accent: Int, params: androidx.glance.action.ActionParameters, description: String, sizeDp: Int) {
    Box(
        modifier = GlanceModifier.size(sizeDp.dp).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        androidx.glance.appwidget.CheckBox(
            checked = done,
            onCheckedChange = actionRunCallback<ToggleHabitAction>(params),
            colors = androidx.glance.appwidget.CheckboxDefaults.colors(
                checkedColor = FixedColor(androidx.compose.ui.graphics.Color(accent)),
                uncheckedColor = fixedThemeColor { it.outline },
            ),
        )
    }
}
