package app.tsuzuku.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.data.BackupRepository
import app.tsuzuku.data.BackupSummary
import java.time.LocalDate
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupScreen(
    repository: BackupRepository,
    onBack: () -> Unit,
    onRestored: suspend () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by rememberSaveable { mutableStateOf(false) }
    var messageRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingJson by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSummary by rememberSaveable { mutableStateOf<BackupSummary?>(null) }
    val exportCache = File(context.cacheDir, "pending-tsuzuku-backup.json")
    val csvCache = File(context.cacheDir, "pending-tsuzuku-report.csv")
    val pdfCache = File(context.cacheDir, "pending-tsuzuku-report.pdf")

    fun fail() {
        busy = false
        messageRes = R.string.backup_operation_failed
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri == null) {
            exportCache.delete()
            busy = false
            return@rememberLauncherForActivityResult
        }
        busy = true
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    require(exportCache.exists())
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output -> exportCache.inputStream().use { it.copyTo(output) } }
                        ?: error("Unable to open backup")
                }
            }.onSuccess {
                exportCache.delete()
                busy = false
                messageRes = R.string.backup_export_success
            }.onFailure { exportCache.delete(); fail() }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        val text = reader.readText()
                        require(text.length <= 10_000_000)
                        text
                    } ?: error("Unable to open backup")
                }
                val summary = repository.preview(json)
                json to summary
            }.onSuccess { (json, summary) ->
                busy = false
                pendingJson = json
                pendingSummary = summary
            }.onFailure { fail() }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri == null) { csvCache.delete(); busy = false; return@rememberLauncherForActivityResult }
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")?.use { out -> csvCache.inputStream().use { it.copyTo(out) } } ?: error("Unable to save CSV") } }
                .onSuccess { csvCache.delete(); busy = false; messageRes = R.string.report_export_success }
                .onFailure { csvCache.delete(); fail() }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        if (uri == null) { pdfCache.delete(); busy = false; return@rememberLauncherForActivityResult }
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "w")?.use { out -> pdfCache.inputStream().use { it.copyTo(out) } } ?: error("Unable to save PDF") } }
                .onSuccess { pdfCache.delete(); busy = false; messageRes = R.string.report_export_success }
                .onFailure { pdfCache.delete(); fail() }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader(stringResource(R.string.backup_restore), onBack) }
        item {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null)
                    Column {
                        Text(stringResource(R.string.local_backup_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.local_backup_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            BackupAction(
                icon = Icons.Rounded.Upload,
                title = stringResource(R.string.create_backup),
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        runCatching {
                            val json = repository.exportJson()
                            withContext(Dispatchers.IO) { exportCache.writeText(json) }
                        }.onSuccess {
                            exportLauncher.launch("Tsuzuku-${LocalDate.now()}.json")
                        }.onFailure { exportCache.delete(); fail() }
                    }
                },
            )
        }
        item {
            BackupAction(
                icon = Icons.Rounded.Download,
                title = stringResource(R.string.restore_backup),
                enabled = !busy,
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
            )
        }
        item {
            BackupAction(
                icon = Icons.Rounded.Upload,
                title = stringResource(R.string.export_csv),
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { csvCache.writeText(repository.exportCsv()) } }
                            .onSuccess { csvLauncher.launch("Tsuzuku-${LocalDate.now()}.csv") }
                            .onFailure { csvCache.delete(); fail() }
                    }
                },
            )
        }
        item {
            BackupAction(
                icon = Icons.Rounded.Upload,
                title = stringResource(R.string.export_pdf),
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { pdfCache.outputStream().use { repository.exportPdf(it) } } }
                            .onSuccess { pdfLauncher.launch("Tsuzuku-${LocalDate.now()}.pdf") }
                            .onFailure { pdfCache.delete(); fail() }
                    }
                },
            )
        }
        item {
            Text(
                stringResource(R.string.backup_security_note),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(6.dp),
            )
        }
        messageRes?.let { resource -> item { Text(stringResource(resource), color = MaterialTheme.colorScheme.primary) } }
    }

    if (pendingJson != null && pendingSummary != null) {
        val summary = pendingSummary!!
        AlertDialog(
            onDismissRequest = { pendingJson = null; pendingSummary = null },
            title = { Text(stringResource(R.string.confirm_restore)) },
            text = {
                Text(
                    stringResource(
                        R.string.restore_summary,
                        summary.habitCount,
                        summary.historyCount,
                        summary.contentCount,
                        summary.goalCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val json = pendingJson ?: return@TextButton
                    pendingJson = null
                    pendingSummary = null
                    busy = true
                    scope.launch {
                        runCatching { repository.restore(json) }.onSuccess {
                            busy = false
                            messageRes = R.string.backup_restore_success
                            onRestored()
                        }.onFailure { fail() }
                    }
                }) { Text(stringResource(R.string.restore_label), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingJson = null; pendingSummary = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun BackupAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
