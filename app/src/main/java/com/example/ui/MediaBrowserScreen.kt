package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.FilterState
import com.example.ui.components.AddSourceDialog
import com.example.ui.components.LightboxViewer
import com.example.ui.components.MediaCard
import com.example.ui.components.SearchBarAndFilters
import com.example.ui.components.SourcePickerBottomSheet
import com.example.ui.components.SourceSelectorBar
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonIndigoLight
import com.example.ui.theme.RoseBadge
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun MediaBrowserScreen(
    viewModel: MediaBrowserViewModel,
    modifier: Modifier = Modifier
) {
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val activeSource by viewModel.activeSource.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val mediaItems by viewModel.mediaItems.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val lightboxIndex by viewModel.lightboxIndex.collectAsStateWithLifecycle()
    val isAddSourceOpen by viewModel.isAddSourceOpen.collectAsStateWithLifecycle()
    val isSourcePickerOpen by viewModel.isSourcePickerOpen.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyStaggeredGridState()

    // Show snackbar alerts
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissSnackbar()
        }
    }

    // Infinite scrolling trigger
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= totalItems - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoading && !isLoadingMore) {
            viewModel.loadNextPage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // App Header
            TopBrandHeader()

            // Source Selector Bar
            SourceSelectorBar(
                activeSource = activeSource,
                onOpenPicker = { viewModel.openSourcePicker() },
                onAddSourceClick = { viewModel.openAddSourceDialog() }
            )

            // Search Bar & Filter Controls
            SearchBarAndFilters(
                searchQuery = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                onSearch = { viewModel.executeSearch() },
                filterState = filterState,
                onFilterChange = { viewModel.setFilterState(it) },
                activeSource = activeSource
            )

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        // Loading skeleton state
                        LoadingSkeletonGrid()
                    }

                    errorMessage != null -> {
                        // Error state
                        ErrorStateView(
                            errorMessage = errorMessage!!,
                            onRetry = { viewModel.loadSourceData(isRefresh = true) }
                        )
                    }

                    mediaItems.isEmpty() -> {
                        // Empty state
                        EmptyStateView(
                            hasFilters = filterState.activeFilterCount > 0 || searchQuery.isNotBlank(),
                            onReset = {
                                viewModel.setSearchQuery("")
                                viewModel.setFilterState(FilterState())
                                viewModel.executeSearch()
                            }
                        )
                    }

                    else -> {
                        // Responsive Media Staggered Grid
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                            state = gridState,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalItemSpacing = 10.dp,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("media_staggered_grid")
                        ) {
                            itemsIndexed(
                                items = mediaItems,
                                key = { index, item -> "${item.id}_$index" }
                            ) { index, item ->
                                MediaCard(
                                    item = item,
                                    onClick = { viewModel.openLightbox(index) },
                                    onDownloadClick = { viewModel.downloadImage(item) },
                                    onTagClick = { tag ->
                                        viewModel.setSearchQuery(tag)
                                        viewModel.executeSearch()
                                    }
                                )
                            }

                            // Infinite loading spinner item at bottom
                            if (isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = NeonIndigoLight,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Source Picker Bottom Sheet
    if (isSourcePickerOpen) {
        SourcePickerBottomSheet(
            sources = sources,
            activeSource = activeSource,
            onSelectSource = { viewModel.setActiveSource(it) },
            onDeleteSource = { viewModel.deleteSource(it) },
            onAddNewSource = { viewModel.openAddSourceDialog() },
            onDismiss = { viewModel.closeSourcePicker() }
        )
    }

    // Add / Edit Source Dialog
    if (isAddSourceOpen) {
        AddSourceDialog(
            initialConfig = null,
            onDismiss = { viewModel.closeAddSourceDialog() },
            onSave = { newSource ->
                viewModel.addCustomSource(newSource)
            }
        )
    }

    // Lightbox Viewer Overlay
    lightboxIndex?.let { index ->
        val currentItem = mediaItems.getOrNull(index)
        if (currentItem != null) {
            LightboxViewer(
                item = currentItem,
                currentIndex = index,
                totalCount = mediaItems.size,
                onPrevious = { viewModel.previousLightboxItem() },
                onNext = { viewModel.nextLightboxItem() },
                onClose = { viewModel.closeLightbox() },
                onDownload = { viewModel.downloadImage(currentItem) },
                onTagClick = { tag ->
                    viewModel.setSearchQuery(tag)
                    viewModel.executeSearch()
                }
            )
        }
    }
}

@Composable
fun TopBrandHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NeonIndigo),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = "Artflux",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Text(
                text = "Anime Art Discovery",
                color = TextTertiary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun LoadingSkeletonGrid() {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        items(6) { index ->
            val ratio = if (index % 2 == 0) 1.3f else 0.8f
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = NeonIndigoLight,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    hasFilters: Boolean,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Media Found",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (hasFilters) "No results match your active search and filter criteria." else "This source returned no items for the current page.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            if (hasFilters) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Search & Filters", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun ErrorStateView(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(RoseBadge.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = RoseBadge,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Failed to Fetch Media",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = errorMessage,
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry Connection", color = TextPrimary)
            }
        }
    }
}
