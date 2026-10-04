package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.FilterState
import com.example.model.MediaSourceConfig
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

enum class MainTab {
    HOME, SEARCH, SETTINGS
}

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

    var currentTab by remember { mutableStateOf(MainTab.HOME) }

    val snackbarHostState = remember { SnackbarHostState() }
    val homeGridState = rememberLazyStaggeredGridState()
    val searchGridState = rememberLazyStaggeredGridState()

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
    val activeGridState = if (currentTab == MainTab.HOME) homeGridState else searchGridState
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = activeGridState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = activeGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ArtfluxBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.HOME -> {
                    HomeScreen(
                        activeSource = activeSource,
                        mediaItems = mediaItems,
                        isLoading = isLoading,
                        isLoadingMore = isLoadingMore,
                        errorMessage = errorMessage,
                        filterState = filterState,
                        searchQuery = searchQuery,
                        gridState = homeGridState,
                        onOpenSourcePicker = { viewModel.openSourcePicker() },
                        onRetry = { viewModel.loadSourceData(isRefresh = true) },
                        onResetFilters = {
                            viewModel.setSearchQuery("")
                            viewModel.setFilterState(FilterState())
                            viewModel.executeSearch()
                        },
                        onOpenLightbox = { viewModel.openLightbox(it) },
                        onDownload = { viewModel.downloadImage(it) },
                        onTagClick = { tag ->
                            viewModel.setSearchQuery(tag)
                            viewModel.executeSearch()
                        }
                    )
                }
                MainTab.SEARCH -> {
                    SearchScreen(
                        sources = sources,
                        activeSource = activeSource,
                        searchQuery = searchQuery,
                        filterState = filterState,
                        mediaItems = mediaItems,
                        isLoading = isLoading,
                        isLoadingMore = isLoadingMore,
                        errorMessage = errorMessage,
                        gridState = searchGridState,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        onSearch = { viewModel.executeSearch() },
                        onFilterChange = { viewModel.setFilterState(it) },
                        onSelectSource = { viewModel.setActiveSource(it) },
                        onOpenPicker = { viewModel.openSourcePicker() },
                        onAddSourceClick = { viewModel.openAddSourceDialog() },
                        onRetry = { viewModel.loadSourceData(isRefresh = true) },
                        onResetFilters = {
                            viewModel.setSearchQuery("")
                            viewModel.setFilterState(FilterState())
                            viewModel.executeSearch()
                        },
                        onOpenLightbox = { viewModel.openLightbox(it) },
                        onDownload = { viewModel.downloadImage(it) },
                        onTagClick = { tag ->
                            viewModel.setSearchQuery(tag)
                            viewModel.executeSearch()
                        }
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        sources = sources,
                        onAddSourceClick = { viewModel.openAddSourceDialog() },
                        onDeleteSource = { viewModel.deleteSource(it) }
                    )
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
fun ArtfluxBottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == MainTab.HOME,
            onClick = { onTabSelected(MainTab.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextPrimary,
                selectedTextColor = TextPrimary,
                indicatorColor = NeonIndigo,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_tab_home")
        )
        NavigationBarItem(
            selected = currentTab == MainTab.SEARCH,
            onClick = { onTabSelected(MainTab.SEARCH) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextPrimary,
                selectedTextColor = TextPrimary,
                indicatorColor = NeonIndigo,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_tab_search")
        )
        NavigationBarItem(
            selected = currentTab == MainTab.SETTINGS,
            onClick = { onTabSelected(MainTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextPrimary,
                selectedTextColor = TextPrimary,
                indicatorColor = NeonIndigo,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_tab_settings")
        )
    }
}

@Composable
fun HomeScreen(
    activeSource: MediaSourceConfig,
    mediaItems: List<com.example.model.MediaItem>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    errorMessage: String?,
    filterState: FilterState,
    searchQuery: String,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    onOpenSourcePicker: () -> Unit,
    onRetry: () -> Unit,
    onResetFilters: () -> Unit,
    onOpenLightbox: (Int) -> Unit,
    onDownload: (com.example.model.MediaItem) -> Unit,
    onTagClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Minimal Chrome Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonIndigo),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Artflux",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            // Active Source Pill (taps to open source picker)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenSourcePicker)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("home_source_pill")
            ) {
                Text(
                    text = activeSource.name,
                    color = NeonIndigoLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Main Discovery Feed Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                isLoading -> {
                    LoadingSkeletonGrid()
                }

                errorMessage != null -> {
                    ErrorStateView(
                        errorMessage = errorMessage,
                        onRetry = onRetry
                    )
                }

                mediaItems.isEmpty() -> {
                    EmptyStateView(
                        hasFilters = filterState.activeFilterCount > 0 || searchQuery.isNotBlank(),
                        onReset = onResetFilters
                    )
                }

                else -> {
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
                                onClick = { onOpenLightbox(index) },
                                onDownloadClick = { onDownload(item) },
                                onTagClick = onTagClick
                            )
                        }

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

@Composable
fun SearchScreen(
    sources: List<MediaSourceConfig>,
    activeSource: MediaSourceConfig,
    searchQuery: String,
    filterState: FilterState,
    mediaItems: List<com.example.model.MediaItem>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    errorMessage: String?,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFilterChange: (FilterState) -> Unit,
    onSelectSource: (MediaSourceConfig) -> Unit,
    onOpenPicker: () -> Unit,
    onAddSourceClick: () -> Unit,
    onRetry: () -> Unit,
    onResetFilters: () -> Unit,
    onOpenLightbox: (Int) -> Unit,
    onDownload: (com.example.model.MediaItem) -> Unit,
    onTagClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search Screen Header
        Text(
            text = "Explore & Search",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Source Selector Bar
        SourceSelectorBar(
            activeSource = activeSource,
            onOpenPicker = onOpenPicker,
            onAddSourceClick = onAddSourceClick
        )

        // Search Bar & Filter Controls (includes quick preset tags)
        SearchBarAndFilters(
            searchQuery = searchQuery,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            filterState = filterState,
            onFilterChange = onFilterChange,
            activeSource = activeSource
        )

        // Search Results Feed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                isLoading -> {
                    LoadingSkeletonGrid()
                }

                errorMessage != null -> {
                    ErrorStateView(
                        errorMessage = errorMessage,
                        onRetry = onRetry
                    )
                }

                mediaItems.isEmpty() -> {
                    EmptyStateView(
                        hasFilters = filterState.activeFilterCount > 0 || searchQuery.isNotBlank(),
                        onReset = onResetFilters
                    )
                }

                else -> {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                        state = gridState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalItemSpacing = 10.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_results_grid")
                    ) {
                        itemsIndexed(
                            items = mediaItems,
                            key = { index, item -> "${item.id}_search_$index" }
                        ) { index, item ->
                            MediaCard(
                                item = item,
                                onClick = { onOpenLightbox(index) },
                                onDownloadClick = { onDownload(item) },
                                onTagClick = onTagClick
                            )
                        }

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
