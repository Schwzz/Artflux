package com.example.data

import android.util.Log
import com.example.model.DiagnosticStatus
import com.example.model.DiagnosticStep
import com.example.model.MediaItem
import com.example.model.MediaRating
import com.example.model.MediaSourceConfig
import com.example.model.MediaType
import com.example.model.SourceDiagnosticReport
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

            // Effective Authentication Query Parameters (multi-parameter support including api_key, user_id, etc.)
            val authParams = source.getEffectiveAuthQueryParams()
            for ((key, value) in authParams) {
                if (key.isNotBlank() && value.isNotBlank()) {
                    urlBuilder.addQueryParameter(key, value)
                }
            }

            val requestBuilder = Request.Builder()
                .url(urlBuilder.build())
                .addHeader("User-Agent", "ArtfluxApp/2.0 (Android; MediaBrowser)")
                .addHeader("Accept", "application/json")

            // Effective Authentication Headers (Bearer token, custom headers, etc.)
            val headers = source.getEffectiveHeaders()
            for ((hName, hVal) in headers) {
                if (hName.isNotBlank() && hVal.isNotBlank()) {
                    requestBuilder.addHeader(hName, hVal)
                }
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

    /**
     * Executes comprehensive diagnostic analysis for a media source configuration.
     * Evaluates connection, authentication, results path, image/thumbnail extraction, tags, and ratings.
     */
    suspend fun diagnoseSource(source: MediaSourceConfig): SourceDiagnosticReport = withContext(Dispatchers.IO) {
        val steps = mutableListOf<DiagnosticStep>()

        val urlBuilder = source.apiUrl.toHttpUrlOrNull()?.newBuilder()
        if (urlBuilder == null) {
            return@withContext SourceDiagnosticReport(
                isSuccess = false,
                summary = "Invalid API URL: '${source.apiUrl}'",
                steps = listOf(
                    DiagnosticStep(
                        title = "Connection",
                        status = DiagnosticStatus.FAILED,
                        detail = "The provided API URL could not be parsed into a valid HTTP URL."
                    )
                )
            )
        }

        // Add page param
        if (source.pageParam.isNotBlank()) {
            urlBuilder.addQueryParameter(source.pageParam, source.pageStartsAt.toString())
        }
        if (source.pageSizeParam.isNotBlank()) {
            urlBuilder.addQueryParameter(source.pageSizeParam, source.defaultPageSize.toString())
        }

        // Add Auth query parameters
        val authParams = source.getEffectiveAuthQueryParams()
        for ((k, v) in authParams) {
            if (k.isNotBlank() && v.isNotBlank()) {
                urlBuilder.addQueryParameter(k, v)
            }
        }

        val requestBuilder = Request.Builder()
            .url(urlBuilder.build())
            .addHeader("User-Agent", "ArtfluxApp/2.0 (Android; Diagnostic)")
            .addHeader("Accept", "application/json")

        val headers = source.getEffectiveHeaders()
        for ((hName, hVal) in headers) {
            if (hName.isNotBlank() && hVal.isNotBlank()) {
                requestBuilder.addHeader(hName, hVal)
            }
        }

        val response = try {
            client.newCall(requestBuilder.build()).execute()
        } catch (e: Exception) {
            return@withContext SourceDiagnosticReport(
                isSuccess = false,
                summary = "Connection failed: ${e.message ?: "Network unreachable"}",
                steps = listOf(
                    DiagnosticStep(
                        title = "Connection",
                        status = DiagnosticStatus.FAILED,
                        detail = "Could not establish connection to ${source.apiUrl}: ${e.message}"
                    ),
                    DiagnosticStep(
                        title = "Authentication",
                        status = DiagnosticStatus.FAILED,
                        detail = "Cannot test authentication because connection failed."
                    )
                )
            )
        }

        val httpCode = response.code
        val isHttpOk = response.isSuccessful

        // 1. Connection Step
        if (isHttpOk) {
            steps.add(DiagnosticStep("Connection", DiagnosticStatus.PASSED, "HTTP $httpCode OK — Successfully reached endpoint."))
        } else {
            steps.add(DiagnosticStep("Connection", DiagnosticStatus.FAILED, "HTTP $httpCode ${response.message}"))
        }

        // 2. Authentication Step
        if (httpCode == 401 || httpCode == 403) {
            steps.add(
                DiagnosticStep(
                    "Authentication",
                    DiagnosticStatus.FAILED,
                    "HTTP $httpCode Unauthorized/Forbidden. Check API key, user ID, or header credentials."
                )
            )
            return@withContext SourceDiagnosticReport(
                isSuccess = false,
                httpStatus = httpCode,
                summary = "Authentication failed (HTTP $httpCode).",
                steps = steps
            )
        } else {
            if (source.hasAuthentication) {
                steps.add(DiagnosticStep("Authentication", DiagnosticStatus.PASSED, "Credentials accepted without authorization error."))
            } else {
                steps.add(DiagnosticStep("Authentication", DiagnosticStatus.PASSED, "Public endpoint (No authentication required)."))
            }
        }

        if (!isHttpOk) {
            return@withContext SourceDiagnosticReport(
                isSuccess = false,
                httpStatus = httpCode,
                summary = "Request failed with HTTP $httpCode.",
                steps = steps
            )
        }

        val bodyString = response.body?.string().orEmpty()
        if (bodyString.isBlank()) {
            steps.add(DiagnosticStep("Results", DiagnosticStatus.WARNING, "Server returned a 200 OK response with an empty body."))
            return@withContext SourceDiagnosticReport(
                isSuccess = true,
                httpStatus = httpCode,
                totalItems = 0,
                summary = "Connected successfully, but body was empty.",
                steps = steps
            )
        }

        // 3. Results Path & Items Parsing
        val trimmed = bodyString.trim()
        val itemsArray: JSONArray? = try {
            if (trimmed.startsWith("[")) {
                steps.add(DiagnosticStep("Results", DiagnosticStatus.PASSED, "Root JSON array detected."))
                JSONArray(trimmed)
            } else if (trimmed.startsWith("{")) {
                val rootObj = JSONObject(trimmed)
                val targetPath = source.itemsPath.trim()
                if (targetPath.isNotBlank()) {
                    val extracted = extractNestedJsonArray(rootObj, targetPath)
                    if (extracted != null) {
                        steps.add(DiagnosticStep("Results", DiagnosticStatus.PASSED, "Items array found at path '$targetPath' (${extracted.length()} item(s))."))
                        extracted
                    } else {
                        // Check common alternatives
                        val commonKey = listOf("post", "posts", "data", "results", "images").firstOrNull { rootObj.has(it) }
                        if (commonKey != null) {
                            steps.add(DiagnosticStep("Results", DiagnosticStatus.WARNING, "Results path '$targetPath' was not found, but array exists at '$commonKey'."))
                            rootObj.optJSONArray(commonKey)
                        } else {
                            steps.add(DiagnosticStep("Results", DiagnosticStatus.FAILED, "Results path '$targetPath' was not found in response object."))
                            null
                        }
                    }
                } else {
                    // Try auto-detection
                    val commonKey = listOf("post", "posts", "data", "results", "images").firstOrNull { rootObj.has(it) }
                    if (commonKey != null) {
                        val arr = rootObj.optJSONArray(commonKey)
                        steps.add(DiagnosticStep("Results", DiagnosticStatus.PASSED, "Auto-detected results array under '$commonKey' (${arr?.length() ?: 0} item(s))."))
                        arr
                    } else {
                        steps.add(DiagnosticStep("Results", DiagnosticStatus.WARNING, "No items array found at root or standard paths."))
                        JSONArray()
                    }
                }
            } else {
                steps.add(DiagnosticStep("Results", DiagnosticStatus.FAILED, "Response is not JSON (received HTML or plaintext)."))
                null
            }
        } catch (e: Exception) {
            steps.add(DiagnosticStep("Results", DiagnosticStatus.FAILED, "JSON Parse Error: ${e.message}"))
            null
        }

        if (itemsArray == null) {
            return@withContext SourceDiagnosticReport(
                isSuccess = false,
                httpStatus = httpCode,
                summary = "Failed to parse items array from API response.",
                steps = steps
            )
        }

        if (itemsArray.length() == 0) {
            steps.add(DiagnosticStep("Images", DiagnosticStatus.WARNING, "No items available to inspect image URLs."))
            steps.add(DiagnosticStep("Thumbnails", DiagnosticStatus.WARNING, "No items available to inspect thumbnail URLs."))
            steps.add(DiagnosticStep("Tags", DiagnosticStatus.WARNING, "No items available to inspect tags."))
            steps.add(DiagnosticStep("Ratings", DiagnosticStatus.WARNING, "No items available to inspect ratings."))
            steps.add(DiagnosticStep("Pagination", DiagnosticStatus.PASSED, "Page parameter configured: '${source.pageParam}' (starts at ${source.pageStartsAt})."))

            return@withContext SourceDiagnosticReport(
                isSuccess = true,
                httpStatus = httpCode,
                totalItems = 0,
                summary = "Connected to API successfully, but 0 items were returned.",
                steps = steps
            )
        }

        // Inspect first item
        val firstObj = itemsArray.optJSONObject(0)
        if (firstObj == null) {
            steps.add(DiagnosticStep("Images", DiagnosticStatus.FAILED, "First item in array is not a valid JSON object."))
            return@withContext SourceDiagnosticReport(
                isSuccess = false,
                httpStatus = httpCode,
                totalItems = itemsArray.length(),
                summary = "Array elements are not JSON objects.",
                steps = steps
            )
        }

        val parsedItems = parseMediaItems(source, bodyString)
        val firstParsed = parsedItems.firstOrNull()

        // 4. Image URL Mapping
        if (firstParsed != null && firstParsed.imageUrl.isNotBlank()) {
            steps.add(DiagnosticStep("Images", DiagnosticStatus.PASSED, "Image URL mapped successfully: '${firstParsed.imageUrl.take(45)}...'"))
        } else {
            val rawImage = extractValue(firstObj, source.imageUrlField)
            if (rawImage.isNotBlank()) {
                steps.add(DiagnosticStep("Images", DiagnosticStatus.PASSED, "Image field '${source.imageUrlField}' found: '$rawImage'."))
            } else {
                steps.add(DiagnosticStep("Images", DiagnosticStatus.FAILED, "No image URL found under configured field '${source.imageUrlField}'."))
            }
        }

        // 5. Thumbnail / Preview URL Mapping
        if (firstParsed != null && firstParsed.thumbnailUrl.isNotBlank() && firstParsed.thumbnailUrl != firstParsed.imageUrl) {
            steps.add(DiagnosticStep("Thumbnails", DiagnosticStatus.PASSED, "Preview URL mapped: '${firstParsed.thumbnailUrl.take(45)}...'"))
        } else {
            val rawThumb = extractValue(firstObj, source.thumbUrlField)
            if (rawThumb.isNotBlank()) {
                steps.add(DiagnosticStep("Thumbnails", DiagnosticStatus.PASSED, "Thumbnail field '${source.thumbUrlField}' found."))
            } else {
                steps.add(DiagnosticStep("Thumbnails", DiagnosticStatus.WARNING, "Thumbnail field '${source.thumbUrlField}' empty (falling back to image URL)."))
            }
        }

        // 6. Tags Extraction
        if (firstParsed != null && firstParsed.tags.isNotEmpty()) {
            steps.add(DiagnosticStep("Tags", DiagnosticStatus.PASSED, "Extracted ${firstParsed.tags.size} tag(s) (e.g. ${firstParsed.tags.take(3).joinToString(", ")})."))
        } else {
            val rawTags = extractValue(firstObj, source.tagsField)
            if (rawTags.isNotBlank()) {
                steps.add(DiagnosticStep("Tags", DiagnosticStatus.PASSED, "Tags field '${source.tagsField}' found."))
            } else {
                steps.add(DiagnosticStep("Tags", DiagnosticStatus.WARNING, "Tags field '${source.tagsField}' was empty or not found."))
            }
        }

        // 7. Ratings Extraction
        if (firstParsed != null) {
            steps.add(DiagnosticStep("Ratings", DiagnosticStatus.PASSED, "Rating mapped to '${firstParsed.rating.label}' (field: '${source.ratingField}')."))
        } else {
            steps.add(DiagnosticStep("Ratings", DiagnosticStatus.WARNING, "Rating field '${source.ratingField}' not found (defaulting to Safe)."))
        }

        // 8. Pagination Check
        steps.add(DiagnosticStep("Pagination", DiagnosticStatus.PASSED, "Page param '${source.pageParam}' (start=${source.pageStartsAt}, limit=${source.defaultPageSize})."))

        val allPassed = steps.none { it.status == DiagnosticStatus.FAILED }

        return@withContext SourceDiagnosticReport(
            isSuccess = allPassed,
            httpStatus = httpCode,
            totalItems = parsedItems.size,
            summary = if (allPassed) "All diagnostics passed! Source is ready to use (${parsedItems.size} items loaded)." else "Diagnostic issues detected.",
            steps = steps,
            sampleTitle = firstParsed?.title,
            sampleImageUrl = firstParsed?.imageUrl,
            sampleThumbUrl = firstParsed?.thumbnailUrl
        )
    }

    internal fun parseMediaItems(source: MediaSourceConfig, body: String): List<MediaItem> {
        val trimmed = body.trim()
        val itemsArray: JSONArray = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val rootObj = JSONObject(trimmed)
            if (source.itemsPath.isBlank()) {
                when {
                    rootObj.has("post") -> rootObj.optJSONArray("post")
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
            }
        }
        Log.d("ArtfluxDebug", "Parsed ${result.size} posts: $gifCount GIF(s), $videoCount VIDEO(s)")
        return result
    }

    private fun mapJsonToMediaItem(source: MediaSourceConfig, obj: JSONObject, index: Int): MediaItem? {
        val rawImage = extractValue(obj, source.imageUrlField)
        val rawThumb = extractValue(obj, source.thumbUrlField).ifBlank { rawImage }

        // Determine image, thumbnail, and sample URLs
        val (finalImage, finalThumb, finalSample) = resolveUrls(source, obj, rawImage, rawThumb)
        if (finalImage.isBlank()) return null

        val id = extractValue(obj, "id").ifBlank { "item-${source.id}-$index" }
        val title = extractValue(obj, source.titleField).ifBlank {
            extractValue(obj, "name").ifBlank { "Art #$id" }
        }

        val postUrl = when {
            source.apiUrl.contains("safebooru.org") -> "https://safebooru.org/index.php?page=post&s=view&id=$id"
            source.apiUrl.contains("danbooru") -> "https://danbooru.donmai.us/posts/$id"
            source.apiUrl.contains("yande.re") -> "https://yande.re/post/show/$id"
            source.apiUrl.contains("gelbooru.com") -> "https://gelbooru.com/index.php?page=post&s=view&id=$id"
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
        val fileExt = obj.optString("file_ext").lowercase().trim().ifBlank {
            finalImage.substringAfterLast('.', "").substringBefore('?').lowercase().takeIf { it.isNotBlank() }
        }
        val typeStr = extractValue(obj, source.mediaTypeField).lowercase()
        val imageExt = finalImage.substringAfterLast('.', "").substringBefore('?').lowercase()
        val rawImageExt = rawImage.substringAfterLast('.', "").substringBefore('?').lowercase()
        val safebooruImageField = obj.optString("image").lowercase()

        val isGif = fileExt == "gif" ||
                imageExt == "gif" ||
                rawImageExt == "gif" ||
                safebooruImageField.endsWith(".gif") ||
                typeStr == "gif" ||
                typeStr.contains("gif")

        val isVideo = !isGif && (
                fileExt in listOf("mp4", "webm", "mkv", "mov") ||
                imageExt in listOf("mp4", "webm", "mkv", "mov") ||
                rawImageExt in listOf("mp4", "webm", "mkv", "mov") ||
                safebooruImageField.endsWith(".mp4") ||
                safebooruImageField.endsWith(".webm") ||
                typeStr.contains("video") ||
                typeStr.contains("mp4") ||
                typeStr.contains("webm")
        )

        val mediaType = when {
            isVideo -> MediaType.VIDEO
            isGif -> MediaType.GIF
            else -> MediaType.IMAGE
        }

        val author = extractValue(obj, source.authorField).ifBlank { null }
        val width = obj.optInt("width", 0).takeIf { it > 0 } ?: obj.optInt("image_width", 0).takeIf { it > 0 }
        val height = obj.optInt("height", 0).takeIf { it > 0 } ?: obj.optInt("image_height", 0).takeIf { it > 0 }
        val fileSize = obj.optLong("file_size", 0L).takeIf { it > 0 }

        return MediaItem(
            id = id,
            title = title,
            imageUrl = finalImage,
            thumbnailUrl = finalThumb,
            sampleUrl = finalSample,
            postUrl = postUrl,
            tags = tagsList,
            rating = rating,
            mediaType = mediaType,
            width = width,
            height = height,
            author = author,
            sourceName = source.name,
            description = obj.optString("alt_text", obj.optString("description", "")).ifBlank { null },
            fileSize = fileSize,
            fileExt = fileExt
        )
    }

    private fun resolveUrls(
        source: MediaSourceConfig,
        obj: JSONObject,
        rawImage: String,
        rawThumb: String
    ): Triple<String, String, String?> {
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
                val sample = if (obj.optBoolean("sample", false)) {
                    "https://safebooru.org/samples/$directory/sample_$image"
                } else null
                return Triple(full, thumb, sample)
            }
        }

        // Danbooru handling
        if (source.apiUrl.contains("danbooru")) {
            val fileUrl = rawImage.ifBlank { obj.optString("file_url").ifBlank { obj.optString("large_file_url") } }
            val sampleUrl = obj.optString("large_file_url").takeIf { it.isNotBlank() }
            val previewUrl = rawThumb.ifBlank { obj.optString("preview_file_url").ifBlank { sampleUrl ?: fileUrl } }
            if (fileUrl.isNotBlank()) {
                return Triple(fixUrl(fileUrl), fixUrl(previewUrl), sampleUrl?.let { fixUrl(it) })
            }
        }

        // Yande.re handling
        if (source.apiUrl.contains("yande.re")) {
            val fileUrl = rawImage.ifBlank { obj.optString("file_url").ifBlank { obj.optString("sample_url") } }
            val previewUrl = rawThumb.ifBlank { obj.optString("preview_url").ifBlank { obj.optString("sample_url") } }
            val sampleUrl = obj.optString("sample_url").takeIf { it.isNotBlank() }
            if (fileUrl.isNotBlank()) {
                return Triple(fixUrl(fileUrl), fixUrl(previewUrl), sampleUrl?.let { fixUrl(it) })
            }
        }

        // Generic URL cleanup
        val full = fixUrl(rawImage)
        val sample = obj.optString("sample_url").ifBlank { obj.optString("large_file_url") }.takeIf { it.isNotBlank() }?.let { fixUrl(it) }
        val thumb = fixUrl(rawThumb.ifBlank { sample ?: rawImage })
        return Triple(full, thumb, sample)
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
