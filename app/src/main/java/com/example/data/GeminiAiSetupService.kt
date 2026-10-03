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

class GeminiAiSetupService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeApiSpecOrJson(input: String): Result<MediaSourceConfig> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Please paste API documentation or an example JSON response."))
        }

        // Check if Gemini API Key is available
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val config = callGeminiApi(apiKey, trimmed)
                if (config != null) {
                    return@withContext Result.success(config)
                }
            } catch (e: Exception) {
                // If Gemini call fails, fallback to local heuristic parser
                e.printStackTrace()
            }
        }

        // Local smart heuristic analyzer fallback
        val heuristicConfig = analyzeWithHeuristics(trimmed)
        if (heuristicConfig != null) {
            return@withContext Result.success(heuristicConfig)
        }

        Result.failure(Exception("Unable to automatically detect API structure. Please review the manual fields or check your Gemini API key in Secrets."))
    }

    private fun callGeminiApi(apiKey: String, input: String): MediaSourceConfig? {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val systemPrompt = """
            You are an API integration engineer. The user is configuring an image/art media browser source.
            Analyze the provided API documentation or example JSON response.
            Extract or infer the fields required to query and parse the API.
            Do NOT write executable code or scripts. Output valid JSON only with exactly these string keys:
            {
              "sourceName": "A concise name for the source",
              "apiUrl": "Base HTTP GET endpoint URL to search or list media items",
              "searchParam": "Query parameter name for search keywords (e.g. q, query, tags, or empty)",
              "pageParam": "Query parameter name for pagination (e.g. page, p, offset, or empty)",
              "itemsPath": "JSON path to the array containing items (e.g. data, results, images, or empty if root is array)",
              "imageUrlField": "JSON path for the full-resolution image URL (e.g. url, file_url, path, download_url)",
              "thumbUrlField": "JSON path for the thumbnail URL (e.g. thumbnail, preview_url, thumb)",
              "postUrlField": "JSON path for post or webpage URL (e.g. link, url, id, short_url)",
              "tagsField": "JSON path for tags list or string (e.g. tags, labels)",
              "ratingField": "JSON path for content rating (e.g. rating, purity)",
              "mediaTypeField": "JSON path for media type (e.g. type, file_type)",
              "titleField": "JSON path for title (e.g. title, name, description)",
              "authorField": "JSON path for creator/artist (e.g. author, user.name, artist)",
              "description": "Brief summary of what this source provides"
            }
        """.trimIndent()

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Input to analyze:\n\n$input")
                        })
                    }
                    put("parts", partsArray)
                })
            }
            put("contents", contentsArray)

            val systemInstruction = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                }
                put("parts", parts)
            }
            put("systemInstruction", systemInstruction)

            val genConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            }
            put("generationConfig", genConfig)
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return null
        }

        val responseBody = response.body?.string().orEmpty()
        val root = JSONObject(responseBody)
        val text = root.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text") ?: return null

        val parsed = JSONObject(text.trim())
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

    private fun analyzeWithHeuristics(input: String): MediaSourceConfig? {
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
        } catch (e: Exception) {
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
