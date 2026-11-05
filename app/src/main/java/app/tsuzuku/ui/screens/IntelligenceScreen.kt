package app.tsuzuku.ui.screens

import androidx.annotation.StringRes
import app.tsuzuku.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.model.Insight
import app.tsuzuku.model.InsightConfidence
import app.tsuzuku.model.InsightPeriod
import app.tsuzuku.model.Recommendation

enum class IntelligenceTab(@param:StringRes val labelRes: Int) {
    Today(R.string.insights_of_the_day),
    Trends(R.string.trends_and_patterns),
    Recommendations(R.string.recommendations),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IntelligenceScreen(
    insights: List<Insight>,
    recommendations: List<Recommendation>,
    onBack: () -> Unit,
    onApplyRecommendation: (Recommendation) -> Unit,
    onOpenAskTsuzuku: () -> Unit = {},
    onOpenPlanAssistant: () -> Unit = {},
    onOpenAiPrivacy: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableStateOf(IntelligenceTab.Today) }
    var selectedWhyInsight by remember { mutableStateOf<Insight?>(null) }
    var pendingRecommendation by remember { mutableStateOf<Recommendation?>(null) }

    val todayInsights = insights.filter { it.period == InsightPeriod.Today || it.period == InsightPeriod.Weekly }
    val trendInsights = insights.filter { it.period == InsightPeriod.Trend || it.period == InsightPeriod.Monthly || it.period == InsightPeriod.Weekly }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tsuzuku Intelligence", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAiPrivacy) {
                        Icon(Icons.Rounded.Shield, contentDescription = "AI & Gizlilik")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            item {
                Text(stringResource(R.string.intelligence_mark), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.6.sp)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.personal_patterns_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.intelligence_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Quick AI Action Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenAskTsuzuku),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.ask_tsuzuku_cta), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.ask_tsuzuku_cta_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenPlanAssistant),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.ai_plan_assistant_cta), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.ai_plan_assistant_cta_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Privacy banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.intelligence_privacy_footer),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Tab bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IntelligenceTab.entries.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }

            // Content
            when (selectedTab) {
                IntelligenceTab.Today -> {
                    if (todayInsights.isEmpty()) {
                        item {
                            EmptyInsightsCard(message = stringResource(R.string.no_patterns_today))
                        }
                    } else {
                        items(todayInsights, key = { it.id }) { insight ->
                            InsightCard(
                                insight = insight,
                                onWhyClick = { selectedWhyInsight = insight },
                                onApplyRecommendation = { rec -> pendingRecommendation = rec },
                            )
                        }
                    }
                }
                IntelligenceTab.Trends -> {
                    if (trendInsights.isEmpty()) {
                        item {
                            EmptyInsightsCard(message = stringResource(R.string.trends_need_more_data))
                        }
                    } else {
                        items(trendInsights, key = { it.id }) { insight ->
                            InsightCard(
                                insight = insight,
                                onWhyClick = { selectedWhyInsight = insight },
                                onApplyRecommendation = { rec -> pendingRecommendation = rec },
                            )
                        }
                    }
                }
                IntelligenceTab.Recommendations -> {
                    if (recommendations.isEmpty()) {
                        item {
                            EmptyInsightsCard(message = stringResource(R.string.no_pending_recommendations))
                        }
                    } else {
                        items(recommendations, key = { it.id }) { rec ->
                            RecommendationItemCard(
                                recommendation = rec,
                                onApply = { pendingRecommendation = rec },
                            )
                        }
                    }
                }
            }
        }
    }

    // Why We Say This Bottom Sheet (Şeffaflık Modalı)
    if (selectedWhyInsight != null) {
        val insight = selectedWhyInsight!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { selectedWhyInsight = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column {
                    Text(stringResource(R.string.transparency_mark), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.why_we_say_this), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }

                Text(
                    insight.whyWeSayThis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (insight.supportingMetrics.isNotEmpty()) {
                    Text(stringResource(R.string.metrics_used), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        insight.supportingMetrics.forEach { (key, value) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    "$key: $value",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.confidence_explanation, insight.confidence.label),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Button(
                    onClick = { selectedWhyInsight = null },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.got_it))
                }
            }
        }
    }

    // Recommendation Preview Dialog (Kullanıcı Onayı)
    if (pendingRecommendation != null) {
        val rec = pendingRecommendation!!
        AlertDialog(
            onDismissRequest = { pendingRecommendation = null },
            title = {
                Text(rec.title, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(rec.description, style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            rec.suggestedChangeText,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        stringResource(R.string.no_change_without_approval),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyRecommendation(rec)
                        pendingRecommendation = null
                    },
                ) {
                    Text(stringResource(R.string.approve_and_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRecommendation = null }) {
                    Text(stringResource(R.string.dismiss_action))
                }
            },
        )
    }
}

@Composable
private fun InsightCard(
    insight: Insight,
    onWhyClick: () -> Unit,
    onApplyRecommendation: (Recommendation) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(insight.type.icon, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(insight.type.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                }
                ConfidenceBadge(insight.confidence)
            }

            Text(insight.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(insight.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onWhyClick) {
                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.why_question), style = MaterialTheme.typography.labelMedium)
                }

                insight.recommendation?.let { rec ->
                    OutlinedButton(
                        onClick = { onApplyRecommendation(rec) },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(rec.title, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationItemCard(
    recommendation: Recommendation,
    onApply: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(recommendation.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(recommendation.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    recommendation.suggestedChangeText,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            Button(
                onClick = onApply,
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(stringResource(R.string.apply_action))
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: InsightConfidence) {
    val (bgColor, textColor) = when (confidence) {
        InsightConfidence.High -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        InsightConfidence.Medium -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        InsightConfidence.Low -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            confidence.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EmptyInsightsCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🌱", fontSize = 32.sp)
            Text(stringResource(R.string.collecting_data), fontWeight = FontWeight.Medium)
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
