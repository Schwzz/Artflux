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
    val defaultPageSize: Int = 20,
    val apiKey: String = "",
    val apiKeyHeader: String = "",
    val apiKeyInQuery: Boolean = false,
    val apiKeyQueryParam: String = "api_key",
    val itemsPath: String = "data",
    val imageUrlField: String = "url",
    val thumbUrlField: String = "thumbnail",
    val postUrlField: String = "link",
    val tagsField: String = "tags",
    val ratingField: String = "rating",
    val mediaTypeField: String = "media_type",
    val titleField: String = "title",
    val authorField: String = "author",
    val isBuiltIn: Boolean = false,
    val description: String = ""
) {
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
                defaultPageSize = json.optInt("defaultPageSize", 20),
                apiKey = json.optString("apiKey", ""),
                apiKeyHeader = json.optString("apiKeyHeader", ""),
                apiKeyInQuery = json.optBoolean("apiKeyInQuery", false),
                apiKeyQueryParam = json.optString("apiKeyQueryParam", "api_key"),
                itemsPath = json.optString("itemsPath", "data"),
                imageUrlField = json.optString("imageUrlField", "url"),
                thumbUrlField = json.optString("thumbUrlField", "thumbnail"),
                postUrlField = json.optString("postUrlField", "link"),
                tagsField = json.optString("tagsField", "tags"),
                ratingField = json.optString("ratingField", "rating"),
                mediaTypeField = json.optString("mediaTypeField", "media_type"),
                titleField = json.optString("titleField", "title"),
                authorField = json.optString("authorField", "author"),
                isBuiltIn = json.optBoolean("isBuiltIn", false),
                description = json.optString("description", "")
            )
        }

        val BUILT_IN_CURATED = MediaSourceConfig(
            id = "builtin_curated",
            name = "Curated Art Showcase",
            apiUrl = "internal://curated",
            searchParam = "q",
            pageParam = "page",
            isBuiltIn = true,
            description = "High-resolution digital paintings, concept art, 3D renders, and cyberpunk illustrations"
        )

        val BUILT_IN_PICSUM = MediaSourceConfig(
            id = "builtin_picsum",
            name = "Lorem Picsum Photos",
            apiUrl = "https://picsum.photos/v2/list",
            searchParam = "q",
            pageParam = "page",
            pageSizeParam = "limit",
            defaultPageSize = 25,
            itemsPath = "", // root array
            imageUrlField = "download_url",
            thumbUrlField = "download_url",
            postUrlField = "url",
            tagsField = "",
            ratingField = "",
            mediaTypeField = "",
            titleField = "author",
            authorField = "author",
            isBuiltIn = true,
            description = "High-resolution modern photography and scenic captures with infinite pagination"
        )

        val BUILT_IN_ARTIC = MediaSourceConfig(
            id = "builtin_artic",
            name = "Art Institute of Chicago",
            apiUrl = "https://api.artic.edu/api/v1/artworks/search",
            searchParam = "q",
            pageParam = "page",
            pageSizeParam = "limit",
            defaultPageSize = 20,
            itemsPath = "data",
            imageUrlField = "image_id",
            thumbUrlField = "thumbnail.alt_text",
            postUrlField = "api_link",
            tagsField = "term_titles",
            titleField = "title",
            authorField = "artist_title",
            isBuiltIn = true,
            description = "Renowned museum collection of historical paintings, sculptures, and fine art masterpieces"
        )

        val BUILT_IN_SAFEBOORU = MediaSourceConfig(
            id = "builtin_safebooru",
            name = "Safebooru Art",
            apiUrl = "https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1",
            searchParam = "tags",
            pageParam = "pid",
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
            description = "Anime and digital illustration archive with tag-based search and rating classification"
        )

        val DEFAULT_SOURCES = listOf(
            BUILT_IN_CURATED,
            BUILT_IN_PICSUM,
            BUILT_IN_ARTIC,
            BUILT_IN_SAFEBOORU
        )
    }
}
