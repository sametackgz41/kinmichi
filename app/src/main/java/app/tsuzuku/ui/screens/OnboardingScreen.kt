package app.tsuzuku.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.R

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val pages = listOf(
        OnboardingPage(Icons.Rounded.CheckCircle, R.string.onboarding_welcome_title, R.string.onboarding_welcome_body),
        OnboardingPage(Icons.AutoMirrored.Rounded.MenuBook, R.string.onboarding_library_title, R.string.onboarding_library_body),
        OnboardingPage(Icons.Rounded.Flag, R.string.onboarding_goals_title, R.string.onboarding_goals_body),
        OnboardingPage(Icons.Rounded.Lock, R.string.onboarding_privacy_title, R.string.onboarding_privacy_body),
    )
    val page = pages[step]
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.brand_japanese), style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onComplete) { Text(stringResource(R.string.skip_label)) }
            }
            LinearProgressIndicator(
                progress = { (step + 1f) / pages.size },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
        Column(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("0${step + 1} / 0${pages.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                    Icon(page.icon, contentDescription = null, modifier = Modifier.size(36.dp))
                }
                Text(stringResource(page.titleRes), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(page.bodyRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        Column {
            Button(
                onClick = { if (step == pages.lastIndex) onComplete() else step++ },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(8.dp),
            ) { Text(stringResource(if (step == pages.lastIndex) R.string.start_using_tsuzuku else R.string.continue_label)) }
            Spacer(Modifier.height(8.dp))
            if (step > 0) {
                TextButton(onClick = { step-- }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
            }
        }
    }
}

private data class OnboardingPage(val icon: ImageVector, val titleRes: Int, val bodyRes: Int)
