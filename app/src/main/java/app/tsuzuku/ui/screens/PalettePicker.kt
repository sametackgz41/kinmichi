package app.tsuzuku.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.data.preferences.PalettePreference
import app.tsuzuku.ui.theme.paletteScheme

/** The palettes offered in the picker, in display order. */
internal val PalettePickerOrder = listOf(
    PalettePreference.Washi,
    PalettePreference.Sumi,
    PalettePreference.Matcha,
    PalettePreference.Sakura,
    PalettePreference.Umi,
    PalettePreference.Momiji,
    PalettePreference.Yoru,
    PalettePreference.Dynamic,
)

@Composable
internal fun paletteName(value: PalettePreference): String = stringResource(
    when (value) {
        PalettePreference.Washi -> R.string.palette_washi
        PalettePreference.Sumi -> R.string.palette_sumi
        PalettePreference.Matcha -> R.string.palette_matcha
        PalettePreference.Sakura -> R.string.palette_sakura
        PalettePreference.Umi -> R.string.palette_umi
        PalettePreference.Momiji -> R.string.palette_momiji
        PalettePreference.Yoru -> R.string.palette_yoru
        PalettePreference.Dynamic -> R.string.palette_dynamic
    },
)

@Composable
private fun paletteDescription(value: PalettePreference): String = stringResource(
    when (value) {
        PalettePreference.Washi -> R.string.palette_washi_desc
        PalettePreference.Sumi -> R.string.palette_sumi_desc
        PalettePreference.Matcha -> R.string.palette_matcha_desc
        PalettePreference.Sakura -> R.string.palette_sakura_desc
        PalettePreference.Umi -> R.string.palette_umi_desc
        PalettePreference.Momiji -> R.string.palette_momiji_desc
        PalettePreference.Yoru -> R.string.palette_yoru_desc
        PalettePreference.Dynamic -> R.string.palette_dynamic_desc
    },
)

/**
 * Palette picker: a card per palette showing its light and dark side as a tiny Today card.
 * A tap applies the palette at once, so the dialog itself recolours as you choose.
 */
@Composable
internal fun PalettePickerDialog(
    selected: PalettePreference,
    onSelect: (PalettePreference) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.palette_label)) },
        text = { PalettePickerGrid(selected, onSelect) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
    )
}

/** The two-column grid of palette cards, shown inside the picker dialog. */
@Composable
internal fun PalettePickerGrid(
    selected: PalettePreference,
    onSelect: (PalettePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val choices = PalettePickerOrder.filter { it != PalettePreference.Dynamic || dynamicAvailable }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        items(choices, key = { it.name }) { palette ->
            val light: ColorScheme
            val dark: ColorScheme
            if (palette == PalettePreference.Dynamic && dynamicAvailable) {
                light = dynamicLightColorScheme(context)
                dark = dynamicDarkColorScheme(context)
            } else {
                light = paletteScheme(palette, dark = false)
                dark = paletteScheme(palette, dark = true)
            }
            PaletteCard(
                name = paletteName(palette),
                description = paletteDescription(palette),
                light = light,
                dark = dark,
                selected = palette == selected,
                onClick = { onSelect(palette) },
            )
        }
    }
}

@Composable
private fun PaletteCard(
    name: String,
    description: String,
    light: ColorScheme,
    dark: ColorScheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(14.dp))) {
                MiniDay(light, Modifier.weight(1f))
                MiniDay(dark, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                if (selected) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** One side of a palette card: an ensō in the palette's ink, a progress bar and a gold dot. */
@Composable
private fun MiniDay(scheme: ColorScheme, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(72.dp)
            .background(scheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(30.dp)) {
                val stroke = 4.dp.toPx()
                val inset = stroke
                val arc = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(scheme.surfaceVariant, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke * 0.6f))
                drawArc(scheme.primary, -100f, 290f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(26.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(scheme.primary))
                Box(Modifier.width(12.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(scheme.surfaceVariant))
                Spacer(Modifier.width(4.dp))
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(scheme.tertiary))
            }
        }
    }
}
