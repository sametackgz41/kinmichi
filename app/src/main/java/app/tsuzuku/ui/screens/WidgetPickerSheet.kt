package app.tsuzuku.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.widget.HabitWidgetReceiver
import app.tsuzuku.widget.MoodWidgetReceiver
import app.tsuzuku.widget.QuickGridWidgetReceiver
import app.tsuzuku.widget.StreakWidgetReceiver
import app.tsuzuku.widget.TsuzukuWidgetProvider
import app.tsuzuku.widget.WeekWidgetReceiver

/** One home-screen widget kind the user can add from Settings. [wide] widgets span the full width. */
internal data class WidgetKind(
    val receiver: Class<*>,
    @StringRes val name: Int,
    @StringRes val description: Int,
    @DrawableRes val preview: Int,
    val wide: Boolean,
)

internal val widgetKinds = listOf(
    WidgetKind(TsuzukuWidgetProvider::class.java, R.string.widget_today_name, R.string.widget_description, R.drawable.widget_preview_today, wide = true),
    WidgetKind(HabitWidgetReceiver::class.java, R.string.widget_habit_name, R.string.widget_habit_description, R.drawable.widget_preview_habit, wide = false),
    WidgetKind(StreakWidgetReceiver::class.java, R.string.widget_streak_name, R.string.widget_streak_description, R.drawable.widget_preview_streak, wide = false),
    WidgetKind(WeekWidgetReceiver::class.java, R.string.widget_week_name, R.string.widget_week_description, R.drawable.widget_preview_week, wide = true),
    WidgetKind(QuickGridWidgetReceiver::class.java, R.string.widget_quick_name, R.string.widget_quick_description, R.drawable.widget_preview_quick, wide = false),
    WidgetKind(MoodWidgetReceiver::class.java, R.string.widget_mood_name, R.string.widget_mood_description, R.drawable.widget_preview_mood, wide = true),
)

/** Three layouts for the widget page, drawn side by side in previews. */
internal enum class WidgetsLayout { List, Carousel, HomeScreen }

/**
 * Every widget with a real preview and an "Add" button. Uses the launcher's pin dialog when it
 * supports one (most launchers since Android 8); otherwise explains where the widgets live.
 */
@Composable
internal fun WidgetsScreen(onBack: () -> Unit, layout: WidgetsLayout = WidgetsLayout.Carousel, canPinOverride: Boolean? = null, initialPage: Int = 0) {
    val context = LocalContext.current
    val canPin = canPinOverride ?: remember(context) { canPinWidgets(context) }
    val add: (WidgetKind) -> Unit = { kind ->
        if (!pinWidget(context, kind.receiver)) Toast.makeText(context, R.string.widgets_sheet_manual, Toast.LENGTH_LONG).show()
    }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                Text(stringResource(R.string.widgets_sheet_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
            }
            Text(
                stringResource(if (canPin) R.string.widgets_sheet_subtitle else R.string.widgets_sheet_manual),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(12.dp))
            when (layout) {
                WidgetsLayout.List -> WidgetList(canPin, add)
                WidgetsLayout.Carousel -> WidgetCarousel(canPin, add, initialPage)
                WidgetsLayout.HomeScreen -> WidgetHomeScreen(canPin, add)
            }
        }
    }
}

/** A soft wallpaper in the palette's colours, so previews look like they do on a home screen. */
@Composable
private fun Modifier.wallpaper(): Modifier {
    val scheme = MaterialTheme.colorScheme
    return this.background(Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer, scheme.secondaryContainer)))
}

@Composable
private fun WidgetPreviewImage(kind: WidgetKind, modifier: Modifier = Modifier) {
    Image(painterResource(kind.preview), contentDescription = stringResource(kind.name), contentScale = ContentScale.Fit, modifier = modifier)
}

@Composable
private fun WidgetList(canPin: Boolean, onAdd: (WidgetKind) -> Unit) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        widgetKinds.forEach { kind ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
                Box(Modifier.fillMaxWidth().height(170.dp).wallpaper().padding(18.dp), contentAlignment = Alignment.Center) {
                    WidgetPreviewImage(kind, Modifier.fillMaxSize())
                }
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(kind.name), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(kind.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (canPin) {
                        FilledTonalButton(onClick = { onAdd(kind) }, contentPadding = PaddingValues(horizontal = 14.dp)) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                            Text(stringResource(R.string.add_to_home), Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetCarousel(canPin: Boolean, onAdd: (WidgetKind) -> Unit, initialPage: Int = 0) {
    val pager = rememberPagerState(initialPage = initialPage) { widgetKinds.size }
    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.weight(1f),
        ) { page ->
            val kind = widgetKinds[page]
            Column(Modifier.fillMaxHeight().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PhoneHomeMock(kind, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Text(stringResource(kind.name), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(kind.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
            repeat(widgetKinds.size) { index ->
                Box(
                    Modifier.padding(4.dp).size(if (index == pager.currentPage) 10.dp else 8.dp).clip(CircleShape)
                        .background(if (index == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        if (canPin) {
            Button(
                onClick = { onAdd(widgetKinds[pager.currentPage]) },
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp).height(56.dp),
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(20.dp))
                Text(stringResource(R.string.widgets_add_to_home_long), Modifier.padding(start = 6.dp))
            }
        }
    }
}

/**
 * The widget on a phone's home screen: bezel, wallpaper, clock and a few app icons, fading out at
 * the bottom like the website's phone, so it's clear how it will look once added.
 */
@Composable
private fun PhoneHomeMock(kind: WidgetKind, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val top = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
    val inner = RoundedCornerShape(topStart = 33.dp, topEnd = 33.dp)
    Box(modifier.padding(horizontal = 6.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(top)
                .background(scheme.surfaceContainerHighest)
                .padding(start = 7.dp, end = 7.dp, top = 7.dp)
                .clip(inner)
                .wallpaper(),
        ) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("9:41", style = MaterialTheme.typography.labelLarge, color = scheme.onPrimaryContainer)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(width = 30.dp, height = 10.dp).clip(RoundedCornerShape(5.dp)).background(scheme.onPrimaryContainer.copy(alpha = 0.55f)))
                }
                Spacer(Modifier.height(22.dp))
                if (kind.wide) {
                    WidgetPreviewImage(kind, Modifier.fillMaxWidth())
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WidgetPreviewImage(kind, Modifier.weight(1f))
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            repeat(2) { row -> AppIconRow(count = 2, seed = row) }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                AppIconRow(count = 4, seed = 2)
                Spacer(Modifier.height(16.dp))
                AppIconRow(count = 4, seed = 3)
                Spacer(Modifier.height(16.dp))
                AppIconRow(count = 4, seed = 1)
            }
        }
        // Fade the bottom of the phone into the page.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(110.dp)
                .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, scheme.background))),
        )
    }
}

/** A row of placeholder app icons with a label line under each. */
@Composable
private fun AppIconRow(count: Int, seed: Int) {
    val scheme = MaterialTheme.colorScheme
    val tints = listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.onPrimaryContainer)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        repeat(count) { index ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(14.dp))
                        .background(tints[(index + seed) % tints.size].copy(alpha = 0.22f + 0.08f * ((index + seed) % 3))),
                )
                Spacer(Modifier.height(5.dp))
                Box(Modifier.size(width = 26.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(scheme.onPrimaryContainer.copy(alpha = 0.35f)))
            }
        }
    }
}

/** All widgets laid out like a real home screen; tap one to pick it, then add it. */
@Composable
private fun WidgetHomeScreen(canPin: Boolean, onAdd: (WidgetKind) -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(32.dp))
                .wallpaper()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val rows = mutableListOf<List<Int>>()
            var pendingSmall = mutableListOf<Int>()
            widgetKinds.forEachIndexed { index, kind ->
                if (kind.wide) rows += listOf(index) else {
                    pendingSmall += index
                    if (pendingSmall.size == 2) { rows += pendingSmall.toList(); pendingSmall = mutableListOf() }
                }
            }
            if (pendingSmall.isNotEmpty()) rows += pendingSmall.toList()
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { index ->
                        val kind = widgetKinds[index]
                        val isSelected = index == selected
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(26.dp))
                                .border(if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.dp, androidx.compose.ui.graphics.Color.Transparent), RoundedCornerShape(26.dp))
                                .clickable { selected = index }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) { WidgetPreviewImage(kind, Modifier.fillMaxWidth()) }
                    }
                    if (row.size == 1 && !widgetKinds[row[0]].wide) Spacer(Modifier.weight(1f))
                }
            }
        }
        val kind = widgetKinds[selected]
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(kind.name), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(kind.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            if (canPin) {
                Button(onClick = { onAdd(kind) }, shape = RoundedCornerShape(24.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Text(stringResource(R.string.add_to_home), Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

private fun canPinWidgets(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

private fun pinWidget(context: Context, receiver: Class<*>): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
}
