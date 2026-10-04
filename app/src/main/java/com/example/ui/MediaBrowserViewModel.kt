package com.example.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaApiClient
import com.example.data.SourceRepository
import com.example.model.FilterState
import com.example.model.MediaItem
import com.example.model.MediaRating
import com.example.model.MediaSourceConfig
import com.example.model.MediaType
import com.example.model.Orientation
import com.example.model.SortOption
import com.example.util.QueryBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediaBrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SourceRepository(application.applicationContext)
    private val apiClient = MediaApiClient()

    val sources: StateFlow<List<MediaSourceConfig>> = repository.sources

    private val _activeSource = MutableStateFlow<MediaSourceConfig>(MediaSourceConfig.BUILT_IN_SAFEBOORU)
    val activeSource: StateFlow<MediaSourceConfig> = _activeSource.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _rawItems = MutableStateFlow<List<MediaItem>>(emptyList())

    // Client-side filtering & sorting applied to raw loaded items
    val mediaItems: StateFlow<List<MediaItem>> = combine(_rawItems, _filterState) { items, filters ->
        var result = items

        val totalRaw = items.size
        val rawGifs = items.count { it.mediaType == MediaType.GIF }
        val rawVideos = items.count { it.mediaType == MediaType.VIDEO }
        Log.d("ArtfluxDebug", "Filter applied: rawTotal=$totalRaw (GIFs=$rawGifs, Videos=$rawVideos), activeFilter=${filters.mediaType}")

        // 1. Rating filter
        if (filters.rating != MediaRating.ALL) {
            result = result.filter { it.rating == filters.rating }
        }

        // 2. Media Type filter
        if (filters.mediaType != MediaType.ALL) {
            result = result.filter { item ->
                when (filters.mediaType) {
                    MediaType.IMAGE -> item.mediaType == MediaType.IMAGE
                    MediaType.GIF -> item.mediaType == MediaType.GIF
                    MediaType.VIDEO -> item.mediaType == MediaType.VIDEO
                    else -> true
                }
            }
        }

        // 3. Orientation filter
        if (filters.orientation != Orientation.ALL) {
            result = result.filter { item ->
                val w = item.width ?: 1
                val h = item.height ?: 1
                when (filters.orientation) {
                    Orientation.PORTRAIT -> h > w
                    Orientation.LANDSCAPE -> w > h
                    Orientation.SQUARE -> (w.toFloat() / h.toFloat()) in 0.9f..1.1f
                    else -> true
                }
            }
        }

        // 4. Sort order
        result = when (filters.sort) {
            SortOption.LATEST -> result
            SortOption.POPULAR -> result.sortedByDescending { it.tags.size }
            SortOption.TOP_RATED -> result.sortedBy { it.rating == MediaRating.SAFE }
            SortOption.RANDOM -> result.shuffled()
        }

        Log.d("ArtfluxDebug", "Remaining after active filter (${filters.mediaType}): ${result.size} items")

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lightboxIndex = MutableStateFlow<Int?>(null)
    val lightboxIndex: StateFlow<Int?> = _lightboxIndex.asStateFlow()

    private val _isAddSourceOpen = MutableStateFlow(false)
    val isAddSourceOpen: StateFlow<Boolean> = _isAddSourceOpen.asStateFlow()

    private val _isSourcePickerOpen = MutableStateFlow(false)
    val isSourcePickerOpen: StateFlow<Boolean> = _isSourcePickerOpen.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var currentPage = 1
    private var hasReachedEnd = false

    init {
        // Load initial data with Curated Art Showcase
        loadSourceData(source = _activeSource.value, query = "", isRefresh = true)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun executeSearch() {
        loadSourceData(source = _activeSource.value, query = _searchQuery.value, isRefresh = true)
    }

    fun setActiveSource(source: MediaSourceConfig) {
        if (_activeSource.value.id == source.id) return
        _activeSource.value = source
        // If current mediaType filter is not supported by the new source, reset to ALL
        val supportedTypes = source.getSupportedMediaTypes()
        if (_filterState.value.mediaType !in supportedTypes) {
            _filterState.value = _filterState.value.copy(mediaType = MediaType.ALL)
        }
        _rawItems.value = emptyList()
        loadSourceData(source = source, query = _searchQuery.value, isRefresh = true)
    }

    fun setFilterState(newFilters: FilterState) {
        val supportedTypes = _activeSource.value.getSupportedMediaTypes()
        val sanitizedFilters = if (newFilters.mediaType !in supportedTypes) {
            newFilters.copy(mediaType = MediaType.ALL)
        } else {
            newFilters
        }

        val oldFilters = _filterState.value
        val oldEffectiveQuery = QueryBuilder.buildEffectiveQuery(
            userQuery = _searchQuery.value,
            source = _activeSource.value,
            mediaType = oldFilters.mediaType,
            rating = oldFilters.rating
        )
        val newEffectiveQuery = QueryBuilder.buildEffectiveQuery(
            userQuery = _searchQuery.value,
            source = _activeSource.value,
            mediaType = sanitizedFilters.mediaType,
            rating = sanitizedFilters.rating
        )

        _filterState.value = sanitizedFilters

        // If the source-specific effective query changed, trigger a fresh page 1 load
        if (oldEffectiveQuery != newEffectiveQuery) {
            _rawItems.value = emptyList()
            loadSourceData(source = _activeSource.value, query = _searchQuery.value, isRefresh = true)
        }
    }

    fun loadSourceData(
        source: MediaSourceConfig = _activeSource.value,
        query: String = _searchQuery.value,
        isRefresh: Boolean = false
    ) {
        if (isRefresh) {
            currentPage = 1
            hasReachedEnd = false
            _isLoading.value = true
            _errorMessage.value = null
        } else {
            if (_isLoading.value || _isLoadingMore.value || hasReachedEnd) return
            _isLoadingMore.value = true
        }

        viewModelScope.launch {
            val pageToFetch = if (isRefresh) 1 else currentPage
            val effectiveQuery = QueryBuilder.buildEffectiveQuery(
                userQuery = query,
                source = source,
                mediaType = _filterState.value.mediaType,
                rating = _filterState.value.rating
            )
            val result = apiClient.fetchMedia(source, effectiveQuery, pageToFetch)

            _isLoading.value = false
            _isLoadingMore.value = false

            result.onSuccess { newItems ->
                if (isRefresh) {
                    _rawItems.value = newItems
                    if (newItems.isEmpty()) {
                        // Empty result
                    }
                } else {
                    if (newItems.isEmpty()) {
                        hasReachedEnd = true
                    } else {
                        // Append unique items
                        val existingIds = _rawItems.value.map { it.id }.toSet()
                        val unique = newItems.filter { it.id !in existingIds }
                        _rawItems.value = _rawItems.value + unique
                    }
                }
                currentPage++

                // Client-side fallback pagination: if a specific media type is requested
                // and the current filtered result is empty, auto-fetch next page (up to page 4)
                val targetType = _filterState.value.mediaType
                if (targetType != MediaType.ALL && newItems.isNotEmpty() && currentPage <= 4) {
                    val hasMatch = _rawItems.value.any { it.mediaType == targetType }
                    if (!hasMatch && !hasReachedEnd) {
                        loadSourceData(source = source, query = query, isRefresh = false)
                    }
                }
            }.onFailure { error ->
                if (isRefresh) {
                    _errorMessage.value = error.message ?: "Failed to connect to source."
                } else {
                    _snackbarMessage.value = "Could not load more items: ${error.message}"
                }
            }
        }
    }

    fun loadNextPage() {
        if (!hasReachedEnd && !_isLoading.value && !_isLoadingMore.value) {
            loadSourceData(isRefresh = false)
        }
    }

    fun addCustomSource(source: MediaSourceConfig) {
        repository.addSource(source)
        setActiveSource(source)
        _isAddSourceOpen.value = false
        _snackbarMessage.value = "Source '${source.name}' added successfully!"
    }

    fun deleteSource(sourceId: String) {
        repository.deleteSource(sourceId)
        if (_activeSource.value.id == sourceId) {
            val fallback = repository.sources.value.firstOrNull { it.isBuiltIn }
                ?: MediaSourceConfig.BUILT_IN_SAFEBOORU
            setActiveSource(fallback)
        }
        _snackbarMessage.value = "Source removed."
    }

    fun openAddSourceDialog() {
        _isAddSourceOpen.value = true
    }

    fun closeAddSourceDialog() {
        _isAddSourceOpen.value = false
    }

    fun openSourcePicker() {
        _isSourcePickerOpen.value = true
    }

    fun closeSourcePicker() {
        _isSourcePickerOpen.value = false
    }

    fun openLightbox(index: Int) {
        _lightboxIndex.value = index
    }

    fun closeLightbox() {
        _lightboxIndex.value = null
    }

    fun nextLightboxItem() {
        val current = _lightboxIndex.value ?: return
        val count = mediaItems.value.size
        if (current < count - 1) {
            _lightboxIndex.value = current + 1
        }
    }

    fun previousLightboxItem() {
        val current = _lightboxIndex.value ?: return
        if (current > 0) {
            _lightboxIndex.value = current - 1
        }
    }

    fun downloadImage(item: MediaItem) {
        try {
            val context = getApplication<Application>().applicationContext
            val uri = Uri.parse(item.imageUrl)
            val request = DownloadManager.Request(uri).apply {
                setTitle(item.title)
                setDescription("Downloading from ${item.sourceName}")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_PICTURES,
                    "MediaBrowser_${System.currentTimeMillis()}.jpg"
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            downloadManager?.enqueue(request)
            _snackbarMessage.value = "Download started for '${item.title}'"
        } catch (e: Exception) {
            _snackbarMessage.value = "Download error: ${e.message}"
        }
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }
}
