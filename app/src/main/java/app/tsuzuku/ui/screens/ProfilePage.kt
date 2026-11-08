package app.tsuzuku.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.HabitHistorySnapshot
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The profile picture: an emoji kept on the device (empty = the name's first letter). */
object ProfilePrefs {
    private const val PREFS = "profile"
    var emoji by mutableStateOf("")
        private set
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        emoji = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("emoji", "").orEmpty()
    }

    fun setEmoji(context: Context, value: String) {
        emoji = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("emoji", value).apply()
    }
}

/** The three numbers on the profile: days on the path, the best current streak, comebacks. */
data class ProfileStats(val days: Int, val streak: Int, val comebacks: Int)

internal fun profileStats(history: HabitHistorySnapshot, today: LocalDate = LocalDate.now()): ProfileStats {
    val start = (history.habits.map { it.createdOn }.filter { it != LocalDate.MIN } + history.records.map { it.date }).minOrNull() ?: today
    return ProfileStats(
        days = (ChronoUnit.DAYS.between(start, today) + 1).toInt().coerceAtLeast(1),
        streak = history.habits.maxOfOrNull { history.streak(it, today).current } ?: 0,
        comebacks = history.comebackCount(start, today),
    )
}

/** Round picture: the chosen emoji, or the first letter of the name on the palette colour. */
@Composable
internal fun ProfileAvatar(name: String, size: Int, modifier: Modifier = Modifier) {
    val emoji = ProfilePrefs.emoji
    Box(
        modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji.isNotBlank()) {
            Text(emoji, fontSize = (size * 0.5).sp)
        } else {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "金",
                fontSize = (size * 0.42).sp,
                fontFamily = app.tsuzuku.ui.theme.SerifDisplay,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier, gold: Boolean = false) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp)) {
        Text(
            value,
            fontSize = 24.sp,
            fontFamily = app.tsuzuku.ui.theme.SerifDisplay,
            fontWeight = FontWeight.Bold,
            color = if (gold) Color(0xFF8A6A1F) else MaterialTheme.colorScheme.onSurface,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Who you are, three numbers, and the things you change about yourself. */
@Composable
internal fun ProfilePage(
    name: String,
    email: String?,
    provider: String?,
    stats: ProfileStats,
    lockSummary: String,
    accountSummary: String,
    onSetName: (String) -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenAccount: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var editName by remember { mutableStateOf(false) }
    var pickEmoji by remember { mutableStateOf(false) }
    val shownName = name.ifBlank { stringResource(R.string.personal_profile) }
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 4.dp, 18.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleLarge)
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.clickable { pickEmoji = true }) {
                    ProfileAvatar(name, 92)
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Edit, stringResource(R.string.profile_icon), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) }
                }
                Text(shownName, style = MaterialTheme.typography.headlineSmall, fontFamily = app.tsuzuku.ui.theme.SerifDisplay, modifier = Modifier.padding(top = 10.dp))
                Text(
                    if (email.isNullOrBlank()) stringResource(R.string.local_profile_description) else listOfNotNull(email, provider).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("${stats.days}", stringResource(R.string.profile_days), Modifier.weight(1f))
                StatBox("🔥 ${stats.streak}", stringResource(R.string.profile_streak), Modifier.weight(1f))
                StatBox("✨ ${stats.comebacks}", stringResource(R.string.profile_comebacks), Modifier.weight(1f), gold = true)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column {
                    ProfileRow(stringResource(R.string.profile_name), shownName) { editName = true }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ProfileRow(stringResource(R.string.profile_icon), ProfilePrefs.emoji.ifBlank { stringResource(R.string.profile_icon_letter) }) { pickEmoji = true }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ProfileRow(stringResource(R.string.profile_greeting), stringResource(R.string.profile_greeting_preview, shownName)) { editName = true }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ProfileRow(stringResource(R.string.app_lock), lockSummary, onOpenSecurity)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ProfileRow(stringResource(R.string.account_title), accountSummary, onOpenAccount)
                }
            }
        }
    }
    if (editName) {
        var draft by remember { mutableStateOf(name) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text(stringResource(R.string.profile_name)) },
            text = {
                OutlinedTextField(value = draft, onValueChange = { draft = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = { TextButton(onClick = { onSetName(draft.trim()); editName = false }) { Text(stringResource(R.string.save_label)) } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (pickEmoji) {
        EmojiPickerDialog(
            selected = ProfilePrefs.emoji,
            onPick = { ProfilePrefs.setEmoji(context, it); pickEmoji = false },
            onDismiss = { pickEmoji = false },
        )
    }
}
