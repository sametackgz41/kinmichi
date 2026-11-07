package app.tsuzuku.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.data.preferences.PalettePreference
import app.tsuzuku.data.preferences.ThemePreference
import app.tsuzuku.ui.theme.paletteScheme

/** Three layouts for choosing theme and palette together, drawn side by side in previews. */
internal enum class AppearanceLayout { Preview, Cards, Grid }

/**
 * Theme (system / light / dark), palette and pure black on one page, with a live preview of the
 * Today screen in the chosen combination.
 */
@Composable
internal fun AppearanceScreen(
    theme: ThemePreference,
    palette: PalettePreference,
    pureBlack: Boolean,
    systemDark: Boolean,
    onBack: () -> Unit,
    onSetTheme: (ThemePreference) -> Unit,
    onSetPalette: (PalettePreference) -> Unit,
    onSetPureBlack: (Boolean) -> Unit,
    layout: AppearanceLayout = AppearanceLayout.Grid,
) {
    val dark = when (theme) {
        ThemePreference.System -> systemDark
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                Text(stringResource(R.string.appearance_theme_palette), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
            }
            when (layout) {
                AppearanceLayout.Preview -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PhoneMock(schemeFor(palette, dark, pureBlack), width = 220.dp)
                    }
                    ModeSegments(theme, onSetTheme)
                    PaletteDots(palette, onSetPalette)
                    PureBlackRow(pureBlack, onSetPureBlack)
                }
                AppearanceLayout.Cards -> {
                    ModeSegments(theme, onSetTheme)
                    availablePalettes().forEach { option ->
                        PaletteWideCard(option, schemeFor(option, dark, pureBlack), option == palette) { onSetPalette(option) }
                    }
                    PureBlackRow(pureBlack, onSetPureBlack)
                }
                AppearanceLayout.Grid -> {
                    ModeSegments(theme, onSetTheme)
                    Text(
                        stringResource(R.string.appearance_grid_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    availablePalettes().chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { option ->
                                SplitPaletteTile(
                                    palette = option,
                                    selectedPalette = palette == option,
                                    dark = dark,
                                    followSystem = theme == ThemePreference.System,
                                    pureBlack = pureBlack,
                                    onPick = { pickDark ->
                                        onSetPalette(option)
                                        if (theme != ThemePreference.System) onSetTheme(if (pickDark) ThemePreference.Dark else ThemePreference.Light)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    PureBlackRow(pureBlack, onSetPureBlack)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun availablePalettes(): List<PalettePreference> {
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return PalettePickerOrder.filter { it != PalettePreference.Dynamic || dynamic }
}

@Composable
private fun schemeFor(palette: PalettePreference, dark: Boolean, pureBlack: Boolean): ColorScheme {
    val context = LocalContext.current
    return if (palette == PalettePreference.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        paletteScheme(palette, dark, pureBlack)
    }
}

@Composable
private fun ModeSegments(theme: ThemePreference, onSetTheme: (ThemePreference) -> Unit) {
    val options = listOf(
        Triple(ThemePreference.System, Icons.Rounded.PhoneAndroid, R.string.theme_system),
        Triple(ThemePreference.Light, Icons.Rounded.LightMode, R.string.theme_light),
        Triple(ThemePreference.Dark, Icons.Rounded.DarkMode, R.string.theme_dark),
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, icon, label) ->
            SegmentedButton(
                selected = theme == value,
                onClick = { onSetTheme(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                // Icons only: the labels were too long to fit; the name is still read out by TalkBack.
                icon = {},
                label = { Icon(icon, contentDescription = stringResource(label), modifier = Modifier.size(22.dp)) },
            )
        }
    }
}

/** Round swatches in a row, each split light/dark, the name underneath. */
@Composable
private fun PaletteDots(selected: PalettePreference, onSelect: (PalettePreference) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        availablePalettes().forEach { option ->
            val light = schemeFor(option, false, false)
            val dark = schemeFor(option, true, false)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { onSelect(option) }.padding(6.dp),
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .border(if (option == selected) 3.dp else 1.dp, if (option == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .padding(5.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(light.primary))
                        Box(Modifier.weight(1f).fillMaxHeight().background(dark.primary))
                    }
                    Box(Modifier.size(12.dp).clip(CircleShape).background(light.tertiary))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    paletteName(option),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (option == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** A palette as a wide card: name on the left, a small Today in the current mode on the right. */
@Composable
private fun PaletteWideCard(palette: PalettePreference, scheme: ColorScheme, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 6.dp, end = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(paletteName(palette), style = MaterialTheme.typography.titleMedium)
                    if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp).size(18.dp))
                }
                Text(paletteDescriptionText(palette), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            MiniToday(scheme, Modifier.width(150.dp).height(96.dp))
        }
    }
}

/** Light half and dark half of one palette; tap a half to choose that palette in that mode. */
@Composable
private fun SplitPaletteTile(
    palette: PalettePreference,
    selectedPalette: Boolean,
    dark: Boolean,
    followSystem: Boolean,
    pureBlack: Boolean,
    onPick: (dark: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        ) {
            listOf(false, true).forEach { half ->
                val chosen = selectedPalette && (followSystem || half == dark)
                Box(Modifier.weight(1f).fillMaxHeight().clickable { onPick(half) }) {
                    MiniToday(schemeFor(palette, half, pureBlack), Modifier.fillMaxSize(), corner = 0.dp)
                    if (chosen) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            paletteName(palette),
            style = MaterialTheme.typography.titleSmall,
            color = if (selectedPalette) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun PureBlackRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.pure_black_label), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.pure_black_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun FollowSystemRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.PhoneAndroid, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(stringResource(R.string.appearance_follow_system), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.appearance_follow_system_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** A tiny phone showing the top of Today in [scheme]. */
@Composable
private fun PhoneMock(scheme: ColorScheme, width: Dp) {
    Box(
        Modifier
            .width(width)
            .height(width * 1.2f)
            .clip(RoundedCornerShape(28.dp))
            .border(6.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(28.dp))
            .padding(6.dp),
    ) {
        MiniToday(scheme, Modifier.fillMaxSize(), corner = 22.dp, rows = 5)
    }
}

/** A sketch of Today drawn in [scheme]: greeting, progress bar and a few habit rows. */
@Composable
internal fun MiniToday(scheme: ColorScheme, modifier: Modifier = Modifier, corner: Dp = 16.dp, rows: Int = 2) {
    Column(
        modifier.clip(RoundedCornerShape(corner)).background(scheme.background).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.width(54.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(scheme.onBackground.copy(alpha = 0.85f)))
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(scheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(0.6f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(scheme.primary))
        }
        repeat(rows) { index ->
            val done = index == 0
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(scheme.surfaceContainerLow).padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (index == 1) scheme.tertiary else scheme.secondaryContainer))
                Box(Modifier.padding(horizontal = 6.dp).weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(scheme.onSurface.copy(alpha = if (done) 0.35f else 0.7f)))
                Box(
                    Modifier.size(12.dp).clip(CircleShape)
                        .background(if (done) scheme.primary else Color.Transparent)
                        .border(1.5.dp, if (done) scheme.primary else scheme.outline, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun paletteDescriptionText(value: PalettePreference): String = stringResource(
    when (value) {
        PalettePreference.Washi -> R.string.palette_washi_desc
        PalettePreference.Sumi -> R.string.palette_sumi_desc
        PalettePreference.Matcha -> R.string.palette_matcha_desc
        PalettePreference.Sakura -> R.string.palette_sakura_desc
        PalettePreference.Umi -> R.string.palette_umi_desc
        PalettePreference.Dynamic -> R.string.palette_dynamic_desc
    },
)
