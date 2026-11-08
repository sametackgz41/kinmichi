package app.tsuzuku.ui.screens

import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.Lock
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
    val sealedCache = File(context.cacheDir, "pending-tsuzuku-backup.kinmichi")
    var password by remember { mutableStateOf("") }
    var passwordAgain by remember { mutableStateOf("") }
    var sealedFile by remember { mutableStateOf<ByteArray?>(null) }
    var openPassword by remember { mutableStateOf("") }
    var wrongPassword by remember { mutableStateOf(false) }

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
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().also { require(it.size <= 10_000_000) }
                    } ?: error("Unable to open backup")
                }
                if (app.tsuzuku.data.BackupCrypto.isEncrypted(bytes)) {
                    bytes to null
                } else {
                    val json = bytes.toString(Charsets.UTF_8)
                    json.toByteArray() to (json to repository.preview(json))
                }
            }.onSuccess { (bytes, plain) ->
                busy = false
                if (plain == null) {
                    sealedFile = bytes
                    openPassword = ""
                    wrongPassword = false
                } else {
                    pendingJson = plain.first
                    pendingSummary = plain.second
                }
            }.onFailure { fail() }
        }
    }
    val sealedLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri == null) { sealedCache.delete(); busy = false; return@rememberLauncherForActivityResult }
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "w")?.use { out -> sealedCache.inputStream().use { it.copyTo(out) } } ?: error("Unable to save backup") } }
                .onSuccess { sealedCache.delete(); busy = false; password = ""; passwordAgain = ""; messageRes = R.string.backup_export_success }
                .onFailure { sealedCache.delete(); fail() }
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
                            exportLauncher.launch("Kinmichi-${LocalDate.now()}.json")
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
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) },
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
                            .onSuccess { csvLauncher.launch("Kinmichi-${LocalDate.now()}.csv") }
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
                            .onSuccess { pdfLauncher.launch("Kinmichi-${LocalDate.now()}.pdf") }
                            .onFailure { pdfCache.delete(); fail() }
                    }
                },
            )
        }
        item {
            SealedBackupCard(
                password = password,
                passwordAgain = passwordAgain,
                onPassword = { password = it },
                onPasswordAgain = { passwordAgain = it },
                enabled = !busy,
                onCreate = {
                    busy = true
                    val secret = password.toCharArray()
                    scope.launch {
                        runCatching {
                            val json = repository.exportJson()
                            withContext(Dispatchers.Default) { sealedCache.writeBytes(app.tsuzuku.data.BackupCrypto.encrypt(json, secret)) }
                        }.onSuccess {
                            secret.fill(' ')
                            sealedLauncher.launch("Kinmichi-${LocalDate.now()}.kinmichi")
                        }.onFailure { secret.fill(' '); sealedCache.delete(); fail() }
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

    sealedFile?.let { bytes ->
        AlertDialog(
            onDismissRequest = { sealedFile = null; openPassword = "" },
            title = { Text(stringResource(R.string.sealed_open_title)) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = openPassword,
                    onValueChange = { openPassword = it; wrongPassword = false },
                    label = { Text(stringResource(R.string.sealed_password)) },
                    singleLine = true,
                    isError = wrongPassword,
                    supportingText = if (wrongPassword) ({ Text(stringResource(R.string.sealed_wrong_password)) }) else null,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    busy = true
                    val secret = openPassword.toCharArray()
                    scope.launch {
                        runCatching {
                            val json = withContext(Dispatchers.Default) { app.tsuzuku.data.BackupCrypto.decrypt(bytes, secret) }
                            json to repository.preview(json)
                        }.onSuccess { (json, summary) ->
                            secret.fill(' ')
                            busy = false
                            sealedFile = null
                            openPassword = ""
                            pendingJson = json
                            pendingSummary = summary
                        }.onFailure {
                            secret.fill(' ')
                            busy = false
                            wrongPassword = true
                        }
                    }
                }, enabled = openPassword.isNotEmpty() && !busy) { Text(stringResource(R.string.sealed_open)) }
            },
            dismissButton = { TextButton(onClick = { sealedFile = null; openPassword = "" }) { Text(stringResource(R.string.cancel)) } },
        )
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

/** "Encrypted full backup": password twice, a strength bar, then the file. */
@Composable
internal fun SealedBackupCard(
    password: String,
    passwordAgain: String,
    onPassword: (String) -> Unit,
    onPasswordAgain: (String) -> Unit,
    enabled: Boolean,
    onCreate: () -> Unit,
) {
    val strength = app.tsuzuku.data.BackupCrypto.strength(password)
    val mismatch = passwordAgain.isNotEmpty() && passwordAgain != password
    Column(
        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.sealed_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
        }
        Text(stringResource(R.string.sealed_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val pw = androidx.compose.ui.text.input.PasswordVisualTransformation()
        val keys = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
        androidx.compose.material3.OutlinedTextField(
            value = password, onValueChange = onPassword, singleLine = true, visualTransformation = pw, keyboardOptions = keys,
            label = { Text(stringResource(R.string.sealed_password_min, app.tsuzuku.data.BackupCrypto.MIN_PASSWORD)) },
            modifier = Modifier.fillMaxWidth(),
        )
        androidx.compose.material3.OutlinedTextField(
            value = passwordAgain, onValueChange = onPasswordAgain, singleLine = true, visualTransformation = pw, keyboardOptions = keys,
            label = { Text(stringResource(R.string.sealed_password_again)) },
            isError = mismatch,
            supportingText = if (mismatch) ({ Text(stringResource(R.string.sealed_mismatch)) }) else null,
            modifier = Modifier.fillMaxWidth(),
        )
        if (password.isNotEmpty()) {
            val color = when (strength) {
                2 -> androidx.compose.ui.graphics.Color(0xFF3F7D5A)
                1 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { (strength + 1) / 3f },
                    color = color,
                    modifier = Modifier.weight(1f).height(5.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
                )
                Text(
                    stringResource(listOf(R.string.sealed_weak, R.string.sealed_fair, R.string.sealed_strong)[strength]),
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
        androidx.compose.material3.Button(
            onClick = onCreate,
            enabled = enabled && password.length >= app.tsuzuku.data.BackupCrypto.MIN_PASSWORD && password == passwordAgain,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.sealed_create)) }
    }
}
