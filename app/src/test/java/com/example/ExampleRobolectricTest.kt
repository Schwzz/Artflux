package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MediaApiClient
import com.example.model.FilterState
import com.example.model.MediaItem
import com.example.model.MediaRating
import com.example.model.MediaSourceConfig
import com.example.model.MediaType
import com.example.ui.MediaBrowserViewModel
import com.example.util.QueryBuilder
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Artflux", appName)
  }

  // --- 1. Source Configuration Tests ---

  @Test
  fun testBuiltInSourcesListContainsSafebooruDanbooruAndYandere() {
    val defaults = MediaSourceConfig.DEFAULT_SOURCES
    assertEquals(3, defaults.size)

    val safebooru = defaults[0]
    assertEquals("Safebooru", safebooru.name)
    assertTrue(safebooru.isBuiltIn)
    assertTrue(safebooru.apiUrl.contains("safebooru.org"))
    assertEquals("animated", safebooru.gifQueryTag)
    assertEquals("", safebooru.videoQueryTag)
    assertEquals("rating:general", safebooru.safeRatingTag)
    assertEquals("", safebooru.suggestiveRatingTag)
    assertEquals("", safebooru.adultRatingTag)

    val danbooru = defaults[1]
    assertEquals("Danbooru", danbooru.name)
    assertTrue(danbooru.isBuiltIn)
    assertTrue(danbooru.apiUrl.contains("danbooru.donmai.us"))
    assertEquals("animated_gif", danbooru.gifQueryTag)
    assertEquals("webm", danbooru.videoQueryTag)
    assertEquals("rating:g", danbooru.safeRatingTag)
    assertEquals("rating:s,q", danbooru.suggestiveRatingTag)
    assertEquals("rating:e", danbooru.adultRatingTag)

    val yandere = defaults[2]
    assertEquals("Yande.re", yandere.name)
    assertTrue(yandere.isBuiltIn)
    assertTrue(yandere.apiUrl.contains("yande.re"))
    assertEquals("", yandere.gifQueryTag)
    assertEquals("", yandere.videoQueryTag)
    assertEquals("rating:s", yandere.safeRatingTag)
    assertEquals("rating:q", yandere.suggestiveRatingTag)
    assertEquals("rating:e", yandere.adultRatingTag)
  }

  @Test
  fun testCustomSourcesSerializationAndLegacyDeserialization() {
    // 1. Serialization with all fields
    val config = MediaSourceConfig(
      name = "Custom Booru",
      apiUrl = "https://custom.booru.org/posts.json",
      searchParam = "tags",
      pageParam = "page",
      imageUrlField = "file_url",
      gifQueryTag = "animated",
      videoQueryTag = "mp4",
      safeRatingTag = "rating:safe",
      suggestiveRatingTag = "rating:questionable",
      adultRatingTag = "rating:explicit"
    )

    val json = config.toJson()
    val restored = MediaSourceConfig.fromJson(json)

    assertEquals("Custom Booru", restored.name)
    assertEquals("animated", restored.gifQueryTag)
    assertEquals("mp4", restored.videoQueryTag)
    assertEquals("rating:safe", restored.safeRatingTag)
    assertEquals("rating:questionable", restored.suggestiveRatingTag)
    assertEquals("rating:explicit", restored.adultRatingTag)

    // 2. Legacy JSON without query tags
    val legacyJson = JSONObject("""
      {
        "id": "legacy_source",
        "name": "Legacy Booru",
        "apiUrl": "https://legacy.booru.org/api",
        "searchParam": "q"
      }
    """.trimIndent())

    val legacyRestored = MediaSourceConfig.fromJson(legacyJson)
    assertEquals("Legacy Booru", legacyRestored.name)
    assertEquals("", legacyRestored.gifQueryTag)
    assertEquals("", legacyRestored.videoQueryTag)
    assertEquals("", legacyRestored.safeRatingTag)
    assertEquals("", legacyRestored.suggestiveRatingTag)
    assertEquals("", legacyRestored.adultRatingTag)
  }

  // --- 2. Rating Filters and Mappings Tests ---

  @Test
  fun testRatingFilterLabels() {
    assertEquals("All Ratings", MediaRating.ALL.label)
    assertEquals("Safe", MediaRating.SAFE.label)
    assertEquals("Suggestive", MediaRating.SUGGESTIVE.label)
    assertEquals("Adult", MediaRating.ADULT.label)
  }

  @Test
  fun testDanbooruRatingParsing() {
    val apiClient = MediaApiClient()
    val danbooru = MediaSourceConfig.BUILT_IN_DANBOORU

    val jsonPayload = """
      [
        {"id": 1, "file_url": "https://cdn.donmai.us/1.jpg", "preview_file_url": "https://cdn.donmai.us/1t.jpg", "rating": "g", "tag_string": "safe_tag"},
        {"id": 2, "file_url": "https://cdn.donmai.us/2.jpg", "preview_file_url": "https://cdn.donmai.us/2t.jpg", "rating": "s", "tag_string": "sensitive_tag"},
        {"id": 3, "file_url": "https://cdn.donmai.us/3.jpg", "preview_file_url": "https://cdn.donmai.us/3t.jpg", "rating": "q", "tag_string": "questionable_tag"},
        {"id": 4, "file_url": "https://cdn.donmai.us/4.jpg", "preview_file_url": "https://cdn.donmai.us/4t.jpg", "rating": "e", "tag_string": "explicit_tag"}
      ]
    """.trimIndent()

    val items = apiClient.parseMediaItems(danbooru, jsonPayload)
    assertEquals(4, items.size)
    assertEquals(MediaRating.SAFE, items[0].rating)
    assertEquals(MediaRating.SUGGESTIVE, items[1].rating) // Danbooru 's' is sensitive -> SUGGESTIVE
    assertEquals(MediaRating.SUGGESTIVE, items[2].rating) // Danbooru 'q' is questionable -> SUGGESTIVE
    assertEquals(MediaRating.ADULT, items[3].rating)       // Danbooru 'e' is explicit -> ADULT
  }

  @Test
  fun testYandereRatingParsing() {
    val apiClient = MediaApiClient()
    val yandere = MediaSourceConfig.BUILT_IN_YANDERE

    val jsonPayload = """
      [
        {"id": 10, "file_url": "https://files.yande.re/10.jpg", "preview_url": "https://assets.yande.re/10t.jpg", "rating": "s", "tags": "safe_art"},
        {"id": 20, "file_url": "https://files.yande.re/20.jpg", "preview_url": "https://assets.yande.re/20t.jpg", "rating": "q", "tags": "suggestive_art"},
        {"id": 30, "file_url": "https://files.yande.re/30.jpg", "preview_url": "https://assets.yande.re/30t.jpg", "rating": "e", "tags": "adult_art"}
      ]
    """.trimIndent()

    val items = apiClient.parseMediaItems(yandere, jsonPayload)
    assertEquals(3, items.size)
    assertEquals(MediaRating.SAFE, items[0].rating)       // Yande.re 's' is safe -> SAFE
    assertEquals(MediaRating.SUGGESTIVE, items[1].rating) // Yande.re 'q' is questionable -> SUGGESTIVE
    assertEquals(MediaRating.ADULT, items[2].rating)       // Yande.re 'e' is explicit -> ADULT
  }

  @Test
  fun testSafebooruRatingParsing() {
    val apiClient = MediaApiClient()
    val safebooru = MediaSourceConfig.BUILT_IN_SAFEBOORU

    val jsonPayload = """
      [
        {"id": 100, "directory": "850", "image": "test.jpg", "rating": "general", "tags": "cute", "preview_url": "https://safebooru.org/t.jpg"}
      ]
    """.trimIndent()

    val items = apiClient.parseMediaItems(safebooru, jsonPayload)
    assertEquals(1, items.size)
    assertEquals(MediaRating.SAFE, items[0].rating)
  }

  // --- 3. Source-Aware Rating & Media Type Query Composition ---

  @Test
  fun testDanbooruSourceAwareRatingQueries() {
    val danbooru = MediaSourceConfig.BUILT_IN_DANBOORU

    val safeQuery = QueryBuilder.buildEffectiveQuery(
      userQuery = "miku",
      source = danbooru,
      mediaType = MediaType.ALL,
      rating = MediaRating.SAFE
    )
    assertEquals("miku rating:g", safeQuery)

    val suggQuery = QueryBuilder.buildEffectiveQuery(
      userQuery = "miku",
      source = danbooru,
      mediaType = MediaType.ALL,
      rating = MediaRating.SUGGESTIVE
    )
    assertEquals("miku rating:s,q", suggQuery)

    val adultQuery = QueryBuilder.buildEffectiveQuery(
      userQuery = "miku",
      source = danbooru,
      mediaType = MediaType.ALL,
      rating = MediaRating.ADULT
    )
    assertEquals("miku rating:e", adultQuery)
  }

  @Test
  fun testYandereSourceAwareRatingQueries() {
    val yandere = MediaSourceConfig.BUILT_IN_YANDERE

    val safeQuery = QueryBuilder.buildEffectiveQuery(
      userQuery = "",
      source = yandere,
      mediaType = MediaType.ALL,
      rating = MediaRating.SAFE
    )
    assertEquals("rating:s", safeQuery)

    val suggQuery = QueryBuilder.buildEffectiveQuery(
      userQuery = "",
      source = yandere,
      mediaType = MediaType.ALL,
      rating = MediaRating.SUGGESTIVE
    )
    assertEquals("rating:q", suggQuery)

    val adultQuery = QueryBuilder.buildEffectiveQuery(
      userQuery = "",
      source = yandere,
      mediaType = MediaType.ALL,
      rating = MediaRating.ADULT
    )
    assertEquals("rating:e", adultQuery)
  }

  @Test
  fun testSafebooruAndCustomFallbackWhenRatingNotSupportedServerSide() {
    val safebooru = MediaSourceConfig.BUILT_IN_SAFEBOORU

    // Safebooru has no adult or suggestive tags
    val adultQuery = QueryBuilder.buildEffectiveQuery(
      userQuery = "scenery",
      source = safebooru,
      mediaType = MediaType.ALL,
      rating = MediaRating.ADULT
    )
    assertEquals("scenery", adultQuery) // remains unchanged for server query

    // Custom source with no rating tags
    val custom = MediaSourceConfig(name = "Untagged Source", apiUrl = "https://custom.org/api")
    val customQuery = QueryBuilder.buildEffectiveQuery(
      userQuery = "landscape",
      source = custom,
      mediaType = MediaType.ALL,
      rating = MediaRating.SUGGESTIVE
    )
    assertEquals("landscape", customQuery)
  }

  // --- 4. Query Composition with Search + Media Type + Rating Deduplication ---

  @Test
  fun testSearchPlusMediaTypePlusRatingCombinationWithoutDuplicates() {
    val danbooru = MediaSourceConfig.BUILT_IN_DANBOORU

    // Search + GIF + Adult
    val query1 = QueryBuilder.buildEffectiveQuery(
      userQuery = "hatsune_miku",
      source = danbooru,
      mediaType = MediaType.GIF,
      rating = MediaRating.ADULT
    )
    assertEquals("hatsune_miku animated_gif rating:e", query1)

    // User already typed "animated_gif" and "rating:e"
    val query2 = QueryBuilder.buildEffectiveQuery(
      userQuery = "animated_gif hatsune_miku rating:e",
      source = danbooru,
      mediaType = MediaType.GIF,
      rating = MediaRating.ADULT
    )
    assertEquals("animated_gif hatsune_miku rating:e", query2)

    // Danbooru Video + Suggestive
    val query3 = QueryBuilder.buildEffectiveQuery(
      userQuery = "genshin",
      source = danbooru,
      mediaType = MediaType.VIDEO,
      rating = MediaRating.SUGGESTIVE
    )
    assertEquals("genshin webm rating:s,q", query3)
  }

  @Test
  fun testSupportedMediaTypesPerSource() {
    val safebooru = MediaSourceConfig.BUILT_IN_SAFEBOORU
    val danbooru = MediaSourceConfig.BUILT_IN_DANBOORU
    val yandere = MediaSourceConfig.BUILT_IN_YANDERE

    assertTrue(safebooru.getSupportedMediaTypes().contains(MediaType.GIF))
    assertFalse(safebooru.getSupportedMediaTypes().contains(MediaType.VIDEO))

    assertTrue(danbooru.getSupportedMediaTypes().contains(MediaType.GIF))
    assertTrue(danbooru.getSupportedMediaTypes().contains(MediaType.VIDEO))

    assertFalse(yandere.getSupportedMediaTypes().contains(MediaType.GIF))
    assertFalse(yandere.getSupportedMediaTypes().contains(MediaType.VIDEO))
  }

  // --- 5. Pagination & Source Switching in ViewModel ---

  @Test
  fun testViewModelSourceSwitchingAndPaginationReset() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MediaBrowserViewModel(app)

    // Initially Safebooru
    assertEquals("builtin_safebooru", viewModel.activeSource.value.id)

    // Switch to Danbooru
    val danbooru = MediaSourceConfig.BUILT_IN_DANBOORU
    viewModel.setActiveSource(danbooru)
    assertEquals("builtin_danbooru", viewModel.activeSource.value.id)

    // Apply Adult filter on Danbooru
    viewModel.setFilterState(FilterState(rating = MediaRating.ADULT))
    val effectiveQuery = QueryBuilder.buildEffectiveQuery(
      userQuery = viewModel.searchQuery.value,
      source = viewModel.activeSource.value,
      mediaType = viewModel.filterState.value.mediaType,
      rating = viewModel.filterState.value.rating
    )
    assertEquals("rating:e", effectiveQuery)

    // Switch to Yande.re
    val yandere = MediaSourceConfig.BUILT_IN_YANDERE
    viewModel.setActiveSource(yandere)
    assertEquals("builtin_yandere", viewModel.activeSource.value.id)

    val yandereEffectiveQuery = QueryBuilder.buildEffectiveQuery(
      userQuery = viewModel.searchQuery.value,
      source = viewModel.activeSource.value,
      mediaType = viewModel.filterState.value.mediaType,
      rating = viewModel.filterState.value.rating
    )
    assertEquals("rating:e", yandereEffectiveQuery)
  }

  // --- 6. Media Types Verification ---

  @Test
  fun testMediaTypeEnumIntegrity() {
    val mediaTypes = MediaType.values().map { it.name }
    assertEquals(4, mediaTypes.size)
    assertTrue(mediaTypes.contains("ALL"))
    assertTrue(mediaTypes.contains("IMAGE"))
    assertTrue(mediaTypes.contains("GIF"))
    assertTrue(mediaTypes.contains("VIDEO"))
    assertFalse(mediaTypes.contains("ART"))
  }

  // --- 7. Search & Home State Isolation Tests (Batch 3) ---

  @Test
  fun testSearchAndHomeStateIsolation() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MediaBrowserViewModel(app)

    // 1. Initial State: Home and Search are both on default source (Safebooru)
    assertEquals("builtin_safebooru", viewModel.homeState.value.activeSource.id)
    assertEquals("builtin_safebooru", viewModel.searchState.value.activeSource.id)

    // 2. Set Home source to Danbooru and filter to Adult
    val danbooru = MediaSourceConfig.BUILT_IN_DANBOORU
    viewModel.setHomeSource(danbooru)
    viewModel.setHomeFilterState(FilterState(rating = MediaRating.ADULT, mediaType = MediaType.GIF))

    // Verify Home has Danbooru and Adult + GIF
    assertEquals("builtin_danbooru", viewModel.homeState.value.activeSource.id)
    assertEquals(MediaRating.ADULT, viewModel.homeState.value.filterState.rating)
    assertEquals(MediaType.GIF, viewModel.homeState.value.filterState.mediaType)

    // Verify Search is still Safebooru with default filters (100% Isolated)
    assertEquals("builtin_safebooru", viewModel.searchState.value.activeSource.id)
    assertEquals(MediaRating.ALL, viewModel.searchState.value.filterState.rating)
    assertEquals(MediaType.ALL, viewModel.searchState.value.filterState.mediaType)

    // 3. User configures Search: switch to Yande.re, query "genshin", rating Suggestive
    val yandere = MediaSourceConfig.BUILT_IN_YANDERE
    viewModel.setSearchSource(yandere)
    viewModel.setSearchQuery("genshin")
    viewModel.setSearchFilterState(FilterState(rating = MediaRating.SUGGESTIVE))

    // Verify Search updated
    assertEquals("builtin_yandere", viewModel.searchState.value.activeSource.id)
    assertEquals("genshin", viewModel.searchState.value.searchQuery)
    assertEquals(MediaRating.SUGGESTIVE, viewModel.searchState.value.filterState.rating)

    // Verify Home remains completely intact and unmutated
    assertEquals("builtin_danbooru", viewModel.homeState.value.activeSource.id)
    assertEquals("", viewModel.homeState.value.searchQuery)
    assertEquals(MediaRating.ADULT, viewModel.homeState.value.filterState.rating)
    assertEquals(MediaType.GIF, viewModel.homeState.value.filterState.mediaType)
  }

  // --- 8. Thumbnail Quality Setting & Resolution Resolution Tests ---

  @Test
  fun testThumbnailQualityResolutionPolicy() {
    val item = MediaItem(
      id = "12345",
      title = "Artwork",
      imageUrl = "https://cdn.booru.org/original/12345.jpg",
      thumbnailUrl = "https://cdn.booru.org/preview/12345.jpg",
      sampleUrl = "https://cdn.booru.org/sample/12345.jpg",
      fileExt = "jpg",
      fileSize = 1048576L
    )

    // 360p / 480p should prefer thumbnailUrl
    assertEquals("https://cdn.booru.org/preview/12345.jpg", item.getThumbnailForQuality(com.example.model.ThumbnailQuality.Q360))
    assertEquals("https://cdn.booru.org/preview/12345.jpg", item.getThumbnailForQuality(com.example.model.ThumbnailQuality.Q480))

    // 720p should prefer sampleUrl if available
    assertEquals("https://cdn.booru.org/sample/12345.jpg", item.getThumbnailForQuality(com.example.model.ThumbnailQuality.Q720))

    // 1080p should prefer sampleUrl or imageUrl
    assertEquals("https://cdn.booru.org/sample/12345.jpg", item.getThumbnailForQuality(com.example.model.ThumbnailQuality.Q1080))

    // Fallback when sampleUrl is null
    val itemNoSample = item.copy(sampleUrl = null)
    assertEquals("https://cdn.booru.org/preview/12345.jpg", itemNoSample.getThumbnailForQuality(com.example.model.ThumbnailQuality.Q720))
    assertEquals("https://cdn.booru.org/original/12345.jpg", itemNoSample.getThumbnailForQuality(com.example.model.ThumbnailQuality.Q1080))
  }

  // --- 9. Media Details & Metadata Parsing Tests ---

  @Test
  fun testMediaItemMetadataParsingWithSampleAndFileSize() {
    val apiClient = MediaApiClient()
    val yandere = MediaSourceConfig.BUILT_IN_YANDERE

    val jsonPayload = """
      [
        {
          "id": 999,
          "file_url": "https://files.yande.re/image/999/art.png",
          "preview_url": "https://assets.yande.re/data/preview/999.jpg",
          "sample_url": "https://files.yande.re/sample/999/art_sample.jpg",
          "rating": "s",
          "tags": "hatsune_miku vocaloid",
          "width": 1920,
          "height": 1080,
          "file_size": 2097152,
          "file_ext": "png",
          "author": "IllustratorX"
        }
      ]
    """.trimIndent()

    val items = apiClient.parseMediaItems(yandere, jsonPayload)
    assertEquals(1, items.size)
    val parsed = items[0]

    assertEquals("999", parsed.id)
    assertEquals("https://files.yande.re/image/999/art.png", parsed.imageUrl)
    assertEquals("https://assets.yande.re/data/preview/999.jpg", parsed.thumbnailUrl)
    assertEquals("https://files.yande.re/sample/999/art_sample.jpg", parsed.sampleUrl)
    assertEquals(1920, parsed.width)
    assertEquals(1080, parsed.height)
    assertEquals(2097152L, parsed.fileSize)
    assertEquals("png", parsed.fileExt)
    assertEquals("IllustratorX", parsed.author)
  }

  // --- 10. Preference Persistence Tests (Batch 4 & 5) ---

  @Test
  fun testPreferencesPersistence() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = com.example.data.PreferencesRepository(context)

    // 1. Theme persistence (Default is DARK)
    assertEquals(com.example.model.AppTheme.DARK, prefsRepo.theme.value)

    prefsRepo.setTheme(com.example.model.AppTheme.LIGHT)
    assertEquals(com.example.model.AppTheme.LIGHT, prefsRepo.theme.value)

    // Simulate restart
    val newPrefsRepo = com.example.data.PreferencesRepository(context)
    assertEquals(com.example.model.AppTheme.LIGHT, newPrefsRepo.theme.value)

    newPrefsRepo.setTheme(com.example.model.AppTheme.DARK)
    assertEquals(com.example.model.AppTheme.DARK, newPrefsRepo.theme.value)

    // 2. NSFW Blur persistence (Default is false)
    assertFalse(prefsRepo.blurNsfw.value)
    prefsRepo.setBlurNsfw(true)
    assertTrue(prefsRepo.blurNsfw.value)

    val restartedRepo = com.example.data.PreferencesRepository(context)
    assertTrue(restartedRepo.blurNsfw.value)
    restartedRepo.setBlurNsfw(false)
    assertFalse(restartedRepo.blurNsfw.value)
  }

  // --- 11. Home Feed Settings / Source & Filter Update Tests (Batch 4) ---

  @Test
  fun testHomeFeedSettingsUpdatesHomeStateOnly() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MediaBrowserViewModel(app)

    // Set Search state to specific query and source
    viewModel.setSearchSource(MediaSourceConfig.BUILT_IN_DANBOORU)
    viewModel.setSearchQuery("cyberpunk")
    viewModel.setSearchFilterState(FilterState(rating = MediaRating.SUGGESTIVE))

    // Now update Home feed settings via unified control
    val yandere = MediaSourceConfig.BUILT_IN_YANDERE
    viewModel.setHomeSourceAndFilter(yandere, FilterState(rating = MediaRating.SAFE, mediaType = MediaType.IMAGE))

    // Home state updated
    assertEquals("builtin_yandere", viewModel.homeState.value.activeSource.id)
    assertEquals(MediaRating.SAFE, viewModel.homeState.value.filterState.rating)
    assertEquals(MediaType.IMAGE, viewModel.homeState.value.filterState.mediaType)

    // Search state is 100% unaffected
    assertEquals("builtin_danbooru", viewModel.searchState.value.activeSource.id)
    assertEquals("cyberpunk", viewModel.searchState.value.searchQuery)
    assertEquals(MediaRating.SUGGESTIVE, viewModel.searchState.value.filterState.rating)
  }

  // --- 12. Pull-To-Refresh Repeated Triggers & Pagination Reset (Batch 4) ---

  @Test
  fun testPullToRefreshResetsPaginationAndPreservesState() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = MediaBrowserViewModel(app)

    val danbooru = MediaSourceConfig.BUILT_IN_DANBOORU
    viewModel.setHomeSource(danbooru)
    viewModel.setHomeFilterState(FilterState(rating = MediaRating.ADULT))

    // Trigger refresh repeatedly
    viewModel.refreshHome()
    assertTrue(viewModel.homeState.value.isRefreshing)
    assertEquals(1, viewModel.homeState.value.currentPage)
    assertEquals("builtin_danbooru", viewModel.homeState.value.activeSource.id)
    assertEquals(MediaRating.ADULT, viewModel.homeState.value.filterState.rating)

    // Search state is independent
    viewModel.setSearchSource(MediaSourceConfig.BUILT_IN_YANDERE)
    viewModel.setSearchQuery("scenery")
    viewModel.refreshSearch()
    assertTrue(viewModel.searchState.value.isRefreshing)
    assertEquals(1, viewModel.searchState.value.currentPage)
    assertEquals("builtin_yandere", viewModel.searchState.value.activeSource.id)
    assertEquals("scenery", viewModel.searchState.value.searchQuery)
  }

  // --- 13. Download Quality Mapping & Format Preservation (Batch 5) ---

  @Test
  fun testDownloadQualityResolutionMapping() {
    val imageItem = MediaItem(
      id = "101",
      title = "Scenery Art",
      imageUrl = "https://cdn.artflux.org/files/original.png",
      thumbnailUrl = "https://cdn.artflux.org/thumbs/preview.jpg",
      sampleUrl = "https://cdn.artflux.org/samples/sample_720.jpg",
      mediaType = MediaType.IMAGE
    )

    // Original uses original file
    assertEquals("https://cdn.artflux.org/files/original.png", imageItem.getDownloadUrl(com.example.model.DownloadQuality.ORIGINAL))

    // 1080p / 720p prefer sampleUrl if present
    assertEquals("https://cdn.artflux.org/samples/sample_720.jpg", imageItem.getDownloadUrl(com.example.model.DownloadQuality.Q1080))
    assertEquals("https://cdn.artflux.org/samples/sample_720.jpg", imageItem.getDownloadUrl(com.example.model.DownloadQuality.Q720))

    // 480p / 360p prefer preview thumbnailUrl
    assertEquals("https://cdn.artflux.org/thumbs/preview.jpg", imageItem.getDownloadUrl(com.example.model.DownloadQuality.Q480))
    assertEquals("https://cdn.artflux.org/thumbs/preview.jpg", imageItem.getDownloadUrl(com.example.model.DownloadQuality.Q360))
  }

  @Test
  fun testDownloadQualityPreservesGifAndVideoMedia() {
    val gifItem = MediaItem(
      id = "202",
      title = "Animated Anime",
      imageUrl = "https://cdn.artflux.org/files/animation.gif",
      thumbnailUrl = "https://cdn.artflux.org/thumbs/static_frame.jpg",
      mediaType = MediaType.GIF
    )

    // Even if 360p is requested, GIF must NOT download static jpg thumbnail
    assertEquals("https://cdn.artflux.org/files/animation.gif", gifItem.getDownloadUrl(com.example.model.DownloadQuality.Q360))
    assertEquals("https://cdn.artflux.org/files/animation.gif", gifItem.getDownloadUrl(com.example.model.DownloadQuality.ORIGINAL))

    val videoItem = MediaItem(
      id = "303",
      title = "AMV Clip",
      imageUrl = "https://cdn.artflux.org/files/video.mp4",
      thumbnailUrl = "https://cdn.artflux.org/thumbs/video_thumb.jpg",
      mediaType = MediaType.VIDEO
    )

    // Video must always download video file
    assertEquals("https://cdn.artflux.org/files/video.mp4", videoItem.getDownloadUrl(com.example.model.DownloadQuality.Q480))
    assertEquals("https://cdn.artflux.org/files/video.mp4", videoItem.getDownloadUrl(com.example.model.DownloadQuality.ORIGINAL))
  }

  // --- 14. Thumbnail Fallback Resolution Tests (Batch 5) ---

  @Test
  fun testThumbnailFallbackUrlResolution() {
    val item = MediaItem(
      id = "404",
      title = "Artwork",
      imageUrl = "https://cdn.booru.org/orig.jpg",
      thumbnailUrl = "https://cdn.booru.org/thumb.jpg",
      sampleUrl = "https://cdn.booru.org/sample.jpg"
    )

    // If preferred 720p is sampleUrl, fallback should provide thumbnailUrl
    val fallbackFor720 = item.getThumbnailFallbackUrl(com.example.model.ThumbnailQuality.Q720)
    assertEquals("https://cdn.booru.org/thumb.jpg", fallbackFor720)

    // If preferred 360p is thumbnailUrl, fallback should provide sampleUrl or imageUrl
    val fallbackFor360 = item.getThumbnailFallbackUrl(com.example.model.ThumbnailQuality.Q360)
    assertEquals("https://cdn.booru.org/sample.jpg", fallbackFor360)
  }

  // --- 15. Structured JSON Import & Parsing Tests (Batch 6) ---

  @Test
  fun testStructuredJsonImportAndParsing() {
    val structuredJson = """
      {
        "name": "ArtStation Search",
        "apiUrl": "https://api.artstation.com/v2/community/explore/projects.json",
        "searchParam": "query",
        "pageParam": "page",
        "pageStartsAt": 1,
        "pageSizeParam": "per_page",
        "defaultPageSize": 30,
        "itemsPath": "data",
        "imageUrlField": "cover.large_image_url",
        "thumbUrlField": "cover.small_image_url",
        "sampleUrlField": "cover.medium_image_url",
        "postUrlField": "permalink",
        "tagsField": "tags",
        "ratingField": "rating",
        "gifQueryTag": "animated",
        "videoQueryTag": "video",
        "safeRatingTag": "purity:safe",
        "suggestiveRatingTag": "purity:sketchy",
        "adultRatingTag": "purity:nsfw",
        "authentication": {
          "type": "query",
          "parameters": {
            "api_key": "abc123secret",
            "client_id": "client999"
          }
        },
        "description": "ArtStation community discovery endpoint."
      }
    """.trimIndent()

    val parseResult = MediaSourceConfig.parseJson(structuredJson)
    assertTrue(parseResult.isSuccess)
    val config = parseResult.getOrThrow()

    assertEquals("ArtStation Search", config.name)
    assertEquals("https://api.artstation.com/v2/community/explore/projects.json", config.apiUrl)
    assertEquals("query", config.searchParam)
    assertEquals("data", config.itemsPath)
    assertEquals("cover.large_image_url", config.imageUrlField)
    assertEquals("cover.small_image_url", config.thumbUrlField)
    assertEquals(com.example.model.AuthType.QUERY_PARAMS, config.authType)
    assertEquals(2, config.authQueryParams.size)
    assertEquals("abc123secret", config.getEffectiveAuthQueryParams()["api_key"])
    assertEquals("client999", config.getEffectiveAuthQueryParams()["client_id"])
  }

  // --- 16. Malformed JSON & Validation Failure Tests (Batch 6) ---

  @Test
  fun testMalformedJsonHandlingProducesErrorWithoutCrashing() {
    val invalidJson = "{ this is not valid json content..."
    val result = MediaSourceConfig.parseJson(invalidJson)
    assertTrue(result.isFailure)
    assertNotNull(result.exceptionOrNull()?.message)
    assertTrue(result.exceptionOrNull()?.message?.contains("JSON") == true)
  }

  @Test
  fun testMissingRequiredFieldsValidation() {
    val missingUrlJson = """
      {
        "name": "No URL Source",
        "searchParam": "q"
      }
    """.trimIndent()
    val result = MediaSourceConfig.parseJson(missingUrlJson)
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("API Endpoint URL is required") == true)

    val missingNameJson = """
      {
        "apiUrl": "https://api.booru.org/posts"
      }
    """.trimIndent()
    val missingNameConfig = MediaSourceConfig.fromJson(org.json.JSONObject(missingNameJson))
    val errors = MediaSourceConfig.validateConfig(missingNameConfig.copy(name = ""))
    assertTrue(errors.any { it.contains("Source Name is required") })
  }

  @Test
  fun testUnknownOptionalFieldsSafelyIgnored() {
    val jsonWithExtraFields = """
      {
        "name": "Custom Source",
        "apiUrl": "https://example.com/api",
        "unknownField123": "irrelevant_value",
        "extraMetadata": {"nested": true},
        "customFlag": 42
      }
    """.trimIndent()
    val result = MediaSourceConfig.parseJson(jsonWithExtraFields)
    assertTrue(result.isSuccess)
    val config = result.getOrThrow()
    assertEquals("Custom Source", config.name)
    assertEquals("https://example.com/api", config.apiUrl)
  }

  // --- 17. Authentication Modes & Multi-Parameter Resolution (Batch 6) ---

  @Test
  fun testBearerTokenAuthenticationHeaderResolution() {
    val config = MediaSourceConfig(
      name = "Bearer Source",
      apiUrl = "https://api.example.com/posts",
      authType = com.example.model.AuthType.BEARER_TOKEN,
      authHeaderValue = "secret_jwt_token_12345"
    )

    val headers = config.getEffectiveHeaders()
    assertEquals(1, headers.size)
    assertEquals("Bearer secret_jwt_token_12345", headers["Authorization"])
  }

  @Test
  fun testCustomHeaderAuthenticationResolution() {
    val config = MediaSourceConfig(
      name = "Custom Header Source",
      apiUrl = "https://api.example.com/posts",
      authType = com.example.model.AuthType.CUSTOM_HEADER,
      authHeaderName = "X-API-KEY",
      authHeaderValue = "custom_secret_key_888"
    )

    val headers = config.getEffectiveHeaders()
    assertEquals(1, headers.size)
    assertEquals("custom_secret_key_888", headers["X-API-KEY"])
  }

  @Test
  fun testGelbooruMultipleQueryParametersAuthentication() {
    val gelbooru = MediaSourceConfig.TEMPLATE_GELBOORU.copy(
      authQueryParams = listOf(
        com.example.model.AuthParam("api_key", "my_gelbooru_api_key"),
        com.example.model.AuthParam("user_id", "456789")
      )
    )

    assertEquals(com.example.model.AuthType.QUERY_PARAMS, gelbooru.authType)
    val queryParams = gelbooru.getEffectiveAuthQueryParams()
    assertEquals(2, queryParams.size)
    assertEquals("my_gelbooru_api_key", queryParams["api_key"])
    assertEquals("456789", queryParams["user_id"])
    assertEquals("https://gelbooru.com/index.php?page=dapi&s=post&q=index&json=1", gelbooru.apiUrl)
  }

  // --- 18. Security: Credential Sanitization on Export (Batch 6) ---

  @Test
  fun testExportedConfigurationSanitizesSecretsWithPlaceholders() {
    val sensitiveConfig = MediaSourceConfig(
      name = "Private Booru",
      apiUrl = "https://private.booru.org/api",
      authType = com.example.model.AuthType.QUERY_PARAMS,
      authQueryParams = listOf(
        com.example.model.AuthParam("api_key", "SUPER_SECRET_KEY_12345"),
        com.example.model.AuthParam("user_id", "my_personal_user_id_999")
      ),
      authHeaderName = "X-Token",
      authHeaderValue = "SECRET_HEADER_VALUE"
    )

    val exportedJsonString = sensitiveConfig.toExportJson(sanitizeSecrets = true)

    // MUST NOT leak the actual secrets
    assertFalse(exportedJsonString.contains("SUPER_SECRET_KEY_12345"))
    assertFalse(exportedJsonString.contains("my_personal_user_id_999"))
    assertFalse(exportedJsonString.contains("SECRET_HEADER_VALUE"))

    // MUST contain standard placeholders
    assertTrue(exportedJsonString.contains("YOUR_API_KEY"))
    assertTrue(exportedJsonString.contains("YOUR_USER_ID"))
  }

  // --- 19. Expanded Templates Integrity (Batch 6) ---

  @Test
  fun testAllFiveSourceTemplatesPresentAndValid() {
    val templates = MediaSourceConfig.SOURCE_TEMPLATES
    assertEquals(5, templates.size)

    val templateNames = templates.map { it.name }
    assertTrue(templateNames.any { it.contains("Safebooru") })
    assertTrue(templateNames.any { it.contains("Danbooru") })
    assertTrue(templateNames.any { it.contains("Yande.re") })
    assertTrue(templateNames.any { it.contains("Gelbooru") })
    assertTrue(templateNames.any { it.contains("Moebooru") })
  }

  // --- 20. Source Diagnostics Logic Tests (Batch 6) ---

  @Test
  fun testSourceDiagnosticsParserAndStepEvaluation() {
    val apiClient = MediaApiClient()
    val customSource = MediaSourceConfig(
      name = "Diagnostic Test Booru",
      apiUrl = "https://custom.org/api",
      itemsPath = "posts",
      imageUrlField = "file_url",
      thumbUrlField = "preview_url",
      tagsField = "tags",
      ratingField = "rating"
    )

    val samplePayload = """
      {
        "posts": [
          {
            "id": 1001,
            "file_url": "https://cdn.custom.org/img/1001.png",
            "preview_url": "https://cdn.custom.org/thumb/1001.jpg",
            "tags": "scenery landscape sunset",
            "rating": "s",
            "file_ext": "png"
          }
        ]
      }
    """.trimIndent()

    val parsed = apiClient.parseMediaItems(customSource, samplePayload)
    assertEquals(1, parsed.size)
    assertEquals("https://cdn.custom.org/img/1001.png", parsed[0].imageUrl)
    assertEquals("https://cdn.custom.org/thumb/1001.jpg", parsed[0].thumbnailUrl)
    assertEquals(3, parsed[0].tags.size)
    assertEquals(MediaRating.SAFE, parsed[0].rating)
  }

  // --- 21. AI Prompt Template Content Validation (Batch 6) ---

  @Test
  fun testAiPromptTemplateContent() {
    val prompt = MediaSourceConfig.AI_PROMPT_TEMPLATE
    assertTrue(prompt.contains("Artflux"))
    assertTrue(prompt.contains("imageUrlField"))
    assertTrue(prompt.contains("thumbUrlField"))
    assertTrue(prompt.contains("searchParam"))
    assertTrue(prompt.contains("authentication"))
    assertTrue(prompt.contains("YOUR_API_KEY"))
  }

  // --- 22. Light Theme Persistence & Application (Batch 6.1) ---

  @Test
  fun testLightThemeSelectionAndPersistence() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val prefsRepo = com.example.data.PreferencesRepository(context)

    prefsRepo.setTheme(com.example.model.AppTheme.LIGHT)
    assertEquals(com.example.model.AppTheme.LIGHT, prefsRepo.theme.value)

    val reloadedRepo = com.example.data.PreferencesRepository(context)
    assertEquals(com.example.model.AppTheme.LIGHT, reloadedRepo.theme.value)
  }

  // --- 23. Feed Thumbnail Quality Preview Resolution across Media Types (Batch 6.1) ---

  @Test
  fun testThumbnailQualityResolutionForImagesGifsAndVideos() {
    val imageItem = MediaItem(
      id = "501",
      title = "Static Image",
      imageUrl = "https://cdn.artflux.org/full.jpg",
      thumbnailUrl = "https://cdn.artflux.org/thumb_360.jpg",
      sampleUrl = "https://cdn.artflux.org/sample_720.jpg",
      mediaType = MediaType.IMAGE
    )

    assertEquals("https://cdn.artflux.org/thumb_360.jpg", imageItem.getThumbnailForQuality(com.example.model.ThumbnailQuality.Q360))
    assertEquals("https://cdn.artflux.org/sample_720.jpg", imageItem.getThumbnailForQuality(com.example.model.ThumbnailQuality.Q720))

    val gifItem = MediaItem(
      id = "502",
      title = "GIF Preview",
      imageUrl = "https://cdn.artflux.org/anim.gif",
      thumbnailUrl = "https://cdn.artflux.org/gif_thumb_360.jpg",
      sampleUrl = "https://cdn.artflux.org/gif_sample_720.jpg",
      mediaType = MediaType.GIF
    )

    assertEquals("https://cdn.artflux.org/gif_thumb_360.jpg", gifItem.getThumbnailForQuality(com.example.model.ThumbnailQuality.Q360))
    assertEquals("https://cdn.artflux.org/gif_sample_720.jpg", gifItem.getThumbnailForQuality(com.example.model.ThumbnailQuality.Q720))

    val videoItem = MediaItem(
      id = "503",
      title = "Video Clip",
      imageUrl = "https://cdn.artflux.org/movie.mp4",
      thumbnailUrl = "https://cdn.artflux.org/video_thumb_360.jpg",
      sampleUrl = "https://cdn.artflux.org/video_sample_720.jpg",
      mediaType = MediaType.VIDEO
    )

    assertEquals("https://cdn.artflux.org/video_thumb_360.jpg", videoItem.getThumbnailForQuality(com.example.model.ThumbnailQuality.Q360))
    assertEquals("https://cdn.artflux.org/video_sample_720.jpg", videoItem.getThumbnailForQuality(com.example.model.ThumbnailQuality.Q720))
  }
}

