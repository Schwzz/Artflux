package com.swartzz.artflux

import com.swartzz.artflux.data.MediaApiClient
import com.swartzz.artflux.data.MediaRequestBuilder
import com.swartzz.artflux.model.MediaRating
import com.swartzz.artflux.model.MediaSourceConfig
import com.swartzz.artflux.model.MediaType
import com.swartzz.artflux.util.ArtfluxNetwork
import okhttp3.HttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for the Waifu.im source after the official api.waifu.im
 * host stopped resolving (service shut down on 2026-10-24). The built-in
 * source now points at a community instance running the same open-source
 * API (api.waifu.best), which was verified live against the v7 contract.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WaifuImMigrationTest {

    @Test fun builtInSourceTargetsResolvingInstance() {
        val source = MediaSourceConfig.BUILT_IN_WAIFU_IM
        assertTrue(source.apiUrl.startsWith("https://api.waifu.best/"))
        assertFalse("api.waifu.im no longer resolves", source.apiUrl.contains("api.waifu.im"))
        assertEquals("items", source.itemsPath)
        assertEquals("isNsfw", source.ratingField)
    }

    @Test fun waifuFamilyDetectionCoversLegacyAndCommunityHosts() {
        assertTrue(MediaSourceConfig.BUILT_IN_WAIFU_IM.isWaifuFamily)
        assertTrue(MediaSourceConfig.BUILT_IN_WAIFU_IM.copy(apiUrl = "https://api.waifu.im/images").isWaifuFamily)
        assertTrue(MediaSourceConfig.BUILT_IN_WAIFU_IM.copy(
            id = "custom_builtin_waifu_im_1234", apiUrl = "https://unrelated.example/feed").isWaifuFamily)
        assertFalse(MediaSourceConfig.BUILT_IN_DANBOORU.isWaifuFamily)
    }

    @Test fun safeRatingFilterIsSentAsBooleanParameter() {
        val url = fetchUrl(rating = MediaRating.SAFE)
        assertEquals("false", url.queryParameter("isNsfw"))
        assertEquals(null, url.queryParameter("is_nsfw"))
    }

    @Test fun adultRatingFilterIsSentAsBooleanParameter() {
        val url = fetchUrl(rating = MediaRating.ADULT)
        assertEquals("true", url.queryParameter("isNsfw"))
        assertEquals(null, url.queryParameter("is_nsfw"))
    }

    @Test fun legacySnakeCaseRatingTagsStillTranslate() {
        val legacy = MediaSourceConfig.BUILT_IN_WAIFU_IM.copy(
            apiUrl = "https://api.waifu.best/images",
            safeRatingTag = "is_nsfw:false"
        )
        val request = MediaRequestBuilder.buildFetchRequest(legacy, query = legacy.safeRatingTag, page = 1)
        val url = request.url
        assertEquals("false", url.queryParameter("isNsfw"))
        assertEquals(null, url.queryParameter("is_nsfw"))
        assertTrue("Pseudo tag must be stripped from the query", url.queryParameterValues("includedTags").isEmpty())
    }

    @Test fun userSearchTagsUseDocumentedIncludedTagsParameter() {
        val url = fetchUrl(rating = MediaRating.ALL, query = "maid uniform")
        assertEquals(listOf("maid", "uniform"), url.queryParameterValues("includedTags"))
        assertEquals(null, url.queryParameter("IncludedTags"))
    }

    @Test fun paginationAndPageSizeFollowV7Contract() {
        val url = fetchUrl(rating = MediaRating.ALL)
        assertEquals("1", url.queryParameter("page"))
        assertEquals("25", url.queryParameter("pageSize"))
    }

    @Test fun communityInstanceIsTreatedAsWaifuByParserClassifierAndUi() {
        val source = MediaSourceConfig.BUILT_IN_WAIFU_IM
        // Classifier accepts the leading-dot extension returned by the live API.
        val apiClient = MediaApiClient()
        val payload = """
            {"items":[{"id":5,"url":"https://cdn.waifu.best/5.gif","isNsfw":false,"extension":".gif","artists":[]}]}
        """.trimIndent()
        val items = apiClient.parseMediaItems(source, payload)
        assertEquals(1, items.size)
        assertEquals(MediaType.GIF, items[0].mediaType)
        assertEquals(MediaRating.SAFE, items[0].rating)
        assertEquals("https://waifu.best/", ArtfluxNetwork.getRefererForUrl("https://cdn.waifu.best/5.gif"))
    }

    private fun fetchUrl(rating: MediaRating, query: String = ""): HttpUrl {
        val source = MediaSourceConfig.BUILT_IN_WAIFU_IM
        val request = MediaRequestBuilder.buildFetchRequest(
            source,
            query = com.swartzz.artflux.util.QueryBuilder.buildEffectiveQuery(query, source, MediaType.ALL, rating),
            page = 1
        )
        return request.url
    }
}
