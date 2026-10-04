package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppTheme
import com.example.model.FilterState
import com.example.model.MediaItem
import com.example.model.MediaRating
import com.example.model.MediaSourceConfig
import com.example.model.MediaType
import com.example.model.Orientation
import com.example.model.SortOption
import com.example.model.ThumbnailQuality
import com.example.ui.components.AddSourceDialog
import com.example.ui.components.DownloadConfigDialog
import com.example.ui.components.LightboxViewer
import com.example.ui.components.MediaCard
import com.example.ui.components.PRESET_TAGS
import com.example.ui.components.SourcePickerBottomSheet
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaBrowserScreen(
    viewModel: MediaBrowserViewModel,
    modifier: Modifier = Modifier
) {
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val blurNsfw by viewModel.blurNsfw.collectAsStateWithLifecycle()
    val thumbnailQuality by viewModel.thumbnailQuality.collectAsStateWithLifecycle()
    val loopVideo by viewModel.loopVideoPlayback.collectAsStateWithLifecycle()

    val lightboxIndex by viewModel.lightboxIndex.collectAsStateWithLifecycle()
    val lightboxItems by viewModel.lightboxItems.collectAsStateWithLifecycle()
    val isAddSourceOpen by viewModel.isAddSourceOpen.collectAsStateWithLifecycle()
    val isSourcePickerOpen by viewModel.isSourcePickerOpen.collectAsStateWithLifecycle()
    val downloadTargetItem by viewModel.downloadTargetItem.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var isHomeSettingsOpen by remember { mutableStateOf(false) }

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

    // Infinite scrolling trigger for Home
    val shouldLoadMoreHome by remember {
        derivedStateOf {
            val total = homeGridState.layoutInfo.totalItemsCount
            val last = homeGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 4
        }
    }
    LaunchedEffect(shouldLoadMoreHome) {
        if (shouldLoadMoreHome && currentTab == MainTab.HOME) {
            viewModel.loadNextHomePage()
        }
    }

    // Infinite scrolling trigger for Search
    val shouldLoadMoreSearch by remember {
        derivedStateOf {
            val total = searchGridState.layoutInfo.totalItemsCount
            val last = searchGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 4
        }
    }
    LaunchedEffect(shouldLoadMoreSearch) {
        if (shouldLoadMoreSearch && currentTab == MainTab.SEARCH) {
            viewModel.loadNextSearchPage()
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
                        homeState = homeState,
                        thumbnailQuality = thumbnailQuality,
                        blurNsfw = blurNsfw,
                        gridState = homeGridState,
                        onOpenFeedSettings = { isHomeSettingsOpen = true },
                        onRefresh = { viewModel.refreshHome() },
                        onRetry = { viewModel.refreshHome() },
                        onResetFilters = {
                            viewModel.setHomeFilterState(FilterState())
                            viewModel.refreshHome()
                        },
                        onOpenLightbox = { index ->
                            viewModel.openLightbox(index, homeState.mediaItems)
                        },
                        onDownload = { viewModel.promptDownload(it) },
                        onTagClick = { tag ->
                            currentTab = MainTab.SEARCH
                            viewModel.setSearchQuery(tag)
                            viewModel.executeSearchQuery()
                        }
                    )
                }
                MainTab.SEARCH -> {
                    SearchScreen(
                        searchState = searchState,
                        sources = sources,
                        thumbnailQuality = thumbnailQuality,
                        blurNsfw = blurNsfw,
                        gridState = searchGridState,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        onSearch = { viewModel.executeSearchQuery() },
                        onFilterChange = { viewModel.setSearchFilterState(it) },
                        onSelectSource = { viewModel.setSearchSource(it) },
                        onOpenPicker = { viewModel.openSourcePicker("search") },
                        onRefresh = { viewModel.refreshSearch() },
                        onRetry = { viewModel.refreshSearch() },
                        onResetFilters = {
                            viewModel.setSearchQuery("")
                            viewModel.setSearchFilterState(FilterState())
                            viewModel.executeSearchQuery()
                        },
                        onOpenLightbox = { index ->
                            viewModel.openLightbox(index, searchState.mediaItems)
                        },
                        onDownload = { viewModel.promptDownload(it) },
                        onTagClick = { tag ->
                            viewModel.setSearchQuery(tag)
                            viewModel.executeSearchQuery()
                        }
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        sources = sources,
                        theme = theme,
                        onThemeChange = { viewModel.setTheme(it) },
                        blurNsfw = blurNsfw,
                        onBlurNsfwChange = { viewModel.setBlurNsfw(it) },
                        thumbnailQuality = thumbnailQuality,
                        onThumbnailQualityChange = { viewModel.setThumbnailQuality(it) },
                        loopVideo = loopVideo,
                        onLoopVideoChange = { viewModel.setLoopVideoPlayback(it) },
                        onAddSourceClick = { viewModel.openAddSourceDialog() },
                        onDeleteSource = { viewModel.deleteSource(it) }
                    )
                }
            }
        }
    }

    // Download Configuration Prompt Dialog
    downloadTargetItem?.let { targetItem ->
        DownloadConfigDialog(
            item = targetItem,
            onConfirm = { quality ->
                viewModel.downloadMediaWithQuality(targetItem, quality)
            },
            onDismiss = {
                viewModel.dismissDownloadPrompt()
            }
        )
    }

    // Home Feed Settings / Filter Bottom Sheet
    if (isHomeSettingsOpen) {
        HomeFeedSettingsSheet(
            sources = sources,
            currentSource = homeState.activeSource,
            currentFilter = homeState.filterState,
            onApply = { newSource, newFilter ->
                viewModel.setHomeSourceAndFilter(newSource, newFilter)
                isHomeSettingsOpen = false
            },
            onDismiss = { isHomeSettingsOpen = false }
        )
    }

    // Source Picker Bottom Sheet (Used by Search source switcher)
    if (isSourcePickerOpen) {
        val currentActiveSource = if (currentTab == MainTab.SEARCH) searchState.activeSource else homeState.activeSource
        SourcePickerBottomSheet(
            sources = sources,
            activeSource = currentActiveSource,
            onSelectSource = {
                if (currentTab == MainTab.SEARCH) {
                    viewModel.setSearchSource(it)
                } else {
                    viewModel.setHomeSource(it)
                }
            },
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

    // Fullscreen Swipeable Lightbox Viewer Overlay
    lightboxIndex?.let { index ->
        val currentItem = lightboxItems.getOrNull(index)
        if (currentItem != null) {
            LightboxViewer(
                item = currentItem,
                currentIndex = index,
                totalCount = lightboxItems.size,
                itemsList = lightboxItems,
                onIndexChanged = { newIdx -> viewModel.setLightboxIndex(newIdx) },
                onPrevious = { viewModel.previousLightboxItem() },
                onNext = { viewModel.nextLightboxItem() },
                onClose = { viewModel.closeLightbox() },
                onDownload = { viewModel.promptDownload(currentItem) },
                onTagClick = { tag ->
                    currentTab = MainTab.SEARCH
                    viewModel.setSearchQuery(tag)
                    viewModel.executeSearchQuery()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeState: FeedState,
    thumbnailQuality: ThumbnailQuality,
    blurNsfw: Boolean = false,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    onOpenFeedSettings: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onResetFilters: () -> Unit,
    onOpenLightbox: (Int) -> Unit,
    onDownload: (MediaItem) -> Unit,
    onTagClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Home Header: App Branding + Unified Feed Settings / Filter Button
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

            // Unified Filter / Feed Settings Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenFeedSettings)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("home_feed_settings_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Feed settings",
                        tint = NeonIndigoLight,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = homeState.activeSource.name,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (homeState.filterState.activeFilterCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NeonIndigo)
                        )
                    }
                }
            }
        }

        // Discovery Feed with Pull-To-Refresh
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            PullToRefreshBox(
                isRefreshing = homeState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    homeState.isLoading && !homeState.isRefreshing -> {
                        LoadingSkeletonGrid()
                    }

                    homeState.errorMessage != null -> {
                        ErrorStateView(
                            errorMessage = homeState.errorMessage,
                            onRetry = onRetry
                        )
                    }

                    homeState.mediaItems.isEmpty() -> {
                        EmptyStateView(
                            hasFilters = homeState.filterState.activeFilterCount > 0,
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
                                items = homeState.mediaItems,
                                key = { index, item -> "${item.id}_home_$index" }
                            ) { index, item ->
                                MediaCard(
                                    item = item,
                                    quality = thumbnailQuality,
                                    blurNsfw = blurNsfw,
                                    onClick = { onOpenLightbox(index) },
                                    onDownloadClick = { onDownload(item) },
                                    onTagClick = onTagClick
                                )
                            }

                            if (homeState.isLoadingMore) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchState: FeedState,
    sources: List<MediaSourceConfig>,
    thumbnailQuality: ThumbnailQuality,
    blurNsfw: Boolean = false,
    gridState: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFilterChange: (FilterState) -> Unit,
    onSelectSource: (MediaSourceConfig) -> Unit,
    onOpenPicker: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onResetFilters: () -> Unit,
    onOpenLightbox: (Int) -> Unit,
    onDownload: (MediaItem) -> Unit,
    onTagClick: (String) -> Unit
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isFilterSheetOpen by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search & Filter Header (Clean, Consolidated)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Top Row: Collapsible Search Input / Search Trigger
            if (isSearchExpanded) {
                OutlinedTextField(
                    value = searchState.searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("search_text_input"),
                    placeholder = {
                        Text(
                            text = "Search tags, artists, concepts...",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NeonIndigoLight,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        onQueryChange("")
                                        onSearch()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    isSearchExpanded = false
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Collapse search",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedBorderColor = NeonIndigo,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        onSearch()
                    })
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .clickable { isSearchExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("collapsed_search_trigger"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NeonIndigoLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        if (searchState.searchQuery.isNotBlank()) {
                            Text(
                                text = searchState.searchQuery,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Search tags, artists, concepts...",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (searchState.searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onQueryChange("")
                                onSearch()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Second Row: Source Selector + Consolidated Filter Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Source Selector (with clear dropdown icon)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenPicker)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("search_source_selector"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SOURCE",
                            color = TextTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = searchState.activeSource.name,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select source",
                        tint = NeonIndigoLight,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Consolidated Filter Action
                Button(
                    onClick = { isFilterSheetOpen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (searchState.filterState.activeFilterCount > 0) NeonIndigo else DarkBackground
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (searchState.filterState.activeFilterCount > 0) NeonIndigoLight else CardBorder
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("search_filter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Open filters",
                        tint = if (searchState.filterState.activeFilterCount > 0) TextPrimary else NeonIndigoLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (searchState.filterState.activeFilterCount > 0) "Filter (${searchState.filterState.activeFilterCount})" else "Filter",
                        color = if (searchState.filterState.activeFilterCount > 0) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Search Results Feed with Pull-To-Refresh
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            PullToRefreshBox(
                isRefreshing = searchState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    searchState.isLoading && !searchState.isRefreshing -> {
                        LoadingSkeletonGrid()
                    }

                    searchState.errorMessage != null -> {
                        ErrorStateView(
                            errorMessage = searchState.errorMessage,
                            onRetry = onRetry
                        )
                    }

                    searchState.mediaItems.isEmpty() -> {
                        EmptyStateView(
                            hasFilters = searchState.filterState.activeFilterCount > 0 || searchState.searchQuery.isNotBlank(),
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
                                items = searchState.mediaItems,
                                key = { index, item -> "${item.id}_search_$index" }
                            ) { index, item ->
                                MediaCard(
                                    item = item,
                                    quality = thumbnailQuality,
                                    blurNsfw = blurNsfw,
                                    onClick = { onOpenLightbox(index) },
                                    onDownloadClick = { onDownload(item) },
                                    onTagClick = onTagClick
                                )
                            }

                            if (searchState.isLoadingMore) {
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

    // Consolidated Search Filter Sheet
    if (isFilterSheetOpen) {
        SearchFilterSheet(
            activeSource = searchState.activeSource,
            filterState = searchState.filterState,
            currentQuery = searchState.searchQuery,
            onApply = { newFilter, newQuery ->
                onFilterChange(newFilter)
                if (newQuery != searchState.searchQuery) {
                    onQueryChange(newQuery)
                    onSearch()
                }
                isFilterSheetOpen = false
            },
            onDismiss = { isFilterSheetOpen = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFilterSheet(
    activeSource: MediaSourceConfig,
    filterState: FilterState,
    currentQuery: String,
    onApply: (FilterState, String) -> Unit,
    onDismiss: () -> Unit
) {
    val supportedTypes = remember(activeSource) { activeSource.getSupportedMediaTypes() }
    var selectedType by remember {
        mutableStateOf(if (filterState.mediaType in supportedTypes) filterState.mediaType else MediaType.ALL)
    }
    var selectedRating by remember { mutableStateOf(filterState.rating) }
    var selectedSort by remember { mutableStateOf(filterState.sort) }
    var selectedQuery by remember { mutableStateOf(currentQuery) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search Filters",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                TextButton(
                    onClick = {
                        selectedType = MediaType.ALL
                        selectedRating = MediaRating.ALL
                        selectedSort = SortOption.LATEST
                        selectedQuery = ""
                    }
                ) {
                    Text("Reset All", color = NeonIndigoLight, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Media Type Section
            FilterSectionHeader(title = "Media Type")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                supportedTypes.forEach { type ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonIndigo else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) NeonIndigoLight else CardBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedType = type }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("filter_type_${type.name}")
                    ) {
                        Text(
                            text = type.label,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Rating Section
            FilterSectionHeader(title = "Content Rating")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaRating.values().filter { it != MediaRating.UNKNOWN }.forEach { rating ->
                    val isSelected = selectedRating == rating
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonIndigo else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) NeonIndigoLight else CardBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedRating = rating }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("filter_rating_${rating.name}")
                    ) {
                        Text(
                            text = rating.label,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tag Presets
            FilterSectionHeader(title = "Tag Presets")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PRESET_TAGS.forEach { tag ->
                    val isSelected = selectedQuery.equals(tag, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonIndigo else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) NeonIndigoLight else CardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                selectedQuery = if (isSelected) "" else tag
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("filter_tag_$tag")
                    ) {
                        Text(
                            text = "#$tag",
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sort Order
            FilterSectionHeader(title = "Sort Order")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOption.values().forEach { sort ->
                    val isSelected = selectedSort == sort
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonIndigo else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) NeonIndigoLight else CardBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedSort = sort }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sort.label,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply Button
            Button(
                onClick = {
                    onApply(
                        filterState.copy(
                            mediaType = selectedType,
                            rating = selectedRating,
                            sort = selectedSort
                        ),
                        selectedQuery
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_search_filters_button")
            ) {
                Text("Apply Filters", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedSettingsSheet(
    sources: List<MediaSourceConfig>,
    currentSource: MediaSourceConfig,
    currentFilter: FilterState,
    onApply: (MediaSourceConfig, FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSource by remember { mutableStateOf(currentSource) }
    val supportedTypes = remember(selectedSource) { selectedSource.getSupportedMediaTypes() }
    var selectedType by remember(selectedSource) {
        mutableStateOf(if (currentFilter.mediaType in supportedTypes) currentFilter.mediaType else MediaType.ALL)
    }
    var selectedRating by remember { mutableStateOf(currentFilter.rating) }
    var selectedSort by remember { mutableStateOf(currentFilter.sort) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Feed Settings (Home)",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                TextButton(
                    onClick = {
                        selectedSource = MediaSourceConfig.BUILT_IN_SAFEBOORU
                        selectedType = MediaType.ALL
                        selectedRating = MediaRating.ALL
                        selectedSort = SortOption.LATEST
                    }
                ) {
                    Text("Reset", color = NeonIndigoLight, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Source Selector Section
            FilterSectionHeader(title = "Source")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sources.forEach { source ->
                    val isSelected = selectedSource.id == source.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonIndigo else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) NeonIndigoLight else CardBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedSource = source
                                val newSupported = source.getSupportedMediaTypes()
                                if (selectedType !in newSupported) {
                                    selectedType = MediaType.ALL
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("home_source_option_${source.id}")
                    ) {
                        Text(
                            text = source.name,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Media Type Section
            FilterSectionHeader(title = "Media Type")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                supportedTypes.forEach { type ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonIndigo else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) NeonIndigoLight else CardBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedType = type }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("home_type_option_${type.name}")
                    ) {
                        Text(
                            text = type.label,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rating Section
            FilterSectionHeader(title = "Content Rating")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaRating.values().filter { it != MediaRating.UNKNOWN }.forEach { rating ->
                    val isSelected = selectedRating == rating
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonIndigo else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) NeonIndigoLight else CardBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedRating = rating }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("home_rating_option_${rating.name}")
                    ) {
                        Text(
                            text = rating.label,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sort Order
            FilterSectionHeader(title = "Sort Order")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOption.values().forEach { sort ->
                    val isSelected = selectedSort == sort
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonIndigo else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) NeonIndigoLight else CardBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedSort = sort }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sort.label,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply Button
            Button(
                onClick = {
                    onApply(
                        selectedSource,
                        currentFilter.copy(
                            mediaType = selectedType,
                            rating = selectedRating,
                            sort = selectedSort
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_home_settings_button")
            ) {
                Text("Apply Feed Settings", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        color = NeonIndigoLight,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun LoadingSkeletonGrid() {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
        modifier = Modifier
            .fillMaxSize()
            .testTag("loading_skeleton_grid")
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
            .verticalScroll(rememberScrollState())
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
            .verticalScroll(rememberScrollState())
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
