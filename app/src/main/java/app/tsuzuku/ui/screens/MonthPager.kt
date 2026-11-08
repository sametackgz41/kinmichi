package app.tsuzuku.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** How far back a calendar can be swiped. */
private const val MONTHS_BACK = 120

/**
 * A month calendar you swipe left and right, one month per page, up to this month. The arrows
 * outside keep working: change [month] and the pager follows; swipe and [onMonth] is told.
 */
@Composable
internal fun MonthPager(
    month: YearMonth,
    onMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    latest: YearMonth = YearMonth.now(),
    content: @Composable (YearMonth) -> Unit,
) {
    val first = latest.minusMonths(MONTHS_BACK.toLong())
    fun pageOf(m: YearMonth) = ChronoUnit.MONTHS.between(first, m).toInt().coerceIn(0, MONTHS_BACK)
    val pager = rememberPagerState(initialPage = pageOf(month)) { MONTHS_BACK + 1 }
    val report by rememberUpdatedState(onMonth)
    val current by rememberUpdatedState(month)
    // Arrows (or anything else) changed the month: slide there.
    LaunchedEffect(month) {
        val target = pageOf(month)
        if (pager.currentPage != target) pager.animateScrollToPage(target)
    }
    // A swipe settled on another month: report it.
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            val m = first.plusMonths(page.toLong())
            if (m != current) report(m)
        }
    }
    HorizontalPager(state = pager, modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, beyondViewportPageCount = 1) { page ->
        content(first.plusMonths(page.toLong()))
    }
}
