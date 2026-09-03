package one.only.player.navigation

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

@Stable
class RootNavigationState(
    val pagerState: PagerState,
    val destinations: List<RootDestination>,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage.coerceIn(destinations.indices))
        private set

    var isNavigating by mutableStateOf(false)
        private set

    val selectedDestination: RootDestination
        get() = destinations.getOrElse(selectedPage) { destinations.first() }

    private var navigationJob: Job? = null

    fun animateTo(destination: RootDestination) {
        val targetPage = destinations.indexOf(destination)
        if (targetPage < 0) return
        if (targetPage == selectedPage) return

        navigationJob?.cancel()
        selectedPage = targetPage
        isNavigating = true

        val pageDistance = abs(targetPage - pagerState.currentPage).coerceAtLeast(2)
        val durationMillis = 100 * pageDistance + 100
        val layoutInfo = pagerState.layoutInfo
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        val distanceInPages = targetPage - pagerState.currentPage - pagerState.currentPageOffsetFraction

        navigationJob = coroutineScope.launch {
            val currentJob = coroutineContext.job
            try {
                pagerState.animateScrollBy(
                    value = distanceInPages * pageSize,
                    animationSpec = tween(
                        durationMillis = durationMillis,
                        easing = EaseInOut,
                    ),
                )
            } finally {
                if (navigationJob == currentJob) {
                    isNavigating = false
                    selectedPage = pagerState.currentPage
                }
            }
        }
    }

    fun jumpTo(destination: RootDestination) {
        val targetPage = destinations.indexOf(destination)
        if (targetPage < 0) return

        navigationJob?.cancel()
        navigationJob = null
        isNavigating = false
        selectedPage = targetPage
        pagerState.requestScrollToPage(targetPage)
    }

    fun syncPage() {
        if (isNavigating || selectedPage == pagerState.currentPage) return
        selectedPage = pagerState.currentPage
    }

    // 目的地列表收缩后当前页可能越界，回退到首个目的地
    fun clampToDestinations() {
        if (selectedPage in destinations.indices) return
        jumpTo(destinations.first())
    }
}

@Composable
fun rememberRootNavigationState(
    destinations: List<RootDestination>,
    initialDestination: RootDestination = RootDestination.HOME,
): RootNavigationState {
    val pagerState = rememberPagerState(
        initialPage = destinations.indexOf(initialDestination).coerceAtLeast(0),
        pageCount = { destinations.size },
    )
    val coroutineScope = rememberCoroutineScope()
    return remember(pagerState, destinations, coroutineScope) {
        RootNavigationState(
            pagerState = pagerState,
            destinations = destinations,
            coroutineScope = coroutineScope,
        )
    }
}
