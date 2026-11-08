package app.tsuzuku.ui.screens

import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.data.preferences.AppSettings
import app.tsuzuku.data.CloudAccountState
import app.tsuzuku.data.preferences.LanguagePreference
import app.tsuzuku.data.preferences.ThemePreference
import app.tsuzuku.data.preferences.WeekStartPreference
import app.tsuzuku.notifications.HabitNotifications

private const val DIALOG_NAME = "name"
private const val DIALOG_THEME = "theme"
private const val DIALOG_PALETTE = "palette"
private const val DIALOG_LANGUAGE = "language"
private const val DIALOG_WEEK_START = "week_start"

@Composable
fun ProfileScreen(
    settings: AppSettings,
    cloudAccount: CloudAccountState,
    onSetProfileName: (String) -> Unit,
    onSetTheme: (ThemePreference) -> Unit,
    onSetPalette: (app.tsuzuku.data.preferences.PalettePreference) -> Unit = {},
    onSetPureBlack: (Boolean) -> Unit = {},
    onSetWidgetOpacity: (Int) -> Unit = {},
    onSetLanguage: (LanguagePreference) -> Unit,
    onSetWeekStart: (WeekStartPreference) -> Unit,
    onSetShowDailySummary: (Boolean) -> Unit,
    onSetProtectScreen: (Boolean) -> Unit,
    onOpenCloudAccount: () -> Unit,
    onOpenProfile: (() -> Unit)? = null,
    profileStats: ProfileStats? = null,
    onOpenAppearance: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenWidgets: () -> Unit = {},
    layout: SettingsLayout = SettingsLayout.Tiles,
    home: SettingsHome = SettingsHome.Groups,
    onOpenPlanning: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenRecords: () -> Unit = {},
    onOpenArchive: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val testBody = stringResource(R.string.test_notification_body)
    val postTestNotification = {
        HabitNotifications.show(context, 9_000_000L, appName, testBody, repeatMinutes = 10)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) postTestNotification() }
    var activeDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var nameDraft by rememberSaveable(settings.profileName) { mutableStateOf(settings.profileName) }

    // The groups of rows, shared by the long list and by the tile home's pages.
    val notificationRows: @Composable () -> Unit = {
        ValueSettingRow(
            icon = Icons.Rounded.Notifications,
            title = stringResource(R.string.test_notification),
            value = stringResource(R.string.test_notification_description),
            onClick = {
                if (
                    Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    postTestNotification()
                }
            },
        )
        var askFeelingOn by remember { mutableStateOf(FeelingPromptSetting.isEnabled(context)) }
        SwitchSettingRow(
            Icons.Rounded.Mood,
            stringResource(R.string.feeling_setting),
            stringResource(R.string.feeling_setting_description),
            askFeelingOn,
            onCheckedChange = {
                askFeelingOn = it
                FeelingPromptSetting.setEnabled(context, it)
            },
        )
        var moodReminderOn by remember { mutableStateOf(app.tsuzuku.notifications.MoodReminder.isEnabled(context)) }
        SwitchSettingRow(
            Icons.Rounded.Notifications,
            stringResource(R.string.mood_reminder_setting),
            stringResource(R.string.mood_reminder_setting_description),
            moodReminderOn,
            onCheckedChange = {
                moodReminderOn = it
                app.tsuzuku.notifications.MoodReminder.setEnabled(context, it)
            },
        )
        var streakGuardOn by remember { mutableStateOf(app.tsuzuku.notifications.StreakGuard.isEnabled(context)) }
        SwitchSettingRow(
            Icons.Rounded.Notifications,
            stringResource(R.string.streak_guard_setting),
            stringResource(R.string.streak_guard_setting_description),
            streakGuardOn,
            onCheckedChange = {
                streakGuardOn = it
                app.tsuzuku.notifications.StreakGuard.setEnabled(context, it)
            },
        )
        var weeklySummaryOn by remember { mutableStateOf(app.tsuzuku.notifications.WeeklySummary.isEnabled(context)) }
        SwitchSettingRow(
            Icons.Rounded.Notifications,
            stringResource(R.string.weekly_summary_setting),
            stringResource(R.string.weekly_summary_setting_description),
            checked = weeklySummaryOn,
            onCheckedChange = {
                weeklySummaryOn = it
                app.tsuzuku.notifications.WeeklySummary.setEnabled(context, it)
            },
        )
    }
    val generalRows: @Composable () -> Unit = {
        ValueSettingRow(
            Icons.Rounded.CalendarMonth,
            stringResource(R.string.week_start_label),
            weekStartLabel(settings.weekStart),
            onClick = { activeDialog = DIALOG_WEEK_START },
        )
        SwitchSettingRow(
            Icons.Rounded.Summarize,
            stringResource(R.string.daily_summary_setting),
            stringResource(R.string.daily_summary_setting_description),
            checked = settings.showDailySummary,
            onCheckedChange = onSetShowDailySummary,
        )
            WidgetOpacityRow(settings.widgetOpacity, onSetWidgetOpacity)
        HealthConnectRow()
        ValueSettingRow(
            Icons.AutoMirrored.Rounded.Label,
            stringResource(R.string.categories_and_tags),
            stringResource(R.string.categories_description),
            onClick = onOpenCategories,
        )
    }
    val archiveRow: @Composable () -> Unit = {
        ValueSettingRow(
            Icons.Rounded.Inventory2,
            stringResource(R.string.archive_title),
            stringResource(R.string.archive_row_description),
            onClick = onOpenArchive,
        )
    }
    val privacyRows: @Composable () -> Unit = {
        SwitchSettingRow(
            Icons.Rounded.Lock,
            stringResource(R.string.screen_protection),
            stringResource(R.string.screen_protection_description),
            checked = settings.protectScreen,
            onCheckedChange = onSetProtectScreen,
        )
        ValueSettingRow(
            Icons.Rounded.Lock,
            stringResource(R.string.app_lock),
            stringResource(if (settings.appLockEnabled) R.string.app_lock_status_on else R.string.app_lock_status_off),
            onClick = onOpenSecurity,
        )
        ValueSettingRow(
            Icons.Rounded.Backup,
            stringResource(R.string.backup_restore),
            stringResource(R.string.local_backup_available),
            onClick = onOpenBackup,
        )
        ValueSettingRow(
            Icons.Rounded.BugReport,
            stringResource(R.string.error_reports),
            stringResource(R.string.error_reports_description),
            onClick = onOpenDiagnostics,
        )
    }
    val aboutRows: @Composable () -> Unit = {
        ValueSettingRow(
            Icons.Rounded.Code,
            stringResource(R.string.github_project),
            stringResource(R.string.github_project_description, app.tsuzuku.BuildConfig.VERSION_NAME),
            onClick = {
                runCatching {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(app.tsuzuku.update.GITHUB_URL)))
                }
            },
        )
        var updateCheckOn by remember { mutableStateOf(app.tsuzuku.update.UpdateChecker.isEnabled(context)) }
        SwitchSettingRow(
            Icons.Rounded.SystemUpdate,
            stringResource(R.string.update_check_setting),
            stringResource(R.string.update_check_setting_description),
            updateCheckOn,
            onCheckedChange = {
                updateCheckOn = it
                app.tsuzuku.update.UpdateChecker.setEnabled(context, it)
            },
        )
        ValueSettingRow(
            Icons.Rounded.AutoAwesome,
            stringResource(R.string.replay_intro),
            stringResource(R.string.replay_intro_description),
            onClick = { app.tsuzuku.ui.IntroReplay.start() },
        )
    }
    val profileCard: @Composable () -> Unit = {
        Column(
            (if (layout == SettingsLayout.Minimal) Modifier else Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)))
                .then(if (onOpenProfile != null) Modifier.clickable(onClick = onOpenProfile) else Modifier)
                .then(if (layout == SettingsLayout.Minimal) Modifier else Modifier.padding(horizontal = 16.dp)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProfileAvatar(
                    when (cloudAccount) {
                        is CloudAccountState.SignedIn -> cloudAccount.displayName.ifBlank { settings.profileName }
                        else -> settings.profileName
                    },
                    58,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        when (cloudAccount) {
                            is CloudAccountState.SignedIn -> cloudAccount.displayName.ifBlank { settings.profileName }
                            else -> settings.profileName
                        }.ifBlank { stringResource(R.string.personal_profile) },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        when (cloudAccount) {
                            is CloudAccountState.SignedIn -> cloudAccount.email
                            else -> stringResource(R.string.local_profile_description)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    profileStats?.let { st ->
                        Text(
                            stringResource(R.string.profile_card_stats, st.streak, st.comebacks, st.days),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF8A6A1F),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                if (onOpenProfile != null) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.profile_title))
                } else if (cloudAccount !is CloudAccountState.SignedIn) {
                    IconButton(onClick = {
                        nameDraft = settings.profileName
                        activeDialog = DIALOG_NAME
                    }) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_profile_name))
                    }
                }
            }
            if (layout == SettingsLayout.Minimal) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
    val dataNote: @Composable () -> Unit = {
        Text(
            stringResource(
                if (cloudAccount is CloudAccountState.SignedIn) R.string.cloud_data_note else R.string.local_data_note,
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val accountStatus = when (cloudAccount) {
        CloudAccountState.Unavailable -> stringResource(R.string.firebase_setup_required)
        CloudAccountState.SignedOut -> stringResource(R.string.google_not_connected)
        is CloudAccountState.SignedIn -> cloudAccount.email.ifBlank { cloudAccount.displayName }
    }
    // Tile home: which page is open inside settings (null = the tiles).
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    androidx.activity.compose.BackHandler(enabled = page != null) { page = null }
    val categories = listOf(
        SettingsCategory(Icons.Rounded.Sync, stringResource(R.string.account_title), accountStatus, onOpenCloudAccount),
        SettingsCategory(Icons.Rounded.Palette, stringResource(R.string.appearance_theme_palette), "${themeLabel(settings.theme)} · ${paletteName(settings.palette)}", onOpenAppearance),
        SettingsCategory(Icons.Rounded.Language, stringResource(R.string.language_label), languageLabel(settings.language), onOpenLanguage),
        SettingsCategory(Icons.Rounded.Widgets, stringResource(R.string.widgets_sheet_title), null, onOpenWidgets),
        SettingsCategory(Icons.Rounded.Notifications, stringResource(R.string.notifications_label), null) { page = SettingsPage.Notifications },
        SettingsCategory(Icons.Rounded.Tune, stringResource(R.string.settings_general), weekStartLabel(settings.weekStart)) { page = SettingsPage.General },
        SettingsCategory(Icons.Rounded.Insights, stringResource(R.string.planning_and_review), null, onOpenPlanning),
        SettingsCategory(Icons.Rounded.Star, stringResource(R.string.settings_records_short), null, onOpenRecords),
        SettingsCategory(Icons.Rounded.Lock, stringResource(R.string.data_privacy_label), stringResource(if (settings.appLockEnabled) R.string.app_lock_status_on else R.string.app_lock_status_off)) { page = SettingsPage.Privacy },
        SettingsCategory(Icons.Rounded.Info, stringResource(R.string.settings_about), stringResource(R.string.settings_version, app.tsuzuku.BuildConfig.VERSION_NAME)) { page = SettingsPage.About },
    )

    val tiles = layout == SettingsLayout.Tiles
    androidx.compose.runtime.CompositionLocalProvider(LocalSettingsLayout provides if (tiles) SettingsLayout.Cards else layout) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val open = page
        if (tiles && open != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { page = null }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(
                        stringResource(
                            when (open) {
                                SettingsPage.Notifications -> R.string.notifications_label
                                SettingsPage.General -> R.string.settings_general
                                SettingsPage.Privacy -> R.string.data_privacy_label
                                SettingsPage.About -> R.string.settings_about
                            },
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            item {
                SettingsCard {
                    when (open) {
                        SettingsPage.Notifications -> notificationRows()
                        SettingsPage.General -> { generalRows(); archiveRow() }
                        SettingsPage.Privacy -> privacyRows()
                        SettingsPage.About -> aboutRows()
                    }
                }
            }
            if (open == SettingsPage.Privacy || open == SettingsPage.About) item { dataNote() }
            return@LazyColumn
        }
        item {
            if (onBack != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(stringResource(R.string.settings_label), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        item { profileCard() }
        if (tiles) {
            item { SettingsHomeTiles(categories, home) }
            return@LazyColumn
        }
        item { SectionLabel(stringResource(R.string.account_label)) }
        item {
            SettingsCard {
                ValueSettingRow(
                    icon = Icons.Rounded.Sync,
                    title = stringResource(R.string.account_title),
                    value = accountStatus,
                    onClick = onOpenCloudAccount,
                )
                ValueSettingRow(Icons.Rounded.Insights, stringResource(R.string.planning_and_review), stringResource(R.string.planning_and_review_description), onClick = onOpenPlanning)
                ValueSettingRow(Icons.Rounded.Star, stringResource(R.string.personal_records_milestones), stringResource(R.string.personal_records_desc), onClick = onOpenRecords)
            }
        }
        item { SectionLabel(stringResource(R.string.appearance_label)) }
        item {
            SettingsCard {
                ValueSettingRow(Icons.Rounded.Palette, stringResource(R.string.appearance_theme_palette), "${themeLabel(settings.theme)} · ${paletteName(settings.palette)}", onClick = onOpenAppearance)
                ValueSettingRow(Icons.Rounded.Language, stringResource(R.string.language_label), languageLabel(settings.language), onClick = onOpenLanguage)
                ValueSettingRow(Icons.Rounded.Widgets, stringResource(R.string.home_widget), stringResource(R.string.home_widget_description), onClick = onOpenWidgets)
                generalRows()
            }
        }
        item { SectionLabel(stringResource(R.string.notifications_label)) }
        item { SettingsCard { notificationRows() } }
        item { SectionLabel(stringResource(R.string.data_privacy_label)) }
        item { SettingsCard { privacyRows() } }
        item { SectionLabel(stringResource(R.string.settings_about)) }
        item { SettingsCard { aboutRows() } }
        item { dataNote() }
    }
    }

    when (activeDialog) {
        DIALOG_NAME -> AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text(stringResource(R.string.profile_name)) },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.profile_name)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetProfileName(nameDraft)
                    activeDialog = null
                }) { Text(stringResource(R.string.save_label)) }
            },
            dismissButton = { TextButton(onClick = { activeDialog = null }) { Text(stringResource(R.string.cancel)) } },
        )
        DIALOG_THEME -> ChoiceDialog(
            title = stringResource(R.string.theme_label),
            choices = ThemePreference.entries,
            selected = settings.theme,
            label = { themeLabel(it) },
            onSelect = {
                onSetTheme(it)
                activeDialog = null
            },
            onDismiss = { activeDialog = null },
        )
        DIALOG_PALETTE -> PalettePickerDialog(
            selected = settings.palette,
            onSelect = onSetPalette,
            onDismiss = { activeDialog = null },
        )
        DIALOG_LANGUAGE -> ChoiceDialog(
            title = stringResource(R.string.language_label),
            choices = LanguagePreference.entries,
            selected = settings.language,
            label = { languageLabel(it) },
            onSelect = {
                onSetLanguage(it)
                activeDialog = null
            },
            onDismiss = { activeDialog = null },
        )
        DIALOG_WEEK_START -> ChoiceDialog(
            title = stringResource(R.string.week_start_label),
            choices = WeekStartPreference.entries,
            selected = settings.weekStart,
            label = { weekStartLabel(it) },
            onSelect = {
                onSetWeekStart(it)
                activeDialog = null
            },
            onDismiss = { activeDialog = null },
        )
    }
}

/** Three looks for the settings page, drawn side by side in previews. */
enum class SettingsLayout { Minimal, Cards, Tiles }

private val LocalSettingsLayout = androidx.compose.runtime.staticCompositionLocalOf { SettingsLayout.Cards }

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    if (LocalSettingsLayout.current == SettingsLayout.Minimal) {
        Column(Modifier.fillMaxWidth()) { content() }
    } else {
        Column(
            Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) { content() }
    }
}

@Composable
private fun SectionLabel(text: String) {
    if (LocalSettingsLayout.current == SettingsLayout.Minimal) {
        Text(
            text.uppercase(LocalConfiguration.current.locales[0]),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
        )
    } else {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, top = 10.dp),
        )
    }
}

internal enum class SettingsPage { Notifications, General, Privacy, About }

/** Looks for the tile home, drawn side by side in previews. */
enum class SettingsHome { Grid, Bento, Groups }

private class SettingsCategory(val icon: ImageVector, val title: String, val status: String?, val onClick: () -> Unit)

/** Every settings group as a tile; Account first. */
@Composable
private fun SettingsHomeTiles(categories: List<SettingsCategory>, home: SettingsHome) {
    when (home) {
        SettingsHome.Grid -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                    pair.forEachIndexed { index, c -> SquareTile(c, accent = 0, Modifier.weight(1f).fillMaxHeight()) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        SettingsHome.Bento -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Wide, two small, two small, wide, ... keeps the page lively without getting long.
            var i = 0
            var row = 0
            while (i < categories.size) {
                val wide = row % 3 == 0
                if (wide) {
                    WideTile(categories[i], accent = row % 3, Modifier.fillMaxWidth())
                    i += 1
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                        categories.drop(i).take(2).forEachIndexed { k, c -> SquareTile(c, accent = (row + k) % 3, Modifier.weight(1f).fillMaxHeight()) }
                        if (categories.size - i == 1) Spacer(Modifier.weight(1f))
                    }
                    i += 2
                }
                row++
            }
        }
        SettingsHome.Groups -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val groups = listOf(
                R.string.account_label to listOf(0, 4, 6, 7),
                R.string.appearance_label to listOf(1, 2, 3, 5),
                R.string.settings_group_more to listOf(8, 9),
            )
            groups.forEach { (label, indices) ->
                SectionLabel(stringResource(label))
                indices.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                        pair.forEach { CompactTile(categories[it], Modifier.weight(1f).fillMaxHeight()) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun accentColors(accent: Int): Pair<Color, Color> = when (accent) {
    1 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    2 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
}

@Composable
private fun TileIcon(icon: ImageVector, accent: Int, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val (bg, fg) = accentColors(accent)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(bg),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = fg, modifier = Modifier.size(size * 0.55f)) }
}

@Composable
private fun SquareTile(c: SettingsCategory, accent: Int, modifier: Modifier) {
    Surface(onClick = c.onClick, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            TileIcon(c.icon, accent)
            Spacer(Modifier.height(12.dp))
            Text(c.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            if (c.status != null) {
                Text(
                    c.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WideTile(c: SettingsCategory, accent: Int, modifier: Modifier) {
    val (bg, _) = accentColors(accent)
    Surface(onClick = c.onClick, shape = RoundedCornerShape(24.dp), color = bg.copy(alpha = 0.45f), modifier = modifier) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TileIcon(c.icon, accent, 48.dp)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(c.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                if (c.status != null) {
                    Text(c.status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompactTile(c: SettingsCategory, modifier: Modifier) {
    Surface(onClick = c.onClick, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            TileIcon(c.icon, 0, 34.dp)
            Text(c.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

@Composable
private fun ValueSettingRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    val modifier = if (onClick == null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    }
    val carded = LocalSettingsLayout.current != SettingsLayout.Minimal
    Column(modifier) {
        Row(
            Modifier.padding(vertical = 14.dp, horizontal = if (carded) 16.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingIcon(icon)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(title, style = if (carded) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(Modifier.padding(start = if (carded) 66.dp else 0.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SwitchSettingRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val carded = LocalSettingsLayout.current != SettingsLayout.Minimal
    Column(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = if (carded) 16.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingIcon(icon)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(title, style = if (carded) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = null)
        }
        HorizontalDivider(Modifier.padding(start = if (carded) 66.dp else 0.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}


@Composable
private fun SettingIcon(icon: ImageVector) {
    if (LocalSettingsLayout.current == SettingsLayout.Minimal) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
    } else {
        Box(
            Modifier.size(36.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    choices: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                choices.forEach { choice ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(choice) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = choice == selected, onClick = { onSelect(choice) })
                        Text(label(choice), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun themeLabel(value: ThemePreference): String = stringResource(
    when (value) {
        ThemePreference.System -> R.string.theme_system
        ThemePreference.Light -> R.string.theme_light
        ThemePreference.Dark -> R.string.theme_dark
    },
)


@Composable
internal fun languageLabel(value: LanguagePreference): String = stringResource(
    when (value) {
        LanguagePreference.System -> R.string.language_system
        LanguagePreference.Turkish -> R.string.language_turkish
        LanguagePreference.English -> R.string.language_english
        LanguagePreference.Spanish -> R.string.language_spanish
        LanguagePreference.German -> R.string.language_german
        LanguagePreference.Portuguese -> R.string.language_portuguese
        LanguagePreference.Japanese -> R.string.language_japanese
        LanguagePreference.French -> R.string.language_french
        LanguagePreference.Italian -> R.string.language_italian
        LanguagePreference.Russian -> R.string.language_russian
        LanguagePreference.Korean -> R.string.language_korean
    },
)

@Composable
private fun weekStartLabel(value: WeekStartPreference): String = stringResource(
    when (value) {
        WeekStartPreference.Monday -> R.string.monday
        WeekStartPreference.Sunday -> R.string.sunday
    },
)

/** Home screen widget see-through level, applied when the slider is released. */
@Composable
private fun WidgetOpacityRow(value: Int, onChange: (Int) -> Unit) {
    var draft by remember(value) { mutableStateOf(value.toFloat()) }
    val carded = LocalSettingsLayout.current != SettingsLayout.Minimal
    Column(Modifier.fillMaxWidth().padding(horizontal = if (carded) 16.dp else 0.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingIcon(Icons.Rounded.Opacity)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.widget_opacity_label), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.widget_opacity_value, draft.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        androidx.compose.material3.Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { onChange(draft.toInt()) },
            valueRange = 20f..100f,
            steps = 15,
            modifier = Modifier.padding(start = 50.dp),
        )
    }
}

/** Health Connect: whether it can be used here, and one tap to install it or manage its permissions. */
@Composable
private fun HealthConnectRow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var status by remember { mutableStateOf(app.tsuzuku.health.HealthSync.status(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        status = app.tsuzuku.health.HealthSync.status(context)
        onPauseOrDispose { }
    }
    if (status == app.tsuzuku.health.HealthSync.Status.Unsupported) return
    ValueSettingRow(
        Icons.Rounded.FavoriteBorder,
        "Health Connect",
        stringResource(
            if (status == app.tsuzuku.health.HealthSync.Status.Ready) R.string.health_settings_ready else R.string.health_settings_install,
        ),
        onClick = { app.tsuzuku.health.HealthSync.open(context) },
    )
}
