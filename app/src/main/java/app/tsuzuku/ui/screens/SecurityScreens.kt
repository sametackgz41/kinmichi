package app.tsuzuku.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.data.preferences.AppSettings

private const val SECURITY_CHANGE = "change"
private const val SECURITY_DISABLE = "disable"

@Composable
fun SecurityScreen(
    settings: AppSettings,
    biometricAvailable: Boolean,
    onBack: () -> Unit,
    onSetPin: (String) -> Boolean,
    onChangePin: (String, String) -> Boolean,
    onDisablePin: (String) -> Boolean,
    onSetBiometric: (Boolean) -> Unit,
    onSetLockDelayMinutes: (Int) -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    var activeDialog by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ScreenHeader(stringResource(R.string.app_lock), onBack) }
        item {
            Column {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        if (settings.appLockEnabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = null,
                    )
                    Text(
                        stringResource(if (settings.appLockEnabled) R.string.app_lock_enabled else R.string.app_lock_disabled),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(R.string.app_lock_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (!settings.appLockEnabled) {
            item { PinField(pin, { pin = pinInput(it); error = false }, R.string.new_pin, error) }
            item { PinField(confirmation, { confirmation = pinInput(it); error = false }, R.string.confirm_pin, error) }
            if (error) item { Text(stringResource(R.string.pin_setup_error), color = MaterialTheme.colorScheme.error) }
            item {
                Button(
                    onClick = {
                        error = pin.length != 4 || pin != confirmation || !onSetPin(pin)
                        if (!error) { pin = ""; confirmation = "" }
                    },
                    enabled = pin.length == 4 && confirmation.length == 4,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) { Text(stringResource(R.string.enable_app_lock)) }
            }
        } else {
            item {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                        Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                            Text(stringResource(R.string.biometric_unlock), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(
                                    if (biometricAvailable) R.string.biometric_unlock_description
                                    else R.string.biometric_unavailable,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = settings.biometricUnlockEnabled,
                            onCheckedChange = onSetBiometric,
                            enabled = biometricAvailable,
                        )
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.lock_after_background), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(0, 1, 5, 15).forEach { minutes ->
                            FilterChip(
                                selected = settings.lockDelayMinutes == minutes,
                                onClick = { onSetLockDelayMinutes(minutes) },
                                label = { Text(if (minutes == 0) stringResource(R.string.immediately) else stringResource(R.string.lock_delay_minutes, minutes)) },
                            )
                        }
                    }
                }
            }
            item {
                FilledTonalButton(onClick = { activeDialog = SECURITY_CHANGE }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.change_pin))
                }
            }
            item {
                OutlinedButton(onClick = { activeDialog = SECURITY_DISABLE }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.disable_app_lock), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    when (activeDialog) {
        SECURITY_CHANGE -> ChangePinDialog(
            onDismiss = { activeDialog = null },
            onConfirm = { current, new ->
                val changed = onChangePin(current, new)
                if (changed) activeDialog = null
                changed
            },
        )
        SECURITY_DISABLE -> VerifyPinDialog(
            title = stringResource(R.string.disable_app_lock),
            confirmLabel = stringResource(R.string.disable_label),
            onDismiss = { activeDialog = null },
            onConfirm = {
                val disabled = onDisablePin(it)
                if (disabled) activeDialog = null
                disabled
            },
        )
    }
}

@Composable
fun AppLockScreen(
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onVerifyPin: (String) -> Boolean,
    onUnlocked: () -> Unit,
    onBiometric: () -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.app_locked), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.enter_pin_to_continue), color = MaterialTheme.colorScheme.onSurfaceVariant)
            PinField(
                value = pin,
                onValueChange = { pin = pinInput(it); error = false },
                labelRes = R.string.pin_code,
                isError = error,
            )
            if (error) Text(stringResource(R.string.incorrect_pin), color = MaterialTheme.colorScheme.error)
            Button(
                enabled = pin.length == 4,
                onClick = {
                    if (onVerifyPin(pin)) onUnlocked() else { error = true; pin = "" }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.unlock)) }
            if (biometricEnabled && biometricAvailable) {
                FilledTonalButton(onClick = onBiometric, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                    Text(stringResource(R.string.use_biometrics), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ChangePinDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Boolean) {
    var current by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_pin)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PinField(current, { current = pinInput(it); error = false }, R.string.current_pin, error)
                PinField(newPin, { newPin = pinInput(it); error = false }, R.string.new_pin, error)
                PinField(confirmation, { confirmation = pinInput(it); error = false }, R.string.confirm_pin, error)
                if (error) Text(stringResource(R.string.pin_change_error), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = current.length == 4 && newPin.length == 4 && confirmation.length == 4,
                onClick = { error = newPin != confirmation || !onConfirm(current, newPin) },
            ) { Text(stringResource(R.string.save_label)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun VerifyPinDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Boolean,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                PinField(pin, { pin = pinInput(it); error = false }, R.string.current_pin, error)
                if (error) Text(stringResource(R.string.incorrect_pin), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = pin.length == 4, onClick = { error = !onConfirm(pin) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    isError: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(labelRes)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        isError = isError,
    )
}

private fun pinInput(value: String): String = value.filter(Char::isDigit).take(4)
