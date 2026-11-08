package app.tsuzuku.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import kotlinx.coroutines.launch

private val Gold = Color(0xFFC9A34A)

/**
 * The opening moment: a brush ensō draws itself around a gold 金, the name settles in, then it
 * fades into the app. About a second; a tap skips it.
 */
@Composable
internal fun OpeningScreen(onDone: () -> Unit, still: Float? = null) {
    val sweep = remember { Animatable(still ?: 0f) }
    val text = remember { Animatable(if (still != null) 1f else 0f) }
    val out = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (still != null) return@LaunchedEffect
        launch { sweep.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
        launch { text.animateTo(1f, tween(420, delayMillis = 280, easing = LinearEasing)) }
        kotlinx.coroutines.delay(1050)
        out.animateTo(0f, tween(260))
        onDone()
    }
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier.fillMaxSize().alpha(out.value).background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(170.dp)) {
                    val w = size.minDimension * 0.075f
                    val inset = w / 2
                    // A brush stroke: thick where it starts, thinning as it closes.
                    val total = 330f * sweep.value
                    val steps = 40
                    for (i in 0 until steps) {
                        val from = total * i / steps
                        val part = total / steps + 0.6f
                        val k = 1f - 0.45f * (i.toFloat() / steps)
                        drawArc(
                            primary, -80f + from, part, false,
                            topLeft = Offset(inset, inset), size = Size(size.width - w, size.height - w),
                            style = Stroke(w * k, cap = StrokeCap.Round),
                        )
                    }
                }
                Text("金", fontSize = 54.sp, fontFamily = app.tsuzuku.ui.theme.SerifDisplay, color = Gold, modifier = Modifier.alpha(text.value))
            }
            Text(
                "Kinmichi",
                fontSize = 40.sp,
                fontFamily = app.tsuzuku.ui.theme.SerifDisplay,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 22.dp).alpha(text.value).graphicsLayer { translationY = (1f - text.value) * 24f },
            )
            Text(
                stringResource(R.string.opening_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(text.value),
            )
        }
    }
}

/**
 * The lock: who it is for, the fingerprint asked at once (when on), and the PIN as a fallback.
 */
@Composable
fun AppLockScreen(
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onVerifyPin: (String) -> Boolean,
    onUnlocked: () -> Unit,
    onBiometric: () -> Unit,
    name: String = "",
    autoPrompt: Boolean = true,
) {
    val fingerprint = biometricEnabled && biometricAvailable
    var showPin by rememberSaveable { mutableStateOf(!fingerprint) }
    var pin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    // Ask for the fingerprint as soon as the screen is up (after the opening has faded).
    LaunchedEffect(autoPrompt, fingerprint) { if (autoPrompt && fingerprint) onBiometric() }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileAvatar(name, 76)
            Text(
                if (name.isBlank()) stringResource(R.string.app_locked) else stringResource(R.string.lock_welcome_back, name),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = app.tsuzuku.ui.theme.SerifDisplay,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            if (fingerprint) {
                Surface(onClick = onBiometric, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(96.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Fingerprint, stringResource(R.string.use_biometrics), Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(stringResource(R.string.lock_fingerprint), style = MaterialTheme.typography.bodyLarge)
            }
            if (showPin) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(4); error = false },
                    label = { Text(stringResource(R.string.pin_code)) },
                    singleLine = true,
                    isError = error,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error) Text(stringResource(R.string.incorrect_pin), color = MaterialTheme.colorScheme.error)
                Button(
                    enabled = pin.length == 4,
                    onClick = { if (onVerifyPin(pin)) onUnlocked() else { error = true; pin = "" } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.unlock)) }
            } else {
                TextButton(onClick = { showPin = true }) { Text(stringResource(R.string.lock_use_pin)) }
            }
        }
    }
}
