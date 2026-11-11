package com.swartzz.artflux

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.swartzz.artflux.ui.components.EmptyStateView
import com.swartzz.artflux.ui.components.FeedPaginationFooter
import com.swartzz.artflux.ui.components.FeedPaginationTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FeedPaginationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failedPageOffersExplicitRetry() {
        var retries = 0
        compose.setContent { MaterialTheme { FeedPaginationFooter(false, "Offline", { retries++ }) } }
        compose.onNodeWithText("Offline").assertIsDisplayed()
        compose.onNodeWithText("Retry loading more").performClick()
        assertEquals(1, retries)
    }

    @Test fun emptyFilteredFeedStillOffersMorePages() {
        var loads = 0
        compose.setContent { MaterialTheme { EmptyStateView(true, {}, { loads++ }) } }
        compose.onNodeWithText("Load more").performClick()
        assertEquals(1, loads)
    }

    @Test fun triggerPredicateUsesHighestVisibleIndex() {
        // A full-line footer at the very end: visible across lanes at the bottom.
        assertTrue(FeedPaginationTrigger.isNearEnd(listOf(28, 29, 30), 31))
        // Mid-feed: not near the end even when lane order differs from index order.
        assertFalse(FeedPaginationTrigger.isNearEnd(listOf(3, 1, 2), 31))
        assertFalse(FeedPaginationTrigger.isNearEnd(emptyList(), 31))
        assertFalse(FeedPaginationTrigger.isNearEnd(listOf(0), 0))
    }

    @Test fun loadMoreFooterStaysReachableAtGridBottom() {
        compose.setContent {
            MaterialTheme {
                val grid = rememberLazyStaggeredGridState()
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(160.dp),
                    state = grid,
                    modifier = Modifier.fillMaxSize().testTag("paginated_grid")
                ) {
                    items(40) { index ->
                        // Mixed heights so lanes end unevenly (the regression scenario).
                        Box(Modifier.fillMaxWidth().height(if (index % 2 == 0) 300.dp else 140.dp))
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        FeedPaginationFooter(isLoading = false, error = null, onLoadMore = {})
                    }
                }
            }
        }
        compose.onNodeWithTag("paginated_grid").performScrollToNode(
            androidx.compose.ui.test.hasTestTag("feed_load_more")
        )
        compose.onNodeWithText("Load more").assertIsDisplayed()
        compose.onNodeWithTag("feed_load_more").performClick()
    }
}
