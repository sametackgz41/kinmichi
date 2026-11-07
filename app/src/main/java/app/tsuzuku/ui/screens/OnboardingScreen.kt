package app.tsuzuku.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.ui.theme.SerifDisplay

private enum class TourPage(val titleRes: Int, val bodyRes: Int) {
    Welcome(R.string.tour_welcome_title, R.string.tour_welcome_body),
    BadDay(R.string.tour_bad_day_title, R.string.tour_bad_day_body),
    Kintsugi(R.string.tour_kintsugi_title, R.string.tour_kintsugi_body),
    IfThen(R.string.tour_if_then_title, R.string.tour_if_then_body),
    Privacy(R.string.tour_privacy_title, R.string.tour_privacy_body),
}

/**
 * First-run tour: what makes Kinmichi different, one idea per page, each with a small drawing
 * made of the app's own pieces.
 */
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val pages = TourPage.entries
    val last = step == pages.lastIndex
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (!last) TextButton(onClick = onComplete) { Text(stringResource(R.string.skip_label)) }
        }
        Spacer(Modifier.height(12.dp))
        StepDots(count = pages.size, current = step)

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
            },
            label = "tour-page",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { index ->
            val page = pages[index]
            Column(
                Modifier.fillMaxSize().padding(top = 24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    when (page) {
                        TourPage.Welcome -> GoldenPathArt()
                        TourPage.BadDay -> BadDayArt()
                        TourPage.Kintsugi -> KintsugiArt()
                        TourPage.IfThen -> IfThenArt()
                        TourPage.Privacy -> PrivacyArt()
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    stringResource(page.titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(page.bodyRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = { if (last) onComplete() else step++ },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(if (last) R.string.start_using_tsuzuku else R.string.continue_label)) }
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.back)) }
        }
    }
}

/** Progress as the same small steps the icon is made of; the current one in gold. */
@Composable
private fun StepDots(count: Int, current: Int) {
    val ink = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.outlineVariant
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val size = if (i == current) 12.dp else 8.dp
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        when {
                            i == current -> gold
                            i < current -> ink
                            else -> track
                        },
                    ),
            )
        }
    }
}

/** The app icon at a larger size: steps climbing a gentle curve, today in gold. */
@Composable
private fun GoldenPathArt() {
    val ink = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.colorScheme.tertiary
    Canvas(Modifier.size(200.dp)) {
        val p0 = Offset(0.22f, 0.82f)
        val p1 = Offset(0.22f, 0.50f)
        val p2 = Offset(0.78f, 0.62f)
        val p3 = Offset(0.78f, 0.20f)
        fun at(t: Float): Offset {
            val u = 1 - t
            return p0 * (u * u * u) + p1 * (3 * u * u * t) + p2 * (3 * u * t * t) + p3 * (t * t * t)
        }
        val steps = 6
        for (k in 0 until steps) {
            val t = k / (steps - 1f)
            val p = at(t)
            val radius = size.minDimension * (if (k == steps - 1) 0.075f else 0.042f + k * 0.004f)
            drawCircle(if (k == steps - 1) gold else ink.copy(alpha = 0.55f + k * 0.08f), radius, Offset(p.x * size.width, p.y * size.height))
        }
    }
}

/** A habit and its bad-day version side by side; the small one still gets the check. */
@Composable
private fun BadDayArt() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.width(280.dp)) {
        ArtRow(stringResource(R.string.tour_art_habit), stringResource(R.string.tour_art_habit_detail), done = false, faded = true)
        ArtRow(stringResource(R.string.tour_art_tiny), stringResource(R.string.tiny_version_label), done = true, gold = true)
    }
}

@Composable
private fun ArtRow(title: String, detail: String, done: Boolean, faded: Boolean = false, gold: Boolean = false) {
    val accent = if (gold) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(if (gold) 1.5.dp else 1.dp, if (gold) accent else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (faded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SerifDisplay, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal),
                    color = if (gold) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (done) accent else Color.Transparent)
                    .border(2.dp, if (done) accent else MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Ten days: a run, one empty day, and the comeback after it drawn with a gold seam. */
@Composable
private fun KintsugiArt() {
    val ink = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val gold = MaterialTheme.colorScheme.tertiary
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(10) { day ->
            val missed = day == 4
            val comeback = day == 5
            Box(
                Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (missed) empty else ink.copy(alpha = if (day < 4) 0.55f + day * 0.1f else 0.9f))
                    .then(if (comeback) Modifier.border(3.dp, gold, RoundedCornerShape(6.dp)) else Modifier),
            )
        }
    }
}

/** The plan written out as a sentence, the way it shows on Today. */
@Composable
private fun IfThenArt() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(290.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.tour_art_if),
                style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.tour_art_then),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** A lock over a small phone: everything stays here. */
@Composable
private fun PrivacyArt() {
    Box(
        Modifier
            .size(width = 120.dp, height = 190.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
        }
    }
}
