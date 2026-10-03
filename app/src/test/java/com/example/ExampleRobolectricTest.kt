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
    assertEquals("animated", danbooru.gifQueryTag)
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
    assertEquals("hatsune_miku animated rating:e", query1)

    // User already typed "animated" and "rating:e"
    val query2 = QueryBuilder.buildEffectiveQuery(
      userQuery = "animated hatsune_miku rating:e",
      source = danbooru,
      mediaType = MediaType.GIF,
      rating = MediaRating.ADULT
    )
    assertEquals("animated hatsune_miku rating:e", query2)

    // Danbooru Video + Suggestive
    val query3 = QueryBuilder.buildEffectiveQuery(
      userQuery = "genshin",
      source = danbooru,
      mediaType = MediaType.VIDEO,
      rating = MediaRating.SUGGESTIVE
    )
    assertEquals("genshin webm rating:s,q", query3)
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
}

