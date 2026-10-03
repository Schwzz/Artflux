package com.example.data

import com.example.BuildConfig
import com.example.model.MediaSourceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAiSetupService(
    private val customBackendUrl: String? = null
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Resolves backend proxy URL from constructor or BuildConfig (without credentials)
    private val backendBaseUrl: String by lazy {
        if (!customBackendUrl.isNullOrBlank()) {
            customBackendUrl.trim().removeSuffix("/")
        } else {
            getBackendUrlFromBuildConfig().removeSuffix("/")
        }
    }

    private fun getBackendUrlFromBuildConfig(): String {
        return try {
            val field = BuildConfig::class.java.getField("ARTFLUX_AI_BACKEND_URL")
            (field.get(null) as? String)?.trim().orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    suspend fun analyzeApiSpecOrJson(input: String): Result<MediaSourceConfig> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Please paste API documentation or an example JSON response."))
        }

        // 1. Try secure serverless backend proxy if configured
        if (backendBaseUrl.isNotBlank() && (backendBaseUrl.startsWith("http://") || backendBaseUrl.startsWith("https://"))) {
            try {
                val serverConfig = callBackendProxy(backendBaseUrl, trimmed)
                if (serverConfig != null) {
                    return@withContext Result.success(serverConfig)
                }
            } catch (_: Exception) {
                // Backend proxy error or unreachable; gracefully fallback to local heuristic analyzer
            }
        }

        // 2. Local smart heuristic analyzer fallback
        val heuristicConfig = analyzeWithHeuristics(trimmed)
        if (heuristicConfig != null) {
            return@withContext Result.success(heuristicConfig)
        }

        Result.failure(
            Exception(
                if (backendBaseUrl.isBlank()) {
                    "Unable to automatically detect API structure from input. Please review manual fields or configure ARTFLUX_AI_BACKEND_URL."
                } else {
                    "Backend AI analysis unavailable and local heuristic detection could not find valid items. Please verify manual fields."
                }
            )
        )
    }

    private fun callBackendProxy(baseUrl: String, input: String): MediaSourceConfig? {
        val endpoint = "$baseUrl/api/analyze-source"

        val requestJson = JSONObject().apply {
            put("input", input)
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .header("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return null
        }

        val responseBody = response.body?.string().orEmpty()
        if (responseBody.isBlank()) return null

        val parsed = JSONObject(responseBody)
        return MediaSourceConfig(
            name = parsed.optString("sourceName", "AI Configured Source").ifBlank { "AI Configured Source" },
            apiUrl = parsed.optString("apiUrl", "https://api.example.com/v1/search"),
            searchParam = parsed.optString("searchParam", "q"),
            pageParam = parsed.optString("pageParam", "page"),
            itemsPath = parsed.optString("itemsPath", ""),
            imageUrlField = parsed.optString("imageUrlField", "url"),
            thumbUrlField = parsed.optString("thumbUrlField", "thumbnail"),
            postUrlField = parsed.optString("postUrlField", "link"),
            tagsField = parsed.optString("tagsField", "tags"),
            ratingField = parsed.optString("ratingField", "rating"),
            mediaTypeField = parsed.optString("mediaTypeField", "media_type"),
            titleField = parsed.optString("titleField", "title"),
            authorField = parsed.optString("authorField", "author"),
            description = parsed.optString("description", "Imported using Gemini AI Setup"),
            isBuiltIn = false
        )
    }

    fun analyzeWithHeuristics(input: String): MediaSourceConfig? {
        try {
            var itemsArray: JSONArray? = null
            var itemsPath = ""

            if (input.startsWith("[")) {
                itemsArray = JSONArray(input)
                itemsPath = ""
            } else if (input.startsWith("{")) {
                val root = JSONObject(input)
                val candidatePaths = listOf("data", "results", "images", "artworks", "posts", "items", "hits", "photos")
                for (key in candidatePaths) {
                    if (root.has(key) && root.optJSONArray(key) != null) {
                        itemsArray = root.getJSONArray(key)
                        itemsPath = key
                        break
                    }
                }
                if (itemsArray == null) {
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = root.opt(key)
                        if (value is JSONArray && value.length() > 0 && value.optJSONObject(0) != null) {
                            itemsArray = value
                            itemsPath = key
                            break
                        }
                    }
                }
            }

            val sampleObj = itemsArray?.optJSONObject(0)
            if (sampleObj != null) {
                val keys = mutableListOf<String>()
                val iter = sampleObj.keys()
                while (iter.hasNext()) {
                    keys.add(iter.next())
                }

                val imageKey = findBestMatchingKey(keys, listOf("download_url", "image_url", "file_url", "sample_url", "url", "image", "src", "path", "large")) ?: "url"
                val thumbKey = findBestMatchingKey(keys, listOf("thumb_url", "thumbnail_url", "preview_url", "thumb", "thumbnail", "preview", "small")) ?: imageKey
                val postKey = findBestMatchingKey(keys, listOf("post_url", "source_url", "short_url", "link", "url", "id", "permalink")) ?: "url"
                val tagsKey = findBestMatchingKey(keys, listOf("tags", "tag_string", "term_titles", "labels", "categories", "keywords")) ?: ""
                val ratingKey = findBestMatchingKey(keys, listOf("rating", "purity", "content_rating", "safety")) ?: ""
                val mediaTypeKey = findBestMatchingKey(keys, listOf("media_type", "file_type", "type", "category")) ?: ""
                val titleKey = findBestMatchingKey(keys, listOf("title", "name", "caption", "description", "label")) ?: "title"
                val authorKey = findBestMatchingKey(keys, listOf("author", "artist", "user", "creator", "artist_title", "owner")) ?: "author"

                return MediaSourceConfig(
                    name = "Pasted JSON Source",
                    apiUrl = "https://api.example.com/v1/search",
                    searchParam = "q",
                    pageParam = "page",
                    itemsPath = itemsPath,
                    imageUrlField = imageKey,
                    thumbUrlField = thumbKey,
                    postUrlField = postKey,
                    tagsField = tagsKey,
                    ratingField = ratingKey,
                    mediaTypeField = mediaTypeKey,
                    titleField = titleKey,
                    authorField = authorKey,
                    description = "Detected from JSON structure with ${itemsArray.length()} sample items",
                    isBuiltIn = false
                )
            }
        } catch (_: Exception) {
            // Not a direct JSON response, maybe documentation text
        }
        return null
    }

    private fun findBestMatchingKey(availableKeys: List<String>, candidates: List<String>): String? {
        for (candidate in candidates) {
            val exact = availableKeys.firstOrNull { it.equals(candidate, ignoreCase = true) }
            if (exact != null) return exact
        }
        for (candidate in candidates) {
            val partial = availableKeys.firstOrNull { it.lowercase().contains(candidate) }
            if (partial != null) return partial
        }
        return null
    }
}
