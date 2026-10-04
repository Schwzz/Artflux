package com.example.model

import org.json.JSONObject
import java.util.UUID

data class MediaSourceConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val apiUrl: String,
    val searchParam: String = "q",
    val pageParam: String = "page",
    val pageStartsAt: Int = 1,
    val pageSizeParam: String = "limit",
    val defaultPageSize: Int = 25,
    val apiKey: String = "",
    val apiKeyHeader: String = "",
    val apiKeyInQuery: Boolean = false,
    val apiKeyQueryParam: String = "api_key",
    val itemsPath: String = "",
    val imageUrlField: String = "sample_url",
    val thumbUrlField: String = "preview_url",
    val postUrlField: String = "id",
    val tagsField: String = "tags",
    val ratingField: String = "rating",
    val mediaTypeField: String = "",
    val titleField: String = "tags",
    val authorField: String = "owner",
    val isBuiltIn: Boolean = false,
    val description: String = "",
    val gifQueryTag: String = "",
    val videoQueryTag: String = "",
    val safeRatingTag: String = "",
    val suggestiveRatingTag: String = "",
    val adultRatingTag: String = ""
) {
    val supportsGifs: Boolean
        get() = gifQueryTag.isNotBlank()

    val supportsVideos: Boolean
        get() = videoQueryTag.isNotBlank()

    fun getSupportedMediaTypes(): List<MediaType> {
        val list = mutableListOf(MediaType.ALL, MediaType.IMAGE)
        if (supportsGifs) list.add(MediaType.GIF)
        if (supportsVideos) list.add(MediaType.VIDEO)
        return list
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("apiUrl", apiUrl)
        json.put("searchParam", searchParam)
        json.put("pageParam", pageParam)
        json.put("pageStartsAt", pageStartsAt)
        json.put("pageSizeParam", pageSizeParam)
        json.put("defaultPageSize", defaultPageSize)
        json.put("apiKey", apiKey)
        json.put("apiKeyHeader", apiKeyHeader)
        json.put("apiKeyInQuery", apiKeyInQuery)
        json.put("apiKeyQueryParam", apiKeyQueryParam)
        json.put("itemsPath", itemsPath)
        json.put("imageUrlField", imageUrlField)
        json.put("thumbUrlField", thumbUrlField)
        json.put("postUrlField", postUrlField)
        json.put("tagsField", tagsField)
        json.put("ratingField", ratingField)
        json.put("mediaTypeField", mediaTypeField)
        json.put("titleField", titleField)
        json.put("authorField", authorField)
        json.put("isBuiltIn", isBuiltIn)
        json.put("description", description)
        json.put("gifQueryTag", gifQueryTag)
        json.put("videoQueryTag", videoQueryTag)
        json.put("safeRatingTag", safeRatingTag)
        json.put("suggestiveRatingTag", suggestiveRatingTag)
        json.put("adultRatingTag", adultRatingTag)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): MediaSourceConfig {
            return MediaSourceConfig(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Custom Source"),
                apiUrl = json.optString("apiUrl", ""),
                searchParam = json.optString("searchParam", "q"),
                pageParam = json.optString("pageParam", "page"),
                pageStartsAt = json.optInt("pageStartsAt", 1),
                pageSizeParam = json.optString("pageSizeParam", "limit"),
                defaultPageSize = json.optInt("defaultPageSize", 25),
                apiKey = json.optString("apiKey", ""),
                apiKeyHeader = json.optString("apiKeyHeader", ""),
                apiKeyInQuery = json.optBoolean("apiKeyInQuery", false),
                apiKeyQueryParam = json.optString("apiKeyQueryParam", "api_key"),
                itemsPath = json.optString("itemsPath", ""),
                imageUrlField = json.optString("imageUrlField", "sample_url"),
                thumbUrlField = json.optString("thumbUrlField", "preview_url"),
                postUrlField = json.optString("postUrlField", "id"),
                tagsField = json.optString("tagsField", "tags"),
                ratingField = json.optString("ratingField", "rating"),
                mediaTypeField = json.optString("mediaTypeField", ""),
                titleField = json.optString("titleField", "tags"),
                authorField = json.optString("authorField", "owner"),
                isBuiltIn = json.optBoolean("isBuiltIn", false),
                description = json.optString("description", ""),
                gifQueryTag = json.optString("gifQueryTag", ""),
                videoQueryTag = json.optString("videoQueryTag", ""),
                safeRatingTag = json.optString("safeRatingTag", ""),
                suggestiveRatingTag = json.optString("suggestiveRatingTag", ""),
                adultRatingTag = json.optString("adultRatingTag", "")
            )
        }

        val BUILT_IN_SAFEBOORU = MediaSourceConfig(
            id = "builtin_safebooru",
            name = "Safebooru",
            apiUrl = "https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1",
            searchParam = "tags",
            pageParam = "pid",
            pageStartsAt = 0,
            pageSizeParam = "limit",
            defaultPageSize = 25,
            itemsPath = "",
            imageUrlField = "sample_url",
            thumbUrlField = "preview_url",
            postUrlField = "id",
            tagsField = "tags",
            ratingField = "rating",
            titleField = "tags",
            authorField = "owner",
            isBuiltIn = true,
            description = "Safe anime illustration archive with tag-based search",
            gifQueryTag = "animated",
            videoQueryTag = "",
            safeRatingTag = "rating:general",
            suggestiveRatingTag = "",
            adultRatingTag = ""
        )

        val BUILT_IN_DANBOORU = MediaSourceConfig(
            id = "builtin_danbooru",
            name = "Danbooru",
            apiUrl = "https://danbooru.donmai.us/posts.json",
            searchParam = "tags",
            pageParam = "page",
            pageStartsAt = 1,
            pageSizeParam = "limit",
            defaultPageSize = 25,
            itemsPath = "",
            imageUrlField = "file_url",
            thumbUrlField = "preview_file_url",
            postUrlField = "id",
            tagsField = "tag_string",
            ratingField = "rating",
            mediaTypeField = "file_ext",
            titleField = "tag_string",
            authorField = "tag_string_artist",
            isBuiltIn = true,
            description = "Premier anime image board with extensive tagging and ratings",
            gifQueryTag = "animated_gif",
            videoQueryTag = "webm",
            safeRatingTag = "rating:g",
            suggestiveRatingTag = "rating:s,q",
            adultRatingTag = "rating:e"
        )

        val BUILT_IN_YANDERE = MediaSourceConfig(
            id = "builtin_yandere",
            name = "Yande.re",
            apiUrl = "https://yande.re/post.json",
            searchParam = "tags",
            pageParam = "page",
            pageStartsAt = 1,
            pageSizeParam = "limit",
            defaultPageSize = 25,
            itemsPath = "",
            imageUrlField = "file_url",
            thumbUrlField = "preview_url",
            postUrlField = "id",
            tagsField = "tags",
            ratingField = "rating",
            mediaTypeField = "file_ext",
            titleField = "tags",
            authorField = "author",
            isBuiltIn = true,
            description = "High-resolution anime illustrations and scans",
            gifQueryTag = "",
            videoQueryTag = "",
            safeRatingTag = "rating:s",
            suggestiveRatingTag = "rating:q",
            adultRatingTag = "rating:e"
        )

        val DEFAULT_SOURCES = listOf(
            BUILT_IN_SAFEBOORU,
            BUILT_IN_DANBOORU,
            BUILT_IN_YANDERE
        )
    }
}
