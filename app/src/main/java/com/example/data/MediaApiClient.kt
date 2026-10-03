package com.example.data

import com.example.model.MediaItem
import com.example.model.MediaRating
import com.example.model.MediaSourceConfig
import com.example.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MediaApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchMedia(
        source: MediaSourceConfig,
        query: String = "",
        page: Int = 1
    ): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            // Handle internal showcase
            if (source.apiUrl.startsWith("internal://")) {
                val filtered = if (query.isBlank()) {
                    CuratedMediaData.ITEMS
                } else {
                    val q = query.lowercase().trim()
                    CuratedMediaData.ITEMS.filter { item ->
                        item.title.lowercase().contains(q) ||
                        item.tags.any { it.lowercase().contains(q) } ||
                        (item.author?.lowercase()?.contains(q) == true) ||
                        (item.description?.lowercase()?.contains(q) == true)
                    }
                }
                // Simulate paging
                val pageSize = source.defaultPageSize
                val startIndex = ((page - 1) * pageSize).coerceAtLeast(0)
                val items = if (startIndex < filtered.size) {
                    filtered.subList(startIndex, (startIndex + pageSize).coerceAtMost(filtered.size))
                } else {
                    emptyList()
                }
                return@withContext Result.success(items)
            }

            // Build dynamic HTTP URL
            val urlBuilder = source.apiUrl.toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid API URL: ${source.apiUrl}"))

            // Query parameter
            if (query.isNotBlank() && source.searchParam.isNotBlank()) {
                urlBuilder.addQueryParameter(source.searchParam, query.trim())
            }

            // Pagination parameter
            if (source.pageParam.isNotBlank()) {
                val actualPage = if (source.pageStartsAt == 0) page - 1 else page
                urlBuilder.addQueryParameter(source.pageParam, actualPage.toString())
            }

            // Page size parameter
            if (source.pageSizeParam.isNotBlank()) {
                urlBuilder.addQueryParameter(source.pageSizeParam, source.defaultPageSize.toString())
            }

            // API key in query
            if (source.apiKey.isNotBlank() && source.apiKeyInQuery && source.apiKeyQueryParam.isNotBlank()) {
                urlBuilder.addQueryParameter(source.apiKeyQueryParam, source.apiKey)
            }

            val requestBuilder = Request.Builder()
                .url(urlBuilder.build())
                .addHeader("User-Agent", "MediaBrowserApp/1.0 (Android)")
                .addHeader("Accept", "application/json")

            // API key in header
            if (source.apiKey.isNotBlank() && !source.apiKeyInQuery && source.apiKeyHeader.isNotBlank()) {
                requestBuilder.addHeader(source.apiKeyHeader, source.apiKey)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("HTTP ${response.code}: ${response.message}")
                )
            }

            val bodyString = response.body?.string().orEmpty()
            if (bodyString.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val items = parseMediaItems(source, bodyString)
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseMediaItems(source: MediaSourceConfig, body: String): List<MediaItem> {
        val trimmed = body.trim()
        val itemsArray: JSONArray = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val rootObj = JSONObject(trimmed)
            if (source.itemsPath.isBlank()) {
                // Try common keys if itemsPath is empty
                when {
                    rootObj.has("data") -> rootObj.optJSONArray("data")
                    rootObj.has("results") -> rootObj.optJSONArray("results")
                    rootObj.has("images") -> rootObj.optJSONArray("images")
                    rootObj.has("posts") -> rootObj.optJSONArray("posts")
                    rootObj.has("artworks") -> rootObj.optJSONArray("artworks")
                    else -> null
                } ?: JSONArray()
            } else {
                extractNestedJsonArray(rootObj, source.itemsPath) ?: JSONArray()
            }
        }

        val result = mutableListOf<MediaItem>()
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.optJSONObject(i) ?: continue
            val item = mapJsonToMediaItem(source, obj, i)
            if (item != null) {
                result.add(item)
            }
        }
        return result
    }

    private fun mapJsonToMediaItem(source: MediaSourceConfig, obj: JSONObject, index: Int): MediaItem? {
        val rawImage = extractValue(obj, source.imageUrlField)
        val rawThumb = extractValue(obj, source.thumbUrlField).ifBlank { rawImage }

        // Determine image & thumbnail URLs
        val (finalImage, finalThumb) = resolveUrls(source, obj, rawImage, rawThumb)
        if (finalImage.isBlank()) return null

        val id = extractValue(obj, "id").ifBlank { "item-${source.id}-$index" }
        val title = extractValue(obj, source.titleField).ifBlank {
            extractValue(obj, "name").ifBlank { "Art #$id" }
        }
        val postUrl = extractValue(obj, source.postUrlField).let {
            if (it.isBlank()) null else fixUrl(it)
        }

        // Tags parsing
        val tagsList = extractTags(obj, source.tagsField)

        // Rating parsing
        val ratingStr = extractValue(obj, source.ratingField).lowercase()
        val rating = when {
            ratingStr.contains("safe") || ratingStr == "s" || ratingStr == "g" -> MediaRating.SAFE
            ratingStr.contains("quest") || ratingStr == "q" -> MediaRating.QUESTIONABLE
            ratingStr.contains("explicit") || ratingStr == "e" -> MediaRating.EXPLICIT
            else -> MediaRating.SAFE
        }

        // Media type
        val typeStr = extractValue(obj, source.mediaTypeField).lowercase()
        val mediaType = when {
            typeStr.contains("gif") || finalImage.endsWith(".gif", true) -> MediaType.GIF
            typeStr.contains("video") || typeStr.contains("mp4") || finalImage.endsWith(".mp4", true) -> MediaType.VIDEO
            typeStr.contains("art") || typeStr.contains("illustration") -> MediaType.ART
            else -> MediaType.IMAGE
        }

        val author = extractValue(obj, source.authorField).ifBlank { null }
        val width = obj.optInt("width", 0).takeIf { it > 0 } ?: obj.optInt("image_width", 0).takeIf { it > 0 }
        val height = obj.optInt("height", 0).takeIf { it > 0 } ?: obj.optInt("image_height", 0).takeIf { it > 0 }

        return MediaItem(
            id = id,
            title = title,
            imageUrl = finalImage,
            thumbnailUrl = finalThumb,
            postUrl = postUrl,
            tags = tagsList,
            rating = rating,
            mediaType = mediaType,
            width = width,
            height = height,
            author = author,
            sourceName = source.name,
            description = obj.optString("alt_text", obj.optString("description", "")).ifBlank { null }
        )
    }

    private fun resolveUrls(
        source: MediaSourceConfig,
        obj: JSONObject,
        rawImage: String,
        rawThumb: String
    ): Pair<String, String> {
        // Art Institute of Chicago special handling
        if (source.apiUrl.contains("artic.edu")) {
            val imageId = rawImage.ifBlank { obj.optString("image_id") }
            if (imageId.isNotBlank() && imageId != "null") {
                val full = "https://www.artic.edu/iiif/2/$imageId/full/843,/0/default.jpg"
                val thumb = "https://www.artic.edu/iiif/2/$imageId/full/400,/0/default.jpg"
                return Pair(full, thumb)
            }
        }

        // Safebooru special handling
        if (source.apiUrl.contains("safebooru.org")) {
            val directory = obj.optString("directory")
            val image = obj.optString("image")
            if (directory.isNotBlank() && image.isNotBlank()) {
                val full = "https://safebooru.org/images/$directory/$image"
                val thumb = "https://safebooru.org/thumbnails/$directory/thumbnail_$image"
                return Pair(full, thumb)
            }
        }

        // Generic URL cleanup
        val full = fixUrl(rawImage)
        val thumb = fixUrl(rawThumb.ifBlank { rawImage })
        return Pair(full, thumb)
    }

    private fun fixUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.isNotBlank() && !trimmed.contains("://") -> "https://$trimmed"
            else -> trimmed
        }
    }

    private fun extractTags(obj: JSONObject, tagsField: String): List<String> {
        if (tagsField.isBlank()) return emptyList()
        val parts = tagsField.split(".")
        var current: Any? = obj
        for (part in parts) {
            if (current is JSONObject) {
                current = current.opt(part)
            } else {
                break
            }
        }

        return when (current) {
            is JSONArray -> {
                val list = mutableListOf<String>()
                for (i in 0 until current.length()) {
                    val item = current.opt(i)
                    if (item is JSONObject) {
                        val name = item.optString("name", item.optString("title", ""))
                        if (name.isNotBlank()) list.add(name)
                    } else if (item != null) {
                        list.add(item.toString())
                    }
                }
                list
            }
            is String -> {
                current.split(",", " ", ";")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            }
            else -> emptyList()
        }
    }

    private fun extractValue(obj: JSONObject, path: String): String {
        if (path.isBlank()) return ""
        val parts = path.split(".")
        var current: Any? = obj
        for (i in parts.indices) {
            val part = parts[i]
            if (current is JSONObject) {
                if (i == parts.lastIndex) {
                    val raw = current.opt(part)
                    return when (raw) {
                        null, JSONObject.NULL -> ""
                        else -> raw.toString()
                    }
                } else {
                    current = current.opt(part)
                }
            } else {
                return ""
            }
        }
        return ""
    }

    private fun extractNestedJsonArray(obj: JSONObject, path: String): JSONArray? {
        val parts = path.split(".")
        var current: Any? = obj
        for (i in parts.indices) {
            val part = parts[i]
            if (current is JSONObject) {
                if (i == parts.lastIndex) {
                    return current.optJSONArray(part)
                } else {
                    current = current.opt(part)
                }
            } else {
                return null
            }
        }
        return null
    }
}
