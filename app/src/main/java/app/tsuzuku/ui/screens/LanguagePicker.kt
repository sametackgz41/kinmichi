package app.tsuzuku.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.data.preferences.LanguagePreference

/** Flag and the language's own name, which stay the same whatever language the app is in. */
internal data class LanguageInfo(val preference: LanguagePreference, val flag: String, val nativeName: String)

internal val LanguageChoices = listOf(
    LanguageInfo(LanguagePreference.Turkish, "🇹🇷", "Türkçe"),
    LanguageInfo(LanguagePreference.English, "🇬🇧", "English"),
    LanguageInfo(LanguagePreference.Spanish, "🇪🇸", "Español"),
    LanguageInfo(LanguagePreference.German, "🇩🇪", "Deutsch"),
    LanguageInfo(LanguagePreference.French, "🇫🇷", "Français"),
    LanguageInfo(LanguagePreference.Italian, "🇮🇹", "Italiano"),
    LanguageInfo(LanguagePreference.Portuguese, "🇧🇷", "Português (Brasil)"),
    LanguageInfo(LanguagePreference.Russian, "🇷🇺", "Русский"),
    LanguageInfo(LanguagePreference.Japanese, "🇯🇵", "日本語"),
    LanguageInfo(LanguagePreference.Korean, "🇰🇷", "한국어"),
)

/** Three layouts for the language page, drawn side by side in previews. */
internal enum class LanguageLayout { List, Grid, Search }

@Composable
internal fun LanguageScreen(
    selected: LanguagePreference,
    onBack: () -> Unit,
    onSelect: (LanguagePreference) -> Unit,
    layout: LanguageLayout = LanguageLayout.Search,
) {
    var query by rememberSaveable { mutableStateOf("") }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                Text(stringResource(R.string.language_label), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
            }
            when (layout) {
                LanguageLayout.List -> {
                    SystemLanguageRow(selected == LanguagePreference.System) { onSelect(LanguagePreference.System) }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
                        LanguageChoices.forEachIndexed { index, info ->
                            if (index > 0) androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            LanguageRow(info, info.preference == selected) { onSelect(info.preference) }
                        }
                    }
                }
                LanguageLayout.Grid -> {
                    SystemLanguageRow(selected == LanguagePreference.System) { onSelect(LanguagePreference.System) }
                    LanguageChoices.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { info ->
                                LanguageTile(info, info.preference == selected, Modifier.weight(1f)) { onSelect(info.preference) }
                            }
                        }
                    }
                }
                LanguageLayout.Search -> {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        placeholder = { Text(stringResource(R.string.language_search)) },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val current = LanguageChoices.firstOrNull { it.preference == selected }
                    SectionTitle(stringResource(R.string.language_current))
                    if (current == null) {
                        SystemLanguageRow(true) { onSelect(LanguagePreference.System) }
                    } else {
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.secondaryContainer)) {
                            LanguageRow(current, true) {}
                        }
                    }
                    SectionTitle(stringResource(R.string.language_all))
                    val names = LanguageChoices.associateWith { localizedLanguageName(it.preference) }
                    val visible = LanguageChoices.filter {
                        query.isBlank() || it.nativeName.contains(query, true) || names.getValue(it).contains(query, true)
                    }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
                        if (current != null) {
                            LanguageRow(null, false) { onSelect(LanguagePreference.System) }
                            androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        visible.forEachIndexed { index, info ->
                            if (index > 0) androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            LanguageRow(info, info.preference == selected) { onSelect(info.preference) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
}

@Composable
private fun FlagDisc(flag: String?, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (flag == null) {
            Icon(Icons.Rounded.PhoneAndroid, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size((size * 0.5f).dp))
        } else {
            Text(flag, fontSize = (size * 0.55f).sp, textAlign = TextAlign.Center)
        }
    }
}

/** One language: flag, its own name, and its name in the app's current language. */
@Composable
private fun LanguageRow(info: LanguageInfo?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlagDisc(info?.flag)
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(info?.nativeName ?: stringResource(R.string.language_system), style = MaterialTheme.typography.bodyLarge)
            Text(
                if (info == null) stringResource(R.string.language_system_desc) else localizedLanguageName(info.preference),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SystemLanguageRow(selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            FlagDisc(null)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.language_system), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.language_system_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** A square tile with a big flag. */
@Composable
private fun LanguageTile(info: LanguageInfo, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Box {
            Column(Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(info.flag, fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text(info.nativeName, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 1)
                Text(localizedLanguageName(info.preference), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (selected) {
                Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(20.dp))
            }
        }
    }
}


/** The language's name in the app's current language ("Japonca" while the app is in Turkish). */
@Composable
internal fun localizedLanguageName(value: LanguagePreference): String = stringResource(
    when (value) {
        LanguagePreference.System -> R.string.language_system
        LanguagePreference.Turkish -> R.string.lang_name_tr
        LanguagePreference.English -> R.string.lang_name_en
        LanguagePreference.Spanish -> R.string.lang_name_es
        LanguagePreference.German -> R.string.lang_name_de
        LanguagePreference.French -> R.string.lang_name_fr
        LanguagePreference.Italian -> R.string.lang_name_it
        LanguagePreference.Portuguese -> R.string.lang_name_pt
        LanguagePreference.Russian -> R.string.lang_name_ru
        LanguagePreference.Japanese -> R.string.lang_name_ja
        LanguagePreference.Korean -> R.string.lang_name_ko
    },
)
