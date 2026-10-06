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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.AddToCollectionDialog
import com.example.ui.components.CollectionDetailScreen
import com.example.ui.components.DownloadConfigDialog
import com.example.ui.components.LightboxViewer
import com.example.ui.components.MediaCard
import com.example.ui.components.SavesAndCollectionsScreen
import com.example.ui.components.SourceDiagnosticsDialog
import com.example.ui.theme.RoseBadge

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
    val colorPalette by viewModel.colorPalette.collectAsStateWithLifecycle()
    val blurNsfw by viewModel.blurNsfw.collectAsStateWithLifecycle()
    val thumbnailQuality by viewModel.thumbnailQuality.collectAsStateWithLifecycle()
    val loopVideo by viewModel.loopVideoPlayback.collectAsStateWithLifecycle()

    val lightboxIndex by viewModel.lightboxIndex.collectAsStateWithLifecycle()
    val lightboxItems by viewModel.lightboxItems.collectAsStateWithLifecycle()
    val isAddSourceOpen by viewModel.isAddSourceOpen.collectAsStateWithLifecycle()
    val editingSourceConfig by viewModel.editingSourceConfig.collectAsStateWithLifecycle()
    val diagnosticSource by viewModel.diagnosticSource.collectAsStateWithLifecycle()
    val diagnosticReport by viewModel.diagnosticReport.collectAsStateWithLifecycle()
    val isDiagnosing by viewModel.isDiagnosing.collectAsStateWithLifecycle()
    val isSourcePickerOpen by viewModel.isSourcePickerOpen.collectAsStateWithLifecycle()
    val downloadTargetItem by viewModel.downloadTargetItem.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val savedItems by viewModel.savedItems.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val selectedCollectionId by viewModel.selectedCollectionId.collectAsStateWithLifecycle()
    val addToCollectionItem by viewModel.addToCollectionItem.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var isHomeSettingsOpen by remember { mutableStateOf(false) }
    var isSavesAndCollectionsOpen by remember { mutableStateOf(false) }

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
        containerColor = MaterialTheme.colorScheme.background,
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
            when {
                selectedCollectionId != null -> {
                    val selectedCol = collections.find { it.id == selectedCollectionId }
                    if (selectedCol != null) {
                        CollectionDetailScreen(
                            collection = selectedCol,
                            items = viewModel.getItemsForCollection(selectedCol.id),
                            thumbnailQuality = thumbnailQuality,
                            blurNsfw = blurNsfw,
                            isSaved = { viewModel.isSaved(it) },
                            onToggleSave = { viewModel.toggleSave(it) },
                            onRemoveFromCollection = { colId, mediaId ->
                                val mediaItem = savedItems.find { it.id == mediaId }
                                if (mediaItem != null) {
                                    viewModel.toggleMediaInCollection(colId, mediaItem)
                                }
                            },
                            onRenameCollection = { colId, newName ->
                                viewModel.renameCollection(colId, newName)
                            },
                            onDeleteCollection = { colId ->
                                viewModel.deleteCollection(colId)
                            },
                            onBack = { viewModel.clearSelectedCollection() },
                            onOpenLightbox = { index, itemsList ->
                                viewModel.openLightbox(index, itemsList)
                            }
                        )
                    } else {
                        viewModel.clearSelectedCollection()
                    }
                }
                isSavesAndCollectionsOpen -> {
                    SavesAndCollectionsScreen(
                        savedItems = savedItems,
                        collections = collections,
                        thumbnailQuality = thumbnailQuality,
                        blurNsfw = blurNsfw,
                        isSaved = { viewModel.isSaved(it) },
                        onToggleSave = { viewModel.toggleSave(it) },
                        onCreateCollection = { viewModel.createCollection(it) },
                        onSelectCollection = { viewModel.selectCollection(it) },
                        onBack = { isSavesAndCollectionsOpen = false },
                        onOpenLightbox = { index, itemsList ->
                            viewModel.openLightbox(index, itemsList)
                        }
                    )
                }
                else -> {
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
                                },
                                savedIds = savedIds,
                                onToggleSave = { viewModel.toggleSave(it) }
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
                                },
                                savedIds = savedIds,
                                onToggleSave = { viewModel.toggleSave(it) }
                            )
                        }
                        MainTab.SETTINGS -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            SettingsScreen(
                                sources = sources,
                                theme = theme,
                                onThemeChange = { viewModel.setTheme(it) },
                                colorPalette = colorPalette,
                                onColorPaletteChange = { viewModel.setColorPalette(it) },
                                blurNsfw = blurNsfw,
                                onBlurNsfwChange = { viewModel.setBlurNsfw(it) },
                                thumbnailQuality = thumbnailQuality,
                                onThumbnailQualityChange = { viewModel.setThumbnailQuality(it) },
                                loopVideo = loopVideo,
                                onLoopVideoChange = { viewModel.setLoopVideoPlayback(it) },
                                onAddSourceClick = { viewModel.openAddSourceDialog(null) },
                                onEditSource = { viewModel.openAddSourceDialog(it) },
                                onCopySource = { viewModel.duplicateSource(it) },
                                onTestSource = { viewModel.openSourceDiagnostics(it) },
                                onExportSource = { source ->
                                    val json = viewModel.exportSourceConfig(source)
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("${source.name} Config", json)
                                    clipboard?.setPrimaryClip(clip)
                                },
                                onDeleteSource = { viewModel.deleteSource(it) },
                                savedCount = savedItems.size,
                                collectionsCount = collections.size,
                                onOpenSavesAndCollections = { isSavesAndCollectionsOpen = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // Download Configuration Prompt Dialog
    downloadTargetItem?.let { targetItem ->
        DownloadConfigDialog(
            item = targetItem,
            onConfirm = { quality, customFilename ->
                viewModel.downloadMediaWithQuality(targetItem, quality, customFilename)
            },
            onDismiss = {
                viewModel.dismissDownloadPrompt()
            }
        )
    }

    // Add to Collection Dialog overlay
    addToCollectionItem?.let { item ->
        AddToCollectionDialog(
            mediaItem = item,
            collections = collections,
            onToggleCollection = { colId, media -> viewModel.toggleMediaInCollection(colId, media) },
            onCreateCollection = { name -> viewModel.createCollection(name) },
            onDismiss = { viewModel.closeAddToCollectionDialog() }
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
            onAddNewSource = { viewModel.openAddSourceDialog(null) },
            onDismiss = { viewModel.closeSourcePicker() }
        )
    }

    // Add / Edit Source Dialog (Add Source 2.0 Flow)
    if (isAddSourceOpen) {
        AddSourceDialog(
            initialConfig = editingSourceConfig,
            onDismiss = { viewModel.closeAddSourceDialog() },
            onSave = { newSource ->
                if (editingSourceConfig != null) {
                    viewModel.updateCustomSource(newSource)
                } else {
                    viewModel.addCustomSource(newSource)
                }
            }
        )
    }

    // Source Diagnostics Dialog (from Settings / Diagnostics)
    diagnosticSource?.let { sourceToTest ->
        SourceDiagnosticsDialog(
            source = sourceToTest,
            report = diagnosticReport,
            isLoading = isDiagnosing,
            onRunDiagnostics = { viewModel.runDiagnosticsForSource(sourceToTest) },
            onEditSource = { source -> viewModel.openEditSourceDialog(source) },
            onDismiss = { viewModel.dismissSourceDiagnostics() }
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
                loopVideo = loopVideo,
                onIndexChanged = { newIdx -> viewModel.setLightboxIndex(newIdx) },
                onPrevious = { viewModel.previousLightboxItem() },
                onNext = { viewModel.nextLightboxItem() },
                onClose = { viewModel.closeLightbox() },
                onDownload = { viewModel.promptDownload(currentItem) },
                onTagClick = { tag ->
                    currentTab = MainTab.SEARCH
                    viewModel.setSearchQuery(tag)
                    viewModel.executeSearchQuery()
                },
                isSaved = { viewModel.isSaved(it) },
                onToggleSave = { viewModel.toggleSave(it) },
                onOpenAddToCollection = { viewModel.openAddToCollectionDialog(it) },
                onUndoUnsave = { viewModel.undoLastUnsave() }
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
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == MainTab.HOME,
            onClick = { onTabSelected(MainTab.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_home")
        )
        NavigationBarItem(
            selected = currentTab == MainTab.SEARCH,
            onClick = { onTabSelected(MainTab.SEARCH) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_tab_search")
        )
        NavigationBarItem(
            selected = currentTab == MainTab.SETTINGS,
            onClick = { onTabSelected(MainTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
    onTagClick: (String) -> Unit,
    savedIds: Set<String> = emptySet(),
    onToggleSave: ((MediaItem) -> Unit)? = null
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
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Artflux",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            // Unified Filter / Feed Settings Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenFeedSettings)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("home_feed_settings_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Feed settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = homeState.activeSource.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (homeState.filterState.activeFilterCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
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
                                    isSaved = savedIds.contains(item.id),
                                    onToggleSave = { onToggleSave?.invoke(item) },
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
                                            color = MaterialTheme.colorScheme.primary,
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
    onTagClick: (String) -> Unit,
    savedIds: Set<String> = emptySet(),
    onToggleSave: ((MediaItem) -> Unit)? = null
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isFilterSheetOpen by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search & Filter Header (Clean, Aligned Single Row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input / Trigger taking weight(1f)
            if (isSearchExpanded) {
                androidx.compose.foundation.text.BasicTextField(
                    value = searchState.searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .focusRequester(focusRequester)
                        .testTag("search_text_input"),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        onSearch()
                    }),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchState.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search tags, artists...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        style = TextStyle(
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                            if (searchState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        onQueryChange("")
                                        onSearch()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    isSearchExpanded = false
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Collapse search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { isSearchExpanded = true }
                        .padding(horizontal = 14.dp),
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        if (searchState.searchQuery.isNotBlank()) {
                            Text(
                                text = searchState.searchQuery,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "Search tags, artists, concepts...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Consolidated Filter Action Button aligned beside search bar
            Button(
                onClick = { isFilterSheetOpen = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (searchState.filterState.activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (searchState.filterState.activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("search_filter_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Open filters",
                    tint = if (searchState.filterState.activeFilterCount > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (searchState.filterState.activeFilterCount > 0) "Filter (${searchState.filterState.activeFilterCount})" else "Filter",
                    color = if (searchState.filterState.activeFilterCount > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
                                    isSaved = savedIds.contains(item.id),
                                    onToggleSave = { onToggleSave?.invoke(item) },
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
                                            color = MaterialTheme.colorScheme.primary,
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
            sources = sources,
            activeSource = searchState.activeSource,
            filterState = searchState.filterState,
            currentQuery = searchState.searchQuery,
            onApply = { newSource, newFilter, newQuery ->
                if (newSource.id != searchState.activeSource.id) {
                    onSelectSource(newSource)
                }
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
    sources: List<MediaSourceConfig>,
    activeSource: MediaSourceConfig,
    filterState: FilterState,
    currentQuery: String,
    onApply: (MediaSourceConfig, FilterState, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSource by remember { mutableStateOf(activeSource) }
    val supportedTypes = remember(selectedSource) { selectedSource.getSupportedMediaTypes() }
    var selectedType by remember(selectedSource) {
        mutableStateOf(if (filterState.mediaType in supportedTypes) filterState.mediaType else MediaType.ALL)
    }
    var selectedRating by remember { mutableStateOf(filterState.rating) }
    var selectedSort by remember { mutableStateOf(filterState.sort) }
    var selectedQuery by remember { mutableStateOf(currentQuery) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                TextButton(
                    onClick = {
                        selectedSource = MediaSourceConfig.BUILT_IN_SAFEBOORU
                        selectedType = MediaType.ALL
                        selectedRating = MediaRating.ALL
                        selectedSort = SortOption.LATEST
                        selectedQuery = ""
                    }
                ) {
                    Text("Reset All", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Source Selector Section inside Search Filter Sheet
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
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedSource = source
                                val newSupported = source.getSupportedMediaTypes()
                                if (selectedType !in newSupported) {
                                    selectedType = MediaType.ALL
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("filter_source_${source.id}")
                    ) {
                        Text(
                            text = source.name,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isWaifu = selectedSource.apiUrl.contains("waifu.im") || selectedSource.id.contains("waifu")

            // Media Type Section (hidden if source supports only ALL media type)
            if (supportedTypes.size > 1) {
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
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { selectedType = type }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("filter_type_${type.name}")
                        ) {
                            Text(
                                text = type.label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Content Rating Section
            FilterSectionHeader(title = "Content Rating")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val availableRatings = if (isWaifu) listOf(MediaRating.ALL, MediaRating.SAFE, MediaRating.ADULT) else MediaRating.values().filter { it != MediaRating.UNKNOWN }
                availableRatings.forEach { rating ->
                    val isSelected = selectedRating == rating
                    val ratingDisplay = if (isWaifu) {
                        when (rating) {
                            MediaRating.SAFE -> "SFW"
                            MediaRating.ADULT -> "NSFW"
                            else -> "All"
                        }
                    } else rating.label

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { selectedRating = rating }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("filter_rating_${rating.name}")
                    ) {
                        Text(
                            text = ratingDisplay,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable {
                                selectedQuery = if (isSelected) "" else tag
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("filter_tag_$tag")
                    ) {
                        Text(
                            text = "#$tag",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                val availableSorts: List<SortOption> = if (isWaifu) listOf(SortOption.LATEST, SortOption.RANDOM) else SortOption.values().toList()
                availableSorts.forEach { sort ->
                    val isSelected = selectedSort == sort
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { selectedSort = sort }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sort.label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        filterState.copy(
                            mediaType = selectedType,
                            rating = selectedRating,
                            sort = selectedSort
                        ),
                        selectedQuery
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_search_filters_button")
            ) {
                Text("Apply Filters", color = MaterialTheme.colorScheme.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        containerColor = MaterialTheme.colorScheme.surface,
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                    Text("Reset", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Source Selector Dropdown Menu
            FilterSectionHeader(title = "Source")
            var isSourceDropdownExpanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable { isSourceDropdownExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("home_source_dropdown_anchor"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = selectedSource.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (selectedSource.isBuiltIn) "Built-in Source" else "Custom Source (${selectedSource.apiUrl})",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select source",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = isSourceDropdownExpanded,
                    onDismissRequest = { isSourceDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                        .testTag("home_source_dropdown_menu")
                ) {
                    sources.forEach { source ->
                        val isSelected = selectedSource.id == source.id
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = source.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = if (source.isBuiltIn) "Built-in" else "Custom",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedSource = source
                                val newSupported = source.getSupportedMediaTypes()
                                if (selectedType !in newSupported) {
                                    selectedType = MediaType.ALL
                                }
                                isSourceDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("home_source_option_${source.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val isHomeWaifu = selectedSource.apiUrl.contains("waifu.im") || selectedSource.id.contains("waifu")

            // Media Type Section
            if (supportedTypes.size > 1) {
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
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { selectedType = type }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("home_type_option_${type.name}")
                        ) {
                            Text(
                                text = type.label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Rating Section
            FilterSectionHeader(title = "Content Rating")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val availableRatings = if (isHomeWaifu) listOf(MediaRating.ALL, MediaRating.SAFE, MediaRating.ADULT) else MediaRating.values().filter { it != MediaRating.UNKNOWN }
                availableRatings.forEach { rating ->
                    val isSelected = selectedRating == rating
                    val ratingDisplay = if (isHomeWaifu) {
                        when (rating) {
                            MediaRating.SAFE -> "SFW"
                            MediaRating.ADULT -> "NSFW"
                            else -> "All"
                        }
                    } else rating.label

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { selectedRating = rating }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("home_rating_option_${rating.name}")
                    ) {
                        Text(
                            text = ratingDisplay,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                val availableSorts: List<SortOption> = if (isHomeWaifu) listOf(SortOption.LATEST, SortOption.RANDOM) else SortOption.values().toList()
                availableSorts.forEach { sort ->
                    val isSelected = selectedSort == sort
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { selectedSort = sort }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sort.label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_home_settings_button")
            ) {
                Text("Apply Feed Settings", color = MaterialTheme.colorScheme.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
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
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Media Found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (hasFilters) "No results match your active search and filter criteria." else "This source returned no items for the current page.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            if (hasFilters) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Search & Filters", color = Color.White)
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry Connection", color = Color.White)
            }
        }
    }
}

val PRESET_TAGS = listOf("1girl", "landscape", "scenery", "original", "cat_ears", "solo", "monochrome", "cyberpunk", "aesthetic", "sky")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcePickerBottomSheet(
    sources: List<MediaSourceConfig>,
    activeSource: MediaSourceConfig,
    onSelectSource: (MediaSourceConfig) -> Unit,
    onDeleteSource: (String) -> Unit,
    onAddNewSource: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Media Source",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                TextButton(
                    onClick = {
                        onDismiss()
                        onAddNewSource()
                    },
                    modifier = Modifier.testTag("add_source_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Source",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Source", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sources, key = { it.id }) { source ->
                    val isSelected = source.id == activeSource.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSelectSource(source)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("source_picker_item_${source.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = source.name,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                if (source.isBuiltIn) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Built-in",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (source.description.isNotBlank()) {
                                Text(
                                    text = source.description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!source.isBuiltIn) {
                            IconButton(
                                onClick = { onDeleteSource(source.id) },
                                modifier = Modifier.size(32.dp).testTag("delete_source_${source.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete source",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
