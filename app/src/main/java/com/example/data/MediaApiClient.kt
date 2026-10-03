package com.example.data

import android.util.Log
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
                .addHeader("User-Agent", "ArtfluxApp/1.0 (Android)")
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

    internal fun parseMediaItems(source: MediaSourceConfig, body: String): List<MediaItem> {
        val trimmed = body.trim()
        val itemsArray: JSONArray = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val rootObj = JSONObject(trimmed)
            if (source.itemsPath.isBlank()) {
                // Try common keys if itemsPath is empty
                when {
                    rootObj.has("posts") -> rootObj.optJSONArray("posts")
                    rootObj.has("data") -> rootObj.optJSONArray("data")
                    rootObj.has("results") -> rootObj.optJSONArray("results")
                    rootObj.has("images") -> rootObj.optJSONArray("images")
                    else -> null
                } ?: JSONArray()
            } else {
                extractNestedJsonArray(rootObj, source.itemsPath) ?: JSONArray()
            }
        }

        Log.d("ArtfluxDebug", "Received ${itemsArray.length()} posts from ${source.name}")
        val result = mutableListOf<MediaItem>()
        var gifCount = 0
        var videoCount = 0
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.optJSONObject(i) ?: continue
            val item = mapJsonToMediaItem(source, obj, i)
            if (item != null) {
                result.add(item)
                if (item.mediaType == MediaType.GIF) gifCount++
                if (item.mediaType == MediaType.VIDEO) videoCount++
                Log.d("ArtfluxDebug", "Post #${item.id}: fileUrl=${item.imageUrl}, detectedType=${item.mediaType}")
            }
        }
        Log.d("ArtfluxDebug", "Parsed ${result.size} posts: $gifCount GIF(s), $videoCount VIDEO(s)")
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

        val postUrl = when {
            source.apiUrl.contains("safebooru.org") -> "https://safebooru.org/index.php?page=post&s=view&id=$id"
            source.apiUrl.contains("danbooru") -> "https://danbooru.donmai.us/posts/$id"
            source.apiUrl.contains("yande.re") -> "https://yande.re/post/show/$id"
            else -> extractValue(obj, source.postUrlField).let {
                if (it.isBlank()) null else fixUrl(it)
            }
        }

        // Tags parsing
        val tagsList = extractTags(obj, source.tagsField)

        // Rating parsing
        val ratingStr = extractValue(obj, source.ratingField).lowercase().trim()
        val rating = when {
            source.apiUrl.contains("danbooru") -> {
                when (ratingStr) {
                    "g", "general" -> MediaRating.SAFE
                    "s", "sensitive", "q", "questionable" -> MediaRating.SUGGESTIVE
                    "e", "explicit" -> MediaRating.ADULT
                    else -> MediaRating.SAFE
                }
            }
            else -> {
                when {
                    ratingStr == "s" || ratingStr == "g" || ratingStr.contains("safe") || ratingStr.contains("general") -> MediaRating.SAFE
                    ratingStr == "q" || ratingStr.contains("quest") || ratingStr.contains("sensit") || ratingStr.contains("suggest") -> MediaRating.SUGGESTIVE
                    ratingStr == "e" || ratingStr.contains("expl") || ratingStr.contains("adult") -> MediaRating.ADULT
                    else -> MediaRating.SAFE
                }
            }
        }

        // Media type detection (GIF, VIDEO, IMAGE)
        val typeStr = extractValue(obj, source.mediaTypeField).lowercase()
        val imageExt = finalImage.substringAfterLast('.', "").substringBefore('?').lowercase()
        val rawImageExt = rawImage.substringAfterLast('.', "").substringBefore('?').lowercase()
        val safebooruImageField = obj.optString("image").lowercase()

        val isGif = typeStr.contains("gif") ||
                imageExt == "gif" ||
                rawImageExt == "gif" ||
                safebooruImageField.endsWith(".gif")

        val isVideo = typeStr.contains("video") ||
                typeStr.contains("mp4") ||
                typeStr.contains("webm") ||
                imageExt in listOf("mp4", "webm", "mkv", "mov") ||
                rawImageExt in listOf("mp4", "webm", "mkv", "mov") ||
                safebooruImageField.endsWith(".mp4") ||
                safebooruImageField.endsWith(".webm")

        val mediaType = when {
            isVideo -> MediaType.VIDEO
            isGif -> MediaType.GIF
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
        // Safebooru handling
        if (source.apiUrl.contains("safebooru.org")) {
            val directory = obj.optString("directory")
            val image = obj.optString("image")
            if (directory.isNotBlank() && image.isNotBlank()) {
                val full = "https://safebooru.org/images/$directory/$image"
                val rawPreview = obj.optString("preview_url")
                val thumb = when {
                    rawPreview.isNotBlank() -> fixUrl(rawPreview)
                    image.endsWith(".mp4", true) || image.endsWith(".webm", true) -> {
                        val baseName = image.substringBeforeLast('.')
                        "https://safebooru.org/thumbnails/$directory/thumbnail_$baseName.jpg"
                    }
                    else -> "https://safebooru.org/thumbnails/$directory/thumbnail_$image"
                }
                return Pair(full, thumb)
            }
        }

        // Danbooru handling
        if (source.apiUrl.contains("danbooru")) {
            val fileUrl = rawImage.ifBlank { obj.optString("file_url").ifBlank { obj.optString("large_file_url") } }
            val previewUrl = rawThumb.ifBlank { obj.optString("preview_file_url").ifBlank { fileUrl } }
            if (fileUrl.isNotBlank()) {
                return Pair(fixUrl(fileUrl), fixUrl(previewUrl))
            }
        }

        // Yande.re handling
        if (source.apiUrl.contains("yande.re")) {
            val fileUrl = rawImage.ifBlank { obj.optString("file_url").ifBlank { obj.optString("sample_url") } }
            val previewUrl = rawThumb.ifBlank { obj.optString("preview_url").ifBlank { obj.optString("sample_url") } }
            if (fileUrl.isNotBlank()) {
                return Pair(fixUrl(fileUrl), fixUrl(previewUrl))
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
