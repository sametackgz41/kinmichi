package app.tsuzuku.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.BuildConfig
import app.tsuzuku.R
import app.tsuzuku.data.DiagnosticStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DiagnosticsScreen(store: DiagnosticStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasCrash by rememberSaveable { mutableStateOf(store.hasCrashReport()) }
    var exportMessage by rememberSaveable { mutableStateOf<Int?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val report = store.report()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(report) }
                        ?: error("Unable to write")
                }
            }.onSuccess { exportMessage = R.string.diagnostic_export_success }
                .onFailure { exportMessage = R.string.diagnostic_export_failed }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader(stringResource(R.string.error_reports), onBack) }
        item {
            Column {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.BugReport, contentDescription = null)
                    Text(
                        stringResource(if (hasCrash) R.string.crash_report_available else R.string.no_crash_reports),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(stringResource(R.string.diagnostic_privacy_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.app_version_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
                }
            }
        }
        item {
            Button(onClick = { exporter.launch("Tsuzuku-diagnostic.txt") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.UploadFile, contentDescription = null)
                Text(stringResource(R.string.export_diagnostic), modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (hasCrash) {
            item {
                FilledTonalButton(
                    onClick = { store.clearCrash(); hasCrash = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    Text(stringResource(R.string.clear_error_report), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        exportMessage?.let { message -> item { Text(stringResource(message), color = MaterialTheme.colorScheme.primary) } }
    }
}
