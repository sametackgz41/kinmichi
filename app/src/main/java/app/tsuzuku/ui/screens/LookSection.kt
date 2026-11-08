package app.tsuzuku.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.tsuzuku.R
import app.tsuzuku.ui.theme.AppIcon
import app.tsuzuku.ui.theme.LookPrefs

/** Extra look options under the palettes: seasonal touches, text size, contrast, home-screen icon. */
@Composable
internal fun LookSection() {
    val context = LocalContext.current
    LookPrefs.load(context)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.look_more_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text(stringResource(R.string.look_text_size), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    listOf(R.string.look_text_normal, R.string.look_text_large, R.string.look_text_larger).forEachIndexed { i, label ->
                        val scale = LookPrefs.TextScales[i]
                        FilterChip(selected = LookPrefs.textScale == scale, onClick = { LookPrefs.setTextScale(context, scale) }, label = { Text(stringResource(label)) })
                    }
                }
            }
            SwitchLine(stringResource(R.string.look_contrast), stringResource(R.string.look_contrast_desc), LookPrefs.highContrast) { LookPrefs.setHighContrast(context, it) }
        }
        Text(stringResource(R.string.look_icon_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        var icon by remember { mutableStateOf(runCatching { AppIcon.current(context) }.getOrDefault(AppIcon.Path)) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                Triple(AppIcon.Path, R.mipmap.ic_launcher, R.string.look_icon_path),
                Triple(AppIcon.Enso, R.mipmap.ic_launcher_enso, R.string.look_icon_enso),
                Triple(AppIcon.Night, R.mipmap.ic_launcher_night, R.string.look_icon_night),
            ).forEach { (choice, res, label) ->
                val selected = icon == choice
                Surface(
                    onClick = {
                        if (!selected) {
                            runCatching { AppIcon.set(context, choice) }
                            icon = choice
                        }
                    },
                    shape = RoundedCornerShape(22.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.weight(1f).then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(22.dp)) else Modifier),
                ) {
                    Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val bitmap = remember(res) {
                            runCatching { androidx.core.content.ContextCompat.getDrawable(context, res)!!.toBitmap(160, 160).asImageBitmap() }.getOrNull()
                        }
                        if (bitmap != null) Image(bitmap, null, Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)))
                        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        Text(stringResource(R.string.look_icon_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun SwitchLine(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
