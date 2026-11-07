package app.tsuzuku.ui.screens

import android.app.Activity
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.data.CloudAccountState
import app.tsuzuku.data.CloudSyncState
import kotlinx.coroutines.launch

/** Accounts that can back up and sync Kinmichi. */
enum class SignInProvider { Google, GitHub }

/**
 * Last step of the first run, after the tour. First: continue with Google, GitHub or as a guest.
 * Then: the name Today greets you with (filled in from the account when there is one).
 */
@Composable
fun WelcomeScreen(
    account: CloudAccountState,
    syncState: CloudSyncState,
    initialName: String,
    onSignIn: suspend (Activity, SignInProvider) -> Unit,
    onFinish: (name: String, signedIn: Boolean) -> Unit,
    startOnName: Boolean = false,
) {
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(initialName) }
    var nameEdited by rememberSaveable { mutableStateOf(initialName.isNotBlank()) }
    var askName by rememberSaveable { mutableStateOf(startOnName) }
    var pending by rememberSaveable { mutableStateOf<SignInProvider?>(null) }
    val signedIn = account as? CloudAccountState.SignedIn
    val working = syncState is CloudSyncState.Working
    if (!working) pending = null
    // Already signed in when the screen opened (e.g. replaying the intro): don't skip ahead.
    val signedInAtStart = androidx.compose.runtime.remember { account is CloudAccountState.SignedIn }

    // After signing in here, move on to the name step with the account's first name filled in.
    LaunchedEffect(signedIn) {
        if (signedIn != null) {
            val accountName = preferredGoogleName(signedIn.displayName)
            // A fresh sign-in here wins over a name left from a guest session; the field stays editable.
            if (accountName.isNotBlank() && (!signedInAtStart || !nameEdited)) {
                name = accountName
                nameEdited = false
            }
            if (!signedInAtStart) askName = true
        }
    }
    androidx.activity.compose.BackHandler(enabled = askName && signedIn == null) { askName = false }

    androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) {
        androidx.compose.animation.AnimatedContent(
            targetState = askName,
            transitionSpec = {
                val forward = targetState
                (androidx.compose.animation.slideInHorizontally { if (forward) it / 4 else -it / 4 } + androidx.compose.animation.fadeIn()) togetherWith
                    (androidx.compose.animation.slideOutHorizontally { if (forward) -it / 4 else it / 4 } + androidx.compose.animation.fadeOut())
            },
            label = "welcome-step",
        ) { nameStep ->
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (nameStep) {
                    NameStep(
                        name = name,
                        onNameChange = { name = it.take(40); nameEdited = true },
                        signedIn = signedIn,
                        onBack = if (signedIn == null) ({ askName = false }) else null,
                        onFinish = { onFinish(name.trim(), signedIn != null) },
                    )
                } else {
                    ChooseStep(
                        signedIn = signedIn,
                        onContinueSignedIn = {
                            if (!nameEdited && signedIn != null) name = preferredGoogleName(signedIn.displayName)
                            askName = true
                        },
                        showAccounts = account !is CloudAccountState.Unavailable,
                        pending = pending,
                        enabled = activity != null && !working,
                        error = (syncState as? CloudSyncState.Error)?.message,
                        onSignIn = { provider ->
                            pending = provider
                            activity?.let { scope.launch { onSignIn(it, provider) } }
                        },
                        onGuest = { askName = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ChooseStep(
    signedIn: CloudAccountState.SignedIn?,
    onContinueSignedIn: () -> Unit,
    showAccounts: Boolean,
    pending: SignInProvider?,
    enabled: Boolean,
    error: String?,
    onSignIn: (SignInProvider) -> Unit,
    onGuest: () -> Unit,
) {
    Spacer(Modifier.weight(1f).height(16.dp))
    GoldenPathArt(artSize = 96.dp)
    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(28.dp))
    Text(
        stringResource(R.string.welcome_hello_title),
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.welcome_hello_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    BenefitList()
    Spacer(Modifier.height(28.dp))
    if (signedIn != null) {
        SignedInRow(signedIn)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onContinueSignedIn,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.continue_label)) }
        Spacer(Modifier.weight(1f).height(20.dp))
        return
    }
    if (showAccounts) {
        ProviderButton(
            label = stringResource(R.string.sign_in_with_google),
            icon = R.drawable.ic_brand_google,
            tintIcon = false,
            loading = pending == SignInProvider.Google,
            enabled = enabled,
            onClick = { onSignIn(SignInProvider.Google) },
        )
        Spacer(Modifier.height(10.dp))
        ProviderButton(
            label = stringResource(R.string.sign_in_with_github),
            icon = R.drawable.ic_brand_github,
            tintIcon = true,
            loading = pending == SignInProvider.GitHub,
            enabled = enabled,
            onClick = { onSignIn(SignInProvider.GitHub) },
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        OrDivider(stringResource(R.string.welcome_or))
        Spacer(Modifier.height(16.dp))
    }
    ProviderButton(
        label = stringResource(R.string.continue_as_guest),
        icon = R.drawable.ic_guest,
        tintIcon = true,
        loading = false,
        enabled = pending == null,
        onClick = onGuest,
        guest = true,
    )
    Spacer(Modifier.weight(1f).height(20.dp))
    Text(
        stringResource(R.string.welcome_accounts_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.NameStep(
    name: String,
    onNameChange: (String) -> Unit,
    signedIn: CloudAccountState.SignedIn?,
    onBack: (() -> Unit)?,
    onFinish: () -> Unit,
) {
    val focus = LocalFocusManager.current
    Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterStart) {
        if (onBack != null) {
            androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
            }
        }
    }
    Spacer(Modifier.weight(1f).height(8.dp))
    GoldenPathArt(artSize = 72.dp)
    Spacer(Modifier.height(20.dp))
    Text(
        stringResource(R.string.welcome_title),
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.welcome_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.welcome_name_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); onFinish() }),
        modifier = Modifier.fillMaxWidth(),
    )
    if (signedIn != null) {
        Spacer(Modifier.height(12.dp))
        SignedInRow(signedIn)
    }
    Spacer(Modifier.height(20.dp))
    GreetingPreview(name)
    Spacer(Modifier.weight(1f).height(32.dp))
    Button(
        onClick = onFinish,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) { Text(stringResource(R.string.start_using_tsuzuku)) }
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(if (signedIn != null) R.string.welcome_synced_note else R.string.welcome_private_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        textAlign = TextAlign.Center,
    )
}

/** What an account adds, in three short lines with the app's own soft icon discs. */
@Composable
internal fun BenefitList() {
    val rows = listOf(
        Icons.Rounded.CloudDone to R.string.welcome_benefit_backup,
        Icons.Rounded.Devices to R.string.welcome_benefit_devices,
        Icons.Rounded.Lock to R.string.welcome_benefit_private,
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { (icon, text) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp)) }
                Text(
                    stringResource(text),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

/**
 * A small copy of the top of Today, updating as the name is typed, so it's clear where the name
 * shows up: today's date and "Good morning, Samet".
 */
@Composable
private fun GreetingPreview(name: String) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val date = androidx.compose.runtime.remember(locale) {
        java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern(app.tsuzuku.model.dayAndWeekdayPattern(locale), locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
    val greeting = stringResource(
        when (java.time.LocalTime.now().hour) {
            in 5..11 -> R.string.good_morning
            in 12..17 -> R.string.good_afternoon
            else -> R.string.good_evening
        },
    )
    val trimmed = name.trim()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            stringResource(R.string.welcome_preview_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(Modifier.height(10.dp))
        Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.animation.AnimatedContent(
            targetState = trimmed,
            transitionSpec = { androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut() },
            label = "greeting-preview",
        ) { value ->
            Text(
                if (value.isBlank()) greeting else stringResource(R.string.personal_greeting, greeting, value),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
            )
        }
    }
}

/** A thin rule with a label in the middle. */
@Composable
private fun OrDivider(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        androidx.compose.material3.HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        androidx.compose.material3.HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * Pill-shaped account button: brand mark on the left (as Google's sign-in branding asks), label
 * centred. The guest button is outlined only, so the account buttons read as the main choice.
 */
@Composable
internal fun ProviderButton(
    label: String,
    @androidx.annotation.DrawableRes icon: Int,
    tintIcon: Boolean,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    guest: Boolean = false,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        color = if (guest) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.align(Alignment.CenterStart).size(24.dp), contentAlignment = Alignment.Center) {
                if (loading) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        painterResource(icon),
                        contentDescription = null,
                        tint = if (tintIcon) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SignedInRow(signedIn: CloudAccountState.SignedIn) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
        Text(
            stringResource(R.string.welcome_signed_in, signedIn.email.ifBlank { signedIn.displayName }),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
