package app.tsuzuku.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.BugReport
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
private const val DIALOG_LANGUAGE = "language"
private const val DIALOG_WEEK_START = "week_start"

@Composable
fun ProfileScreen(
    settings: AppSettings,
    cloudAccount: CloudAccountState,
    onSetProfileName: (String) -> Unit,
    onSetTheme: (ThemePreference) -> Unit,
    onSetLanguage: (LanguagePreference) -> Unit,
    onSetWeekStart: (WeekStartPreference) -> Unit,
    onSetShowDailySummary: (Boolean) -> Unit,
    onSetProtectScreen: (Boolean) -> Unit,
    onOpenCloudAccount: () -> Unit,
    onOpenPlanning: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenRecords: () -> Unit = {},
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
        item {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(62.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.padding(16.dp))
                    }
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
                        )
                    }
                    if (cloudAccount !is CloudAccountState.SignedIn) {
                        IconButton(onClick = {
                            nameDraft = settings.profileName
                            activeDialog = DIALOG_NAME
                        }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_profile_name))
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        item { SectionLabel(stringResource(R.string.account_label)) }
        item {
            SettingsCard {
                ValueSettingRow(
                    icon = Icons.Rounded.Sync,
                    title = stringResource(R.string.google_account_label),
                    value = when (cloudAccount) {
                        CloudAccountState.Unavailable -> stringResource(R.string.firebase_setup_required)
                        CloudAccountState.SignedOut -> stringResource(R.string.google_not_connected)
                        is CloudAccountState.SignedIn -> cloudAccount.email.ifBlank { cloudAccount.displayName }
                    },
                    onClick = onOpenCloudAccount,
                )
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
                ValueSettingRow(
                    icon = Icons.Rounded.Insights,
                    title = stringResource(R.string.planning_and_review),
                    value = stringResource(R.string.planning_and_review_description),
                    onClick = onOpenPlanning,
                )
                ValueSettingRow(
                    icon = Icons.Rounded.Star,
                    title = stringResource(R.string.personal_records_milestones),
                    value = stringResource(R.string.personal_records_desc),
                    onClick = onOpenRecords,
                )
            }
        }
        item { SectionLabel(stringResource(R.string.appearance_label)) }
        item {
            SettingsCard {
                ValueSettingRow(
                    Icons.Rounded.DarkMode,
                    stringResource(R.string.theme_mode_label),
                    themeLabel(settings.theme),
                    onClick = { activeDialog = DIALOG_THEME },
                )
                ValueSettingRow(
                    Icons.Rounded.Language,
                    stringResource(R.string.language_label),
                    languageLabel(settings.language),
                    onClick = { activeDialog = DIALOG_LANGUAGE },
                )
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
            }
        }
        item { SectionLabel(stringResource(R.string.data_privacy_label)) }
        item {
            SettingsCard {
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
                ValueSettingRow(
                    Icons.AutoMirrored.Rounded.Label,
                    stringResource(R.string.categories_and_tags),
                    stringResource(R.string.categories_description),
                    onClick = onOpenCategories,
                )
                ValueSettingRow(
                    Icons.Rounded.Widgets,
                    stringResource(R.string.home_widget),
                    stringResource(R.string.home_widget_description),
                )
            }
        }
        item {
            Text(
                stringResource(
                    if (cloudAccount is CloudAccountState.SignedIn) R.string.cloud_data_note else R.string.local_data_note,
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) { content() }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(LocalConfiguration.current.locales[0]),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
    )
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
    Column(modifier) {
        Row(
            Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingIcon(icon)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) Text("→", color = MaterialTheme.colorScheme.secondary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
    Column(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingIcon(icon)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = null)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}


@Composable
private fun SettingIcon(icon: ImageVector) {
    Icon(
        icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(22.dp),
    )
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
private fun languageLabel(value: LanguagePreference): String = stringResource(
    when (value) {
        LanguagePreference.System -> R.string.language_system
        LanguagePreference.Turkish -> R.string.language_turkish
        LanguagePreference.English -> R.string.language_english
    },
)

@Composable
private fun weekStartLabel(value: WeekStartPreference): String = stringResource(
    when (value) {
        WeekStartPreference.Monday -> R.string.monday
        WeekStartPreference.Sunday -> R.string.sunday
    },
)
